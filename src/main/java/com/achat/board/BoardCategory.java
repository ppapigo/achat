package com.achat.board;

public enum BoardCategory {
    FREE("자유"),
    GUIDE("공략"),
    QUESTION("질문"),
    TRADE("거래"),
    NOTICE("공지");

    private final String displayName;

    BoardCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}