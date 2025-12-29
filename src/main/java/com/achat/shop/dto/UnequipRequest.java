package com.achat.shop.dto;

import com.achat.shop.EquipSlot;
import jakarta.validation.constraints.NotNull;

public record UnequipRequest(@NotNull EquipSlot slot) {}