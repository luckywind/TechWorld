[参考](https://labuladong.github.io/algo/2/21/42/)

首先，BST 的特性大家应该都很熟悉了：

1、对于 BST 的每一个节点 `node`，左子树节点的值都比 `node` 的值要小，右子树节点的值都比 `node` 的值大。

2、对于 BST 的每一个节点 `node`，它的左侧子树和右侧子树都是 BST。

<font color=red>**从做算法题的角度来看 BST，除了它的定义，还有一个重要的性质：BST 的中序遍历结果是有序的（升序）。 当然如果想要降序，改一下递归顺序，先递归右子树，后递归左子树就行了**。</font> 



# 第K小的元素

[力扣](https://leetcode.cn/problems/kth-smallest-element-in-a-bst/)

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20221127113410113.png" alt="image-20221127113410113" style="zoom:50%;" />

```java
int kthSmallest(TreeNode root, int k) {
    // 利用 BST 的中序遍历特性
    traverse(root, k);
    return res;
}

// 记录结果
int res = 0;
// 记录当前元素的排名
int rank = 0;
void traverse(TreeNode root, int k) {
    if (root == null) {
        return;
    }
    traverse(root.left, k);
    /* 中序遍历代码位置 */
    rank++;
    if (k == rank) {
        // 找到第 k 小的元素
        res = root.val;
        return;
    }
    /*****************/
    traverse(root.right, k);
}
```

复杂度： 这个复杂度是O(N)，如果想达到O(log(N))， 需要每个节点知道它的左子树的大小，即TreeNode需要维护一个size的字段表示左子树的大小，这样每个节点就知道自己的排名是多少了。

# BST转化累加树

<img src="https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/title1.png" alt="img" style="zoom:50%;" />

```java
* 思路：
 * BST的特点： 左子树<=根节点<=右子树
 * 中序遍历是有序的，至于是升序还是降序就看先递归左子树还是先递归右子树。
 *
 * 累加树，要求累加不小于当前节点的值，就是降序累加，于是先递归右子树，来遍历整棵树
public class ConvertBST {
  int nval=0;//新累加值
  TreeNode convertBST(TreeNode root){
     if(root ==null) return null;
    convertBST(root.right);
    //中序位置更新当前节点的值
    nval+=root.val;
    root.val=nval;
    convertBST(root.left);
    return root;
  }
}
```

