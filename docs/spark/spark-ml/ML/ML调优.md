[官方文档](https://spark.apache.org/docs/latest/ml-tuning.html)

# 模型选择

建议对整个Pipeline进行调优，MLlib支持通过 [`CrossValidator`](https://spark.apache.org/docs/latest/api/scala/org/apache/spark/ml/tuning/CrossValidator.html) 和 [`TrainValidationSplit`](https://spark.apache.org/docs/latest/api/scala/org/apache/spark/ml/tuning/TrainValidationSplit.html)这样的工具进行模型选择(超参数调优)，这些工具需要以下:

1. Estimator: 需要调优的算法或者Pipleline
2. ParamMaps: 搜索参数网格
3. Evaluator: 评估器

工作流程：

1. 把训练集和测试集拆分为多份
2. 对每个(train, test) 遍历参数map
   1. 对每个参数集，fit Estimator得到模型，并使用Evaluator进行评估
3. 选择最好的模型



可以使用 [`ParamGridBuilder`](https://spark.apache.org/docs/latest/api/scala/org/apache/spark/ml/tuning/ParamGridBuilder.html)构建参数网格，默认参数搜索是串行的，但可以设置parallelism来并行搜索，建议10以内。





## CrossValidator

CrossValidator首先把数据集拆分为多个folds,即(train,test)对。例如k=3时，每个fold使用2/3的数据训练，1/3的数据用于测试。 对于每个参数集，CrossValidator取三个fold的指标均值。

注意： 这个计算量非常大，参数网组合x k



```scala
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("words")
    val hashingTF = new HashingTF()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("features")
    val lr = new LogisticRegression()
      .setMaxIter(10)
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, hashingTF, lr))

    // We use a ParamGridBuilder to construct a grid of parameters to search over.
    // With 3 values for hashingTF.numFeatures and 2 values for lr.regParam,
    // this grid will have 3 x 2 = 6 parameter settings for CrossValidator to choose from.
    val paramGrid = new ParamGridBuilder()
      .addGrid(hashingTF.numFeatures, Array(10, 100, 1000))
      .addGrid(lr.regParam, Array(0.1, 0.01))
      .build()

    // We now treat the Pipeline as an Estimator, wrapping it in a CrossValidator instance.
    // This will allow us to jointly choose parameters for all Pipeline stages.
    // A CrossValidator requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.
    // Note that the evaluator here is a BinaryClassificationEvaluator and its default metric
    // is areaUnderROC.
    val cv = new CrossValidator()
      .setEstimator(pipeline)
      .setEvaluator(new BinaryClassificationEvaluator)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(3)  // 

    // Run cross-validation, and choose the best set of parameters.
    val cvModel = cv.fit(training)

```



## TrainValidationSplit

<font color=red>TrainValidationSplit只对每种参数集评估一次，计算量比CrossValidator小</font>，当数据集不够大时，可能效果不好。TrainValidationSplit使用trainRatio参数把数据集拆分为(training, test)。  TrainValidationSplit最终使用整个数据集和最优参数fit Estimator得到模型



```scala
    val lr = new LinearRegression()
        .setMaxIter(10)

    // We use a ParamGridBuilder to construct a grid of parameters to search over.
    // TrainValidationSplit will try all combinations of values and determine best model using
    // the evaluator.
    val paramGrid = new ParamGridBuilder()
      .addGrid(lr.regParam, Array(0.1, 0.01))
      .addGrid(lr.fitIntercept)
      .addGrid(lr.elasticNetParam, Array(0.0, 0.5, 1.0))
      .build()

    // In this case the estimator is simply the linear regression.
    // A TrainValidationSplit requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.
    val trainValidationSplit = new TrainValidationSplit()
      .setEstimator(lr)
      .setEvaluator(new RegressionEvaluator)
      .setEstimatorParamMaps(paramGrid)
      // 80% of the data will be used for training and the remaining 20% for validation.
      .setTrainRatio(0.8)

    // Run train validation split, and choose the best set of parameters.
    val model = trainValidationSplit.fit(training)
```

