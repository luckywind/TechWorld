package com.demo.spark

import com.demo.spark.model.BookStorage

/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved. 
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-06-29 
 */
object ScalaRunner extends App {
  implicit val books = new BookStorage().getBooks
  BooksProcessor.filterByAuthor("Jack London").foreach(b => println(b))

}
