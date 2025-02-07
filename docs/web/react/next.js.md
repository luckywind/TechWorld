# 路由

Next.js 使用基于文件系统的路由器，其中：

- **文件夹**用于定义路由。路由是一个从**根文件夹**到包含 `page.js` 文件的最终**叶子文件夹**的嵌套文件夹的单一路径，遵循文件系统层次结构。参见 [定义路由](https://nextjscn.org/docs/app/building-your-application/routing/defining-routes)。 <u>使用特殊的 [`page.js` 文件](https://nextjscn.org/docs/app/building-your-application/routing/pages) 可以使路由段公开访问。</u>
- **文件**用于创建显示在路由段中的 UI。参见 [特殊文件](https://nextjscn.org/docs/app/building-your-application/routing#file-conventions)。

- 在嵌套路由中，段的组件将嵌套在其父段的组件**内部**。

# **页面**

页面是对应特定路由的**唯一** UI。你可以通过在 `page.js` 文件中默认导出一个组件来定义页面。

要创建页面，就在某个文件夹下创建page.js文件，文件中默认导出一个组件。

- 主页
  是应用的默认入口页面，通常对应app/page.js文件，它的内容会被渲染在根布局中的{children}占位符处。只负责主页特定的内容，不包含全局共享的部分。

# **布局**

用于显示在多个路由之间共享的UI.在导航时，布局会保持状态，保持交互性，并且不会重新渲染。布局也可以 [嵌套](https://nextjscn.org/docs/app/building-your-application/routing/layouts-and-templates#nesting-layouts)。Next.js 会自动应用与页面路径匹配的布局文件。

特殊文件 [layout.js](https://nextjscn.org/docs/app/building-your-application/routing/layouts-and-templates#layouts) 和 [template.js](https://nextjscn.org/docs/app/building-your-application/routing/layouts-and-templates#templates) 允许你创建在多个 [路由](https://nextjscn.org/docs/app/building-your-application/routing/defining-routes#creating-routes) 之间共享的 UI

layout里的children相当于挖了一个坑，同级的page.js的内容会填进去。

- 每个页面都会嵌入到同级或最近的上级目录中的layout布局文件中。所以一个layout布局文件同级目录及子目录下的页面都会在访问时自动嵌入进来。
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

  

- 嵌套布局：文件夹层次结构中的布局是**嵌套的**，这意味着它们通过 `children` 属性包裹子布局。文件夹下创建layout.js

  



# 模板

模板与布局类似，都是包裹子布局或页面。但与在路由间保持状态的布局不同，模板在导航时会为其每个子组件创建一个新实例。这意味着当用户在共享模板的路由之间导航时，会挂载子组件的新实例，重新创建 DOM 元素，客户端组件中的状态**不会**保留，并且会重新同步 effects。

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

当在表单中调用时，操作会自动接收 [`FormData`](https://developer.mozilla.org/docs/Web/API/FormData/FormData) 对象。你不需要使用 React `useState` 来管理字段，而是可以使用原生的 [`FormData` 方法](https://developer.mozilla.org/en-US/docs/Web/API/FormData#instance_methods)来提取数据：

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







# 参考

[NEXT.js中文文档](https://nextjscn.org/docs/getting-started/installation)

[blog教程](https://nextjs.org/learn-pages-router/basics/create-nextjs-app)

