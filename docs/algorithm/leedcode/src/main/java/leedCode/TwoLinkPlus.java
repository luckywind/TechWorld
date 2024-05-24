package leedCode;


public class TwoLinkPlus {


    public static void main(String[] args) {
        Node left = new Node(1);
        left.next = new Node(2);
        Node right = new Node(2);
        right.next = new Node(5);
        Node result = sumList(left, right);
        while (result != null) {
            System.out.print(result.data + "->");
            result = result.next;

        }
    }
    public static Node sumList(Node left, Node right) {
        Node dummyHead = new Node(-1);
        Node current = dummyHead;
        while (left != null || right != null) {
            int a = left == null ? left.data : 0;
            int b = right == null ? right.data : 0;
            int c = (a + b) / 10;
            current.next= new Node((a + b) % 10);
            left = left.next;
            right = right.next;
            current = current.next;
        }
        return dummyHead.next;
    }

    static class Node{
        Integer data;
        Node next;

        public Node(Integer data) {
            this.data = data;
        }
    }

}
