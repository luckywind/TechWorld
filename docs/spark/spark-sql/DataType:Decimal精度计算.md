# Spark DataType -  Decimal  精度提升与cast实现

本文总结，decimal类型之间二元运算，结果类型的精度算法和数据cast原理

## 1. 案例与问题引入

我们以一个decimal类型的除法案例来引入这个问题，sql语句如下：

```
select cast(3 as decimal(38,14)) / cast(9 as decimal(38,14)) 

```

执行结果如下：

```
scala> val df=spark.sql("select cast(3 as decimal(38,14)) / cast(9 as decimal(38,14))")
df: org.apache.spark.sql.DataFrame = : **decimal(38,6)**]

scala> df.show()
+-------------------------------------------------------+
|(CAST(3 AS DECIMAL(38,14)) / CAST(9 AS DECIMAL(38,14)))|
+-------------------------------------------------------+
|                                               0.333333|
+-------------------------------------------------------+
```

执行计划：

```

scala> df.explain(true)
== Parsed Logical Plan ==
'Project [unresolvedalias((cast(3 as decimal(38,14)) / cast(9 as decimal(38,14))), None)]
+- OneRowRelation

== Analyzed Logical Plan ==
(CAST(3 AS DECIMAL(38,14)) / CAST(9 AS DECIMAL(38,14))): decimal(38,6)
Project [CheckOverflow((promote_precision(cast(cast(3 as decimal(38,14)) as decimal(38,14))) / promote_precision(cast(cast(9 as decimal(38,14)) as decimal(38,14)))), DecimalType(38,6), true) AS (CAST(3 AS DECIMAL(38,14)) / CAST(9 AS DECIMAL(38,14)))#0]
+- OneRowRelation

== Optimized Logical Plan ==
Project [0.333333 AS (CAST(3 AS DECIMAL(38,14)) / CAST(9 AS DECIMAL(38,14)))#0]
+- OneRowRelation

== Physical Plan ==
*(1) Project [0.333333 AS (CAST(3 AS DECIMAL(38,14)) / CAST(9 AS DECIMAL(38,14)))#0]
+- *(1) Scan OneRowRelation[]
```



**问题1**: 为什么结果的小数位会截断，只有6位？
**问题2:**执行计划为什么会出现表达式promote_precision和CheckOverflow，分别是什么作用？
**问题3**: 当类型变化后，数据本身是如何转化的


## 2. 前置知识

### Decimal类型

Decimal 是数据库中的一种数据类型，不属于浮点数类型，可以在定义时划定整数部分以及小数部分的位数。对于一个 Decimal 类型，scale 表示其小数部分的位数，precision 表示整数部分位数和小数部分位数之和。
一个 Decimal 类型表示为 Decimal(precision, scale)，在 Spark 中，precision 和 scale 的上限都是 38。
一个 double 类型可以精确地表示小数点后 15 位，有效位数为 16 位。
可见，Decimal 类型则可以更加精确地表示，保证数据计算的精度。
例如一个 Decimal(38, 24)类型可以精确表示小数点后 23 位（最后一位四舍五入），小数点后有效位数为 24 位。而其整数部分还剩下 14 位可以用来表示数据，所以整数部分可以表示的范围是-10^14+1~10^14-1。

### 精度和Overflow

关于精度的问题，比如求两个小数加减乘除的结果，然后保留小数点后若干有效位，这就是保留精度。乘法操作我们都很清楚，如果一个 n 位小数乘以一个 m 位小数，那么结果一定是一个(n+m)位小数。
举个例子, 1.11 * 1.11 精确的结果是 1.2321，如果我们只能保留小数点后两位有效位，那么结果就是 1.23。
上面我们提到过，对于 Decimal 类型，由于其整数部分位数是(precision-scale),因此该类型能表示的范围是有限的，一旦超出这个范围，就会发生 Overflow。而在 Spark 中，如果 Decimal 计算发生了 Overflow，就会默认返回 Null 值。
举个例子，一个 Decimal(3,2)类型代表小数点后用两位表示，整数部分用一位表示，因此该类型可表示的整数部分范围为-9~9。如果我们 CAST(12.32 as Decimal(3,2))，那么将会发生 Overflow。

## 3. Spark的类型转换规则

         一条 SQL 语句进入 Spark-sql 引擎之后，要经历 Analysis→optimization→生成可执行物理计划的过程。而这个过程就是不同的 Rule 不断作用在 Plan 上面，然后 Plan 随之转化的过程。
**          在 Spark-sql 中有一系列关于类型转换的 Rule，这些 Rule 作用在 Analysis 阶段的 Resolution 子阶段**。
用于强制转换统一参与运算的不同类型的一批规则。
公共类型，通常有两种情况， 我们需要扩大数据类型(例如union , 二元操作)，

* 第一种情况：我们需要给多个类型找到一个公共类型，这种情况，没有精度损失。 
* 第二种情况：我们要接受精度损失的情况下，找到一个公共类型(例如，double和decimal两者没有公共类型，因为double范围更大，但decimal精度更大，这种情况下，我们把decimal强转为double)

规则列表

```
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

其中有一个规则DecimalPrecision专门用于处理Decimal类型之间的二元运算之前的类型统一工作，这里通常发生精度提升。
找到一个wider DataType： left和right都可以转到的类型，且不会发生溢出。

## 4.DecimalPrecision

专门用于处理Decimal类型之间的二元运算之前的类型统一工作，这里通常发生精度提升。
找到一个wider DataType： left和right都可以转到的类型，且不会发生溢出。

### 关于类型提升配置

decimalAndDecimal是类型提升的入口，它有两个参数：

```scala
def decimalAndDecimal(allowPrecisionLoss: Boolean, nullOnOverflow: Boolean)
```

* allowPrecisionLoss:
    是否允许精度损失，由配置参数spark.sql.decimalOperations.allowPrecisionLoss决定，**默认true**
* nullOnOverflow： 
    由配置参数spark.sql.ansi.enabled决定，默认true，当精度溢出时返回空值

**注意**⚠️，Spark默认允许精度损失，似乎可以解释前面的问题了，我们把它关掉，再测试,发现精度没有丢失：

```scala
scala> spark.sql("set spark.sql.decimalOperations.allowPrecisionLoss=false")
val sql = """select cast(cast(3 as decimal(38,14)) / cast(9 as decimal(38,14)) as decimal(38,14)) val"""
scala> spark.sql(sql)
res2: org.apache.spark.sql.DataFrame = [val: decimal(38,14)]
scala> spark.sql(sql).show
+----------------+
|             val|
+----------------+
|0.33333333333333|
+----------------+
```

### resultType精度计算

```
*   Operation    Result Precision                        Result Scale
 *   ------------------------------------------------------------------------
 *   e1 + e2      max(s1, s2) + max(p1-s1, p2-s2) + 1     max(s1, s2)
 *   e1 - e2      max(s1, s2) + max(p1-s1, p2-s2) + 1     max(s1, s2)
 *   e1 * e2      p1 + p2 + 1                             s1 + s2
 *   e1 / e2      p1 - s1 + s2 + max(6, s1 + p2 + 1)      max(6, s1 + p2 + 1)
 *   e1 % e2      min(p1-s1, p2-s2) + max(s1, s2)         max(s1, s2)
 *   e1 union e2  max(s1, s2) + max(p1-s1, p2-s2)         max(s1, s2)
```

结果整数位逻辑：

+： 整数位取最大

-：整数位取最大

*： 整数位相加

/:   左边整数位+右边小数位 ，例如 10/0.1=100

%: 整数位取最小

union: 整数位取最大

注意，这里计算完后，由于Spark不允许精度超过38，如果Precision/scale超过38了，最终结果还会截取。
<font color=red>分两种情况，允许精度损失和不允许精度损失； 在允许精度损失的情况下，会进行adjustPrecisionScale，它是基于HIVE的规则，当precision超过38时减少scale来防止整数部分被截断。</font>

#### 允许精度损失

```scala
        // 先按照公式计算结果精度
        // Precision: p1 - s1 + s2 + max(6, s1 + p2 + 1)
        // Scale: max(6, s1 + p2 + 1)
        val intDig = p1 - s1 + s2
        val scale = max(DecimalType.MINIMUM_ADJUSTED_SCALE, s1 + p2 + 1)
        val prec = intDig + scale
        // 再进行调整
        DecimalType.adjustPrecisionScale(prec, scale)
```

调整逻辑如下

```scala
  private def adjustPrecisionScale(precision: Int, scale: Int): DecimalType = {
    // Assumptions:
    assert(precision >= scale)

    if (precision <= MAX_PRECISION) {
      // 不超过最大精度，无需调整
      DecimalType(precision, scale)
    } else if (scale < 0) {
      // 超过了最大精度38，并且scale<0，我们也只能调整精度到38，这种情况大概率会overflow！！
      DecimalType(MAX_PRECISION, scale)
    } else {
      //  precision>38 且scale>=0
      //  Precision必须调整为MAX_PRECISION.
      val intDigits = precision - scale
      // If original scale is less than MINIMUM_ADJUSTED_SCALE, use original scale value; otherwise
      // preserve at least MINIMUM_ADJUSTED_SCALE fractional digits
      val minScaleValue = Math.min(scale, MINIMUM_ADJUSTED_SCALE)
      // The resulting scale is the maximum between what is available without causing a loss of
      // digits for the integer part of the decimal and the minimum guaranteed scale, which is
      // computed above
      val adjustedScale = Math.max(MAX_PRECISION - intDigits, minScaleValue)

      DecimalType(MAX_PRECISION, adjustedScale)
    }
      
```

总结上面代码，当允许精度损失时，会进行精度调整，

* precision<=38时，无精度损失，无需调整
* precision>38 且 scale<0时，precision调整为38，发生精度损失，这大概率会overflow
* precision>38 且 scale>=0时，precision调整为38，发生精度损失，其逻辑如下：

首先保证小数位，当小数位超过6位时，为了不损失整数位，只保留至多6位小数； 
但是如果此时整数位剩余空间(距离38)超过了这个精度，则可相应调大小数位。
        也就是说Spark在**尽量**不损失整数位的前提下，尽量保留小数位。  为什么说是尽量呢？ 因为precision超过38时，这时Spark也只支持38，这样是有可能会overflow的。


#### 不允许精度损失

这种情况即保证小数位，我们以除法为例，看具体逻辑：

```scala
         // 结果整数位=   左数整数位 + 右数小数位
        var intDig = min(DecimalType.MAX_SCALE, p1 - s1 + s2)
           //结果小数位 s1+p2+1 > s1+s2
        var decDig = min(DecimalType.MAX_SCALE, max(6, s1 + p2 + 1))
        val diff = (intDig + decDig) - DecimalType.MAX_SCALE
        if (diff > 0) {  // 超过了最大精度，必须要调整
          decDig -= diff / 2 + 1 //调少小数位,减去diff的一半
          intDig = DecimalType.MAX_SCALE - decDig
        }
        DecimalType.bounded(intDig + decDig, decDig)
```
### widerType精度计算

```scala
  def widerDecimalType(p1: Int, s1: Int, p2: Int, s2: Int): DecimalType = {
    //小数位取最大
    val scale = max(s1, s2)
    //整数位取最大
    val range = max(p1 - s1, p2 - s2)
    DecimalType.bounded(range + scale, scale)
  }


  private[sql] def bounded(precision: Int, scale: Int): DecimalType = {
    DecimalType(min(precision, MAX_PRECISION), min(scale, MAX_SCALE))
  }
```


widerType的precision调整为precision和38的最小者，widerType的scale调整为scale和 38 的最小者。

再回到前面的案例，按照公式，cast(3 as decimal(38,14)) / cast(9 as decimal(38,14)) 的resultType精度：

允许精度损失的情况下

先按照公式

precision= p1 - s1 + s2 + max(6, s1 + p2 + 1)=91=38-14+14+max(6, 14+38+1)
scale=max(6, s1 + p2 + 1)=53
再按照规则调整后:
precision=38
scale=6
是符合实际的。 
至此， 第一个问题已经有了答案了。

下面开始第二个问题

## 5. CheckOverflow

为何要有CheckOverflow？ 第四节中发现，DecimalPrecision这个规则只是粗暴的按照规则计算了中间结果的类型widerType和最终结果的类型resultType，并没有关心整数部分是否会导致数据溢出。

promotePrecision把操作数cast为widerType，完成计算后，CheckOverflow再把计算结果cast为最终结果resultType，同时校验能否成功cast为resultType，即是否会导致溢出，**默认情况下，如果溢出，将返回null值**。  

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231008123141617.png" alt="image-20231008123141617" style="zoom:50%;" />

其类源码如下：

```scala
case class CheckOverflow(
    child: Expression,
    dataType: DecimalType,
    nullOnOverflow: Boolean) extends UnaryExpression {
  
    override def nullSafeEval(input: Any): Any =
    input.asInstanceOf[Decimal].toPrecision(
      dataType.precision,
      dataType.scale,
      Decimal.ROUND_HALF_UP,//四舍五入
      nullOnOverflow,
      origin.context)
}



  private[sql] def toPrecision(
      precision: Int,
      scale: Int,
      roundMode: BigDecimal.RoundingMode.Value = ROUND_HALF_UP,
      nullOnOverflow: Boolean = true,
      context: String = ""): Decimal = {
    val copy = clone()
    // 这个逻辑其实就是cast的逻辑， 解释详见后面cast核心逻辑
    if (copy.changePrecision(precision, scale, roundMode)) {
      copy
    } else {
      if (nullOnOverflow) {  //cast失败时，返回null
        null
      } else {
        throw QueryExecutionErrors.cannotChangeDecimalPrecisionError(
          this, precision, scale, context)
      }
    }
  }
```

CheckOverflow是一个UnaryExpression, 这里dataType就是上面的resultType,  从签名看，它就是要检查child的类型能否不溢出的情况下转为resultType类型。我们仍以除法为例进行说明

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
     //找到更宽松的类型
      val widerType = widerDecimalType(p1, s1, p2, s2)
      //构造一个CheckOverflow表达式，其子节点是d.copy, 只是用promotePrecision表达式把left和right都Cast为了widerType
      CheckOverflow(
        d.copy(left = promotePrecision(e1, widerType), right = promotePrecision(e2, widerType)),
        resultType, nullOnOverflow)
    }
  
```

#### promotePrecision

再看这个promotePrecision, 它包了一个Cast表达式，正是这个cast把表达式类型转为widerType类型。

```scala
  private def promotePrecision(e: Expression, dataType: DataType): Expression = {
    //精度提升其实就是做一个Cast， 把子类型转为更宽松的公共类型
    PromotePrecision(Cast(e, dataType))
  }
```

至此，第二个问题我们也能解释了，DecimalPrecision规则给Cast表达式包装了两个表达式：promotePrecision(用于精度提升)和CheckOverflow(校验溢出同时把计算结果cast为resultType)


## 6. decimal类型的Cast核心逻辑

分析代码发现，decimal之间的cast，最终是对副本调了Decimal类的changePrecision方法
<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231008112605106.png" alt="image-20231008112605106" style="zoom:50%;" />

cast是四舍五入的改变精度ROUND_HALF_UP

```scala
  def changePrecision(precision: Int, scale: Int): Boolean = {
    changePrecision(precision, scale, ROUND_HALF_UP)
  }
```



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
          case ROUND_HALF_UP => //向最近的邻居取整，等距时向上， 
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

这段代码如果有疑问，请移步[Spark DataType - Decimal  Design](https://yusur-first.quip.com/hUQbATsPhgwQ)

大体逻辑如下：
![image-20231008112629249](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231008112629249.png)

> 注意，图中应该是计算resultType的表示范围，而不是widerType

这个过程中，副本已经完成了类型转换，如果changePresicion最终结果是成功的，toPresicion方法将返回这个副本，完成cast。 
至此，回答了问题3。

# rapids的decimal限制

rapids 0.4.1/0.5 s 只支持64位即precision为18的decimal， 当precision>18时会退回CPU([rapids](https://nvidia.github.io/spark-rapids/docs/supported_ops.html))。 

但是有些情况的回退不容易理解，例如下面的例子，乘法的结果类型是(15,5)，并没有超过18，但是会退回CPU，这是因为Spark插入了promotePrecison把左右两边提升到了(9,3)，这时的乘法结果类型实际是(19,6)，GPU无法处理了；最后Spark是借助CheckOverflow把(19,6)再cast到(15,5)的。

![image-20231008145328181](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231008145328181.png)
