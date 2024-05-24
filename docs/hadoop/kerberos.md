[入坑指南](https://www.jianshu.com/p/fc2d2dbd510b)

几个概念

1. principal
    认证的主体，简单来说就是"用户名"
2. realm
    realm有点像编程语言中的namespace。在编程语言中，变量名只有在某个"namespace"里才有意义。同样的，一个principal只有在某个realm下才有意义。
    所以realm可以看成是principal的一个"容器"或者"空间"。相对应的，principal的命名规则是"what_name_you_like@realm"。
    在kerberos, 大家都约定成俗用大写来命名realm, 比如"EXAMPLE.COM"
3. password
    某个用户的密码，对应于kerberos中的master_key。password可以存在一个keytab文件中。所以kerberos中需要使用密码的场景都可以用一个keytab作为输入。
4. credential
    credential是“证明某个人确定是他自己/某一种行为的确可以发生”的凭据。在不同的使用场景下， credential的具体含义也略有不同：
   - 对于某个principal个体而言，他的credential就是他的password。
   - 在kerberos认证的环节中，credential就意味着各种各样的ticket。

