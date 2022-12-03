package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-27
 * @Desc
 */
public class DeleteBST {

  TreeNode deleteNode(TreeNode root, int key) {
    //边界条件
    if(root == null) return null;


    //有两个子节点， 则要让右子树的最小节点代替自己
    if(root.val==key) //找到了这个节点
    {
      //只有一个子节点，则让该子节点代替自己即可
      if(root.left == null) //只有右子节点
         return  root.right;
      if(root.right==null) return  root.left;

      //有两个子节点，则用右子树的最小节点代替自己
      TreeNode minRight=getMin(root.right);
      //再右子树中删除这个最小节点，因为它要移动到root节点去了
      root.right = deleteNode(root.right, minRight.val);
      //操作链表来进行minRight和root的交换
      minRight.left=root.left;
      minRight.right=root.right;
      root=minRight;
    } else if (root.val > key) {
      //递归删除左子树
      deleteNode(root.left, key);
    } else {
      deleteNode(root.right, key);
    }
    return root;

  }

  //BST的最左侧的节点就是最小的
  private TreeNode getMin(TreeNode root) {
    while (root.left != null) {
      root = root.left;
    }
    return root;
  }

}
