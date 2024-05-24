package leedCode;

public class MergeLink {
    public static void main(String[] args) {
        Node left = new Node(1);
        left.next = new Node(2);
        left.next.next = new Node(4);
        Node right = new Node(1);
        right.next = new Node(3);

//        Node newHead = mergeLink(left, right);
        Node newHead = mergeTwoLink(left, right);
        while (newHead != null) {
            System.out.println(newHead);
            newHead = newHead.next;
        }

    }

    private static Node mergeLink(Node left, Node right) {
        Node head=new Node(-1);
//        head = left.data <= right.data ? left : right;
        Node newHead=head;
        Node current;
        while (left != null && right != null) {
            if (left.data <= right.data) {
                current = left;
                left = left.next;
            } else {
                current = right;
                right = right.next;
            }
            head.next = current;
            head = head.next;
        }
        head.next = left == null ? right : left; //把没比较完的尾巴续上
        return newHead.next;
    }

    private static Node mergeTwoLink(Node left, Node right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        Node head;
        if (left.data <= right.data) {
            head = left;
            head.next = mergeTwoLink(left.next, right);
        } else {
            head = right;
            head.next = mergeTwoLink(left, right.next);
        }
        return head;
    }


}
