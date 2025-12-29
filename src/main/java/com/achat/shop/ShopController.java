package com.achat.shop;

import com.achat.common.ApiResponse;
import com.achat.shop.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    // 상점 아이템 목록
    @GetMapping("/items")
    public ApiResponse<List<ShopItemResponse>> getItems(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) ItemCategory category
    ) {
        List<ShopItem> items = category != null
                ? shopService.getItemsByCategory(category)
                : shopService.getAllItems();

        List<ShopItemResponse> response = items.stream()
                .map(item -> ShopItemResponse.from(item, shopService.hasItem(userId, item.getId())))
                .toList();

        return ApiResponse.ok(response);
    }

    // 구매
    @PostMapping("/purchase")
    public ApiResponse<?> purchase(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PurchaseRequest request
    ) {
        var result = shopService.purchase(userId, request.itemId());
        if (!result.success()) {
            return ApiResponse.error(result.message());
        }
        return ApiResponse.ok(Map.of(
                "message", result.message(),
                "itemName", result.item().getName(),
                "remainingCoins", result.remainingCoins()
        ));
    }

    // 인벤토리 조회
    @GetMapping("/inventory")
    public ApiResponse<InventoryResponse> getInventory(@AuthenticationPrincipal String userId) {
        var inventory = shopService.getInventory(userId);
        var items = inventory.stream()
                .map(inv -> {
                    ShopItem item = shopService.getItem(inv.getItemId());
                    return InventoryResponse.Item.from(inv, item);
                })
                .toList();
        return ApiResponse.ok(new InventoryResponse(items));
    }

    // 장착
    @PostMapping("/equip")
    public ApiResponse<?> equip(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody EquipRequest request
    ) {
        var result = shopService.equip(userId, request.itemId());
        if (!result.success()) {
            return ApiResponse.error(result.message());
        }
        return ApiResponse.ok(Map.of(
                "message", result.message(),
                "slot", result.slot().name(),
                "itemId", result.itemId()
        ));
    }

    // 장착 해제
    @PostMapping("/unequip")
    public ApiResponse<?> unequip(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UnequipRequest request
    ) {
        var result = shopService.unequip(userId, request.slot());
        if (!result.success()) {
            return ApiResponse.error(result.message());
        }
        return ApiResponse.ok(Map.of("message", result.message()));
    }

    // 현재 장착 상태
    @GetMapping("/equipped")
    public ApiResponse<EquippedResponse> getEquipped(@AuthenticationPrincipal String userId) {
        var equipment = shopService.getEquipment(userId);
        var equipped = equipment.getAllEquipped();

        var response = equipped.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> {
                            ShopItem item = shopService.getItem(e.getValue());
                            return new EquippedResponse.EquippedItem(e.getValue(), item != null ? item.getName() : "Unknown");
                        }
                ));

        return ApiResponse.ok(new EquippedResponse(response));
    }

    // 소모품 사용
    @PostMapping("/use")
    public ApiResponse<?> useItem(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody UseItemRequest request
    ) {
        var result = shopService.useItem(userId, request.itemId());
        if (!result.success()) {
            return ApiResponse.error(result.message());
        }
        return ApiResponse.ok(Map.of(
                "message", result.message(),
                "effect", result.effect()
        ));
    }
    /**
     * 보유 가구 목록
     */
    @GetMapping("/furniture")
    public ApiResponse<List<ShopItemResponse>> getOwnedFurniture(
            @AuthenticationPrincipal String userId
    ) {
        List<ShopItem> furniture = shopService.getOwnedFurniture(userId);
        List<ShopItemResponse> response = furniture.stream()
                .map(item -> ShopItemResponse.from(item, true))  // 보유 중이므로 true
                .toList();
        return ApiResponse.ok(response);
    }
}