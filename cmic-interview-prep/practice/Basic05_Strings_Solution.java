public class Basic05_Strings_Solution {
    public static void main(String[] args) {
        String s = "INV-001-2026";

        System.out.println(s.length());                              // 12
        System.out.println(s.toUpperCase());                         // INV-001-2026
        System.out.println(s.startsWith("INV-"));                    // true
        System.out.println(s.substring(s.indexOf('-') + 1));         // 001-2026
    }
}
