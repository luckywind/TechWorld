# 常见排序算法

Java中常见的排序算法有以下八个：

- 直接插入排序
- 希尔排序
- 简单选择排序
- 堆排序
- 冒泡排序
- 快速排序
- 归并排序
- 基数排序



## 直接插入排序

### 思想

插入排序：
<font color=red>把元素依次当作当前插入的元素，前面的是已经排好序的，要插入当前元素，需要从已排序的数组后面开始，依次和当前要插入的元素比较，比插入元素大就要往后移动
当前元素比要插入元素小时，把要插入元素放到当前元素后面即可。</font>

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221024095636605.png" alt="image-20221024095636605" style="zoom:50%;" />

### 源码

<font color=red>这里注意!!!  insertNum要记下来，因为移动后a[i]已经变了 </font>

```java
 /**
     *
     * @param a
     */
    public void insertSort(int[] a) {
        int length = a.length;
        int insertNum;
        for (int i = 1; i < length; i++) {//第2个元素开始作为新插入元素，所以从1开始遍历
            insertNum = a[i];//新插入元素
            int j = i - 1;//插入元素的前一个元素
            while (j >= 0 && a[j] > insertNum) {//当前面的元素a[j]大于插入元素时，需要互换
                a[j + 1] = a[j];    //把大元素往后移动
                j--;
            } //至此,a[j]不满足a[j]>insertNum,也就是说a[j+1]是第一个大于insertNum的数， 最后执行j--
            a[j + 1] = insertNum;//把新插入元素放到所有不大于插入元素的最右边
        }
    }

```

## 希尔排序:改进插入排序

### 思想

<font color=red>是插入排序的改进版</font>：  **插入排序在插入一个小元素时，需要把前面的大元素依次往后移动，开销巨大。 希尔排序通过较大的间隔(增量)，选择一个子序列，对子序列进行插入排序，将小元素快速送到前面。**

增量递减至1的子序列进行插入排序，当增量为1时，子序列就是整个待排序的数组

![image-20221030171724080](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221030171724080.png)

### 复杂度分析

是冲破2次复杂度的一种算法，速度较快，其复杂度依赖于增量序列， 当增量序列为2^k-1时，复杂度为O(n^1.5)

详见https://zhuanlan.zhihu.com/p/73726253和https://blog.csdn.net/ginnosx/article/details/12263619

### 源码

```java
public static void shellSort(int[] arr) {
    int length = arr.length;
    int temp;
  for (int step = length / 2; step >= 1; step /= 2) {// 步长/2逐渐减少 产生多个子序列
      for (int i = step; i < length; i++) {  //对每个子序列按照插入排序的思想排序
            temp = arr[i];
            int j = i - step;
            while (j >= 0 && arr[j] > temp) {//前面元素逐个向后移动
                arr[j + step] = arr[j];
                j -= step;
            }
            arr[j + step] = temp;//插入当前元素
        }
    }
}



public void shellSort(int[] a) {
        int length = a.length;
        int d = length / 2;
        while (d > 0) {
            for (int i = 0; i < d; i++) {
                for (int j = i + d; j < length; j += d) { //从i+d开始，即组内第二个元素开始作为插入元素
                    int k = j - d; //子序列内，当前插入元素的前一个元素
                    int insertNum = a[j]; //待插入元素
                    while (k >= 0 && a[k] > insertNum) {
                        a[k + d] = a[k];
                        k -= d;
                    }
                    a[k + d] = insertNum;
                }
            }
            d /= 2;
        }

    }
```

## 简单选择排序

### 思想

> 常用于取序列中最大最小的几个数时。

依次选择子序列最小值交换到子序列头部

1. 遍历整个序列，将最小的数放在最前面。
2. 遍历剩下的序列，将最小的数放在最前面。
3. 重复第二步，直到只剩下一个数。

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221024101807392.png" alt="image-20221024101807392" style="zoom:50%;" />

### 源码

```java
 /**
     * 依次选择子序列最小值交换到子序列头部
     *
     * @param a
     */
    public void selectSort(int[] a) {
        int minIndex;
        int tmp;
        for (int i = 0; i < a.length; i++) {
						//初始化
            minIndex = i;
            //找minIndex
            for (int j = i + 1; j < a.length; j++) {
                if (a[j] < a[minIndex]) {
                    minIndex = j;
                }
            }
            //minIndex和i交换
            tmp = a[i];
            a[i] = a[minIndex];
            a[minIndex] = tmp;
        }
    }
```

## 堆排序

### 思想

对简单选择排序的优化。

1. 将序列构建成大顶堆。
2. 将根节点与最后一个节点交换，然后断开最后一个节点。
3. 重复第一、二步，直到所有节点断开。

### 源码

```java
 public void heapSort(int[] a) {
        System.out.println("开始排序");
        int arrayLength = a.length;
        //循环建堆
        for (int i = 0; i < arrayLength - 1; i++) {
            //建堆
            buildMaxHeap(a, arrayLength - 1 - i);
            //交换堆顶和最后一个元素
            swap(a, 0, arrayLength - 1 - i);
            System.out.println(Arrays.toString(a));
        }
    }

    private void swap(int[] data, int i, int j) {
        // TODO Auto-generated method stub
        int tmp = data[i];
        data[i] = data[j];
        data[j] = tmp;
    }

    //对data数组从0到lastIndex建大顶堆
    private void buildMaxHeap(int[] data, int lastIndex) {
        //从lastIndex处节点（最后一个节点）的父节点开始
        for (int i = (lastIndex - 1) / 2; i >= 0; i--) {
            //k保存正在判断的节点
            int k = i;
            //如果当前k节点的子节点存在
            while (k * 2 + 1 <= lastIndex) {
                //k节点的左子节点的索引
                int biggerIndex = 2 * k + 1;
                //如果biggerIndex小于lastIndex，即biggerIndex+1代表的k节点的右子节点存在
                if (biggerIndex < lastIndex) {
                    //若果右子节点的值较大
                    if (data[biggerIndex] < data[biggerIndex + 1]) {
                        //biggerIndex总是记录较大子节点的索引
                        biggerIndex++;
                    }
                }
                //如果k节点的值小于其较大的子节点的值
                if (data[k] < data[biggerIndex]) {
                    //交换他们
                    swap(data, k, biggerIndex);
                    //将biggerIndex赋予k，开始while循环的下一次循环，重新保证k节点的值大于其左右子节点的值
                    k = biggerIndex;
                } else {
                    break;
                }
            }
        }
    }

```

## 冒泡排序

### 思想

两两比较，将大数冒后面去

### 源码



```java
  /**
     * 冒泡排序
     * 设置循环次数。
     * 设置开始比较的位数，和结束的位数。
     * 两两比较，将大数冒后面去
     * 重复2、3步，直到循环次数完毕
     *
     * @param a
     */
    public void bubbleSort(int[] a) {
        int length = a.length;
        int temp;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a.length - i - 1; j++) {
                if (a[j] > a[j + 1]) {
                    temp = a[j];
                    a[j] = a[j + 1];
                    a[j + 1] = temp;
                }
            }
        }
    }

```

## 快速排序

### 思想



选择一个枢纽元，比它更小的放在左边，比它更大的放在右边，使得枢纽元位置正确。
     具体办法，从数组两头挑选两个和枢纽元构成逆序的数对，并交换以解决逆序；当所有构成逆序的数对都完成交换后，枢纽元就归位了。

![image-20221024121438051](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221024121438051.png)

[快速排序的三种方式](https://juejin.cn/post/7056416599003136014)

1. 挖坑法
2. 左右指针法，下面的代码就是左右指针法
3. 前后指针法

### 源码

```java
/**
     * 快速排序
     * 选择一个枢纽元，比它更小的放在左边，比它更大的放在右边。
     * 具体办法，是从左边第一个开始找，直到找到一个比枢纽元大的元素，
     * 再从右边最后一个开始找，直到找到一个比枢纽元小的元素
     *
     * @param
     */
    public  void quickSort(int[] numbers, int start, int end) {
        if (start < end) {
            int base = numbers[start]; // 选定的基准值（第一个数值作为基准值）
            int temp; // 记录临时中间值
            int i = start, j = end;
            do {
               //左指针找第一个大于base的 
               while ((numbers[i] < base) && (i < end))
                    i++;
               //右指针找第一个小于base的 
                while ((numbers[j] > base) && (j > start))
                    j--;
               
                if (i <= j) { 
                    // i ,j 位置互换
                    temp = numbers[i];
                    numbers[i] = numbers[j];
                    numbers[j] = temp;
                    // 推进i,j
                    i++;
                    j--;
                }
            } while (i <= j);   //到这里，其实[start,end]区间内枢纽元(这里我们选择的是原数组start位置的值，到了这里start位置可能不是枢纽元了)已经排好序了。
          //推出while循环，说明i>j了， 而do中的while保证了numbers[i]>=base,  numbers[j]<=base, do中的逆序交换保证了
            if (start < j)  //代表， 枢纽元左边有未排序序列， 对左边分区进行排序
                quickSort(numbers, start, j);//左端点作为枢纽元
            if (end > i)//代表，枢纽元右边有未排序序列，对右边分区进行排序
                quickSort(numbers, i, end);
        }
    }

```

## 归并排序

### 思想

```
归并排序
选择相邻两个数组成一个有序序列。
选择相邻的两个有序序列组成一个有序序列。
重复第二步，直到全部组成一个有序序列。
参考https://www.cnblogs.com/chengxiao/p/6194356.html
```

### 源码

```java
 public static void mergeSort(int []arr){
        int []temp = new int[arr.length];//在排序前，先建好一个长度等于原数组长度的临时数组，避免递归中频繁开辟空间
        mergeSort(arr,0,arr.length-1,temp);
    }
    private static void mergeSort(int[] arr, int left, int right, int []temp){
        if(left<right){
            int mid = (left+right)/2;
            mergeSort(arr,left,mid,temp);//左边归并排序，使得左子序列有序
            mergeSort(arr,mid+1,right,temp);//右边归并排序，使得右子序列有序
            merge(arr,left,mid,right,temp);//将两个有序子数组合并操作
        }
    }
    private static void merge(int[] arr,int left,int mid,int right,int[] temp){
        int i = left;//左序列指针
        int j = mid+1;//右序列指针
        int t = 0;//临时数组指针
        while (i<=mid && j<=right){
            if(arr[i]<=arr[j]){
                temp[t++] = arr[i++];
            }else {
                temp[t++] = arr[j++];
            }
        }
        while(i<=mid){//将左边剩余元素填充进temp中
            temp[t++] = arr[i++];
        }
        while(j<=right){//将右序列剩余元素填充进temp中
            temp[t++] = arr[j++];
        }
        t = 0;
        //将temp中的元素全部拷贝到原数组中
        while(left <= right){
            arr[left++] = temp[t++];
        }
    }
```

## 基数排序

### 思想

```
/**
 * 基数排序
 * 其实就是从低位到高位一位一位的比较
 *
 * 基数排序可以看成是桶排序的扩展，以整数排序为例，主要思想是将整数按位数划分，准备 10 个桶，代表 0 - 9，
 * 根据整数个位数字的数值将元素放入对应的桶中，之后按照输入赋值到原序列中，依次对十位、百位等进行同样的操作，最终就完成排序的操作
 *
 * 将所有的数的个位数取出，按照个位数进行排序，构成一个序列。
 * 将新构成的所有的数的十位数取出，按照十位数进行排序，构成一个序列。
 * @param array
 */
```

### 源码

```java
 public void baseSort(int[] array) {
        //首先确定排序的趟数;
        int max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        int time = 0;
        //判断位数;
        while (max > 0) {
            max /= 10;
            time++;
        }
        //建立10个队列;
        List<ArrayList> queue = new ArrayList<ArrayList>();
        for (int i = 0; i < 10; i++) {
            ArrayList<Integer> queue1 = new ArrayList<Integer>();
            queue.add(queue1);  //0-9 共10个桶的队列
        }
        //进行time次分配和收集;
        for (int i = 0; i < time; i++) {
            //分配数组元素;
            for (int j = 0; j < array.length; j++) {
                //得到数字的第time+1（也就是i+1）位数;
                int x = array[j] % (int) Math.pow(10, i + 1) / (int) Math.pow(10, i);
                ArrayList<Integer> queue2 = queue.get(x); //取出相应的桶
                queue2.add(array[j]);
                queue.set(x, queue2);
            }
            int count = 0;//元素计数器;
            //收集队列元素;
            for (int k = 0; k < 10; k++) { //依次收集10个桶里的元素
                while (queue.get(k).size() > 0) {
                    ArrayList<Integer> selectedQueue = queue.get(k);
                    array[count] = selectedQueue.get(0);
                    selectedQueue.remove(0);
                    count++;
                }
            }
        }
```



# 复杂度总结

![排序算法复杂度](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/%E6%8E%92%E5%BA%8F%E7%AE%97%E6%B3%95%E5%A4%8D%E6%9D%82%E5%BA%A6.png)

