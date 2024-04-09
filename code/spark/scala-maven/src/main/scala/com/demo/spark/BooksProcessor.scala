package com.demo.spark

import com.demo.spark.model.Book
import java.util

import scala.collection.JavaConversions._
/**
 * Copyright (c) 2015 xxx Inc. All Rights Reserved. 
 * Authors: chengxingfu <chengxingfu@xxx.com>
 * Date:2020-06-29 
 */
object BooksProcessor {

  def filterByAuthor(author: String)(implicit books: util.ArrayList[Book]) = {
    books.filter(book => book.getAuthor == author)
  }
}
