# 自动生成时间戳

## mapper接口必须继承BaseMapper

自动更新时间

mapper接口必须继承BaseMapper

**注意**

这个BaseMapper里定义了一些接口，UserMapper继承它后，相当于新增了一些接口，如果用到了这些接口就比较蛋疼，原因是这些接口会默认把表名全大写，导致找不到表。 

有两种解决办法

1. 就是1.2节里使用@TableName(value = "user")指定表名。
2. 我们重写BaseMapper里的对应方法

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
```

## 实体时间类型为Timestamp

时间字段为Timestamp,且添加注解@TableField

```java
@Data
@TableName(value = "user")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    private String name;

    private Integer age;

    private String email;

    @TableField(fill = FieldFill.INSERT)
    private Timestamp createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Timestamp updateTime;
}

```

## 自定义处理器

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("start insert fill ....");
       //注意，这里createTime是指model类的属性，Timestamp.class是model类的字段类型，值当然也要转成这个类型
        this.strictInsertFill(metaObject, "createTime", Timestamp.class, Timestamp.valueOf(LocalDateTime.now()));
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("start update fill ....");
        this.strictUpdateFill(metaObject, "updateTime", Timestamp.class, Timestamp.valueOf(LocalDateTime.now()));
    }
}

```

## 数据库字段类型也是timestamp

