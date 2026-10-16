package com.achat.shop;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;

@Entity
@Table(name = "user_inventory_items", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "item_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserInventoryItem extends JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter(AccessLevel.NONE)
    private Long id;
    @Column(length = 36)
    @Getter(AccessLevel.NONE)
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
}
