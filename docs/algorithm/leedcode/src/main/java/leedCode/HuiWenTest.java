package leedCode;

public class HuiWenTest {
    public static void main(String[] args) {
        System.out.println(isPalindrome(1000));
        System.out.println(isPalindrome(1001));
    }
    public static boolean isPalindrome(int x) {
        //边界判断
        if (x < 0) return false;
        int div = 1;
        //
        while (x / div >= 10) div *= 10;//找到用于获取第一位的除数 高手！
        while (x > 0) {
            int left = x / div;
            int right = x % 10;
            if (left != right) return false;
            x = (x % div) / 10; //去掉首尾两位
            div /= 100;   //除数变小
        }
        return true;
    }
}
