package com.achat.home;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 유저의 개인 홈 (싸이월드 미니홈피 컨셉)
 */
public class Home {

    private final String userId;
    private String statusMessage;           // 상태 메시지 (100자 이하)
    private String representativeJobId;     // 대표 직업
    private String representativeAugmentId; // 대표 증강
    private String backgroundTheme;         // 배경 테마
    private String titleBadge;              // 칭호
    private String avatarStyle = "pixel-art";  // 아바타 스타일
    private String avatarSeed;                  // 아바타 시드 (기본: userId)
    private String avatarBgColor = "transparent"; // 배경색
    private long visitCount;                // 방문 수

    // ✅ 가구 배치 (가구ID -> 위치)
    private Map<String, FurniturePosition> furnitureLayout;

    private Instant createdAt;
    private Instant updatedAt;



    public Home(String userId) {
        this.userId = userId;
        this.statusMessage = "";
        this.backgroundTheme = "default";
        this.visitCount = 0;
        this.furnitureLayout = getDefaultFurnitureLayout();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // === 가구 위치 내부 클래스 ===
    public static class FurniturePosition {
        private double x;  // 0-100 (퍼센트)
        private double y;  // 0-100 (퍼센트)

        public FurniturePosition() {}

        public FurniturePosition(double x, double y) {
            this.x = Math.max(0, Math.min(100, x));
            this.y = Math.max(0, Math.min(100, y));
        }

        public double getX() { return x; }
        public double getY() { return y; }

        public void setX(double x) { this.x = Math.max(0, Math.min(100, x)); }
        public void setY(double y) { this.y = Math.max(0, Math.min(100, y)); }
    }

    // === Getters ===
    public String getUserId() { return userId; }
    public String getStatusMessage() { return statusMessage; }
    public String getRepresentativeJobId() { return representativeJobId; }
    public String getRepresentativeAugmentId() { return representativeAugmentId; }
    public String getBackgroundTheme() { return backgroundTheme; }
    public String getTitleBadge() { return titleBadge; }
    public long getVisitCount() { return visitCount; }
    public Map<String, FurniturePosition> getFurnitureLayout() { return furnitureLayout; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getAvatarStyle() { return avatarStyle; }
    public String getAvatarSeed() { return avatarSeed; }
    public String getAvatarBgColor() { return avatarBgColor; }

    public void setAvatarStyle(String avatarStyle) { this.avatarStyle = avatarStyle; }
    public void setAvatarSeed(String avatarSeed) { this.avatarSeed = avatarSeed; }
    public void setAvatarBgColor(String avatarBgColor) { this.avatarBgColor = avatarBgColor; }

    // === Update 메서드 ===
    public void updateStatusMessage(String message) {
        if (message != null && message.length() > 100) {
            throw new IllegalArgumentException("상태 메시지는 100자 이하");
        }
        this.statusMessage = message != null ? message : "";
        this.updatedAt = Instant.now();
    }

    public void updateRepresentativeJob(String jobId) {
        this.representativeJobId = jobId;
        this.updatedAt = Instant.now();
    }

    public void updateRepresentativeAugment(String augmentId) {
        this.representativeAugmentId = augmentId;
        this.updatedAt = Instant.now();
    }

    public void updateBackgroundTheme(String theme) {
        this.backgroundTheme = theme != null ? theme : "default";
        this.updatedAt = Instant.now();
    }

    public void updateTitleBadge(String badge) {
        this.titleBadge = badge;
        this.updatedAt = Instant.now();
    }

    public void incrementVisitCount() {
        this.visitCount++;
    }

    // ✅ 가구 배치 업데이트
    public void updateFurnitureLayout(Map<String, FurniturePosition> layout) {
        if (layout == null || layout.isEmpty()) {
            return;
        }
        this.furnitureLayout = new HashMap<>(layout);
        this.updatedAt = Instant.now();
    }

    // ✅ 특정 가구 위치 업데이트
    public void updateFurniturePosition(String furnitureId, double x, double y) {
        if (this.furnitureLayout == null) {
            this.furnitureLayout = new HashMap<>();
        }
        this.furnitureLayout.put(furnitureId, new FurniturePosition(x, y));
        this.updatedAt = Instant.now();
    }

    // ✅ 가구 제거
    public void removeFurniture(String furnitureId) {
        if (this.furnitureLayout != null) {
            this.furnitureLayout.remove(furnitureId);
            this.updatedAt = Instant.now();
        }
    }

    // ✅ 기본 가구 배치
    private Map<String, FurniturePosition> getDefaultFurnitureLayout() {
        Map<String, FurniturePosition> layout = new HashMap<>();
        layout.put("avatar", new FurniturePosition(50, 70));
        layout.put("bed", new FurniturePosition(15, 65));
        layout.put("desk", new FurniturePosition(75, 60));
        layout.put("sofa", new FurniturePosition(45, 55));
        layout.put("plant", new FurniturePosition(85, 50));
        layout.put("tv", new FurniturePosition(10, 35));
        return layout;
    }

}