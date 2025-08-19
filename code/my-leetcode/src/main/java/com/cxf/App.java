package com.cxf;

import java.util.HashMap;
import java.util.Stack;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
        String str = "[](";
        HashMap<String, String> m = new HashMap<>();
        m.put("(", ")");
        m.put("{", "}");
        m.put("[", "]");
        Stack<String> mystack = new Stack<>();
        for (int i = 0; i < str.length(); i++) {
          char c = str.charAt(i);
          if (!mystack.isEmpty()){
              String peeked = mystack.peek();
              if (m.get(peeked).equals(c + "")){mystack.pop();}
              else {
                  mystack.push(c + "");
              }
          }else {
              mystack.push(c + "");
          }
        }
        if(mystack.isEmpty()){
            System.out.println("Y");
        }else {
            System.out.println("N");
        }


    }
}
