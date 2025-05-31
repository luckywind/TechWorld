# React基础

- **JS**：基础的脚本语言，处理逻辑和动态内容。
- **JSX**：基于JS的语法糖，专为声明式UI设计，需转译为JS后执行。
- 可以在JSX中嵌入JavaScript表达式，用大括号{}包裹。这样动态的内容就可以和静态的HTML结构混合在一起。而普通的JS则需要用createElement这样的函数来构建DOM结构，比较繁琐。

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

1. <u>组件需要大写开头</u>， 这是与普通HTML标签最大的区别。
2. 组件的使用方式与HTML标签相同，使用尖括号<>。 且只能返回一个JSX标签，如果需要返回多个，必须包装到一个共享的父级中，例如`<div>...<div>`或者一个空的`<>...</>`包装器。
2. 返回值开头必须紧跟 return后头，除非用括号，否则下一行会被忽略。
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

在单独的css文件中编写css规则

```css
.avatar {
  border-radius: 50%;
}
```

添加css文件最基本的方式是使用`<Link>`标签，具体参考使用的框架。

## 使用Props显示数据

类似于 JavaScript 函数，组件也可以带参数（或 props），这些参数会更改组件的行为或在渲染到屏幕上时可见显示的内容。然后，您可以将这些 props 从父组件传递到子组件。

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



解构：用花括号声明Props:

```jsx
function Avatar({ person, size }) {
  // 在这里 person 和 size 是可访问的
}
```



4. 传递css对象
   可以使用花括号表示对象`{ }` , 对象也可以在JSX中传递

   ```jsx
   export default function TodoList() {
     return (
       <ul style={
                   {
                 backgroundColor: 'black',
                 color: 'pink'
               }
         }>
         <li>Improve the videophone</li>
         <li>Prepare aeronautics lectures</li>
         <li>Work on the alcohol-fuelled engine</li>
       </ul>
     );
   }
   ```

   

1. `baseUrl + person.imageId + person.imageSize + '.jpg'` 会生成正确的 URL 字符串



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





### 渲染列表

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

**Hook 比其他函数更严格。您只能在组件（或其他 Hook）的 *顶部* 调用 Hook。如果您想在条件或循环中使用 `useState`，请提取一个新组件并将其放在那里。**





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

## 快速入门



### 井字棋

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



#### 不变性很重要

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

### React思考方式

使用React构建用户界面时，你首先会将其分解成称为*组件*的片段。然后，你将描述每个组件的不同视觉状态。最后，你将组件连接在一起，以便数据流经它们。

**步骤一**：将UI分解成组件层次结构
一个组件理想情况下只做一件事情

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/s_thinking-in-react_ui_outline.png)

这五个组件的层次结构如下：

```markdown
FilterableProductTable
  SearchBar
  ProductTable
    ProductCategoryRow
    ProductRow
```

**步骤二**：构建静态版本
先不考虑交互性，把组件自上而下或者自下而上地逐个构建：

```jsx
function ProductCategoryRow({ category }) {
  return (
    <tr>
      <th colSpan="2">
        {category}
      </th>
    </tr>
  );
}

function ProductRow({ product }) {
  const name = product.stocked ? product.name :
    <span style={{ color: 'red' }}>
      {product.name}
    </span>;

  return (
    <tr>
      <td>{name}</td>
      <td>{product.price}</td>
    </tr>
  );
}

function ProductTable({ products }) {
  const rows = [];
  let lastCategory = null;

  products.forEach((product) => {
    if (product.category !== lastCategory) {
      rows.push(
        <ProductCategoryRow
          category={product.category}
          key={product.category} />
      );
    }
    rows.push(
      <ProductRow
        product={product}
        key={product.name} />
    );
    lastCategory = product.category;
  });

  return (
    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>Price</th>
        </tr>
      </thead>
      <tbody>{rows}</tbody>
    </table>
  );
}

function SearchBar() {
  return (
    <form>
      <input type="text" placeholder="Search..." />
      <label>
        <input type="checkbox" />
        {' '}
        Only show products in stock
      </label>
    </form>
  );
}

function FilterableProductTable({ products }) {
  return (
    <div>
      <SearchBar />
      <ProductTable products={products} />
    </div>
  );
}

const PRODUCTS = [
  {category: "Fruits", price: "$1", stocked: true, name: "Apple"},
  {category: "Fruits", price: "$1", stocked: true, name: "Dragonfruit"},
  {category: "Fruits", price: "$2", stocked: false, name: "Passionfruit"},
  {category: "Vegetables", price: "$2", stocked: true, name: "Spinach"},
  {category: "Vegetables", price: "$4", stocked: false, name: "Pumpkin"},
  {category: "Vegetables", price: "$1", stocked: true, name: "Peas"}
];

export default function App() {
  return <FilterableProductTable products={PRODUCTS} />;
}
```

**步骤三**：最小状态

将状态视为应用程序需要记住的最小变化数据集合。

如何区分是否为状态？

- 它是否**随时间保持不变**？如果是，则它不是状态。
- 它是否**通过 props 从父组件传递**？如果是，则它不是状态。
- 您可以**根据组件中现有的状态或 props 计算它**吗？如果是，则它*绝对*不是状态！

1. 原始产品列表**作为 props 传递，因此它不是状态。**
2. 搜索文本似乎是状态，因为它会随时间变化，并且无法从任何地方计算出来。
3. 复选框的值似乎是状态，因为它会随时间变化，并且无法从任何地方计算出来。
4. 过滤后的产品列表**不是状态，因为它可以通过**获取原始产品列表并根据搜索文本和复选框的值对其进行过滤来计算。

这意味着只有搜索文本和复选框的值是状态！做得好！



**步骤四**：确定状态应该存在的位置

确定应用程序的最小状态数据后，您需要确定哪个组件负责更改此状态，或*拥有*该状态。

React 使用单向数据流，将数据从父组件向下传递到组件层次结构中的子组件，因此需要找到依赖这些状态的所有组件，然后找到这些组件的公共父组件，如果不存在则新建一个公共父组件，这个公共父组件就是要保存状态的组件！

```jsx
import { useState } from 'react';

function FilterableProductTable({ products }) {
  const [filterText, setFilterText] = useState('');
  const [inStockOnly, setInStockOnly] = useState(false);

  return (
    <div>
     ❤️ 将filterText和inStockOnly作为 props 传递给ProductTable和SearchBar
      <SearchBar 
        filterText={filterText} 
        inStockOnly={inStockOnly} />
      <ProductTable 
        products={products}
        filterText={filterText}
        inStockOnly={inStockOnly} />
    </div>
  );
}
```

**步骤五**：添加逆向数据流

为了根据用户输入更改状态，您需要支持反向数据流：层次结构中深层的表单组件需要更新 `FilterableProductTable` 中的状态。状态由 `FilterableProductTable` 拥有，因此只有它才能调用 `setFilterText` 和 `setInStockOnly`。为了让 `SearchBar` 更新 `FilterableProductTable` 的状态，您需要将这些函数传递给 `SearchBar`。这样下游组件SearchBar就可以更改上游组件的状态了。

```jsx
function FilterableProductTable({ products }) {
  const [filterText, setFilterText] = useState('');
  const [inStockOnly, setInStockOnly] = useState(false);

  return (
    <div>
      <SearchBar 
        filterText={filterText} 
        inStockOnly={inStockOnly}
        onFilterTextChange={setFilterText}
        onInStockOnlyChange={setInStockOnly} />
      
      
function SearchBar({
  filterText,
  inStockOnly,
  onFilterTextChange,
  onInStockOnlyChange
}) {
  return (
    <form>
      <input
        type="text"
        value={filterText}
        placeholder="Search..." 
        onChange={(e) => onFilterTextChange(e.target.value)}
      />
      <label>
        <input
          type="checkbox"
          checked={inStockOnly}
          onChange={(e) => onInStockOnlyChange(e.target.checked)}      
```



## 描述UI

[组件](https://reactjs.ac.cn/learn/your-first-component)：UI构建块，React 允许你将你的标记、CSS 和 JavaScript 结合到自定义“组件”中。

组件的标记有几种：像`<img/>`这种jsx语法，大写开头的标记被认为是自定义的标记。

JSX语法比HTML更严格：

1. 返回单个根元素
2. 关闭所有标签
   自闭合标签，例如 `<img>` 必须变成 `<img />`，而包装标签，例如 `<li>oranges` 必须写成 `<li>oranges</li>`。
3. 驼峰式命名
   JSX 会转换成 JavaScript，在 JSX 中编写的属性会成为 JavaScript 对象的键，但是 JavaScript 对变量名有限制。
4. JSX 类似于 HTML，但也有一些不同之处。如果需要，可以使用 [转换器](https://transform.tools/html-to-jsx)。



### 组件

是js函数，建议组件函数都在文件顶层定义，当子组件需要来自父组件的一些数据时，[通过 props 传递它](https://reactjs.ac.cn/learn/passing-props-to-a-component)，而不是嵌套定义。

#### 导入导出

| 语法 | 导出语句                              | 导入语句                                |
| ---- | ------------------------------------- | --------------------------------------- |
| 默认 | `export default function Button() {}` | `import Button from './Button.js';`     |
| 具名 | `export function Button() {}`         | `import { Button } from './Button.js';` |

1. **一个文件里有且仅有一个默认 导出，但是可以有任意多个具名 导出。**

2. **具名导入一定带花括号{}**
3. 当使用默认导入时，你可以在 `import` 语句后面进行任意命名。比如 `import Banana from './Button.js'`，如此你能获得与默认导出一致的内容。相反，对于具名导入，导入和导出的名字必须一致。这也是称其为 **具名** 导入的原因！

### JSX书写标签语言

JSX规则：

1. 只能 返回一个根元素
   如果想要在一个组件中包含多个元素，**需要用一个父标签把它们包裹起来**。例如<>` 和 `</>

   > JSX 虽然看起来很像 HTML，但在底层其实被转化为了 JavaScript 对象

2. 标签必须闭合

3. 使用驼峰命名属性
   而 JSX 中的属性也会变成 JavaScript 对象中的键值对，但 JavaScript 对变量的命名有限制。例如，变量名称不能包含 `-` 符号或者像 `class` 这样的保留字

## 添加交互性

### 响应事件

可以通过props给自定义组件传递自定义处理函数，但是`<button>`等内置组件只支持内置浏览器事件，如onClick。

**事件处理函数**

1. **定义事件处理函数:**

- 通常在你的组件 **内部** 定义。
- 名称以 `handle` 开头，后跟事件名称。

或者，你也可以在 JSX 中定义一个**内联**的事件处理函数：

```jsx
<button onClick={function handleClick() {
  alert('你点击了我！');
}}>
```

或者，直接使用更为简洁箭头函数：

```jsx
<button onClick={() => {
  alert('你点击了我！');
}}>
```

2. **访问组件的props**
   由于处理函数定义在组件内部，所以可以访问组的props

3. **通过props传递给子组件**
   注意，不要调用！！！

   ```jsx
   function Button({ onClick, children }) {
     return (
       <button onClick={onClick}>
         {children}
       </button>
     );
   }
   
   function PlayButton({ movieName }) {
     //父组件定义一个处理函数
     function handlePlayClick() {
       alert(`正在播放 ${movieName}！`);
     }
   
     return (
       //传递给子组件
       <Button onClick={handlePlayClick}>
         播放 "{movieName}"
       </Button>
     );
   }
   ```

4. 事件传播
   事件会沿着树向上冒泡或者传播

   ```jsx
   export default function Toolbar() {
     return (
       <div className="Toolbar" onClick={() => {
         alert('你点击了 toolbar ！');
       }}>  
         // button的事件会传播到div
         <button onClick={() => alert('正在播放！')}>
           播放电影
         </button>
         <button onClick={() => alert('正在上传！')}>
           上传图片
         </button>
       </div>
     );
   }
   
   ```

5. 阻止传播
   事件处理函数接收一个 **事件对象** 作为唯一的参数。按照惯例，它通常被称为 `e` ，代表 “event”（事件）。你可以使用此对象来读取有关事件的信息。

   这个事件对象还允许你阻止传播。如果你想阻止一个事件到达父组件，你需要像下面 `Button` 组件那样调用 `e.stopPropagation()` ：

   ```jsx
   function Button({ onClick, children }) {
     return (
       <button onClick={e => {//这是事件处理函数，可以向这里添加更多代码，此模式是事件传播的另一种替代方案
         // ❤️它让子组件处理事件，同时也让父组件指定一些额外的行为
   			e.stopPropagation();
         onClick();
       }}>
         {children}
       </button>
     );
   }
   ```

   



下面的代码注意：

1. 在 JSX 里，位于组件标签之间的内容会被当作 `children` props 传递给该组件，这是建议的做法
2. 也可以用空格分割不同的属性

```jsx
export default function App() {
  return (
    <Toolbar
      onPlayMovie={() => alert('Playing!')}
      onUploadImage={() => alert('Uploading!')}
    />
  );
}

function Toolbar({ onPlayMovie, onUploadImage }) {
  return (
    <div>
             // 这里用空格分割不同的属性
      <Button onClick={onPlayMovie} children='Play Movie'>
      </Button>
        		 // Button标签内的“Upload Image” 将作为children props传递给Button组件
      <Button onClick={onUploadImage}>
        Upload Image  
      </Button>
    </div>
  );
}

function Button({ onClick, children }) {
  return (
    <button onClick={onClick}>
      {children}
    </button>
  );
}
```



### state组件状态

**state 完全私有于声明它的组件**。父组件无法更改它。这使你可以向任何组件添加或删除 state，而不会影响其他组件。

1. state声明必须在组件开头
2. 以相同的顺序调用Hook

```jsx
import { useState } from 'react';
export default function FeedbackForm() {
  const [isSent, setIsSent] = useState(false);
  const [message, setMessage] = useState('');
  ...
}
```

3. state变量仅用于在组件重渲染时保存信息，在单个函数中，普通变量就足够了

```jsx
import { useState } from 'react';

export default function FeedbackForm() {
  const [name, setName] = useState('');

  function handleClick() {
    setName(prompt('What is your name?'));
    alert(`Hello, ${name}!`); // 为什么这里不能拿到name的最新值？
  }

  return (
    <button onClick={handleClick}>
      Greet
    </button>
  );
}

```

### state不变快照

1. React调用组件(一个函数)的过程就是渲染的过程，组件返回的JSX就像是一个UI快照，它的props和内部变量都是根据当前渲染时的state计算出来的，而state是位于函数之外的React本身中。

2. **当前UI是根据渲染时的state计算出来的快照，不会变更state的值，**React 会使 state 的值始终“固定”在一次渲染的各个事件处理函数内部**。你无需担心代码运行时 state 是否发生了变化。设置 state 只会为下一次渲染变更 state 的值，并请求一次新的渲染**

3. 批处理：**React 会等到事件处理函数中的** 所有 **代码都运行完毕再处理你的 state 更新。** 

分析下面这段代码会让number变成多少？

```jsx
import { useState } from 'react';

export default function Counter() {
  const [number, setNumber] = useState(0);

  return (
    <>
      <h1>{number}</h1>
      <button onClick={() => {
        setNumber(number + 1);
        setNumber(number + 1);
        setNumber(number + 1);
      }}>+3</button>
    </>
  )
}

```

实际上number 是一个state，第一次渲染时React告诉组件state=0,  于是代码等同于下面

```jsx
import { useState } from 'react';

export default function Counter() {
  const [number, setNumber] = useState(0);

  return (
    <>
      <h1>{number}</h1>
      <button onClick={() => {
        // 这里其实就相当于告诉React下一次渲染时把number改为1，只不过告诉了三次
        //(渲染了三次？没有，因为React 会等到事件处理函数中的 所有 代码都运行完毕再处理你的 state 更新)
        setNumber(0 + 1);
        setNumber(0 + 1);
        setNumber(0 + 1);
      }}>+3</button>
    </>
  )
}

```

所以点击按钮后，number实际上变成了1, 而不是3。

### 多次更新同一个state

- 像 `setNumber(n => n + 1)` 这样传入一个根据队列中的前一个 state 计算下一个 state 的 **函数**， 这是一种告诉 React “用 state 值做某事”而不是仅仅替换它的方法。

`n => n + 1` 被称为 **更新函数**。当你将它传递给一个 state 设置函数时：

1. React 会将此函数加入队列，以便在事件处理函数中的所有其他代码运行后进行处理。
2. 在下一次渲染期间，React 会遍历队列并给你更新之后的最终 state。

- setState(x)实际上会像 setState(n => x) 一样运行，只是没有使用 n， 本质也是一个更新函数
  ```jsx
        <button onClick={() => {
          setNumber(number + 5);
          setNumber(n => n + 1);
        }}>增加数字</button>
  
  点击按钮后number是6 
  ```

  

### 更新state中的对象

state 中可以保存任意类型的 JavaScript 值，包括对象。但是，你不应该直接修改存放在 React state 中的对象。相反，当你想要更新一个对象时，你需要创建一个新的对象（或者将其拷贝一份），然后将 state 更新为此对象。

**把所有存放在 state 中的 JavaScript 对象都视为只读的**。

> 原因在于直接修改state中的对象并不会触发重渲染，并会改变前一次渲染“快照”中 state 的值。

使用...对象展开语法复制(浅拷贝，只能一层)对象并更新某个属性：

```jsx
setPerson({
  ...person, // 复制上一个 person 中的所有字段
  firstName: e.target.value // 但是覆盖 firstName 字段 
});
```

如果是嵌套对象，考虑使用Immer库







## 管理状态

### 用State响应输入







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

CommandItem：

- key:`key` 仅在 React 内部使用，不会作为属性传递给组件本身
- value:`value` 属性代表 `CommandItem` 的值，这个值会在用户选择该项时传递给 `onSelect` 回调函数。你可以将它看作是该项的 “数据标识”，用于在用户交互时获取与该项相关的数据。 `value` 被设置为 `module.pipeline_id`，这样当用户选择某个 `CommandItem` 时，就可以获取到对应的 `pipeline_id`。
- onSelect: `onSelect` 是一个回调函数，当用户选择某个 `CommandItem` 时会触发该函数。它接收一个参数 `value`，这个参数就是 `CommandItem` 的 `value` 属性值。



一个Demo：

```jsx
function selectorForPipelineId(open, setOpen, pipelineId, products, setpipelineId) {
    return <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
            <Button
                variant="outline"
                role="combobox"
                aria-expanded={open}
                className="border border-gray-300 p-2 w-full mb-2"
            >
                {pipelineId
                    ? products.find(
                        (module) => module.pipeline_id === pipelineId
                    )?.pipeline_name
                    : "选择流水线"}
                <ChevronsUpDown className="opacity-50" />
            </Button>
        </PopoverTrigger>
        <PopoverContent className="w-[250px] p-0">
            <Command>
                <CommandInput placeholder="选择流水线" />
                <CommandList>
                    <CommandEmpty>未选择</CommandEmpty>
                    <CommandGroup>
                        {products.map((module) => (
                            <CommandItem 
                                // 这个key是React自己用的，不会传递
                                key={module.pipeline_id}
                                // 这个value即使传递module,onSelect中接收的value依然是pipeline_name，原                                   因未知
                                value={module}
                                onSelect={(value) => {
                                    let selectedPipelineId=products.find(
                                        (module) => module.pipeline_name === value
                                    )?.pipeline_id
                                    setpipelineId( selectedPipelineId === pipelineId ? "" : selectedPipelineId);
                                    setOpen(false);
                                } }
                            >
                                {module.pipeline_name}
                                <Check
                                    className={cn(
                                        "ml-auto",
                                        pipelineId === module.pipeline_id
                                            ? "opacity-100"
                                            : "opacity-0"
                                    )} />
                            </CommandItem>
                        ))}
                    </CommandGroup>
                </CommandList>
            </Command>
        </PopoverContent>
    </Popover>;
}
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

### 可新增的下拉选

`npm i react-select --legacy-peer-deps`

```jsx
<CreatableSelect isClearable options={['2200E','2200R','2200T','2200P'].map(value => ({ value, label: value }))}
                          value={productName}
                          onChange={setProductName}
                          placeholder="选择产品名称"
                         />
```

- `react-select` 的 `CreatableSelect` 要求 `options` 必须是 **包含 `value` 和 `label` 的对象数组**

- 这里onChange直接把选项对象set给了productName，父组件通过value告诉子组件显示当前选项对象。

  因此productName实际是{value:'2200E', label:'2200E'}

能否只接收其value呢？

```jsx
                         <CreatableSelect isClearable options={['2200E','2200R','2200T','2200P'].map(value => ({ value, label: value }))}
                            // 若productName非空，则创建一个选项对象，控制子组件显示
                            value={productName ? { value: productName, label: productName } : null}
                            // 当选择选项时，修改状态
                            onChange={option => setProductName(option ? option.value : null)}
                          placeholder="选择产品名称"
                         />
```



#### 受控组件的核心公式

父组件状态（state） → value prop → 子组件显示（View）   子组件交互（User Action） → onChange 回调 → 父组件更新状态（setState）

- **`value` 是 “单向输入”**：父组件告诉子组件 “你现在应该是什么样子”。声明当前状态，该状态只能是选项对象(或null),即使底层状态是字符串，也需要先转为选项。
  子组件自身没有独立状态，完全由父组件的 `value` 控制显示，这就是 **受控组件的本质**。
- **`onChange` 是 “单向输出”**：子组件告诉父组件 “用户让我变成了什么样子”。
- **合起来实现 “双向绑定”**：通过这两个 prop，父组件完全控制子组件的状态，同时子组件实时反馈用户操作，形成闭环。







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

[参考React基础](https://nextjs.org/learn/react-foundations)，[对应中文版](https://nextjs.net.cn/learn/react-foundations), [新官网](https://zh-hans.react.dev/learn/your-first-component)

[React官方快速入门](https://reactjs.ac.cn/learn)

UI 组件库（如 Radix UI、Chakra UI、Material-UI 等）来提升开发效率