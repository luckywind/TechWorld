package leedCode;

import java.util.HashMap;

public class ThreePlus0 {
    public static void main(String[] args) {
        int[] nums = {0, 2, 1, 3, -2, -5};
        HashMap<Integer, Integer> numMapIdx = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            numMapIdx.put(nums[i], i);
        }
        for (int i = 0; i < nums.length-1; i++) {
            for (int j = i+1; j < nums.length; j++) {
                int tmp = nums[i] + nums[j];
                if (numMapIdx.containsKey(-tmp)) {
                    System.out.println(nums[i]);
                    System.out.println(nums[j]);
                    System.out.println(-tmp);
                }
                break;
            }
            break;
        }
    }
}
