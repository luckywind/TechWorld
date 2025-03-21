package com.cxf.test;
import java.util.Scanner;
import java.util.Stack;
public class HJ050FourArith {

  public static void main(String[] args) {
// 输入
    Scanner in = new Scanner(System.in);
    String str = in.nextLine();
// 中括号、大括号全部转为小括号，方便处理
    str = str.replace('[', '(');
    str = str.replace('{', '(');
    str = str.replace(']', ')');
    str = str.replace('}', ')');

// 为了统一计算逻辑，在最外面的表达式也放括号
//    if (str.charAt(0) != '(') {
//      str = '(' + str + ')';
//    }
// 输出
    System.out.println(solve(str));
// 关闭
    in.close();
  }

  private static int solve(String str) {
    char[] charArray = str.toCharArray();
    int length = charArray.length;
// 用于存放数字。
    Stack<Integer> stack = new Stack<>();
// 纪录数字
    int number = 0;
// 纪录上个操作符
    char opr = '+';
    for (int i = 0; i < length; i++) {
      char ch = charArray[i];
      // 一直入栈
      // 遇到右括号就出栈，直到左括号出现为止
      // 括号内包裹的表达式进行计算
      // 1. 如果当前字符是小括号
      if (ch == '(') {
        // 移到小括号后一位字符
        int j = i + 1;
        // 统计括号的数量
        int count = 1;
        while (count > 0) {
          // 遇到右括号，括号数-1
          if (charArray[j] == ')') {
            count--;
          }
          // 遇到左括号，括号数+1
          if (charArray[j] == '(') {
            count++;
          }
          j++;
        }
        // 递归，剥掉小括号：解小括号中的表达式
        number = solve(str.substring(i + 1, j - 1));
        i = j - 1;
      } else if (Character.isDigit(ch)) {  //2. 如果当前字符是数字
        // 多位数字的处理，ch-'0'是转为整数
        number = number * 10 + ch - '0';
      }

       //经过上面的处理，已经没有括号了，括号内的表达式已经换成整数了，接下来遇到了运算符，这个整数要怎么处理？
       //3. 如果非括号且非数字，即运算符，或者最后一个字符，要开始真正的运算了， 数字入栈，栈内的元素最后统一加起来，所以遇到加减法先不计算，因为它们的优先级低，-也可换成+相反数，所以也入栈
      if (!Character.isDigit(ch) || i == length - 1) {
        // 遇到符号，将数字处理后放进栈
        // 如果上次的符号是'+',直接入栈
        if (opr == '+') {
          stack.push(number);
        }
        // 如果是'-',数字取相反数在入栈
        else if (opr == '-') {
          stack.push(-1 * number);
        }
        // 如果是'*',弹出一个数字乘后放入栈
        else if (opr == '*') {
          stack.push(stack.pop() * number);
        }
        // 如果是'/',弹出一个数字/后放入栈
        else if (opr == '/') {
          stack.push(stack.pop() / number);
        }
        // 更新符号
        opr = ch;
        // 刷新数字
        number = 0;
      }
    }
// 栈中数字求和得到结果
    int sum = 0;
    while (!stack.isEmpty()) {
      sum += stack.pop();
    }
    return sum;
  }

}



