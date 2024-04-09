package com.lbld.datastruct.binaryTree;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-24
 * @Desc从前序与中序遍历序列构造二叉树
 */
public class ConstructTreeByRes {

  public static void main(String[] args) {
    int[]inorder = new int[]{9,3,15,20,7};
    int[] postorder = new int[]{9,15,7,20,3};
    ConstructTreeByRes builder = new ConstructTreeByRes();
    TreeNode root = builder.buildTree(postorder, inorder);
    IterTree.preOrder(root);
  }

  TreeNode buildTree(int[] preorder, int[] inorder) {
    return build(preorder,0, preorder.length-1, inorder,0,inorder.length-1);
  }
  TreeNode build(int[] preorder, int preStart,int preEnd, int[] inorder,int inStart,int inEnd) {
    //边界条件
    if (preEnd < preStart || inStart>inEnd) {
      return null;
    }
    //构造当前节点
    int rootVal = preorder[preStart];
    TreeNode root = new TreeNode(rootVal);
    //去中序遍历中找到这个值的索引index
    int index=inStart;
    for (int i = inStart; i < inEnd; i++) {
      if (inorder[i] == rootVal) {
        index=i;
        break;
      }
    }
    //左子树中序遍历
    int left_in_start = inStart; //ok
    int left_in_end=index-1;   //ok
    //右子树中序遍历
    int right_in_start=index+1; //ok
    int right_in_end=inEnd;  //ok

    //左子树元素个数
    int left_size = left_in_end - left_in_start +1 ; //ok
    //左子树前序遍历结果索引
    int left_pre_start=preStart+1; //因为preStart是根节点
    int left_pre_end = preStart + left_size -1; //注意
    int right_pre_start=left_pre_end+1;

    root.left = build(preorder,left_pre_start,left_pre_end, //左子树前序
        inorder, left_in_start,left_in_end);  //左子树前序
    root.right = build(preorder,right_pre_start,preEnd, //右子树前序
        inorder, index+1,inEnd);  //右子树中序
    return  root;
  }




}
