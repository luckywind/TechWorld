# TreePatternBits

位集的一个操作接口，判断TreePattern的id是否在位集里面，从而判断树是否包含某种结构

```scala
trait TreePatternBits {
  protected val treePatternBits: BitSet

  /** 核心接口，就是看枚举的id(即序号)是否在位集里面
   * @param t, the tree pattern enum to be tested.
   * @return true if the bit for `t` is set; false otherwise.
   */
  @inline final def containsPattern(t: TreePattern): Boolean = {
    treePatternBits.get(t.id)
  }

  /**
   * @param patterns, a sequence of tree pattern enums to be tested.
   * @return true if every bit for `patterns` is set; false otherwise.
   */
  final def containsAllPatterns(patterns: TreePattern*): Boolean = {
    val iterator = patterns.iterator
    while (iterator.hasNext) {
      if (!containsPattern(iterator.next)) {
        return false
      }
    }
    true
  }

  /**
   * @param patterns, a sequence of tree pattern enums to be tested.
   * @return true if at least one bit for `patterns` is set; false otherwise.
   */
  final def containsAnyPattern(patterns: TreePattern*): Boolean = {
    val iterator = patterns.iterator
    while (iterator.hasNext) {
      if (containsPattern(iterator.next)) {
        return true
      }
    }
    false
  }
}

```

## BitSet(numBits: Int)

定长bit集,是一个很长的“0/1”序列，他的功能就是存储0或者1。内部维护了一个long数组，初始只有一个long，所以BitSet最小的size是64，当随着存储的元素越来越多，BitSet内部会动态扩充，最终内部是由N个long来存储，这些针对操作都是透明的。

用1位来表示一个数据是否出现过，0为没有出现过，1表示出现过。使用用的时候既可根据某一个是否为0表示，此数是否出现过。

```scala
def cardinality(): Int
Return the number of bits set to true in this BitSet.

def get(index: Int): Boolean
Return the value of the bit with the specified index.

def nextSetBit(fromIndex: Int): Int
Returns the index of the first bit that is set to true that occurs on or after the specified starting index.

def set(index: Int): Unit
Sets the bit at the specified index to true.
```

# TreePattern枚举

这里列了一系列枚举，其关键就是其id序号。 

# SparkSQL如何使用树模式

TreeNode的位集有默认实现，且维护了<font color=red>节点位集</font>和<font color=red>树位集</font>

```scala
abstract class TreeNode[BaseType <: TreeNode[BaseType]] extends Product with TreePatternBits
{protected def getDefaultTreePatternBits: BitSet = {
    val bits: BitSet = new BitSet(TreePattern.maxId)
    // Propagate node pattern bits
    val nodePatternIterator = nodePatterns.iterator
    while (nodePatternIterator.hasNext) {
      bits.set(nodePatternIterator.next().id)
    }
    // Propagate children's pattern bits
    val childIterator = children.iterator
    while (childIterator.hasNext) {
      bits.union(childIterator.next().treePatternBits)
    }
    bits
  }
 //树位集
  override lazy val treePatternBits: BitSet = getDefaultTreePatternBits
 //节点位集
  protected val nodePatterns: Seq[TreePattern] = Seq()
}
```

SparkPlan实际上继承了TreePatternBits

```scala
abstract class QueryPlan[PlanType <: QueryPlan[PlanType]]
  extends TreeNode[PlanType] with SQLConfHelper

abstract class SparkPlan extends QueryPlan[SparkPlan]
```

表达式也继承了

```scala
abstract class Expression extends TreeNode[Expression]
abstract class PlanExpression[T <: QueryPlan[_]] extends Expression 
```

而且PlanExpression重写了nodePatterns属性

```scala
abstract class PlanExpression[T <: QueryPlan[_]] extends Expression {

  // 树的位集
  override lazy val treePatternBits: BitSet = {
    val bits: BitSet = getDefaultTreePatternBits
    // Propagate its query plan's pattern bits
    bits.union(plan.treePatternBits)
    bits
  }

  final override val nodePatterns: Seq[TreePattern] = Seq(PLAN_EXPRESSION) ++ nodePatternsInternal


  // Subclasses can override this function to provide more TreePatterns.
  def nodePatternsInternal(): Seq[TreePattern] = Seq()
  //  ... ...
  def plan: T
}

```

例如：ScalarSubquery声明了自己的节点模式位集为SCALAR_SUBQUERY

```scala
case class ScalarSubquery(
    plan: LogicalPlan,
    outerAttrs: Seq[Expression] = Seq.empty,
    exprId: ExprId = NamedExpression.newExprId,
    joinCond: Seq[Expression] = Seq.empty)
  extends SubqueryExpression(plan, outerAttrs, exprId, joinCond) with Unevaluable {
     ... ...
  final override def nodePatternsInternal: Seq[TreePattern] = Seq(SCALAR_SUBQUERY)
}
```

