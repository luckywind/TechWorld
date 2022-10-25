package leedCode;

import java.util.HashMap;
import java.util.Stack;

public class ValidBrace {
    public static void main(String[] args) {
        String str = "([()])";
        HashMap<String, String> braceMap = new HashMap<>();
        braceMap.put("(", ")");
        braceMap.put("[", "]");
        braceMap.put("{", "}");
        Stack<String> braces = new Stack<>();
        String[] braces_str = str.split("");
        for (int i = 0; i < braces_str.length; i++) {
//            braces.push(braces_str[i]);
            if (!braces.isEmpty()) {
                String left = braces.peek();
                if (braceMap.get(left).equals(braces_str[i])) {
                    braces.pop();
                } else {
                    braces.push(braces_str[i]);
                }
            } else {
                braces.push(braces_str[i]);
            }
        }
        System.out.println(braces.isEmpty());
    }
}
