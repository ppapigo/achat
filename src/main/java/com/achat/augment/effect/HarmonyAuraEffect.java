package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

import java.util.Map;
import java.util.regex.Pattern;

public class HarmonyAuraEffect implements AugmentEffect {

    // 욕설 → 순화 매핑
    private static final Map<String, String> FILTER_MAP = Map.ofEntries(
            // 욕설 필터링
            Map.entry("시발", "아이고"),
            Map.entry("씨발", "아이고"),
            Map.entry("ㅅㅂ", "ㅎㅎ"),
            Map.entry("ㅆㅂ", "ㅎㅎ"),
            Map.entry("병신", "친구"),
            Map.entry("ㅂㅅ", "ㅎㅎ"),
            Map.entry("지랄", "그렇구나"),
            Map.entry("ㅈㄹ", "ㅎㅎ"),
            Map.entry("개새끼", "친구"),
            Map.entry("새끼", "친구"),
            Map.entry("꺼져", "안녕"),
            Map.entry("닥쳐", "잠깐만"),
            Map.entry("죽어", "힘내"),
            Map.entry("미친", "대단한"),
            Map.entry("ㅁㅊ", "ㅎㅎ"),
            Map.entry("썅", "앗"),
            Map.entry("엿", "응"),
            Map.entry("좆", "멋진"),
            Map.entry("ㅈ같", "그렇네"),

            // 공격적 표현 완화
            Map.entry("뭐야", "뭐지"),
            Map.entry("왜?", "왜요?"),
            Map.entry("하지마", "잠깐만요"),
            Map.entry("싫어", "글쎄요"),
            Map.entry("짜증", "음"),
            Map.entry("화나", "아쉽")
    );

    // ㅋㅋㅋ, ㅎㅎㅎ 과다 반복 패턴
    private static final Pattern SPAM_LAUGH = Pattern.compile("[ㅋㅎ]{4,}");
    // !!! ??? 과다 반복
    private static final Pattern SPAM_PUNCT = Pattern.compile("[!?]{2,}");
    // ㄱㄱㄱ, ㅇㅇㅇ 등 자음 반복
    private static final Pattern SPAM_CONSONANT = Pattern.compile("([ㄱ-ㅎ])\\1{2,}");

    @Override
    public AugmentType getType() { return AugmentType.HARMONY_AURA; }

    @Override
    public String getName() { return "하모니 오라"; }

    @Override
    public String getDescription() { return "공격적 표현이 자동 완화됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        String result = message;

        // 1. 욕설/공격적 표현 필터링
        for (Map.Entry<String, String> entry : FILTER_MAP.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // 2. 스팸 패턴 정리
        result = SPAM_LAUGH.matcher(result).replaceAll("ㅎㅎ");
        result = SPAM_PUNCT.matcher(result).replaceAll(".");
        result = SPAM_CONSONANT.matcher(result).replaceAll("$1");

        // 3. 대문자 공격(영어) 완화
        if (result.equals(result.toUpperCase()) && result.length() > 3) {
            result = result.toLowerCase();
        }

        return "🌈 " + result;
    }
}