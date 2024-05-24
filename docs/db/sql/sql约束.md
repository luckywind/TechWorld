# 唯一约束

## 单列唯一

```sql
//建表时指定
  `username` varchar(18) NOT NULL unique, 
//给已有表添加
ALTER TABLE `t_user` ADD unique(`username`);
或者：
create unique index UserNameIndex on 't_user' ('username');
```

## 多列唯一

```sql
ALTER TABLE jw_resource
ADD UNIQUE KEY(resource_name, resource_type);
//删除唯一约束
ALTER TABLE jw_resource DROP INDEX `resource_name`;
//
show index from jw_resource;
```

***\*注意：\****唯一键约束添加后，在建表的元数据中，默认的唯一键约束名称为第一列的名称。

***\*注意：\****唯一键约束添加后，实际上建立了一个索引，将该索引删除后，就等于删除了联合唯一约束。

