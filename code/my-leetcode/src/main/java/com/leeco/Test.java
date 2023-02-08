package com.leeco;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2023-01-03
 * @Desc
 */
public class Test {



  public static void main(String[] args) {
    int[] nums = new int[]{-1,0,1,2,-1,-4};
    Test test = new Test();
    List<List<Integer>> lists = test.threeSum(nums, 0);
    for (int i = 0; i < lists.size(); i++) {
      System.out.println(Arrays.toString(lists.get(i).toArray()));
      System.out.println("\n");
    }
  }

  public List<List<Integer>> threeSum(int[] nums,int target) {
    Arrays.sort(nums);//先排序，省的后面不停排序
    List<List<Integer>> res = new ArrayList<>();
    int n=nums.length;
    for(int i=0;i<n-2;i++){
      int x=nums[i];//第一个元素确定下来,再从nums[i+1,..]中找其他两个数
      ArrayList<ArrayList<Integer>> tps = twoSum(nums, i + 1, target - x);
      for(int j=0;j<tps.size();j++){
        boolean add = tps.get(j).add(nums[i]);
        res.add(tps.get(j));
      }
      //去除第一个元素重复的
      while(i<n-2 && nums[i]==nums[i+1]) i++;
    }
    return res;
  }
  ArrayList<ArrayList<Integer>> twoSum(int[] nums,int start,  int target){
    //注意，调用这个函数前，保证nums是排序的
    ArrayList<ArrayList<Integer>> res = new ArrayList<>();
    int n = nums.length;
    int left=start,right=n-1; //左右指针
    while(left<right){
      if(nums[left]+nums[right] < target){
        //需要更新left
        while(left<right && nums[left]==nums[left+1])left++;
      }else  if(nums[left]+nums[right] > target){
        //需要更新right
        while(left<right && nums[right]==nums[right+1])right--;
      }else{
        ArrayList<Integer> tp = new ArrayList<>();
        tp.add(nums[left]);
        tp.add(nums[right]);
        res.add(tp);
      }
    }
    return res;
  }

  public static  boolean isValid(String s) {
    char[] ch=s.toCharArray();
    Stack<Character> stack=new Stack();
    for(int i=0;i<ch.length;i++){
      //先检查栈顶元素与待插入元素是否匹配，如果匹配，则把栈顶和待插入元素丢掉；否则入栈


      if(!stack.empty()){
        char t=stack.peek();
        if(t=='(' && ch[i]==')' ||
            t=='[' && ch[i]==']' ||
            t=='{' && ch[i]=='}' )
        {stack.pop();}
        else{
          stack.push(ch[i]);
        }

      }else{
        stack.push(ch[i]);
      }

    }

    //遍历完成后，检查栈是否为空
    return stack.empty();

  }

}
