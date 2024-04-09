# 添加ng-zorro

```shell
$ ng new today-ng
$ cd today-ng
$ ng add ng-zorro-antd
```

html模版中可以使用#定义变量

```html
    <input nz-input
           placeholder="请输入你喜欢的用户名"
           #usernameInput     //定义变量usernameInput
           [(ngModel)]="username">
    <button nz-button
            [nzType]="'primary'"
            (click)="completeSetup()"
            [disabled]="!usernameInput.value">  //引用变量usernameInput
      开始
    </button>
```

另外，button和input两个组件的使用需要搜索一下

# 总结

- 把 ng-zorro-antd 引入到项目中
- button input 两个组件的使用
- 利用 service 解耦