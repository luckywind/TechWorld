[中文网](https://ptorch.com/)

[pytorch英文官网](https://pytorch.org/docs/stable/index.html)

[指南](https://zhuanlan.zhihu.com/p/29024978)

[guide](https://blog.paperspace.com/ultimate-guide-to-pytorch/)

# pytorch

- PyTorch is an open-source library that consists of three core components: a tensor library, automatic differentiation functions, and deep learning utilities.
- PyTorch’s tensor library is similar to array libraries like NumPy
- In the context of PyTorch, tensors are array-like data structures to represent scalars, vectors, matrices, and higher-dimensional arrays.
- PyTorch tensors can be executed on the CPU, but one major advantage of PyTorch’s tensor format is its GPU support to accelerate computations.
- The automatic differentiation (autograd) capabilities in PyTorch allow us to conveniently train neural networks using backpropagation without manually deriving gradients.
- The deep learning utilities in PyTorch provide building blocks for creating custom deep neural networks.
- PyTorch includes `Dataset` and `DataLoader` classes to set up efficient data loading pipelines.
- It’s easiest to train models on a CPU or single GPU.
- Using DistributedDataParallel is the simplest way in PyTorch to accelerate the training if multiple GPUs are available.

## numpy

[参考](https://learn.lianglianglee.com/%e4%b8%93%e6%a0%8f/PyTorch%e6%b7%b1%e5%ba%a6%e5%ad%a6%e4%b9%a0%e5%ae%9e%e6%88%98/02%20NumPy%ef%bc%88%e4%b8%8a%ef%bc%89%ef%bc%9a%e6%a0%b8%e5%bf%83%e6%95%b0%e6%8d%ae%e7%bb%93%e6%9e%84%e8%af%a6%e8%a7%a3.md)

numpy 数组：
**创建**：

1. arr_2_d = np.asarray([[1, 2], [3, 4]])，可以指定类型
2. ones(shape=(2,3), dtype='int32')， zeros 用法一样
3. arange([start, ]stop, [step, ]dtype=None)
4. linspace（start, stop, num=50, endpoint=True, retstep=False, dtype=None）

**属性**

ndim 维度

shape 形状元组， reshape 可以修改

size 总数

dtype 类型

**沿轴聚合**

sum(interest_score, axis=0)  沿着 0 轴(行)的方向求和，其实当axis=i时，就是按照第i个轴的方向进行计算的，或者可以理解为第i个轴的数据将会被折叠或聚合到一起。

**数据加载**

图片的加载：

1. Pillow 方式
2. OpenCV 方式

**模型评估**

np.argmax(probs) 最大/最小值对应的索引

argsort：数组排序后返回原数组的索引

## Tensor

- In the context of PyTorch, tensors are array-like data structures to represent scalars, vectors, matrices, and higher-dimensional arrays.

用Rank（秩）表示阶数，0 阶张量就是标量、1 阶张量就是向量、2 阶张量就是矩阵。

### 创建

1. torch.tensor(data, dtype=None, device=None,requires_grad=False)
2. torch.from_numpy(ndarry)
3. 特殊的
   - torch.zeros(*size, dtype=None...)
   - torch.eye(size, dtype=None...)
   - torch.ones(size, dtype=None...)
   - torch.rand(size) torch.randn(size) torch.normal(mean, std, size) torch.randint(low, high, size）

### 转换

- int
  a = torch.tensor(1) b = a.item()

- list / Numpy
  ```python
  a = [1, 2, 3]
  b = torch.tensor(a)
  c = b.numpy().tolist()
  ```

- CPU 与GPU 的Tensor 之间的转换

  ```python
  CPU->GPU: data.cuda()
  GPU->CPU: data.cpu()
  ```

### 常用操作

size()  类似shape

numel 数量

permute 转置

view 变更维度，但不能处理permute 导致的内存不连续张量，reshape 可以

squeeze()/unsqueeze() 增减维度

- 变形

1. torch.cat(tensors, dim = 0, out = None) 按dim 指定的维度连接
2. torch.stack(inputs, dim=0)  按dim 方向建立新的维度来拼接

- 切分

1. torch.chunk(input, chunks, dim=0)进行尽可能平均的划分。
2. torch.split(tensor, split_size_or_sections, dim=0) 自定义切分
3. torch.unbind(input, dim=0)   降维切分

- 索引操作

1. torch.index_select(tensor, dim, index) 基于给定的索引来进行数据提取 
2. torch.masked_select(input, mask, out=None) mask须跟input张量有相同数量的元素数目，但形状或维度不需要相同。

## Torchvision: 获取数据

### Dataset 类

PyTorch中的Dataset类是一个抽象类，它可以用来表示数据集。我们通过继承Dataset类来自定义数据集的格式、大小和其它属性，后面就可以供DataLoader类直接使用。

```python
import torch
from torch.utils.data import Dataset

class MyDataset(Dataset):
    # 构造函数
    def __init__(self, data_tensor, target_tensor):
        self.data_tensor = data_tensor
        self.target_tensor = target_tensor
    # 返回数据集大小
    def __len__(self):
        return self.data_tensor.size(0)
    # 返回索引的数据与标签
    def __getitem__(self, index):
        return self.data_tensor[index], self.target_tensor[index]
```

使用

```python
# 生成数据
data_tensor = torch.randn(10, 3)
target_tensor = torch.randint(2, (10,)) # 标签是0或1

# 将数据封装成Dataset
my_dataset = MyDataset(data_tensor, target_tensor)

# 查看数据集大小
print('Dataset size:', len(my_dataset))
'''
输出：
Dataset size: 10
'''

# 使用索引调用数据
print('tensor_data[0]: ', my_dataset[0])
'''
输出:
tensor_data[0]:  (tensor([ 0.4931, -0.0697,  0.4171]), tensor(0))
'''
```

### Dataloader类

DataLoader是一个迭代器，最基本的使用方法就是传入一个Dataset对象，它会根据参数 batch_size的值生成一个batch的数据，节省内存的同时，它还可以实现多进程、数据打乱等处理。

```python
from torch.utils.data import DataLoader
tensor_dataloader = DataLoader(dataset=my_dataset, # 传入的数据集, 必须参数
                               batch_size=2,       # 输出的batch大小
                               shuffle=True,       # 数据是否打乱
                               num_workers=0)      # 进程数, 0表示只有主进程

# 以循环形式输出
for data, target in tensor_dataloader: 
    print(data, target)
'''
输出:
tensor([[-0.1781, -1.1019, -0.1507],
        [-0.6170,  0.2366,  0.1006]]) tensor([0, 0])
tensor([[ 0.9451, -0.4923, -1.8178],
        [-0.4046, -0.5436, -1.7911]]) tensor([0, 0])
tensor([[-0.4561, -1.2480, -0.3051],
        [-0.9738,  0.9465,  0.4812]]) tensor([1, 0])
tensor([[ 0.0260,  1.5276,  0.1687],
        [ 1.3692, -0.0170, -1.6831]]) tensor([1, 0])
tensor([[ 0.0515, -0.8892, -0.1699],
        [ 0.4931, -0.0697,  0.4171]]) tensor([1, 0])
'''
 
# 输出一个batch
print('One batch tensor data: ', iter(tensor_dataloader).next())
'''
输出:
One batch tensor data:  [tensor([[ 0.9451, -0.4923, -1.8178],
        [-0.4046, -0.5436, -1.7911]]), tensor([0, 0])]
'''
```

### Torchvision

Torchvision 库就是**常用数据集+常见网络模型+常用图像处理方法**。

#### 数据加载

#### 数据增强

1. 数据类型转换
   transforms.ToTensor() ：  `transforms.ToTensor()(img)`
   transforms.ToPILImage(mode=None): ` transforms.ToPILImage()(img1) `

2. Resize操作
   `torchvision.transforms.Resize(size, interpolation=2)  `

   size：期望输出的尺寸。如果 size 是一个像 (h, w) 这样的元组，则图像输出尺寸将与之匹配。如果 size 是一个 int 类型的整数，图像较小的边将被匹配到该整数，另一条边按比例缩放。

   interpolation：插值算法，int类型，默认为2，表示 PIL.Image.BILINEAR。

> 在我们训练时，通常要把图片resize到一定的大小，比如说128x128，256x256这样的。如果直接给定resize后的高与宽，是没有问题的。但如果设定的是一个int型，较长的边就会按比例缩放。

```python
from PIL import Image
from torchvision import transforms 

# 定义一个Resize操作
resize_img_oper = transforms.Resize((200,200), interpolation=2)

# 原图
orig_img = Image.open('jk.jpg') 
display(orig_img)

# Resize操作后的图
img = resize_img_oper(orig_img)
display(img)
```

3. Crop裁剪
   - torchvision.transforms.CenterCrop(size)  中心裁剪
   - torchvision.transforms.RandomCrop(size, padding=None)  随机裁剪
   - torchvision.transforms.FiveCrop(size) 裁剪为5块

4. Flip 翻转
   - torchvision.transforms.RandomHorizontalFlip(p=0.5) 随机水平翻转
   - torchvision.transforms.RandomVerticalFlip(p=0.5)  随机垂直翻转

5. 标准化-只有Tensor可以
   torchvision.transforms.Normalize(mean, std, inplace=False) 

   mean：表示各通道的均值；

   std：表示各通道的标准差；

   inplace：表示是否原地操作，默认为否。

6. Compose: 变换的组合
   torchvision.transforms.Compose(transforms)

Compose类是未来我们在实际项目中经常要使用到的类，结合`torchvision.datasets`包，就可以在读取数据集的时候做图像变换与数据增强操作。

7. make_grid 网格拼接图片
8. save_img 将Tensor 保存为图片

#### 网络模型

`torchvision.models` 模块中包含了常见网络模型结构的定义，这些网络模型可以解决以下四大类问题：图像分类、图像分割、物体检测和视频分类。

1. 创建一个模型
   ```python
   import torchvision.models as models
   googlenet = models.googlenet()
   ```

2. 导入训练好的模型
   ```python
   import torchvision.models as models
   googlenet = models.googlenet(pretrained=True)
   ```

   在实例化一个预训练好的模型时，模型的参数会被下载至缓存目录中，下载一次后不需要重复下载。这个缓存目录可以通过环境变量TORCH_MODEL_ZOO来指定

3. 模型微调
   问题是数据集中狗的类别很多，但数据却不多。你发现从零开始训练一个图片分类模型，但这样模型效果很差，并且很容易过拟合。这种问题该如何解决呢？于是你想到了使用迁移学习，可以用已经在ImageNet数据集上训练好的模型来达成你的目的。使用经过预训练的模型，要比使用随机初始化的模型训练**效果更好**，**更容易收敛**，并且**训练速度更快**，在小数据集上也能取得比较理想的效果。通常我们需要调整输出类别的数量。

   ```python
   import torch
   import torchvision.models as models
   
   # 加载预训练模型
   googlenet = models.googlenet(pretrained=True)
   
   # 提取分类层的输入参数
   fc_in_features = googlenet.fc.in_features
   print("fc_in_features:", fc_in_features)
   
   # 查看分类层的输出参数
   fc_out_features = googlenet.fc.out_features
   print("fc_out_features:", fc_out_features)
   
   # 修改预训练模型的输出分类数(在图像分类原理中会具体介绍torch.nn.Linear)
   googlenet.fc = torch.nn.Linear(fc_in_features, 10)
   '''
   输出：
   fc_in_features: 1024
   fc_out_features: 1000
   '''
   ```

   

## 卷积

卷积神经网络，两个优秀特点：

1. 稀疏连接：让学习的参数变得很少
2. 平移不变性：不关心物体出现在图像中什么位置

**卷积的计算**

- **最简单的情况**：

![image-20250829143130591](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250829143130591.png)

- **标准卷积**
  输入的特征有m个通道，宽为w，高为h；输出有n个特征图，宽为\(w^{\\prime}\)，高为\(h^{\\prime}\)；卷积核的大小为kxk。 根据输出特征图的通道数由**卷积核的个数决定**的，所以说卷积核的个数为n。根据卷积计算的定义，**输入特征图有m个通道，所以每个卷积核里要也要有m个通道**。

![image-20250829145526258](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250829145526258.png)

卷积核1与全部输入特征进行卷积计算，就获得了输出特征图中第1个通道的数据，卷积核2与全部输入特征图进行计算获得输出特征图中第2个通道的数据。以此类推，最终就能计算输出的n个特征图(也可以说是通道)。

那么当输入有多个通道时，如何计算输出的一个通道呢？ 其实计算方式类似，输入特征的每一个通道与卷积核中对应通道的数据按我们之前讲过的方式进行卷积计算，也就是输入特征图中第i个特征图与卷积核中的第i个通道的数据进行卷积。这样计算后会生成**m**个特征图，然后将这m个特征图按对应位置求和即可，求和后m个特征图合并为输出特征中一个通道的特征图。

### Padding

如果不补零且步长（stride）为1的情况下，当有多层卷积层时，特征图会一点点变小。如果我们希望有更多层卷积层来提取更加丰富的信息时，就可以让特征图变小的速度稍微慢一些，这个时候就可以考虑补零。

这个补零的操作就叫做padding，padding等于1就是补一圈的零，等于2就是补两圈的零

当滑动到特征图最右侧时，发现输出的特征图的宽与输入的特征图的宽不一致，它会自动补零，直到输出特征图的宽与输入特征图的宽一致为止。如下图所示：

![image-20250901101216969](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250901101216969.png)

### PyTorch 中的卷积

在torch.nn模块中，关于今天介绍的卷积操作有nn.Conv1d、nn.Conv2d与nn.Conv3d三个类。

```python
# Conv2d类
class torch.nn.Conv2d(in_channels, 
                      out_channels, 
                      kernel_size, 
                      stride=1, 
                      padding=0, 
                      dilation=1, 
                      groups=1, 
                      bias=True, 
                      padding_mode='zeros', 
                      device=None, 
                      dtype=None)

```

- in_channels/out_channels 输入/输出通道数，都是int
- kernel_size是卷积核的大小，数据类型为int或tuple，需要注意的是只给定卷积核的高与宽即可，在标准卷积的讲解中kernel_size为k。

- stride为滑动的步长，数据类型为int或tuple，默认是1，在前面的例子中步长都为1。

- padding为补零的方式，注意**当padding为’valid’或’same’时，stride必须为1**。

对于kernel_size、stride、padding都可以是tuple类型，当为tuple类型时，第一个维度用于height的信息，第二个维度时用于width的信息。

- bias是否使用偏移项。
- dilation空洞卷积
- groups深度可分离卷积 

**Pytorch输入tensor的维度信息是(batch_size, 通道数，高，宽)**, 所以要对tensor 的 维度进行修改，

1. unsqueeze 指定在哪个位置添加维度

`input_feat = torch.tensor([[4, 1, 7, 5], [4, 4, 2, 5], [7, 7, 2, 4], [1, 0, 2, 4]], dtype=torch.float32).unsqueeze(0).unsqueeze(0) `

2. 修改维度
   `torch.Size([1, 1, 4, 4])`

3. 计算卷积
   ```python
   conv2d = nn.Conv2d(1, 1, (2, 2), stride=1, padding='same', bias=True)
   output = conv2d(input_feat)
   ```

   

### 深度可分离卷积 

谷歌在MobileNet v1中提出的一种轻量化卷积。**简单来说，深度可分离卷积就是我们刚才所说的在效果近似相同的情况下，需要的计算量更少**。由 **Depthwise**（DW）和 **Pointwise**（PW）这两部分卷积组合而成的。

深度可分离卷积的目标是轻量化标准卷积计算的，所以它是可以来替换标准卷积的，这也意味着原卷积计算的输出尺寸是什么样，替换后的输出尺寸也要保持一致。

- DW 卷积

DW卷积就是有m个卷积核的卷积，每个卷积核中的通道数为1，这m个卷积核分别与输入特征图对应的通道数据做卷积运算，所以DW卷积的输出是有**m**个通道的特征图。通常来说，DW卷积核的大小是3x3的。

- PW 卷积(逐点卷积)

在卷积神经网络中，我们经常可以看到使用1x1的卷积，1x1的卷积主要作用就是升维与降维。所以，在DW的输出之后的PW卷积，就是n个卷积核的1x1的卷积，每个卷积核中有m个通道的卷积数据。

![image-20250901110644410](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250901110644410.png)

经过这样的DW与PW的组合，我们就可以获得一个与标准卷积有同样输出尺寸的轻量化卷积啦

#### PyTorch 中实现

groups参数的作用就是控制输入特征图与输出特征图的分组情况。

- 当groups等于1的时候，就是我们上一节课讲的标准卷积，而groups=1也是nn.Conv2d的默认值。

- 当groups不等于1的时候，会将输入特征图分成groups个组，每个组都有自己对应的卷积核，然后分组卷积，获得的输出特征图也是有groups个分组的。需要注意的是，**groups不为1的时候，groups必须能整除in_channels和out_channels**。

当groups等于in_channels时，就是我们的DW卷积啦。







