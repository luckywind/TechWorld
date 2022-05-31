![095d0e882d34260f3d54635457bcf09c](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image/095d0e882d34260f3d54635457bcf09c.jpg)

# 事务传播实战

事务具有四个特性 ——ACID。其中 A 代表原子性，意思是一个事务要么成功（将结果写入数据库），要么失败（不对数据库有任何影响）。这种方式在一个事务单打独斗的时候是一个非常好的做法，但是如果在一个批量任务里（假设包含 1000 个独立的任务），前面的 999 个任务都非常顺利、完美、漂亮、酷毙且成功的执行了，等到执行最后一个的时候，结果这个任务非常悲催、很是不幸的失败了。这时候 Spring 对着前面 999 个成功执行的任务大手一挥说：兄弟们，我们有一个任务失败了，现在全体恢复原状！如果这样的话，那可真是「一顿操作猛如虎，定睛一看原地杵」。

在 Spring 中， 当一个方法调用另外一个方法时，可以让事务采取不同的策略工作，如新建事务或者挂起当前事务等，这便是事务的传播行为。Spring 为我们提供了七种传播行为的策略，通过枚举类 Propagation 定义，源码如下：

```java
package org.springframework.transaction.annotation;

import org.springframework.transaction.TransactionDefinition;

public enum Propagation {

    /**
     * 需要事务，它是默认传播行为，如果当前存在事务，就沿用当前事务，
     * 去否则新建一个事务运行内部方法
     */
    REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),

    /**
     * 支持事务，如果当前存在事务，就沿用当前事务，
     * 如果不存在，则继续采用无事务的方式运行内部方法
     */
    SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS),

    /**
     * 必须使用事务，如果当前没有事务，则会抛出异常，
     * 如果存在当前事务，则沿用当前事务
     */
    MANDATORY(TransactionDefinition.PROPAGATION_MANDATORY),

    /**
     * 无论当前事务是否存在，都会创建新事务运行方法，
     * 这样新事务就可以拥有新的锁和隔离级别等特性，与当前事务相互独立
     */
    REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),

    /**
     * 不支持事务，当前存在事务时，将挂起事务，运行方法
     */
    NOT_SUPPORTED(TransactionDefinition.PROPAGATION_NOT_SUPPORTED),

    /**
     * 不支持事务，如果当前方法存在事务，则抛出异常，否则继续使用无事务机制运行
     */
    NEVER(TransactionDefinition.PROPAGATION_NEVER),

    /** 
     * 在当前方法调用内部方法时，如果内部方法发生异常，
     * 只回滚内部方法执行过的 SQL ，而不回滚当前方法的事务
     */
    NESTED(TransactionDefinition.PROPAGATION_NESTED);

		......

}
```

本文会研究一些常用场景的事务传播机制，文中代码只是突出了关键代码，并不完整，完整代码请参考文章末尾的链接。对照代码食用更好！

准备两张表

```sql
CREATE TABLE `student` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `age` int(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
)

CREATE TABLE `course` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
)
```



## REQUIRED(同生共死)

结论：一旦发生回滚，所有接口都回滚

### 内层失败场景

```java
//StudentServiceImpl.java
@Transactional(propagation = Propagation.REQUIRED)
public class StudentServiceImpl implements StudentService{
    @Autowired
    private CourseService courseService;
    @Resource
    private StudentMapper studentMapper;
    @Override
    public int insert(Student record) {
        int insert = studentMapper.insert(record);
        courseService.deleteByPrimaryKey(1);
        return insert;
    }
}

// CourseServiceImpl.java
@Transactional(propagation = Propagation.REQUIRED)
@Service
public class CourseServiceImpl implements CourseService{
    public int deleteByPrimaryKey(Integer id) {
        int res = 1 / 0;     //内层事务失败
        return courseMapper.deleteByPrimaryKey(id);
    }

```

```java
//创建事务
- Creating new transaction with name [com.cxf.data.service.impl.StudentServiceImpl.insert]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT 
  //给当前事务获取一个数据库连接
- Acquired Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] for JDBC transaction
  //切换连接为手动提交
 - Switching JDBC Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] to manual commit
 - ==>  Preparing: insert into student (`name`, age) values (?, ?) 
 - ==> Parameters: zhangsan(String), null
 - <==    Updates: 1
//加入当前事务   
 - Participating in existing transaction
//加入事务失败，标记当前事务回滚
 - Participating transaction failed - marking existing transaction as rollback-only
 - Setting JDBC transaction [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] rollback-only
//回滚   
 - Initiating transaction rollback
 - Rolling back JDBC transaction on Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d]
//释放连接
 - Releasing JDBC Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] after transaction
```

### 外层失败场景

```java
//StudentServiceImpl.java
@Transactional(propagation = Propagation.REQUIRED)
public class StudentServiceImpl implements StudentService{
    @Autowired
    private CourseService courseService;
    @Resource
    private StudentMapper studentMapper;
    @Override
    public int insert(Student record) {
        int insert = studentMapper.insert(record);
       int res = 1 / 0;     //外层事务失败
        courseService.deleteByPrimaryKey(1);
        return insert;
    }
}

// CourseServiceImpl.java
@Transactional(propagation = Propagation.REQUIRED)
@Service
public class CourseServiceImpl implements CourseService{
    public int deleteByPrimaryKey(Integer id) {
        return courseMapper.deleteByPrimaryKey(id);
    }


```

日志如下

```java
//创建事务
- Creating new transaction with name [com.cxf.data.service.impl.StudentServiceImpl.insert]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
- Acquired Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] for JDBC transaction
- Switching JDBC Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] to manual commit
//回滚  
- Initiating transaction rollback
- Rolling back JDBC transaction on Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d]
- Releasing JDBC Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] after transaction
```



## REQUIRES_NEW(不受外层影响)

当内部方法的传播行为设置为 REQUIRES_NEW 时，内部方法会先将外部方法的事务挂起，然后开启一个新的事务，等内部方法执行完后再提交外层事务。

结论：

1. 内部回滚会导致外部事务也回滚
2. 外层回滚不影响内层的提交

### 内层失败场景

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
@Service
public class CourseServiceImpl implements CourseService{

    public int deleteByPrimaryKey(Integer id) {
        int res = 1 / 0;
        return courseMapper.deleteByPrimaryKey(id);
    }
```

日志输出如下

```java
//创建事务 
- Creating new transaction with name [com.cxf.data.service.impl.StudentServiceImpl.insert]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
 HikariPool-1 - Starting...
 HikariPool-1 - Start completed.
 - Acquired Connection [HikariProxyConnection@1700751834 wrapping com.mysql.cj.jdbc.ConnectionImpl@43b5021c] for JDBC transaction
 - Switching JDBC Connection [HikariProxyConnection@1700751834 wrapping com.mysql.cj.jdbc.ConnectionImpl@43b5021c] to manual commit
 //挂起当前事务，创建新事务
 - Suspending current transaction, creating new transaction with name [com.cxf.data.service.impl.CourseServiceImpl.deleteByPrimaryKey]
 - Acquired Connection [HikariProxyConnection@1888400144 wrapping com.mysql.cj.jdbc.ConnectionImpl@6ebc9573] for JDBC transaction
 - Switching JDBC Connection [HikariProxyConnection@1888400144 wrapping com.mysql.cj.jdbc.ConnectionImpl@6ebc9573] to manual commit
 //回滚
 - Initiating transaction rollback
 - Rolling back JDBC transaction on Connection [HikariProxyConnection@1888400144 wrapping com.mysql.cj.jdbc.ConnectionImpl@6ebc9573]
 - Releasing JDBC Connection [HikariProxyConnection@1888400144 wrapping com.mysql.cj.jdbc.ConnectionImpl@6ebc9573] after transaction
//内部事务回滚完成后重新唤醒挂起的事务  
 - Resuming suspended transaction after completion of inner transaction
//执行回滚
 - Initiating transaction rollback
 - Rolling back JDBC transaction on Connection [HikariProxyConnection@1700751834 wrapping com.mysql.cj.jdbc.ConnectionImpl@43b5021c]
 - Releasing JDBC Connection [HikariProxyConnection@1700751834 wrapping com.mysql.cj.jdbc.ConnectionImpl@43b5021c] after transaction
```

### 外层失败场景

外层失败不影响内层事务的成功提交

```java
//StudentServiceImpl.java
@Transactional(propagation = Propagation.REQUIRED)
public class StudentServiceImpl implements StudentService{
    @Autowired
    private CourseService courseService;
    @Resource
    private StudentMapper studentMapper;
    @Override
    public int insert(Student record) {
        int insert = studentMapper.insert(record);
        courseService.deleteByPrimaryKey(1);
        int res = 1 / 0;     //外层事务失败
        return insert;
    }
}

// CourseServiceImpl.java
@Transactional(propagation = Propagation.REQUIRED)
@Service
public class CourseServiceImpl implements CourseService{
    public int deleteByPrimaryKey(Integer id) {
        return courseMapper.deleteByPrimaryKey(id);
    }
```

日志如下

```java
//创建事务 
- Creating new transaction with name [com.cxf.data.service.impl.StudentServiceImpl.insert]: PROPAGATION_REQUIRES_NEW,ISOLATION_DEFAULT
 - Acquired Connection [HikariProxyConnection@674667952 wrapping com.mysql.cj.jdbc.ConnectionImpl@30893e08] for JDBC transaction
 - Switching JDBC Connection [HikariProxyConnection@674667952 wrapping com.mysql.cj.jdbc.ConnectionImpl@30893e08] to manual commit
 - ==>  Preparing: insert into student (`name`, age) values (?, ?) 
 - ==> Parameters: zhangsan(String), null
 - <==    Updates: 1
   //挂起当前事务，并创建一个新事务
 - Suspending current transaction, creating new transaction with name [com.cxf.data.service.impl.CourseServiceImpl.deleteByPrimaryKey]
 - Acquired Connection [HikariProxyConnection@1857852787 wrapping com.mysql.cj.jdbc.ConnectionImpl@1e977098] for JDBC transaction
 - Switching JDBC Connection [HikariProxyConnection@1857852787 wrapping com.mysql.cj.jdbc.ConnectionImpl@1e977098] to manual commit
Preparing: delete from course where id = ? 
arameters: 1(Integer)
  Updates: 1
    //提交内层事务---  注意，内层成功了，通过查看数据库发现记录被成功删除
 - Initiating transaction commit
 - Committing JDBC transaction on Connection [HikariProxyConnection@1857852787 wrapping com.mysql.cj.jdbc.ConnectionImpl@1e977098]
 - Releasing JDBC Connection [HikariProxyConnection@1857852787 wrapping com.mysql.cj.jdbc.ConnectionImpl@1e977098] after transaction
 - Resuming suspended transaction after completion of inner transaction
   //回滚外层事务 
 - Initiating transaction rollback
 - Rolling back JDBC transaction on Connection [HikariProxyConnection@674667952 wrapping com.mysql.cj.jdbc.ConnectionImpl@30893e08]
 - Releasing JDBC Connection [HikariProxyConnection@674667952 wrapping com.mysql.cj.jdbc.ConnectionImpl@30893e08] after transaction
```

## NESTED(不受内层影响)

当内部方法的传播行为设置为 NESTED 时，内部方法会开启一个新的嵌套事务 

每个 NESTED 事务执行前会将当前操作保存下来，叫做 savepoint （保存点），如果当前 NESTED 事务执行失败，则回滚到之前的保存点，以便之前的执行结果不受当前 NESTED 事务的影响，从而内层方法回滚，则并不影响外层方法的提交。

NESTED 事务在外部事务提交以后自己才会提交。

### 外层为REQUIRED

#### 内层失败场景

结论： 内层回滚不影响外层的执行

```java
//StudentServiceImpl.java
@Transactional(propagation = Propagation.REQUIRED)
public class StudentServiceImpl implements StudentService{

    public int insert(Student record) {
        int insert = studentMapper.insert(record);
        courseService.deleteByPrimaryKey(1);
        return insert;
    }
//CourseServiceImpl.java  
@Transactional(propagation = Propagation.NESTED)//注意事务传播机制的修改
@Service
public class CourseServiceImpl implements CourseService{

    @Override
    public int deleteByPrimaryKey(Integer id) {
        int res = 1 / 0;    //内层失败
        return courseMapper.deleteByPrimaryKey(id);
    }
```

日志如下

```java
//创建事务 
- Creating new transaction with name [com.cxf.data.service.impl.StudentServiceImpl.insert]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
 HikariPool-1 - Starting...
 HikariPool-1 - Start completed.
 - Acquired Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] for JDBC transaction
 - Switching JDBC Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] to manual commit
 - ==>  Preparing: insert into student (`name`, age) values (?, ?) 
 - ==> Parameters: zhangsan(String), null
 - <==    Updates: 1
   //创建嵌套事务, 挂起外层事务
 - Creating nested transaction with name [com.cxf.data.service.impl.CourseServiceImpl.deleteByPrimaryKey]
   //嵌套事务回滚到savepoint
 - Rolling back transaction to savepoint
 - Initiating transaction rollback
 - Rolling back JDBC transaction on Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d]
//继续完成外层事务
 - Releasing JDBC Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] after transaction
```

#### 外层失败场景

外层失败会导致内层回滚

```java
//StudentServiceImpl.java
@Transactional(propagation = Propagation.REQUIRED)
public class StudentServiceImpl implements StudentService{

    public int insert(Student record) {
        int insert = studentMapper.insert(record);
        courseService.deleteByPrimaryKey(1);
      int res = 1 / 0;    //外层失败
        return insert;
    }
//CourseServiceImpl.java  
@Transactional(propagation = Propagation.NESTED)//注意事务传播机制的修改
@Service
public class CourseServiceImpl implements CourseService{

    @Override
    public int deleteByPrimaryKey(Integer id) {
        
        return courseMapper.deleteByPrimaryKey(id);
    }
```

日志如下

```java
//新建事务
- Creating new transaction with name [com.cxf.data.service.impl.StudentServiceImpl.insert]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
 HikariPool-1 - Starting...
 HikariPool-1 - Start completed.
 - Acquired Connection [HikariProxyConnection@247334525 wrapping com.mysql.cj.jdbc.ConnectionImpl@3a4ab7f7] for JDBC transaction
 - Switching JDBC Connection [HikariProxyConnection@247334525 wrapping com.mysql.cj.jdbc.ConnectionImpl@3a4ab7f7] to manual commit
 - ==>  Preparing: insert into student (`name`, age) values (?, ?) 
 - ==> Parameters: zhangsan(String), null
 - <==    Updates: 1
   //创建内嵌事务
 - Creating nested transaction with name [com.cxf.data.service.impl.CourseServiceImpl.deleteByPrimaryKey]
Preparing: delete from course where id = ? 
arameters: 1(Integer)
  Updates: 1
  //释放savepoint  
 - Releasing transaction savepoint
  //内嵌事务回滚
 - Initiating transaction rollback
 - Rolling back JDBC transaction on Connection [HikariProxyConnection@247334525 wrapping com.mysql.cj.jdbc.ConnectionImpl@3a4ab7f7]
 - Releasing JDBC Connection [HikariProxyConnection@247334525 wrapping com.mysql.cj.jdbc.ConnectionImpl@3a4ab7f7] after transaction
```

查看数据库发现外层失败会导致内层回滚。

## MANDATORY

必须在一个已有的事务中执行，否则报错

如果外层NOT_SUPPORTED，而内层是MANDATORY，则会抛异常

```java
Should roll back transaction but cannot - no transaction available
org.springframework.transaction.IllegalTransactionStateException: No existing transaction found for transaction marked with propagation 'mandatory'
```

## NEVEL

必须在一个没有的事务中执行，否则报错

## **SUPPORTS**

如果其他bean调用这个方法时，其他bean声明了事务，则就用这个事务，如果没有声明事务，那就不用事务

外层使用REQUIRED，内层使用SUPPORTS

```java
//创建事务
- Creating new transaction with name [com.cxf.data.service.impl.StudentServiceImpl.insert]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
 - Acquired Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] for JDBC transaction
 - Switching JDBC Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] to manual commit
 - ==>  Preparing: insert into student (`name`, age) values (?, ?) 
 - ==> Parameters: zhangsan(String), null
 - <==    Updates: 1
//加入已有事务   
 - Participating in existing transaction
 - Participating transaction failed - marking existing transaction as rollback-only
 - Setting JDBC transaction [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] rollback-only
 - Initiating transaction rollback
 - Rolling back JDBC transaction on Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d]
 - Releasing JDBC Connection [HikariProxyConnection@2096598149 wrapping com.mysql.cj.jdbc.ConnectionImpl@ebe067d] after transaction
```



## 总结

### 区别

REQUIRES_NEW 最为简单，不管当前有无事务，它都会开启一个全新事务，既不影响外部事务，也不会影响其他内部事务，真正的井水不犯河水，坚定而独立。

REQUIRED 在没有外部事务的情况下，会开启一个事务，不影响其他内部事务；而当存在外部事务的情况下，则会与外部事务还有其他内部事务同命运共生死。有条件会直接上，没条件是会自己创造条件，然后再上。

NESTED 在没有外部事务的情况下与 REQUIRED 效果相同；而当存在外部事务的情况下，则与外部事务生死与共，但与其他内部事务互不相干。要么孑然一身，要么誓死追随主公（外部事务）。

### 传播

1. REQUIRED(**同生共死**)

​      当两个方法的传播机制都是REQUIRED时，如果一旦发生回滚，两个方法都会回滚

2. REQUIRES_NEW(**内层可独立提交**)

​      当内层方法传播机制为REQUIRES_NEW，会开启一个新的事务，并单独提交方法，所以外层方法的回滚并不影响内层方法事务提交

3. NESTED(**外层可单独提交)**

   当外层方法为REQUIRED，内层方法为NESTED时，内层方法开启一个嵌套事务；

   当外层方法回滚时，内层方法也会回滚；反之，如果内层方法回滚，则并不影响外层方法的提交

完整源码地址 https://github.com/luckywind/TechWorld/blob/master/code/boot/boot-transaction

