# 查看scala编译器生成的代码

## javap

假设Person.scala文件包含如下代码

```scala
class Person (var name: String, var age: Int)
```

```java
scalac Person.scala  #会产生一个Person.class文件
javap Person #会产生java代码
  
Compiled from "Person.scala"
public class Person {
  public java.lang.String name();          //对应getter方法
  public void name_$eq(java.lang.String);  //对应setter方法
  public int age();
  public void age_$eq(int);
  public Person(java.lang.String, int);
}
```

