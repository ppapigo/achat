package com.achat.report;

public enum ReportReason {
    // 유저 신고 사유
    HARASSMENT("괴롭힘/욕설"),
    SPAM("스팸/도배"),
    INAPPROPRIATE_CONTENT("부적절한 콘텐츠"),
    IMPERSONATION("사칭"),
    CHEATING("부정행위"),

    // 방 신고 사유
    ILLEGAL_CONTENT("불법 콘텐츠"),
    INAPPROPRIATE_NAME("부적절한 방 이름"),
    SCAM("사기/피싱"),

    // 게시글 신고 사유
    FALSE_INFO("허위 정보"),
    ADVERTISEMENT("광고/홍보"),
    HATE_SPEECH("혐오 발언"),
    PRIVACY_VIOLATION("개인정보 침해"),

    OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}