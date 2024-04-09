package leedCode;

public class SplitLink {

    public static void main(String[] args) {
        Node head = new Node(0);
        head.next = new Node(1);
        head.next.next = new Node(3);
        head.next.next.next = new Node(5);

        int x = 3;
        Node splitLink = splitLink(head, x);
        while (splitLink != null) {
            System.out.println(splitLink.data);
            splitLink = splitLink.next;
        }


    }

    /**
     * 用两个链表存储两部分节点
     * @param head
     * @param x
     * @return
     */
    private static Node  splitLink(Node head, int x) {
        Node dumyleft = new Node(-1);
        Node left = dumyleft;
        Node dumyright = new Node(-1);
        Node right = dumyright;
        while (head != null) {
            if (head.data < x) {
                dumyleft.next = head;
                dumyleft = dumyleft.next;
            } else {
                dumyright.next = head;
                dumyright = dumyright.next;
            }
            head = head.next;
        }
        dumyleft.next = right.next;
        return left.next;
    }
}
