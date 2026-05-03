import java.util.*;

public class Basic06_ArrayList_Solution {
    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("apple");
        list.add("banana");
        list.add("cherry");

        System.out.println(list.size());    // 3

        for (String s : list) {
            System.out.println(s);
        }

        System.out.println(list.get(1));    // banana
    }
}
