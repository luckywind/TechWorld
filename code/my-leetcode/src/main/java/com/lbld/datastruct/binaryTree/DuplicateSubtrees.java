package com.lbld.datastruct.binaryTree;

import apple.laf.JRSUIUtils.Tree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2022-11-26
 * @Desc
 * 要求： 找到相同的子树，并返回子树的根节点
 * 相同子树，用map记录树出现的次数，如何用String表示一颗树？就是树的遍历结果(前中后序都可以)！
 * 所以问题转化为计算每个子树的遍历结果。并利用外部存储记录中间结果(出现的次数)和答案
 * 回想二叉树的三个套路
 * 迭代函数的返回值改成子树遍历结果(String)
 *

 */
public class DuplicateSubtrees {

  HashMap<String, Integer> treeNum = new HashMap<String, Integer>();
  List<TreeNode> res = new ArrayList<>();

  public List<TreeNode> findDuplicateSubtrees(TreeNode root) {
    traverse(root);
    return res;
  }

  String traverse(TreeNode root) {
    //边界条件:空树遍历结果用#代替
    if (root == null) {
      return "#";
    }

    String left = traverse(root.left);
    String right = traverse(root.right);
    //我们选择后序遍历，后序位置，组织子树的遍历结果
    String treeKey = left + "," + right + "," + root.val;
    Integer find = treeNum.getOrDefault(treeKey, 0);
    if (find == 1) { //出现过
      res.add(root);
    }
    treeNum.put(treeKey, find+ 1);
    return treeKey;
  }


}
