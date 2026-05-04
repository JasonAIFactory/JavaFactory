/*
 * Drill 2 — StringBuilder
 *
 * 🎯 Goal:
 *   Build a single string "1,2,3,4,5" from int[] {1,2,3,4,5} using StringBuilder.
 *   No String + in a loop allowed.
 *
 * 🗣️ Say 10 times:
 *   "StringBuilder is faster than String plus in a loop.
 *    Because String is immutable, every plus makes a new object.
 *    StringBuilder reuses one buffer.
 *    For multi-thread, use StringBuffer because it is synchronized."
 *   [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ] [ ]
 *
 * Follow-up:
 *   Q: "Why is String immutable?"
 *   A: "For security, caching, and thread-safety. Also for hashcode caching."
 *
 *   Q: "When do you use StringBuffer?"
 *   A: "Only when many threads append to the same string. Rare in modern code."
 */
public class Drill02_StringBuilder {
    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4, 5};

        // TODO: build "1,2,3,4,5" with StringBuilder


        // expected output: 1,2,3,4,5
    }
}
