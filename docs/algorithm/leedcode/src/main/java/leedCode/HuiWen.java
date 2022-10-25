package leedCode;

public class HuiWen {
    public static void main(String[] args) {
        System.out.println(isHuiWen("abba"));
    }

    public static boolean isHuiWen(String str) {
        if (str == null) {
            return false;
        }
        String[] chs = str.split("");
        for (int i = 0; i < chs.length / 2; i++) {
            if (!chs[i].equalsIgnoreCase(chs[chs.length - 1 - i])) {
                return false;
            }
        }
        return true;
    }
}
