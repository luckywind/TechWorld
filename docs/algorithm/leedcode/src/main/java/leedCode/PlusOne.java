package leedCode;

import java.util.Stack;

/**
 * 给定一个由**整数**组成的**非空**数组所表示的非负整数，在该数的基础上加一。
 * 最高位数字存放在数组的首位， 数组中每个元素只存储一个数字。
 * 你可以假设除了整数 0 之外，这个整数不会以零开头。
 */
public class PlusOne {
    public static void main(String[] args) {
        int[] arr = {1,8, 9};

        int[] res = plusOne(arr);
        for (int j = 0; j < res.length; j++) {
            System.out.println(res[j]);
        }

    }

    private static int[] plusOne(int[] arr) {
        Stack<Integer> nums = new Stack<>();
        int tmp=1;
        for (int i = arr.length-1; i >= 0; i--) {
            if (arr[i] +tmp == 10) {
                nums.push(0);
                tmp = 1;
            } else {
                nums.push(arr[i] + tmp);
                tmp = 0;
            }
        }
        if (tmp==1) nums.push(tmp);
        int[] res = new int[nums.size()];
        int i = 0;
        while (!nums.isEmpty()) {
            res[i++] = nums.pop();
        }
        return res;
    }
}
