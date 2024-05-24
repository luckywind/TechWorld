[Spark Sql Expression的规范化操作](https://zhuanlan.zhihu.com/p/442441877)

这个操作返回经过规范化处理后的表达式
规范化处理会在确保输出结果相同的前提下通过一些规则对表达式进重写，这样可以使它们引用同一个实际计算的结果，避免多次计算。



### 规范化结果集中的命名


两种情况：

- 对于AttributeReference引用类的表达式，主要做法是消除名称和可空性带来的差异
- GetStructField复杂类型的表达式，消除名称带来的差异

对于引用类型的表达式，判断是否相同，只需要引用的id(exprId)是相同的就ok，所以这里的处理方法是把name统一置none，来消除差异，比如：

```scala
select b,B,sum(A+b) as ab,sum(B+a) as ba from testdata2 where b>3 group by b

//name#exprId
Expression(b) ---> b#3   
ignoreNamesTypes(b)----none#3

Expression(B) ---> B#3
ignoreNamesTypes(B)----none#3

结过上面转化，b和B都为none#3
```

### 计算类表达式 

- 对于交换和结合运算（Add和Multiply）的子运算，通过“hashCode”来对左右子节点排序
- 对于交换和结合运算（Or和And）的子运算，通过“hashCode”来对左右子节点排序,但要求表达式必须是确定性的
- EqualTo和EqualNullSafe通过“hashCode”来对左右子节点排序。
- 其他比较（greatethan，LessThan）由“hashCode”反转。
- in中的元素按`hashCode'重新排序

## 扩展操作semanticEquals

```scala
  // 两个表达式计算相同的结果时返回true,判断依据是：两个表达式都确定性的，且两个表达式规范化之后相同
  def semanticEquals(other: Expression): Boolean =
    deterministic && other.deterministic && canonicalized == other.canonicalized
```