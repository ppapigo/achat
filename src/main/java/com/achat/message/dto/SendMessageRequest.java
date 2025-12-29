package com.achat.message.dto;

public record SendMessageRequest(
        String content,
        String replyToId  // 추가!
) {
    // 기존 호환용 생성자
    public SendMessageRequest(String content) {
        this(content, null);
    }
}