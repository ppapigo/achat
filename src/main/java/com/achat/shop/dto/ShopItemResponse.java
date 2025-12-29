package com.achat.shop.dto;

import com.achat.shop.*;

public record ShopItemResponse(
        String id,
        ItemType type,
        ItemCategory category,
        String name,
        int price,
        String description,
        Rarity rarity,
        EquipSlot equipSlot,
        boolean owned
) {
    public static ShopItemResponse from(ShopItem item, boolean owned) {
        return new ShopItemResponse(
                item.getId(),
                item.getType(),
                item.getCategory(),
                item.getName(),
                item.getPrice(),
                item.getDescription(),
                item.getRarity(),
                item.getEquipSlot(),
                owned
        );
    }
}