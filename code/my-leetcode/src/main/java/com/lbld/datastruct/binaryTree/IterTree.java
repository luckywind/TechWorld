package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-24
 * @Desc
 */
public class IterTree {

  /**
   * 前序遍历二叉树
   */
  public static void preOrder(TreeNode root) {
    //边界条件
    if(root == null){
      return;}
    System.out.println(root.getVal());  //前序位置打印
    preOrder(root.left);
    preOrder(root.right);
  }

}
