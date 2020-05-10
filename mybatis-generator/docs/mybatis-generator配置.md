# 配置文件

`<jdbcConnection>`用于配置mysql数据库连接,需要路径，驱动，数据库用户名和密码。
`<javaModelGenerator>`生成model实体类文件位置
`<sqlMapGenerator>`生成mapper.xml配置文件位置
`<javaClientGenerator>`生成mapper接口文件位置

`<table>`需要生成的实体类对应的表名，多个实体类复制多份该配置即可,例如

```xml
        <table tableName="people" domainObjectName="People"
               enableCountByExample="false" enableUpdateByExample="false"
               enableDeleteByExample="false" enableSelectByExample="false"
               selectByExampleQueryId="false">
        </table>
```

tableName是必须填的，

可选填[官方文档](http://mybatis.org/generator/configreference/table.html)：
1，`schema`：数据库的schema；如果数据库没有使用schema或者有默认的schema就不用填
2，`catalog`：数据库的catalog；如果数据库没有使用catalog或者有默认的catalog就不用填
3，`alias`：为数据表设置的别名，如果设置了alias，那么生成的所有的SELECT SQL语句中，列名会变成：alias_actualColumnName
4，`domainObjectName`：生成的domain类的名字，如果不设置，直接使用表名作为domain类的名字；可以设置为somepck.domainName，那么会自动把domainName类再放到somepck包里面；
5，`enableInsert`（默认true）：指定是否生成insert语句；
6，`enableSelectByPrimaryKey`（默认true）：指定是否生成按照主键查询对象的语句（就是getById或get）；
7，`enableSelectByExample`（默认true）：MyBatis3Simple为false，指定是否生成动态查询语句；
8，`enableUpdateByPrimaryKey`（默认true）：指定是否生成按照主键修改对象的语句（即update)；
9，`enableDeleteByPrimaryKey`（默认true）：指定是否生成按照主键删除对象的语句（即delete）；
10，`enableDeleteByExample`（默认true）：MyBatis3Simple为false，指定是否生成动态删除语句；
11，`enableCountByExample`（默认true）：MyBatis3Simple为false，指定是否生成动态查询总条数语句（用于分页的总条数查询）；
12，`enableUpdateByExample`（默认true）：MyBatis3Simple为false，指定是否生成动态修改语句（只修改对象中不为空的属性）；
13，`modelType`：参考context元素的defaultModelType，相当于覆盖；
14，`delimitIdentifiers`：参考tableName的解释，注意，默认的delimitIdentifiers是双引号，如果类似MYSQL这样的数据库，使用的是（反引号，那么还需要设置context的beginningDelimiter和endingDelimiter属性）
15，`delimitAllColumns`：设置是否所有生成的SQL中的列名都使用标识符引起来。默认为false，delimitIdentifiers参考context的属性

16,mapperName:生成的mapper类和xml文件名字，默认是xxxMapper和xxxMapper.xml

默认属性情况下：

1. 会生成按照主键增删改查的接口
2. 模糊查询的删改和count