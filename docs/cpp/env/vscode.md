

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



# 插件

# 运行

cmd+r 运行

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

## React项目debug

[参考文档](https://www.hi-ruofei.com/archives/react-debug-with-vscode.html)

1. 先跑起来项目

2. 创建launch.json
   $ touch <project_dir>/.vscode/launch.json

   ```json
   {
     // 欲了解更多信息，请访问: <https://go.microsoft.com/fwlink/?linkid=830387>
     "version": "0.2.0",
     "configurations": [
       {
         "name": "debug",
         "request": "launch",
         "type": "chrome",
         "url": "<http://localhost:3000>"
       }
     ]
   }
   ```

   - `name`：定义名为 `debug` 的调试配置。
   - `request`：该字段有两个值，分别是 `launch` 和 `attach`。`launch` 的意思是，我们希望调试器启动我们的代码并开始调试会话。
   - `type`：表示让调试器应该使用哪种类型的调试器。`chrome` 的意思是，我们希望使用 Chrome 调试器来调试我们的代码。
   - `url`：指定了我们要调试的 Web 应用的 URL。显然，我们这个 Demo 是跑在本地 3000 端口上的。

3. 然后，按下键盘的 `F5` 键，或者是点一下 VS Code 中的调试按钮：
   这时候你会发现打开了一个新的 Chrome 窗口。

![image-20250213093452049](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250213093452049.png)

调试按钮作用：

- 逐过程F10：一行一行的执行，不进入函数内部
- 单步调试F11：进入函数内部
- 单步跳出Shift + F11: 退出当前函数并继续执行函数外部的代码

调试过程中左上角可以看到变量

# 代码同步

## sftp插件

cmd+shift+p 搜索sftp安装后，配置

**watcher**：files可以默认为"/*"，意思是监控当前文件夹下的所有文件，autoUpload，autoDelete也都默认为true，这样你在新增或删除任何东西的时候，本地和服务端都会实时保持同步

**ignore**：这个挺关键的，你可以用glob的方式制定忽略同步的文件，比如一些很大的数据文件其实不需要被同步，只需要关键的代码就行。

### 多主机

```json
{
  "username": "username",
  "password": "password",
  "remotePath": "/remote/workspace/a",
  "watcher": {
    "files": "dist/*.{js,css}",
    "autoUpload": false,
    "autoDelete": false
  },
  "profiles": {
    "dev": {
      "host": "dev-host",
      "remotePath": "/dev",
      "uploadOnSave": true
    },
    "prod": {
      "host": "prod-host",
      "remotePath": "/prod"
    }
  },
  "defaultProfile": "dev"
}
```

使用SFTP: Set Profile 来切换profile

### 多Context

```json
[
  {
    "name": "server1",
    "context": "project/build",
    "host": "host",
    "username": "username",
    "password": "password",
    "remotePath": "/remote/project/build"
  },
  {
    "name": "server2",
    "context": "project/src",
    "host": "host",
    "username": "username",
    "password": "password",
    "remotePath": "/remote/project/src"
  }
]
```

# ssh

remote -SSH

按F1搜索 remote-ssh 连接服务器

1. [服务器离线远程](https://zhuanlan.zhihu.com/p/294933020)

1. Failed to set up dynamic port forwarding connection over SSH to the VS Code Server. (Show log)

https://blog.csdn.net/weixin_54468359/article/details/144004739

