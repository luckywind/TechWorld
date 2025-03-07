# 状态异步更新问题

下面这个下一页按钮点击第一次时，page并没有更新，第二次才更新

```jsx
          <Button
            variant="outline"
            size="sm"
            onClick={() => {
              setPage(page + 1);
              getProductsPipelineHistory();
            }}
            disabled={nextDisable}
          >
            下一页
          </Button>
```

根本原因是状态更新的异步性导致闭包中捕获了旧值。通过**直接传递新值**或**分离副作用到 `useEffect`**，可以确保逻辑与状态同步。

```jsx
// 监听 page 变化时自动触发请求
useEffect(() => {
  getProductsPipelineHistory();
}, [page]); // 当 page 变化时执行

// 按钮只需更新状态
<Button
  onClick={() => setPage(prev => prev + 1)}
  // ...
>
  下一页
</Button>
```

# import问题

## is not a function

```js
import { verifyBkToken } from './app/page';
```

这个导入没有写文件后缀，而代码中存在同名但不同后缀的文件，导致调用verifyBkToken时提示TypeError: (0 , v.verifyBkToken) is not a function

解决： 

导入语句写明确后缀page.jsx

