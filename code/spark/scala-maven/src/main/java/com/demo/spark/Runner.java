package com.demo.spark;

import com.demo.spark.model.BookStorage;

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved.
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-06-29
 */
public class Runner {
    public static void main(String[] args) {
        BookStorage storage = new BookStorage();
        storage.getBooks().stream().forEach(System.out::println);
    }
}
