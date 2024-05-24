package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-27
 * @Desc
 * 找到搜索树中的值为target的节点
 * 思路：遍历树，匹配到就行， 但是利用搜索树的特点， 可以加快搜索速度
 */
public class SearchBST {
  TreeNode searchBST(TreeNode root, int target) {
      if(root == null) return null;
      //前序位置 匹配
    if(root.val==target) return root;//匹配到了就返回
    else if (root.val < target) {   //target在右子树中
     return searchBST(root.right, target);
    } else {
     return searchBST(root.left, target);
    }
  }


}
