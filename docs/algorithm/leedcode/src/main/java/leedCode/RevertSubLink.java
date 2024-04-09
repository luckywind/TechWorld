package leedCode;

import java.util.Stack;

public class RevertSubLink {
    public static void main(String[] args) {
        Node head = new Node(0);
        head.next = new Node(1);
        head.next.next = new Node(3);
        head.next.next.next = new Node(5);
        head.next.next.next.next = new Node(6);

        int m = 2, n = 4;
        Node revert = reverseBetween(head, m, n);
        while (revert != null) {
            System.out.println(revert.data);
            revert = revert.next;
        }


    }


    /**
     * 使用stack
     *
     * Runtime : 1ms
     *
     * beats 2.94% of java submissions
     *
     * @param head
     * @param m
     * @param n
     * @return
     */
    public static Node reverseBetween(Node head, int m, int n) {
        if (head == null || head.next == null || m >= n) {
            return head;
        }
        Node dummyNode = new Node(0);
        dummyNode.next = head;
        head = dummyNode;
        Node preNode = head; // pre node
        for (int i = 1; i < m; i++) {
            preNode = preNode.next;
        }
        Node tempNode = preNode.next;
        Stack<Node> stack = new Stack<>();
        int i = 0;
        while (m + i <= n) {
            stack.push(tempNode);
            tempNode = tempNode.next;
            i++;
        }
        Node postNode = tempNode; // post node
        Node resultNode = stack.pop();
        tempNode = resultNode;
        while (!stack.isEmpty()) {
            tempNode.next = stack.pop();
            tempNode = tempNode.next;
        }
        preNode.next = resultNode;
        tempNode.next = postNode;
        return dummyNode.next;
    }
}
