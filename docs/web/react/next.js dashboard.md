React 是一个用于构建用户界面的 JAVASCRIPT 库。

JSX 是 JavaScript 语法的扩展。React 开发不一定使用 JSX ，但我们建议使用它。

[ES6](https://www.runoob.com/w3cnote/es6-tutorial.html)全称 ECMAScript 6.0 ，是 JavaScript 的下一个版本标准

# next.js

## [getting started](https://nextjs.org/learn/dashboard-app/getting-started)

<u>我们建议使用 [`pnpm`](https://pnpm.npmjs.net.cn/) 作为你的包管理器，因为它比 `npm` 或 `yarn` 更快更高效。</u>



```shell
$npm install -g pnpm
$cd my-app
$npx create-next-app@latest nextjs-dashboard --example "https://github.com/vercel/next-learn/tree/main/dashboard/starter-example" --use-pnpm
Success! Created nextjs-dashboard at /Users/chengxingfu/code/yusur/hados/my-next-app/nextjs-dashboard
Inside that directory, you can run several commands:

  pnpm run dev
    Starts the development server.

  pnpm run build
    Builds the app for production.

  pnpm start
    Runs the built app in production mode.

We suggest that you begin by typing:

  cd nextjs-dashboard
  pnpm run dev   #运行
```



项目目录结构：

- **`/app`**: Contains all the routes, components, and logic for your application, this is where you'll be mostly working from.
- **`/app/lib`**: Contains functions used in your application, such as reusable utility functions and data fetching functions.
- **`/app/ui`**: Contains all the UI components for your application, such as cards, tables, and forms. To save time, we've pre-styled these components for you.
- **`/public`**: Contains all the static assets for your application, such as images.
- **Config Files**: create-next-app自动生成的配置文件，无需关注



 占位数据：JS对象模拟的数据

```tsx
const invoices = [
  {
    customer_id: customers[0].id,
    amount: 15795,
    status: 'pending',
    date: '2022-12-06',
  },
  {
    customer_id: customers[1].id,
    amount: 20348,
    status: 'pending',
    date: '2022-11-14',
  },
  // ...
];
```

定义接口数据

```tsx
export type Invoice = {
  id: string;
  customer_id: string;
  amount: number;
  date: string;
  // In TypeScript, this is called a string union type.
  // It means that the "status" property can only be one of the two strings: 'pending' or 'paid'.
  status: 'pending' | 'paid';
};
```



### 运行开发服务器

```shell
pnpm i   安装项目包
pnpm dev 启动服务，默认3000端口上启动Next.js开发服务器
```

## css 样式

两种不同的样式设置方法：Tailwind 和 CSS 模块。

### Global styles


所有组件都可以通过`import '@/app/ui/global.css';`命令引入/app/ui/global.css来添加css规则。但是建议添加到顶层组件。例如`/app/layout.tsx`:

```tsx
import '@/app/ui/global.css';
 
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
```

### Taiwind

Taiwind是一个css框架，允许在TSX标记里写工具类快速开发。

通过添加className来格式化元素，例如把h1元素改成蓝色：

```html
<h1 className="text-blue-500">I'm blue!</h1>
```

### css模块

[CSS 模块](https://nextjs.net.cn/docs/basic-features/built-in-css-support)允许您通过自动创建唯一的类名来将 CSS 范围限定到组件，因此您也不必担心样式冲突。



### 使用clsx库切换类名

在某些情况下，您可能需要根据状态或其他条件有条件地设置元素的样式。

- 假设您想创建一个接受`status`的`InvoiceStatus`组件。状态可以是`'pending'`或`'paid'`。
- 如果是`'paid'`，则希望颜色为绿色。如果是`'pending'`，则希望颜色为灰色。

## 优化字体和图片

###  `next/font` 添加自定义字体。

`import { Inter } from 'next/font/google';   ` 导入Inter字体

`export const inter = Inter({ subsets: ['latin'] });` 加载其子集latin

``` tsx
import { inter } from '@/app/ui/fonts';
<body className={`${inter.className} antialiased`}>{children}</body>
```
字体应用到body元素



###  `next/image` 添加图片

`<Image>` 组件是 HTML `<img>` 标签的扩展，并具有自动图片优化功能，例如

- 在图片加载时自动防止布局偏移。
- 调整图片大小以避免向具有较小视口的设备发送大型图片。
- 默认情况下延迟加载图片（图片在进入视区时加载）。
- 在浏览器支持的情况下，使用现代格式（如 [WebP](https://mdn.org.cn/en-US/docs/Web/Media/Formats/Image_types#webp) 和 [AVIF](https://mdn.org.cn/en-US/docs/Web/Media/Formats/Image_types#avif_image)）提供图片。



```tsx
import Image from 'next/image';

          <Image
        src="/hero-desktop.png"
        width={1000}
        height={760}
        className="hidden md:block"
        alt="Screenshots of the dashboard project showing desktop version"
      />
```



## 创建布局和页面

### 嵌套路由

Next.js 使用文件系统路由，其中文件夹用于创建嵌套路由。每个文件夹代表一个路由段，对应到一个 URL 段。

您可以使用`layout.tsx`和`page.tsx`文件为每个路由创建单独的UI。

`page.tsx`是 Next.js 的一个特殊文件，它导出一个 React 组件，并且它是路由可访问的必要条件。在您的应用程序中，您已经有一个页面文件：`/app/page.tsx` - 这是与路由`/`关联的主页。

要创建嵌套路由，您可以在彼此内部嵌套文件夹，并在其中添加`page.tsx`文件。例如

**`/app/dashboard/page.tsx`与`/dashboard`路径关联。**



### 创建页面

就是在路由对应的文件夹下创建page.tsx文件， 文件内导出一个组件。

### 创建布局

在 Next.js 中，您可以使用特殊的`layout.tsx`文件来创建在多个**页面之间共享**的 UI

1. `import SideNav ` 导入到布局文件中，那么此文件中导入的任何组件都将成为布局的一部分
2. `<Layout />`组件接收一个`children`属性。此子元素可以是页面或另一个布局。
3. 布局文件所在文件夹内部的页面将自动嵌套在`<Layout/>`内部
   ![image-20250115143245439](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250115143245439.png)
4. 在 Next.js 中使用布局的一个好处是在导航时，只有页面组件会更新，而布局不会重新渲染。这称为[部分渲染](https://nextjs.net.cn/docs/app/building-your-application/routing/linking-and-navigating#4-partial-rendering)

app/dashboard/layout.tsx:

```tsx
import SideNav from '@/app/ui/dashboard/sidenav';
 
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-screen flex-col md:flex-row md:overflow-hidden">
      <div className="w-full flex-none md:w-64">
        <SideNav />
      </div>
      <div className="flex-grow p-6 md:overflow-y-auto md:p-12">{children}</div>
    </div>
  );
}
```

![image-20241230151522198](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20241230151522198.png)

### 根布局

添加到根布局的任何 UI 将在应用程序中的所有页面之间共享。你可以使用根布局来修改 `<html>` 和 `<body>` 标签，添加元数据。

例如我们优化字体时，就把Inter字体引入到/app/layout.tsx布局中了。

## 页面之间导航

### Link组件

**每次页面导航时都会出现完整的页面刷新！Link组件就是为了优化导航的，让页面之间导航无需看到完整的刷新，使其感觉像一个web应用程序。**

`<Link>` 允许您使用 JavaScript 进行[客户端导航(opens in a new tab)](https://nextjs.org/docs/app/building-your-application/routing/linking-and-navigating#how-routing-and-navigation-works)。

要使用 `<Link />` 组件，请打开 `/app/ui/dashboard/nav-links.tsx`，并从 [next/link(opens in a new tab)](https://nextjs.org/docs/app/api-reference/components/link) 导入 `Link` 组件。然后，将 `<a>` 标签替换为 `<Link>`：



#### 原理

为了提高导航体验，Next.js 会自动按路由段拆分您的应用程序。这与传统的 React [SPA(opens in a new tab)](https://developer.mozilla.org/en-US/docs/Glossary/SPA) 不同，传统 SPA 在初始加载时会加载应用程序的所有代码。

按路由拆分代码意味着页面变得隔离。如果某个页面抛出错误，应用程序的其余部分仍将正常工作。

此外，在生产环境中，每当 `<Link>` 组件出现在浏览器的视口中时，Next.js 会自动在后台预取链接路由的代码。当用户点击链接时，目标页面的代码将在后台已经加载，这就是使页面过渡几乎瞬间完成的原因！

### 显示活动链接

活动链接用蓝色突出显示，以向用户指示他们当前所在的页面，Next.js 提供了一个名为 `usePathname()` 的钩子，您可以使用它来检查路径并实现此模式。

由于 [usePathname()(opens in a new tab)](https://nextjs.org/docs/app/api-reference/functions/use-pathname) 是一个钩子，您需要将 `nav-links.tsx` 转换为客户端组件。



## 建立数据库

`pnpm i @vercel/postgres` 安装Vercel Postgres SDK

接下来需要看原英文版，代码有变化。





















```shell

$ cnpm install -g create-react-app
$ create-react-app my-app  --legacy-peer-deps
$ cd my-app/
$ npm start

```





# cli

## [create-next-app](https://nextjs.org/docs/app/api-reference/cli/create-next-app)

用途： 使用模板创建新Next应用

`npx create-next-app@latest [project-name] [options]`

| Options                                 | Description                                                  |
| --------------------------------------- | ------------------------------------------------------------ |
| `-h` or `--help`                        | Show all available options                                   |
| `-v` or `--version`                     | Output the version number                                    |
| `--no-*`                                | Negate default options. E.g. `--no-eslint`                   |
| `--ts` or `--typescript`                | Initialize as a TypeScript project (default)                 |
| `--js` or `--javascript`                | Initialize as a JavaScript project                           |
| `--tailwind`                            | Initialize with Tailwind CSS config (default)                |
| `--eslint`                              | Initialize with ESLint config                                |
| `--app`                                 | Initialize as an App Router project                          |
| `--src-dir`                             | Initialize inside a `src/` directory                         |
| `--turbopack`                           | Enable Turbopack by default for development                  |
| `--import-alias <alias-to-configure>`   | Specify import alias to use (default "@/*")                  |
| `--empty`                               | Initialize an empty project                                  |
| `--use-npm`                             | Explicitly tell the CLI to bootstrap the application using npm |
| `--use-pnpm`                            | Explicitly tell the CLI to bootstrap the application using pnpm |
| `--use-yarn`                            | Explicitly tell the CLI to bootstrap the application using Yarn |
| `--use-bun`                             | Explicitly tell the CLI to bootstrap the application using Bun |
| `-e` or `--example [name] [github-url]` | An example to bootstrap the app with                         |
| `--example-path <path-to-example>`      | Specify the path to the example separately                   |
| `--reset-preferences`                   | Explicitly tell the CLI to reset any stored preferences      |
| `--skip-install`                        | Explicitly tell the CLI to skip installing packages          |
| `--yes`                                 | Use previous preferences or defaults for all options         |



默认模板

`npx create-next-app@latest`

## next  CLI

作用：开发、构建、启动应用等

`npx next [command] [options]`



# 参考

[react官方教程](https://zh-hans.react.dev/learn/start-a-new-react-project)

参考[菜鸟教程](https://www.runoob.com/react/react-tutorial.html)

[Learn next.js](https://nextjs.org/learn), [对应中文教程](https://qufei1993.github.io/nextjs-learn-cn/chapter2)，[另一个](https://nextjs.net.cn/learn/dashboard-app/getting-started)

[Next.js中文入门](https://www.nextjs.cn/learn/basics/create-nextjs-app?utm_source=next-site&utm_medium=nav-cta&utm_campaign=next-website)

