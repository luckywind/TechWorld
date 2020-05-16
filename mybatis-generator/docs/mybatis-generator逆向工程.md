# mybatis逆向工程

周末研究了下mybatis-generator逆向工程，发现真的好用，能省好多代码

使用大体步骤：

1. pom中新增一个mybatis-generator-maven-plugin插件
2. 在resource目录下新建一个mybatis-generator.xml配置文件，这里定义数据库信息，需要逆向的表，Domain类，mapper接口位置和mapper.xml文件位置。主要的工作其实就在这个文件上
3. 执行mybatis-generator插件的generat目标。

是不是很简单？ 

下面实际操作一番：

## 插件

在maven项目的pom.xml文件中添加如下插件

```xml
<plugin>
                <groupId>org.mybatis.generator</groupId>
                <artifactId>mybatis-generator-maven-plugin</artifactId>
                <version>1.3.5</version>
                <dependencies>
                    <dependency>
                        <groupId> mysql</groupId>
                        <artifactId> mysql-connector-java</artifactId>
                        <version> 5.1.49</version>
                    </dependency>
                    <dependency>
                        <groupId>org.mybatis.generator</groupId>
                        <artifactId>mybatis-generator-core</artifactId>
                        <version>1.3.5</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <!--允许移动生成的文件 -->
                    <verbose>true</verbose>
                    <!-- 是否覆盖 -->
                    <overwrite>true</overwrite>
                    <!-- 自动生成的配置 -->
                    <configurationFile>
                        src/main/resources/mybatis-generator.xml</configurationFile>
                </configuration>
            </plugin>
```

## 插件配置文件

在mybatis-generator.xml文件中配置数据库信息，需要逆向的表，Domain类，mapper接口位置和mapper.xml文件位置。好吧，这里的配置挺多的，可以按照下面这个写，相关配置项详细含义，参考官网即可。

`</table>`标签里是需要逆向的表，假设我的数据库里有三张表people,dept和emp三张表，我需要给它们生成相应代码。

建表语句如下：

```sql
create table people(id int not null PRIMARY key,
name VARCHAR(20) not null,
sex char(1) DEFAULT 'm'
);

create table dept(
deptid int,
dname varchar(20),
constraint dept_deptid_pk primary key(deptid)
 );
insert into dept(deptid,dname) values(10,'市场部');
 insert into dept(deptid,dname) values(20,'销售部');

create table emp(
      id int,
      name varchar(20),
      deptid int,
      constraint emp_id_pk primary key(id),
      constraint emp_deptid_fk  foreign key(deptid)
        references dept(deptid)
				 on delete cascade --改变默认级联删除规则
    ); 
```



```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
        PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
        "http://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">
<generatorConfiguration>
    <!--导入属性配置 -->
    <properties resource="db.properties"></properties>
    <context id="MysqlTables" targetRuntime="MyBatis3">
        <commentGenerator>
            <property name="suppressDate" value="true"/>
            <!-- 是否去除自动生成的注释 true：是 ： false:否 -->
            <property name="suppressAllComments" value="true"/>
        </commentGenerator>
        <!--数据库链接地址账号密码-->
        <jdbcConnection driverClass="${jdbc.driverClass}"
                        connectionURL="${jdbc.connectionURL}"
                        userId="${jdbc.userId}"
                        password="${jdbc.password}">
        </jdbcConnection>
        <!--数据库类型和java类型的控制转换-->
        <javaTypeResolver>
            <property name="forceBigDecimals" value="false"/>
        </javaTypeResolver>
        <!--生成Model类存放位置-->
        <javaModelGenerator targetPackage="com.cxf.model.po" targetProject="src/main/java">
            <property name="enableSubPackages" value="true"/>
            <property name="trimStrings" value="true"/>
        </javaModelGenerator>
        <!--生成映射文件存放位置-->
        <sqlMapGenerator targetPackage="com.cxf.mapper" targetProject="src/main/resources">
            <property name="enableSubPackages" value="true"/>
        </sqlMapGenerator>
        <!--生成Dao类存放位置-->
        <!-- 客户端代码，生成易于使用的针对Model对象和XML配置文件 的代码
                type="ANNOTATEDMAPPER",生成Java Model 和基于注解的Mapper对象
                type="MIXEDMAPPER",生成基于注解的Java Model 和相应的Mapper对象
                type="XMLMAPPER",生成SQLMap XML文件和独立的Mapper接口
        -->
        <javaClientGenerator type="XMLMAPPER" targetPackage="com.cxf.mapper" targetProject="src/main/java">
            <property name="enableSubPackages" value="true"/>
        </javaClientGenerator>
        <!--生成对应表及类名-->
        <!-- 数据表进行生成操作 schema:相当于库名; tableName:表名; domainObjectName:对应的DO -->
        <table tableName="people" domainObjectName="People"

               enableCountByExample="false"
               enableUpdateByExample="false"
               enableDeleteByExample="false"
               enableSelectByExample="false"
               selectByExampleQueryId="false">
        </table>
        <table tableName="dept" domainObjectName="Dept"
               enableCountByExample="false" enableUpdateByExample="false"
               enableDeleteByExample="false" enableSelectByExample="false"
               selectByExampleQueryId="false">
        </table>
        <table  tableName="emp" domainObjectName="Emp"
               enableCountByExample="false" enableUpdateByExample="false"
               enableDeleteByExample="false" enableSelectByExample="false"
               selectByExampleQueryId="false">
        </table>

 </context>
</generatorConfiguration>
```

当然这里，我把数据库的连接信息放到db.properties文件里了，这里就不贴出来了。

默认属性情况下：

1. 会生成按照主键增删改查的接口
2. 模糊查询的删改和count

这里，我们在table标签里通过enablexxxByExample属性把模糊查询关闭了。下面看看都生成了哪些代码

## 生成代码

这一步我们在IDEA中执行，需要安装mybatis-generator插件，请自行百度安装。

安装完成后，双击下图所示目标即可。

<img src="https://tva1.sinaimg.cn/large/007S8ZIlgy1geneda8jfnj30jq0lg41m.jpg" alt="image-20200510154206907" style="zoom:50%;" />

如果顺利执行完成，生成的代码结构如下：

<img src="https://tva1.sinaimg.cn/large/007S8ZIlgy1genegghl6rj30jy0lw0v0.jpg" alt="image-20200510154519312" style="zoom:50%;" />



DeptMapper.java

```java
public interface DeptMapper {
    int deleteByPrimaryKey(Integer deptid);

    int insert(Dept record);

    int insertSelective(Dept record);

    Dept selectByPrimaryKey(Integer deptid);

    int updateByPrimaryKeySelective(Dept record);

    int updateByPrimaryKey(Dept record);
}
```

EmpMapper.java

```java
public interface EmpMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Emp record);

    int insertSelective(Emp record);

    Emp selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Emp record);

    int updateByPrimaryKey(Emp record);
}
```

可以看到主键相关的增删改查接口都自动生成了，是不是很方便

[完整项目地址](https://github.com/luckywind/TechWorld)

最后，欢迎关注公众号

![公众号二维码](https://tva1.sinaimg.cn/large/007S8ZIlgy1genl8zwck7j3076076wey.jpg)