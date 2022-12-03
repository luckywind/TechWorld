package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-24
 * @Desc
 * 构造最大二叉树
 * https://leetcode.cn/problems/maximum-binary-tree/
 * 以最大元素为界限， 左边构造左子树，右边构造右子树，返回树根
 *
 */

public class ConstructMaxTree {

  public static void main(String[] args) {
    int[] nums = new int[]{3, 2, 1, 6, 0, 5};
    ConstructMaxTree maxTree = new ConstructMaxTree();
    TreeNode treeNode = maxTree.constructMaximumBinaryTree(nums);
    IterTree.preOrder(treeNode);

  }
  public TreeNode constructMaximumBinaryTree(int[] nums) {
   return  build(nums, 0, nums.length - 1);
  }

  TreeNode build1(int[] nums, int lo, int hi) {
    // base case
    if (lo > hi) {
      return null;
    }

    // 找到数组中的最大值和对应的索引
    int index = lo, maxVal = Integer.MIN_VALUE;
    for (int i = lo; i <= hi; i++) {
      if (maxVal < nums[i]) {
        index = i;
        maxVal = nums[i];
      }
    }

    // 先构造出根节点
    TreeNode root = new TreeNode(maxVal);
    // 递归调用构造左右子树
    root.left = build(nums, lo, index - 1);
    root.right = build(nums, index + 1, hi);

    return root;
  }


  /**
   * 返回树根，说明迭代函数的返回值是TreeNode,  入参需要确定要处理的数据
   */
  TreeNode  build(int[] nums, int lo, int hi) {


    if (lo > hi) {
      return null;
    }



    //找到lo-hi之间的最大值， 进行递归
    int maxInx=-1;
    int maxVal = Integer.MIN_VALUE;
    for (int i = lo; i <= hi; i++) {
      if (nums[i] > maxVal) {
        maxInx=i;
        maxVal = nums[i];
      }
    }
    TreeNode root = new TreeNode();
    root.setVal(maxVal);
    //递归左边的
    root.left = build(nums, lo, maxInx - 1);
    //递归右边的
    root.right= build(nums, maxInx + 1, hi);
    //返回根节点

    return root;
  }
}
