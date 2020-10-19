查看ssh服务状态

sudo systemsetup -getremotelogin

如果已经开启的话，指令会显示 “Remote Login: On” ，反之就会显示“Remote Login: Off”。

开启ssh服务：sudo systemsetup -setremotelogin on

关闭ssh服务：sudo systemsetup -setremotelogin off

# 远程拷贝

```shell
scp chengxingfu@10.224.208.48:/Users/chengxingfu/code/mi/codelab/spark/scala/SparkAppDemo/target/spark-scala-sparkappdemo-1.0-SNAPSHOT-jar-with-dependencies.jar .
```

