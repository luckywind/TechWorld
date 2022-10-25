package leedCode;

import java.util.ArrayList;

public class delLinkNode {
    public static void main(String[] args) {
        Node head = new Node(1);
        head.next = new Node(2);
        head.next.next = new Node(3);
        head.next.next.next = new Node(4);
        head.next.next.next.next = new Node(5);
        int n =3;//删除倒数第三个节点
        delLink(head, n);

    }

    private static void delLink(Node head, int n) {
        ArrayList<Node> nodes = new ArrayList<>();
        Node newHead = head;
        while (head.next!=null) {
            nodes.add(head);
            head = head.next;
        }
        nodes.add(head);
        nodes.get(nodes.size() - (n+1)).next = nodes.get(nodes.size() - (n-1));

        while (newHead.next != null) {
            System.out.println(newHead.data);
            newHead = newHead.next;
        }
        System.out.println(newHead.data);
    }
}
