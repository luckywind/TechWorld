# 映射方式

## getter/setter注入

这要求实体属性必须有getter/setter方法

```java
@Data
public class Employee implements Serializable {
    private static final long serialVersionUID = -7145891282327539285L;
    private String employeeId;
    private String employeeName;
    private Integer employeeType;
}

```

```xml
<mapper namespace="cn.felord.mybatis.mapper.EmployeeMapper">
    <resultMap id="EmployeeMap" type="cn.felord.mybatis.entity.Employee">
        <id column="employee_id" property="employeeId"/>
        <result column="employee_name" property="employeeName"/>
        <result column="employee_type" property="employeeType"/>
    </resultMap>
</mapper>
```



## 构造器注入

带有构造参数的构造器

```java
public Employee(String employeeId, String employeeName, Integer employeeType) {
    this.employeeId = employeeId;
    this.employeeName = employeeName;
    this.employeeType = employeeType;
}
```

```xml
<mapper namespace="cn.felord.mybatis.mapper.EmployeeMapper">
    <resultMap id="EmployeeConstructorMap" type="cn.felord.mybatis.entity.Employee">
        <constructor>
            <idArg column="employee_id" javaType="String" name="employeeId"/>
            <!-- 你可以不按参数列表顺序添加-->
            <arg column="employee_type" javaType="Integer" name="employeeType"/>
            <arg column="employee_name" javaType="String" name="employeeName"/>
        </constructor>
    </resultMap>
</mapper>
```

通过name指定属性，mybatis3.4.3才支持，否则只能按照构造参数的顺序指定

## 继承关系

跟 **Java** 的继承关键字一样使用 `extends` 来进行继承。

```xml
<resultMap id="RegularEmployeeMap" extends="EmployeeMap" type="cn.felord.mybatis.entity.RegularEmployee">
    <result column="level" property="level"/>
    <result column="job_number" property="jobNumber"/>
    <association property="department" javaType="cn.felord.mybatis.entity.Department">
        <id column="department_id" property="departmentId"/>
        <result column="department_name" property="departmentName"/>
        <result column="department_level" property="departmentLevel"/>
    </association>
</resultMap>
```

## 一对一关联

1.3中的例子，使用association标签把RegularEmployeeMap的department属性关联信息映射到结果集了

还可以嵌套

## 一对多关联

例如，部门实体

```java
/**
 * @author felord.cn
 * @since 15:33
 **/
public class DepartmentAndEmployeeList extends Department {
    private static final long serialVersionUID = -2503893191396554581L;
    private List<Employee> employees;

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }
}

```

我们想查出所有员工,使用collection标签即可完成

```xml
<resultMap id="DepartmentAndEmployeeListMap" extends="DepartmentMap"
           type="cn.felord.mybatis.entity.DepartmentAndEmployeeList">
    <collection property="employees" ofType="cn.felord.mybatis.entity.RegularEmployee">
        <id column="employee_id" property="employeeId"/>
        <result column="employee_name" property="employeeName"/>
        <result column="level" property="level"/>
        <result column="job_number" property="jobNumber"/>
    </collection>
</resultMap>
```

## 鉴别器

类似java中的switch case

假设实体,部门有正式员工集合，和临时员工集合：

```java
/**
 * @author felord.cn
 * @since 15:33
 **/
public class DepartmentAndTypeEmployees extends Department {
    private static final long serialVersionUID = -2503893191396554581L;
    private List<RegularEmployee> regularEmployees;
    private List<TemporaryEmployee> temporaryEmployees;
    // getter setter
}

```

想要分别查出他们

```xml
<resultMap id="DepartmentAndTypeEmployeesMap" extends="DepartmentMap"
           type="cn.felord.mybatis.entity.DepartmentAndTypeEmployees">
    <collection property="regularEmployees" ofType="cn.felord.mybatis.entity.RegularEmployee">
        <discriminator javaType="int" column="employee_type">
            <case value="1">
                <id column="employee_id" property="employeeId"/>
                <result column="employee_name" property="employeeName"/>
                <result column="employee_type" property="employeeType"/>
                <result column="level" property="level"/>
                <result column="job_number" property="jobNumber"/>
            </case>
        </discriminator>
    </collection>
    <collection property="temporaryEmployees" ofType="cn.felord.mybatis.entity.TemporaryEmployee">
        <discriminator javaType="int" column="employee_type">
            <case value="0">
                <id column="employee_id" property="employeeId"/>
                <result column="employee_name" property="employeeName"/>
                <result column="employee_type" property="employeeType"/>
                <result column="company_no" property="companyNo"/>
            </case>
        </discriminator>
    </collection>
</resultMap>

```

[集合映射](https://blog.csdn.net/menghuanzhiming/article/details/79206068)

