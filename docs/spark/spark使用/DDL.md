[参考](https://spark.apache.org/docs/latest/sql-ref-syntax-ddl-create-table-datasource.html)

# DataSource语法建表

使用DataSource语法创建一个新的表

## 语法

```sql
CREATE TABLE [ IF NOT EXISTS ] table_identifier
    [ ( col_name1 col_type1 [ COMMENT col_comment1 ], ... ) ]
    
    USING data_source   -- 数据存储格式，可以是CSV、TXT、ORC、JDBC、PARQUET等
    [ OPTIONS ( key1=val1, key2=val2, ... ) ]
  
    [ PARTITIONED BY ( col_name1, col_name2, ... ) ]
    [ CLUSTERED BY ( col_name3, col_name4, ... ) 
        [ SORTED BY ( col_name [ ASC | DESC ], ... ) ] 
        INTO num_buckets BUCKETS ]
    [ LOCATION path ]  -- 存储路径，可以是file:///,  hdfs:/
    [ COMMENT table_comment ]
    [ TBLPROPERTIES ( key1=val1, key2=val2, ... ) ]
    
    [ AS select_statement ]
```

### **OPTIONS**

数据源的选项，将注入到存储属性中。[参考华为云](https://support.huaweicloud.com/sqlref-spark-dli/dli_08_0076.html)

| **参数**            | **描述**                                                     | 默认值 |
| ------------------- | ------------------------------------------------------------ | ------ |
| path                | 指定的表路径，即OBS存储路径。                                | -      |
| multiLevelDirEnable | 嵌套子目录场景下，是否迭代查询子目录中的数据。当配置为true时，查询该表时会迭代读取该表路径中所有文件，包含子目录中的文件。 | false  |
| dataDelegated       | 是否需要在删除表或分区时，清除path路径下的数据。             | false  |
| compression         | 指定压缩格式。一般为parquet格式时指定该参数，推荐使用'zstd'压缩格式。 |        |

当file_format为csv时，还可以设置以下OPTIONS参数。

| 参数            | 描述                                                         | 默认值              |
| --------------- | ------------------------------------------------------------ | ------------------- |
| delimiter       | 数据分隔符。                                                 | 逗号（即",”）       |
| quote           | 引用字符。                                                   | 双引号（即“"”）     |
| escape          | 转义字符。                                                   | 反斜杠（即“\”）     |
| multiLine       | 列数据中是否包含回车符或转行符，true为包含，false为不包含    | false               |
| dateFormat      | 指定CSV文件中date字段的日期格式                              | yyyy-MM-dd          |
| timestampFormat | 指定CSV文件中timestamp字段的日期格式                         | yyyy-MM-dd HH:mm:ss |
| mode            | 指定解析CSV时的模式，有三种模式。PERMISSIVE：宽容模式，遇到错误的字段时，设置该字段为NullDROPMALFORMED: 遇到错误的字段时，丢弃整行。FAILFAST：报错模式，遇到错误的字段时直接报错。 | PERMISSIVE          |
| header          | CSV是否包含表头信息，true表示包含表头信息，false为不包含。   | false               |
| nullValue       | 设置代表null的字符，例如，nullValue=“\\N”表示设置\N 代表null。 | -                   |
| comment         | 设置代表注释开头的字符，例如，comment='#'表示以#开头的行为注释。 | -                   |
| compression     | 设置数据的压缩格式。目前支持gzip、bzip2、deflate压缩格式，若不希望压缩，则输入none。 | none                |
| encoding        | 数据的编码格式。支持utf-8，gb2312，gbk三种，如果不填写，则默认为utf-8。 | utf-8               |



```sql
OPTIONS(header 'false', delimiter '|', path 'file://_datPath_/tpcds1tdv/web_site.dat')
```

### properties

#### 查看

```sql
SHOW TBLPROPERTIES 表名;
```





## 只有表结构

### create table ... using

```sql
create table 
(xxx)
USING csv OPTIONS(header 'false', delimiter '|', path 'file://_datPath_/tpcds1tdv/store_sales.dat')
```



### create table ... stored as 

```sql
create table 
[(col1, col2, ...)]
STORED AS PARQUET TBLPROPERTIES('parquet.compression'='none') 
```

# HIVE语法建表

使用Hive语法创建OBS表。DataSource语法和Hive语法主要区别在于支持的表数据存储格式范围、支持的分区数等有差异，详细请参考语法格式和注意事项说明。[参考](https://support.huaweicloud.com/sqlref-spark-dli/dli_08_0077.html)

```sql
CREATE [EXTERNAL] TABLE [IF NOT EXISTS] [db_name.]table_name 
  [(col_name1 col_type1 [COMMENT col_comment1], ...)]
  [COMMENT table_comment] 
  [PARTITIONED BY (col_name2 col_type2, [COMMENT col_comment2], ...)] 
  [ROW FORMAT row_format]
  [STORED AS file_format] 
  LOCATION 'obs_path'
  [TBLPROPERTIES (key = value)]
  [AS select_statement]
row_format:
  : SERDE serde_cls [WITH SERDEPROPERTIES (key1=val1, key2=val2, ...)]
  | DELIMITED [FIELDS TERMINATED BY char [ESCAPED BY char]]
      [COLLECTION ITEMS TERMINATED BY char]
      [MAP KEYS TERMINATED BY char]
      [LINES TERMINATED BY char]
      [NULL DEFINED AS char]

```

1. STORED AS :  TEXTFILE, AVRO, ORC, SEQUENCEFILE, RCFILE, PARQUET格式
2. TBLPROPERTIES：TBLPROPERTIES子句允许用户给表添加key/value的属性。例如多版本支持
3. row_format： 行数据格式





# insert overwrite

# 工具

## 导出库中所有表的DDL

```shell
1. hive -e "use databaseName; show tables;" > all_tables.txt
2. 运行脚本:
#!/bin/bash
cat all_tables.txt |while read LINE
do
hive -e "use 库名;show create table $LINE" >>tablesDDL.txt
done
```

## spark

```scala
// 创建内部表
df.write.mode(SaveMode.Overwrite)
 .saveAsTable("db.tb")

// 创建外部表
df.write.mode()
.option("path","/path/to/external/table")
.saveAsTable("db.tb")
```

外部表在drop表时，数据不会清除。可以类比linux里的软链接与硬链接