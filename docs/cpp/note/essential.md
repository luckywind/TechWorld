[参考](https://codeantenna.com/a/dh6ZYXQWvl)

[源代码参考](https://blog.csdn.net/m0_46181359/category_11141582.html)

class分为两部分：

1. 头文件：声明class所提供的各种操作行为

2. 程序代码文件：实现内容

iostream中定义的cout对象可以打印到终端， <<是输出操作符

```c++
#include <iostream>
#include <string>
using namespace std;  //把标准库命名空间下的所有名称暴露出来
int main(){
    string user_name;
    cout << "Please enter your first name: ";
    cin >> user_name;
    cout << '\n'
         << "Hello ,"
         << user_name
         << " ... and goodbye \n";
    return  0;
}
```



初始化

```c++
int a; //不初始化
int a=0; //=初始化
int a(0);  //构造函数语法 进行初始化，可以处理有多个成员的初始化
```



不可变修饰符const

```c++
const int max=3;
```

[C++的头文件和实现文件分别写什么 ](https://www.cnblogs.com/ider/archive/2011/06/30/what_is_in_cpp_header_and_implementation_file.html)

# C primer plus

[源代码](https://gitee.com/qtyresources/cprimer-plus/)
