package com.achat.message.dto;

public record SendMessageRequest(
        String content,
        String replyToId,
        String clientMessageId
) {
    // 기존 호환용 생성자
    public SendMessageRequest(String content) {
        this(content, null, null);
    }
    public SendMessageRequest(String content, String replyToId) { this(content, replyToId, null); }
}
