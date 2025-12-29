package com.achat.round;

public enum RoundPhase {
    WAITING,    // 대기 중
    PICKING,    // 증강 선택 중 (20초)
    PLAYING,    // 채팅 중
    ENDED       // 라운드 종료
}