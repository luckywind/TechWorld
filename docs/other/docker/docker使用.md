[参考](https://www.freecodecamp.org/chinese/news/the-docker-handbook/)

[Docker 从入门到实践](https://yeasy.gitbook.io/docker_practice/install/ubuntu)



# 简介

容器： 装进程的壳子

docker: 壳子的封装

# 架构

Docker 包括三个基本概念:

- **镜像（Image）**：Docker 镜像（Image），就相当于是一个 root 文件系统。比如官方镜像 ubuntu:16.04 就包含了完整的一套 Ubuntu16.04 最小系统的 root 文件系统。采用分层存储，前一层是后一层的基础。

- **容器（Container）**：镜像（Image）和容器（Container）的关系，就像是面向对象程序设计中的类和实例一样，镜像是静态的定义，容器是镜像运行时的实体。容器可以被创建、启动、停止、删除、暂停等。容器的实质是进程，但与直接在宿主执行的进程不同，容器进程运行于属于自己的独立的 [命名空间](https://en.wikipedia.org/wiki/Linux_namespaces)。因此容器可以拥有自己的 `root` 文件系统、自己的网络配置、自己的进程空间，甚至自己的用户 ID 空间。容器内的进程是运行在一个隔离的环境里，使用起来，就好像是在一个独立于宿主的系统下操作一样。这种特性使得容器封装的应用比直接在宿主运行更加安全。也因为这种隔离的特性，很多人初学 Docker 时常常会混淆容器和虚拟机。容器也是分层存储，每一个容器运行时，是以镜像为基础层，在其上创建一个当前容器的存储层，我们可以称这个为容器运行时读写而准备的存储层为 **容器存储层**。容器存储层的生存周期和容器一样，容器消亡时，容器存储层也随之消亡。因此，任何保存于容器存储层的信息都会随容器删除而丢失。按照 Docker 最佳实践的要求，容器不应该向其存储层内写入任何数据，容器存储层要保持无状态化。所有的文件写入操作，都应该使用 [数据卷（Volume）]()、或者 [绑定宿主目录]()，在这些位置的读写会跳过容器存储层，直接对宿主（或网络存储）发生读写，其性能和稳定性更高。

  数据卷的生存周期独立于容器，容器消亡，数据卷不会消亡。因此，使用数据卷后，容器删除或者重新运行之后，数据却不会丢失。

- **仓库（Repository）**：仓库可看成一个代码控制中心，用来保存镜像。镜像构建完成后，可以很容易的在当前宿主机上运行，但是，如果需要在其它服务器上使用这个镜像，我们就需要一个集中的存储、分发镜像的服务，[Docker Registry]() 就是这样的服务。

  一个 **Docker Registry** 中可以包含多个 **仓库**（`Repository`）；每个仓库可以包含多个 **标签**（`Tag`）；每个标签对应一个镜像。
  通常，一个仓库会包含同一个软件不同版本的镜像，而标签就常用于对应该软件的各个版本。我们可以通过 `<仓库名>:<标签>` 的格式来指定具体是这个软件哪个版本的镜像。如果不给出标签，将以 `latest` 作为默认标签。

Docker 使用客户端-服务器 (C/S) 架构模式，使用远程API来管理和创建Docker容器。

Docker 容器通过 Docker 镜像来创建。

容器与镜像的关系类似于面向对象编程中的对象与类。

| 概念                   | 说明                                                         |
| :--------------------- | :----------------------------------------------------------- |
| Docker 镜像(Images)    | Docker 镜像是用于**创建 Docker 容器的模板**，比如 Ubuntu 系统。 |
| Docker 容器(Container) | 容器是独立运行的一个或一组应用，**是镜像运行时的实体**。     |
| Docker 客户端(Client)  | Docker 客户端通过命令行或者其他工具使用 Docker SDK (https://docs.docker.com/develop/sdk/) 与 Docker 的守护进程通信。 |
| Docker 主机(Host)      | 一个物理或者虚拟的机器用于执行 Docker 守护进程和容器。       |
| Docker Registry        | **Docker 仓库用来保存镜像，可以理解为代码控制中的代码仓库**。Docker Hub([https://hub.docker.com](https://hub.docker.com/)) 提供了庞大的镜像集合供使用。**一个 Docker Registry 中可以包含多个仓库（Repository）；每个仓库可以包含多个标签（Tag）；每个标签对应一个镜像。**通常，一个仓库会包含同一个软件不同版本的镜像，而标签就常用于对应该软件的各个版本。我们可以通过 **<仓库名>:<标签>** 的格式来指定具体是这个软件哪个版本的镜像。如果不给出标签，将以 **latest** 作为默认标签。 |
| Docker Machine         | Docker Machine是一个简化Docker安装的命令行工具，通过一个简单的命令行即可在相应的平台上安装Docker，比如VirtualBox、 Digital Ocean、Microsoft Azure。 |

# 安装docker

mac安装

```shell
brew cask install docker
docker --version
```

## ubuntu

```shell
# $ curl -fsSL test.docker.com -o get-docker.sh
$ curl -fsSL get.docker.com -o get-docker.sh
$ sudo sh get-docker.sh --mirror Aliyun
# $ sudo sh get-docker.sh --mirror AzureChinaCloud
```

测试

docker run --rm hello-world

## 启动 Docker

```
$ sudo systemctl enable docker
$ sudo systemctl start docker
```



## 镜像加速

鉴于国内网络问题，后续拉取 Docker 镜像十分缓慢，我们可以需要配置加速器来解决，我使用的是网易的镜像地址：**http://hub-mirror.c.163.com**。

在任务栏点击 Docker for mac 应用图标 -> Perferences... -> Daemon -> Registry mirrors。在列表中填写加速器地址即可。修改完成之后，点击 Apply & Restart 按钮，Docker 就会重启并应用配置的镜像地址了。

# 使用镜像

Docker 运行容器前需要本地存在对应的镜像，如果本地不存在该镜像，Docker 会从镜像仓库下载该镜像。

## 增删查命令汇总

- 查找镜像
  docker search xxx

- 获取镜像

$ docker pull [选项] [Docker Registry 地址[:端口号]/]仓库名[:标签]

> 端口号 和 标签可省略

命令docker pull ubuntu:18.04 

1. 省略了Docker镜像仓库地址，默认地址是docker.io

2. **仓库名有两部分，即<用户名>/<软件名>，  用户名默认为library**

所以完整的镜像名称是docker.io/library/ubuntu:18.04

[docker.io]/[library/ubuntu]:[18.04]

- 运行容器

docker run -it --rm ubuntu:18.04 bash

> -i 交互模式，让容器的标准输入保持打开
>
> -t 终端程序
>
> -rm 容器退出后自动删除

- 列出已经下载的镜像

docker image ls

​	  1. 列出指定仓库下的镜像docker image ls ubuntu

   2. 列出中间层镜像 docker image ls -a

   3. 过滤 docker image ls --filter/-f

   4. 特定格式显示--format

      ```shell
      $ docker image ls -q
      5f515359c7f8
      05a60462f8ba
      fe9198c04d62
      00285df0df87
      329ed837d508
      329ed837d508
      $ docker image ls --format "{{.ID}}: {{.Repository}}"
       docker image ls --format "table {{.ID}}\t{{.Repository}}\t{{.Tag}}"
      ```

      

- 镜像体积
  docker system df 

- 删除镜像
  docker image rm [选项] <镜像1> [<镜像2> ...]

  > 镜像只有在没有其他镜像、容器、Tag依赖时，才会真正触发Delete,否则rm只会触发Untagged

  成批删除镜像，例如要删除所有仓库名为redis的镜像
  docker image rm $(docker image ls -q redis)

- 打标签
  docker tag 镜像ID 用户名/仓库名:tag

- 虚拟镜像
  仓库名和标签都是<none>. 

  这是出现了新的同名镜像，这个镜像已经没有价值了，可以随意删除掉，可以用这个命令删除：
  docker  image prune

## [理解 Docker 镜像和容器的存储路径](https://www.freecodecamp.org/chinese/news/where-are-docker-images-stored-docker-container-paths-explained/)

```shell
$ docker info

...
 Storage Driver: overlay2  # 存储驱动， 保存docker镜像
 Docker Root Dir: /var/lib/docker # 根目录

```

`/var/lib/docker` 目录中保存着各种信息，例如：容器数据、卷、构建文件、网络文件和集群数据。

```shell
$ ls -la /var/lib/docker

total 152
drwx--x--x   15 root     root          4096 Feb  1 13:09 .
drwxr-xr-x   13 root     root          4096 Aug  1  2019 ..
drwx------    2 root     root          4096 May 20  2019 builder
drwx------    4 root     root          4096 May 20  2019 buildkit
drwx------    3 root     root          4096 May 20  2019 containerd
drwx------    2 root     root         12288 Feb  3 19:35 containers
drwx------    3 root     root          4096 May 20  2019 image
drwxr-x---    3 root     root          4096 May 20  2019 network
drwx------    6 root     root         77824 Feb  3 19:37 overlay2
drwx------    4 root     root          4096 May 20  2019 plugins
drwx------    2 root     root          4096 Feb  1 13:09 runtimes
drwx------    2 root     root          4096 May 20  2019 swarm
drwx------    2 root     root          4096 Feb  3 19:37 tmp
drwx------    2 root     root          4096 May 20  2019 trust
drwx------   15 root     root         12288 Feb  3 19:35 volumes
```







查看镜像

```shell
$ docker image pull nginx
$ docker image inspect nginx

[
    {
        "Id": "sha256:207...6e1",
        "RepoTags": [
            "nginx:latest"
        ],
        "RepoDigests": [
            "nginx@sha256:ad5...c6f"
        ],
        "Parent": "",
 ...
        "Architecture": "amd64",
        "Os": "linux",
        "Size": 126698063,
        "VirtualSize": 126698063,
        "GraphDriver": {
            "Data": {
                "LowerDir": "/var/lib/docker/overlay2/585...9eb/diff:
                             /var/lib/docker/overlay2/585...9eb/diff",
                "MergedDir": "/var/lib/docker/overlay2/585...9eb/merged",
                "UpperDir": "/var/lib/docker/overlay2/585...9eb/diff",
                "WorkDir": "/var/lib/docker/overlay2/585...9eb/work"
            },
...
```

**LowerDir** 包含镜像的只读层，表示变更的读写层包含在 **UpperDir** 中

## Docker卷

可以利用卷来持久化容器内的数据，容器和宿主机之间、容器和容器之间也可以通过共享卷来共享数据。使用 **-v** 选项可以让容器以挂载卷的方式启动：

```shell
$ docker run --name nginx_container -v /var/log nginx

$ docker inspect nginx_container
"Mounts": [
            {
                "Type": "volume",
                "Name": "1e4...d9c",
                "Source": "/var/lib/docker/volumes/1e4...d9c/_data",
                "Destination": "/var/log",
                "Driver": "local",
                "Mode": "",
                "RW": true,
                "Propagation": ""
            }
        ],
```



# [仓库管理](https://www.runoob.com/docker/docker-repository.html)

仓库登录需要输入用户名和密码，登录成功后，我们就可以从 docker hub 上拉取自己账号下的全部镜像。

docker login/logout

## 推送镜像



# 操作容器

## 运行容器

- 新建并运行
- 启动已终止的容器

可以利用 `docker container start` 命令，直接将一个已经终止（`exited`）的容器启动运行。

- 守护态运行  -d参数
  可通过 docker container logs 查看容器的输出信息

## 终止

容器中指定的应用执行结束时，容器会自动终止，但也可使用docker container stop 来提前终止。

docker container ls -a  可以看到终止状态的容器。

docker container start / restart 这些命令

## 进入容器

### exec命令

两个重要的参数

-i  交互，但没有命令提示符

-it  带命令提示符的交互

```shell
$ docker exec -it 69d1 bash
```



## 删除容器

可以使用 `docker container rm` 来删除一个处于终止状态的容器。-f 可删除运行中的容器

docker container prune  清理所有终止状态的容器

## 查看容器日志

```shell
$ docker logs [OPTIONS] CONTAINER
  Options:
        --details        显示更多的信息
    -f, --follow         跟踪实时日志
        --since string   显示自某个timestamp之后的日志，或相对时间，如42m（即42分钟）
        --tail string    从日志末尾显示多少行日志， 默认是all
    -t, --timestamps     显示时间戳
        --until string   显示自某个timestamp之前的日志，或相对时间，如42m（即42分钟）
```



# 数据管理



# 其他命令

docker ps



# Dockerfile

## [定制镜像](https://yeasy.gitbook.io/docker_practice/image/build)

### FROM 和RUN 命令

```shell
FROM    centos:6.7
MAINTAINER      Fisher "fisher@sudops.com"

RUN     /bin/echo 'root:123456' |chpasswd
RUN     useradd runoob
RUN     /bin/echo 'runoob:123456' |chpasswd
RUN     /bin/echo -e "LANG=\"en_US.UTF-8\"" >/etc/default/local
EXPOSE  22
EXPOSE  80
CMD     /usr/sbin/sshd -D
```



每一个指令都会在镜像上创建一个新的层，每一个指令的前缀都必须是大写的。

第一条FROM，指定使用哪个镜像源

RUN 指令告诉docker 在镜像内执行命令，安装了什么。。。

然而，每一个命令都去建一层完全没有意义，最好的写法是这样：

```shell
FROM debian:stretch

RUN set -x; buildDeps='gcc libc6-dev make wget' \
    && apt-get update \
    && apt-get install -y $buildDeps \
    && wget -O redis.tar.gz "http://download.redis.io/releases/redis-5.0.3.tar.gz" \
    && mkdir -p /usr/src/redis \
    && tar -xzf redis.tar.gz -C /usr/src/redis --strip-components=1 \
    && make -C /usr/src/redis \
    && make -C /usr/src/redis install \
    && rm -rf /var/lib/apt/lists/* \
    && rm redis.tar.gz \
    && rm -r /usr/src/redis \
    && apt-get purge -y --auto-remove $buildDeps
```

## 构建镜像

我们在使用 Dockerfile 文件所在目录执行命令，通过 docker build 命令来构建一个镜像。

docker build -t nginx:v3 .
-t 指定最终镜像的名称

.  指定上下文目录，该目录会被打包交给Docker引擎，如果要排除某些文件，可以用 `.gitignore` 一样的语法写一个 `.dockerignore`。

-f 指定Dockerfile， 但这里没有指定，默认会到上下文目录中寻找

### 其他用法

docker build -t hello-world https://github.com/docker-library/hello-world.git#master:amd64/hello-world

docker build http://server/context.tar.gz

docker build - < Dockerfile

docker build - < context.tar.gz

## 其他命令

- WORKDIR    类似cd，注意，层之间的cd会时效，WORKDIR不会

- COPY

- EXPOSE <端口1> [<端口2>...]    声明容器运行时提供服务的端口，这只是一个声明，在容器运行时并不会因为这个声明应用就会开启这个端口的服务

- CMD ["可执行文件", "参数1", "参数2"...]

`CMD` 指令就是用于指定默认的容器主进程的启动命令的。

在运行时可以指定新的命令来替代镜像设置中的这个默认命令，比如，`ubuntu` 镜像默认的 `CMD` 是 `/bin/bash`，如果我们直接 `docker run -it ubuntu` 的话，会直接进入 `bash`。我们也可以在运行时指定运行别的命令，如 `docker run -it ubuntu cat /etc/os-release`。这就是用 `cat /etc/os-release` 命令替换了默认的 `/bin/bash` 命令了，输出了系统版本信息。

# Demo

参考例子/home/chengxf/code/docker-img/mynginx

```shell
# 拉取镜像
$ docker pull luckywindwhu/helloworld:v1
v1: Pulling from luckywindwhu/helloworld
123275d6e508: Pull complete 
dd1cd6637523: Pull complete 
0c4e6d630f2c: Pull complete 
13e9cd8f0ea1: Pull complete 
ad62cd8fe93e: Pull complete 
c9f8f145f5d0: Pull complete 
f6e40a7a727e: Pull complete 
Digest: sha256:8fc6c283c297752b6d3a509c0eea3e2bc5778a2f270f61372efcc499a893057c
Status: Downloaded newer image for luckywindwhu/helloworld:v1
docker.io/luckywindwhu/helloworld:v1

$ docker image list 
REPOSITORY                TAG       IMAGE ID       CREATED          SIZE
mynginx                   v1        12326145d387   14 minutes ago   187MB
luckywindwhu/helloworld   v1        90dbe22af504   3 weeks ago      158MB
nginx                     latest    92b11f67642b   6 weeks ago      187MB
hello-world               latest    d2c94e258dcb   11 months ago    13.3kB

# 推送到仓库时，先打个tag
$ docker tag mynginx:v1 luckywindwhu/mynginx:v1
$ docker image list 
REPOSITORY                TAG       IMAGE ID       CREATED          SIZE
mynginx                   v1        12326145d387   16 minutes ago   187MB
luckywindwhu/mynginx      v1        12326145d387   16 minutes ago   187MB
luckywindwhu/helloworld   v1        90dbe22af504   3 weeks ago      158MB
nginx                     latest    92b11f67642b   6 weeks ago      187MB
hello-world               latest    d2c94e258dcb   11 months ago    13.3kB
$ docker push luckywindwhu/mynginx:v1
The push refers to repository [docker.io/luckywindwhu/mynginx]
fecc834da549: Pushed 
```

