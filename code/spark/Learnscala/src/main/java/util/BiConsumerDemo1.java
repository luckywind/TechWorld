package util;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2021-07-27
 * @Desc
 */
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class BiConsumerDemo1{
   public static Map<String, String> map
            = new HashMap<>();
    // Main method
    public static void main(String[] args)
    {


        // Create a HashMap
        // and add some values

        map.put("geeks", null);
        map.put("noupdate", "老3");
        map.put("for", "老for值");


        Map<String, String> map2
                = new HashMap<>();

        map2.put("geeks", "null值更新");// null值直接更新
        map2.put("noupdate", "新1");// 值非空并不更新
        map2.put("for", null); // 空值不处理
        map2.put("other", "2新增");// 新增复制
        // creating an action
        BiConsumer<String, String> action
                = new MyBiConsumer1();

        // calling forEach method
        map2.forEach(action);
        Set<String> keyset = map.keySet();
        for (String s : keyset) {
            System.out.println("key:"+s+", val:"+map.get(s));
        }
    }
}

// Defining Our Action in MyBiConsumer class
class MyBiConsumer1 implements BiConsumer<String, String> {

    public void accept(String k, String v)
    {
        System.out.println("Key: " + k
                + "\tValue: " + v);

        if (v != null && StringUtils.isNotBlank(v)) {
            BiConsumerDemo1.map.putIfAbsent(k, v);
        }
    }
}
