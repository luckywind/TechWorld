package com.cxf.link;


/**
 * Copyright (c) 2015 xx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xx.com>
 * @Date 2023-01-14
 * @Desc
 * 两两交换链表
 * https://leetcode.cn/problems/swap-nodes-in-pairs/
 */
public class SwapPair {

  public static void main(String[] args) {
    ListNode head = new ListNode(1);
    head.next = new ListNode(2,new ListNode(3,new ListNode(4)));
    ListNode res = swapPairs(head);
    while (res != null) {
      System.out.println(res.val);
      res = res.next;
    }
  }

  public static  ListNode swapPairs(ListNode head) {
    if(head == null || head.next==null) return head;
    ListNode dummy=new ListNode(0,head);// 设置一个虚拟头结点

    ListNode cur=dummy;
    while(cur.next!=null && cur.next.next!=null){
      ListNode tmp1=cur.next.next;
      ListNode tmp2=cur.next.next.next;
      cur.next.next=tmp2;  //第一步
      tmp1.next=cur.next;  //第二步
      cur.next=tmp1;     //第三步
      cur=cur.next.next;// cur移动两位，准备下一轮交换
    }
    return dummy.next;

  }
}
