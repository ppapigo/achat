package com.achat.sanction;

public enum SanctionType {
    WARNING("경고", 0),           // 경고만
    MUTE("채팅 금지", 60),        // 채팅 금지 (기본 1시간)
    TEMP_BAN("임시 정지", 1440),  // 임시 정지 (기본 24시간)
    PERM_BAN("영구 정지", -1);    // 영구 정지

    private final String description;
    private final int defaultMinutes; // -1이면 영구

    SanctionType(String description, int defaultMinutes) {
        this.description = description;
        this.defaultMinutes = defaultMinutes;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultMinutes() {
        return defaultMinutes;
    }
}