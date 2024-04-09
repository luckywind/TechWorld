package leedCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class PlusNums {
    public static void main(String[] args) {
        Vector<Integer> nums = new  Vector<Integer>();//new Vector<Integer>();
        nums.add(1);
        nums.add(3);
        nums.add(5);
        Vector<Integer> integers = twoSum(nums, 8);
        for (int i = 0; i < integers.size(); i++) {
            Integer integer = integers.get(i);
            System.out.println(integer);

        }

    }

    public static  Vector<Integer> twoSum(Vector<Integer> nums, int target) {
        Vector<Integer> result = new Vector<Integer>();
        HashMap<Integer, Integer> record = new HashMap<Integer, Integer>();
        for (int i = 0; i < nums.size(); i++) {
            int gap = target - nums.get(i);
            if (record.containsValue(gap)) {
                result.add(i);
                for (Map.Entry<Integer, Integer> entry : record.entrySet()) {
                    if (entry.getValue() == gap) {
                        result.add(entry.getKey());
                        break;
                    }
                }
                break;
            } else {
                record.put(i, nums.get(i));
            }
        }
        return result;
    }
}
