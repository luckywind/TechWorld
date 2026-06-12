# obsidian marp

## 内置主题

[内置主题](https://github.com/marp-team/marp-core/tree/main/themes#readme)，[Marpit doc](https://marpit.marp.app/directives?id=theme)

### 通用特性

1. 宽高比
`<!-- size: 4:3 -->`

2. 颜色反转
   `<!-- class: invert -->`

3. 切换主题 

   `<!-- theme: default -->`   其他可选主题

   uncover: 简单、现代化

   gaia: 默认左上对齐的主题，但也可以调整为uncover的居中方式

   ```css
   <!--
   theme: gaia
   class: lead
   -->
   ```

   > 使用 Marpit 的局部变量只对当前页调整
   >
   > <!-- _class: lead -->

4. 自定义颜色
   ```html
   <style>
     :root {
       --color-fg-default: #eff;
       --color-canvas-default: #246;
       /* ... */
     }
   </style>
   ```

   

### 特性

class 可以指定多个值，用空格分开

### Gaia 主题

自定义配色

```css
<style>
  :root {
    --color-background: #fff;
    --color-foreground: #333;
    --color-highlight: #f96;
    --color-dimmed: #888;
  }
</style>
```

### Uncover主题

自定义配色

```css
<style>
  :root {
    --color-background: #ddd;
    --color-background-code: #ccc;
    --color-background-paginate: rgba(128, 128, 128, 0.05);
    --color-foreground: #345;
    --color-highlight: #99c;
    --color-highlight-hover: #aaf;
    --color-highlight-heading: #99c;
    --color-header: #bbb;
    --color-header-shadow: transparent;
  }
</style>
```

## 图片

### 背景图

1. 铺满

```markdown
![bg cover](image.png)
```

2. 只占一侧

```markdown
# 右边的标题
右边的正文内容可以正常写
![bg left:40%](image.png)
```

3. 透明度
   ```markdown
   ![bg cover opacity:.2](cover.jpg)
   ```

4. 多张平铺

   ```markdown
   ![bg](a.png)![bg](b.png)![bg](c.png)    横向排列
   ![bg vertical](a.png)![bg](b.png)   竖向排列
   ```

| **写法**                 | **效果**                           |
| :----------------------- | :--------------------------------- |
| `![bg cover](x.png)`     | 铺满全屏，**超出部分裁剪**（默认） |
| `![bg contain](x.png)`   | 整图可见，**留白**                 |
| `![bg fit](x.png)`       | 同 contain，兼容 Deckset 语法      |
| `![bg auto](x.png)`      | 原图尺寸，不缩放                   |
| `![bg 150%](x.png)`      | 按百分比缩放                       |
| `![bg left](x.png)`      | 占左半边                           |
| `![bg left:33%](x.png)`  | 自定义占比                         |
| `![bg right:40%](x.png)` | 右侧占比                           |

以上写法如果不加 contain 可能会裁剪图片，如果不想图片被裁剪，后面加上 contain:
![bg right contain]

#### 纯色背景

背景颜色的基本句式为 `![bg](颜色参数)` ，文字颜色的基本句式为 `![](颜色参数)`

### 调整大小

```markdown
![width:200px](image.jpg) <!-- Setting width to 200px -->
![height:30cm](image.jpg) <!-- Setting height to 300px -->
![width:200px height:30cm](image.jpg) <!-- Setting both lengths -->
也可以缩写为 w 和 h
![w:32 h:32](image.jpg) <!-- Setting size to 32x32 px -->
```



# 课件

## 模板 1

```yaml
---
# 整体设置
# 是否幻灯片
marp: true
theme: gaia
paginate: true
# 页脚
footer: '程老师'
backgroundColor: white
### ------------------- 幻灯片尺寸，宽版：4:3
#size: 16:9
headingDivider: 2 
style: |
  section {
    background-color: #ffffff;
  }
  h1 {
    color: black !important;
  }
---
<!--_class: lead-->
<style scoped>
section {
  background-color: #ffffff;
}
h1 {
  color: #e74c3c !important;
  font-size: 128px !important;   /* 首页设置 */
  text-align: center;
}
</style>
```



# 附录

## Marpit markdown

### 使用水平尺制作幻灯片

```markdown
# Slide 1

foo

---

# Slide 2

bar

```

根据 [CommonMark](https://spec.commonmark.org/0.29/#example-28) 规范，破折号标尺前可能需要空行。如果您不想添加空行，可以使用下划线标尺 `___` 、星号标尺 `***` 和包含空格的标尺 `- - -`



### [Directives 指令](https://marpit.marp.app/directives)

Marpit Markdown 具有名为 **“指令”** 的扩展语法，用于支持编写精美的幻灯片。它可以控制幻灯片的模板主题、页码、页眉、页脚、样式等等。编写的指令将被解析为 [YAML](http://yaml.org/) 。

Marp 提供两种使用方法：

#### HTML comment

这种需要在 `theme` 等指令前后添加`<!-- -->`。

```markdown
<!--
theme: default
paginate: true
-->
```

#### [Front-matter ](https://marpit.marp.app/directives?id=front-matter)

它必须是 Markdown 内容的第一部分，并且位于短横线之间。实际幻灯片内容将从前言部分的结束标尺之后开始。

```yaml
---
theme: default
paginate: true
---
```

#### 指令类型



![image-20260612085714228](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20260612085714228.png)

##### 全局指令

| Name             | Description                                                  |
| ---------------- | ------------------------------------------------------------ |
| `headingDivider` | 标题分割符，自动按照标题进行分页，无需`---`,  取值是标题最大层级 |
| `lang`           | Set the value of [`lang` attribute](https://developer.mozilla.org/docs/Web/HTML/Global_attributes/lang) for each slide |
| `style`          | Specify CSS for tweaking theme.                              |
| `theme`          | Specify theme of the slide deck.                             |

按照标题分页：`<!-- headingDivider: 2 -->` 2 级标题分页

在全局指令前面添加前缀 `$`，就可以实现对整个幻灯片的设定。

##### [Local directives](https://marpit.marp.app/directives?id=local-directives-1)

| Name                 | Description                                                  |
| -------------------- | ------------------------------------------------------------ |
| `paginate`           | 展示页码 Show page number on the slide if you set `true`.如果我们不想在标题页面出现页码，只需将指令 `paginate` 移到第二页即可。 |
| `header`             | Specify the content of slide header.                         |
| `footer`             | Specify the content of slide footer.                         |
| `class`              | Specify HTML class of slide’s `<section>` element.           |
| `backgroundColor`    | Setting `background-color` style of slide.                   |
| `backgroundImage`    | Setting `background-image` style of slide.                   |
| `backgroundPosition` | Setting `background-position` style of slide.                |
| `backgroundRepeat`   | Setting `background-repeat` style of slide.                  |
| `backgroundSize`     | Setting `background-size` style of slide.                    |
| `color`              | Setting `color` style of slide.                              |

# 参考

[**AwesomeMarp for University**](https://panwangyuang.com/post/awesomemarp-for-university/)