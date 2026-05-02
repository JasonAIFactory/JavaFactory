/*
 * Problem 3 — Validate + Detect Invalid (Enterprise pattern)
 *
 * Given a list of Orders, separate them into valid and invalid lists
 * based on these rules:
 *   - quantity > 0
 *   - productId is not null and not empty
 *   - price >= 0
 *
 * Why this matters: input validation is a daily task in any ERP system.
 *
 * Hints:
 *   - Extract the rule into a private isValid() method
 *   - Easier to test, easier to extend when business rules change
 */

import java.util.*;

public class Problem03_ValidateOrders {

    static class Order {
        String productId;
        int quantity;
        double price;
        Order(String p, int q, double pr) { productId = p; quantity = q; price = pr; }
        public String toString() {
            return "Order[" + productId + ", qty=" + quantity + ", price=" + price + "]";
        }
    }

    static class ValidationResult {
        List<Order> valid = new ArrayList<>();
        List<Order> invalid = new ArrayList<>();
    }

    public ValidationResult validate(List<Order> orders) {
        // TODO: implement
        return new ValidationResult();
    }

    public static void main(String[] args) {
        Problem03_ValidateOrders p = new Problem03_ValidateOrders();

        List<Order> input = Arrays.asList(
            new Order("P-001", 5, 10.0),     // valid
            new Order("P-002", 0, 20.0),     // invalid: qty 0
            new Order(null,    3, 15.0),     // invalid: null id
            new Order("",      2, 8.0),      // invalid: empty id
            new Order("P-003", 1, -5.0),     // invalid: negative price
            new Order("P-004", 10, 0.0)      // valid (price 0 OK)
        );

        ValidationResult r = p.validate(input);

        check("valid count", 2, r.valid.size());
        check("invalid count", 4, r.invalid.size());

        // Empty input
        ValidationResult empty = p.validate(new ArrayList<>());
        check("empty valid", 0, empty.valid.size());
        check("empty invalid", 0, empty.invalid.size());
    }

    static void check(String label, int expected, int actual) {
        String status = (actual == expected) ? "PASS" : "FAIL";
        System.out.println(status + " | " + label + " expected=" + expected + " actual=" + actual);
    }
}
