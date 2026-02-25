package com.kdue.browserfx;

/**
 * Represents a product extracted from the current page (e.g. from product cards),
 * for display in the AI Companion chat with thumbnail, link, price, and optional add-to-cart CTA label.
 */
public record ProductInfo(String title, String url, String imageUrl, String price, String addToCartLabel) {

    public ProductInfo(String title, String url, String imageUrl, String price) {
        this(title, url, imageUrl, price, null);
    }

    public static ProductInfo of(String title, String url, String imageUrl, String price) {
        return new ProductInfo(
            nullToEmpty(title),
            nullToEmpty(url),
            nullToEmpty(imageUrl),
            nullToEmpty(price),
            null
        );
    }

    public static ProductInfo of(String title, String url, String imageUrl, String price, String addToCartLabel) {
        return new ProductInfo(
            nullToEmpty(title),
            nullToEmpty(url),
            nullToEmpty(imageUrl),
            nullToEmpty(price),
            addToCartLabel == null || addToCartLabel.isBlank() ? null : addToCartLabel.trim()
        );
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
