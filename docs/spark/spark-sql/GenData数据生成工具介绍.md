### 简介

该工具可以快速的生成parquet数据，用户只需提供每个字段的生成逻辑(SparkSQL语法)
工具地址： 192.168.28.16:/home/hadoop/chengxingfu/data_gen/spark-race-app-1.0-SNAPSHOT-jar-with-dependencies.jar

### 使用指南

​      打包后，运行主类com.yusur.spark.GenData类即可， 它是一个Spark应用，可以按照你喜欢的方式运行，例如local模式、Spark on Yarn等。
 主类接收5个参数：

1. 输出路径， 支持本地文件系统([file:///开头](file:///开头))，   hdfs目录(/开头)
2. 数据量，单位为条数
3. 每批数据量，单位为条数
4. 生成文件数
5. SparkSQL语法的字段生成逻辑
   这里只需写逗号分隔的Project字段集合即可，工具会按照这里的逻辑生成数据



### 示例

假如生成数据的需求如下：
生成20亿条数据，共100个parquet文件， 有prod_catalog_cd/p_day_id/p_lan_id三个string类型的字段，且其分布有如下要求：

1. prod_catalog_cd取值范围为[‘0’,‘1’ , ‘2’, ‘3’，‘99’]  且 0占比25%, 1占比25%， 2占比25%，3占比24%， 99占比1%
2. p_day_id为日期类型，格式为yyyyMMdd
3. p_lan_id取值范围为[21, 11,13],    且21占比2.7%， 11占比16.2%， 其余为13

则我们可以这么写一个脚本，该脚本将向local模式提交一个Spark应用，生成数据放到file:///data/int3bas.db/bas_prd_prod_inst_d路径下

```sql
$SPARK_HOME/bin/spark-submit \
  --master local[8] \
  --driver-memory 100g \
  --executor-memory 4g \
  --executor-cores 1 \
  --conf spark.task.cpus=1 \
  --class com.yusur.spark.GenData\
  /home/hadoop/chengxingfu/data_gen/spark-race-app-1.0-SNAPSHOT-jar-with-dependencies.jar \
  file:///data/int3bas.db/bas_prd_prod_inst_d 2000000000 1000000 100 "case when r<0.25 then '0' \
when r >= 0.25 and r<0.5 then '1'    \
when r>=0.5 and r<0.75 then '2'     \
when r>=0.76 and r<0.9 then '3'     \
else '99' end as prod_catalog_cd,\
date_format(date_sub(date('2023-05-09'),cast(rand()*100 as int)),'yyyyMMdd') p_day_id,\
case when r2<0.027 then '21' \
when r2>=0.027 and r2<(0.027+0.162) then '11' \
else '13' end as p_lan_id" 
```

**注意**： 

1. 这里的r 是SparkSQL的函数rand()生成的一个(0,1)范围的随机数，工具内置了r, r2, r3三个，当有更多需求时，可直接调用rand()方法自行生成
2. 当生成char类型数据时，如果数据长度不足，请补齐
2. 如果只想在本机生成数据，则只能使用local模式， 集群模式需要每个节点都有目标目录

#### 示例2

从某个集合中随机取值

```scala
split('AZ,SC,LA,MN,NJ,DC,OR,VA,RI,KY,WY,NH,MI,NV,WI,ID,CA,NE,CT,MT,NC,VT,MD,DE,MO,IL,ME,ND,WA,MS,AL,IN,OH,TN,IA,NM,PA,SD,1 ,NY,TX,WV,GA,MA,KS,FL,CO,AK,AR,OK,UT,HI',',')[cast(rand()*50 as int)] as ca_state
```

