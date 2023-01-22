package com.leeco;

import java.util.Stack;

/**
 * Copyright (c) 2015 XiaoMi Inc. All Rights Reserved.
 *
 * @author chengxingfu <chengxingfu@xiaomi.com>
 * @Date 2023-01-03
 * @Desc
 */
public class Test {



  public static void main(String[] args) {

    System.out.println(isValid("({[)"));
  }


  public static  boolean isValid(String s) {
    char[] ch=s.toCharArray();
    Stack<Character> stack=new Stack();
    for(int i=0;i<ch.length;i++){
      //先检查栈顶元素与待插入元素是否匹配，如果匹配，则把栈顶和待插入元素丢掉；否则入栈


      if(!stack.empty()){
        char t=stack.peek();
        if(t=='(' && ch[i]==')' ||
            t=='[' && ch[i]==']' ||
            t=='{' && ch[i]=='}' )
        {stack.pop();}
        else{
          stack.push(ch[i]);
        }

      }else{
        stack.push(ch[i]);
      }

    }

    //遍历完成后，检查栈是否为空
    return stack.empty();

  }

}
