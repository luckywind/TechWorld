# Spring-框架-ClassUtils类isAssignable方法

工作中遇到一个问题： 

```java
@Data
public class DeriveMetricVO {

  private AtomMetricVO atomMetric;
  private List<GrainVO> grainList;
  private List<TimeRangeVO> timeRangeList;
  private List<ConditionVO> conditionList;
  private List<JoinInfoVO> joinInfoList;
}

@Data
public class DeriveMetricBO {

  private AtomMetricBO atomMetric;
  private List<GrainBO> grainList;
  private List<TimeRangeBO> timeRangeList;
  private List<ConditionBO> conditionList;
  private List<JoinInfoBO> joinInfoList;
}

BeanUtils.copyProperties(deriveMetricVO, deriveMetricBO);
```

BeanUtils拷贝属性时，没有把非List属性且类型不相同的atomMetric属性拷贝到目标属性。

查看源码，发现是ClassUtils.isAssignable方法调用返回了false。

实现调用的是java Native方法，这里不继续展开。

与instanceof（）方法对比：

A instanceof  B ：判断A对象是否为B类或接口的实例或者子类或子接口的对象 
A.isAssignableFrom(B) ：判断A Class对象所代表的类或者接口 是否为B Class对象所代表的类或者接口

PS：简单说就是 instanceof是判断A是否继承B，isAssignableFrom是判断B是否继承A

[原文地址](https://blog.csdn.net/lindai329/article/details/102622743)

bean拷贝的高级工具：mapstruct

