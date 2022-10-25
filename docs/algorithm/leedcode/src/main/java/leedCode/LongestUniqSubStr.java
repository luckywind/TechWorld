package leedCode;

import sun.security.util.Length;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LongestUniqSubStr {
    public static void main(String[] args) {
        String str = "sdgdryter4";
        int res = getLongUnigSubstr(str);
        System.out.println(res);
    }

    private static int getLongUnigSubstr(String str) {
        int res=1;
        for (int i = 0; i < str.length(); i++) {
            for (int j = i ; j <=str.length(); j++) {
                String subs = str.substring(i, j);
                HashSet charset = new HashSet<>(Arrays.asList(subs.split("")));
                if ( charset.size()== subs.length())  {  //uniq
                    if (subs.length() > res) {
                        res = subs.length();
                    }
                }
            }
        }
        return res;
    }
}
