package com.achat.message.dto;

public record CooldownNotice(
        long remainingMs,
        boolean shouldWarn,
        String message
) {
    public static CooldownNotice of(long remainingMs, boolean shouldWarn) {
        String msg = shouldWarn
                ? "잠시 쉬어주세요"
                : "메시지가 길어 잠시 후 전송돼요";
        return new CooldownNotice(remainingMs, shouldWarn, msg);
    }
}