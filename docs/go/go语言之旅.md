# [基础](https://tour.go-zh.org/basics/7)

## 变量与类型

```go
package main

import ("fmt"
        "math.rand")

const Pi = 3.14 //用const声明常量
var i, j int = 1, 2 // 可自动从初值推断类型


func swap(x, y string) (string, string) {//可返回多个值
	return y, x
}

func main() {
  a, b := swap("hello", "world") // 短赋值语句:=
	fmt.Println(a, b)
}
```

1. import可分组导入多个包
2. 函数可返回多个值
3. **函数同类型参数，除最后一个外可省略类型**
4. 大写开头的对象是包导出的可访问对象
5. fmt.Println可打印元组
6. var声明变量
7. 短赋值语句可在函数中声明var变量，函数外的每个语句都 **必须** 以关键字开始（`var`、`func` 等），因此 `:=` 结构不能在函数外使用。常量不能用该语法



### 带名字的返回值

```go
package main

import "fmt"

func split(sum int) (x, y int) {
	x = sum * 4 / 9
	y = sum - x
	return  // 直接返回已命名的返回值
}

func main() {
	fmt.Println(split(17))
}
# 输出 7 10
```

### 基本类型

```go
bool

string

int  int8  int16  int32  int64
uint uint8 uint16 uint32 uint64 uintptr

byte // uint8 的别名

rune // int32 的别名
     // 表示一个 Unicode 码位

float32 float64

complex64 complex128
```

```go
package main

import (
	"fmt"
	"math/cmplx"
)

var (
	ToBe   bool       = false
	MaxInt uint64     = 1<<64 - 1
	z      complex128 = cmplx.Sqrt(-5 + 12i)
)

func main() {
	fmt.Printf("类型：%T 值：%v\n", ToBe, ToBe)
	fmt.Printf("类型：%T 值：%v\n", MaxInt, MaxInt)
	fmt.Printf("类型：%T 值：%v\n", z, z)
}
类型：bool 值：false
类型：uint64 值：18446744073709551615
类型：complex128 值：(2+3i)
```

字符串打印要用%q

### 类型转换表达式T

表达式 `T(v)` 将值 `v` 转换为类型 `T`。注意,go的类型转换只能显示转换

一些数值类型的转换：

```
var i int = 42
var f float64 = float64(i)
var u uint = uint(f)
```

或者，更加简短的形式：

```
i := 42
f := float64(i)
u := uint(f)
```

## 流程控制

### 循环

Go 只有一种循环结构：`for` 循环。

基本的 `for` 循环由三部分组成，它们**用分号隔开，无需小括号**：

- 初始化语句：在第一次迭代前执行
- 条件表达式：在每次迭代前求值
- 后置语句：在每次迭代的结尾执行

```go
package main

import "fmt"

func main() {
	sum := 0
	for i := 0; i < 10; i++ {
		sum += i
	}
	fmt.Println(sum)
  
  //也可以把初始化语句 和 后置语句不要
  for sum < 1000 {
		sum += sum
	}
}

```

### if判断

1. 和 `for` 一样，`if` 语句可以在条件表达式前执行一个简短语句。
2. 该语句声明的变量作用域在 `if` 和else之内。

```go
package main

import (
	"fmt"
	"math"
)

func pow(x, n, lim float64) float64 {
	if v := math.Pow(x, n); v < lim {
		return v
	} else {
		fmt.Printf("%g >= %g\n", v, lim)
	}
	// can't use v here, though
	return lim
}

func main() {
	fmt.Println(
		pow(3, 2, 10),
		pow(3, 3, 20),
	)
}

```

### switch分支

1. 只会运行选定的case,即每个case后自动添加break语句
2. case无需为常量，且取值不限于整数



```go
package main

import (
	"fmt"
	"time"
)

func main() {
  
  switch os := runtime.GOOS; os {
	case "darwin":
		fmt.Println("macOS.")
	case "linux":
		fmt.Println("Linux.")
	default:
		// freebsd, openbsd,
		// plan9, windows...
		fmt.Printf("%s.\n", os)
	}
  
  
	t := time.Now()
	switch {  // 无条件switch可以让代码更清晰
	case t.Hour() < 12:
		fmt.Println("早上好！")
	case t.Hour() < 17:
		fmt.Println("下午好！")
	default:
		fmt.Println("晚上好！")
	}
}

```

### defer推迟

defer 语句会将函数推迟到外层函数返回之后执行。

1. 推迟调用的函数其参数会立即求值，但直到外层函数返回前该函数都不会被调用。即使包含`defer`的函数在执行过程中发生了错误或提前返回，这个被`defer`的函数仍然会被执行。
2. 多个defer函数会按照倒序执行



用于在函数返回之前执行一些必要的清理操作或者释放资源

```go
package main

import "fmt"

func main() {
	defer fmt.Println("world") //会在外层函数main返回前执行
	fmt.Println("hello")
}
// 输出：
// Hello
// World
```

## 更多类型

### 指针

*T 是指向T类型值的指针，零值为nil

&操作符生成一个指向其操作数的指针。

*操作符表示指针指向的底层值，即解引用

Go没有指针运算

```go
package main

import "fmt"

func main() {
	i, j := 42, 2701

	p := &i         // 指向 i
	fmt.Println(*p) // 通过指针读取 i 的值
	*p = 21         // 通过指针设置 i 的值
	fmt.Println(i)  // 查看 i 的值

	p = &j         // 指向 j
	*p = *p / 37   // 通过指针对 j 进行除法运算
	fmt.Println(j) // 查看 j 的值
```

### 结构体

是一组字段

- 结构体指针
  结构体字段可以通过结构体指针来访问，`(*p).X`，但这么写太啰嗦，可以直接写p.X
- 结构体字面量

```go

var (
	v1 = Vertex{1, 2}  // 创建一个 Vertex 类型的结构体
	v2 = Vertex{X: 1}  // Y:0 被隐式地赋予零值
	v3 = Vertex{}      // X:0 Y:0
	p  = &Vertex{1, 2} // 创建一个 *Vertex 类型的结构体（指针）
)
```





```go
package main

import "fmt"
// 定义一个结构体
type Vertex struct {
	X int
	Y int
}

func main() {
	v := Vertex{1, 2}
	p := &v
	p.X = 1e9 // 访问结构体指针的字段
	fmt.Println(v)
}
```

### 数组与切片

```go
package main

import "fmt"

func main() {
	var a [2]string
	a[0] = "Hello"
	a[1] = "World"
	fmt.Println(a[0], a[1])
	fmt.Println(a)

	primes := [6]int{2, 3, 5, 7, 11, 13}
	fmt.Println(primes)
  var s []int = primes[1:4]
	fmt.Println(s)
}
```

1. 类型 `[n]T` 表示一个数组，它拥有 `n` 个类型为 `T` 的值。**数组的长度是其类型的一部分，因此数组不能改变大小。**

2. 类型 `[]T` 表示一个元素类型为 `T` 的切片。[low : high ] 含头不含尾
   **切片不存储数据，只是数组的引用，和底层数组共享数据**
3. 切片默认下界为0，上界为切片长度
4. **切片字面量**

这是一个数组字面量：

```
[3]bool{true, true, false}
```

下面这样则会创建一个和上面相同的数组，然后再构建一个引用了它的切片：

```
[]bool{true, true, false}
```

5. **切片的长度len是它包含的元素个数，容量cap是切片开始到底层数组末尾的个数。** 
   可以通过重新切片（修改其low和high值）来扩展一个切片
   
   > Low影响容量，high-low是Len，所以共同影响

```go
package main

import "fmt"

func main() {
	s := []int{2, 3, 5, 7, 11, 13}
	printSlice(s)  //len=6 cap=6 [2 3 5 7 11 13]

	// 截取切片使其长度为 0
	s = s[:0]
	printSlice(s)  //len=0 cap=6 []

	// 扩展其长度
	s = s[:4]
	printSlice(s)  //len=4 cap=6 [2 3 5 7]

	// 舍弃前两个值， 注意长度是在切片基础上修改了low，其high依然有效
	s = s[2:]
	printSlice(s)  //len=2 cap=4 [5 7]
}

func printSlice(s []int) {
	fmt.Printf("len=%d cap=%d %v\n", len(s), cap(s), s)
}


```

6. nil切片: 长度和容量为0且没有底层数组

7. make创建切片， 创建动态数组的方式
   make函数会分配一个元素为零值的数组并返回一个引用了它的切片

   ```go
   a := make([]int, 5)  // len(a)=5 cap=5 [0 0 0 0 0]
   //要指定它的容量，需向 make 传入第三个参数：
   b := make([]int, 0, 5) // len(b)=0, cap(b)=5
   
   b = b[:cap(b)] // len(b)=5, cap(b)=5
   b = b[1:]      // len(b)=4, cap(b)=4
   ```

8. 切片的切片

9. 向切片追加元素append(s []T, vs ...T) []T

   append 的结果是一个包含原切片所有元素加上新添加元素的切片。

   当 s的底层数组太小，不足以容纳所有给定的值时，它就会分配一个更大的数组。 返回的切片会指向这个新分配的数组。
   
   
   
### range

**Go 语言中 range 关键字用于 for 循环中迭代数组(array)、切片(slice)、通道(channel)或集合(map)的元素。在数组和切片中它返回元素的索引和索引对应的值，在集合中返回 key-value 对。**

for 循环的 range 格式可以对 slice、map、数组、字符串等进行迭代循环。格式如下：

```
for key, value := range oldMap {
    newMap[key] = value
}
```

以上代码中的 key 和 value 是可以省略。

   当使用 `for` 循环遍历切片时，每次迭代都会返回两个值。 第一个值为当前元素的下标，第二个值为该下标所对应元素的一份**副本**。

   ```go
   package main
   
   import "fmt"
   
   var pow = []int{1, 2, 4, 8, 16, 32, 64, 128}
   
   func main() {
   	for i, v := range pow {
   		fmt.Printf("2**%d = %d\n", i, v)
   	}
   }
   2**0 = 1
   2**1 = 2
   2**2 = 4
   2**3 = 8
   2**4 = 16
   2**5 = 32
   2**6 = 64
   2**7 = 128
   ```

   

## map映射

1. `make` 函数会返回给定类型的映射，并将其初始化备用。
2. 映射字面量： 和结构体类似，只不过必须有键名。 Map[键类型]值类型{}
3. **删除元素delete(m , key)**
4. **检查键是否存在elem, ok := m[key]**

```go
package main

import "fmt"

type Vertex struct {
	Lat, Long float64
}

var m map[string]Vertex  // 只声明不初始化

func main() {
	m = make(map[string]Vertex)
	m["Bell Labs"] = Vertex{
		40.68433, -74.39967,
	}
	fmt.Println(m["Bell Labs"])
}
{40.68433 -74.39967}
```



字面量

```go
var m = map[string]Vertex{
	"Bell Labs": Vertex{
		40.68433, -74.39967,
	},
	"Google": Vertex{
		37.42202, -122.08408,
	},
}
```

wordcount计算

```go
package main

import (
	"golang.org/x/tour/wc";
	"strings"
)

func WordCount(s string) map[string]int {
	words := strings.Fields(s)
	wordcount :=make(map[string]int)
	for _, word := range words{
		elem,ok := wordcount[word]
		if ok == false{
			wordcount[word] = 1
		}else{
			wordcount[word] = elem + 1
		}
	}
	return wordcount
}

func main() {
	wc.Test(WordCount)
}
```

## 函数值 & 闭包

1. 函数也是值
2. 闭包是一个函数值，它引用了函数体之外的变量，即函数绑定到了这些变量

```go
func adder() func(int) int {
	sum := 0
	return func(x int) int {
		sum += x
		return sum
	}
}


func main() {
	pos, neg := adder(), adder()
	for i := 0; i < 10; i++ {
		fmt.Println(
			pos(i), //闭包函数接收参数
			neg(-2*i),
		)
	}
}

```

斐波那契数列

```go
package main

import "fmt"

// fibonacci 是返回一个「返回一个 int 的函数」的函数
func fibonacci() func(int) int {
	a1:=0
	a2:=1
	return func(a int) int {
		if a==0 {
		return 0
		} else if a==1 {
		 return 1
		}else{
		res:=a1+a2
		a1=a2
		a2=res
		 return res
		}
	
	}
}

func main() {
	f := fibonacci()
	for i := 0; i < 10; i++ {
		fmt.Println(f(i))
	}
}

```

# 方法和接口

## 方法

Go没有类，但可以给类型定义方法

1. 方法就是一类带特殊的 **接收者** 参数的函数。
2. 接收者类型必须在同一个包内
3. 接收者为指针的方法 可以修改指针指向的值

### 方法是带接收者的函数

```go
package main

import (
	"fmt"
	"math"
)

type Vertex struct {
	X, Y float64
}

func (v Vertex) Abs() float64 {  //接收者为struct， 注意写法，函数名前面带接受者
	return math.Sqrt(v.X*v.X + v.Y*v.Y)
}

type MyFloat float64

func (f MyFloat) Abs() float64 { //接收者为数值类型
	if f < 0 {
		return float64(-f)
	}
	return float64(f)
}


func main() {
	v := Vertex{3, 4}
	fmt.Println(v.Abs()) // 注意，接收者可以调用方法
}

5
```



方法即函数，正常函数也能实现

```go
package main

import (
	"fmt"
	"math"
)

type Vertex struct {
	X, Y float64
}

func Abs(v Vertex) float64 { //方法名前面并没有接收者
	return math.Sqrt(v.X*v.X + v.Y*v.Y)
}

func main() {
	v := Vertex{3, 4}
	fmt.Println(Abs(v)) //正常的函数调用，而非方法调用
}
5
```

### 指针类型的接收者

1. 指针接收者的方法可以修改接收者指向的值（如这里的 `Scale` 所示）。 由于方法经常需要修改它的接收者，指针接收者比值接收者更常用。

2. 如果接收者非指针，而是值，那么方法修改的是副本！

3. 指针的方法，值也可以调用(自动取址)，反之也一样(自动提值)
   ```go
   var v Vertex
   v.Scale(5)  // OK
   p := &v
   p.Scale(10) // OK
   ```

   go会自动对值取址

```go
package main

import (
	"fmt"
	"math"
)

type Vertex struct {
	X, Y float64
}

func (v Vertex) Abs() float64 {
	return math.Sqrt(v.X*v.X + v.Y*v.Y)
}

func (v *Vertex) Scale(f float64) {
	v.X = v.X * f
	v.Y = v.Y * f
}

func main() {
	v := Vertex{3, 4}
	v.Scale(10)
	fmt.Println(v.Abs())
}
50
```

## 接口

接口类型的定义为一组方法签名

- 指针实现的接口方法，值未必实现。

- 接口隐式实现： 无需implements关键字
  ```go
  package main
  
  import "fmt"
  
  type I interface {
  	M()
  }
  
  type T struct {
  	S string
  }
  
  // 此方法表示类型 T 实现了接口 I，不过我们并不需要显式声明这一点。
  func (t T) M() {
  	fmt.Println(t.S)
  }
  
  func main() {
  	var i I = T{"hello"}
  	i.M()
  }
  ```

- 接口值
  接口也是值，可以像其他值一样传递， 保存具体底层类型的值

- 没有指定方法的接口值称为空接口
  ```go
  interface{}
  ```

  

# 类型断言

## 强转

提供了访问接口值底层具体值的方式，有两种写法

1. `t := i.(T)`: 相当于把i强制转换为T，如果失败，则触发panic
2. `t, ok := i.(T)` : **成功则ok为true, 失败则ok为false,并不会触发panic**

```go
package main

import "fmt"

func main() {
	var i interface{} = "hello"

	s := i.(string)
	fmt.Println(s)

	s, ok := i.(string)
	fmt.Println(s, ok)

	f, ok := i.(float64)
	fmt.Println(f, ok)

	f = i.(float64) // panic
	fmt.Println(f)
}

```

## 按类型switch

```go
switch v := i.(type) {
case T:
    // v 的类型为 T
case S:
    // v 的类型为 S
default:
    // 没有匹配，v 与 i 的类型相同
}
```

## 内置接口

### Stringer

Stringer是一个接口，定义了一个String() string 方法签名，返回一个字符串。

```go
import "fmt"

type IPAddr [4]byte

// TODO: Add a "String() string" method to IPAddr.

func (v IPAddr) String() string{  // 实现Stringer接口
    return fmt.Sprintf("%v.%v.%v.%v", v[0],v[1],v[2],v[3])  
}  

func main() {
    hosts := map[string]IPAddr{
        "loopback":  {127, 0, 0, 1},
        "googleDNS": {8, 8, 8, 8},
    }
    for name, ip := range hosts {
        fmt.Printf("%v: %v\n", name, ip)
    }
}
```

### error

```go
type error interface {
    Error() string
}
```

通常函数会返回一个 `error` 值，调用它的代码应当判断这个错误是否等于 `nil` 来进行错误处理。

```
i, err := strconv.Atoi("42")
if err != nil {
    fmt.Printf("couldn't convert number: %v\n", err)
    return
}
fmt.Println("Converted integer:", i)
```

`error` 为 nil 时表示成功；非 nil 的 `error` 表示失败。

### Readers

`io` 包指定了 `io.Reader` 接口，它表示数据流的读取端。

Go 标准库包含了该接口的[许多实现](https://cs.opensource.google/search?q=Read\(\w%2B\s\[\]byte\)&ss=go%2Fgo)，包括文件、网络连接、压缩和加密等等。

`io.Reader` 接口有一个 `Read` 方法：

```
func (T) Read(b []byte) (n int, err error)
```

`Read` 用数据填充给定的字节切片并返回填充的字节数和错误值。在遇到数据流的结尾时，它会返回一个 `io.EOF` 错误。

# 范型

## 类型参数

 函数的类型参数出现在函数参数之前的方括号里

```
func Index[T comparable](s []T, x T) int
```

此声明意味着 `s` 是满足内置约束 `comparable` 的任何类型 `T` 的切片。 `x` 也是相同类型的值。

`comparable` 是一个有用的约束，它能让我们对任意满足该类型的值使用 `==` 和 `!=` 运算符。在此示例中，我们使用它将值与所有切片元素进行比较，直到找到匹配项。 该 `Index` 函数适用于任何支持比较的类型。

```go
package main

import "fmt"

// Index 返回 x 在 s 中的下标，未找到则返回 -1。
func Index[T comparable](s []T, x T) int {
	for i, v := range s {
		// v 和 x 的类型为 T，它拥有 comparable 可比较的约束，
		// 因此我们可以使用 ==。
		if v == x {
			return i
		}
	}
	return -1
}

func main() {
	// Index 可以在整数切片上使用
	si := []int{10, 20, 15, -10}
	fmt.Println(Index(si, 15))

	// Index 也可以在字符串切片上使用
	ss := []string{"foo", "bar", "baz"}
	fmt.Println(Index(ss, "hello"))
}

```

## 范型类型

类型可以使用类型参数进行参数化，这对于实现通用数据结构非常有用

```go
package main

// List 表示一个可以保存任何类型的值的单链表。
type List[T any] struct {
	next *List[T]
	val  T
}

func main() {
}

```

# 并发编程

## [go  routine](https://www.topgoer.com/%E5%B9%B6%E5%8F%91%E7%BC%96%E7%A8%8B/goroutine.html)

goroutine的概念类似于线程，但 goroutine是由Go的运行时（runtime）调度和管理的。**Go程序会智能地将 goroutine 中的任务合理地分配给每个CPU**。Go语言之所以被称为现代化的编程语言，就是因为它在**语言层面已经内置了调度和上下文切换的机制。**

**在Go语言编程中你不需要去自己写进程、线程、协程，你的技能包里只有一个技能–goroutine，当你需要让某个任务并发执行的时候，你只需要把这个任务包装成一个函数，开启一个goroutine去执行这个函数就可以了，就是这么简单粗暴。**

- 使用goroutine

Go语言中使用goroutine非常简单，只需要在调用函数的时候在前面加上`go`关键字，就可以为一个函数创建一个goroutine。

一个goroutine必定对应一个函数，可以创建多个goroutine去执行相同的函数。



多任务同步

```go
var wg sync.WaitGroup

func hello(i int) {
    defer wg.Done() // goroutine结束就登记-1
    fmt.Println("Hello Goroutine!", i)
}
func main() {

    for i := 0; i < 10; i++ {
        wg.Add(1) // 启动一个goroutine就登记+1
        go hello(i)
    }
    wg.Wait() // 等待所有登记的goroutine都结束
}
```

### goroutine与线程

- **可增长的栈** :OS线程（操作系统线程）一般都有固定的栈内存（通常为2MB）,一个goroutine的栈在其生命周期开始时只有很小的栈（典型情况下2KB），goroutine的栈不是固定的，他可以按需增大和缩小，goroutine的栈大小限制可以达到1GB，虽然极少会用到这个大。所以在Go语言中一次创建十万左右的goroutine也是可以的。
- **goroutine调度**：

## channel

channel是可以让一个goroutine发送特定值到另一个goroutine的通信机制。Go 语言中的通道（channel）是一种特殊的类型。通道像一个传送带或者队列，总是遵循先入先出（First In First Out）的规则，保证收发数据的顺序。每一个通道都是一个具体类型的导管，也就是声明channel的时候需要为其指定元素类型。

>单纯地将函数并发执行是没有意义的。函数与函数间需要交换数据才能体现并发执行函数的意义。
>
>虽然可以使用共享内存进行数据交换，但是共享内存在不同的goroutine中容易发生竞态问题。为了保证数据交换的正确性，必须使用互斥量对内存进行加锁，这种做法势必造成性能问题。
>
>Go语言的并发模型是CSP（Communicating Sequential Processes），提倡通过通信共享内存而不是通过共享内存而实现通信。
>
>如果说goroutine是Go程序并发的执行体，channel就是它们之间的连接。channel是可以让一个goroutine发送特定值到另一个goroutine的通信机制。



1. channel是引用类型

```go
var 变量 chan 元素类型
例如：
    var ch1 chan int   // 声明一个传递整型的通道
    var ch2 chan bool  // 声明一个传递布尔型的通道
    var ch3 chan []int // 声明一个传递int切片的通道
```



2. 创建channel

声明的通道后需要使用make函数初始化之后才能使用。

创建channel的格式如下：

```go
    make(chan 元素类型, [缓冲大小])
```

3. channel操作

通道有发送（send）、接收(receive）和关闭（close）三种操作。

发送和接收都使用<-符号。

```go
ch := make(chan int)
ch <- 10 //把10发送到ch中
x := <- ch // 从ch中接收值并赋值给变量x
<-ch       // 从ch中接收值，忽略结果
close(ch) // 关闭通道， 用于通知接收方goroutine数据已发送完毕
```

### 有/无缓冲通道

 不指定缓冲大小就得到一个无缓冲通道， 无缓冲通道也被称为同步通道，使用无缓冲通道进行通信将导致发送和接收的goroutine同步化。无缓冲的通道只有在有人接收值的时候才能发送值。

### 从通道中循环取值

有两种方式判断通道是否被关闭

1. 读取时，判断ok
2. for range自动判断， 常用

```go
func main() {
    ch1 := make(chan int)
    ch2 := make(chan int)
    // 开启goroutine将0~100的数发送到ch1中
    go func() {
        for i := 0; i < 100; i++ {
            ch1 <- i
        }
        close(ch1)
    }()
    // 开启goroutine从ch1中接收值，并将该值的平方发送到ch2中
    go func() {
        for {
            i, ok := <-ch1 // 通道关闭后再取值ok=false
            if !ok {
                break
            }
            ch2 <- i * i
        }
        close(ch2)
    }()
    // 在主goroutine中从ch2中接收值打印
    for i := range ch2 { // 通道关闭后会退出for range循环
        fmt.Println(i)
    }
}
```



### 单向通道

```
    1.chan<- int是一个只能发送的通道，可以发送但是不能接收；
    2.<-chan int是一个只能接收的通道，可以接收但是不能发送。
```

### select

select关键字，可以同时响应多个通道的操作。

select的使用类似于switch语句，它有一系列case分支和一个默认的分支。每个case会对应一个通道的通信（接收或发送）过程。select会一直等待，直到某个case的通信操作完成时，就会执行case分支对应的语句。具体格式如下：

```go
    select {
    case <-chan1:
       // 如果chan1成功读到数据，则进行该case处理语句
    case chan2 <- 1:
       // 如果成功向chan2写入数据，则进行该case处理语句
    default:
       // 如果上面都没有成功，则进入default处理流程
    }
```

- select可以同时监听一个或多个channel，直到其中一个channel ready

  ```go
  package main
  
  import (
     "fmt"
     "time"
  )
  
  func test1(ch chan string) {
     time.Sleep(time.Second * 5)
     ch <- "test1"
  }
  func test2(ch chan string) {
     time.Sleep(time.Second * 2)
     ch <- "test2"
  }
  
  func main() {
     // 2个管道
     output1 := make(chan string)
     output2 := make(chan string)
     // 跑2个子协程，写数据
     go test1(output1)
     go test2(output2)
     // 用select监控
     select {
     case s1 := <-output1:
        fmt.Println("s1=", s1)
     case s2 := <-output2:
        fmt.Println("s2=", s2)
     }
  }
  ```

  
