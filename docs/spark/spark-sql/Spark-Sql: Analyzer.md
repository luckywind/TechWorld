# Analyzer

[参考](https://www.jianshu.com/p/47de88e24bd7)

当一条 sql 语句被 SparkSqlParser 解析为一个 unresolved logicalPlan 后，接下来就会使用 Analyzer 进行 resolve。所谓的 resolve 也就是在未解析的 db、table、function、partition 等对应的 node 上应用一条条 Rule（规则）来替换为新的 node，应用 Rule 的过程中往往会访问 catalog 来获取相应的信息。

org.apache.spark.sql.catalyst.analysis.Analyzer$ResolveReferences应用之前的plan

```
'Project [unresolvedalias(cast((1 > 0) as bigint), None)]
+- OneRowRelation
```

## ResolveReferences

```scala
    def apply(plan: LogicalPlan): LogicalPlan = plan.resolveOperatorsUpWithPruning(
      AlwaysProcess.fn, ruleId) {
      case p: LogicalPlan if !p.childrenResolved => p

      // Wait for the rule `DeduplicateRelations` to resolve conflicting attrs first.
      case p: LogicalPlan if hasConflictingAttrs(p) => p

      // If the projection list contains Stars, expand it.
      case p: Project if containsStar(p.projectList) =>
        p.copy(projectList = buildExpandedProjectList(p.projectList, p.child))
      // If the aggregate function argument contains Stars, expand it.
      case a: Aggregate if containsStar(a.aggregateExpressions) =>
        if (a.groupingExpressions.exists(_.isInstanceOf[UnresolvedOrdinal])) {
          throw QueryCompilationErrors.starNotAllowedWhenGroupByOrdinalPositionUsedError()
        } else {
          a.copy(aggregateExpressions = buildExpandedProjectList(a.aggregateExpressions, a.child))
        }
      case g: Generate if containsStar(g.generator.children) =>
        throw QueryCompilationErrors.invalidStarUsageError("explode/json_tuple/UDTF",
          extractStar(g.generator.children))

      case u @ Union(children, _, _)
        // if there are duplicate output columns, give them unique expr ids
          if children.exists(c => c.output.map(_.exprId).distinct.length < c.output.length) =>
        val newChildren = children.map { c =>
          if (c.output.map(_.exprId).distinct.length < c.output.length) {
            val existingExprIds = mutable.HashSet[ExprId]()
            val projectList = c.output.map { attr =>
              if (existingExprIds.contains(attr.exprId)) {
                // replace non-first duplicates with aliases and tag them
                val newMetadata = new MetadataBuilder().withMetadata(attr.metadata)
                  .putNull("__is_duplicate").build()
                Alias(attr, attr.name)(explicitMetadata = Some(newMetadata))
              } else {
                // leave first duplicate alone
                existingExprIds.add(attr.exprId)
                attr
              }
            }
            Project(projectList, c)
          } else {
            c
          }
        }
        u.withNewChildren(newChildren)

      // When resolve `SortOrder`s in Sort based on child, don't report errors as
      // we still have chance to resolve it based on its descendants
      case s @ Sort(ordering, global, child) if child.resolved && !s.resolved =>
        val newOrdering =
          ordering.map(order => resolveExpressionByPlanOutput(order, child).asInstanceOf[SortOrder])
        Sort(newOrdering, global, child)

      // A special case for Generate, because the output of Generate should not be resolved by
      // ResolveReferences. Attributes in the output will be resolved by ResolveGenerate.
      case g @ Generate(generator, _, _, _, _, _) if generator.resolved => g

      case g @ Generate(generator, join, outer, qualifier, output, child) =>
        val newG = resolveExpressionByPlanOutput(generator, child, throws = true)
        if (newG.fastEquals(generator)) {
          g
        } else {
          Generate(newG.asInstanceOf[Generator], join, outer, qualifier, output, child)
        }

      // Skips plan which contains deserializer expressions, as they should be resolved by another
      // rule: ResolveDeserializer.
      case plan if containsDeserializer(plan.expressions) => plan

      // SPARK-31670: Resolve Struct field in groupByExpressions and aggregateExpressions
      // with CUBE/ROLLUP will be wrapped with alias like Alias(GetStructField, name) with
      // different ExprId. This cause aggregateExpressions can't be replaced by expanded
      // groupByExpressions in `ResolveGroupingAnalytics.constructAggregateExprs()`, we trim
      // unnecessary alias of GetStructField here.
      case a: Aggregate =>
        val planForResolve = a.child match {
          // SPARK-25942: Resolves aggregate expressions with `AppendColumns`'s children, instead of
          // `AppendColumns`, because `AppendColumns`'s serializer might produce conflict attribute
          // names leading to ambiguous references exception.
          case appendColumns: AppendColumns => appendColumns
          case _ => a
        }

        val resolvedGroupingExprs = a.groupingExpressions
          .map(resolveExpressionByPlanChildren(_, planForResolve))
          .map(trimTopLevelGetStructFieldAlias)

        val resolvedAggExprs = a.aggregateExpressions
          .map(resolveExpressionByPlanChildren(_, planForResolve))
            .map(_.asInstanceOf[NamedExpression])

        a.copy(resolvedGroupingExprs, resolvedAggExprs, a.child)

      case o: OverwriteByExpression if o.table.resolved =>
        // The delete condition of `OverwriteByExpression` will be passed to the table
        // implementation and should be resolved based on the table schema.
        o.copy(deleteExpr = resolveExpressionByPlanOutput(o.deleteExpr, o.table))

      case m @ MergeIntoTable(targetTable, sourceTable, _, _, _)
        if !m.resolved && targetTable.resolved && sourceTable.resolved =>

        EliminateSubqueryAliases(targetTable) match {
          case r: NamedRelation if r.skipSchemaResolution =>
            // Do not resolve the expression if the target table accepts any schema.
            // This allows data sources to customize their own resolution logic using
            // custom resolution rules.
            m

          case _ =>
            val newMatchedActions = m.matchedActions.map {
              case DeleteAction(deleteCondition) =>
                val resolvedDeleteCondition = deleteCondition.map(
                  resolveExpressionByPlanChildren(_, m))
                DeleteAction(resolvedDeleteCondition)
              case UpdateAction(updateCondition, assignments) =>
                val resolvedUpdateCondition = updateCondition.map(
                  resolveExpressionByPlanChildren(_, m))
                UpdateAction(
                  resolvedUpdateCondition,
                  // The update value can access columns from both target and source tables.
                  resolveAssignments(assignments, m, resolveValuesWithSourceOnly = false))
              case UpdateStarAction(updateCondition) =>
                val assignments = targetTable.output.map { attr =>
                  Assignment(attr, UnresolvedAttribute(Seq(attr.name)))
                }
                UpdateAction(
                  updateCondition.map(resolveExpressionByPlanChildren(_, m)),
                  // For UPDATE *, the value must from source table.
                  resolveAssignments(assignments, m, resolveValuesWithSourceOnly = true))
              case o => o
            }
            val newNotMatchedActions = m.notMatchedActions.map {
              case InsertAction(insertCondition, assignments) =>
                // The insert action is used when not matched, so its condition and value can only
                // access columns from the source table.
                val resolvedInsertCondition = insertCondition.map(
                  resolveExpressionByPlanChildren(_, Project(Nil, m.sourceTable)))
                InsertAction(
                  resolvedInsertCondition,
                  resolveAssignments(assignments, m, resolveValuesWithSourceOnly = true))
              case InsertStarAction(insertCondition) =>
                // The insert action is used when not matched, so its condition and value can only
                // access columns from the source table.
                val resolvedInsertCondition = insertCondition.map(
                  resolveExpressionByPlanChildren(_, Project(Nil, m.sourceTable)))
                val assignments = targetTable.output.map { attr =>
                  Assignment(attr, UnresolvedAttribute(Seq(attr.name)))
                }
                InsertAction(
                  resolvedInsertCondition,
                  resolveAssignments(assignments, m, resolveValuesWithSourceOnly = true))
              case o => o
            }
            val resolvedMergeCondition = resolveExpressionByPlanChildren(m.mergeCondition, m)
            m.copy(mergeCondition = resolvedMergeCondition,
              matchedActions = newMatchedActions,
              notMatchedActions = newNotMatchedActions)
        }

      // Skip the having clause here, this will be handled in ResolveAggregateFunctions.
      case h: UnresolvedHaving => h

      case q: LogicalPlan =>
        logTrace(s"Attempting to resolve ${q.simpleString(conf.maxToStringFields)}")
      // 对表达式进行解析
        q.mapExpressions(resolveExpressionByPlanChildren(_, q))
    }
```



### resolveOperatorsUpWithPruning

把rule按照后序递归应用到所有子节点以及自己后，返回。会跳过标记为analyzed的子树。

```scala
 
def resolveOperatorsUpWithPruning(cond: TreePatternBits => Boolean,
    ruleId: RuleId = UnknownRuleId)(rule: PartialFunction[LogicalPlan, LogicalPlan])
  : LogicalPlan = {
    if (!analyzed && cond.apply(self) && !isRuleIneffective(ruleId)) {
      AnalysisHelper.allowInvokingTransformsInAnalyzer {
        // 递归应用到子节点
        val afterRuleOnChildren = mapChildren(_.resolveOperatorsUpWithPruning(cond, ruleId)(rule))
        val afterRule = if (self fastEquals afterRuleOnChildren) {
          CurrentOrigin.withOrigin(origin) {
            rule.applyOrElse(self, identity[LogicalPlan])
          }
        } else {
          CurrentOrigin.withOrigin(origin) {
            rule.applyOrElse(afterRuleOnChildren, identity[LogicalPlan])
          }
        }
        if (self eq afterRule) {
          self.markRuleAsIneffective(ruleId)
          self
        } else {
          afterRule.copyTagsFrom(self)
          afterRule
        }
      }
    } else {
      self
    }
  }
```

### resolveExpressionByPlanChildren

解析UnresolvedAttribute

```scala
   * @param e The expression need to be resolved.
   * @param q The LogicalPlan whose children are used to resolve expression's attribute.
   * @return resolved Expression.  
def resolveExpressionByPlanChildren(
      e: Expression,
      q: LogicalPlan): Expression = {
    resolveExpression(
      e,
      resolveColumnByName = nameParts => {
        q.resolveChildren(nameParts, resolver)
      },
      getAttrCandidates = () => {
        assert(q.children.length == 1)
        q.children.head.output
      },
      throws = true)
  }
```

#### resolveExpression

解析UnresolvedAttribute，GetColumnByOrdinal

```scala
  private def resolveExpression(
      expr: Expression,
      resolveColumnByName: Seq[String] => Option[Expression],
      getAttrCandidates: () => Seq[Attribute],
      throws: Boolean): Expression = {
    // 先定义一个函数
    def innerResolve(e: Expression, isTopLevel: Boolean): Expression = {
      // 已经解析则直接返回， 这个resolved是lazy执行的，不同的表达式有自己的实现
      if (e.resolved) return e
      e match {
        case f: LambdaFunction if !f.bound => f

        case GetColumnByOrdinal(ordinal, _) =>
          val attrCandidates = getAttrCandidates()
          assert(ordinal >= 0 && ordinal < attrCandidates.length)
          attrCandidates(ordinal)

        case GetViewColumnByNameAndOrdinal(
            viewName, colName, ordinal, expectedNumCandidates, viewDDL) =>
          val attrCandidates = getAttrCandidates()
          val matched = attrCandidates.filter(a => resolver(a.name, colName))
          if (matched.length != expectedNumCandidates) {
            throw QueryCompilationErrors.incompatibleViewSchemaChange(
              viewName, colName, expectedNumCandidates, matched, viewDDL)
          }
          matched(ordinal)

        case u @ UnresolvedAttribute(nameParts) =>
          val result = withPosition(u) {
            resolveColumnByName(nameParts).orElse(resolveLiteralFunction(nameParts)).map {
              // We trim unnecessary alias here. Note that, we cannot trim the alias at top-level,
              // as we should resolve `UnresolvedAttribute` to a named expression. The caller side
              // can trim the top-level alias if it's safe to do so. Since we will call
              // CleanupAliases later in Analyzer, trim non top-level unnecessary alias is safe.
              case Alias(child, _) if !isTopLevel => child
              case other => other
            }.getOrElse(u)
          }
          logDebug(s"Resolving $u to $result")
          result

        case u @ UnresolvedExtractValue(child, fieldName) =>
          val newChild = innerResolve(child, isTopLevel = false)
          if (newChild.resolved) {
            withOrigin(u.origin) {
              ExtractValue(newChild, fieldName, resolver)
            }
          } else {
            u.copy(child = newChild)
          }
				// 其他情况，例如alias，解析其孩子
        case _ => e.mapChildren(innerResolve(_, isTopLevel = false))
      }
    }

    //解析，expr例子: unresolvedalias(cast((1 > 0) as bigint), None)
    // UnresolvedAlias是一个UnaryExpression
    try {
      innerResolve(expr, isTopLevel = true)
    } catch {
      case ae: AnalysisException if !throws =>
        logDebug(ae.getMessage)
        expr
    }
  }
```



例如，对于cast而言，其resolve会通过canCast校验输入、输出类型

```scala
childrenResolved && checkInputDataTypes().isSuccess && (!needsTimeZone || timeZoneId.isDefined)

Cast.scala
  override def checkInputDataTypes(): TypeCheckResult = {
    // canCast这里定义了所有spark支持的类型转换
    if (canCast(child.dataType, dataType)) {
      TypeCheckResult.TypeCheckSuccess
    } else {
      TypeCheckResult.TypeCheckFailure(typeCheckFailureMessage)
    }
  }
```



## ApplyCharTypePadding

这个规则用于charl类型比较时的padding，把短的padding为长的。 string和char类型比较时，会被当成char

```scala
  override def apply(plan: LogicalPlan): LogicalPlan = {
    if (SQLConf.get.charVarcharAsString) {
      return plan
    }
    // 对包含二元比较或者IN的节点应用规则
    plan.resolveOperatorsUpWithPruning(_.containsAnyPattern(BINARY_COMPARISON, IN)) {
      case operator => operator.transformExpressionsUpWithPruning(
        _.containsAnyPattern(BINARY_COMPARISON, IN)) {
        case e if !e.childrenResolved => e

        // String literal is treated as char type when it's compared to a char type column.
        // We should pad the shorter one to the longer length.
        case b @ BinaryComparison(e @ AttrOrOuterRef(attr), lit) if lit.foldable =>
          padAttrLitCmp(e, attr.metadata, lit).map { newChildren =>
            b.withNewChildren(newChildren)
          }.getOrElse(b)

        case b @ BinaryComparison(lit, e @ AttrOrOuterRef(attr)) if lit.foldable =>
          padAttrLitCmp(e, attr.metadata, lit).map { newChildren =>
            b.withNewChildren(newChildren.reverse)
          }.getOrElse(b)
/**in表达式的处理
  
*/
        case i @ In(e @ AttrOrOuterRef(attr), list)
          if attr.dataType == StringType && list.forall(_.foldable) =>
          CharVarcharUtils.getRawType(attr.metadata).flatMap {
            case CharType(length) =>
              val (nulls, literalChars) =  // 过滤掉null
                list.map(_.eval().asInstanceOf[UTF8String]).partition(_ == null)
              val literalCharLengths = literalChars.map(_.numChars())//计算每个长度
              val targetLen = (length +: literalCharLengths).max //最终长度： 输入列和参数所有长度取最大
              Some(i.copy(
                //输入列padding
                value = addPadding(e, length, targetLen),
                //参数列表padding
                list = list.zip(literalCharLengths).map {
                  case (lit, charLength) => addPadding(lit, charLength, targetLen)
                } ++ nulls.map(Literal.create(_, StringType))))
            case _ => None
          }.getOrElse(i)

        // For char type column or inner field comparison, pad the shorter one to the longer length.
        case b @ BinaryComparison(e1 @ AttrOrOuterRef(left), e2 @ AttrOrOuterRef(right))
            // For the same attribute, they must be the same length and no padding is needed.
            if !left.semanticEquals(right) =>
          val outerRefs = (e1, e2) match {
            case (_: OuterReference, _: OuterReference) => Seq(left, right)
            case (_: OuterReference, _) => Seq(left)
            case (_, _: OuterReference) => Seq(right)
            case _ => Nil
          }
          val newChildren = CharVarcharUtils.addPaddingInStringComparison(Seq(left, right))
          if (outerRefs.nonEmpty) {
            b.withNewChildren(newChildren.map(_.transform {
              case a: Attribute if outerRefs.exists(_.semanticEquals(a)) => OuterReference(a)
            }))
          } else {
            b.withNewChildren(newChildren)
          }

        case i @ In(e @ AttrOrOuterRef(attr), list) if list.forall(_.isInstanceOf[Attribute]) =>
          val newChildren = CharVarcharUtils.addPaddingInStringComparison(
            attr +: list.map(_.asInstanceOf[Attribute]))
          if (e.isInstanceOf[OuterReference]) {
            i.copy(
              value = newChildren.head.transform {
                case a: Attribute if a.semanticEquals(attr) => OuterReference(a)
              },
              list = newChildren.tail)
          } else {
            i.copy(value = newChildren.head, list = newChildren.tail)
          }
      }
    }
  }

```







# Optimizer:常量折叠

## 前置函数

### transformDownWithPruning

<font color=red>rule按照前序应用到所有子节点后返回节点副本</font>

cond:一个表示是否遍历该节点子树的lambda表达式，如果cond.apply返回true则遍历该子树，false跳过该子树

```scala
def transformDownWithPruning(cond: TreePatternBits => Boolean,
  ruleId: RuleId = UnknownRuleId)(rule: PartialFunction[BaseType, BaseType])
: BaseType 
= {
    if (!cond.apply(this) || isRuleIneffective(ruleId)) {
      return this
    }
    val afterRule = CurrentOrigin.withOrigin(origin) {
      rule.applyOrElse(this, identity[BaseType])
    }

    // Check if unchanged and then possibly return old copy to avoid gc churn.
    if (this fastEquals afterRule) {
      val rewritten_plan = mapChildren(_.transformDownWithPruning(cond, ruleId)(rule))
      if (this eq rewritten_plan) {
        markRuleAsIneffective(ruleId)
        this
      } else {
        rewritten_plan
      }
    } else {
      // If the transform function replaces this node with a new one, carry over the tags.
      afterRule.copyTagsFrom(this)
      afterRule.mapChildren(_.transformDownWithPruning(cond, ruleId)(rule))
    }
  }
```

### transformWithPruning

直接调用了transformDownWithPruning

```scala
def transformWithPruning(cond: TreePatternBits => Boolean,
  ruleId: RuleId = UnknownRuleId)(rule: PartialFunction[BaseType, BaseType])
: BaseType = {
  transformDownWithPruning(cond, ruleId)(rule)
}
```



### transformExpressionsDownWithPruning

<font color=red>所有表达式执行transformDownWithPruning(cond, ruleId)(rule)</font>

```scala
def transformExpressionsDownWithPruning(cond: TreePatternBits => Boolean,
  ruleId: RuleId = UnknownRuleId)(rule: PartialFunction[Expression, Expression])
: this.type = {
  mapExpressions(_.transformDownWithPruning(cond, ruleId)(rule))
}
```

## ConstantFolding

```scala
def apply(plan: LogicalPlan): LogicalPlan =   
plan.transformWithPruning(AlwaysProcess.fn, ruleId) {//按前序顺序对所有子节点做下面的事情：
   //当前算子做下面的事情：
  case q: LogicalPlan => q.transformExpressionsDownWithPruning(
    AlwaysProcess.fn, ruleId) 
    //该算子下所有表达式做下面的事情
   {
    case l: Literal => l
      //size函数，计算array长度
    case Size(c: CreateArray, _) if c.children.forall(hasNoSideEffect) =>
      Literal(c.children.length)
     //size函数，计算map大小
    case Size(c: CreateMap, _) if c.children.forall(hasNoSideEffect) =>
      Literal(c.children.length / 2)

    // 对可折叠表达式进行计算
    case e if e.foldable => Literal.create(e.eval(EmptyRow), e.dataType)
  }
}
```

foldable是所有表达式都有的一个属性，折叠规则：

1. Coalesce在所有子节点可折叠的条件下可折叠
2. BinaryExpression在所有子节点可折叠的条件下可折叠
3. Not/IsNull/IsNotNull 在子节点可折叠的条件下可折叠
4. Cast /  UnaryMinus 在子节点可折叠的条件下可折叠

### 案例

以这条sql为例

```scala
spark.sql("select cast('2000-08-23' as date) from warehouse").collect()
```

常量折叠前的计划,是有cast运算的

```
Project [cast(2000-08-23 as date) AS CAST(2000-08-23 AS DATE)#28]
+- Relation [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet
```


Cast的子节点是常量是可折叠的，

应用完常量折叠后的计划,已经没有cast表达式了

```
Project [2000-08-23 AS CAST(2000-08-23 AS DATE)#28]
+- Relation [w_warehouse_sk#0,w_warehouse_id#1,w_warehouse_name#2,w_warehouse_sq_ft#3,w_street_number#4,w_street_name#5,w_street_type#6,w_suite_number#7,w_city#8,w_county#9,w_state#10,w_zip#11,w_country#12,w_gmt_offset#13] parquet
```