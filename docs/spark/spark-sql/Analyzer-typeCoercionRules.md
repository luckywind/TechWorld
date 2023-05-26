# 关于 Decimal 类型

先介绍一下 Decimal。

**Decimal 是数据库中的一种数据类型，不属于浮点数类型，可以在定义时划定整数部分以及小数部分的位数**。对于一个 Decimal 类型，scale 表示其小数部分的位数，precision 表示整数部分位数和小数部分位数之和。



一个 Decimal 类型表示为 Decimal(precision, scale)，在 Spark 中，precision 和 scale 的上限都是 **38**。



一个 double 类型可以精确地表示小数点后 **15 位**，有效位数为 **16 位**。



可见，Decimal 类型则可以更加精确地表示，保证数据计算的精度。



例如一个 **Decimal(38, 24)类型**可以精确表示小数点后 23 位，小数点后有效位数为 24 位。而其整数部分还剩下 14 位可以用来表示数据，所以整数部分可以表示的范围是-10^14+1~10^14-1。

# 关于精度和 Overflow

关于**精度的问题**其实我们小学时候就涉及到了，比如求两个小数加减乘除的结果，然后保留小数点后若干有效位，这就是保留精度。

乘法操作我们都很清楚，如果一个 **n 位**小数乘以一个 **m 位**小数，那么结果一定是一个**(n+m)位**小数。

举个例子, 1.11 * 1.11 精确的结果是 1.2321，如果我们只能保留小数点后两位有效位，那么结果就是 1.23。

上面我们提到过，对于 Decimal 类型，由于其整数部分位数是(precision-scale),因此该类型能表示的范围是有限的，**一旦超出这个范围，就会发生 Overflow。而在 Spark 中，如果 Decimal 计算发生了 Overflow，就会默认返回 Null 值**。

举个例子，一个 **Decimal(3,2)类型**代表小数点后用两位表示，整数部分用一位表示，因此该类型可表示的整数部分范围为-9~9。如果我们 CAST(12.32 as Decimal(3,2))，那么将会发生 Overflow。

# TypeCoercion

Batch("Resolution")中的用于强制转换统一参与运算的不同类型的一批规则。

公共类型，通常有两种情况， 我们需要扩大数据类型(例如union , 二元操作)，第一种情况：我们需要给多个类型找到一个公共类型，这种情况，没有精度损失。 第二种情况：我们要接受精度损失的情况下，找到一个公共类型(例如，double和decimal两者没有公共类型，因为double范围更大，但decimal精度更大，这种情况下，我们把decimal强转为double)

规则

```scala
    WidenSetOperationTypes ::
    new CombinedTypeCoercionRule(
      InConversion ::
      PromoteStrings ::
      DecimalPrecision ::// 调整decimal二元运算结果类型精度
      BooleanEquality ::
      FunctionArgumentConversion ::
      ConcatCoercion ::
      MapZipWithCoercion ::
      EltCoercion ::
      CaseWhenCoercion ::
      IfCoercion ::
      StackCoercion ::
      Division ::
      IntegralDivision ::
      ImplicitTypeCasts ::  //转为表达式期望的输入类型
      DateTimeOperations ::
      WindowFrameCoercion ::
      StringLiteralCoercion :: Nil) :: Nil
```

## ImplicitTypeCasts

自动把节点类型转为父节点的输入类型

### canHandleTypeCoercion

用于判断这个Rule是否可以处理这种类型的转换，只要是两个decimal或者一个decimal和一个非decimal且非空的计算都不处理

```scala
    private def canHandleTypeCoercion(leftType: DataType, rightType: DataType): Boolean = {
      (leftType, rightType) match {
        case (_: DecimalType, NullType) => true
        case (NullType, _: DecimalType) => true
        case _ =>
          // 除空值外，只要出现Decimal类型，都由DecimalPrecision处理，即这里不处理，所以返回false
          !leftType.isInstanceOf[DecimalType] && !rightType.isInstanceOf[DecimalType] &&
            leftType != rightType
      }
    }
```

### 处理逻辑

找到最小的公共类型，并进行必要的Cast

```scala
      case b @ BinaryOperator(left, right)
          if canHandleTypeCoercion(left.dataType, right.dataType) =>
       //找到最小的公共类型，并进行必要的Cast
        findTightestCommonType(left.dataType, right.dataType).map { commonType =>
          if (b.inputType.acceptsType(commonType)) {
            // If the expression accepts the tightest common type, cast to that.
            val newLeft = if (left.dataType == commonType) left else Cast(left, commonType)
            val newRight = if (right.dataType == commonType) right else Cast(right, commonType)
            b.withNewChildren(Seq(newLeft, newRight))
          } else {
            // Otherwise, don't do anything with the expression.
            b
          }
        }.getOrElse(b) 
```



## DecimalPrecision

用于调整decimal二元运算结果类型精度，单独一个Rule， 也是转化成Cast

```scala
  override def transform: PartialFunction[Expression, Expression] = {
    decimalAndDecimal()
      .orElse(integralAndDecimalLiteral)
      .orElse(nondecimalAndDecimal(conf.literalPickMinimumPrecision))
  }
```

### def decimalAndDecimal

def decimalAndDecimal(allowPrecisionLoss: Boolean, nullOnOverflow: Boolean)

>  *Decimal precision promotion for +, -, \*, /, %, pmod, and binary comparison.* 

nullOnOverflow： 由配置参数spark.sql.ansi.enabled决定，默认true，当精度溢出时返回空值

#### DecimalType.adjustPrecisionScale

当允许精度损失时，会进行精度调整，其逻辑如下：

首先保证小数位，当小数位超过6位时，为了不损失整数位，只保留至多6位小数； 

但是如果此时整数位剩余空间(距离38)超过了这个精度，则可相应调大小数位。

```scala
precision>38  &&  scale>=0 时：      
      val intDigits = precision - scale //整数位
      // If original scale is less than MINIMUM_ADJUSTED_SCALE, use original scale value; otherwise
      // preserve at least MINIMUM_ADJUSTED_SCALE fractional digits
      val minScaleValue = Math.min(scale, MINIMUM_ADJUSTED_SCALE)
      // The resulting scale is the maximum between what is available without causing a loss of
      // digits for the integer part of the decimal and the minimum guaranteed scale, which is
      // computed above
      val adjustedScale = Math.max(MAX_PRECISION - intDigits, minScaleValue)

      DecimalType(MAX_PRECISION, adjustedScale)
```



#### 结果类型精度计算公式

decimal四则运算结果类型精度计算公式

```java
*   Operation    Result Precision                        Result Scale
 *   ------------------------------------------------------------------------
 *   e1 + e2      max(s1, s2) + max(p1-s1, p2-s2) + 1     max(s1, s2)
 *   e1 - e2      max(s1, s2) + max(p1-s1, p2-s2) + 1     max(s1, s2)
 *   e1 * e2      p1 + p2 + 1                             s1 + s2
 *   e1 / e2      p1 - s1 + s2 + max(6, s1 + p2 + 1)      max(6, s1 + p2 + 1)
 *   e1 % e2      min(p1-s1, p2-s2) + max(s1, s2)         max(s1, s2)
 *   e1 union e2  max(s1, s2) + max(p1-s1, p2-s2)         max(s1, s2)
```

以下是实现代码

```scala
  private[catalyst] def decimalAndDecimal(allowPrecisionLoss: Boolean, nullOnOverflow: Boolean)
    : PartialFunction[Expression, Expression] = {
    // Skip nodes whose children have not been resolved yet
    case e if !e.childrenResolved => e

    // Skip nodes who is already promoted
    case e: BinaryArithmetic if e.left.isInstanceOf[PromotePrecision] => e

    case a @ Add(e1 @ DecimalType.Expression(p1, s1), e2 @ DecimalType.Expression(p2, s2), _) =>
      val resultScale = max(s1, s2)
      val resultType = if (allowPrecisionLoss) {
        //允许精度损失，则进行调整， precision>38,scale>=0则precision=38,scale=max(38-(precision-scale),min(scale,6))
        DecimalType.adjustPrecisionScale(max(p1 - s1, p2 - s2) + resultScale + 1,
          resultScale)
      } else {
        DecimalType.bounded(max(p1 - s1, p2 - s2) + resultScale + 1, resultScale)
      }
      CheckOverflow(
        a.copy(left = promotePrecision(e1, resultType), right = promotePrecision(e2, resultType)),
        resultType, nullOnOverflow)

    case s @ Subtract(e1 @ DecimalType.Expression(p1, s1),
        e2 @ DecimalType.Expression(p2, s2), _) =>
      val resultScale = max(s1, s2)
      val resultType = if (allowPrecisionLoss) {
        DecimalType.adjustPrecisionScale(max(p1 - s1, p2 - s2) + resultScale + 1,
          resultScale)
      } else {
        DecimalType.bounded(max(p1 - s1, p2 - s2) + resultScale + 1, resultScale)
      }
      CheckOverflow(
        s.copy(left = promotePrecision(e1, resultType), right = promotePrecision(e2, resultType)),
        resultType, nullOnOverflow)

    case m @ Multiply(
        e1 @ DecimalType.Expression(p1, s1), e2 @ DecimalType.Expression(p2, s2), _) =>
      val resultType = if (allowPrecisionLoss) {
        DecimalType.adjustPrecisionScale(p1 + p2 + 1, s1 + s2)
      } else {
        DecimalType.bounded(p1 + p2 + 1, s1 + s2)
      }
      val widerType = widerDecimalType(p1, s1, p2, s2)
      //构造一个CheckOverflow表达式，其子节点是promotePrecision表达式
      //其dataType就是widerType
      CheckOverflow( 
        m.copy(left = promotePrecision(e1, widerType), right = promotePrecision(e2, widerType)),
        resultType, nullOnOverflow)

    case d @ Divide(e1 @ DecimalType.Expression(p1, s1), e2 @ DecimalType.Expression(p2, s2), _) =>
      val resultType = if (allowPrecisionLoss) {
        // Precision: p1 - s1 + s2 + max(6, s1 + p2 + 1)
        // Scale: max(6, s1 + p2 + 1)
        val intDig = p1 - s1 + s2
        val scale = max(DecimalType.MINIMUM_ADJUSTED_SCALE, s1 + p2 + 1)
        val prec = intDig + scale
        DecimalType.adjustPrecisionScale(prec, scale)
      } else {
        var intDig = min(DecimalType.MAX_SCALE, p1 - s1 + s2)
        var decDig = min(DecimalType.MAX_SCALE, max(6, s1 + p2 + 1))
        val diff = (intDig + decDig) - DecimalType.MAX_SCALE
        if (diff > 0) {
          decDig -= diff / 2 + 1
          intDig = DecimalType.MAX_SCALE - decDig
        }
        DecimalType.bounded(intDig + decDig, decDig)
      }
      val widerType = widerDecimalType(p1, s1, p2, s2)
      CheckOverflow(
        d.copy(left = promotePrecision(e1, widerType), right = promotePrecision(e2, widerType)),
        resultType, nullOnOverflow)
    }
```

#### CheckOverflow

```scala
case class CheckOverflow(
    child: Expression,
    dataType: DecimalType,
    nullOnOverflow: Boolean) extends UnaryExpression
```

1. 将小数舍入到给定的精度，

2. 并检查小数是否适合所提供的精度，如果不适合

   如果' nullOnOverflow '为' true '，它返回' null ';否则会抛出ArithmeticException异常。

#### 以乘法为例

```scala
  private[catalyst] def decimalAndDecimal(allowPrecisionLoss: Boolean, nullOnOverflow: Boolean)
    : PartialFunction[Expression, Expression] = {
      // .... 
case m @ Multiply(
        e1 @ DecimalType.Expression(p1, s1), e2 @ DecimalType.Expression(p2, s2), _) =>
      val resultType = if (allowPrecisionLoss) {
        DecimalType.adjustPrecisionScale(p1 + p2 + 1, s1 + s2)
      } else {
        DecimalType.bounded(p1 + p2 + 1, s1 + s2)
      }
      //（一）找到更宽松的类型
      val widerType = widerDecimalType(p1, s1, p2, s2)
      //（二）构造一个CheckOverflow表达式，其子节点是promotePrecision表达式，
      // 把表达式dataType Cast为这个宽松的dataType
      CheckOverflow( 
        m.copy(left = promotePrecision(e1, widerType), right = promotePrecision(e2, widerType)),
        resultType, nullOnOverflow)
}

（一）更宽松的类型
  def widerDecimalType(p1: Int, s1: Int, p2: Int, s2: Int): DecimalType = {
    val scale = max(s1, s2)//小数部分取大
    val range = max(p1 - s1, p2 - s2)//整数部分取大
    DecimalType.bounded(range + scale, scale)
  }
（二）PromotePrecision表达式用于标记该表达式已经完成了精度提升
  private def promotePrecision(e: Expression, dataType: DataType): Expression = {
    //精度提升其实就是做一个Cast， 把子类型转为更宽松的公共类型
    PromotePrecision(Cast(e, dataType))
  }
```



#### 除法

```scala
case d @ Divide(e1 @ DecimalType.Expression(p1, s1), e2 @ DecimalType.Expression(p2, s2), _) =>
      val resultType = if (allowPrecisionLoss) {
        // Precision: p1 - s1 + s2 + max(6, s1 + p2 + 1)
        // Scale: max(6, s1 + p2 + 1)
        val intDig = p1 - s1 + s2
        val scale = max(DecimalType.MINIMUM_ADJUSTED_SCALE, s1 + p2 + 1)
        val prec = intDig + scale
        DecimalType.adjustPrecisionScale(prec, scale)
      } else {
        var intDig = min(DecimalType.MAX_SCALE, p1 - s1 + s2)
        var decDig = min(DecimalType.MAX_SCALE, max(6, s1 + p2 + 1))
        val diff = (intDig + decDig) - DecimalType.MAX_SCALE
        if (diff > 0) {
          decDig -= diff / 2 + 1
          intDig = DecimalType.MAX_SCALE - decDig
        }
        DecimalType.bounded(intDig + decDig, decDig)
      }
     //（一）找到更宽松的类型
      val widerType = widerDecimalType(p1, s1, p2, s2)
      //（二）构造一个CheckOverflow表达式，其子节点是promotePrecision表达式，
      // 把表达式dataType Cast为这个宽松的dataType
      CheckOverflow(
        d.copy(left = promotePrecision(e1, widerType), right = promotePrecision(e2, widerType)),
        resultType, nullOnOverflow)
    }

（一）更宽松的类型
  def widerDecimalType(p1: Int, s1: Int, p2: Int, s2: Int): DecimalType = {
    val scale = max(s1, s2)//小数部分取大
    val range = max(p1 - s1, p2 - s2)//整数部分取大
    DecimalType.bounded(range + scale, scale)
  }
（二）PromotePrecision表达式用于标记该表达式已经完成了精度提升
  private def promotePrecision(e: Expression, dataType: DataType): Expression = {
    //精度提升其实就是做一个Cast， 把子类型转为更宽松的公共类型
    PromotePrecision(Cast(e, dataType))
  }
```





### decimal类型的cast

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20230424100655562.png" alt="image-20230424100655562" style="zoom:33%;" />

#### changePrecision

精度改变的核心方法

这里是对当前值的copy直接修改精度，如果成功则返回true,否则返回false

```scala
  private[sql] def changePrecision(
      precision: Int,
      scale: Int,
      roundMode: BigDecimal.RoundingMode.Value): Boolean = {
    // 相同直接返回true
    if (precision == this.precision && scale == this.scale) {
      return true
    }
    DecimalType.checkNegativeScale(scale)
    // decimalVal为空，意味着值用longVal表示的，
    if (decimalVal.eq(null)) {
      if (scale < _scale) {//  精度下降
        // Easier case: we just need to divide our scale down
        val diff = _scale - scale
        val pow10diff = POW_10(diff)
        // % and / always round to 0
        val droppedDigits = longVal % pow10diff
        longVal /= pow10diff  //根据小数位变化修改longVal
        roundMode match {//round to 0
          case ROUND_FLOOR =>
            if (droppedDigits < 0) { //向下取整
              longVal += -1L
            }
          case ROUND_CEILING =>
            if (droppedDigits > 0) {  //向上取整
              longVal += 1L
            }
          case ROUND_HALF_UP => //向最近的邻居取整，等距时向上
            if (math.abs(droppedDigits) * 2 >= pow10diff) {
              longVal += (if (droppedDigits < 0) -1L else 1L)
            }
          case ROUND_HALF_EVEN =>
            val doubled = math.abs(droppedDigits) * 2
            if (doubled > pow10diff || doubled == pow10diff && longVal % 2 != 0) {
              longVal += (if (droppedDigits < 0) -1L else 1L)
            }
          case _ =>
            throw QueryExecutionErrors.unsupportedRoundingMode(roundMode)
        }
      } else if (scale > _scale) {  //精度提升
        // We might be able to multiply longVal by a power of 10 and not overflow, but if not,
        // switch to using a BigDecimal
        val diff = scale - _scale
        val p = POW_10(math.max(MAX_LONG_DIGITS - diff, 0))
        if (diff <= MAX_LONG_DIGITS && longVal > -p && longVal < p) {
          // 18位以下，修改longVal
          longVal *= POW_10(diff)
        } else {
          // 否则，直接使用BigDecimal表示, 注意，此时还未转向新的精度
          decimalVal = BigDecimal(longVal, _scale)
        }
      }
      // In both cases, we will check whether our precision is okay below
    }

    // decimalVal非空，有两种情况
    // 一： 原始值就是BigDecimal
    // 二： 精度提升，导致long溢出，改为了BigDecimal
    if (decimalVal.ne(null)) { 
      // 转向新的精度
      val newVal = decimalVal.setScale(scale, roundMode)
      // precision不可以变小
      if (newVal.precision > precision) {
        return false
      }
      decimalVal = newVal //调整decimalVal
    } else {
      // 如果仍然是18位以下的decimal
      // precision的表示范围要能覆盖longVal
      // We're still using Longs, but we should check whether we match the new precision
      val p = POW_10(math.min(precision, MAX_LONG_DIGITS))
      if (longVal <= -p || longVal >= p) {
        return false
      }
    }
// 最后修改precision和scale  主要分析decimalVal和longVal是如何变化的
    _precision = precision
    _scale = scale
    true
  }

```



# code

## 精度损失(默认允许,即true)

```scala
  val DECIMAL_OPERATIONS_ALLOW_PREC_LOSS =
    buildConf("spark.sql.decimalOperations.allowPrecisionLoss")
      .internal()
      .doc("When true (default), establishing the result type of an arithmetic operation " +
        "happens according to Hive behavior and SQL ANSI 2011 specification, i.e. rounding the " +
        "decimal part of the result if an exact representation is not possible. Otherwise, NULL " +
        "is returned in those cases, as previously.")
      .version("2.3.1")
      .booleanConf
      .createWithDefault(true)
```

允许精度损失后，当无法获取准确结果时，小数部分会被截取



```scala
scala> val sql = """select cast(cast(3 as decimal(38,14)) / cast(9 as decimal(38,14)) as decimal(38,14)) val"""
scala> spark.sql(sql)
res2: org.apache.spark.sql.DataFrame = [val: decimal(38,14)]
scala> spark.sql(sql).show
+----------------+
|             val|
+----------------+
|0.33333300000000|
+----------------+
scala> spark.sql(sql).explain(true)
== Parsed Logical Plan ==
Project [cast((cast(3 as decimal(38,14)) / cast(9 as decimal(38,14))) as decimal(38,14)) AS val#7]
+- OneRowRelation

== Analyzed Logical Plan ==
val: decimal(38,14)
Project [cast(CheckOverflow((promote_precision(cast(cast(3 as decimal(38,14)) as decimal(38,14))) / promote_precision(cast(cast(9 as decimal(38,14)) as decimal(38,14)))), DecimalType(38,6), true) as decimal(38,14)) AS val#7]
+- OneRowRelation

== Optimized Logical Plan ==
Project [0.33333300000000 AS val#7]
+- OneRowRelation

== Physical Plan ==
*(1) Project [0.33333300000000 AS val#7]
+- *(1) Scan OneRowRelation[]











```

### 开启spark.sql.decimalOperations.allowPrecisionLoss=false

```scala
spark.sql("set spark.sql.decimalOperations.allowPrecisionLoss=false")
val sql = """select cast(cast(3 as decimal(38,14)) / cast(9 as decimal(38,14)) as decimal(38,14)) val"""
scala> spark.sql(sql)
res2: org.apache.spark.sql.DataFrame = [val: decimal(38,14)]
scala> spark.sql(sql).show
+----------------+
|             val|
+----------------+
|0.33333333333333|
+----------------+
scala> spark.sql(sql).explain(true)
== Parsed Logical Plan ==
Project [cast((cast(3 as decimal(38,14)) / cast(9 as decimal(38,14))) as decimal(38,14)) AS val#30]
+- OneRowRelation

== Analyzed Logical Plan ==
val: decimal(38,14)
Project [cast(CheckOverflow((promote_precision(cast(cast(3 as decimal(38,14)) as decimal(38,14))) / promote_precision(cast(cast(9 as decimal(38,14)) as decimal(38,14)))), DecimalType(38,18), true) as decimal(38,14)) AS val#30]
+- OneRowRelation

== Optimized Logical Plan ==
Project [0.33333333333333 AS val#30]
+- OneRowRelation

== Physical Plan ==
*(1) Project [0.33333333333333 AS val#30]
+- *(1) Scan OneRowRelation[]
```

