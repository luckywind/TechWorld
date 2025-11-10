[参考](https://mp.weixin.qq.com/s/dkzslZrJBTFSMs8y_R0b1g)

[在线生成](https://latex.codecogs.com/eqneditor/editor.php)



$$V_{初始}$$， $$V_{\mbox{初始}}$$

${1+x}^{(3+y)}$

$\frac{x+y}{y+z}$,  $\displaystyle \frac{x+y}{y+z}$



规律： 

1. {}内的会作为一个整体
2. **\pm**  plus与minus   $x \pm y =z$
3. \times    $x \times y=z$
4. \cdot    $x \cdot y=z$，  \cdots    $\cdots$
5. \times 乘法
6. \div   除法
7. \frac{}{}  分数
8. \underline、\overline
9. \sqrt 开方  \sqrt[3]开3次方 $\sqrt 5$, $\sqrt[3] {5+3}$, $\sqrt{3}$
10. \geq >= 、 \leq <=、\neq  $x \neq y$,  等价`\iff`:$\iff$
11. **集合**： in 、notin、subset、cup、cap

12. 箭头：`\Rightarrow`$\Rightarrow$, 大写R 代表双箭头，小写代表单箭头，其他方向的用up/down/left 等

关于公式块：

1. 快速插入公式块： `Command+alt+B`，    $$回车
2. 公式内换行： `\\`

关于行内公式：`$a<b<c$`

3. 多行公式左对齐：用`\begin{aligned}`包裹，且对齐处写`&`
$$
\begin{aligned}
S &= 1 + 2 + 3 + \cdots + n \\
  &= \frac{n(n+1)}{2}
\end{aligned}
$$

## **括号**

使用花括号有两种 [环境](https://katex.org/docs/supported.html?spm=a2c6h.12873639.article-detail.4.19a6874a5uWcox#environments) 可以选择：

`cases` 左花括号，内容靠左边对齐;

`rcases` 右花括号，内容靠右边对齐;

```markdown
$$
	我是夜阑的狗
	\begin{cases}
		角色保底+1 \\
		武器定轨+1\\
		七七命座+1\\
		斩尽牛杂+999
	\end{cases}
$$
```

$$
我是夜阑的狗
	\begin{cases}
		角色保底+1 \\
		武器定轨+1\\
		七七命座+1\\
		斩尽牛杂+999
	\end{cases}
$$

```markdown
$$
	\begin{rcases}
		角色保底+1 \\
		武器定轨+1\\
		七七命座+1\\
		斩尽牛杂+999
	\end{rcases}
	我是夜阑的狗
$$
```

$$
\begin{rcases}
		角色保底+1 \\
		武器定轨+1\\
		七七命座+1\\
		斩尽牛杂+999
	\end{rcases}
	我是夜阑的狗
$$
