package com.achat.shop;

import org.springframework.transaction.annotation.Transactional;

import com.achat.home.HomeService;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class ShopService {

    private final UserStore userStore;
    private final HomeService homeService;

    private final Map<String, ShopItem> shopItems = new ConcurrentHashMap<>();
    private final UserInventoryItemRepository inventoryRepository;
    private final UserEquipmentRepository equipmentRepository;

    public ShopService(UserStore userStore, HomeService homeService,
                       UserInventoryItemRepository inventoryRepository, UserEquipmentRepository equipmentRepository) {
        this.inventoryRepository = inventoryRepository;
        this.equipmentRepository = equipmentRepository;
        this.userStore = userStore;
        this.homeService = homeService;
        initCatalog();
    }

    private void initCatalog() {
        // === Consumables ===
        shopItems.put("augment_pack_basic", new ShopItem(
                "augment_pack_basic", ItemType.CONSUMABLE, ItemCategory.AUGMENT_PACK,
                "Basic Augment Pack", 100, "Choose 1 of 3 augments", Rarity.COMMON, null
        ));
        shopItems.put("augment_pack_rare", new ShopItem(
                "augment_pack_rare", ItemType.CONSUMABLE, ItemCategory.AUGMENT_PACK,
                "Rare Augment Pack", 300, "Choose 1 of 3 rare+ augments", Rarity.RARE, null
        ));
        shopItems.put("reroll_ticket", new ShopItem(
                "reroll_ticket", ItemType.CONSUMABLE, ItemCategory.REROLL,
                "Reroll Ticket", 50, "Reroll augment choices", Rarity.COMMON, null
        ));

        // === Titles ===
        shopItems.put("title_newbie", new ShopItem(
                "title_newbie", ItemType.ENTITLEMENT, ItemCategory.TITLE,
                "Newbie", 100, "First steps", Rarity.COMMON, EquipSlot.TITLE
        ));
        shopItems.put("title_chatter", new ShopItem(
                "title_chatter", ItemType.ENTITLEMENT, ItemCategory.TITLE,
                "Chatter", 200, "Talkative one", Rarity.RARE, EquipSlot.TITLE
        ));
        shopItems.put("title_whale", new ShopItem(
                "title_whale", ItemType.ENTITLEMENT, ItemCategory.TITLE,
                "Whale", 1000, "Big spender", Rarity.LEGENDARY, EquipSlot.TITLE
        ));
        shopItems.put("title_streamer", new ShopItem(
                "title_streamer", ItemType.ENTITLEMENT, ItemCategory.TITLE,
                "Streaker", 300, "On fire", Rarity.EPIC, EquipSlot.TITLE
        ));

        // === Emoji Sets ===
        shopItems.put("emoji_fire", new ShopItem(
                "emoji_fire", ItemType.ENTITLEMENT, ItemCategory.EMOJI,
                "Fire Set", 150, "5 fire emojis", Rarity.RARE, EquipSlot.EMOJI_SET
        ));
        shopItems.put("emoji_cute", new ShopItem(
                "emoji_cute", ItemType.ENTITLEMENT, ItemCategory.EMOJI,
                "Cute Set", 150, "5 cute emojis", Rarity.RARE, EquipSlot.EMOJI_SET
        ));

        // === Borders ===
        shopItems.put("border_gold", new ShopItem(
                "border_gold", ItemType.ENTITLEMENT, ItemCategory.BORDER,
                "Gold Border", 500, "Golden profile border", Rarity.EPIC, EquipSlot.BORDER
        ));
        shopItems.put("border_rainbow", new ShopItem(
                "border_rainbow", ItemType.ENTITLEMENT, ItemCategory.BORDER,
                "Rainbow Border", 800, "Rainbow profile", Rarity.LEGENDARY, EquipSlot.BORDER
        ));

        // === Backgrounds ===
        shopItems.put("bg_default", new ShopItem(
                "bg_default", ItemType.ENTITLEMENT, ItemCategory.BACKGROUND,
                "Default", 0, "Simple dark background", Rarity.COMMON, EquipSlot.BACKGROUND
        ));
        shopItems.put("bg_sunset", new ShopItem(
                "bg_sunset", ItemType.ENTITLEMENT, ItemCategory.BACKGROUND,
                "Sunset", 500, "Warm sunset background", Rarity.RARE, EquipSlot.BACKGROUND
        ));
        shopItems.put("bg_ocean", new ShopItem(
                "bg_ocean", ItemType.ENTITLEMENT, ItemCategory.BACKGROUND,
                "Ocean", 500, "Deep ocean background", Rarity.RARE, EquipSlot.BACKGROUND
        ));
        shopItems.put("bg_forest", new ShopItem(
                "bg_forest", ItemType.ENTITLEMENT, ItemCategory.BACKGROUND,
                "Forest", 1200, "Magic forest background", Rarity.EPIC, EquipSlot.BACKGROUND
        ));
        shopItems.put("bg_galaxy", new ShopItem(
                "bg_galaxy", ItemType.ENTITLEMENT, ItemCategory.BACKGROUND,
                "Galaxy", 1500, "Starry universe", Rarity.EPIC, EquipSlot.BACKGROUND
        ));
        shopItems.put("bg_aurora", new ShopItem(
                "bg_aurora", ItemType.ENTITLEMENT, ItemCategory.BACKGROUND,
                "Aurora", 3000, "Northern lights", Rarity.LEGENDARY, EquipSlot.BACKGROUND
        ));
        shopItems.put("bg_cherry", new ShopItem(
                "bg_cherry", ItemType.ENTITLEMENT, ItemCategory.BACKGROUND,
                "Cherry Blossom", 800, "Spring blossoms", Rarity.RARE, EquipSlot.BACKGROUND
        ));
        shopItems.put("bg_rain", new ShopItem(
                "bg_rain", ItemType.ENTITLEMENT, ItemCategory.BACKGROUND,
                "Rainy Night", 1000, "Calm rain sounds", Rarity.EPIC, EquipSlot.BACKGROUND
        ));

        // === Room Themes ===
        shopItems.put("room_default", new ShopItem(
                "room_default", ItemType.ENTITLEMENT, ItemCategory.ROOM,
                "Default Room", 0, "Basic room", Rarity.COMMON, EquipSlot.ROOM
        ));
        shopItems.put("room_cozy", new ShopItem(
                "room_cozy", ItemType.ENTITLEMENT, ItemCategory.ROOM,
                "Cozy Room", 500, "Warm and cozy", Rarity.RARE, EquipSlot.ROOM
        ));
        shopItems.put("room_modern", new ShopItem(
                "room_modern", ItemType.ENTITLEMENT, ItemCategory.ROOM,
                "Modern Room", 800, "Sleek modern style", Rarity.EPIC, EquipSlot.ROOM
        ));
        shopItems.put("room_forest", new ShopItem(
                "room_forest", ItemType.ENTITLEMENT, ItemCategory.ROOM,
                "Forest Cabin", 1200, "Nature retreat", Rarity.EPIC, EquipSlot.ROOM
        ));
        shopItems.put("room_space", new ShopItem(
                "room_space", ItemType.ENTITLEMENT, ItemCategory.ROOM,
                "Space Station", 2000, "Zero gravity", Rarity.LEGENDARY, EquipSlot.ROOM
        ));
        // === Furniture (가구) ===
// 기본 가구 (무료)
        shopItems.put("furn_bed", new ShopItem(
                "furn_bed", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🛏️ 침대", 0, "기본 침대", Rarity.COMMON, null
        ));
        shopItems.put("furn_desk", new ShopItem(
                "furn_desk", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🪑 책상", 0, "기본 책상", Rarity.COMMON, null
        ));
        shopItems.put("furn_sofa", new ShopItem(
                "furn_sofa", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🛋️ 소파", 0, "기본 소파", Rarity.COMMON, null
        ));
        shopItems.put("furn_tv", new ShopItem(
                "furn_tv", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "📺 TV", 0, "기본 TV", Rarity.COMMON, null
        ));
        shopItems.put("furn_plant", new ShopItem(
                "furn_plant", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🪴 화분", 0, "기본 화분", Rarity.COMMON, null
        ));

// 유료 가구
        shopItems.put("furn_piano", new ShopItem(
                "furn_piano", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🎹 피아노", 500, "멋진 피아노", Rarity.RARE, null
        ));
        shopItems.put("furn_aquarium", new ShopItem(
                "furn_aquarium", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🐠 어항", 300, "열대어 어항", Rarity.RARE, null
        ));
        shopItems.put("furn_bookshelf", new ShopItem(
                "furn_bookshelf", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "📚 책장", 400, "가득 찬 책장", Rarity.RARE, null
        ));
        shopItems.put("furn_lamp", new ShopItem(
                "furn_lamp", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "💡 스탠드", 200, "무드등", Rarity.COMMON, null
        ));
        shopItems.put("furn_rug", new ShopItem(
                "furn_rug", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🟫 러그", 250, "푹신한 러그", Rarity.COMMON, null
        ));
        shopItems.put("furn_cat", new ShopItem(
                "furn_cat", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🐱 고양이", 800, "귀여운 고양이", Rarity.EPIC, null
        ));
        shopItems.put("furn_arcade", new ShopItem(
                "furn_arcade", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🕹️ 오락기", 1000, "레트로 오락기", Rarity.EPIC, null
        ));
        shopItems.put("furn_robot", new ShopItem(
                "furn_robot", ItemType.ENTITLEMENT, ItemCategory.FURNITURE,
                "🤖 로봇", 1500, "AI 로봇", Rarity.LEGENDARY, null
        ));
    }

    public List<ShopItem> getAllItems() {
        return shopItems.values().stream()
                .filter(ShopItem::isAvailable)
                .toList();
    }

    public List<ShopItem> getItemsByCategory(ItemCategory category) {
        return shopItems.values().stream()
                .filter(item -> item.isAvailable() && item.getCategory() == category)
                .toList();
    }

    public ShopItem getItem(String itemId) {
        return shopItems.get(itemId);
    }

    public PurchaseResult purchase(String userId, String itemId) {
        ShopItem item = shopItems.get(itemId);
        if (item == null || !item.isAvailable()) {
            return PurchaseResult.fail("Item not found or unavailable");
        }

        User user = userStore.findById(userId).orElse(null);
        if (user == null) {
            return PurchaseResult.fail("User not found");
        }

        if (user.getCoins() < item.getPrice()) {
            return PurchaseResult.fail("Not enough coins");
        }

        if (item.getType() == ItemType.ENTITLEMENT && hasItem(userId, itemId)) {
            return PurchaseResult.fail("Already owned");
        }

        user.addCoins(-item.getPrice());
        addToInventory(userId, itemId, item.isConsumable());

        return PurchaseResult.success(item, user.getCoins());
    }

    private void addToInventory(String userId, String itemId, boolean stackable) {
        Optional<UserInventoryItem> existing = inventoryRepository.findByUserIdAndItemId(userId, itemId);

        if (existing.isPresent() && stackable) {
            existing.get().addQuantity(1);
        } else if (existing.isEmpty()) {
            inventoryRepository.save(new UserInventoryItem(userId, itemId));
        }
    }

    public List<UserInventoryItem> getInventory(String userId) {
        return inventoryRepository.findByUserIdOrderByAcquiredAtAsc(userId);
    }

    public boolean hasItem(String userId, String itemId) {
        return getInventory(userId).stream()
                .anyMatch(i -> i.getItemId().equals(itemId) && i.getQuantity() > 0);
    }

    public EquipResult equip(String userId, String itemId) {
        if (!hasItem(userId, itemId)) {
            return EquipResult.fail("Item not owned");
        }

        ShopItem item = shopItems.get(itemId);
        if (item == null || !item.isEquippable()) {
            return EquipResult.fail("Cannot equip this item");
        }

        UserEquipment equipment = equipmentRepository.findById(userId).orElseGet(() -> equipmentRepository.save(new UserEquipment(userId)));
        equipment.equip(item.getEquipSlot(), itemId);

        if (item.getEquipSlot() == EquipSlot.BACKGROUND) {
            homeService.updateBackgroundTheme(userId, itemId);
        } else if (item.getEquipSlot() == EquipSlot.TITLE) {
            homeService.updateTitleBadge(userId, item.getName());
        } else if (item.getEquipSlot() == EquipSlot.ROOM) {
            homeService.updateBackgroundTheme(userId, itemId);
        }

        return EquipResult.success(item.getEquipSlot(), itemId);
    }

    public EquipResult unequip(String userId, EquipSlot slot) {
        UserEquipment equipment = equipmentRepository.findById(userId).orElse(null);
        if (equipment == null || equipment.getEquipped(slot) == null) {
            return EquipResult.fail("Nothing equipped");
        }

        equipment.unequip(slot);

        if (slot == EquipSlot.BACKGROUND || slot == EquipSlot.ROOM) {
            homeService.updateBackgroundTheme(userId, "default");
        } else if (slot == EquipSlot.TITLE) {
            homeService.updateTitleBadge(userId, null);
        }

        return EquipResult.success(slot, null);
    }

    public UserEquipment getEquipment(String userId) {
        return equipmentRepository.findById(userId).orElseGet(() -> equipmentRepository.save(new UserEquipment(userId)));
    }

    public UseResult useItem(String userId, String itemId) {
        ShopItem item = shopItems.get(itemId);
        if (item == null || item.getType() != ItemType.CONSUMABLE) {
            return UseResult.fail("Cannot use this item");
        }

        Optional<UserInventoryItem> invItem = inventoryRepository.findByUserIdAndItemId(userId, itemId);

        if (invItem.isEmpty() || !invItem.get().useOne()) {
            return UseResult.fail("Item not owned");
        }

        return applyItemEffect(userId, item);
    }

    private UseResult applyItemEffect(String userId, ShopItem item) {
        return switch (item.getCategory()) {
            case AUGMENT_PACK -> UseResult.success("Augment pack used!", Map.of(
                    "action", "OPEN_AUGMENT_SELECTION",
                    "rarity", item.getRarity().name()
            ));
            case REROLL -> UseResult.success("Reroll used!", Map.of(
                    "action", "REROLL_AUGMENTS"
            ));
            default -> UseResult.fail("Unknown item");
        };
    }

    public record PurchaseResult(boolean success, String message, ShopItem item, int remainingCoins) {
        public static PurchaseResult success(ShopItem item, int remainingCoins) {
            return new PurchaseResult(true, "Purchase complete!", item, remainingCoins);
        }
        public static PurchaseResult fail(String message) {
            return new PurchaseResult(false, message, null, 0);
        }
    }

    public record EquipResult(boolean success, String message, EquipSlot slot, String itemId) {
        public static EquipResult success(EquipSlot slot, String itemId) {
            return new EquipResult(true, "Equipped!", slot, itemId);
        }
        public static EquipResult fail(String message) {
            return new EquipResult(false, message, null, null);
        }
    }

    public record UseResult(boolean success, String message, Map<String, Object> effect) {
        public static UseResult success(String message, Map<String, Object> effect) {
            return new UseResult(true, message, effect);
        }
        public static UseResult fail(String message) {
            return new UseResult(false, message, null);
        }
    }
    // ========== 가구 시스템 ==========

    private static final List<String> DEFAULT_FURNITURE = List.of(
            "furn_bed", "furn_desk", "furn_sofa", "furn_tv", "furn_plant"
    );

    /**
     * 신규 유저에게 기본 가구 지급
     */
    public void grantDefaultFurniture(String userId) {
        for (String furnitureId : DEFAULT_FURNITURE) {
            if (!hasItem(userId, furnitureId)) {
                addToInventory(userId, furnitureId, false);
            }
        }
    }

    /**
     * 보유 가구 목록 조회
     */
    public List<ShopItem> getOwnedFurniture(String userId) {
        // 기본 가구 먼저 지급
        grantDefaultFurniture(userId);

        return getInventory(userId).stream()
                .map(inv -> shopItems.get(inv.getItemId()))
                .filter(item -> item != null && item.getCategory() == ItemCategory.FURNITURE)
                .toList();
    }
}