Mybatis-generator官方不支持生成一对一关系的类，好在[大牛](https://blog.csdn.net/bandaotixiruiqiang/article/details/72478361)写了一些插件支持

# 逆向工程支持一对多关系

## 假设数据库中有以下表

部门对应多个员工：

```sql
-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for employee
-- ----------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `dpt_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_employee` (`dpt_id`),
  CONSTRAINT `fk_employee` FOREIGN KEY (`dpt_id`) REFERENCES `department` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
```



## 插件配置

原来pom.xml文件中逆向工程插件，需要更新为一个升级后的插件mybatis-generator.jar，我们把它放到项目根目录下的lib目录，然后修改pom.xml

```xml
  <dependency>
                        <groupId>org.mybatis.generator</groupId>
                        <artifactId>mybatis-generator-core</artifactId>
                        <version>1.3.5</version>
                        <!--                        如果要支持一对一，一对多，要添加下面两行两行-->
                        <scope>system</scope>
                        <systemPath>${basedir}/lib/mybatis-generator.jar</systemPath>
                    </dependency>
```

这里比多了以下两行，表示加载本地文件依赖

```xml
<scope>system</scope>
                        <systemPath>${basedir}/lib/mybatis-generator.jar</systemPath>
```



完整的插件配置如下：

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
                        <!--                        如果要支持一对一，一对多，要添加下面两行两行-->
                        <scope>system</scope>
                        <systemPath>${basedir}/lib/mybatis-generator.jar</systemPath>
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





## 逆向工程配置文件

在context标签下新增插件

```xml
 <!-- 生成一对一配置 -->
        <plugin type="cc.bandaotixi.plugins.OneToOnePlugin"></plugin>
        <!-- 生成一对多配置 -->
        <plugin type="cc.bandaotixi.plugins.OneToManyPlugin"></plugin>
<!--        批量插入和批量更新-->
        <plugin type="cc.bandaotixi.plugins.BatchInsertPlugin"></plugin>
        <plugin type="cc.bandaotixi.plugins.BatchUpdatePlugin"></plugin>
<!--注释生成策略-->
        <commentGenerator type="cc.bandaotixi.plugins.BDTCommentGenerator">
            <property name="javaFileEncoding" value="UTF-8"/>
            <property name="suppressDate" value="true"/>
            <property name="suppressAllComments" value="false" />
        </commentGenerator>
```

其余配置正常配置，完整配置文件如下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
        PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
        "http://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">
<generatorConfiguration>
    <!--导入属性配置 -->
    <properties resource="db.properties"></properties>
    <context id="MysqlTables" targetRuntime="MyBatis3">
        <!-- 生成一对一配置 -->
        <plugin type="cc.bandaotixi.plugins.OneToOnePlugin"></plugin>
        <!-- 生成一对多配置 -->
        <plugin type="cc.bandaotixi.plugins.OneToManyPlugin"></plugin>
<!--        批量插入和批量更新-->
        <plugin type="cc.bandaotixi.plugins.BatchInsertPlugin"></plugin>
        <plugin type="cc.bandaotixi.plugins.BatchUpdatePlugin"></plugin>

        <commentGenerator type="cc.bandaotixi.plugins.BDTCommentGenerator">
            <property name="javaFileEncoding" value="UTF-8"/>
            <property name="suppressDate" value="true"/>
            <property name="suppressAllComments" value="false" />
        </commentGenerator>

<!--        <commentGenerator>
            <property name="suppressDate" value="true"/>
            &lt;!&ndash; 是否去除自动生成的注释 true：是 ： false:否 &ndash;&gt;
            <property name="suppressAllComments" value="true"/>
        </commentGenerator>-->
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
        <javaModelGenerator targetPackage="com.cxf.model" targetProject="src/main/java">
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
        <!-- 指定数据库表，要生成哪些表，就写哪些表，要和数据库中对应，不能写错！ -->
        <!-- 数据表进行生成操作 schema:相当于库名; tableName:表名; domainObjectName:对应的DO -->

<!--
domainObjectName 必须填写不然会报空引用
column是当前表中字段，joinColumn是关联表的字段
-->
        <table tableName="employee" domainObjectName="Employee"
               enableCountByExample="false" enableUpdateByExample="false"
               enableDeleteByExample="false" enableSelectByExample="false"
               selectByExampleQueryId="false" selectByPrimaryKeyQueryId="false">
            <!--根据数据库字段名称转成驼峰写法 -->
            <!--写法问题： oracle 数据库，字段不区分大小写，数据库中全是大写，驼峰写法：设置false 字段名称在驼峰中使用下划线"_"
            例如：file_name
                转为fileName -->
            <!--写法问题： mysql 数据库，字段区分大小写，驼峰写法：设置true时 字段名称正常写法 例如：fileName 转为fileName;
                设置false 则是filename -->
            <property name="useActualColumnNames" value="true" />
            <generatedKey column="id" sqlStatement="MySql"
                          identity="true" />
            <oneToOne mappingTable="department" column="dept_id" joinColumn="id" />
            <!-- 有对应实体的时候，把字段忽略生成 -->
            <ignoreColumn column="dept_id" />
        </table>
        <table tableName="department" domainObjectName="Department"
               enableCountByExample="false" enableUpdateByExample="false"
               enableDeleteByExample="false" enableSelectByExample="false"
               selectByExampleQueryId="false" selectByPrimaryKeyQueryId="false">
<!--     注意，设置了自增的字段，插入sql语句中会省略他，但是如果你的表中这个字段不是自增的且没有默认值，将会报错 -->
<!--            <generatedKey column="id" sqlStatement="MySql"-->
<!--                          identity="true" />-->
        </table>

 </context>
</generatorConfiguration>
```



## 生成接口

执行逆向工程后，生成如下接口

```java
package com.cxf.mapper;

import com.cxf.model.Employee;
import java.util.List;

public interface EmployeeMapper {
    /**
     *  根据主键删除数据库的记录,employee
     *
     * @param id
     */
    int deleteByPrimaryKey(Integer id);

    /**
     *  新写入数据库记录,employee
     *
     * @param record
     */
    int insert(Employee record);

    /**
     *  动态字段,写入数据库记录,employee
     *
     * @param record
     */
    int insertSelective(Employee record);

    /**
     *  根据指定主键获取一条数据库记录,employee
     *
     * @param id
     */
    Employee selectByPrimaryKey(Integer id);

    /**
     *  动态字段,根据主键来更新符合条件的数据库记录,employee
     *
     * @param record
     */
    int updateByPrimaryKeySelective(Employee record);

    /**
     *  根据主键来更新符合条件的数据库记录,employee
     *
     * @param record
     */
    int updateByPrimaryKey(Employee record);

    int insertBatchSelective(List<Employee> records);

    int updateBatchByPrimaryKeySelective(List<Employee> records);
}
```



```java
package com.cxf.mapper;

import com.cxf.model.Department;
import org.apache.ibatis.annotations.Select;

import java.util.List;


public interface DepartmentMapper {
    /**
     *  根据主键删除数据库的记录,department
     *
     * @param id
     */
    int deleteByPrimaryKey(Integer id);

    /**
     *  新写入数据库记录,department
     *
     * @param record
     */
    int insert(Department record);

    /**
     *  动态字段,写入数据库记录,department
     *
     * @param record
     */
    int insertSelective(Department record);

    /**
     *  根据指定主键获取一条数据库记录,department
     *
     * @param id
     */
    Department selectByPrimaryKey(Integer id);

    /**
     *  动态字段,根据主键来更新符合条件的数据库记录,department
     *
     * @param record
     */
    int updateByPrimaryKeySelective(Department record);

    /**
     *  根据主键来更新符合条件的数据库记录,department
     *
     * @param record
     */
    int updateByPrimaryKey(Department record);

    int insertBatchSelective(List<Department> records);

    int updateBatchByPrimaryKeySelective(List<Department> records);
}
```

我们看到生成的接口中多了一些批量插入和批量更新的接口

**值得注意的是，如果使用批量更新功能需要在连接的配置上添加allowMultiQueries=true**



# 遇到的问题

插入数据库时，提示：

mybatis  doesn't have a default value

说明mysql表的字段没有设置自增或者没有提供默认值，而我们的xml里写的insert语句没有把该值填充。解决办法有两个：

一是该表字段为自增或者有默认值，而是改生成的xml文件里的sql语句。

归根结底，是我们的配置写错了,把generatedKey这个标签删除掉，因为在数据库中这个字段不是自增的。

```xml
        <table tableName="department" domainObjectName="Department"
               enableCountByExample="false" enableUpdateByExample="false"
               enableDeleteByExample="false" enableSelectByExample="false"
               selectByExampleQueryId="false" selectByPrimaryKeyQueryId="false">
<!--     注意，设置了自增的字段，插入sql语句中会省略他，但是如果你的表中这个字段不是自增的且没有默认值，将会报错 -->
<!--            <generatedKey column="id" sqlStatement="MySql"-->
<!--                          identity="true" />-->
        </table>
```



这里提一下insert和insertSelective的区别

# insert和insertSelective的区别

updateByPrimaryKey对你注入的字段全部更新（不判断是否为Null）

updateByPrimaryKeySelective会对字段进行判断再更新(如果为Null就忽略更新)

区别了这两点就很容易根据业务来选择服务层的调用了！

详细可以查看generator生成的源代码！

insert和insertSelective和上面类似，加入是insert就把所有值插入,但是要注意加入数据库字段有default,default是不会起作用的

insertSelective不会忽略default
