# 日期区间

```scala
import org.joda.time.Days

val start = DateTime.now.minusDays(5)
val end   = DateTime.now.plusDays(5)    

val daysCount = Days.daysBetween(start, end).getDays()
(0 until daysCount).map(start.plusDays(_)).foreach(println)
```

# DataFrame=>RDD

[map](https://datasciencevademecum.com/2016/03/01/mapping-dataframe-to-a-typed-rdd/)

```sql
    val spark = SparkSession
      .builder()
      .config("spark.hadoop.fs.permissions.umask-mode","000")
      .config("spark.sql.sources.partitionOverwriteMode","dynamic")
      .getOrCreate()
    val frame: DataFrame = spark.read.parquet(routerDir)//.na.drop()
    frame.printSchema()
    res=frame.rdd.map{
      case Row(did:String,first_day:Long,last_day:Long,start_day:Long,etl_tm:String) =>{
        val chain = new DwmDvcOtDeviceActiveChain()
        chain.setDid(did)
        chain.setFirst_day(first_day)
        chain.setLast_day(last_day)
        chain.setStart_day(start_day)
        chain.setEtl_tm(etl_tm)
      }
    }
```

# 读写数据文件错误

https://xiaomi.f.mioffice.cn/docs/dock4KdAQKgIxSXRKcW0JHr92vg

## ParquetDecodingException: Can not read value at 0 in block -1 in file 

