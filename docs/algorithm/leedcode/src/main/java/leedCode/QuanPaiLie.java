package leedCode;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * 字符串的全排列，具体内容如下
 * 输入一个字符串,按字典序打印出该字符串中字符的所有排列。例如输入字符串abc,则打印出由字符a,b,c所能排列出来的所有字符串abc,acb,bac,bca,cab和cba。 结果请按字母顺序输出。
 * 采用递归的思想：
 * 把需要全排列的字符串分为两部分看待：
 * （1）字符串的第一个字符；
 * （2）第一个字符后面的所有字符；
 * 求所有可能出现在第一个位置的字符；将第一个字符和后面的字符一次交换；
 * 固定第一个字符，对第一个字符后面的所有字符求全排列。第一个字符后面的所有字符又可以分为两部分；
 */
public class QuanPaiLie {
    public static void main(String[] args) {
        ArrayList<String> res = Permutation("abc");
        for (int i = 0; i < res.size(); i++) {
            String s = res.get(i);
            System.out.println(s);
        }
    }
    public static ArrayList<String> Permutation(String str) {
        ArrayList<String> res = new ArrayList<String>();
        if(str==null||str.length()==0)
        {
            return res;
        }
        char[] charArray = str.toCharArray();
        //输出按照输入字典顺序
        TreeSet<String> tempRes = new TreeSet<String>();
        PermutationCore(charArray,tempRes,0);
        res.addAll(tempRes);
        return res;

    }
    private static void  PermutationCore( char[] charArray,TreeSet<String> tempRes,int loc)
    {
        if(charArray==null || charArray.length==0 || loc<0 || loc>charArray.length-1)
        {
            return ;
        }
        if(loc==charArray.length-1)
        {
            tempRes.add(String.valueOf(charArray));//递归的出口
        }
        else
        {
            for(int i=loc;i<charArray.length;i++)
            {
                swap(charArray,i,loc);//将第一个字符与后面的字符交换
                PermutationCore(charArray,tempRes,loc+1);//对后面所有的字符进行全排列
                swap(charArray,i,loc);//再将之前交换的字符交换回来，以便第一个字符再与其他字符交换
            }

        }
    }
    private static void swap(char[] charArray,int i,int j)
    {
        char temp = charArray[i];
        charArray[i] = charArray[j];
        charArray[j] = temp;
    }
}
