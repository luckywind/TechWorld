package Demo;

import java.util.HashMap;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xxx.com>
 * @Date 2021-07-27
 * @Desc
 */
public class PutIFAbsentDemo {
    // Main method
    public static void main(String[] args)
    {

        // create a HashMap and add some values
        HashMap<String, Integer> map
                = new HashMap<>();
        map.put("a", 10000);
        map.put("b", 55000);
        map.put("c", 44300);
        map.put("e", null);

        // print original map
        System.out.println("HashMap:\n "
                + map.toString());

        // put a new value which is not mapped
        // before in map and store the returned
        // value in r1
        Integer r1 = map.putIfAbsent("d", 77633);

        // put a new value for key 'e' which is mapped
        // with a null value, and store the returned
        // value in r2
        Integer r2 = map.putIfAbsent("e", 77633);
        Integer r3 = map.putIfAbsent("b", 3534);

        // print the value of r1
        System.out.println("Value of r1:\n " + r1);

        // print the value of r2
        System.out.println("Value of r2:\n " + r2);
        System.out.println("Value of r3:\n " + r3);

        // print newly mapped map
        System.out.println("New HashMap:\n "
                + map);
    }
}
