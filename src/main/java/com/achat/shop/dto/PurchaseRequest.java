package com.achat.shop.dto;

import jakarta.validation.constraints.NotBlank;

public record PurchaseRequest(@NotBlank String itemId) {}