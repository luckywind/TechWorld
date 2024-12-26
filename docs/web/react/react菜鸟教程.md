React 是一个用于构建用户界面的 JAVASCRIPT 库。

JSX 是 JavaScript 语法的扩展。React 开发不一定使用 JSX ，但我们建议使用它。

[ES6](https://www.runoob.com/w3cnote/es6-tutorial.html)全称 ECMAScript 6.0 ，是 JavaScript 的下一个版本标准

# next.js

## [getting started](https://nextjs.org/learn/dashboard-app/getting-started)

```shell
$npm install -g pnpm
$cd my-app
$npx create-next-app@latest nextjs-dashboard --example "https://github.com/vercel/next-learn/tree/main/dashboard/starter-example" --use-pnpm
Success! Created nextjs-dashboard at /Users/chengxingfu/code/yusur/hados/my-app/nextjs-dashboard
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



## css styling

How to add a global CSS file to your application.

Two different ways of styling: Tailwind and CSS modules.

How to conditionally add class names with the `clsx` utility package.

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

























```shell

$ cnpm install -g create-react-app
$ create-react-app my-app  --legacy-peer-deps
$ cd my-app/
$ npm start

```







[react官方教程](https://zh-hans.react.dev/learn/start-a-new-react-project)

参考[菜鸟教程](https://www.runoob.com/react/react-tutorial.html)

[Learn next.js](https://nextjs.org/learn), [对应中文教程](https://qufei1993.github.io/nextjs-learn-cn/chapter2)

[Next.js中文入门](https://www.nextjs.cn/learn/basics/create-nextjs-app?utm_source=next-site&utm_medium=nav-cta&utm_campaign=next-website)