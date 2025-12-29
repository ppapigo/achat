package com.achat.round;

/**
 * 점수 정책 상수
 * 한 곳에서 관리해서 밸런싱 쉽게
 */
public class ScorePolicy {

    // 레이트 리밋: N초에 1회만 점수 인정
    public static final int RATE_LIMIT_SECONDS = 5;

    // 라운드당 점수 인정 메시지 최대 개수
    public static final int MAX_SCORED_MESSAGES = 30;

    // 최소 메시지 길이 (미만이면 0점)
    public static final int MIN_MESSAGE_LENGTH = 8;

    // 연속 발언 최대 횟수 (초과 시 다른 유저가 말할 때까지 0점)
    public static final int MAX_CONSECUTIVE = 2;

    // 점수 계산
    public static final int POINTS_PER_MESSAGE = 10;
    public static final int AUGMENT_BONUS = 50;

    private ScorePolicy() {}
}