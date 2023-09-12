

# g++编译

g++参数

-o:   给输出的可执行文件指定名字

-O:  编译优化选项，可选O0/O1/O2/O3

-std=c++17： 添加C++标准

-W -Wall:  编译不产生警告

-g 让编译得到的可执行文件不要优化，尽量接近源代码，添加调试信息



`release`：

```
g++ say_hi.cpp -o say_hi.out -W -Wall -O2 -std=c++17
g++ <filename>.cpp <other_cpp_files> -o <filename>.out -W -Wall -O2 -std=c++17
```

`debug`：

```
g++ say_hi.cpp -o say_hi.out -W -Wall -g -std=c++17
g++ <filename>.cpp <other_cpp_files> -o <filename>.out -W -Wall -g -std=c++17
```



![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/16023410133021.jpg)

> GCC：GNU Compiler Collection(GUN 编译器集合)，它可以编译C、C++、JAV、Fortran、Pascal、Object-C等语言。g++是GCC中的GUN C++ Compiler（C++编译器）。g++调用了C++ compiler。

# 编辑技巧

添加注释：⌘/ ⌥⇧A

代码提示：`code snippet` ⇥ 和 ⌘I（`I` refers to `IntelliSense`）

双击选中一个词，三击选中一行

⌥左右 和 ⌘上下左右；⇧光标区域选中；⇧上下左右 和 ⇧⌥左右 和 ⌘⇧上下左右

⌥⌫、⌘⌫；fn⌫

⌘C和⌘X可以对一行生效

⌥点击多处添加多个光标

cmd+N新建文件

cmd+shift+N新建文件夹

## 设置快捷键

cmd+k然后cmd+s搜索快捷键 , 把run code的快捷键修改为cmd+r



[杨希杰在macOS上用VSSCode写C++](https://www.bilibili.com/video/BV1g54y1s74Z/?spm_id_from=333.788&vd_source=fa2aaef8ece31d2c310d46092c301b46)

[对应文档](https://yang-xijie.github.io/LECTURE/vscode-cpp/5_%E5%BC%80%E5%90%AFVSCode%E7%9A%84%E5%A4%A7%E9%97%A8/)

# debug

1. task.json 在`VS Code`中可以自定义一些task（任务），这些任务会帮你自动化执行一些东西。任务的配置文件是`tasks.json`。我们希望定义一个**编译程序**的task，以后调试（`debug`）之前都会自动执行这个task。

2. debug`的配置文件是`launch.json
   VSCode内置的调试器配置文件时.vscode/launch.json文件

## 创建配置json

三个json文件

command+shift+p   

1. edit Configurations   创建c_cpp_properties.json
2. 打开c/c++文件的情况下，输入tasks, 选择configure task , 再选择 clang build active file 创建tasks.json
3. launch,      选择debug:Open launch.json,选择c++(GDB/LLDB)环境，创建launch.json文件

