package com.achat.shop;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;

@Entity
@Table(name = "user_inventory_items", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id"}))
public class UserInventoryItem extends JpaEntity {
    protected UserInventoryItem() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 36)
    private String userId;
    @Column(length = 36)
    private String itemId;
    private int quantity;
    private Instant acquiredAt;

    public UserInventoryItem(String userId, String itemId) {
        this.userId = userId;
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