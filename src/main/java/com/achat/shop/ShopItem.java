package com.achat.shop;

import java.util.Map;

public class ShopItem {
    private String id;
    private ItemType type;
    private ItemCategory category;
    private String name;
    private int price;
    private String description;
    private Rarity rarity;
    private boolean available;
    private EquipSlot equipSlot;  // ENTITLEMENT인 경우 장착 슬롯
    private Map<String, Object> payload;  // 추가 데이터

    public ShopItem(String id, ItemType type, ItemCategory category,
                    String name, int price, String description,
                    Rarity rarity, EquipSlot equipSlot) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.name = name;
        this.price = price;
        this.description = description;
        this.rarity = rarity;
        this.equipSlot = equipSlot;
        this.available = true;
    }

    // Getters
    public String getId() { return id; }
    public ItemType getType() { return type; }
    public ItemCategory getCategory() { return category; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public String getDescription() { return description; }
    public Rarity getRarity() { return rarity; }
    public boolean isAvailable() { return available; }
    public EquipSlot getEquipSlot() { return equipSlot; }
    public Map<String, Object> getPayload() { return payload; }

    public boolean isConsumable() { return type == ItemType.CONSUMABLE; }
    public boolean isEquippable() { return equipSlot != null; }
}