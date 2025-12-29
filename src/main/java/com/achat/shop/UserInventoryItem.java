package com.achat.shop;

import java.time.Instant;

public class UserInventoryItem {
    private String itemId;
    private int quantity;
    private Instant acquiredAt;

    public UserInventoryItem(String itemId) {
        this.itemId = itemId;
        this.quantity = 1;
        this.acquiredAt = Instant.now();
    }

    public void addQuantity(int amount) {
        this.quantity += amount;
    }

    public boolean useOne() {
        if (quantity <= 0) return false;
        quantity--;
        return true;
    }

    public String getItemId() { return itemId; }
    public int getQuantity() { return quantity; }
    public Instant getAcquiredAt() { return acquiredAt; }
}