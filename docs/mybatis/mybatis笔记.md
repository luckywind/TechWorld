[中文官方文档](https://mybatis.org/mybatis-3/zh/sqlmap-xml.html)

# xml映射文件

## 结果映射

通过resultType指定结果类型

别名  映射的类型 

date         Date
decimal  BigDecimal
bigdecimal BigDecimal
object 	Object
map 		Map
hashmap HashMap
list 			List
arraylist 	ArrayList
collection Collection
iterator 	Iterator

### 映射到map

```xml
查询到map里
<select id="selectPerson" parameterType="int" resultType="hashmap">
  SELECT * FROM PERSON WHERE ID = #{id}
</select>
```

### 映射到javaBean

```xml
<select id="selectUsers" resultType="com.someapp.model.User">
  select id, username, hashedPassword
  from some_table
  where id = #{id}
</select>
```

当然如果我们给类型定义了别名，那么就只需要写别名就行了

```xml
<!-- mybatis-config.xml 中 -->
<typeAlias type="com.someapp.model.User" alias="User"/>

<!-- SQL 映射 XML 中 -->
<select id="selectUsers" resultType="User">
  select id, username, hashedPassword
  from some_table
  where id = #{id}
</select>
```

### resultMap

`resultMap` 元素是 MyBatis 中最重要最强大的元素。设计思想是，对简单的语句做到零配置，对于复杂一点的语句，只需要描述语句之间的关系就行了。

例如，映射到javaBean可以这么写

1. 先定义一个resultMap
2. 再通过resultMap引用
3. 这种方式也可以解决字段名和类属性名不一致的问题

```xml
<resultMap id="userResultMap" type="User">
  <id property="id" column="user_id" />
  <result property="username" column="user_name"/>
  <result property="password" column="hashed_password"/>
</resultMap>

<select id="selectUsers" resultMap="userResultMap">
  select user_id, user_name, hashed_password
  from some_table
  where id = #{id}
</select>
```

但它远远不止这么简单，现在来看下它的籽元素

#### resultMap子元素

- constructor 用于在实例化类时，注入结果到构造方法中

  - `idArg` - ID 参数；标记出作为 ID 的结果可以帮助提高整体性能
  - `arg` - 将被注入到构造方法的一个普通结果

- id 一个 ID 结果；标记出作为 ID 的结果可以帮助提高整体性能
- result – 注入到字段或 JavaBean 属性的普通结果
- association – 一个复杂类型的关联；许多结果将包装成这种类型
  - 嵌套结果映射 – 关联可以是 resultMap 元素，或是对其它结果映射的引用
- collection – 一个复杂类型的集合
  - 嵌套结果映射 – 集合可以是 resultMap 元素，或是对其它结果映射的引用
- discriminator –鉴别器， 使用结果值来决定使用哪个 resultMap
  - case – 基于某些值的结果映射
     嵌套结果映射 – case 也是一个结果映射，因此具有相同的结构和元素；或者引用其它的结果映射


看一个非常复杂的结果映射: 表示了一篇博客，它由某位作者所写，有很多的博文，每篇博文有零或多条的评论和标签

```xml
<!-- 非常复杂的结果映射 -->
<resultMap id="detailedBlogResultMap" type="Blog">
  <constructor>
    <idArg column="blog_id" javaType="int"/>
  </constructor>
  <result property="title" column="blog_title"/>   //列注入到字段
  <association property="author" javaType="Author">  //包装成一个javaBean
    <id property="id" column="author_id"/>
    <result property="username" column="author_username"/>
    <result property="password" column="author_password"/>
    <result property="email" column="author_email"/>
    <result property="bio" column="author_bio"/>
    <result property="favouriteSection" column="author_favourite_section"/>
  </association>
  <collection property="posts" ofType="Post">
    <id property="id" column="post_id"/>
    <result property="subject" column="post_subject"/>
    <association property="author" javaType="Author"/>  //复杂属性
    <collection property="comments" ofType="Comment">   //复杂属性集合
      <id property="id" column="comment_id"/>
    </collection>
    <collection property="tags" ofType="Tag" >
      <id property="id" column="tag_id"/>
    </collection>
    <discriminator javaType="int" column="draft">
      <case value="1" resultType="DraftPost"/>
    </discriminator>
  </collection>
</resultMap>
```

```sql
<!-- 非常复杂的语句 -->
<select id="selectBlogDetails" resultMap="detailedBlogResultMap">
  select
       B.id as blog_id,
       B.title as blog_title,
       B.author_id as blog_author_id,
       A.id as author_id,
       A.username as author_username,
       A.password as author_password,
       A.email as author_email,
       A.bio as author_bio,
       A.favourite_section as author_favourite_section,
       P.id as post_id,
       P.blog_id as post_blog_id,
       P.author_id as post_author_id,
       P.created_on as post_created_on,
       P.section as post_section,
       P.subject as post_subject,
       P.draft as draft,
       P.body as post_body,
       C.id as comment_id,
       C.post_id as comment_post_id,
       C.name as comment_name,
       C.comment as comment_text,
       T.id as tag_id,
       T.name as tag_name
  from Blog B
       left outer join Author A on B.author_id = A.id
       left outer join Post P on B.id = P.blog_id
       left outer join Comment C on P.id = C.post_id
       left outer join Post_Tag PT on PT.post_id = P.id
       left outer join Tag T on PT.tag_id = T.id
  where B.id = #{id}
</select>
```

下面详细说明上述子元素

#### id和result

```xml
<id property="id" column="post_id"/>
<result property="subject" column="post_subject"/>
```

1. *id* 和 *result* 元素都将一个列的值映射到一个简单数据类型（String, int, double, Date 等）的属性或字段。

2. 这两者之间的唯一不同是，*id* 元素对应的属性会被标记为对象的标识符，在**比较对象实例时**使用。 这样可以提高整体的性能

#### Association

它用来处理“有一个”的类型关系，例如，博客有一个用户

```xml
<association property="author" column="blog_author_id" javaType="Author">
  <id property="id" column="author_id"/>
  <result property="username" column="author_username"/>
</association>
```

关联的不同之处是，你需要告诉 MyBatis 如何加载关联。MyBatis 有两种不同的方式加载关联：

- 嵌套 Select 查询：通过执行另外一个 SQL 映射语句来加载期望的复杂类型。
- 嵌套结果映射：使用嵌套的结果映射来处理连接结果的重复子集。

1. 嵌套select查询

```xml
<resultMap id="blogResult" type="Blog">
  <association property="author" column="author_id" javaType="Author" select="selectAuthor"/>
</resultMap>

<select id="selectAuthor" resultType="Author">
  SELECT * FROM AUTHOR WHERE ID = #{id}
</select>
```

2. 嵌套结果映射

```xml
<select id="selectBlog" resultMap="blogResult">
  select
    B.id            as blog_id,
    B.title         as blog_title,
    B.author_id     as blog_author_id,
    A.id            as author_id,
    A.username      as author_username,
    A.password      as author_password,
    A.email         as author_email,
    A.bio           as author_bio
  from Blog B left outer join Author A on B.author_id = A.id
  where B.id = #{id}
</select>
```



```xml
<resultMap id="blogResult" type="Blog">
  <id property="id" column="blog_id" />
  <result property="title" column="blog_title"/>
  <association property="author" column="blog_author_id" javaType="Author" resultMap="authorResult"/>
</resultMap>

<resultMap id="authorResult" type="Author">
  <id property="id" column="author_id"/>
  <result property="username" column="author_username"/>
  <result property="password" column="author_password"/>
  <result property="email" column="author_email"/>
  <result property="bio" column="author_bio"/>
</resultMap>
```

在上面的例子中，你可以看到，博客（Blog）作者（author）的关联元素委托名为 “authorResult” 的结果映射来加载作者对象的实例。authorResult被重用了。

当然，如果觉得没有必要有这个authorResult, 或者不想重用它，也可以在关联里面写

```xml
<resultMap id="blogResult" type="Blog">
  <id property="id" column="blog_id" />
  <result property="title" column="blog_title"/>
  <association property="author" javaType="Author">
    <id property="id" column="author_id"/>
    <result property="username" column="author_username"/>
    <result property="password" column="author_password"/>
    <result property="email" column="author_email"/>
    <result property="bio" column="author_bio"/>
  </association>
</resultMap>
```

#### collection

语法和association很像，但它是表示“有很多”这种关系

```xml
<collection property="posts" ofType="domain.blog.Post">
  <id property="id" column="post_id"/>
  <result property="subject" column="post_subject"/>
  <result property="body" column="post_body"/>
</collection>
```

假设博客有一个List类型的评论属性，映射嵌套结果集合到一个 List 中，可以使用集合元素。 和关联元素一样，我们可以使用嵌套 Select 查询，或基于连接的嵌套结果映射集合。

1. 使用select 

可以看到collection新增了一个ofType属性

```xml
<select id="selectBlog" resultMap="blogResult">
  SELECT * FROM BLOG WHERE ID = #{id}
</select>

<resultMap id="blogResult" type="Blog">
  <collection property="posts" javaType="ArrayList" column="id" ofType="Post" select="selectPostsForBlog"/>
</resultMap>

<select id="selectPostsForBlog" resultType="Post">
  SELECT * FROM POST WHERE BLOG_ID = #{id}
</select>
```

2. 使用嵌套resultMap

```xml
<select id="selectBlog" resultMap="blogResult">
  select
  B.id as blog_id,
  B.title as blog_title,
  B.author_id as blog_author_id,
  P.id as post_id,
  P.subject as post_subject,
  P.body as post_body,
  from Blog B
  left outer join Post P on B.id = P.blog_id
  where B.id = #{id}
</select>

<resultMap id="blogResult" type="Blog">
  <id property="id" column="blog_id" />
  <result property="title" column="blog_title"/>
  <collection property="posts" ofType="Post">
    <id property="id" column="post_id"/>
    <result property="subject" column="post_subject"/>
    <result property="body" column="post_body"/>
  </collection>
</resultMap>
```







#### resultMap的属性

| 属性          | 描述                                                         |
| :------------ | :----------------------------------------------------------- |
| `id`          | 当前命名空间中的一个唯一标识，用于标识一个结果映射。         |
| `type`        | 类的完全限定名, 或者一个类型别名（关于内置的类型别名，可以参考上面的表格）。 |
| `autoMapping` | 如果设置这个属性，MyBatis 将会为本结果映射开启或者关闭自动映射。 这个属性会覆盖全局的属性 autoMappingBehavior。默认值：未设置（unset）。 |





## sql代码段

1. 通过include标签的refid引用代码段
2. 通过property子标签传参数

```xml
代码段可以使用变量占位
<sql id="userColumns"> ${alias}.id,${alias}.username,${alias}.password </sql>

然后include标签可以给变量传值
<select id="selectUsers" resultType="map">
  select
    <include refid="userColumns"><property name="alias" value="t1"/></include>,
    <include refid="userColumns"><property name="alias" value="t2"/></include>
  from some_table t1
    cross join some_table t2
</select>
```

更复杂点的:

1. include标签可以嵌套使用
2. 外层参数可以传给内层include

```xml
<sql id="sometable">
  ${prefix}Table
</sql>

<sql id="someinclude">
  from
    <include refid="${include_target}"/>
</sql>

<select id="select" resultType="map">
  select
    field1, field2, field3
  <include refid="someinclude">
    <property name="prefix" value="Some"/>
    <property name="include_target" value="sometable"/>
  </include>
</select>
```

## 属性自动填充

复杂类型参数，其属性可以自动填充

```xml
<insert id="insertUser" parameterType="User">
  insert into users (id, username, password)
  values (#{id}, #{username}, #{password})
</insert>
```

## 字段类型处理

默认情况下，大部分都无需干预，mybatis会根据对象的属性类型来确认参数的类型。 但是如果参数是hashmap, 则可能需要我们显示指定参数类型

```xml
#{property,javaType=int,jdbcType=NUMERIC}
甚至可以指定自定义类型
#{age,javaType=int,jdbcType=NUMERIC,typeHandler=MyTypeHandler}
```

## 字符串替换

注意传参时， #和$得区别！据说面试常问

`#`用于构造预编译语句占位的，

`$`不转译的原生字符串替换

默认情况下，使用 `#{}` 参数语法时，MyBatis 会创建 `PreparedStatement` 参数占位符，并通过占位符安全地设置参数（就像使用 ? 一样）。 这样做更安全，更迅速，通常也是首选做法，不过有时你就是想直接在 SQL 语句中直接插入一个不转义的字符串。 比如 ORDER BY 子句，这时候你可以：

```xml
ORDER BY ${columnName}
```

这样，MyBatis 就不会修改或转义该字符串了。

# 动态sql

动态sql的支持依赖于以下四个元素

- if
- choose (when, otherwise)
- trim (where, set)
- foreach

## if

通常用于拼接where 条件

```xml
<select id="findActiveBlogWithTitleLike"
     resultType="Blog">
  SELECT * FROM BLOG
  WHERE state = ‘ACTIVE’
  <if test="title != null">
    AND title like #{title}
  </if>
</select>
```

## choose (when, otherwise)

有时候，我们不想使用所有的条件，而只是想从多个条件中选择一个使用。针对这种情况，MyBatis 提供了 choose 元素，它有点像 Java 中的 **switch 语句。**

还是上面的例子，但是策略变为：传入了 “title” 就按 “title” 查找，传入了 “author” 就按 “author” 查找的情形。若两者都没有传入，就返回标记为 featured 的 BLOG

```xml
<select id="findActiveBlogLike"
     resultType="Blog">
  SELECT * FROM BLOG WHERE state = ‘ACTIVE’
  <choose>
    <when test="title != null">
      AND title like #{title}
    </when>
    <when test="author != null and author.name != null">
      AND author_name like #{author.name}
    </when>
    <otherwise>
      AND featured = 1
    </otherwise>
  </choose>
</select>
```

## trim、where、set

1. *where* 元素只会在子元素有返回内容的情况下才插入 “WHERE” 子句
2. 若子句的开头为 “AND” 或 “OR”，*where* 元素也会将它们去除。

```xml
<select id="findActiveBlogLike"
     resultType="Blog">
  SELECT * FROM BLOG
  <where>
    <if test="state != null">
         state = #{state}
    </if>
    <if test="title != null">
        AND title like #{title}
    </if>
    <if test="author != null and author.name != null">
        AND author_name like #{author.name}
    </if>
  </where>
</select>
```

如果 *where* 元素与你期望的不太一样，你也可以通过自定义 trim 元素来定制 *where* 元素的功能。比如，和 *where* 元素等价的自定义 trim 元素为：

```xml
<trim prefix="WHERE" prefixOverrides="AND |OR ">
  ...
</trim>
我感觉是给我们的条件输入加一个prefix，也就是WHERE，如果有多个if，从第二个if块开始，其prefix会使用prefixOverrides替换
```

*prefixOverrides* 属性会忽略通过管道符分隔的文本序列（注意此例中的空格是必要的）。上述例子会移除所有 *prefixOverrides* 属性中指定的内容，并且插入 *prefix* 属性中指定的内容。



### set

用于动态更新语句的类似解决方案叫做 *set*。*set* 元素可以用于动态包含需要更新的列，忽略其它不更新的列。比如：

***set* 元素会动态地在行首插入 SET 关键字，并会删掉额外的逗号**

```xml
<update id="updateAuthorIfNecessary">
  update Author
    <set>
      <if test="username != null">username=#{username},</if>
      <if test="password != null">password=#{password},</if>
      <if test="email != null">email=#{email},</if>
      <if test="bio != null">bio=#{bio}</if>
    </set>
  where id=#{id}
</update>
```

## foreach

```xml
<select id="selectPostIn" resultType="domain.blog.Post">
  SELECT *
  FROM POST P
  WHERE ID in
  <foreach item="item" index="index" collection="list"
      open="(" separator="," close=")">
        #{item}
  </foreach>
</select>
```

## cdata

- XML文件会在解析XML时将5种特殊字符进行转义，分别是&， <， >， “， ‘， 我们不希望语法被转义，就需要进行特别处理。
- 有两种解决方法：其一，使用``标签来包含字符。其二，使用XML转义序列来表示这些字符。

```xml
<select id="userInfo" parameterType="java.util.HashMap" resultMap="user">   
     SELECT id,newTitle, newsDay FROM newsTable WHERE 1=1  
     AND  newsday <![CDATA[>=]]> #{startTime}
     AND newsday <![CDATA[<= ]]>#{endTime}  
  ]]>  
 </select>  
```

在CDATA内部的所有内容都会被解析器忽略，保持原貌。所以在Mybatis配置文件中，要尽量缩小 ``
的作用范围

mybatisHelper可以使用cd快捷生成cdata

其二就是使用xml的转译序列

    特殊字符     转义序列
    <           &lt;
    >           &gt;
    &           &amp;
    "           &quot;
    '           &apos;
# insert和insertSelective的区别

updateByPrimaryKey对你注入的字段全部更新（不判断是否为Null）

updateByPrimaryKeySelective会对字段进行判断再更新(如果为Null就忽略更新)

区别了这两点就很容易根据业务来选择服务层的调用了！

详细可以查看generator生成的源代码！

insert和insertSelective和上面类似，加入是insert就把所有值插入,但是要注意加入数据库字段有default,default是不会起作用的

insertSelective不会忽略default

就是插入和选择性插入的区别

# 执行自定义sql

## 方法一

[使用xml](https://blog.csdn.net/asd8510678/article/details/53519189)

# 没有主键的表如何查？

```java
List<DorisConfig> selectByAll(DorisConfig dorisConfig);
```

还可以使用NamedSql

## selectByAll

其实是根据提供的对象的所有非空字段去查。 有个容易忽视的问题，就是时间戳字段，不要随便设置，否则查不到结果。

# 打印sql

**方法一 properties：** 在application.properties配置文件中增加如下配置

```
logging.level.com.marvin.demo.dao=debug

复制代码
```

【注】：logging.level.com后面的路径指的是Mybatis对应的方法接口所在的包,一般是*.dao所在的包，而并不是mapper.xml所在的包。 debug代表的是日志级别。

**方法二 yml：** 在application.yml配置文件中增加如下配置

```
logging:
  level:
     com.marvin.demo.dao : debug
```

# 分页

[三种分页方式](https://juejin.im/entry/59127ad4da2f6000536f64d8)

## 查出所有数据后手动分页

缺点：数据库查询并返回所有的数据，而我们需要的只是极少数符合要求的数据。当数据量少时，还可以接受。当数据库数据量过大时，每次查询对数据库和程序的性能都会产生极大的影响。

```java
@Override
    public List<Student> queryStudentsByArray(int currPage, int pageSize) {
        List<Student> students = studentMapper.queryStudentsByArray();//查出所有数据
//        从第几条数据开始
        int firstIndex = (currPage - 1) * pageSize;
//        到第几条数据结束
        int lastIndex = currPage * pageSize;
        return students.subList(firstIndex, lastIndex);
    }

//controller
 @ResponseBody
    @RequestMapping("/student/array/{currPage}/{pageSize}")
    public List<Student> getStudentByArray(@PathVariable("currPage") int currPage, @PathVariable("pageSize") int pageSize) {
        List<Student> student = StuServiceIml.queryStudentsByArray(currPage, pageSize);
        return student;
 }

```

## 借助sql语句

sql分页语句如下：`select * from table limit index, pageSize;`

```java
    public List<Student> queryStudentsBySql(int currPage, int pageSize) {
        Map<String, Object> data = new HashedMap();
        data.put("currIndex", (currPage-1)*pageSize);
        data.put("pageSize", pageSize);
        return studentMapper.queryStudentsBySql(data);
    }
```



```xml
 <select id="queryStudentsBySql" parameterType="map" resultMap="studentmapper">
        select * from student limit #{currIndex} , #{pageSize}
</select>
```

缺点：虽然这里实现了按需查找，每次检索得到的是指定的数据。但是每次在分页的时候都需要去编写limit语句，很冗余。而且不方便统一管理，维护性较差。所以我们希望能够有一种更方便的分页实现。

## 拦截器分页

​        利用拦截器达到分页的效果。**利用拦截器达到分页的效果。自定义拦截器实现了拦截所有以ByPage结尾的查询语句，并且利用获取到的分页相关参数统一在sql语句后面加上limit分页的相关语句，一劳永逸。不再需要在每个语句中单独去配置分页相关的参数了。。**，并且利用获取到的分页相关参数统一在sql语句后面加上limit分页的相关语句，一劳永逸。不再需要在每个语句中单独去配置分页相关的参数了。。

​		首先我们看一下拦截器的具体实现，在这里我们需要拦截所有以ByPage结尾的所有查询语句，因此要使用该拦截器实现分页功能，那么再定义名称的时候需要满足它拦截的规则（以ByPage结尾）

### 拦截器

```java
package com.cbg.interceptor;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

/**
 * Created by chenboge on 2017/5/7.
 * <p>
 * Email:baigegechen@gmail.com
 * <p>
 * description:
 */

/**
 * @Intercepts 说明是一个拦截器
 * @Signature 拦截器的签名
 * type 拦截的类型 四大对象之一( Executor,ResultSetHandler,ParameterHandler,StatementHandler)
 * method 拦截的方法
 * args 参数
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class MyPageInterceptor implements Interceptor {

//每页显示的条目数
    private int pageSize;
//当前现实的页数
    private int currPage;

    private String dbType;


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //获取StatementHandler，默认是RoutingStatementHandler
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        //获取statementHandler包装类
        MetaObject MetaObjectHandler = SystemMetaObject.forObject(statementHandler);

        //分离代理对象链
        while (MetaObjectHandler.hasGetter("h")) {
            Object obj = MetaObjectHandler.getValue("h");
            MetaObjectHandler = SystemMetaObject.forObject(obj);
        }

        while (MetaObjectHandler.hasGetter("target")) {
            Object obj = MetaObjectHandler.getValue("target");
            MetaObjectHandler = SystemMetaObject.forObject(obj);
        }

        //获取连接对象
        //Connection connection = (Connection) invocation.getArgs()[0];


        //object.getValue("delegate");  获取StatementHandler的实现类

        //获取查询接口映射的相关信息
        MappedStatement mappedStatement = (MappedStatement) MetaObjectHandler.getValue("delegate.mappedStatement");
        String mapId = mappedStatement.getId();

        //statementHandler.getBoundSql().getParameterObject();

        //拦截以.ByPage结尾的请求，分页功能的统一实现
        if (mapId.matches(".+ByPage$")) {
            //获取进行数据库操作时管理参数的handler
            ParameterHandler parameterHandler = (ParameterHandler) MetaObjectHandler.getValue("delegate.parameterHandler");
            //获取请求时的参数
            Map<String, Object> paraObject = (Map<String, Object>) parameterHandler.getParameterObject();
            //也可以这样获取
            //paraObject = (Map<String, Object>) statementHandler.getBoundSql().getParameterObject();

            //参数名称和在service中设置到map中的名称一致
            currPage = (int) paraObject.get("currPage");
            pageSize = (int) paraObject.get("pageSize");

            String sql = (String) MetaObjectHandler.getValue("delegate.boundSql.sql");
            //也可以通过statementHandler直接获取
            //sql = statementHandler.getBoundSql().getSql();

            //构建分页功能的sql语句
            String limitSql;
            sql = sql.trim();
            limitSql = sql + " limit " + (currPage - 1) * pageSize + "," + pageSize;

            //将构建完成的分页sql语句赋值个体'delegate.boundSql.sql'，偷天换日
            MetaObjectHandler.setValue("delegate.boundSql.sql", limitSql);
        }

        return invocation.proceed();
    }


    //获取代理对象
    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    //设置代理对象的参数
    @Override
    public void setProperties(Properties properties) {
//如果项目中分页的pageSize是统一的，也可以在这里统一配置和获取，这样就不用每次请求都传递pageSize参数了。参数是在配置拦截器时配置的。
        String limit1 = properties.getProperty("limit", "10");
        this.pageSize = Integer.valueOf(limit1);
        this.dbType = properties.getProperty("dbType", "mysql");
    }
}
```

上面即是拦截器功能的实现，在intercept方法中获取到select标签和sql语句的相关信息，拦截所有以ByPage结尾的select查询，并且统一在查询语句后面添加limit分页的相关语句，统一实现分页功能。

### 注册拦截器

编写好拦截器后，需要注册到项目中，才能发挥它的作用。在mybatis的配置文件中，添加如下代码：

```xml
  <plugins>
        <plugin interceptor="com.cbg.interceptor.MyPageInterceptor">
            <property name="limit" value="10"/>
            <property name="dbType" value="mysql"/>
        </plugin>
    </plugins>
```

如上所示，还能在里面配置一些属性，在拦截器的setProperties方法中可以获取配置好的属性值。如项目分页的pageSize参数的值固定，我们就可以配置在这里了，以后就不需要每次传入pageSize了。

### 使用

首先还是添加dao层的方法和xml文件的sql语句配置，注意项目中拦截的是以ByPage结尾的请求，所以在这里，我们的方法名称也以此结尾：

```xml
方法
List<Student> queryStudentsByPage(Map<String,Object> data);

xml文件的select语句
    <select id="queryStudentsByPage" parameterType="map" resultMap="studentmapper">
        select * from student
    </select>
```

service层代码

```java
方法：
List<Student> queryStudentsByPage(int currPage,int pageSize);

实现：
 @Override
    public List<Student> queryStudentsByPage(int currPage, int pageSize) {
        Map<String, Object> data = new HashedMap();
        data.put("currPage", currPage);
        data.put("pageSize", pageSize);
        return studentMapper.queryStudentsByPage(data);
    }
```

这里我们虽然传入了currPage和pageSize两个参数，但是在sql的xml文件中并没有使用，直接在拦截器中获取到统一使用。

通过拦截器的实现方式是最简便的，只需一次编写，所有的分页方法共同使用，还可以避免多次配置时的出错机率，需要修改时也只需要修改这一个文件，一劳永逸。当然这也是我们推荐的使用方式。

### springboot中配置插件

[springboot自定义mybatis插件](https://www.iteye.com/blog/412887952-qq-com-2409334)

