/*
 * Problem 3 — SOLUTION
 *
 * Validation logic in a private isValid() helper.
 * Clean to test, easy to extend when business rules change.
 */

import java.util.*;

public class Problem03_ValidateOrders_Solution {

    static class Order {
        String productId;
        int quantity;
        double price;
        Order(String p, int q, double pr) { productId = p; quantity = q; price = pr; }
    }

    static class ValidationResult {
        List<Order> valid = new ArrayList<>();
        List<Order> invalid = new ArrayList<>();
    }

    public ValidationResult validate(List<Order> orders) {
        ValidationResult result = new ValidationResult();
        for (Order o : orders) {
            if (isValid(o)) result.valid.add(o);
            else            result.invalid.add(o);
        }
        return result;
    }

    private boolean isValid(Order o) {
        if (o.productId == null || o.productId.isEmpty()) return false;
        if (o.quantity <= 0) return false;
        if (o.price < 0) return false;
        return true;
    }

    public static void main(String[] args) {
        Problem03_ValidateOrders_Solution p = new Problem03_ValidateOrders_Solution();

        List<Order> input = Arrays.asList(
            new Order("P-001", 5, 10.0),
            new Order("P-002", 0, 20.0),
            new Order(null,    3, 15.0),
            new Order("",      2, 8.0),
            new Order("P-003", 1, -5.0),
            new Order("P-004", 10, 0.0)
        );

        ValidationResult r = p.validate(input);
        check("valid count", 2, r.valid.size());
        check("invalid count", 4, r.invalid.size());

        ValidationResult empty = p.validate(new ArrayList<>());
        check("empty valid", 0, empty.valid.size());
        check("empty invalid", 0, empty.invalid.size());
    }

    static void check(String label, int expected, int actual) {
        System.out.println((actual == expected ? "PASS" : "FAIL")
            + " | " + label + " expected=" + expected + " actual=" + actual);
    }
}
