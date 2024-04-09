[API速查](https://www.showmeai.tech/article-detail/108)

# [SKLearn使用指南](https://juejin.cn/post/7077565783412310029#heading-18)

[性能度量](https://www.showmeai.tech/article-detail/186)

## 核心API

### 估计器

Sklearn 里万物皆估计器。估计器是个非常抽象的叫法，可把它不严谨的当成一个模型 (用来回归、分类、聚类、降维)，或当成一套流程 (预处理、网格最终)。

本节三大 API 其实都是估计器：

1. 估计器 (estimator) 当然是**估计器**
2. 预测器 (predictor) 是具有预测功能的**估计器**
3. 转换器 (transformer) 是具有转换功能的**估计器**

**定义**：任何可以基于数据集对一些参数进行估计的对象都被称为估计器。

1. 创建估计器：需要设置一组超参数

2. 拟合估计器：需要训练集。在有监督学习中的代码范式为

```python

# 有监督学习
from sklearn.xxx import SomeModel
# xxx 可以是 linear_model 或 ensemble 等

model = SomeModel( hyperparameter )
model.fit( X, y )
# 无监督学习
from sklearn.xxx import SomeModel
# xxx 可以是 cluster 或 decomposition 等

model = SomeModel( hyperparameter )
model.fit( X )
```

### 预测器

**定义：预测器在估计器上做了一个延展，延展出预测的功能**

估计器都有 fit() 方法，预测器都有 predict() 和 score() 方法，言外之意不是每个预测器都有 predict_proba() 和 decision_function() 方法

#### predict/predict_proba

对于分类问题，我们不仅想知道预测的类别是什么，有时还想知道预测该类别的信心如何。前者用 predict()，后者用 predict_proba()。

#### score/decision_function

- score() 返回的是分类准确率
- decision_function() 返回的是每个样例在每个类下的分数值

### 转换器

定义：转换器也是一种估计器，两者都带拟合功能，但估计器做完拟合来预测，而转换器做完拟合来转换。

核心点：估计器里 fit + predict，转换器里 fit + transform。

> LabelEncoder 和 OrdinalEncoder 都可以将字符转成数字，但是
> LabelEncoder  的输入是一维，比如 1d ndarray
> OrdinalEncoder  的输入是二维，比如 DataFrame

1. 分类特征数值化

   ```python
   enc = ['win','draw','lose','win']
   dec = ['draw','draw','win']
   
   
   from sklearn.preprocessing import LabelEncoder
   LE = LabelEncoder()
   print( LE.fit(enc) )
   print( LE.classes_ )
   print( LE.transform(dec) )
   
   
   from sklearn.preprocessing import OrdinalEncoder
   OE = OrdinalEncoder()
   enc_DF = pd.DataFrame(enc)
   dec_DF = pd.DataFrame(dec)
   print( OE.fit(enc_DF) )
   print( OE.categories_ )
   print( OE.transform(dec_DF) )
   ```

   

2. 规范化 (normalize) 或标准化 (standardize) 数值型变量

standardization   normalization



## 高级API

​          Sklearn 里核心 API 接口是估计器，那高级 API 接口就是元估计器 (meta-estimator)，即由很多基估计器 (base estimator) 组合成的估计器。元估计器把估计器当成参数。代码范式大概如下：

```python
 meta_model( base_model )
```

参数：
n_estimators： 基础估计器个数

estimators_：基础估计器列表



五大元估计器

```python
集成
ensemble.BaggingClassifier
ensemble.VotingClassifier
多分类
multiclass.OneVsOneClassifier
multiclass.OneVsRestClassifier
多输出
multioutput.MultiOutputClassifier
模型选择
model_selection.GridSearchCV
model_selection.RandomizedSearchCV
流水线
pipeline.Pipeline
```

### 集成ensemble

最常用的 Ensemble 估计器排列如下：

- `AdaBoostClassifier`: 逐步提升分类器
- `AdaBoostRegressor`: 逐步提升回归器
- `BaggingClassifier`: 装袋分类器
- `BaggingRegressor`: 装袋回归器
- `GradientBoostingClassifier`: 梯度提升分类器
- `GradientBoostingRegressor`: 梯度提升回归器
- `RandomForestClassifier`: 随机森林分类器
- `RandomForestRegressor`: 随机森林回归器
- `VotingClassifier`: 投票分类器
- `VotingRegressor`: 投票回归器

### 多分类/标签multiclass

sklearn.multiclass 可以处理多类别 (multi-class) 的多标签 (multi-label) 的分类问题。

### 多输出multioutput

sklearn.multioutput 可以处理多输出 (multi-output) 的分类问题。

多输出分类是多标签分类的泛化，在这里每一个标签可以是多类别 (大于两个类别) 的。一个例子就是预测图片每一个像素(标签) 的像素值是多少 (从 0 到 255 的 256 个类别)。



![图片](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/640.jpeg)



Multioutput 估计器有两个：

- MultiOutputRegressor`: 多输出回归  

- `MultiOutputClassifier`: 多输出分类

### Model Selection 模型选择估计器

模型选择 (Model Selction) 在机器学习非常重要，它主要用于评估模型表现，常见的 Model Selection 估计器有以下几个：

- `cross_validate`: 评估交叉验证的表现。
- `learning_curve`: 建立学习曲线。  
- `GridSearchCV`: 用交叉验证从网格中一组超参数搜索出最佳超参数。
- `RandomizedSearchCV`: 用交叉验证从一组随机超参数搜索出最佳超参数。

### **Pipeline 估计器**

Pipeline 将若干个估计器按顺序连在一起，比如

  特征提取 -> 降维 -> 拟合 -> 预测

在整个 Pipeline 中，它的属性永远和最后一个估计器属性一样

- 如果最后一个估计器是预测器，那么 Pipeline 是预测器
- 如果最后一个估计器是转换器，那么 Pipeline 是转换器

#### FeatureUnion

如果我们想在一个节点同时运行几个估计器，我们可用 FeatureUnion

```python
import numpy as np
import pandas as pd
from sklearn.impute import SimpleImputer
from sklearn.pipeline import Pipeline, FeatureUnion
from sklearn.preprocessing import MinMaxScaler, OneHotEncoder

from Xgboost.wangwangdui.pipe.dfselector import DataFrameSelector

d={'IQ':['high','avg','avg','low','high','avg','high','high',None],
   'temper':['good',None,'good','bad','bad','bad','bad',None,'bad'],
   'income':[50,40,30,5,7,10,9,np.NAN,12],
   'height':[1.68,1.83,1.77,np.NAN,1.9,1.65,1.88,np.NAN,1.75]}
X=pd.DataFrame(d)
categorical_features=['IQ','temper']
numeric_features=['income','height']
categorical_pipe=Pipeline([
   ('select',DataFrameSelector(categorical_features)),
   ('impute',SimpleImputer(missing_values=np.nan,strategy='most_frequent')),
   ('one_hot_encode',OneHotEncoder(sparse=False))])

numeric_pipe=Pipeline([
   ('select',DataFrameSelector(numeric_features)),
   ('impute',SimpleImputer(missing_values=np.nan,strategy='mean')),
   ('normalize',MinMaxScaler())])

full_pipe=FeatureUnion(
   transformer_list=[
      ('numeric_pipe',numeric_pipe),
      ('categorical_pipe',categorical_pipe)
   ]
)

X_proc = full_pipe.fit_transform(X)
print(X_proc)

```



## 评估

```python
from sklearn import metrics
metrics.accuracy_scor  准确率
```

