package util;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2021-07-27
 * @Desc
 */
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class BiConsumerDemo {

    // Main method
    public static void main(String[] args)
    {

        // Create a HashMap
        // and add some values
        Map<String, Integer> map
                = new HashMap<>();

        map.put("geeks", 55);
        map.put("for", 13);
        map.put("geeks", 22);
        map.put("is", 11);
        map.put("heaven", 90);
        map.put("for", 100);
        map.put("geekies like us", 96);

        // creating an action
        BiConsumer<String, Integer> action
                = new MyBiConsumer();

        // calling forEach method
        map.forEach(action);
    }
}

// Defining Our Action in MyBiConsumer class
class MyBiConsumer implements BiConsumer<String, Integer> {

    public void accept(String k, Integer v)
    {
        System.out.println("Key: " + k
                + "\tValue: " + v);

        if ("for".equals(k)) {
            System.out.println("Its the "
                    + "highest value\n");
        }
    }
}
