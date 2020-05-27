# 模板语法

## *ngFor

1. `*ngFor` 是一个 "结构型指令"。结构型指令会通过添加、删除和操纵它们的宿主元素等方式塑造或重塑 DOM 的结构。任何带有星号 `*` 的指令都是结构型指令。
2. 有了 `*ngFor`，这个 `` 就会被列表中的每个商品都重复渲染一次。

```html
<div *ngFor="let product of products">
</div>
```

## 插值语法{{}}

插值会把属性的值作为文本渲染出来

```html
<h3>
      {{ product.name }}
  </h3>
```

## 属性绑定语法[]

属性绑定语法 `[]` 允许你在模板表达式中使用属性值。

​		 `[]` 把该链接的 `title` 设置成该商品的名字

```html
<h3>
    <a [title]="product.name + ' details'">
      {{ product.name }}
    </a>
  </h3>
```

## *ngIf

类似if条件

```html
<p *ngIf="product.description">
    Description: {{ product.description }}
  </p>
```

## 事件绑定()

事件绑定是通过把事件名称包裹在圆括号 `( )` 中完成的,例如，把click事件绑定到share()方法上

```html
 <button (click)="share()">
    Share
  </button>
```



# 组件component

*组件*在用户界面（也就是 UI）中定义了一些责任区，让你能重用这些 UI 功能集

组件包含三部分：

- **一个组件类**，它用来处理数据和功能。上一节，我们在组件类中定义了商品数据和 `share()` 方法，它们分别用来处理数据和功能。
- **一个 HTML 模板**，它决定了 UI。在上一节中，商品列表的 HTML 模板用来显示每个商品的名称、描述和 “Share” 按钮。
- **组件专属的样式**定义了外观和感觉。商品列表中还没有定义任何样式，那属于组件 CSS 负责。

Angular 应用程序由一棵组件树组成，每个 Angular 组件都有一个明确的用途和责任。

