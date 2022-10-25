package leedCode;

/**
 * 给定一个排序数组，你需要在**原地**删除重复出现的元素，使得每个元素只出现一次，返回移除后数组的新长度。
 *
 * 不要使用额外的数组空间，你必须在**原地修改输入数组**并在使用 O(1) 额外空间的条件下完成。
 */
public class Del_duplicate_orderArray {
    public static void main(String[] args) {
        int[] arr = {2, 2, 2, 2, 4};
        System.out.println( deldup(arr));
    }

    private static int deldup(int[] arr) {
        if (arr.length == 0) {
            return 0;
        }
        int len = 1;
        for (int i = 0, j = 1; i <= j && j < arr.length-1; i++) {
            while (arr[i] == arr[j]) {
                j++;
            }
            arr[i+1] = arr[j];
            len++;
        }
        return len;
    }
}
