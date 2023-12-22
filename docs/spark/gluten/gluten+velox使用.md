# [编译包](https://confluence.yusur.tech/pages/viewpage.action?pageId=123868564#Gluten+Velox%E6%B5%8B%E8%AF%95%E7%8E%AF%E5%A2%83%E6%90%AD%E5%BB%BA-%E4%BA%94%E3%80%81gluten+velox%E7%8E%AF%E5%A2%83%E6%90%AD%E5%BB%BA%EF%BC%88Centos7%EF%BC%89)

## 工具依赖

CMake版本：>=3.20

GCC版本：>= 10.0

JDK版本：1.8

Maven版本：>= 3.0.0

## 脚本

192.168.28.99:/home/hadoop/chengxingfu/code/build_gluten_velox.sh

```shell
sudo yum install -y locales wget tar tzdata git ccache ninja-build dnf

#git clone http://192.168.2.114/luotianran/gluten.git
base_dir=`pwd`
cd $base_dir/gluten
cd ep/build-arrow/src
./get_arrow.sh
./build_arrow.sh

cd $base_dir/gluten
cd ep/build-velox/src
./get_velox.sh --enable_hdfs=ON
./build_velox.sh --enable_hdfs=ON    该步骤失败gzip: stdin: unexpected end of file，修复办法见build_velox.sh脚本遇到错误一节


cd $base_dir/gluten/cpp
./compile.sh --build_velox_backend=ON --enable_hdfs=ON
cd $base_dir/gluten
mvn clean package -Pbackends-velox -Prss -Pspark-3.2 -Phadoop-3.2 -DskipTests

[INFO] Reactor Summary for Gluten Parent Pom 1.1.0-SNAPSHOT:
[INFO] 
[INFO] Gluten Parent Pom .................................. SUCCESS [  0.088 s]
[INFO] Gluten Shims ....................................... SUCCESS [  0.160 s]
[INFO] Gluten Shims Common ................................ SUCCESS [  7.285 s]
[INFO] Gluten Shims for Spark 3.2 ......................... SUCCESS [  7.126 s]
[INFO] Gluten UI .......................................... SUCCESS [  2.583 s]
[INFO] Gluten Core ........................................ SUCCESS [ 23.328 s]
[INFO] Gluten Data ........................................ SUCCESS [  5.074 s]
[INFO] Gluten Backends Velox .............................. SUCCESS [ 14.474 s]
[INFO] Gluten Celeborn .................................... SUCCESS [  0.007 s]
[INFO] Gluten Celeborn Common ............................. SUCCESS [  2.281 s]
[INFO] Gluten Celeborn Velox .............................. SUCCESS [  2.041 s]
[INFO] Gluten Celeborn Common ............................. SUCCESS [  0.012 s]
[INFO] Gluten Package ..................................... SUCCESS [ 12.618 s]
[INFO] Gluten Substrait Spark ............................. SUCCESS [  6.615 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:23 min
[INFO] Finished at: 2023-12-05T02:41:50-05:00


#执行完成后，gluten的执行包在 gluten/package/target 目录下，使用 gluten-velox-bundle-spark3.2_2.12-centos7-1.1.0-SNAPSHOT.jar 即可
```

## 问题

### build_velox.sh脚本遇到错误

```shell
+ wget -q --max-redirect 3 -O - https://hub.nuaa.cf/google/re2/archive/refs/tags/2023-03-01.tar.gz
+ tar -xz -C re2 --strip-components=1

gzip: stdin: unexpected end of file
tar: Child returned status 1
tar: Error is not recoverable: exiting now
```

/Users/chengxingfu/code/yusur/yusur_gluten/gluten/ep/build-velox/build/velox_ep/scripts/setup-centos7.sh里面的install_re2函数会调用wget_and_untar函数下载并编译

```shell
function install_re2 {
  cd "${DEPENDENCY_DIR}"
  wget_and_untar https://hub.nuaa.cf/google/re2/archive/refs/tags/2023-03-01.tar.gz re2
  cd re2
  $SUDO make install
}
function wget_and_untar {
  local URL=$1
  local DIR=$2
  mkdir -p "${DIR}"
  wget -q --max-redirect 3 -O - "${URL}" | tar -xz -C "${DIR}" --strip-components=1
}
```

这个`wget -q --max-redirect 3 -O -`命令中的-q是不打印执行信息，我们把它去掉，发现是原因是Issued certificate has expired，有一个办法解决，就是加一个参数--no-check-certificate。

但是只改这个脚本，不会生效，因为/Users/chengxingfu/code/yusur/yusur_gluten/gluten/ep/build-velox/src/get_velox.sh会从velox仓库中拉取velox的最新代码覆盖velox_ep目录。我们只要在get_velox.sh的process_setup_centos7函数内修改拉取到的代码即可。

```shell
sed -i "s/wget -q --max-redirect 3 -O -/wget --no-check-certificate -q --max-redirect 3 -O -/g" scripts/setup-centos7.sh
sed -i "s/wget https:/wget --no-check-certificate https:/g" scripts/setup-centos7.sh
```



解法二： yum install -y ca-certificates

### arrow编译问题

cmake  AVX2 required but compiler doesn't support it







# 使用

```shell
#!/bin/bash

SPARK_GLUTEN_PLUGIN_JAR="/home/hadoop/chengxingfu/jars/gluten-thirdparty-lib-centos-7.jar,/home/hadoop/chengxingfu/gluten/package/target/gluten-package-1.1.0-SNAPSHOT-3.2.jar,/home/hadoop/chengxingfu/gluten/package/target/gluten-velox-bundle-spark3.2_2.12-centos_7-1.1.0-SNAPSHOT.jar"
$SPARK_HOME/bin/spark-sql \
    --master local \
    --num-executors 1 \
    --driver-memory 30g \
    --executor-memory 30g \
    --conf spark.plugins=io.glutenproject.GlutenPlugin \
    --conf spark.memory.offHeap.enabled=true \
    --conf spark.memory.offHeap.size=20g \
    --conf spark.shuffle.manager=org.apache.spark.shuffle.sort.ColumnarShuffleManager \
    --conf spark.sql.autoBroadcastJoinThreshold=256MB \
    --conf spark.gluten.sql.columnar.backend.lib=velox \
    --conf spark.gluten.loadLibFromJar=true \
    --conf spark.driver.extraClassPath=${SPARK_GLUTEN_PLUGIN_JAR} \
    --conf spark.executor.extraClassPath=${SPARK_GLUTEN_PLUGIN_JAR} \
    --jars ${SPARK_GLUTEN_PLUGIN_JAR}
```

## 问题

### 找不到local.dir下面的so文件

23/12/05 02:56:27 INFO JniLibLoader: Trying to load library libarrow.so.1200.0.0
Benchmark driver error: /data/spark_tmp/gluten-d289c8e4-6aef-405e-b4e1-067f3c686026/jni/fa31652c-7b53-412c-9f31-976cfbea23f1/gluten-2761979433048022154/libarrow.so.1200.0.0: libprotobuf.so.32: cannot open shared object file: No such file or directory

libarrow.so.1200.0.0 这个文件是存在的

解决方案： 加jar: /home/hadoop/chengxingfu/jars/gluten-thirdparty-lib-centos-7.jar

###  java.io.FileNotFoundException: libgluten.so

还是velox编译拉取的包有问题，

sed -i "s/wget https:/wget --no-check-certificate https:/g" scripts/setup-centos7.sh

也可能是没加这个参数--conf spark.gluten.loadLibFromJar=true

