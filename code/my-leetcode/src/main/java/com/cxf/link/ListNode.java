package com.cxf.link;

/**
 * Copyright (c) 2015 xx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xx.com>
 * @Date 2023-01-14
 * @Desc
 */
public class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }

    @Override
    public String toString() {
        return String.valueOf(val);
    }
}
