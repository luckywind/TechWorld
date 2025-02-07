编写`Makefile`就是编写一系列规则，用来告诉`make`如何执行这些规则，最终生成我们期望的目标文件。

1. **规则**：每一条规则指出一个目标文件（Target），若干依赖文件（prerequisites），以及生成目标文件的命令。

```shell
# 目标文件: 依赖文件1 依赖文件2
m.txt: a.txt b.txt
	cat a.txt b.txt > m.txt  #命令必须以Tab开头
	pwd   #执行多个命令的写法
```
**注意**：make针对每个命令都会创建一个独立的shell环境，类似cd ..这样的命令并不会影响当前目录，解决办法是用;分割写到一行




2. **规则执行顺序**：make 默认执行第一条规则，但当依赖文文件不满足时，会自动先执行依赖规则

​	例如

```shell
x.txt: m.txt c.txt
	cat m.txt c.txt > x.txt

m.txt: a.txt b.txt
	cat a.txt b.txt > m.txt
```

会先执行规则m.txt,再执行x.txt

3. **伪目标**：不要生成文件，单纯为了执行命令

   > 防止文件存在时，make偷懒不执行命令

   ```shell
   .PHONY: clean  #这里把clean标记为伪目标，二非文件
   clean:
   	rm -f m.txt
   	rm -f x.txt
   ```

   clean 、install等已被约定为伪目标了
   
3. 不要打印命令
默认情况下，`make`会打印出它执行的每一条命令。如果我们不想打印某一条命令，可以在命令前加上`@`，表示不打印命令（但是仍然会执行）

5. 使用变量
   注意，定义在规则之外
   
   - 自定义变量：变量 定义用`变量名 = 值`或者`变量名 := 值`，通常变量名全大写。引用变量用`$(变量名)`，非常简单。
   - 内置变量： `$@` 表示目标， `$^`表示所有依赖文件









[参考](https://liaoxuefeng.com/books/makefile/introduction/index.html)