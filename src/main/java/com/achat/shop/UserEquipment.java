package com.achat.shop;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.util.EnumMap;
import java.util.Map;

@Entity
@Table(name = "user_equipment")
public class UserEquipment extends JpaEntity {
    protected UserEquipment() {}

    @Id
    @Column(length = 36)
    private String userId;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "equipment_slots", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "equip_slot")
    @Column(name = "item_id")
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
        Map<EquipSlot, String> copy = new EnumMap<>(EquipSlot.class);
        copy.putAll(equipped);
        return copy;
    }

    public String getUserId() { return userId; }
}