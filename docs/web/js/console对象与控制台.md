# console对象

类似stdout/stderr

作用：

1. 调试程序
2. 提供命令行接口，用来和网页代码互动

其浏览器实现包含在浏览器自带的开发工具中，F12可以打开

## 静态方法

### log

支持占位符

- `%s` 字符串
- `%d` 整数
- `%i` 整数
- `%f` 浮点数
- `%o` 对象的链接
- `%c` CSS 格式字符串

```javascript
console.log("aaa")
console.log('%d %s balloons', number, color);
console.log(
  '%cThis text is styled!',
  'color: red; background: yellow; font-size: 24px;'
)
```

除了log还有info/debug方法，用法类似

还有可读性更高的dir方法,trace方法

```javascript
console.dir(obj, {colors: true})
```

### warn/error



```javascript
//table方法以表格方式显示
var languages = {
  csharp: { name: "C#", paradigm: "object-oriented" },
  fsharp: { name: "F#", paradigm: "functional" }
};

console.table(languages);

//count方法输出调用次数
function greet(user) {
  console.count();
  return 'hi ' + user;
}

greet('bob')
//  : 1
// "hi bob"

greet('alice')
//  : 2
// "hi alice"

greet('bob')
//  : 3
// "hi bob"
```

## debugger语句

`debugger`语句主要用于除错，作用是设置断点。如果有正在运行的除错工具，程序运行到`debugger`语句时会自动停下

```javascript
for(var i = 0; i < 5; i++){
  console.log(i);
  if (i === 2) debugger;
}
F8继续
```

