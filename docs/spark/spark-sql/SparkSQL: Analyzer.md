# Analyzer

[参考](https://www.jianshu.com/p/47de88e24bd7)

当一条 sql 语句被 SparkSqlParser 解析为一个 unresolved logicalPlan 后，接下来就会使用 Analyzer 进行 resolve。所谓的 resolve 也就是在未解析的 db、table、function、partition 等对应的 node 上应用一条条 Rule（规则）来替换为新的 node，应用 Rule 的过程中往往会访问 catalog 来获取相应的信息。



```scala
  def executeAndCheck(plan: LogicalPlan, tracker: QueryPlanningTracker): LogicalPlan = {
    if (plan.analyzed) return plan
    AnalysisHelper.markInAnalyzer {
      val analyzed = executeAndTrack(plan, tracker)
      try {
        //注意这个步骤，在解析完成后，还做了一步checkAnalysis： 内联所有CTE
        checkAnalysis(analyzed)
        analyzed
      } catch {
        case e: AnalysisException =>
          val ae = e.copy(plan = Option(analyzed))
          ae.setStackTrace(e.getStackTrace)
          throw ae
      }
    }
  }
```

# batchs

Batch("Substitution", fixedPoint,
  // This rule optimizes `UpdateFields` expression chains so looks more like optimization rule.
  // However, when manipulating deeply nested schema, `UpdateFields` expression tree could be
  // very complex and make analysis impossible. Thus we need to optimize `UpdateFields` early
  // at the beginning of analysis.
  OptimizeUpdateFields,
  CTESubstitution,
  WindowsSubstitution,
  EliminateUnions,
  SubstituteUnresolvedOrdinals),
Batch("Disable Hints", Once,
  new ResolveHints.DisableHints),
Batch("Hints", fixedPoint,
  ResolveHints.ResolveJoinStrategyHints,
  ResolveHints.ResolveCoalesceHints),
Batch("Simple Sanity Check", Once,
  LookupFunctions),
Batch("Keep Legacy Outputs", Once,
  KeepLegacyOutputs),
Batch("Resolution", fixedPoint,
  ResolveTableValuedFunctions(*v1SessionCatalog*) ::
  ResolveNamespace(catalogManager) ::
  new ResolveCatalogs(catalogManager) ::
  ResolveUserSpecifiedColumns ::
  ResolveInsertInto ::
  ResolveRelations ::
  ResolvePartitionSpec ::
  ResolveFieldNameAndPosition ::
  AddMetadataColumns ::
  DeduplicateRelations ::
  ResolveReferences ::
  ResolveExpressionsWithNamePlaceholders ::
  ResolveDeserializer ::
  ResolveNewInstance ::
  ResolveUpCast ::
  ResolveGroupingAnalytics ::
  ResolvePivot ::
  ResolveOrdinalInOrderByAndGroupBy ::
  ResolveAggAliasInGroupBy ::
  ResolveMissingReferences ::
  ExtractGenerator ::
  ResolveGenerate ::
  ResolveFunctions ::
  ResolveAliases ::
  ResolveSubquery ::
  ResolveSubqueryColumnAliases ::
  ResolveWindowOrder ::
  ResolveWindowFrame ::
  ResolveNaturalAndUsingJoin ::
  ResolveOutputRelation ::
  ExtractWindowExpressions ::
  GlobalAggregates ::
  ResolveAggregateFunctions ::
  TimeWindowing ::
  SessionWindowing ::
  ResolveDefaultColumns(this, *v1SessionCatalog*) ::
  ResolveInlineTables ::
  ResolveLambdaVariables ::
  ResolveTimeZone ::
  ResolveRandomSeed ::
  ResolveBinaryArithmetic ::
  ResolveUnion ::
  RewriteDeleteFromTable ::
  typeCoercionRules ++
  Seq(ResolveWithCTE) ++
  *extendedResolutionRules* : _*),
Batch("Remove TempResolvedColumn", Once, RemoveTempResolvedColumn),
Batch("Apply Char Padding", Once,
  ApplyCharTypePadding),
Batch("Post-Hoc Resolution", Once,
  Seq(ResolveCommandsWithIfExists) ++
  *postHocResolutionRules*: _*),
Batch("Remove Unresolved Hints", Once,
  new ResolveHints.RemoveAllHints),
Batch("Nondeterministic", Once,
  PullOutNondeterministic),
Batch("UDF", Once,
  HandleNullInputsForUDF,
  ResolveEncodersInUDF),
Batch("UpdateNullability", Once,
  UpdateAttributeNullability),
Batch("Subquery", Once,
  UpdateOuterReferences),
Batch("Cleanup", fixedPoint,
  CleanupAliases),
Batch("HandleAnalysisOnlyCommand", Once,
  HandleAnalysisOnlyCommand)



## ResolveReferences
org.apache.spark.sql.catalyst.analysis.Analyzer$ResolveReferences应用之前的plan

```
'Project [unresolvedalias(cast((1 > 0) as bigint), None)]
+- OneRowRelation
```

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



## DeduplicateRelations

拿到的plan:

```json
'WithCTE
:- CTERelationDef 0, false
:  +- SubqueryAlias inv
:     +- Aggregate [avg(inv_quantity_on_hand#6) AS mean#2]
:        +- SubqueryAlias spark_catalog.ds1g.inventory
:           +- HiveTableRelation [`ds1g`.`inventory`, org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe, Data Cols: [inv_date_sk#3, inv_item_sk#4, inv_warehouse_sk#5, inv_quantity_on_hand#6], Partition Cols: []]
+- 'Sort ['inv1_mean ASC NULLS FIRST, 'inv2_mean ASC NULLS FIRST], true
   +- 'Project ['inv1.mean AS inv1_mean#0, 'inv2.mean AS inv2_mean#1]
      +- 'Join Inner
         :- SubqueryAlias inv1
         :  +- SubqueryAlias inv
         :     +- CTERelationRef 0, true, [mean#2]
         +- SubqueryAlias inv2
            +- SubqueryAlias inv
               +- CTERelationRef 0, true, [mean#2]
```





def collectConflictPlans(plan: LogicalPlan): Seq[(LogicalPlan, LogicalPlan)]

对于如MultiInstanceRelation, Project, Aggregate等，其输出不直接继承其子，我们可以停止对其进行收集。因为我们总是可以用新计划中的新属性替换所有较低的冲突属性。理论上，我们应该对Generate和Window进行递归收集，但我们将其留给下一批处理，以减少可能的开销，因为这应该是一个极端情况。

需要说明一下MultiInstanceRelation这个trait, 它表示一个查询计划中重复出现的节点，spark不允许这样，因为它打破了表达式ID唯一的保证，表达式ID就是用来区分属性的。

join的左右两边的输出属性集如果有交集，则认为未完成重复解析(duplicateResolved), 也就是join要求左右两边的属性不能冲突， 它们的交集称为冲突属性。

 def dedupRight(left: LogicalPlan, right: LogicalPlan)

对右边节点中冲突的属性采用不同的表达式ID生成一个新的逻辑计划，例如这里right是

```json
         +- SubqueryAlias inv2
            +- SubqueryAlias inv
               +- CTERelationRef 0, true, [mean#2]
```

对他进行递归，当检查CTERelationRef 0, true, [mean#2]时，发现它出现了两次，即是MultiInstanceRelation的一个实例，属性[mean#2]冲突了，那么会要求它重新生成一个实例；而CTERelationRef的重新生成一个新的实例也是非常简单的把属性重新生成ID

```scala
def newInstance(): LogicalPlan = copy(output = output.map(_.newInstance()))
```

产生的结果就是

```json
CTERelationRef 0, true, [mean#9]
```

整个计划树

```json
WithCTE
:- CTERelationDef 0, false
:  +- SubqueryAlias inv
:     +- Aggregate [avg(inv_quantity_on_hand#6) AS mean#2]
:        +- SubqueryAlias spark_catalog.ds1g.inventory
:           +- HiveTableRelation [`ds1g`.`inventory`, org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe, Data Cols: [inv_date_sk#3, inv_item_sk#4, inv_warehouse_sk#5, inv_quantity_on_hand#6], Partition Cols: []]
+- Sort [inv1_mean#0 ASC NULLS FIRST, inv2_mean#1 ASC NULLS FIRST], true
   +- Project [mean#2 AS inv1_mean#0, mean#9 AS inv2_mean#1]
      +- Join Inner
         :- SubqueryAlias inv1
         :  +- SubqueryAlias inv
         :     +- CTERelationRef 0, true, [mean#2]
         +- SubqueryAlias inv2
            +- SubqueryAlias inv
               +- CTERelationRef 0, true, [mean#9]
```







### renewDuplicatedRelations

对逻辑计划中的relations去重，这是一个自底向上递归的函数，参数

1. 已知的去重后的relation
2. 需要去重的plan

返回

(去重后的新plan ,    新plan的所有relations,  plan是否发生变更)

```scala
  private def renewDuplicatedRelations(
      existingRelations: mutable.HashSet[ReferenceEqualPlanWrapper],
      plan: LogicalPlan)
    : (LogicalPlan, mutable.HashSet[ReferenceEqualPlanWrapper], Boolean) = plan match {
    case p: LogicalPlan if p.isStreaming => (plan, mutable.HashSet.empty, false)

    case m: MultiInstanceRelation =>
      val planWrapper = ReferenceEqualPlanWrapper(m)
      if (existingRelations.contains(planWrapper)) {
        //要求重复的relation重新生成表达式ID
        val newNode = m.newInstance()
        newNode.copyTagsFrom(m)
        (newNode, mutable.HashSet.empty, true)
      } else {
        val mWrapper = new mutable.HashSet[ReferenceEqualPlanWrapper]()
        mWrapper.add(planWrapper)
        (m, mWrapper, false)
      }

    case plan: LogicalPlan =>
      val relations = new mutable.HashSet[ReferenceEqualPlanWrapper]()
      var planChanged = false
      val newPlan = if (plan.children.nonEmpty) {
        val newChildren = mutable.ArrayBuffer.empty[LogicalPlan]
        for (c <- plan.children) {
          val (renewed, collected, changed) =
            renewDuplicatedRelations(existingRelations ++ relations, c)
          newChildren += renewed
          relations ++= collected
          if (changed) {
            planChanged = true
          }
        }

        if (planChanged) {
          if (plan.childrenResolved) {
            val planWithNewChildren = plan.withNewChildren(newChildren.toSeq)
            val attrMap = AttributeMap(
              plan
                .children
                .flatMap(_.output).zip(newChildren.flatMap(_.output))
                .filter { case (a1, a2) => a1.exprId != a2.exprId }
            )
            if (attrMap.isEmpty) {
              planWithNewChildren
            } else {
              //这一步对算子树、表达式进行了map
              planWithNewChildren.rewriteAttrs(attrMap)
            }
          } else {
            plan.withNewChildren(newChildren.toSeq)
          }
        } else {
          plan
        }
      } else {
        plan
      }

      val planWithNewSubquery = newPlan.transformExpressions {
        case subquery: SubqueryExpression =>
          val (renewed, collected, changed) = renewDuplicatedRelations(
            existingRelations ++ relations, subquery.plan)
          relations ++= collected
          if (changed) planChanged = true
          subquery.withNewPlan(renewed)
      }
      (planWithNewSubquery, relations, planChanged)
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





