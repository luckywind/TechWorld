package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-27
 * @Desc
 * 更新二叉搜索树中的每个节点的值，使其是不小于原值的所有值之和
 *
 * 思路：
 * BST的特点： 左子树<=根节点<=右子树
 * 中序遍历是有序的，至于是升序还是降序就看先递归左子树还是先递归右子树。
 *
 * 累加树，要求累加不小于当前节点的值，就是降序累加，于是先递归右子树，来遍历整棵树
 */
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
