package com.achat.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record EquipRequest(@NotBlank String itemId) {}