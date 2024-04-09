[参考](https://www.jianshu.com/p/e0865a2193cc)

# 使用docker搭建spark(2.4.1)集群

```shell
git clone https://github.com/gettyimages/docker-spark.git
cd docker-spark
compose up
```

在浏览器中输入localhost:8080可以发现master和worker都已经启动了

此时命令行挂住了，ctrl+c结束后，可以使用docker start 容器id启动相应容器

注意查看docker-compose.yml 文件最后配置，将本地文件`./data`挂载到容器中的`/tmp/data`下

```shell
    ports:
      - 8081:8081
    volumes:
      - ./conf/worker:/conf
      - ./data:/tmp/data
```

