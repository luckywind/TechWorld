package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-27
 * @Desc
 * 二叉搜索树的第K小元素
 * 中序遍历结果是递增的，遍历到第K个就行了
 */
public class BSTSmallest {

  int i=0;
  int kth=0;
  int kthElement=0;

  void traverse(TreeNode root) {
    if (root==null) return ;
    traverse(root.left);
    //中序位置  中间值
    if (i == kth - 1) {
      kthElement=root.val;

    }
    i++;
    traverse(root.right);
  }

  public int kthSmallest(TreeNode root, int k) {
    kth=k;
    traverse(root);
    return kthElement;

  }
  public static void main(String[] args) {

  }

}
