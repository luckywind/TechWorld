[中文网](https://ptorch.com/)

[pytorch英文官网](https://pytorch.org/docs/stable/index.html)

[指南](https://zhuanlan.zhihu.com/p/29024978)

[guide](https://blog.paperspace.com/ultimate-guide-to-pytorch/)

# pytorch

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





