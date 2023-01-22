package com.lbld.datastruct.dp;

import com.lbld.datastruct.binaryTree.TreeNode;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-12-14
 * @Desc https://leetcode.cn/problems/path-sum/?show=1
 * 给你二叉树的根节点 root 和一个表示目标和的整数 targetSum 。
 * 判断该树中是否存在 根节点到叶子节点 的路径，这条路径上所有节点值相加等于目标和 targetSum 。
 * 如果存在，返回 true ；否则，返回 false 。
 *
 * 叶子节点 是指没有子节点的节点。
 */
public class HasPathSum {
  //遍历，记录路径和
  Boolean found=false;
  int targetSum;
  int curSum=0;//当前和


  /**
   * 分解思路
   * @param root
   * @param targetSum
   * @return
   */
  public boolean hasPathSum(TreeNode root, int targetSum) {
    //分解的思路
     if(root==null) return false;
     if(root.left ==null && root.right==null && root.val==targetSum) return true;
     return hasPathSum(root.left, targetSum-root.val)
        || hasPathSum(root.right,targetSum-root.val);
  }

  /**
   * 遍历思路
   * @param root
   * @param targetSum
   * @return
   */
  public boolean hasPathSum_2(TreeNode root, int targetSum) {
    //分解的思路
    // if(root==null) return false;
    // if(root.left ==null && root.right==null && root.val==targetSum) return true;
    // return hasPathSum(root.left, targetSum-root.val)
    //    || hasPathSum(root.right,targetSum-root.val);
    this.targetSum=targetSum;
    traverse(root);
    return found;
  }

  public void traverse(TreeNode root){
    //边界条件
    if(root==null) return;

    //前序位置,更新curSUm
    curSum=curSum+root.val;
    if(root.left ==null && root.right==null){
      if(curSum==targetSum) found=true;
    }
    traverse(root.left);
    traverse(root.right);
    //后序，离开当前节点了，更新curSUm
    curSum-=root.val;
  }
}
