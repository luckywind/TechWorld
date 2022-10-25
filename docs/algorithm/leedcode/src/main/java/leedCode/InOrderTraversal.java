package leedCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 中序遍历的顺序为**左-根-右**，具体算法为：
 *
 * - 从根节点开始，先将根节点压入栈
 * - 然后再将其所有左子结点压入栈，取出栈顶节点，保存节点值
 * - 再将当前指针移到其右子节点上，若存在右子节点，则在下次循环时又可将其所有左子结点压入栈中
 */
public class InOrderTraversal {
    public static void main(String[] args) {

    }


    public List<Integer> inorderTraversal(TreeNode root) {
        List<Integer> list = new ArrayList<>();
        Stack<TreeNode> stack = new Stack<>();
        TreeNode cur = root;
        while (cur != null || !stack.isEmpty()) {
            if (cur != null) {
                stack.push(cur);
                cur = cur.left;
            } else {
                cur = stack.pop();
                list.add(cur.val);
                cur = cur.right;
            }
        }
        return list;
    }
}
