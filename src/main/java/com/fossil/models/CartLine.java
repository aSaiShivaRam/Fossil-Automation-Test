package com.fossil.models;

import java.math.BigDecimal;

/** Snapshot of one row on the cart page. Pages return these; tests assert on them. */
public final class CartLine {

    private final String name;
    private final String sku;
    private final BigDecimal unitPrice;
    private final int quantity;

    public CartLine(String name, String sku, BigDecimal unitPrice, int quantity) {
        this.name = name;
        this.sku = sku;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return sku + " x" + quantity + " @ " + unitPrice + " (" + name + ")";
    }
}
