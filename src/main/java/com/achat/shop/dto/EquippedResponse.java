package com.achat.shop.dto;

import java.util.Map;

public record EquippedResponse(Map<String, EquippedItem> slots) {
    public record EquippedItem(String itemId, String name) {}
}