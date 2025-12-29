package com.achat.shop.dto;

import com.achat.shop.*;
import java.time.Instant;
import java.util.List;

public record InventoryResponse(List<Item> items) {

    public record Item(
            String itemId,
            String name,
            ItemType type,
            ItemCategory category,
            Rarity rarity,
            EquipSlot equipSlot,
            int quantity,
            Instant acquiredAt
    ) {
        public static Item from(UserInventoryItem inv, ShopItem item) {
            return new Item(
                    inv.getItemId(),
                    item != null ? item.getName() : "Unknown",
                    item != null ? item.getType() : null,
                    item != null ? item.getCategory() : null,
                    item != null ? item.getRarity() : null,
                    item != null ? item.getEquipSlot() : null,
                    inv.getQuantity(),
                    inv.getAcquiredAt()
            );
        }
    }
}