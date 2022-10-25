package leedCode;

public class SwapLink {
    /**
     * 不对啊
     * @param args
     */
    public static void main(String[] args) {
        Node head = new Node(1);
        head.next = new Node(2);
        head.next.next = new Node(3);
        head.next.next.next = new Node(4);

        Node nodes = swapPairs(head);
        while (nodes != null) {
            System.out.println(nodes.data);
            nodes = nodes.next;
        }

    }

    public static Node swapPairs(Node head) {
        Node tmp =new Node(0);  //申请一个空的节点
        tmp.next = head;  //让链表的头节点指向那个空节点的下一个节点

        Node temp = tmp;  //把这个空节点保存下来，用这个变量去完成交换
        while(head != null && head.next !=null){
            temp.next = head.next;
            head.next = temp.next.next;
            temp.next.next = head;
            temp = temp.next.next;  //当上面交换完了后，temp向后移两个节点。
            head = temp.next;
        }
        return tmp.next; //返回空节点之后已经交换完了的链表
    }
}
