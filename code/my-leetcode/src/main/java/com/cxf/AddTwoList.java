package com.cxf;

import java.util.PriorityQueue;
import sun.lwawt.PlatformEventNotifier;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2021-08-01
 * @Desc
 */
public class AddTwoList {
   public static class ListNode {

     int val;
     ListNode next;

     ListNode() {
     }

     ListNode(int val) {
       this.val = val;
     }

     ListNode(int val, ListNode next) {
       this.val = val;
       this.next = next;
     }
   }

  public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
      //同时遍历两个链表，直到两个值都是空
    //需要定义两个节点：l1拿出的m,l2拿出的n
    //考虑到进位，还需一个值x
    ListNode m=l1, n=l2;
    int x=0;
    ListNode head = new ListNode(-1); //虚拟的头节点
    ListNode cur=head;
    while (m != null || n != null) {
      int sum;
      int mval;
      int nval;
      mval = (m != null) ? m.val : 0;
      nval = (n != null) ? n.val : 0;
      if (x >0) {
        sum = mval+nval  + 1;
      } else {
        sum = mval+nval;
      }
      int resBit;
      if (sum >= 10) {
        resBit = sum % 10;
        x = 1;
      } else {
        resBit = sum;
        x = 0; //注意，要归位！
      }
      ListNode sumBitNode = new ListNode(resBit);
      cur.next = sumBitNode;

      m = (m!=null)?m.next:null;
      n = (n!=null)?n.next:null;
      cur = sumBitNode;
    }

    //最后如果两个链表都结束了，但是进了一位，即x=1
    if (x > 0) {
      cur.next = new ListNode(1);
    }

    return head.next;
  }

  public static void main(String[] args) {
    ListNode n2 = new ListNode(2);
    ListNode n4 = new ListNode(4);
    ListNode n3 = new ListNode(3);
    ListNode n5 = new ListNode(5);
    ListNode n6 = new ListNode(6);
    ListNode n41 = new ListNode(4);

    n2.next = n4;
    n4.next = n3;
    ListNode l1 = n2;
    n5.next = n6;
    n6.next = n41;
    ListNode l2 = n5;
    AddTwoList addTwoList = new AddTwoList();
    ListNode listNode = addTwoList.addTwoNumbers(l1, l2);
    System.out.println(listNode);


  }

}
