package com.achat.shop;

import java.util.EnumMap;
import java.util.Map;

public class UserEquipment {
    private String userId;
    private Map<EquipSlot, String> equipped = new EnumMap<>(EquipSlot.class);

    public UserEquipment(String userId) {
        this.userId = userId;
    }

    public void equip(EquipSlot slot, String itemId) {
        equipped.put(slot, itemId);
    }

    public void unequip(EquipSlot slot) {
        equipped.remove(slot);
    }

    public String getEquipped(EquipSlot slot) {
        return equipped.get(slot);
    }

    public Map<EquipSlot, String> getAllEquipped() {
        return new EnumMap<>(equipped);
    }

    public String getUserId() { return userId; }
}