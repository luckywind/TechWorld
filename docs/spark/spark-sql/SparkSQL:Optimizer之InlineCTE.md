# InlineCTE

如果满足以下任一条件，则将CTE定义插入相应的引用中：

1. CTE定义不包含任何非确定性表达式。如果此CTE定义引用了另一个具有非确定性表达式的CTE定义，则仍然可以内联当前CTE定义。
2. 在整个主查询和所有子查询中，CTE定义只被引用一次。此外，由于相关子查询的复杂性，无论上述条件如何，相关子查询中的所有CTE引用都是内联的。

出现在子查询中的CTE定义

```scala
case class InlineCTE(alwaysInline: Boolean = false) extends Rule[LogicalPlan] {

  override def apply(plan: LogicalPlan): LogicalPlan = {
    if (!plan.isInstanceOf[Subquery] && plan.containsPattern(CTE)) {
      val cteMap = mutable.HashMap.empty[Long, (CTERelationDef, Int)]
      //一 构建cteMap
      buildCTEMap(plan, cteMap)
      val notInlined = mutable.ArrayBuffer.empty[CTERelationDef]
      //二内联
      val inlined = inlineCTE(plan, cteMap, notInlined)
      // CTEs in SQL Commands have been inlined by `CTESubstitution` already, so it is safe to add
      // WithCTE as top node here.
      //三如果全部内联了，则返回内联后的plan，否则返回一个WithCTE
      if (notInlined.isEmpty) {
        inlined
      } else {
        WithCTE(inlined, notInlined.toSeq)
      }
    } else {
      plan
    }
  }
```

## buildCTEMap

```scala
  private def buildCTEMap(
      plan: LogicalPlan,
      cteMap: mutable.HashMap[Long, (CTERelationDef, Int)]): Unit = {
    //构造cteMap: key为cteid, value为(CTERelationDef, Int)
    plan match {
      case WithCTE(_, cteDefs) =>
        cteDefs.foreach { cteDef =>
          cteMap.put(cteDef.id, (cteDef, 0))
        }

      case ref: CTERelationRef =>
        //发现一个引用，则更新对应cte的引用计数
        val (cteDef, refCount) = cteMap(ref.cteId)
        cteMap.update(ref.cteId, (cteDef, refCount + 1))

      case _ =>
    }
     //这里递归构建cteMap
    if (plan.containsPattern(CTE)) {
      plan.children.foreach { child =>
        buildCTEMap(child, cteMap)
      }
     //递归查找表达式中的cte，构建cteMap
      plan.expressions.foreach { expr =>
        if (expr.containsAllPatterns(PLAN_EXPRESSION, CTE)) {
          expr.foreach {
            case e: SubqueryExpression =>
              buildCTEMap(e.plan, cteMap)
            case _ =>
          }
        }
      }
    }
  }
```



## inlineCTE

```scala
  private def inlineCTE(
      plan: LogicalPlan,
      cteMap: mutable.HashMap[Long, (CTERelationDef, Int)],
      notInlined: mutable.ArrayBuffer[CTERelationDef]): LogicalPlan = {
    plan match {
      case WithCTE(child, cteDefs) =>
        cteDefs.foreach { cteDef =>
          val (cte, refCount) = cteMap(cteDef.id)
          if (refCount > 0) {
            //cte进行递归内联： cte本身也可能引用了其他cte
            val inlined = cte.copy(child = inlineCTE(cte.child, cteMap, notInlined))
            //cteMap 更新为内联后的值
            cteMap.update(cteDef.id, (inlined, refCount))
            if (!shouldInline(inlined, refCount)) {
              notInlined.append(inlined)
            }
          }
        }
        inlineCTE(child, cteMap, notInlined)

      case ref: CTERelationRef =>
        val (cteDef, refCount) = cteMap(ref.cteId)
        if (shouldInline(cteDef, refCount)) {
           //ref的输出和cteDef的输出相同时才会直接引用
          if (ref.outputSet == cteDef.outputSet) {
            cteDef.child
          } else {
            //否则
            val ctePlan = DeduplicateRelations(
              Join(cteDef.child, cteDef.child, Inner, None, JoinHint(None, None))).children(1)
            val projectList = ref.output.zip(ctePlan.output).map { case (tgtAttr, srcAttr) =>
              Alias(srcAttr, tgtAttr.name)(exprId = tgtAttr.exprId)
            }
            Project(projectList, ctePlan)
          }
        } else {
          ref
        }

      case _ if plan.containsPattern(CTE) =>
        plan
           //递归内联cte子节点：把CTERelationRef替换为CTERelationDef
          .withNewChildren(plan.children.map(child => inlineCTE(child, cteMap, notInlined)))
           //递归内联cte表达式
          .transformExpressionsWithPruning(_.containsAllPatterns(PLAN_EXPRESSION, CTE)) {
            case e: SubqueryExpression =>
              e.withNewPlan(inlineCTE(e.plan, cteMap, notInlined))
          }

      case _ => plan
    }
  }
```



## shouldInline

我们不需要为' deterministic '或' OuterReference '检查所包含的' CTERelationRef ' s， 因为

```scala
  private def shouldInline(cteDef: CTERelationDef, refCount: Int): Boolean = alwaysInline || {
    // We do not need to check enclosed `CTERelationRef`s for `deterministic` or `OuterReference`,
    // because:
    // 1) It is fine to inline a CTE if it references another CTE that is non-deterministic;
    // 2) Any `CTERelationRef` that contains `OuterReference` would have been inlined first.
    refCount == 1 ||
      cteDef.deterministic ||
      cteDef.child.exists(_.expressions.exists(_.isInstanceOf[OuterReference]))
  }

```



# 案例

```scala
WithCTE
:- CTERelationDef 0, false
:  +- Aggregate [avg(inv_quantity_on_hand#6) AS mean#2]
:     +- Relation ds1g.inventory[inv_date_sk#3,inv_item_sk#4,inv_warehouse_sk#5,inv_quantity_on_hand#6] parquet
+- Sort [inv1_mean#0 ASC NULLS FIRST, inv2_mean#1 ASC NULLS FIRST], true
   +- Project [mean#2 AS inv1_mean#0, mean#9 AS inv2_mean#1]
      +- Join Inner
         :- CTERelationRef 0, true, [mean#2]
         +- CTERelationRef 0, true, [mean#9]
```

CTERelationDef 0 被引用了两次， 其类型是我们替换后的

再递归内联Sort树的过程中替换了CTERelationRef，为什么CTERelationRef 0, true, [mean#2]能够直接替换，而CTERelationRef 0, true, [mean#9]没有直接替换呢？ 原因是在内联过程中要求ref和def的outputSet相同：

```scala
    case ref: CTERelationRef =>
         //找到对应的cteDef
        val (cteDef, refCount) = cteMap(ref.cteId)
        if (shouldInline(cteDef, refCount)) {
           //ref的输出和cteDef的输出相同时才会直接引用
          if (ref.outputSet == cteDef.outputSet) {
            cteDef.child
          } else {
            //否则,会把cteDef复制一份做个Join，然后去修改右边，最后只取右边，再做个取别名的project。真骚的操作！
            val ctePlan = DeduplicateRelations(
              Join(cteDef.child, cteDef.child, Inner, None, JoinHint(None, None))).children(1)
            val projectList = ref.output.zip(ctePlan.output).map { case (tgtAttr, srcAttr) =>
              Alias(srcAttr, tgtAttr.name)(exprId = tgtAttr.exprId)
            }
            Project(projectList, ctePlan)
          }
        } else {
          ref
        }
```

CTERelationRef 0, true, [mean#9] 的outputSet是mean#9，类型是DoubleType和cteDef不同。

DeduplicateRelations规则改动的

复制后正常

```json
'Join Inner
:- Aggregate [avg(inv_quantity_on_hand#6) AS mean#2]
:  +- Relation ds1g.inventory[inv_date_sk#3,inv_item_sk#4,inv_warehouse_sk#5,inv_quantity_on_hand#6] parquet
+- Aggregate [avg(inv_quantity_on_hand#6) AS mean#2]
   +- Relation ds1g.inventory[inv_date_sk#3,inv_item_sk#4,inv_warehouse_sk#5,inv_quantity_on_hand#6] parquet
```

