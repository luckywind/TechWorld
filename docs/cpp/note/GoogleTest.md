[GooleTest用户指南](https://google.github.io/googletest/)

一个TestSuite包含多个test，当这些test需要共用一些对象时，可放入test fixture类。

一个test程序可以包含多个TestSuite

[GoogleTest 测试框架](https://developer.aliyun.com/article/1415828?spm=a2c6h.24874632.expert-profile.15.18cc36c54hNfv8)

# 断言

1. ASSERT_* 产生fatal型失败会终止函数
2. EXPECT_*产生非fatal型失败，不会终止函数
3. 可通过 << 添加自定义的失败信息
4. [各种断言列表](https://google.github.io/googletest/reference/assertions.html)

```c
ASSERT_EQ(x.size(), y.size()) << "Vectors x and y are of unequal length";

for (int i = 0; i < x.size(); ++i) {
  EXPECT_EQ(x[i], y[i]) << "Vectors x and y differ at index " << i;
}
```

# 简单测试

1. 使用TEST()宏来定义和命名一个test函数。

2. 在这个函数里使用gtest断言检查值

   ```c
   TEST(TestSuiteName, TestName) {
     ... test body ...
   }
   ```

相关的test用TestSuite来分组

# Test Fixtures：多个测试使用相同的数据配置

1. 继承testing::Test类，body以protected开头
2. 声明共用对象
3. 如果需要：写一个默认的构造函数/SetUp()函数来初始化对象
4. 如果需要：写一个析构函数/TearDown()函数释放资源

如果使用fixture，使用TEST_F代替TEST，因为它允许获取test fixture里的对象。

TEST_F的第一个参数必须是test fixture类的名字，这个宏不需要指定test suite名字。

每个test拿到的test fixture都是gtest新建的，不会互相影响。

例如，我们要给一个FIFO队列创建测试，队列接口如下：

```c
template <typename E>  // E is the element type.
class Queue {
 public:
  Queue();
  void Enqueue(const E& element);
  E* Dequeue();  // Returns NULL if the queue is empty.
  size_t size() const;
  ...
};
```

首先，我们定义一个test fixture：

```c
class QueueTest : public testing::Test {
 protected:
  void SetUp() override {
     // q0_ remains empty
     q1_.Enqueue(1);
     q2_.Enqueue(2);
     q2_.Enqueue(3);
  }

  // void TearDown() override {}

  Queue<int> q0_;
  Queue<int> q1_;
  Queue<int> q2_;
};
```

现在，我们写测试

```c
TEST_F(QueueTest, IsEmptyInitially) {
  EXPECT_EQ(q0_.size(), 0);
}

TEST_F(QueueTest, DequeueWorks) {
  int* n = q0_.Dequeue();
  EXPECT_EQ(n, nullptr);

  n = q1_.Dequeue();
  ASSERT_NE(n, nullptr);
  EXPECT_EQ(*n, 1);
  EXPECT_EQ(q1_.size(), 0);
  delete n;

  n = q2_.Dequeue();
  ASSERT_NE(n, nullptr);
  EXPECT_EQ(*n, 2);
  EXPECT_EQ(q2_.size(), 1);
  delete n;
}
```



# main函数

大多数情况下，是不需要写main函数的，只需要使用gtest_main进行link，它定义了入口。

main函数只在fixture和test suite框架无法表达的情况，而你又需要在测试前做的工作。

main函数必须返回RUN_ALL_TESTS()来运行所有测试

```c
int main(int argc, char **argv) {
   // 1.定义 main 函数：初始化 gtest
   ::testing::InitGoogleTest(&argc, argv);
   // 2.定义 main 函数：开启全部测试
   return RUN_ALL_TESTS();
 }
```



# 宏

gtest除了定义了TEST/TEST_F外，还定义了TEST_P宏

语法：

```c
TEST_P(TestFixtureName, TestName) {
  ... statements ...
}
```

可以通过GetParam()函数获取测试参数

























