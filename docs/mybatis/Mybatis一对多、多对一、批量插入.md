[参考](https://blog.csdn.net/dyy_gusi/article/details/49302519?utm_medium=distribute.pc_relevant_right.none-task-blog-BlogCommendFromMachineLearnPai2-3.nonecase&depth_1-utm_source=distribute.pc_relevant_right.none-task-blog-BlogCommendFromMachineLearnPai2-3.nonecase)

# 一对一关系类

## 

三张表

假设，emp和dept是一对一关系， emp和kpi是一对多关系

```mysql
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
    ); 

CREATE table kpi(
kpiId int ,
kpiName VARCHAR(20),
empId int
)
 
```

