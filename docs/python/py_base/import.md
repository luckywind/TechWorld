[参考](https://docs.python.org/zh-cn/3/reference/import.html)

[参考2](https://www.biaodianfu.com/python-import.html)

# python模块、包、库、框架

1. **模块**是一种以.py为后缀的文件，在.py文件中定义了一些常量和函数。模块的名称是该.py文件的名称。模块的名称作为一个全局变量__name__的取值可以被其他模块获取或导入。

   > 系统自带了sys模块，自己的模块就不可命名为sys.py

2. **包**体现了模块的结构化管理思想，包由模块文件构成，将众多具有相关功能的模块文件结构化组合形成包。包是一个类似文件夹的东西，它里面包含了很多.py文件还有一个`__init__.py`文件，`__init__.py`这个文件是描述有多少个模块的东西。你可以将包理解为完成一系列的功能的一个东西。

   目前的 Python 实际上是有两种包的存在：正规包（regular Package） 以及命名空间包（Namespace package）。

   - 正规包：在 Python 3.2 之前就已经存在了，通常是以包含一个py文件的目录形式展现。当 package 被导入时，这个`__init__.py` 文件会被隐式地执行。
   - 命名空间包：根据 PEP 420 的定义，命名空间包是由多个portion组成的 ——portion 类似于父包下的子包，但它们物理位置上不一定相邻，而且它们可能表现为 .zip 中的文件、网络上的文件等等。命名空间包不需要 py 文件，只要它本身或者子包（也就是 portion）被导入时，Python 就会给顶级的部分创建为命名空间包 —— 因此，命名空间包不一定直接对应到文件系统中的对象，它可以是一个虚拟的 module 。
   - 要注意的是，Python 的 package 实际上都是特殊的 module ：可以通过导入 package 之后查看globals()可知；实际上，任何带有 `__path__` 属性的对象都会被 Python 视作 package 。

3. 库是指具有相关功能模块的集合。这也是Python的一大特色之一，即具有强大的标准库、第三方库以及自定义模块。
   - 标准库：Python里那些自带的模块
   - 第三方库：就是由其他的第三方机构，发布的具有特定功能的模块。
   - 自定义模块：用户自己可以自行编写模块，然后使用。
4. 框架
   框架是Python库的集合。框架跟库类似，从功能上来说的，框架往往集成了多种库的功能，框架是用来辅助开发某个领域功能的一个包，一般包内还会含有多个子包

# 导入模块

import 语句是Python最常用的导入机制，但不是唯一方式。importlib.import_module() 以及内置的 `__import__()` 函数都可以调起导入机制。

Python的import 语句实际上结合了两个操作：

- 搜索操作：根据指定的命名查找模块
- 绑定操作：将搜索的结果绑定到当前作用域对应的命名上

当一个模块被首次导入时，Python 会搜索该模块，如果找到就创建一个module对象并初始化。 

导入还分为绝对导入和相对导入，推荐使用绝对导入

## 模块搜索路径

```python
import sys
from pprint import pprint
pprint(sys.path)
```

sys.path的初始值来自于：

- 运行脚本所在的目录（如果打开的是交互式解释器则是当前目录）
- PYTHONPATH环境变量（类似于PATH变量，也是一组目录名组成）
- Python 安装时的默认设置

如何修改呢？可以使用.pth文件，该文件位置:

```python
import site
from pprint import pprint

pprint(site.getsitepackages())
```



## import语句机制

[参考](https://huaweicloud.csdn.net/63807245dacf622b8df88703.html?spm=1001.2101.3001.6661.1&utm_medium=distribute.pc_relevant_t0.none-task-blog-2~default~CTRLIST~activity-1-122818873-blog-77018298.pc_relevant_3mothn_strategy_recovery&depth_1-utm_source=distribute.pc_relevant_t0.none-task-blog-2~default~CTRLIST~activity-1-122818873-blog-77018298.pc_relevant_3mothn_strategy_recovery&utm_relevant_index=1)