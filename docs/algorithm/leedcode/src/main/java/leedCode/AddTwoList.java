package leedCode;

public class AddTwoList {
    public static void main(String[] args) {
        ListNode lef = new ListNode(1);
        lef.next = new ListNode(2);
        ListNode right = new ListNode(3);
        right.next = new ListNode(5);

        ListNode result = addTwoNumbers(lef, right);
        while (result != null) {
            System.out.print(result.val + "->");
            result = result.next;
        }
    }

    public static ListNode addTwoNumbers(ListNode l1, ListNode l2, ListNode prev) {
        ListNode next1 = null;
        ListNode next2 = null;
        int val1 = 0;
        int val2 = 0;
        if (l1 != null) {
            val1 = l1.val;
            next1 = l1.next;
        }
        if (l2 != null) {
            val2 = l2.val;
            next2 = l2.next;
        }
        ListNode newNode = new ListNode(val1 + val2);
        if (prev != null) {
            if (prev.val >= 10) {
                prev.val %= 10;
                newNode.val += 1;
            }
        }
        if (next1 != null || next2 != null) {
            newNode.next = addTwoNumbers(next1, next2, newNode);
        } else if (newNode.val >= 10) {
            newNode.next = addTwoNumbers(next1, next2, newNode);
        }
        return newNode;
    }

    public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        if (l1 != null || l2 != null) {
            dummy.val = ((l1 != null) ? l1.val : 0) + ((l2 != null) ? l2.val : 0);
            if (l1 != null) l1 = l1.next;
            if (l2 != null) l2 = l2.next;
            if(dummy.val >= 10){
                if (l1 != null) l1.val += 1;
                else l1 = new ListNode(1);
                dummy.val %= 10;
            }
            dummy.next = addTwoNumbers(l1, l2);
        }else{
            return null;
        }
        return dummy;
    }


    private static class ListNode {
        private Integer val;
        ListNode next;

        public ListNode(Integer val) {
            this.val = val;
        }
    }
}

