[参考](https://mp.weixin.qq.com/s/IgPNwHICi4MmxgMQNluzPA)

[TOC]

# mapper传参方式

## 传递一个参数

Mapper接口方法中只有一个参数，如：

```java
UserModel getByName(String name);//加不加@Param注解都是可以的
```

Mapper xml引用这个name参数：

```
#{任意合法名称}
```

如：`#{name}、#{val}、${x}等等写法都可以引用上面name参数的值`。

## 传递map参数

对应的mapper xml中可以通过`#{map中的key}`可以获取key在map中对应的value的值作为参数

```java
List<Student> selectByMap(Map<String, Object> map);
```

```xml
    <select id="selectByMap" resultType="com.cxf.batishelper.model.Student">
        select
        <include refid="Base_Column_List"></include>
        from student
        WHERE id=#{id} OR name = #{name}
    </select>
```



```java
    public void testSelectByMap()  {
        HashMap<String , Object> mapParam = new HashMap<>();
        mapParam.put("id", 1);
        mapParam.put("name", "xiaoming");
        List<Student> students = mapper.selectByMap(mapParam);
        for (Student student : students) {
            System.out.println(student);
        }
    }
```

## 传递多个参数

也是使用#{参数}获取参数值，高版本mybatis的mapper方法加不加@Param注解都可以(据说低版本不加的话参数名称会变，没有实验)，但是加了的话，xml中会提示

```java
  List<Student> selectByidAndName(@Param("id") Integer id, @Param("name") String name);
```

```xml'
    <select id="selectByidAndName" resultType="com.cxf.batishelper.model.Student">
        select
        <include refid="Base_Column_List"></include>
        from student
        WHERE id=#{id} OR name = #{name}
    </select>
```

## 传递一个collection参数

使用#{collection}均可以访问到集合参数，使用下表获取具体的值,当然如果明确参数是List类型，通过#{list}也可以获取参数，array类似

```java
  List<Student> selectByIdList(@Param("idCollection") Collection<Integer> idCollection);
```

```xml
    <select id="selectByIdList" resultType="com.cxf.batishelper.model.Student">
        select
        <include refid="Base_Column_List"></include>
        from student
        where id in (#{list[0]},#{collection[1]})
    </select>
```

对于集合参数，mybatis会进行一些特殊处理，代码在下面的方法中：

```
org.apache.ibatis.session.defaults.DefaultSqlSession#wrapCollection
```

这个方法的源码如下：

```java
private Object wrapCollection(final Object object) {
    if (object instanceof Collection) {
      StrictMap<Object> map = new StrictMap<>();
      map.put("collection", object);
      if (object instanceof List) {
        map.put("list", object);
      }
      return map;
    } else if (object != null && object.getClass().isArray()) {
      StrictMap<Object> map = new StrictMap<>();
      map.put("array", object);
      return map;
    }
    return object;
  }
```

## 传递ResultHandler

查询的数量比较大的时候，返回一个List集合占用的内存还是比较多的，比如我们想导出很多数据，实际上如果我们通过jdbc的方式，遍历`ResultSet`的`next`方法，一条条处理，而不用将其存到List集合中再取处理。

mybatis中也支持我们这么做，可以使用`ResultHandler`对象，犹如其名，这个接口是用来处理结果的

先看一下其定义：

```java
public interface ResultHandler<T> {

  void handleResult(ResultContext<? extends T> resultContext);

}
```

里面有1个方法，方法的参数是`ResultContext`类型的，这个也是一个接口，看一下源码：

```java
public interface ResultContext<T> {
  T getResultObject();  //获取当前行的结果
  int getResultCount();//获取当前结果到第几行了
  boolean isStopped();//判断是否需要停止遍历结果集
  void stop();//停止遍历结果集
}
```

`ResultContext`接口有一个实现类`org.apache.ibatis.executor.result.DefaultResultContext`，mybatis中默认会使用这个类。

感觉用处不大，没有测试。

源码参见:https://github.com/luckywind/TechWorld/blob/master/code/mybatis/batis-helper/