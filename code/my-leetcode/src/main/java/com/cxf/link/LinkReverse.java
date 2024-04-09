package com.cxf.link;

/**
 * Copyright (c) 2015 xx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xx.com>
 * @Date 2023-01-19
 * @Desc
 */
public class LinkReverse {

  public static void main(String[] args) {
    ListNode head = new ListNode(1);
    head.next = new ListNode(2, new ListNode(3, new ListNode(4)));
    LinkReverse obj = new LinkReverse();
    ListNode reversed = obj.reverse(head);
    while (reversed != null) {
      System.out.println(reversed.val);
      reversed = reversed.next;
    }
  }

  ListNode reverse(ListNode head) {
    //边界条件
    if (head == null || head.next == null) {
      return head;
    }
    //递归反转
    ListNode last = reverse(head.next);
//    last.next=head;
    head.next.next = head;
    head.next=null;
    return last;
  }

}
