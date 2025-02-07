# React基础

## 预备

### 关于React和Next.js

[React](https://reactjs.ac.cn/) 是一个用于构建**交互式用户界面**的 JavaScript **库**。

> 库的意思是 React 提供了构建 UI 的有用函数 (API)，但将这些函数在应用中使用的位置留给开发者决定。

Next.js 是一个 React **框架**，它为您提供了构建 Web 应用的基本模块。

> 框架的意思是 Next.js 处理 React 所需的工具和配置，并为您的应用提供额外的结构、功能和优化。

### UI 

当用户访问一个网页时，服务器会向浏览器返回一个 HTML 文件，浏览器读取 HTML 并构建文档对象模型 (DOM)。

DOM 是 HTML 元素的对象表示。它充当代码和用户界面之间的桥梁，并具有树状结构，包含父子关系。

你可以使用 DOM 方法和 JavaScript 来监听用户事件并[操作 DOM](https://mdn.org.cn/docs/Learn/JavaScript/Client-side_web_APIs/Manipulating_documents)，通过选择、添加、更新和删除用户界面中的特定元素。DOM 操作不仅允许你定位特定元素，还可以更改其样式和内容。

![image-20241227150549679](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20241227150549679.png)

### 使用 JavaScript 更新 UI

- 命令式编程，告诉浏览器如何一步步更新用户界面：

```html
<html>
  <body>
    <div id="app"></div>
    <script type="text/javascript">
    	 const app = document.getElementById('app');
    	  // Create a new H1 element
      const header = document.createElement('h1');
 
      // Create a new text node for the H1 element
      const text = 'Develop. Preview. Ship.';
      const headerContent = document.createTextNode(text);
 
      // Append the text to the H1 element
      header.appendChild(headerContent);
 
      // Place the H1 element inside the div
      app.appendChild(header);
    </script>
  </body>
</html>
```

- 声明式更简洁
  [React](https://reactjs.ac.cn/) 是一个流行的声明式库，您可以使用它来构建用户界面。

### 入门

- **react** 是 React 的核心库。
- **react-dom** 提供了特定于 DOM 的方法，使您能够将 React 与 DOM 一起使用。
- Js编译器 [Babel](https://babel.node.org.cn/)，将 JSX 代码转换为常规 JavaScript



```html
<html>
  <body>
    <div id="app"></div>
    <script src="https://unpkg.com/react@18/umd/react.development.js"></script>
    <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
        <!-- Babel Script 将 JSX 代码转换为常规 JavaScript-->
    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
        <!-- type="text/jsx 告知 Babel 要转换哪些代码 -->
    <script type="text/jsx"> 
      const app = document.getElementById('app');
      const root = ReactDOM.createRoot(app);
        <!-- 将 React 代码渲染到 DOM 中-->
      root.render(<h1>Develop. Preview. Ship.</h1>);
    </script>
  </body>
</html>
```

这正是 React 的作用，它是一个包含可重用代码片段的库，这些代码片段代表您执行任务——在本例中，更新 UI。

## 使用组件构建UI 

React三个概念：

- 组件
- 属性（Props）
- 状态（State）

React 组件的好处在于它们只是 JavaScript

### 创建组件

<font color=red>在 React 中，组件是返回一个 UI 元素(也就是标记)的**Js函数**，在函数的 return 语句中，您可以编写 JSX。</font>

1. 组件需要大写开头
2. 组件的使用方式与HTML标签相同，使用尖括号<>。 且只能返回一个JSX标签，如果需要返回多个，必须包装到一个共享的父级中，例如`<div>...<div>`或者一个空的`<>...</>`包装器。
3. export default关键字指定文件中的主组件

```html
<html>
  <body>
    <div id="app"></div>
    <script src="https://unpkg.com/react@18/umd/react.development.js"></script>
    <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"></script>
        <!-- Babel Script -->
    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
    <script type="text/jsx">
      const app = document.getElementById('app');
      function Header() {
		  return <h1>Develop. Preview. Ship.</h1>;
		}
		 
		const root = ReactDOM.createRoot(app);
		root.render(<Header />);
    </script>
  </body>
</html>
```

### 嵌套组件与组件树

可以像使用常规 HTML 元素一样，将 React 组件**嵌套**在彼此内部。

```html
function Header() {
  return <h1>Develop. Preview. Ship.</h1>;
}
 
function HomePage() {
  return (
    <div>
      {/* Nesting the Header component */}
      <Header />
    </div>
  );
}
```

顶级`HomePage`组件可以包含`Header`、`Article`和`Footer`组件。而每个组件又可以有自己的子组件，依此类推。

### 添加样式

在 React 中，您使用`className`指定 CSS 类。它的作用与 HTML [`class`](https://mdn.org.cn/en-US/docs/Web/HTML/Global_attributes/class) 属性相同。

```html
<img className="avatar" />
```



## 使用Props显示数据

类似于 JavaScript 函数，您可以设计接受自定义参数（或 props）的组件，这些参数会更改组件的行为或在渲染到屏幕上时可见显示的内容。然后，您可以将这些 props 从父组件传递到子组件。

**注意：**在 React 中，数据沿着组件树向下流动。

1. props是对象，因此可以使用[**对象解构**](https://mdn.org.cn/docs/Web/JavaScript/Reference/Operators/Destructuring_assignment)在您的函数参数内部显式命名 props 的值
2. <u>通过添加{ } 在JSX标记内（也可以在标签属性中）直接编写常规JavaScript，{ }是JSX语法，是进入JavaScript的一种方式。使用花括号在“JavaScript”和“JSX”世界之间穿梭。</u>

> JSX 允许您将标记放入 JavaScript 中,  花括号{ }允许您“返回”到 JavaScript。 
>
> 当JS遇到新的JSX标签，JSX标签内部还需要写JS时，仍然要再次写花括号{ }。

```html
 <!-- 解构参数-->
  function Header({ title }) {
    return <h1>{title ? title : 'Default title'}</h1>;
  }
 
		 function HomePage() {
		  return (
		    <div>
		      {/* Nesting the Header component */}
		      <Header title="React"/>
		      <Header title="another title"/>
		    </div>
		  );
		}
```

3. 传递数据

```jsx
      // 父组件中的数据
      var person = {
        name: "qianguyihao",
        age: 27,
        gender: "男",
        address: "深圳"
      };

	  // 在子组件中，如果想要使用外部传递过来的数据，必须显示的在 构造函数参数列表中，定义 props 属性来接收
	  // 通过 props 得到的任何数据都是只读的，不能重新赋值
      function Hello(props) {
        return (
          <div>
            <h3>这是 Hello子组件 中定义的元素： {props.name}</h3>
          </div>
        );
      }

      ReactDOM.render(
      	<!-- 注意：这里的 ...Obj 语法，是 ES6中的属性扩散，表示：把这个对象上的所有属性，展开了，放到这个位置 -->
        <div>
          <Hello {...person}> </Hello>
        </div>,
        document.getElementById("app")
      );
```









### 遍历列表

```html
function HomePage() {
  const names = ['Ada Lovelace', 'Grace Hopper', 'Margaret Hamilton'];
 
  return (
    <div>
      <Header title="Develop. Preview. Ship." />
      <ul>
        {names.map((name) => (
          <li>{name}</li>
        ))}
      </ul>
    </div>
  );
}
```

建议给li一个唯一标识，这里可用name： `          <li key={name}>{name}</li>`

## 使用状态添加交互性

使用状态和事件处理程序添加交互性

这只是对状态的介绍，您还可以学习更多关于在 React 应用程序中管理状态和数据流的知识。要了解更多信息，我们建议您查看 React 文档中的[添加交互性](https://reactjs.ac.cn/learn/adding-interactivity)和[管理状态](https://reactjs.ac.cn/learn/managing-state)部分。

### 监听与处理事件

1. 加个按钮`<button>Like</button>`

2. 按钮监听点击事件`<button onClick={xxx}>按钮文本</button>`
   点击事件是可以响应用户交互的众多事件之一，还有输入字段的onChange和表单的onSubmit等

   > 😳：除了按钮元素外，还有哪些监听元素？

3. 处理事件`<button onClick={handleClick}>Like</button>`
   ⚠️：这里并不是一个函数调用，后面没有() ，只需要传递它就行。但有的时候需要在点击时传参数该怎么办呢？ 也不能直接加()改成调用，因为这个调用会更改状态从而重新渲染，而重新渲染又会调用... ... 造成死循环； 正确的处理办法是使用箭头函数：`onClick={() =>handleClick(参数)}`

```html
 function handleClick() {
    console.log("increment like count")
  }

  
	function HomePage() {
	  const names = ['Ada Lovelace', 'Grace Hopper', 'Margaret Hamilton'];
	 
	  return (
	    <div>
	      <Header title="Develop. Preview. Ship." />
	      <ul>
	        {names.map((name) => (
	                    <li key={name}>{name}</li>
	        ))}
	      </ul>
	      <button onClick={handleClick}>Like</button>
	    </div>
	  );
	}
```

### 状态和Hook函数

向组件添加其他逻辑，例如状态。状态可看做UI中任何随时间变化的信息，通常由用户交互触发。

以 `use` 开头的函数称为 *Hook*。 `useState` 是 React 提供的内置 Hook。您可以在 [API 参考](https://reactjs.ac.cn/reference/react) 中找到其他内置 Hook。您还可以通过组合现有的 Hook 来编写自己的 Hook。

Hook 比其他函数更严格。您只能在组件（或其他 Hook）的 *顶部* 调用 Hook。如果您想在条件或循环中使用 `useState`，请提取一个新组件并将其放在那里。





用于管理状态的 React Hook 称为：`useState()`,它返回一个数组

1. 数组中的第一项是状态`值`
2. 第二项是用于`更新`值的**函数**。您可以将更新函数命名为任何内容，但通常以`set`开头，后跟要更新的状态变量的名称
   `const [likes, setLikes] = React.useState();`
3. 设置初始值
   `const [likes, setLikes] = React.useState(0);`

```html
	function HomePage() {
	  const names = ['Ada Lovelace', 'Grace Hopper', 'Margaret Hamilton'];
	  const [likes, setLikes] = React.useState(0);
	  
    function handleClick() {
    console.log("increment like count")
     setLikes(likes + 1); <!--调用更新函数-->
  }
	  return (
	    <div>
	      <Header title="Develop. Preview. Ship." />
	      <ul>
	        {names.map((name) => (
	                    <li key={name}>{name}</li>
	        ))}
	      </ul>
	     <button onClick={handleClick}>Like({likes})</button>
	    </div>
	  );
	}

```

### 组件间共享数据(props)

提升状态： 将状态从各个组件“向上”移动到包含所有组件的最近的父组件。

**父组件不仅可以向子组件传递状态，还可以传递函数，他们都是props**

```js
import { useState } from 'react';

export default function MyApp() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(count + 1);
  }

  return (
    <div>
      <h1>Counters that update together</h1>
      <MyButton count={count} onClick={handleClick} /> 作为props传递给每个子组件
      <MyButton count={count} onClick={handleClick} />
    </div>
  );
}

function MyButton({ count, onClick }) {
  return (
    <button onClick={onClick}>
      Clicked {count} times
    </button>
  );
}

```







## 从React到Next.js

Next.js 处理了大部分设置和配置，并具有其他功能来帮助您构建 React 应用程序。

### 安装Next.js

在项目中使用 Next.js 时，您不再需要从 [unpkg.com](https://unpkg.com/) 加载 `react` 和 `react-dom` 脚本。相反，您可以使用 `npm` 或您首选的包管理器在本地安装这些包。

> ** 注意**：要使用 Next.js，您需要在您的机器上安装 Node.js 版本 **18.17.0** 或更高版本

1. 在index.html同级文件夹创建空的package.json文件，内容为{}.

2. 终端执行`npm install react@latest react-dom@latest next@latest`
   package.json将列出安装的依赖:

   ```json
   {
     "dependencies": {
       "next": "^14.0.3",
       "react": "^18.3.1",
       "react-dom": "^18.3.1"
     }
   }
   ```

   还会生成一个`package-lock.json` 的新文件，其中包含有关每个包的确切版本的详细信息。

### 创建第一个页面

1. Next.js 使用文件系统路由。这意味着您可以使用文件夹和文件而不是使用代码来定义应用程序的路由。
   把index.js移动到app目录下的page.js

2. export default 标记的组件作为主组件，`export default function HomePage() {`

page.js内容:

```jsx
import { useState } from 'react';
 
function Header({ title }) {
  return <h1>{title ? title : 'Default title'}</h1>;
}
 
export default function HomePage() {
  const names = ['Ada Lovelace', 'Grace Hopper', 'Margaret Hamilton'];
 
  const [likes, setLikes] = useState(0);
 
  function handleClick() {
    setLikes(likes + 1);
  }
 
  return (
    <div>
      <Header title="Develop. Preview. Ship." />
      <ul>
        {names.map((name) => (
          <li key={name}>{name}</li>
        ))}
      </ul>
 
      <button onClick={handleClick}>Like ({likes})</button>
    </div>
  );
}
```





### 运行开发服务器

package.json中添加next dev脚本

```json
{
  "scripts": {
    "dev": "next dev"
  },
  "dependencies": {
    "next": "^14.0.3",
    "react": "^18.3.1",
    "react-dom": "^18.3.1"
  }
}
```

执行命令`npm run dev`, 然后访问localhost:3000，  目前的代码会报服务器端不能使用useState的错误，先不管。

app目录下会自动生成layout.js的布局文件



### 服务器和客户端环境

在 Web 应用程序的上下文中

- **客户端**指的是用户设备上的浏览器，它向服务器发送请求以获取您的应用程序代码。然后，它将从服务器接收到的响应转换为用户可以交互的界面。
- **服务器**指的是数据中心中的计算机，它存储您的应用程序代码，接收来自客户端的请求，执行一些计算，并发送回相应的响应。
- **网络边界**是分隔不同环境的概念线。

![A component tree showing a layout that has 3 components as its children: Nav, Page, and Footer. The page component has 2 children: Posts and LikeButton. The Posts component is rendered on the server, and the LikeButton component is rendered on the client.](https://nextjs.net.cn/_next/image?url=%2Flearn%2Flight%2Flearn-client-server-modules.png&w=3840&q=75&dpl=dpl_4FFcrev3cFP2zwnf13Q6F7Kw9i3v)

在幕后，组件被分成两个模块图。**服务器模块图（或树）**包含在服务器上渲染的所有服务器组件，而**客户端模块图（或树）**包含所有客户端组件。

服务器组件渲染后，一种称为**React 服务器组件有效负载 (RSC)**的特殊数据格式将发送到客户端。RSC 有效负载包含

1. 服务器组件的渲染结果。
2. 客户端组件应渲染位置的占位符（或空洞）及其 JavaScript 文件的引用。

React 使用此信息来整合服务器组件和客户端组件，并在客户端上更新 DOM。

#### 使用客户端组件

Next.js 默认使用服务器组件 - 这是为了提高应用程序的性能

1. 文件顶部添加`'use client';`指令告诉React在客户端上渲染组件
2. 通过import 导入其他文件导出的组件。 也可以从react中导入，例如`import React from 'react';`后续可以`React.useState来声明状态`

/app/like-button.js  导出一个客户端渲染的组件

```jsx
'use client';
 
import { useState } from 'react';
 
export default function LikeButton() {   导出组件
  const [likes, setLikes] = useState(0);
 
  function handleClick() {
    setLikes(likes + 1);
  }
 
  return <button onClick={handleClick}>Like ({likes})</button>;
}
```

/app/page.js

```jsx
import LikeButton from './like-button';  这里导入
 
function Header({ title }) {
  return <h1>{title ? title : 'Default title'}</h1>;
}
 
export default function HomePage() {
  const names = ['Ada Lovelace', 'Grace Hopper', 'Margaret Hamilton'];
 
  return (
    <div>
      <Header title="Develop. Preview. Ship." />
      <ul>
        {names.map((name) => (
          <li key={name}>{name}</li>
        ))}
      </ul>
      <LikeButton />    这里直接使用这个组件
    </div>
  );
}
```



# [React官方快速入门](https://reactjs.ac.cn/learn)

## 基础

### 条件渲染

1. 利用if

```jsx
let content;
if (isLoggedIn) {
  content = <AdminPanel />;
} else {
  content = <LoginForm />;
}
return (
  <div>
    {content}
  </div>
);
```

2. 利用&&

```tsx
function Item({ name, isPacked }) {
  return (
    <li className="item">
      {name} {isPacked && '✅'}
    </li>
  );
}

export default function PackingList() {
  return (
    <section>
      <h1>Sally Ride's Packing List</h1>
      <ul>
        <Item
          isPacked={true}
          name="Space suit"
        />
        <Item
          isPacked={true}
          name="Helmet with a golden leaf"
        />
        <Item
          isPacked={false}
          name="Photo of Tam"
        />
      </ul>
    </section>
  );
}
```





## 井字棋

```js
// 父组件向子组件传递状态和函数
<Square value={squares[0]} onSquareClick={() => handleClick(0)} />


function Square({value,onSquareClick}) {
  function handleClick() {
    setValue('X');
  }


  return <button className="square" onClick={onSquareClick}>
    {value}</button>;
}
```

DOM `<button>` 元素的 `onClick` props 对 React 有特殊意义，因为它是一个内置组件。对于像 Square 这样的自定义组件，命名由你决定。你可以给 `Square` 的 `onSquareClick` props 或 `Board` 的 `handleClick` 函数起任何名字，代码还是可以运行的。在 React 中，通常使用 `onSomething` 命名代表事件的 props，使用 `handleSomething` 命名处理这些事件的函数。



### 不变性很重要

更改状态会导致重新渲染，如果很多组件共用一个数组，更新数组中的一个元素，会导致所有组件都重新渲染，但是实际只需要一个组件重新渲染时，最好是复制该数组，并修改指定位置，以避免重新渲染所有组件。

```jsx
  function handleClick(i) {
    if (squares[i] || calculateWinner(squares) ) {
      return;
    }
    const nextSquares = squares.slice(); //复制一份
    if (xIsNext) {
      nextSquares[i] = "X";  //修改副本
    } else {
      nextSquares[i] = "O";
    }
    setSquares(nextSquares);  //渲染修改后的副本
    setXIsNext(!xIsNext);
  }
```













# 使用笔记

## useEffect与useState

`useEffect` 是 React 用来处理副作用的 Hook。通常打开页面时自动加载数据

它接受两个参数：

1. 一个副作用函数，通常用于异步操作、DOM 操作、订阅等。
2. 可选的依赖数组，用于控制副作用函数的触发时机。

你可以通过 `return` 语句来返回清理函数，在组件卸载时或副作用重新执行之前清理资源（如取消订阅、清除定时器等）。

两者经常结合使用，例如，下面的代码演示了如何用 `useEffect` 发送一个 API 请求，并用 `useState` 存储返回的数据：

```tsx
import { useEffect, useState } from "react";

function App() {
  const [data, setData] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      const response = await fetch("https://api.example.com/data");
      const result = await response.json();
      setData(result); // 更新状态
    };
    
    fetchData();
  }, []); // 空数组，表示只在组件挂载时执行一次

  return (
    <div>
      {data ? <pre>{JSON.stringify(data, null, 2)}</pre> : "加载中..."}
    </div>
  );
}

```

## 顶部导航条

```js
      <div className="flex my-5">
        <Breadcrumb>
          <BreadcrumbList>
            <BreadcrumbItem>
              <BreadcrumbLink href="/">应用管理</BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbLink href="/products">产品构建</BreadcrumbLink>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>
      </div>
```

## 搜索框

### Popover/PopoverTrigger/PopoverContent

[参考radix文档](https://radix-ui.com.cn/primitives/docs/components/popover)

#### 显示当前选中的值

```jsx
{pipelineName
  ? products.find((module) => module.pipeline_name === pipelineName)?.pipeline_name
  : "选择产品流水线"}

```

如果 `pipelineName` 有值，则显示选中的流水线名称；否则显示占位文本。

#### 实时筛选

[Command的使用参考shadcn.ui](https://ui.shadcn.com/docs/components/command)

```jsx
 <PopoverContent className="w-[250px] p-0">
            <Command>
              <CommandInput placeholder="选择产品流水线" />
              <CommandList>
                <CommandEmpty>未选择</CommandEmpty>
                <CommandGroup> 
                  {/*动态选项*/}
                  {products.map((module) => (
                    <CommandItem  {/*选项*/}
                      key={module.pipeline_id}
                      value={module.pipeline_name}
                      onSelect={(value) => {
                        setPipelineName(value === pipelineName ? "" : value);
                        setOpen(false);
                      }}
                    >
                      {module.pipeline_name}   {/*显示的内容*/}
                      <Check    {/*标记打勾选中項*/}
                        className={cn(
                          "ml-auto",
                          pipelineName === module.pipeline_name
                            ? "opacity-100"
                            : "opacity-0",
                        )}
                      />
                    </CommandItem>
                  ))}
                </CommandGroup>
              </CommandList>
            </Command>
          </PopoverContent>
```



## 下拉选

**`<Select>`**: 这是一个下拉选择框的根组件。它负责展示下拉选项列表，并处理用户的选择。

**`onValueChange={(v) => { getPipelineTagSubmodules(v); }}`**: 这个属性指定了当用户选择一个值时的回调函数。`onValueChange` 会在选择框的值发生变化时被调用，`v` 是用户选择的值。在这里，`getPipelineTagSubmodules(v)` 函数会被调用，`v` 作为参数传入

```tsx
            <Select
                  onValueChange={(v) => {
                    getPipelineTagSubmodules(v);
                  }}
                >
								  SelectTrigger是触发下拉菜单的按钮，显示当前选中的值或占位符。
                  <SelectTrigger className="w-[180px]">
                    <SelectValue placeholder="选择产品包版本" />
                  </SelectTrigger>
  
                  <SelectContent>
                    第一个选项：传递default
                    <SelectItem value="default">default</SelectItem>
                    其余选项：传递{tag}
                    {pipelineTags.map((tag) => {
                      return (
                        <SelectItem key={tag} value={tag}>
                          {tag}
                        </SelectItem>
                      );
                    })}
                  </SelectContent>
                </Select>
```



### 复杂下拉选

  let submodulesTag = {};  

```tsx
<div className="flex-col space-y-2 max-h-[600px] overflow-y-scroll">
                  {pipelineTagSubmodules.map((item, index) => { 每个模块都有一个下拉选择
                    return (
                      <div key={index} className="flex items-center">
                        <Label className="w-[200px] text-right px-3">
                          {item.packages}
                        </Label>
                        <Select
                          defaultValue={item.version}
                          onValueChange={(val) => {
                            设置指定模块的tag
                            setSubmodulesTag(item.packages, val);
                          }}
                        >
                          <SelectTrigger className="w-[180px]">
                            <SelectValue placeholder="选择版本" />
                          </SelectTrigger>
                          
                          <SelectContent>
                             第一个选项
                            <SelectItem value="latest">latest</SelectItem>
                             其余选项
                            {item["pipelines"].map((vitem) => {
                              return (  选项又进行分组显示，按流水线名称分组
                                <SelectGroup key={vitem["pipeline_name"]}>
                                  <SelectLabel>
                                    {vitem["pipeline_name"]} 分组标签是流水线名称
                                  </SelectLabel>
                                  {vitem["tags"].map((titem) => {
                                    return (
                                      <SelectItem
                                        key={titem}
                                        value={
                                          vitem["pipeline_id"] +
                                          "/" +
                                          item["packages"] +
                                          "/" +
                                          titem
                                        }
                                      >
                                        {titem}
                                      </SelectItem>
                                    );
                                  })}
                                </SelectGroup>
                              );
                            })}
                          </SelectContent>
                        </Select>
                      </div>
                    );
                  })}
                </div>
```

## Dialog

需要点击确认按钮的

### DialogContent

### DialogHeader

### DialogFooter

对话框右下角

```tsx
           <DialogFooter>
              <Button type="submit" onClick={startBuildProduct}>
                确认构建
              </Button>
            </DialogFooter>
```

## 抽屉展示区

### Drawer/DrawerTrigger/DrawerPortal

```jsx
 <Drawer direction="left">
                          <DrawerTrigger>
                            <Button
                              variant="link"
                              onClick={() => {
                                setPipelineBuildSubmodules([]);
                                getPipelineBuildNumSubmodules(apps.build_num);
                              }}
                            >
                              模块信息
                            </Button>
                          </DrawerTrigger>
                          <DrawerPortal>
                            <DrawerOverlay className="fixed inset-0 bg-black/40" />
                            <DrawerContent className="h-screen w-[600px]">
                              <DrawerHeader>
                                <DrawerTitle>该产品包含以下模块</DrawerTitle>
                                <DrawerDescription>
                                  <div className="flex-col mt-5 space-y-4">
                                    展示内容
                                  </div>
                                </DrawerDescription>
                              </DrawerHeader>
                            </DrawerContent>
                          </DrawerPortal>
                        </Drawer>
```

## 状态切换

```jsx
  const [expandedItems, setExpandedItems] = useState({}); 

  // 切换某个版本号的展开/折叠状态
  const toggleExpand = (key) => {
    setExpandedItems((prevState) => ({
      ...prevState,
      [key]: !prevState[key],
    }));
  };

                                        <button
                                          className="text-lg font-semibold"
                                          onClick={() => toggleExpand(versionKey)} // 点击版本号时切换展开状态
                                        >
                                          {versionKey} {expandedItems[versionKey] ? "[-]" : "[+]" }
                                        </button>
```

React 的状态更新函数 `setExpandedItems` 支持传入一个回调函数，回调函数的参数 `prevState` 是当前状态值（这里是 `expandedItems`）。使用回调函数是为了确保状态更新是基于最新的状态，避免潜在的状态竞争问题。

# UI

## [radix](https://radix-ui.com.cn/primitives/docs/components/popover)

[中文API](https://www.radix-ui.com/primitives/docs/components/popover)

## [shadcn](https://www.shadcn-ui.cn/docs)

**Shadcn UI**是一个现代化的 UI 组件库，旨在帮助开发人员快速构建美观、响应式、可访问性强的用户界面。它基于**React**和**Tailwind CSS**开发，提供了各种预设计的 UI 组件，旨在通过简单的 API 和灵活的定制选项来加速开发流程。

**特点**

- 前面有提到Shadcn ui与一般的组件库最大的不同在于，所有的元件都可以直接在项目中进行编辑，**按需加载，加载的原件会出现在项目的components/ui目录下**

> [Shadcn UI可定制UI 框架](https://www.kancloud.cn/idcpj/python/3249022)

```shell
npx shadcn@latest init  #初始化项目
npx shadcn@latest add button #加载按钮

npx shadcn@latest add #直接回车会列出所有元素，供选择
```











# [调试](https://mdnice.com/writing/9f73edcfaf4641a79b063bca87c6151b)

# JSX

JSX的全称是 `Javascript and XML`，React发明了JSX，它是一种可以在JS中编写XML的语言，扩展了JavaScript语法，具有JavaScript的全部功能。[JSX到底是什么东西](https://juejin.cn/post/7034765440362479646).

JSX是JS的语法糖，语法上更接近JavaScript，而非HTML，编译时JSX会通过Babel编译成JS，即调用React.createElement()。

1. 使用小驼峰定义属性名
2. 变量和表达式放在{ }中

```jsx
const element = <h1 className="app">Hello, { name }</h1>;
```

3. 内联样式

   ```jsx
   const style = {
     fontSize: 100,
     color: '#FF0000'
   }
   const element = <h1 style={ style }>Hello, { name }</h1>;
   ```

   





# 参考

[参考React基础](https://nextjs.org/learn/react-foundations)，[对应中文版](https://nextjs.net.cn/learn/react-foundations)

[React官方快速入门](https://reactjs.ac.cn/learn)

UI 组件库（如 Radix UI、Chakra UI、Material-UI 等）来提升开发效率