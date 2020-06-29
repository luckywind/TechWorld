package com.demo.spark;

import com.demo.spark.model.BookStorage;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xiaomi.com>
 * Date:2020-06-29
 */
public class Runner {
    public static void main(String[] args) {
        BookStorage storage = new BookStorage();
        storage.getBooks().stream().forEach(System.out::println);
    }
}
