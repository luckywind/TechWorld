# [代码组织](https://go-zh.org/doc/code.html)

1. Go代码必须放在**工作空间**内。它其实就是一个目录，其中包含三个子目录：

- `src` 目录包含Go的源文件，它们被组织成包（每个目录都对应一个包），
- `pkg` 目录包含包对象，
- `bin` 目录包含可执行命令。

`go` 工具用于构建源码包，并将其生成的二进制文件安装到 `pkg` 和 `bin` 目录中。

2. `GOPATH` 环境变量指定了你的工作空间位置。它或许是你在开发Go代码时， 唯一需要设置的环境变量。作为约定，请将此工作空间的 `bin` 子目录添加到你的 `PATH` 中：

$ **export PATH=$PATH:$GOPATH/bin**
3. 包路径，要选择一个基本路径，保证不会与其他标准库冲突
3. 在 Go 中，作为应用程序执行的代码必须在`main`包中.

## [go mod](https://golang-minibear2333.github.io/1.base/1-3-go-mod/)



`java` 里有一个叫 `maven` 的包管理工具， `go` 也有一个叫 `go mod` 的管理工具，可以管理项目引用的第三方包版本、自动识别项目中用到的包、自动下载和管理包。

- 自动下载依赖包
- 项目不必放在`$GOPATH/src`内了
- 项目内会生成一个`go.mod`文件，列出包依赖
- 所以来的第三方包会准确的指定版本号
- 对于已经转移的包，可以用 `replace` 申明替换，不需要改代码



modules和传统GOPATH不同，不需要包含src，bin这样的子目录，一个源代码目录甚至是空目录都可以作为[module](https://so.csdn.net/so/search?q=module&spm=1001.2101.3001.7020)，只要其中包含go.mod文件。

### 配置

```shell
go env -w GO111MODULE="on"
go env -w GOPROXY=https://goproxy.io
go mod init 项目名 #初始化项目
go mod tidy #自动增加包和删除无用包到 GOPATH 目录下（build的时候也会自动下载包加入到go.mod里面的）
```

注意：只要在本地设置一个公用path目录就可以了，全部的包都会下载到那里，其他本地项目用到时就可以共享了

自动生成了go.mod和go.sum文件

### 使用

go mod init：初始化go mod， 生成go.mod文件，后可接参数指定 module 名

go mod download：手动触发下载依赖包到本地cache（默认为$GOPATH/pkg/mod目录）

go mod graph：打印项目的模块依赖结构

go mod tidy ：添加缺少的包，且删除无用的包

go mod verify ：校验模块是否被篡改过

go mod why：查看为什么需要依赖

go mod vendor ：导出项目所有依赖到vendor下

# go 模块

[创建go模块](https://go.p2hp.com/go.dev/doc/tutorial/create-module)

## 调用另一个模块的代码

1. 启动模块

```shell
cd greetings 
go mod init example.com/greetings 

cd hello
go mod init example.com/hello 
```

2. 代码组织
   1. 可执行程序必须放到main包中

```shell
➜  greetings tree -a
.
|____go.mod  # module example.com/greetings
|____greetings.go # 这里写各种函数，注意方法名大写(作用类似于public)

➜  hello tree -a
.
|____go.mod #module example.com/hello
|____hello.go
```

​	2. 编写代码

```go
//greetings.go
package greetings

import "fmt"
func Hello(name string) string {
    message := fmt.Sprintf("Hi, %v. Welcome!", name)
    return message
}

// hello.go
package main  
import (
	"fmt"
	"example.com/greetings" //导入module
)

func main() {
	// 获取问候信息并打印出来.
	message := greetings.Hello("Gladys")
	fmt.Println(message)
}

```

3. 使用本地模块

   1. 模块路径重定向到本地目录

   ```shell
   cd hello
   go mod edit -replace example.com/greetings=../greetings
   ```

   此时，hello/go.mod文件多一行replace指令
   ```shell
   replace example.com/greetings => ../greetings
   ```

   2. 同步依赖项，添加代码所需的依赖项

   ```shell
   cd hello
   go mod tidy
   ```

   此时,hello/go.mod又多一行 require指令，表示当前模块依赖example.com/greetings模块，后面是一个伪版本号

   ```shell
   require example.com/greetings v0.0.0-00010101000000-000000000000
   ```

​	3. 如果是引用已发布的模块，则无需replace指令，且require指令后面是真实的版本号

## 处理错误

1. 被调用方利用多值函数，直接返回error值
2. 调用方检查error是否非空

```go
func Hello(name string) (string, error) {  //多值函数
	// 返回在消息中嵌入名称的问候语.
	if name == "" {
		return "",errors.New("empty name")
	}
	message := fmt.Sprintf("Hi, %v. Welcome!", name)
	return message,nil
}

func main() {
	// 获取问候信息并打印出来.
	log.SetPrefix("greetings: ")
	log.SetFlags(0)
	message, err := greetings.Hello("")
	if err != nil {
		log.Fatal(err) //打印错误，并停止
	}
	fmt.Println(message)
}

```

## 添加测试

1. 以 _test.go 结尾的文件名告诉`go test`命令该文件包含测试函数
2. 在与要测试的代码相同的包中实现测试函数.
3. 测试函数名称的格式为`Test*Name*`
4. 运行go test 执行测试

## 编译并安装应用程序

go run命令可以运行程序，但是并不会生成二进制可执行文件。下面两个文件用于构建代码：

1. go build 编译包及其依赖，但不安装结果

2. go install 编译并安装包
   所谓的安装，就是把生成的二进制可执行文件放到GO的安装路径中,如果把安装路径加入到$PATH中，那么在其他任何地方都可以执行它

   ```shell
   go list -f '{{.Target}}'  #查找go安装路径
   ```



# go模块管理

[参考](https://blog.csdn.net/weixin_43700106/article/details/118279983)

GO111MODULE=auto，即默认情况，当然=on与=off也包含在下述情况中：
一、没go.mod文件时，属于GOPATH模式，则使用 vendor 特性
二、有go.mod文件时，此时默认启用 modules特性
1.只找当前目录，不找GOPATH/src目录
2.当前目录下有vendor目录，则查找当前目录下vendor是否有此包；
3.当前目录下没有vendor目录，则查找GOROOT/src下是否有此包；
4.如果未找到，则启动GOPROXY特性，到仓库下载此包；
5.如果未下载到则提示包不存在；

包顺序：标准库 -> 项目内包 -> 内部第三方 -> 外部第三方包






# 管理依赖项

go help获取帮助

