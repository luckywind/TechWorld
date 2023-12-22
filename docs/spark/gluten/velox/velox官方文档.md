[velox github](https://github.com/facebookincubator/velox/)

[velox document](https://facebookincubator.github.io/velox/index.html)

[使用CLion进行C++开发](https://bbs.huaweicloud.com/blogs/158643)

[一文带你全面剖析 Facebook Velox 运行机制](https://juejin.cn/post/7211451845034377271)

[从源码看Velox如何做序列化](https://developer.aliyun.com/article/1339651)

[Facebook Velox 运行机制全面解析](https://mp.weixin.qq.com/s/jX58PUmrKgT08A5WfloBRg)

# build velox

192.168.28.99

```shell
base_dir=`pwd`
set -uex
git clone --recursive http://192.168.2.114/luotianran/velox.git 
cd velox
# if you are updating an existing checkout
git submodule sync --recursive
git submodule update --init --recursive

./scripts/setup-ubuntu.sh 
make
```

Run `make` in the root directory to compile the sources. For development, use `make debug` to build a non-optimized debug version, or `make release` to build an optimized version. Use `make unittest` to build and run tests.

> 注意，中间需要提供gitlab账号

构建失败

```shell
CMake Error at /usr/local/share/cmake-3.25/Modules/FindPackageHandleStandardArgs.cmake:230 (message):
  Could NOT find OpenSSL, try to set the path to OpenSSL root folder in the
  system variable OPENSSL_ROOT_DIR: Found unsuitable version "1.0.2k", but
  required is at least "1.1.1" (found /usr/lib64/libcrypto.so, )
```

需要Clion的CMake通过后，才可以单元测试