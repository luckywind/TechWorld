# 路由

Next.js 使用基于文件系统的路由器，其中：

- **文件夹**用于定义路由。路由是一个从**根文件夹**到包含 `page.js` 文件的最终**叶子文件夹**的嵌套文件夹的单一路径，遵循文件系统层次结构。参见 [定义路由](https://nextjscn.org/docs/app/building-your-application/routing/defining-routes)。 <u>使用特殊的 [`page.js` 文件](https://nextjscn.org/docs/app/building-your-application/routing/pages) 可以使路由段公开访问。</u>
- **文件**用于创建显示在路由段中的 UI。参见 [特殊文件](https://nextjscn.org/docs/app/building-your-application/routing#file-conventions)。

- 在嵌套路由中，段的组件将嵌套在其父段的组件**内部**。

## **页面**

页面是对应特定路由的**唯一** UI。你可以通过在 `page.js` 文件中默认导出一个组件来定义页面。

要创建页面，就在某个文件夹下创建page.js文件，文件中默认导出一个组件。

- 主页
  是应用的默认入口页面，通常对应app/page.js文件，它的内容会被渲染在根布局中的{children}占位符处。只负责主页特定的内容，不包含全局共享的部分。



## 元数据

通过元数据修改`<head>` HTML元素，入titile和meta

```js
export const metadata = {
  title: '页面标题',
}
export default xxx;
```

## 错误处理

错误可以分为两类：**预期错误**和**未捕获的异常**：

- **将预期错误建模为返回值**：在 Server Actions 中避免使用 `try`/`catch` 来处理预期错误。使用 [`useActionState`](https://react.dev/reference/react/useActionState) 来管理这些错误并将其返回给客户端。
- **对于意外错误使用错误边界**：使用 `error.tsx` 和 `global-error.tsx` 文件实现错误边界，以处理意外错误并提供备用 UI

## 路由组

用途： 将路由段和项目文件组织成逻辑组，而不会影响URL路径结构。

给文件夹添加圆括号变为路由组，不会出现在URL路径中。

- 不同路由组中可以有自己的布局文件

- 同组页面共享组内布局
  有了组内布局，甚至可以不要顶层布局，从而实现多个根布局，只是每个根布局需要有`<html>,<body>`标签

  ```shell
  app
  - layout.ts
  - - (marketing)
  - - - layout.js
  - - (shop) //该组内的多个页面共享一个布局
  - - - account
  - - - - page.js
  - - - cart
  - - - - page.js
  - - - layout.js
  ```

## 动态路由

用途：从动态数据中创建路由段。动态段可以在请求时填充或在构建时渲染。

将文件夹名称用方括号括起来可以创建动态段：`[folderName]`。例如，`[id]` 或 `[slug]`

动态段会作为 `params` 属性传递给 [`layout`](https://nextjscn.org/docs/app/api-reference/file-conventions/layout)、[`page`](https://nextjscn.org/docs/app/api-reference/file-conventions/page)、[`route`](https://nextjscn.org/docs/app/building-your-application/routing/route-handlers) 和 [`generateMetadata`](https://nextjscn.org/docs/app/api-reference/functions/generate-metadata#generatemetadata-function) 函数。

- 捕获所有段
  动态段可以通过在方括号中添加省略号来扩展为**捕获所有**后续段：`[...folderName]`
- 可选捕获所有段
  将捕获所有段设为**可选**：`[[...folderName]]`， 区别在于，可选模式下也会匹配不带参数的路由

## 并行路由

用途： 在同一个布局中同时或者有条件地渲染一个或多个页面，对于高度动态的部分非常有用。

插槽：@开头的目录， 它并非路由段，不影响URL结构。

- 同一个插槽可以有多个子页面
- 插槽也可以有自己的布局，子页面共享

例如，同时渲染"team"和"analytics"页面：

![image-20250421160943015](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250421160943015.png)



## Middleware

Middleware 允许你在请求完成前运行代码。然后，基于传入的请求，你可以通过重写、重定向、修改请求或响应头，或直接响应来修改响应。







# 布局和模板

## **布局**

**用于显示在多个路由之间共享的UI.在导航时，布局会保持状态，保持交互性，并且不会重新渲染。**布局也可以 [嵌套](https://nextjscn.org/docs/app/building-your-application/routing/layouts-and-templates#nesting-layouts)。Next.js 会自动应用与页面路径匹配的布局文件。

特殊文件 [layout.js](https://nextjscn.org/docs/app/building-your-application/routing/layouts-and-templates#layouts) 和 [template.js](https://nextjscn.org/docs/app/building-your-application/routing/layouts-and-templates#templates) 允许你创建在多个 [路由](https://nextjscn.org/docs/app/building-your-application/routing/defining-routes#creating-routes) 之间共享的 UI

layout里的children相当于挖了一个坑，同级的page.js的内容会填进去。

- 通过在 `layout.js` 文件中默认导出一个 React 组件来定义布局。该组件应**接受一个 `children` 属性**，在渲染期间将填充子布局（如果存在）或页面

- **每个页面都会嵌入到同级或最近的上级目录中的layout布局文件中。所以一个layout布局文件同级目录及子目录下的页面都会在访问时自动嵌入进来。**

- 布局文件中适合放一些多个页面共享的组件

- 根布局是必须的，必须包含html和body标签
  提供全局框架，管理所有页面的公共部分

  ```jsx
  // app/layout.js
  export default function RootLayout({ children }) {
    return (
      <html lang="en">
        <body>{children}</body>
      </html>
    )
  }
  ```

  

- 嵌套布局：文件夹层次结构中的布局是**嵌套的**，这意味着它们通过 `children` 属性包裹子布局。在路由段文件夹下创建layout.js来创建嵌套布局。

  



## 模板

模板与布局类似，都是包裹子布局或页面。但与在路由间保持状态的布局不同，模板在导航时会为其每个子组件创建一个新实例。这意味着当用户在共享模板的路由之间导航时，会挂载子组件的新实例，重新创建 DOM 元素，客户端组件中的状态**不会**保留，并且会重新同步 effects。

模板使用场景：

- 在导航时重新同步 `useEffect`。
- 在导航时重置子 Client Components 的状态。

可以通过从 `template.js` 文件中导出一个默认的 React 组件来定义模板。该组件应接受一个 `children` 属性。

```jsx
// app/template.tsx
export default function Template({ children }: { children: React.ReactNode }) {
  return <div>{children}</div>
}
```

实际效果就是page.jsx先在Template渲染一次，再到layout布局里渲染一次：

```jsx
<Layout>
  {/* 注意，模板被赋予了一个唯一的键。 */}
  <Template key={routeParam}>{children}</Template>
</Layout>
```

![image-20250115095924226](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250115095924226.png)



# 链接和导航

## 激活的导航链接

可以使用 [usePathname()](https://nextjscn.org/docs/app/api-reference/functions/use-pathname) hook 来确定导航链接是否处于激活状态。

```jsx
'use client'
 
import { usePathname } from 'next/navigation'
import Link from 'next/link'
 
export function Links() {
  const pathname = usePathname() // 获取当前URL
 
  return (
    <nav>
      <Link className={`link ${pathname === '/' ? 'active' : ''}`} href="/">
        首页
      </Link>
      
      <Link className={pathname === "/" ? "text-purple-500":""} href="/">
        首页2
      </Link>
 
      <Link
        className={`link ${pathname === '/about' ? 'active' : ''}`}
        href="/about"
      >
        关于
      </Link>
    </nav>
  )
}
```





1. `<Link>` 是一个内置组件，它扩展了 HTML 的 `<a>` 标签

2. useRouter()  hook 允许以编程方式更改路由

   > 除非你有使用 `useRouter` 的特定需求，否则请使用 `<Link>` 组件进行路由导航。

   ```jsx
   'use client'
    
   import { useRouter } from 'next/navigation'
    
   export default function Page() {
     const router = useRouter()
    
     return (
       <button type="button" onClick={() => router.push('/dashboard')}>
         仪表盘
       </button>
     )
   }
   ```

3. redirect函数
   服务器组件使用

   ```jsx
   import { redirect } from 'next/navigation'
   ...
   redirect('/login')
   ```

- `redirect` 也接受绝对 URL，可用于重定向到外部链接。



# 数据获取

## 客户端获取数据

```jsx
'use client'
 
import { useState, useEffect } from 'react'
 
export function Posts() {
  const [posts, setPosts] = useState(null)
 
  useEffect(() => {
    async function fetchPosts() {
      let res = await fetch('https://api.vercel.app/blog')
      let data = await res.json()
      setPosts(data)
    }
    fetchPosts()
  }, [])
 
  if (!posts) return <div>Loading...</div>
 
  return (
    <ul>
      {posts.map((post) => (
        <li key={post.id}>{post.title}</li>
      ))}
    </ul>
  )
}
```

# 服务器操作和数据变更

处理表单提交和数据修改

[服务器操作](https://react.dev/reference/rsc/server-actions)是在服务器上执行的**异步函数**。它们可以在服务器组件和客户端组件中调用，用于处理 Next.js 应用程序中的表单提交和数据修改。

约定： 服务器操作可以通过 React 的 [`"use server"`](https://react.dev/reference/react/use-server) 指令定义。你可以将该指令放在 `async` 函数体的顶部以将该函数标记为服务器操作，或者放在单独文件的顶部以将该文件的所有导出标记为服务器操作。

1. 服务器组件

```jsx
export default function Page() {
  // 服务器操作
  async function create() {
    'use server'
    // 修改数据
  }
 
  return '...'
}
```

2. 客户端组件调用服务器操作

app/action.js  服务器组件导出一个可被客户端和服务器端组件重用的服务器操作：

```jsx
'use server'
 
export async function create() {}
```

app/ui/button.js  客户端组件调用服务器组件

```jsx
'use client'
 
import { create } from '@/app/actions'
 
export function Button() {
  return <button onClick={() => create()}>Create</button>
}
```

3. 将服务器操作作为prop传递给客户端组件：
   客户端组件app/client-component.js：

   ```jsx
   'use client'
    
   export default function ClientComponent({ updateItemAction }) {
     return <form action={updateItemAction}>{/* ... */}</form>
   }
   ```

   将服务器操作作为prop传递给客户端组件：

   ```jsx
   <ClientComponent updateItemAction={updateItem} />
   ```

## Form

React 继承了 HTML的`<form>`元素允许通过action prop调用Server端操作。

- 当在表单中调用时，操作会自动接收 [`FormData`](https://developer.mozilla.org/docs/Web/API/FormData/FormData) 对象。你不需要使用 React `useState` 来管理字段，而是可以使用原生的 [`FormData` 方法](https://developer.mozilla.org/en-US/docs/Web/API/FormData#instance_methods)来提取数据：

```jsx
export default function Page() {
  async function createInvoice(formData) {
    'use server'
 
    const rawFormData = {
      customerId: formData.get('customerId'),
      amount: formData.get('amount'),
      status: formData.get('status'),
    }
 
    // mutate data
    // revalidate cache
  }
 
  return <form action={createInvoice}>...</form>
}
```

[React Form](https://react.dev/reference/react-dom/components/form#handling-multiple-submission-types) 可以处理多个提交动作，一个表单里的每个按钮可以通过formAction属性关联不同的操作。

```js
export default function Search() {
  function publish(formData) {
    const content = formData.get("content");
    const button = formData.get("button");
    alert(`'${content}' was published with the '${button}' button`);
  }

  function save(formData) {
    const content = formData.get("content");
    alert(`Your draft of '${content}' has been saved!`);
  }

  return (
    <form action={publish}>
      <textarea name="content" rows={4} cols={40} />
      <br />
      <button type="submit" name="button" value="submit">Publish</button>
      <button formAction={save}>Save draft</button>
    </form>
  );
}
```









# rest 请求

```js
import * as React from "react";
async function getBuildArtifacts(build_num) {
    setArtifactsLoading(true);
    setArtifacts([]);
    let pipeline_id = productsMap[pipelineName]; //初始化一个变量
    try {
      const response = await fetch( //构造get请求
        ApiHost +
          `/history/package?pipeline_id=${pipeline_id}&build_num=${build_num}`,
      );
      if (!response.ok) {
        toast({
          className: cn(
            "top-0 right-0 flex fixed md:max-w-[420px] md:top-4 md:right-4",
          ),
          duration: 2000,
          title: "请求失败",
          description: `请求异常: ${response.status}`,
        });
      }
      const artifactsData = await response.json();
      setArtifacts(artifactsData["data"]["records"]);
    } catch (error) {
      toast({
        className: cn(
          "top-0 right-0 flex fixed md:max-w-[420px] md:top-4 md:right-4",
        ),
        duration: 2000,
        title: "请求失败",
        description: `请求异常: ${error}`,
      });
    } finally {
      setArtifactsLoading(false);
    }
  }


构造post请求
      const response = await fetch(ApiHost + "/pipeline/product/build", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ pipeline_id: pipeline_id,tag: baseTag, modules: submodules }),
      });

```







# 登录登出

[参考](https://medium.com/@dorinelrushi8/how-to-create-a-login-page-in-next-js-f4c57b8b387d)

```shell
npx create-next-app@latest login-page
cd login-page
npm install bcryptjs jsonwebtoken next-auth axios
```

## middleware

### 拦截请求

middleware.js，Next.js 会自动识别并在匹配的 API 路由（`matcher` 里定义的路径）执行这个中间件。

middleware [matcher](https://medium.com/@turingvang/how-to-use-matcher-in-next-js-middleware-cf18f441d52a)

定义了哪些路由触发中间件，使用config对象配置，是一个路由/路由模式数组。特点：

1. 静态路由

2. 动态路由

   - `:path*`代表所有子路由

   - 排除路由： 可以直接在中间件逻辑里处理

     ```js
     export function middleware(req) {
       if (req.nextUrl.pathname.startsWith('/login') || req.nextUrl.pathname.startsWith('/signup')) {
         return NextResponse.next(); // Skip middleware for /login and /signup
       }
     
       // Apply middleware to all other routes
       return NextResponse.next();
     }
     
     export const config = {
       matcher: ['/:path*'], // Applies to all routes
     };
     ```

     





### [请求头添加token](https://nextjs.org/docs/app/building-your-application/routing/middleware#using-cookies)

**Token 存储在 Cookie 中**，自动在请求时附带 `Authorization` 头。

```js
import { NextResponse } from "next/server";

export function middleware(req) {
    const token = req.cookies.get("token")?.value; // 从 Cookie 获取 Token

    if (!token) {
        return NextResponse.redirect(new URL("/login", req.url)); // 没有 Token，重定向到登录页
    }

    const requestHeaders = new Headers(req.headers);
    requestHeaders.set("Authorization", `Bearer ${token}`); // 添加 Token 到请求头

    return NextResponse.next({
        request: {
            headers: requestHeaders,
        },
    });
}

export const config = {
    matcher: ["/api/:path*", "/protected/:path*"], // 只对 API 或受保护路由生效
};

```

这里的 **`Bearer` 表示 "持有者令牌"（Bearer Token）**，服务器通过它来识别用户身份。





## fetch添加token

1. 前端设置credentials

   ```js
       const response = await fetch(ApiHost + "/pipeline/list?type=2", {
         method: "GET",
         credentials: "include",  // 允许携带 Cookie
         headers: {
             "Content-Type": "application/json"
         },
       });
   ```

2. 请求后端使用域名，而非IP地址
   ApiHost使用域名, 相应的Django的ALLOWED_HOSTS也要把这个域名加上，否则会报400 BadRequest。

3. 后端允许Cookie

Django后端允许跨域请求携带Cookie,否则浏览器不会发送Cookie。

```python
CORS_ALLOWED_ORIGINS = ["http://vmp-dev.yusur.tech","http://vmp-build.yusur.tech"]
CORS_ALLOW_CREDENTIALS = True  # 允许跨域携带 Cookie
```



![image-20250310093614610](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250310093614610.png)





# 其他

1. 从 5.2 版本开始，Node.js 捆绑了 [`npx`命令](https://flaviocopes.com/npx/)。这个方便的工具可以让我们下载并执行一个 JavaScript 命令，

`npx create-next-app` 该命令询问应用程序的名称



# 参考

[NEXT.js中文文档](https://nextjscn.org/docs/getting-started/installation)

[blog教程](https://nextjs.org/learn-pages-router/basics/create-nextjs-app)

[next完全手册](https://www.freecodecamp.org/chinese/news/the-next-js-handbook/)

## [NextJS（8部曲）](https://dev.to/skipperhoa/create-a-middleware-in-nextjs-13-17oh)

1. swr( Stale-While-Revalidate ）库方便请求API
   `npm i swr`   安装swr

```js
import useSWR from 'swr'
const fetcher = (url:string) => fetch(url).then(res => res.json())
export default function Post() {
    const { data: posts, error, isLoading } = useSWR('/api/posts', fetcher)
  if (error) return <div>Failed to load</div>;
  if (isLoading) return <div>Loading...</div>;
  if (!data) return null;
  return (
    < 组件数据/>
  )
```

2. generateStaticParams函数会在应用运行时首先调用，可用于获取数据

3. Middleware

4. form 表单

   ```js
   export default function PostEdit({params} :{params:{id:number}}) {
     const router = useRouter()
     const {data : post,isLoading, error} = useSWR(`/api/posts/${params.id}`,fetcher)
     const [title, setTitle] =useState<string>('');
     const [body, setBody] = useState<string>('');
     useEffect(()=>{
        if(post){
            setTitle(post.result.title)
            setBody(post.result.content)
        }
     },[post, isLoading])
     const updatePost = async (e: any) => {
     };
     if(isLoading) return <div><span>Loading...</span></div>
     if (!post) return null;
     return (
       <form className='w-full' onSubmit={updatePost}>
           <div className='w-full py-2'>
             <button className="w-20 p-2 text-white border-gray-200 border-[1px] rounded-sm bg-green-400">Submit</button>
           </div>
       </form>
     )
   }
   ```

   

[youtube next.js简介](https://www.youtube.com/watch?v=703cQNdRNPU)



