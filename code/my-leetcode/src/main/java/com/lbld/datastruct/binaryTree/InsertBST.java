package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-27
 * @Desc
 * 插入一个元素，需要先遍历，找到它应该在的位置
 */
public class InsertBST {

  TreeNode  traverse(TreeNode root, int target) {
    if (root == null) {
      return new TreeNode(target);}

    if (root.val < target) {   //target在右子树中，继续搜索右子树
      root.right=traverse(root.right, target);
    } else {
      root.left= traverse(root.left, target);
    }
    return root;
  }
}
