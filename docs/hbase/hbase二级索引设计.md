[hbase二级索引设计](https://cloud.tencent.com/developer/article/1081489)

# **概要设计**

**主要思路：**

为每个DataTable创建一个与之对应的IndexTable，通过各种途径，保证IndexTable Region与DataTable Region一一对应，并且存储在同一个RegionServer上，存储结构如图所示。最终要实现的效果是，每个IndexTable Region是对应的DataTable Region的局部索引，使用索引进行查询时，将对每个IndexTable Region进行检索，找出所有符合条件的DataTable RowKey，再根据DataTable RowKey到对应的DataTable Region中读取相应DataTable Row。

![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/1620.jpeg)

# 详细设计

## **IndexTable的创建**

IndexTable的创建主要出现在两个时机，

一是创建新DataTable时，系统根据索引定义，自动创建对应的IndexTable；

二是对已存在的DataTable，用户调用接口方法，动态创建索引。

**IndexTable的创建过程如下：**

**第一步，：**

获取DataTable的所有RegionInfo，得到所有DataTable Region的StartKey。

**第二步：**

结合索引定义和DataTable Region的StartKey信息，调用HBaseAdmin的createTable(final HTableDescriptor desc, byte [][] splitKeys)方法创建索引表。

通过以上两步便建立了IndexTable Region和DataTable Region的以StartKey为依据的一一对应关系。

##  IndexTable RowKey的设计

IndexTable的RowKey由四部分组成，按顺序依次是：DataTable Region StartKey、IndexName、IndexValue和DataTable RowKey，如图所示。

![image-20221219200858598](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221219200858598.png)

1. <font color=red>DataTable Region StartKey</font>。将DataTable Region的StartKey作为IndexTable Region的RowKey的第一部分，主要基于两个方面的考虑。

- 一是使得IndexTable Region和对应的DataTable Region拥有相同的StartKey，这样便可将StartKey作为两个Region的关联依据；

- 二是当DataTable Region分裂时，可使用相同的SplitKey对IndexTable Region进行相应的分裂操作，并将新产生的DataTable Region和IndexTable Region建立关联关系。

2. <font color=red>IndexName</font>。

**在一张DataTable的基础上可以定义多个索引**，如果为每个索引创建一个IndexTable，则在实际应用过程中，势必会产生大量的IndexTable，当DataTable Region分裂时，还需要对与之关联的所有IndexTable Region分别执行分裂操作，这将消耗大量的系统资源，并且不易维护。因此，我们考虑将一张DataTable的所有索引数据，存放到同一张IndexTable中，**不同索引的数据以IndexName进行区分。**

3. <font color=red>IndexValue</font>。如果索引是单列索引，IndexValue就是DataTable Row的某个Column Value，如果索引是组合索引的话，则IndexValue就是DataTable Row的多个Column Value组合而成的。

4. <font color=red>DataTable RowKey</font>。被用来定位DataTable Row，以获取最终的数据。

## **IndexTable Region的分配控制**

IndexTable Region的分配控制过程，即是保证IndexTable Region与DataTable Region的一一对应，并且被分配到同一个RegionServer的过程。

自定义LoadBalancer，重写balanceCluster方法，主要增加对IndexTable Region的分配控制。以相同的StartKey作为IndexTable Region和DataTable Region的关联依据，将IndexTable Region和与其对应的DataTable Region分配到同一个RegionServer。自定义LoadBalancer对IndexTable Region的分配控制过程如图所示。

![img](https://ask.qcloudimg.com/http-save/yehe-1484446/vgkc77zm2p.jpeg?imageView2/2/w/1620)

**注意：**

这里只增加对IndexTable Region分配的控制，并不对DataTable Region的分配进行干预，DataTable Region由HBase按照指定的[负载均衡](https://cloud.tencent.com/product/clb?from=10680)策略进行分配，使得对现有HBase运行环境的影响降到最小。

```java
public class ColocatedRegionAssigner extends BaseRegionPlacementPolicy {
    @Override
    public void assignRegions(List<RegionInfo> regions) {
        Map<byte[], RegionInfo> indexRegions = new TreeMap<>(Bytes.BYTES_COMPARATOR);
        Map<byte[], RegionInfo> dataRegions = new TreeMap<>(Bytes.BYTES_COMPARATOR);
        
        // 分离索引表和数据表Region
        for (RegionInfo region : regions) {
            if (region.getTable().equals("IndexTable")) {
                indexRegions.put(region.getStartKey(), region);
            } else if (region.getTable().equals("DataTable")) {
                dataRegions.put(region.getStartKey(), region);
            }
        }
        
        // 配对相同StartKey的Region
        for (byte[] startKey : indexRegions.keySet()) {
            RegionInfo indexRegion = indexRegions.get(startKey);
            RegionInfo dataRegion = dataRegions.get(startKey);
            
            if (dataRegion != null) {
                // 分配到同一RegionServer
                ServerName targetServer = selectOptimalServer();
                assignRegion(indexRegion, targetServer);
                assignRegion(dataRegion, targetServer);
            }
        }
    }
}
```

配置

```xml
<!-- hbase-site.xml -->
<property>
  <name>hbase.master.region.placement.policy</name>
  <value>com.example.ColocatedRegionAssigner</value>
</property>
```



## IndexTable Region的分裂过程

<font color=red>关键就是DataTable Region分裂后，会产生一个以splitKey为StartKey的新Region, 那么IndexTable Region如何拆分得到一个新Region和它一一对应呢？答案就是把Rowkey的第一部分(也就是DataTable Region的StartKey大于splitKey的替换成新的StartKey，也就是splitKey)</font>

![image-20221219203617428](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221219203617428.png)

本文将以一个示例讲述IndexTable Region的分裂过程，假设当前有一个DataTable Region和对应的IndexTable Region，如图所示，绿色表格为DataTable Region，红色表格为IndexTable Region。

![img](https://ask.qcloudimg.com/http-save/yehe-1484446/z7jxj41b7v.jpeg?imageView2/2/w/1620)

根据概要设计中的说明，假设SplitKey:03，经过一系列操作之后，原来的DataTable Region和IndexTable Region均分裂成两个新的Region，并且依然保存一一对应关系。最终效果如图所示。

![img](https://ask.qcloudimg.com/http-save/yehe-1484446/d4infe8osj.jpeg?imageView2/2/w/1620)

**IndexTable Region具体分步骤说明如下：**

**第一步：**

确定SplitKey后，遍历IndexTable Region中所有的行，找出所有DataTable RowKey大于或等于SplitKey的Row，如图所示。

![img](https://ask.qcloudimg.com/http-save/yehe-1484446/tscub4yfrf.jpeg?imageView2/2/w/1620)

**第二步：**

删除第一步找到的所有DataTable RowKey大于或等于SplitKey的Row，并将RowKey的第一部分（DataTable Region StartKey）替换成SplitKey后，重新插入到IndexTable Region中，如图所示。

![img](https://ask.qcloudimg.com/http-save/yehe-1484446/ifgoywfkgq.jpeg?imageView2/2/w/1620)

**第三步：**

以SplitKey，同时对DataTable Region和IndexTable Region进行分裂操作，如图所示。最终达到如图所示的效果。

![img](https://ask.qcloudimg.com/http-save/yehe-1484446/rzgl0yl7hx.jpeg?imageView2/2/w/1620)

## 数据的写入过程

数据的写入过程，主要通过Coprocessor代理完成，保证更新DataTable Region数据的同时更新IndexTable Region中的数据。具体步骤如图所示。

![img](https://ask.qcloudimg.com/http-save/yehe-1484446/ups8mc5p9u.jpeg?imageView2/2/w/1620)

**2.6.   数据的读取过程**

与数据的写入过程一样，数据的读取过程也是由Coprocessor代理完成。Coprocessor收到查询请求后，首先判断是否可以利用某个索引，提高本次查询效率。如果有可用的索引，则先从IndexTable Region中查出所有符合条件的RowKey，再根据RowKey，从DataTable Region中查找出实际的数据返回给客户端。具体步骤如图所示。

![img](https://ask.qcloudimg.com/http-save/yehe-1484446/vdmnmosgs2.jpeg?imageView2/2/w/1620)

**注意：**

描述的IndexTable Region分配机制，保证了IndexTable Region和对应的DataTable Region处于同一个RegionServer上，这样便解决了在有大量符合查询条件的Row的情况下，通过RowKey从DataTable Region中获取实际数据的效率问题。

此外，考虑到从IndexTable中获取的RowKey列表也是有序的，所以在实现时，并不直接调用HBase提供的Get接口，去获取单个实际数据，而是在HFile Data Index的辅助下，通过遍历HFile，获取所有实际数据。