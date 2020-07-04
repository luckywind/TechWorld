# sparkContext/sqlContext/hiveContext/sparkSession

## What is SparkContext?

1.  driver 使用SparkContext与 cluster连接并交互 用于和资源管理器协调SparkJob的执行.
2. 使用 SparkContext 可以直接获得 contexts 例如SQLContext 和 HiveContext.
3. 使用 SparkContext设置Spark Job的参数

If you are in spark-shell, a SparkContext is already available for you and is assigned to the variable sc. If you don’t have a SparkContext already, you can create one by first creating a SparkConf first.

```
//set up the spark configurationval sparkConf = new SparkConf().setAppName("hirw").setMaster("yarn")//get SparkContext using the SparkConfval sc = new SparkContext(sparkConf)
```

## What is a SQLContext?

SQLContext是SparkSQL的入口. 使用SparkContext.创建SQLContext:

```
// sc is an existing SparkContext.val sqlContext = new org.apache.spark.sql.SQLContext(sc)
```

Once you have the SQLContext you can start working with DataFrame, DataSet etc.

## What is a HiveContext?

HiveContext是 Hive的入口. HiveContext has all the functionalities of a SQLContext. In fact, if you look at the API documentation you can see that HiveContext extends SQLContext, meaning, it has support the functionalities that SQLContext support plus more (Hive specific functionalities)

`public class HiveContextextends SQLContextimplements Logging`
Here is how we can get a HiveContext using the SparkContext

```
// sc is an existing SparkContext.val sqlContext = new org.apache.spark.sql.hive.HiveContext(sc)
```

## What is a SparkSession?

SparkSession was introduced in Spark 2.0 to make it easy for the developers so we don’t have worry about different contexts and to streamline the access to different contexts. By having access to SparkSession, we automatically have access to the SparkContext.

Here is how we can create a SparkSession –

`val spark = SparkSession.builder().appName("hirw-test").config("spark.some.config.option", "some-value").getOrCreate()`
`SparkSession` is now the new entry point of Spark that replaces the old `SQLContext` and `HiveContext`. Note that the old SQLContext and HiveContext are kept for backward compatibility(向后兼容).

Once we have access to a SparkSession, we can start working with DataFrame and Dataset. Simply using the SparkSession to read a JSON file in to a DataFrame.

使用SparkSession可以直接

`val df = spark.read.json("path/to/file.json")`
开启hive支持

`val spark = SparkSession.builder().appName("hirw-hive-test").config("spark.sql.warehouse.dir", warehouseLocation).enableHiveSupport().getOrCreate()`
如果使用spark2.x，请使用 SparkSession.

# 使用

## 创建sparkSession

```scala
// Create a SparkSession. No need to create SparkContext
// You automatically get it as part of the SparkSession
val warehouseLocation = "file:${system:user.dir}/spark-warehouse"
val spark = SparkSession
   .builder()
   .appName("SparkSessionZipsExample")
   .config("spark.sql.warehouse.dir", warehouseLocation)
   .enableHiveSupport()
   .getOrCreate()
```

## 配置运行时属性

```scala
//set new runtime options
spark.conf.set("spark.sql.shuffle.partitions", 6)
spark.conf.set("spark.executor.memory", "2g")
//get all settings
val configMap:Map[String, String] = spark.conf.getAll
```

```scala
spark.conf.getAll
res3: Map[String,String] = Map(spark.port.maxRetries -> 100, spark.akka.timeout -> 300, spark.serializer -> org.apache.spark.serializer.KryoSerializer, spark.bsaapp.clean.dir -> /home/master/spark-2.4.0-bin-hadoop2.7/clean, spark.scheduler.allocation.file -> /home/master/spark-2.4.0-bin-hadoop2.7/conf/fairscheduler.xml, spark.executor.extraJavaOptions -> -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=70, spark.driver.host -> bsa223, spark.driver.port -> 38758, spark.repl.class.uri -> spark://bsa223:38758/classes, spark.jars -> "", spark.repl.class.outputDir -> /home/master/spark-2.4.0-bin-hadoop2.7/tmp/spark-de2172dc-3dde-48ab-b1d1-6c222fbe23a8/repl-e32cac8f-f026-47af-ba96-d74ad125b86f, spark.app.name -> Spark shell, spark.network.timeout -...
```

## 获取catalog元数据

Sparksession 暴漏catalog作为一个公共实例，它包含操作元数据(即catalog)的方法，这些方法返回Dataset.

```scala
//fetch metadata data from the catalog
spark.catalog.listDatabases.show(false)

spark.catalog.listTables.show(false)
```



## 创建Datasets和Dataframes

```scala
//create a Dataset using spark.range starting from 5 to 100, with increments of 5
val numDS = spark.range(5, 100, 5)
// reverse the order and display first 5 items
numDS.orderBy(desc("id")).show(5)
//compute descriptive stats and display them
numDs.describe().show()
// create a DataFrame using spark.createDataFrame from a List or Seq
val langPercentDF = spark.createDataFrame(List(("Scala", 35), ("Python", 30), ("R", 15), ("Java", 20)))
//rename the columns
val lpDF = langPercentDF.withColumnRenamed("_1", "language").withColumnRenamed("_2", "percent")
//order the DataFrame in descending order of percentage
lpDF.orderBy(desc("percent")).show(false)
```

## 读取JSON

```scala
// read the json file and create the dataframe
val jsonFile = args(0)
val zipsDF = spark.read.json(jsonFile)
//filter all cities whose population > 40K
zipsDF.filter(zipsDF.col("pop") > 40000).show(10)
```

## 使用SparkSQL

```scala
// Now create an SQL table and issue SQL queries against it without
// using the sqlContext but through the SparkSession object.
// Creates a temporary view of the DataFrame
zipsDF.createOrReplaceTempView("zips_table")
zipsDF.cache()
val resultsDF = spark.sql("SELECT city, pop, state, zip FROM zips_table")
resultsDF.show(10)
```

## 读写hive表

```scala
//drop the table if exists to get around existing table error
spark.sql("DROP TABLE IF EXISTS zips_hive_table")
//save as a hive table
spark.table("zips_table").write.saveAsTable("zips_hive_table")
//make a similar query against the hive table 
val resultsHiveDF = spark.sql("SELECT city, pop, state, zip FROM zips_hive_table WHERE pop > 40000")
resultsHiveDF.show(10)
```

# SparkSession源码

```scala


package org.apache.spark.sql

import java.beans.Introspector
import java.util.concurrent.atomic.AtomicReference

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag
import scala.util.control.NonFatal

import org.apache.spark.{SPARK_VERSION, SparkConf, SparkContext}
import org.apache.spark.annotation.{DeveloperApi, Experimental}
import org.apache.spark.api.java.JavaRDD
import org.apache.spark.internal.Logging
import org.apache.spark.internal.config.CATALOG_IMPLEMENTATION
import org.apache.spark.rdd.RDD
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationEnd}
import org.apache.spark.sql.catalog.Catalog
import org.apache.spark.sql.catalyst._
import org.apache.spark.sql.catalyst.encoders._
import org.apache.spark.sql.catalyst.expressions.AttributeReference
import org.apache.spark.sql.catalyst.plans.logical.{LocalRelation, Range}
import org.apache.spark.sql.execution._
import org.apache.spark.sql.execution.datasources.LogicalRelation
import org.apache.spark.sql.execution.ui.SQLListener
import org.apache.spark.sql.internal.{CatalogImpl, SessionState, SharedState}
import org.apache.spark.sql.sources.BaseRelation
import org.apache.spark.sql.streaming._
import org.apache.spark.sql.types.{DataType, LongType, StructType}
import org.apache.spark.sql.util.ExecutionListenerManager
import org.apache.spark.util.Utils


/**
 * dataset和dataframe API编程的入口
   对于已经创建该对象的环境(REPL/notebooks)，使用builder获取该session
 * {{{
 *   SparkSession.builder().getOrCreate()
 * }}}
 *
 * builder也可以用来创建新的session
 *
 * {{{
 *   SparkSession.builder()
 *     .master("local")
 *     .appName("Word Count")
 *     .config("spark.some.config.option", "some-value")
 *     .getOrCreate()
 * }}}
 */
class SparkSession private(
    @transient val sparkContext: SparkContext,
    @transient private val existingSharedState: Option[SharedState])
  extends Serializable with Logging { self =>

  private[sql] def this(sc: SparkContext) {
    this(sc, None)
  }

  sparkContext.assertNotStopped()

  /**
   * The version of Spark on which this application is running.
   *
   * @since 2.0.0
   */
  def version: String = SPARK_VERSION

  /* ----------------------- *
   |  Session-related state  |
   * ----------------------- */

  /**
   * State shared across sessions, including the [[SparkContext]], cached data, listener,
   * and a catalog that interacts with external systems.
   */
  @transient
    //注意这个方法的修饰符，只有sql包可以访问
  private[sql] lazy val sharedState: SharedState = {
    existingSharedState.getOrElse(
      SparkSession.reflect[SharedState, SparkContext](
        SparkSession.sharedStateClassName(sparkContext.conf),
        sparkContext))
  }

  /**
   * State isolated across sessions, including SQL configurations, temporary tables, registered
   * functions, and everything else that accepts a [[org.apache.spark.sql.internal.SQLConf]].
   */
  @transient
  private[sql] lazy val sessionState: SessionState = {
    SparkSession.reflect[SessionState, SparkSession](
      SparkSession.sessionStateClassName(sparkContext.conf),
      self)
  }

  /**
   * A wrapped version of this session in the form of a [[SQLContext]], for backward compatibility.
   *
   * @since 2.0.0
   */
  @transient
  val sqlContext: SQLContext = new SQLContext(this)

  /**
   * Runtime configuration interface for Spark.
   *
   * This is the interface through which the user can get and set all Spark and Hadoop
   * configurations that are relevant to Spark SQL. When getting the value of a config,
   * this defaults to the value set in the underlying [[SparkContext]], if any.
   *
   * @since 2.0.0
   */
  @transient lazy val conf: RuntimeConfig = new RuntimeConfig(sessionState.conf)

  /**
   * :: Experimental ::
   * An interface to register custom [[org.apache.spark.sql.util.QueryExecutionListener]]s
   * that listen for execution metrics.
   *
   * @since 2.0.0
   */
  @Experimental
  def listenerManager: ExecutionListenerManager = sessionState.listenerManager

  /**
   * :: Experimental ::
   * A collection of methods that are considered experimental, but can be used to hook into
   * the query planner for advanced functionality.
   *
   * @since 2.0.0
   */
  @Experimental
  def experimental: ExperimentalMethods = sessionState.experimentalMethods

  /**
   * 用于注册udf的方法集合，udf必须是确定的，为了优化，udf可能会多次调用或者省略重复调用
   * 注册一个scala闭包作为udf:
   * {{{
   *   sparkSession.udf.register("myUDF", (arg1: Int, arg2: String) => arg2 + arg1)
   * }}}
   *
   * The following example registers a UDF in Java:
   * {{{
   *   sparkSession.udf().register("myUDF",
   *       new UDF2<Integer, String, String>() {
   *           @Override
   *           public String call(Integer arg1, String arg2) {
   *               return arg2 + arg1;
   *           }
   *      }, DataTypes.StringType);
   * }}}
   *
   * Or, to use Java 8 lambda syntax:
   * {{{
   *   sparkSession.udf().register("myUDF",
   *       (Integer arg1, String arg2) -> arg2 + arg1,
   *       DataTypes.StringType);
   * }}}
   *
   * @since 2.0.0
   */
  def udf: UDFRegistration = sessionState.udf

  /**
   * :: Experimental ::
   * Returns a [[StreamingQueryManager]] that allows managing all the
   * [[StreamingQuery StreamingQueries]] active on `this`.
   *
   * @since 2.0.0
   */
  @Experimental
  def streams: StreamingQueryManager = sessionState.streamingQueryManager

  /**
   * 使用独立的sql配置启动一个新的session，临时表、注册的函数是独立的，但是共享sparkContext和缓存数据。
     注：除了sparkContext,所有共享状态的初始化都是lazy的。这个方法会强制共享状态的初始化以确保父子session有相同的状态，
     如果catalog实现是hive，这会初始化metastore。
   * 
   */
  def newSession(): SparkSession = {
    new SparkSession(sparkContext, Some(sharedState))
  }


  /* --------------------------------- *
   |  Methods for creating DataFrames  |
   * --------------------------------- */

  /**
   * Returns a [[DataFrame]] with no rows or columns.
   *
   * @since 2.0.0
   */
  @transient
  lazy val emptyDataFrame: DataFrame = {
    createDataFrame(sparkContext.emptyRDD[Row], StructType(Nil))
  }

  /**
   * :: Experimental ::
   * Creates a new [[Dataset]] of type T containing zero elements.
   *
   * @return 2.0.0
   */
  @Experimental
  def emptyDataset[T: Encoder]: Dataset[T] = {
    val encoder = implicitly[Encoder[T]]
    new Dataset(self, LocalRelation(encoder.schema.toAttributes), encoder)
  }

  /**
   * :: Experimental ::
   * Creates a [[DataFrame]] from an RDD of Product (e.g. case classes, tuples).
   *
   * @since 2.0.0
   */
  @Experimental
  def createDataFrame[A <: Product : TypeTag](rdd: RDD[A]): DataFrame = {
    SparkSession.setActiveSession(this)
    val schema = ScalaReflection.schemaFor[A].dataType.asInstanceOf[StructType]
    val attributeSeq = schema.toAttributes
    val rowRDD = RDDConversions.productToRowRdd(rdd, schema.map(_.dataType))
    Dataset.ofRows(self, LogicalRDD(attributeSeq, rowRDD)(self))
  }

  /**
   * :: Experimental ::
   * Creates a [[DataFrame]] from a local Seq of Product.
   *
   * @since 2.0.0
   */
  @Experimental
  def createDataFrame[A <: Product : TypeTag](data: Seq[A]): DataFrame = {
    SparkSession.setActiveSession(this)
    val schema = ScalaReflection.schemaFor[A].dataType.asInstanceOf[StructType]
    val attributeSeq = schema.toAttributes
    Dataset.ofRows(self, LocalRelation.fromProduct(attributeSeq, data))
  }

  /**
   * :: DeveloperApi ::
   * Creates a [[DataFrame]] from an [[RDD]] containing [[Row]]s using the given schema
     必须确保RDD的每个Row的结构匹配schema，否则抛异常
   * Example:
   * {{{
   *  import org.apache.spark.sql._
   *  import org.apache.spark.sql.types._
   *  val sparkSession = new org.apache.spark.sql.SparkSession(sc)
   *
   *  val schema =
   *    StructType(
   *      StructField("name", StringType, false) ::
   *      StructField("age", IntegerType, true) :: Nil)
   *
   *  val people =
   *    sc.textFile("examples/src/main/resources/people.txt").map(
   *      _.split(",")).map(p => Row(p(0), p(1).trim.toInt))
   *  val dataFrame = sparkSession.createDataFrame(people, schema)
   *  dataFrame.printSchema
   *  // root
   *  // |-- name: string (nullable = false)
   *  // |-- age: integer (nullable = true)
   *
   *  dataFrame.createOrReplaceTempView("people")
   *  sparkSession.sql("select name from people").collect.foreach(println)
   * }}}
   *
   * @since 2.0.0
   */
  @DeveloperApi
  def createDataFrame(rowRDD: RDD[Row], schema: StructType): DataFrame = {
    createDataFrame(rowRDD, schema, needsConversion = true)
  }

  /**
   * :: DeveloperApi ::
   * Creates a [[DataFrame]] from a [[JavaRDD]] containing [[Row]]s using the given schema.
   * It is important to make sure that the structure of every [[Row]] of the provided RDD matches
   * the provided schema. Otherwise, there will be runtime exception.
   *
   * @since 2.0.0
   */
  @DeveloperApi
  def createDataFrame(rowRDD: JavaRDD[Row], schema: StructType): DataFrame = {
    createDataFrame(rowRDD.rdd, schema)
  }

  /**
   * :: DeveloperApi ::
   * Creates a [[DataFrame]] from a [[java.util.List]] containing [[Row]]s using the given schema.
   * It is important to make sure that the structure of every [[Row]] of the provided List matches
   * the provided schema. Otherwise, there will be runtime exception.
   *
   * @since 2.0.0
   */
  @DeveloperApi
  def createDataFrame(rows: java.util.List[Row], schema: StructType): DataFrame = {
    Dataset.ofRows(self, LocalRelation.fromExternalRows(schema.toAttributes, rows.asScala))
  }

  /**
   * Applies a schema to an RDD of Java Beans.
   *
   * WARNING: Since there is no guaranteed ordering for fields in a Java Bean,
   * SELECT * queries will return the columns in an undefined order.
   *
   * @since 2.0.0
   */
  def createDataFrame(rdd: RDD[_], beanClass: Class[_]): DataFrame = {
    val attributeSeq: Seq[AttributeReference] = getSchema(beanClass)
    val className = beanClass.getName
    val rowRdd = rdd.mapPartitions { iter =>
    // BeanInfo is not serializable so we must rediscover it remotely for each partition.
      val localBeanInfo = Introspector.getBeanInfo(Utils.classForName(className))
      SQLContext.beansToRows(iter, localBeanInfo, attributeSeq)
    }
    Dataset.ofRows(self, LogicalRDD(attributeSeq, rowRdd)(self))
  }

  /**
   * Applies a schema to an RDD of Java Beans.
   *
   * WARNING: Since there is no guaranteed ordering for fields in a Java Bean,
   * SELECT * queries will return the columns in an undefined order.
   *
   * @since 2.0.0
   */
  def createDataFrame(rdd: JavaRDD[_], beanClass: Class[_]): DataFrame = {
    createDataFrame(rdd.rdd, beanClass)
  }

  /**
   * Applies a schema to a List of Java Beans.
   *
   * WARNING: Since there is no guaranteed ordering for fields in a Java Bean,
   *          SELECT * queries will return the columns in an undefined order.
   * @since 1.6.0
   */
  def createDataFrame(data: java.util.List[_], beanClass: Class[_]): DataFrame = {
    val attrSeq = getSchema(beanClass)
    val beanInfo = Introspector.getBeanInfo(beanClass)
    val rows = SQLContext.beansToRows(data.asScala.iterator, beanInfo, attrSeq)
    Dataset.ofRows(self, LocalRelation(attrSeq, rows.toSeq))
  }

  /**
   * Convert a [[BaseRelation]] created for external data sources into a [[DataFrame]].
   *
   * @since 2.0.0
   */
  def baseRelationToDataFrame(baseRelation: BaseRelation): DataFrame = {
    Dataset.ofRows(self, LogicalRelation(baseRelation))
  }

  /* ------------------------------- *
   |  Methods for creating DataSets  |
   * ------------------------------- */

  /**
   * :: Experimental ::
   * 从给定类型的Seq创建Dataset，这个方法需要一个encoder(把T类型的jvm对象和Spark SQL内部对象互转)，encoder通常自动通过SparkSession的隐式转换中自动创建，
   也可以通过Encoders的静态方法显示创建。
   * == Example ==
   *
   * {{{
   *
   *   import spark.implicits._
   *   case class Person(name: String, age: Long)
   *   val data = Seq(Person("Michael", 29), Person("Andy", 30), Person("Justin", 19))
   *   val ds = spark.createDataset(data)
   *
   *   ds.show()
   *   // +-------+---+
   *   // |   name|age|
   *   // +-------+---+
   *   // |Michael| 29|
   *   // |   Andy| 30|
   *   // | Justin| 19|
   *   // +-------+---+
   * }}}
   *
   * @since 2.0.0
   */
  @Experimental
  def createDataset[T : Encoder](data: Seq[T]): Dataset[T] = {
    val enc = encoderFor[T]
    val attributes = enc.schema.toAttributes
    val encoded = data.map(d => enc.toRow(d).copy())
    val plan = new LocalRelation(attributes, encoded)
    Dataset[T](self, plan)
  }

  /**
   * :: Experimental ::
   * Creates a [[Dataset]] from an RDD of a given type. This method requires an
   * encoder (to convert a JVM object of type `T` to and from the internal Spark SQL representation)
   * that is generally created automatically through implicits from a `SparkSession`, or can be
   * created explicitly by calling static methods on [[Encoders]].
   *
   * @since 2.0.0
   */
  @Experimental
  def createDataset[T : Encoder](data: RDD[T]): Dataset[T] = {
    val enc = encoderFor[T]
    val attributes = enc.schema.toAttributes
    val encoded = data.map(d => enc.toRow(d))
    val plan = LogicalRDD(attributes, encoded)(self)
    Dataset[T](self, plan)
  }

  /**
   * :: Experimental ::
   * Creates a [[Dataset]] from a [[java.util.List]] of a given type. This method requires an
   * encoder (to convert a JVM object of type `T` to and from the internal Spark SQL representation)
   * that is generally created automatically through implicits from a `SparkSession`, or can be
   * created explicitly by calling static methods on [[Encoders]].
   *
   * == Java Example ==
   *
   * {{{
   *     List<String> data = Arrays.asList("hello", "world");
   *     Dataset<String> ds = spark.createDataset(data, Encoders.STRING());
   * }}}
   *
   * @since 2.0.0
   */
  @Experimental
  def createDataset[T : Encoder](data: java.util.List[T]): Dataset[T] = {
    createDataset(data.asScala)
  }

  /**
   * :: Experimental ::
   * Creates a [[Dataset]] with a single [[LongType]] column named `id`, containing elements
   * in a range from 0 to `end` (exclusive) with step value 1.
   *
   * @since 2.0.0
   */
  @Experimental
  def range(end: Long): Dataset[java.lang.Long] = range(0, end)

  /**
   * :: Experimental ::
   * Creates a [[Dataset]] with a single [[LongType]] column named `id`, containing elements
   * in a range from `start` to `end` (exclusive) with step value 1.
   *
   * @since 2.0.0
   */
  @Experimental
  def range(start: Long, end: Long): Dataset[java.lang.Long] = {
    range(start, end, step = 1, numPartitions = sparkContext.defaultParallelism)
  }

  /**
   * :: Experimental ::
   * Creates a [[Dataset]] with a single [[LongType]] column named `id`, containing elements
   * in a range from `start` to `end` (exclusive) with a step value.
   *
   * @since 2.0.0
   */
  @Experimental
  def range(start: Long, end: Long, step: Long): Dataset[java.lang.Long] = {
    range(start, end, step, numPartitions = sparkContext.defaultParallelism)
  }

  /**
   * :: Experimental ::
   * Creates a [[Dataset]] with a single [[LongType]] column named `id`, containing elements
   * in a range from `start` to `end` (exclusive) with a step value, with partition number
   * specified.
   *
   * @since 2.0.0
   */
  @Experimental
  def range(start: Long, end: Long, step: Long, numPartitions: Int): Dataset[java.lang.Long] = {
    new Dataset(self, Range(start, end, step, numPartitions), Encoders.LONG)
  }

  /**
   * Creates a [[DataFrame]] from an RDD[Row].
   * User can specify whether the input rows should be converted to Catalyst rows.
   */
  private[sql] def internalCreateDataFrame(
      catalystRows: RDD[InternalRow],
      schema: StructType): DataFrame = {
    // TODO: use MutableProjection when rowRDD is another DataFrame and the applied
    // schema differs from the existing schema on any field data type.
    val logicalPlan = LogicalRDD(schema.toAttributes, catalystRows)(self)
    Dataset.ofRows(self, logicalPlan)
  }

  /**
   * Creates a [[DataFrame]] from an RDD[Row].
   * User can specify whether the input rows should be converted to Catalyst rows.
   */
  private[sql] def createDataFrame(
      rowRDD: RDD[Row],
      schema: StructType,
      needsConversion: Boolean) = {
    // TODO: use MutableProjection when rowRDD is another DataFrame and the applied
    // schema differs from the existing schema on any field data type.
    val catalystRows = if (needsConversion) {
      val encoder = RowEncoder(schema)
      rowRDD.map(encoder.toRow)
    } else {
      rowRDD.map{r: Row => InternalRow.fromSeq(r.toSeq)}
    }
    val logicalPlan = LogicalRDD(schema.toAttributes, catalystRows)(self)
    Dataset.ofRows(self, logicalPlan)
  }


  /* ------------------------- *
   |  Catalog-related methods 
   |  catalog相关方法
   * ------------------------- */

  /**
   * Interface through which the user may create, drop, alter or query underlying
   * databases, tables, functions etc.
     用户可以create /drop/alter/query 数据库、表、函数等接口
   *
   * @since 2.0.0
   */
  @transient lazy val catalog: Catalog = new CatalogImpl(self)

  /**
   * Returns the specified table as a [[DataFrame]].
   *
   * @since 2.0.0
   */
  def table(tableName: String): DataFrame = {
    table(sessionState.sqlParser.parseTableIdentifier(tableName))
  }

  private[sql] def table(tableIdent: TableIdentifier): DataFrame = {
    Dataset.ofRows(self, sessionState.catalog.lookupRelation(tableIdent))
  }

  /* ----------------- *
   |  Everything else  |
   * ----------------- */

  /**
   * 执行一个SQL，返回DataFrame，sql解析等方言可以通过spark.sql.dialect配置
   * @since 2.0.0
   */
  def sql(sqlText: String): DataFrame = {
    Dataset.ofRows(self, sessionState.sqlParser.parsePlan(sqlText))
  }

  /**
   * Returns a [[DataFrameReader]] that can be used to read non-streaming data in as a
   * [[DataFrame]].
   * {{{
   *   sparkSession.read.parquet("/path/to/file.parquet")
   *   sparkSession.read.schema(schema).json("/path/to/file.json")
   * }}}
   *
   * @since 2.0.0
   */
  def read: DataFrameReader = new DataFrameReader(self)

  /**
   * :: Experimental ::
   * Returns a [[DataStreamReader]] that can be used to read streaming data in as a [[DataFrame]].
   * {{{
   *   sparkSession.readStream.parquet("/path/to/directory/of/parquet/files")
   *   sparkSession.readStream.schema(schema).json("/path/to/directory/of/json/files")
   * }}}
   *
   * @since 2.0.0
   */
  @Experimental
  def readStream: DataStreamReader = new DataStreamReader(self)


  // scalastyle:off
  // Disable style checker so "implicits" object can start with lowercase i
  /**
   * :: Experimental ::
   * (Scala-specific) Implicit methods available in Scala for converting
   * common Scala objects into [[DataFrame]]s.
   *
   * {{{
   *   val sparkSession = SparkSession.builder.getOrCreate()
   *   import sparkSession.implicits._
   * }}}
   *
   * @since 2.0.0
   */
  @Experimental
  object implicits extends SQLImplicits with Serializable {
    protected override def _sqlContext: SQLContext = SparkSession.this.sqlContext
  }
  // scalastyle:on

  /**
   * Stop the underlying [[SparkContext]].
   *
   * @since 2.0.0
   */
  def stop(): Unit = {
    sparkContext.stop()
  }

  /**
   * Parses the data type in our internal string representation. The data type string should
   * have the same format as the one generated by `toString` in scala.
   * It is only used by PySpark.
   */
  protected[sql] def parseDataType(dataTypeString: String): DataType = {
    DataType.fromJson(dataTypeString)
  }

  /**
   * Apply a schema defined by the schemaString to an RDD. It is only used by PySpark.
   */
  private[sql] def applySchemaToPythonRDD(
      rdd: RDD[Array[Any]],
      schemaString: String): DataFrame = {
    val schema = DataType.fromJson(schemaString).asInstanceOf[StructType]
    applySchemaToPythonRDD(rdd, schema)
  }

  /**
   * Apply a schema defined by the schema to an RDD. It is only used by PySpark.
   */
  private[sql] def applySchemaToPythonRDD(
      rdd: RDD[Array[Any]],
      schema: StructType): DataFrame = {
    val rowRdd = rdd.map(r => python.EvaluatePython.fromJava(r, schema).asInstanceOf[InternalRow])
    Dataset.ofRows(self, LogicalRDD(schema.toAttributes, rowRdd)(self))
  }

  /**
   * Returns a Catalyst Schema for the given java bean class.
   */
  private def getSchema(beanClass: Class[_]): Seq[AttributeReference] = {
    val (dataType, _) = JavaTypeInference.inferDataType(beanClass)
    dataType.asInstanceOf[StructType].fields.map { f =>
      AttributeReference(f.name, f.dataType, f.nullable)()
    }
  }

}


/**
伴生对象
*/
object SparkSession {

  /**
   * Builder for [[SparkSession]].
   */
  class Builder extends Logging {

    private[this] val options = new scala.collection.mutable.HashMap[String, String]

    private[this] var userSuppliedContext: Option[SparkContext] = None

    private[spark] def sparkContext(sparkContext: SparkContext): Builder = synchronized {
      userSuppliedContext = Option(sparkContext)
      this
    }

    /**
     * Sets a name for the application, which will be shown in the Spark web UI.
     * If no application name is set, a randomly generated name will be used.
     *
     * @since 2.0.0
     */
    def appName(name: String): Builder = config("spark.app.name", name)

    /**
     * Sets a config option. Options set using this method are automatically propagated to
     * both [[SparkConf]] and SparkSession's own configuration.
     *
     * @since 2.0.0
     */
    def config(key: String, value: String): Builder = synchronized {
      options += key -> value
      this
    }

    /**
     * Sets a config option. Options set using this method are automatically propagated to
     * both [[SparkConf]] and SparkSession's own configuration.
     *
     * @since 2.0.0
     */
    def config(key: String, value: Long): Builder = synchronized {
      options += key -> value.toString
      this
    }

    /**
     * Sets a config option. Options set using this method are automatically propagated to
     * both [[SparkConf]] and SparkSession's own configuration.
     *
     * @since 2.0.0
     */
    def config(key: String, value: Double): Builder = synchronized {
      options += key -> value.toString
      this
    }

    /**
     * Sets a config option. Options set using this method are automatically propagated to
     * both [[SparkConf]] and SparkSession's own configuration.
     *
     * @since 2.0.0
     */
    def config(key: String, value: Boolean): Builder = synchronized {
      options += key -> value.toString
      this
    }

    /**
     * Sets a list of config options based on the given [[SparkConf]].
     *
     * @since 2.0.0
     */
    def config(conf: SparkConf): Builder = synchronized {
      conf.getAll.foreach { case (k, v) => options += k -> v }
      this
    }

    /**
     * Sets the Spark master URL to connect to, such as "local" to run locally, "local[4]" to
     * run locally with 4 cores, or "spark://master:7077" to run on a Spark standalone cluster.
     *
     * @since 2.0.0
     */
    def master(master: String): Builder = config("spark.master", master)

    /**
     * Enables Hive support, including connectivity to a persistent Hive metastore, support for
     * Hive serdes, and Hive user-defined functions.
     *
     * @since 2.0.0
     */
    def enableHiveSupport(): Builder = synchronized {
      if (hiveClassesArePresent) {
        config(CATALOG_IMPLEMENTATION.key, "hive")
      } else {
        throw new IllegalArgumentException(
          "Unable to instantiate SparkSession with Hive support because " +
            "Hive classes are not found.")
      }
    }

    /**
     * Gets an existing [[SparkSession]] or, if there is no existing one, creates a new
     * one based on the options set in this builder.
     *
     * This method first checks whether there is a valid thread-local SparkSession,
     * and if yes, return that one. It then checks whether there is a valid global
     * default SparkSession, and if yes, return that one. If no valid global default
     * SparkSession exists, the method creates a new SparkSession and assigns the
     * newly created SparkSession as the global default.
     *
     * In case an existing SparkSession is returned, the config options specified in
     * this builder will be applied to the existing SparkSession.
     *
       1、检查有效的thread-local Sparksession，有则直接返回它
       2、检查有效的全局Sparksession 有则直接返回它
       3、否则创建一个，并将它设置为全局SparkSession
       4、以上所有情况，都会把builder中的配置应用到sparksession中
       
     * @since 2.0.0
     */
    def getOrCreate(): SparkSession = synchronized {
      // Get the session from current thread's active session.
      var session = activeThreadSession.get()
      if ((session ne null) && !session.sparkContext.isStopped) {
        options.foreach { case (k, v) => session.conf.set(k, v) }
        if (options.nonEmpty) {
          logWarning("Using an existing SparkSession; some configuration may not take effect.")
        }
        return session
      }

      // Global synchronization so we will only set the default session once.
      SparkSession.synchronized {
        // If the current thread does not have an active session, get it from the global session.
        session = defaultSession.get()
        if ((session ne null) && !session.sparkContext.isStopped) {
          options.foreach { case (k, v) => session.conf.set(k, v) }
          if (options.nonEmpty) {
            logWarning("Using an existing SparkSession; some configuration may not take effect.")
          }
          return session
        }

        // No active nor global default session. Create a new one.
        val sparkContext = userSuppliedContext.getOrElse {
          // set app name if not given
          val randomAppName = java.util.UUID.randomUUID().toString
          val sparkConf = new SparkConf()
          options.foreach { case (k, v) => sparkConf.set(k, v) }
          if (!sparkConf.contains("spark.app.name")) {
            sparkConf.setAppName(randomAppName)
          }
          val sc = SparkContext.getOrCreate(sparkConf)
          // maybe this is an existing SparkContext, update its SparkConf which maybe used
          // by SparkSession
          options.foreach { case (k, v) => sc.conf.set(k, v) }
          if (!sc.conf.contains("spark.app.name")) {
            sc.conf.setAppName(randomAppName)
          }
          sc
        }
        session = new SparkSession(sparkContext)
        options.foreach { case (k, v) => session.conf.set(k, v) }
        defaultSession.set(session)

        // Register a successfully instantiated context to the singleton. This should be at the
        // end of the class definition so that the singleton is updated only if there is no
        // exception in the construction of the instance.
        sparkContext.addSparkListener(new SparkListener {
          override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit = {
            defaultSession.set(null)
            sqlListener.set(null)
          }
        })
      }

      return session
    }
  }

  /**
   * Creates a [[SparkSession.Builder]] for constructing a [[SparkSession]].
   *
   * @since 2.0.0
   */
  def builder(): Builder = new Builder

  /**
   * Changes the SparkSession that will be returned in this thread and its children when
   * SparkSession.getOrCreate() is called. This can be used to ensure that a given thread receives
   * a SparkSession with an isolated session, instead of the global (first created) context.
   *
   * @since 2.0.0
   */
  def setActiveSession(session: SparkSession): Unit = {
    activeThreadSession.set(session)
  }

  /**
   * Clears the active SparkSession for current thread. Subsequent calls to getOrCreate will
   * return the first created context instead of a thread-local override.
   *
   * @since 2.0.0
   */
  def clearActiveSession(): Unit = {
    activeThreadSession.remove()
  }

  /**
   * Sets the default SparkSession that is returned by the builder.
   *
   * @since 2.0.0
   */
  def setDefaultSession(session: SparkSession): Unit = {
    defaultSession.set(session)
  }

  /**
   * Clears the default SparkSession that is returned by the builder.
   *
   * @since 2.0.0
   */
  def clearDefaultSession(): Unit = {
    defaultSession.set(null)
  }

  private[sql] def getActiveSession: Option[SparkSession] = Option(activeThreadSession.get)

  private[sql] def getDefaultSession: Option[SparkSession] = Option(defaultSession.get)

  /** A global SQL listener used for the SQL UI. */
  private[sql] val sqlListener = new AtomicReference[SQLListener]()

  ////////////////////////////////////////////////////////////////////////////////////////
  // Private methods from now on
  ////////////////////////////////////////////////////////////////////////////////////////

  /** The active SparkSession for the current thread. */
  private val activeThreadSession = new InheritableThreadLocal[SparkSession]

  /** Reference to the root SparkSession. */
  private val defaultSession = new AtomicReference[SparkSession]

  private val HIVE_SHARED_STATE_CLASS_NAME = "org.apache.spark.sql.hive.HiveSharedState"
  private val HIVE_SESSION_STATE_CLASS_NAME = "org.apache.spark.sql.hive.HiveSessionState"

  private def sharedStateClassName(conf: SparkConf): String = {
    conf.get(CATALOG_IMPLEMENTATION) match {
      case "hive" => HIVE_SHARED_STATE_CLASS_NAME
      case "in-memory" => classOf[SharedState].getCanonicalName
    }
  }

  private def sessionStateClassName(conf: SparkConf): String = {
    conf.get(CATALOG_IMPLEMENTATION) match {
      case "hive" => HIVE_SESSION_STATE_CLASS_NAME
      case "in-memory" => classOf[SessionState].getCanonicalName
    }
  }

  /**
   * Helper method to create an instance of [[T]] using a single-arg constructor that
   * accepts an [[Arg]].
   */
  private def reflect[T, Arg <: AnyRef](
      className: String,
      ctorArg: Arg)(implicit ctorArgTag: ClassTag[Arg]): T = {
    try {
      val clazz = Utils.classForName(className)
      val ctor = clazz.getDeclaredConstructor(ctorArgTag.runtimeClass)
      ctor.newInstance(ctorArg).asInstanceOf[T]
    } catch {
      case NonFatal(e) =>
        throw new IllegalArgumentException(s"Error while instantiating '$className':", e)
    }
  }

  /**
   * Return true if Hive classes can be loaded, otherwise false.
   */
  private[spark] def hiveClassesArePresent: Boolean = {
    try {
      Utils.classForName(HIVE_SESSION_STATE_CLASS_NAME)
      Utils.classForName(HIVE_SHARED_STATE_CLASS_NAME)
      Utils.classForName("org.apache.hadoop.hive.conf.HiveConf")
      true
    } catch {
      case _: ClassNotFoundException | _: NoClassDefFoundError => false
    }
  }

}


```

