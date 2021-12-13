[参考](https://soulmachine.gitbooks.io/system-design/content/cn/distributed-id-generator.html)

ID生成的三大核心需求：

- 全局唯一(unique)
- 按照时间粗略有序(sortable by time)
- 尽可能短



# UUID

用过MongoDB的人会知道，MongoDB会自动给每一条数据赋予一个唯一的[ObjectId](https://docs.mongodb.com/manual/reference/method/ObjectId/),保证不会重复，这是怎么做到的呢？实际上它用的是一种UUID算法，生成的ObjectId占12个字节，由以下几个部分组成，

- 4个字节表示的Unix timestamp,
- 3个字节表示的机器的ID
- 2个字节表示的进程ID
- 3个字节表示的计数器

[UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier)是一类算法的统称，具体有不同的实现。UUID的优点是每台机器可以独立产生ID，理论上保证不会重复，所以天然是分布式的，缺点是生成的ID太长，不仅占用内存，而且索引查询效率低。

# 多台MySQL服务器

既然MySQL可以产生自增ID，那么用多台MySQL服务器，能否组成一个高性能的分布式发号器呢？ 显然可以。

假设用8台MySQL服务器协同工作，第一台MySQL初始值是1，每次自增8，第二台MySQL初始值是2，每次自增8，依次类推。前面用一个 round-robin load balancer 挡着，每来一个请求，由 round-robin balancer 随机地将请求发给8台MySQL中的任意一个，然后返回一个ID。

[Flickr就是这么做的](http://code.flickr.net/2010/02/08/ticket-servers-distributed-unique-primary-keys-on-the-cheap/)，仅仅使用了两台MySQL服务器。可见这个方法虽然简单无脑，但是性能足够好。不过要注意，在MySQL中，不需要把所有ID都存下来，每台机器只需要存一个MAX_ID就可以了。这需要用到MySQL的一个[REPLACE INTO](http://dev.mysql.com/doc/refman/5.0/en/replace.html)特性。

这个方法跟单台数据库比，缺点是**ID是不是严格递增的**，只是粗略递增的。不过这个问题不大，我们的目标是粗略有序，不需要严格递增。