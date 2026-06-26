[用好Obsidian模板打造高效公众号写作流](https://mp.weixin.qq.com/s/4K0JO1xd7uVqL6Bwqe9byA)
插件详细安装使用教程：
使用指南：
https://mp.weixin.qq.com/s/LYujo4ODEYLuq0OkzkkoCw

自定义CSS、数学公式、公众号卡片：
https://mp.weixin.qq.com/s/_HQhks2djrIwiMwWBFk6qQ

标注（Callout）：
https://mp.weixin.qq.com/s/6VoaxUu9Rh9OMgO6Zw8qlw

模板：
https://mp.weixin.qq.com/s/4K0JO1xd7uVqL6Bwqe9byA


注册码申请：
如果需要申请订阅服务内测，请先详细阅读插件使用指南https://mp.weixin.qq.com/s/LYujo4ODEYLuq0OkzkkoCw，然后根据“3、插件配置”，获取AppID、AppSecret，并且设置IP白名单后，填写问卷申请开通。

注册码申请问卷：
https://f1b9139iu4h.feishu.cn/share/base/form/shrcnak7ybJHCXr5nIq0rM7UOmH



Obsidian视频教程：
B站清单控沙牛大佬的教程[ https://www.bilibili.com/video/BV1H44y1n71k/ ]即可轻松上手，看完前三个视频就足以上手了，后面可以再慢慢看。



一键从 Obsidian 发布到公众号后台

https://pkmer.cn/Pkmer-Docs/pkmer%E8%AE%BA%E5%9D%9B/7341-%E6%88%91%E5%86%99%E4%BA%86%E4%B8%AA%E6%8F%92%E4%BB%B6%E4%B8%80%E9%94%AE%E4%BB%8E-obsidian-%E5%8F%91%E5%B8%83%E5%88%B0%E5%85%AC%E4%BC%97%E5%8F%B7%E5%90%8E%E5%8F%B0/





[批量上传图片](https://github.com/luhui-dev/NotePic-OSS-Obsidian) 不管是本地图片还是远程图片, ⚠️：一定要在编辑模式下上传，否则无法替换链接

# 目录多级编号

## ai 写的，不大好用

.vault/.obsidian/snippets/heading-numbering.css

设置 → 外观(Appearance) → CSS 片段(Snippets)

```css
/* ===========================================================
   Obsidian 多级标题编号（阅读视图 · 修复 wrapper 断裂版）
   格式：1 → 1.1 → 1.1.1 （无末尾点，无 0，不重复 1.1）
   =========================================================== */

/* ① 计数器的"根"——挂在阅读视图容器上 */
.markdown-preview-view,
.markdown-rendered {
  counter-reset: N1 N2 N3 N4;
}

/* ② 把 reset 挂到 wrapper 层，而不是 h1/h2 自身
     这样 reset 的作用域就能覆盖到后续兄弟 wrapper */
.markdown-preview-section:has(h1),
.markdown-rendered > :is(h1, h1 + *) {
  counter-reset: N2 N3 N4;
}
.markdown-preview-section:has(h2) {
  counter-reset: N3 N4;
}
.markdown-preview-section:has(h3) {
  counter-reset: N4;
}

/* ③ 编号显示 —— 全部 increment 写在 ::before 里，绝不分散 */
.markdown-preview-view h1::before,
.markdown-rendered h1::before {
  counter-increment: N1;
  content: counter(N1) " ";
}

.markdown-preview-view h2::before,
.markdown-rendered h2::before {
  counter-increment: N2;
  content: counter(N1) "." counter(N2) " ";
}

.markdown-preview-view h3::before,
.markdown-rendered h3::before {
  counter-increment: N3;
  content: counter(N1) "." counter(N2) "." counter(N3) " ";
}

.markdown-preview-view h4::before,
.markdown-rendered h4::before {
  counter-increment: N4;
  content: counter(N1) "." counter(N2) "." counter(N3) "." counter(N4) " ";
}
```

## 网友手搓的

https://github.com/platon-ivanov/obsidian-visually-numbered-headings

但是编号是灰色的，这是 ES构建出来的代码，不要直接看，要看逻辑可以看它的 github源代码。

章节可以折叠，很赞

有人基于此代码做了修改，得到[效果](https://pkmer.cn/Pkmer-Docs/10-obsidian/obsidian%E5%A4%96%E8%A7%82/css-%E7%89%87%E6%AE%B5/obsidian%E6%A0%B7%E5%BC%8F-%E6%A0%B8%E5%BF%83%E5%A4%A7%E7%BA%B2outline%E6%A0%87%E9%A2%98%E6%98%BE%E7%A4%BA%E8%87%AA%E5%8A%A8%E7%BC%96%E5%8F%B7/)，但不知道怎么改的，估计是修改的源代码，重新编译的。

# 图床

## 问题

1. upload failed, check dev console

使用不带 pro 的插件









# 插件

Claudian
Editing Toolbar
Image auto upload
Marp Slides
Visually Numbered Headings
