package org.example.util;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

/**
 * Centralized input validation utility used by DAOs and UI layers.
 * <p>
 * All methods throw {@link IllegalArgumentException} when validation fails.
 */
public final class Validator {

    private static final List<String> PAYMENT_STATUSES = Arrays.asList("PAID", "PENDING", "FAILED");
    private static final List<String> ORDER_STATUSES =
            Arrays.asList("PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");

    private Validator() {
    }

    /**
     * Validates that the given value is not {@code null}.
     *
     * @param value     value to validate
     * @param fieldName logical field name for error messages
     */
    public static void notNull(Object value, String fieldName) {
        if (value == null) throw new IllegalArgumentException(fieldName + " cannot be null");
    }

    /**
     * Validates that the given string is not {@code null} and not blank.
     *
     * @param value     string to validate
     * @param fieldName logical field name for error messages
     */
    public static void notEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
    }

    /**
     * Validates that the given integer is strictly positive.
     *
     * @param value     integer to validate
     * @param fieldName logical field name for error messages
     */
    public static void positiveInt(int value, String fieldName) {
        if (value <= 0) throw new IllegalArgumentException(fieldName + " must be positive");
    }

    /**
     * Validates that the given integer is zero or positive.
     *
     * @param value     integer to validate
     * @param fieldName logical field name for error messages
     */
    public static void nonNegativeInt(int value, String fieldName) {
        if (value < 0) throw new IllegalArgumentException(fieldName + " cannot be negative");
    }

    /**
     * Validates that the given double is strictly positive.
     *
     * @param value     number to validate
     * @param fieldName logical field name for error messages
     */
    public static void positiveDouble(double value, String fieldName) {
        if (value <= 0) throw new IllegalArgumentException(fieldName + " must be positive");
    }

    /**
     * Validates that the given double is zero or positive.
     *
     * @param value     number to validate
     * @param fieldName logical field name for error messages
     */
    public static void nonNegativeDouble(double value, String fieldName) {
        if (value < 0) throw new IllegalArgumentException(fieldName + " cannot be negative");
    }

    /**
     * Validates that the given date is not {@code null}.
     *
     * @param date      date to validate
     * @param fieldName logical field name for error messages
     */
    public static void validDate(Date date, String fieldName) {
        notNull(date, fieldName);
    }

    /**
     * Validates client fields.
     *
     * @param clientId client identifier (positive)
     * @param name     client name (non-empty)
     * @param email    client email (contains '@')
     * @param phone    client phone number (7-15 digits, optional leading '+')
     */
    public static void validateClient(int clientId, String name, String email, String phone) {
        positiveInt(clientId, "Client ID");
        notEmpty(name, "Client name");
        notEmpty(email, "Client email");
        if (!email.contains("@")) throw new IllegalArgumentException("Invalid email format");
        notEmpty(phone, "Client phone");
        if (!phone.matches("\\+?\\d{7,15}")) {
            throw new IllegalArgumentException("Invalid phone format");
        }
    }

    /**
     * Validates employee fields.
     *
     * @param employeeId employee identifier (positive)
     * @param name       employee name (non-empty)
     * @param position   employee position (non-empty)
     */
    public static void validateEmployee(int employeeId, String name, String position) {
        positiveInt(employeeId, "Employee ID");
        notEmpty(name, "Employee name");
        notEmpty(position, "Position");
    }

    /**
     * Validates computer product fields.
     */
    public static void validateComputer(int productId, String name, String description, double price,
                                        int quantity, int categoryId, String cpu, String ram, String storage, String gpu) {
        positiveInt(productId, "Product ID");
        notEmpty(name, "Product name");
        notEmpty(description, "Product description");
        positiveDouble(price, "Product price");
        nonNegativeInt(quantity, "Quantity");
        positiveInt(categoryId, "Category ID");
        notEmpty(cpu, "CPU");
        notEmpty(ram, "RAM");
        notEmpty(storage, "Storage");
        notEmpty(gpu, "GPU");
    }

    /**
     * Validates accessory product fields.
     */
    public static void validateAccessory(int productId, String name, String description, double price,
                                         int quantity, int categoryId, String accessoryType, String compatibility) {
        positiveInt(productId, "Product ID");
        notEmpty(name, "Accessory name");
        notEmpty(description, "Accessory description");
        positiveDouble(price, "Accessory price");
        nonNegativeInt(quantity, "Quantity");
        positiveInt(categoryId, "Category ID");
        notEmpty(accessoryType, "Accessory type");
        notEmpty(compatibility, "Compatibility");
    }

    /**
     * Validates promotion fields.
     *
     * @param promotionId     promotion identifier (positive)
     * @param name            promotion name (non-empty)
     * @param discountPercent discount percent in range [0..100]
     * @param startDate       start date (non-null)
     * @param endDate         end date (non-null, not before start date)
     */
    public static void validatePromotion(int promotionId, String name, double discountPercent, Date startDate, Date endDate) {
        positiveInt(promotionId, "Promotion ID");
        notEmpty(name, "Promotion name");
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("Discount percent must be between 0 and 100");
        }
        validDate(startDate, "Start date");
        validDate(endDate, "End date");
        if (endDate.before(startDate)) throw new IllegalArgumentException("End date cannot be before start date");
    }

    /**
     * Validates review fields.
     */
    public static void validateReview(int reviewId, int clientId, int productId, int rating, String text, Date reviewDate) {
        positiveInt(reviewId, "Review ID");
        positiveInt(clientId, "Client ID");
        positiveInt(productId, "Product ID");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be between 1 and 5");
        notEmpty(text, "Review text");
        validDate(reviewDate, "Review date");
    }

    /**
     * Validates product-promotion link fields.
     */
    public static void validateProductPromotion(int id, int productId, int promotionId) {
        positiveInt(id, "ProductPromotion ID");
        positiveInt(productId, "Product ID");
        positiveInt(promotionId, "Promotion ID");
    }

    /**
     * Validates payment fields.
     *
     * @param paymentId payment identifier (positive)
     * @param amount    payment amount (non-negative)
     * @param payDate   payment date (non-null)
     * @param status    payment status (PAID, PENDING, FAILED)
     * @param orderId   order identifier (positive)
     */
    public static void validatePayment(int paymentId, double amount, Date payDate, String status, int orderId) {
        positiveInt(paymentId, "Payment ID");
        nonNegativeDouble(amount, "Payment amount");
        validDate(payDate, "Payment date");
        notEmpty(status, "Payment status");
        if (!PAYMENT_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Invalid payment status: " + status);
        }
        positiveInt(orderId, "Order ID");
    }

    /**
     * Validates delivery fields.
     */
    public static void validateDelivery(int deliveryId, String courier, String trackingNumber, String deliveryAddress, int orderId) {
        positiveInt(deliveryId, "Delivery ID");
        notEmpty(courier, "Courier");
        notEmpty(trackingNumber, "Tracking number");
        notEmpty(deliveryAddress, "Delivery address");
        positiveInt(orderId, "Order ID");
    }

    /**
     * Validates order fields for creation (without requiring payment/delivery IDs).
     *
     * @param orderId    order identifier (positive)
     * @param orderDate  order date (non-null)
     * @param status     order status (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
     * @param clientId   client identifier (positive)
     * @param employeeId employee identifier (positive)
     */
    public static void validateOrderCreate(int orderId, Date orderDate, String status, int clientId, int employeeId) {
        positiveInt(orderId, "Order ID");
        validDate(orderDate, "Order date");
        notEmpty(status, "Order status");
        positiveInt(clientId, "Client ID");
        positiveInt(employeeId, "Employee ID");
    }

    /**
     * Validates order fields for update, allowing optional payment/delivery IDs.
     *
     * @param orderId    order identifier (positive)
     * @param orderDate  order date (non-null)
     * @param status     order status (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
     * @param clientId   client identifier (positive)
     * @param employeeId employee identifier (positive)
     * @param paymentId  payment identifier (optional; if not null must be positive)
     * @param deliveryId delivery identifier (optional; if not null must be positive)
     */
    public static void validateOrderUpdate(int orderId, Date orderDate, String status, int clientId, int employeeId,
                                           Integer paymentId, Integer deliveryId) {
        validateOrderCreate(orderId, orderDate, status, clientId, employeeId);
        if (paymentId != null) positiveInt(paymentId, "Payment ID");
        if (deliveryId != null) positiveInt(deliveryId, "Delivery ID");
    }

    /**
     * Validates order fields (legacy/full validation requiring payment and delivery IDs).
     *
     * @param orderId    order identifier (positive)
     * @param orderDate  order date (non-null)
     * @param status     order status (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
     * @param clientId   client identifier (positive)
     * @param employeeId employee identifier (positive)
     * @param paymentId  payment identifier (positive)
     * @param deliveryId delivery identifier (positive)
     */
    public static void validateOrder(int orderId, Date orderDate, String status, int clientId, int employeeId,
                                     int paymentId, int deliveryId) {
        validateOrderCreate(orderId, orderDate, status, clientId, employeeId);
        positiveInt(paymentId, "Payment ID");
        positiveInt(deliveryId, "Delivery ID");
    }

    /**
     * Validates category identifier.
     *
     * @param categoryId category identifier (positive)
     * @param fieldName  logical field name for error messages
     */
    public static void validateCategory(int categoryId, String fieldName) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * Validates category identifier and name.
     *
     * @param categoryId   category identifier (positive)
     * @param categoryName category name (non-empty)
     * @param fieldName    logical field name for error messages
     */
    public static void validateCategory(int categoryId, String categoryName, String fieldName) {
        validateCategory(categoryId, fieldName);
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " name cannot be empty");
        }
    }
}