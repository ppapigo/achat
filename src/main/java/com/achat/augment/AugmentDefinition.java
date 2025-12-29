package com.achat.augment;

public record AugmentDefinition(
        AugmentType type,
        String name,
        String description,
        String category
) {
    // 기존 3개 파라미터 생성자 호환
    public AugmentDefinition(AugmentType type, String name, String description) {
        this(type, name, description, "기본");
    }

    public static AugmentDefinition of(AugmentType type) {
        return switch (type) {
            case REVERSE_TEXT -> new AugmentDefinition(type, "리버스", "메시지가 거꾸로 표시됩니다", "기본");
            case CAPS_LOCK -> new AugmentDefinition(type, "캡스락", "모든 텍스트가 대문자로 변환됩니다", "기본");
            case WHISPER -> new AugmentDefinition(type, "속삭임", "모든 텍스트가 소문자로 변환됩니다", "기본");
            case TURBO_CASTER -> new AugmentDefinition(type, "터보 캐스터", "메시지 쿨타임 40% 감소", "속도");
            case ECHO_BURST -> new AugmentDefinition(type, "에코 버스트", "첫 줄이 강조 표시됩니다", "이펙트");
            case VISUAL_SPELL -> new AugmentDefinition(type, "비주얼 스펠", "메시지에 비주얼 이펙트 적용", "이펙트");
            case THREAD_WEAVER -> new AugmentDefinition(type, "쓰레드 위버", "답장이 시각적으로 연결됩니다", "이펙트");
            case SHADOW_SPEAKER -> new AugmentDefinition(type, "그림자 화자", "익명으로 발언합니다", "소셜");
            case SPOTLIGHT -> new AugmentDefinition(type, "스팟라이트", "메시지를 강조 고정합니다", "소셜");
            case LINK_BRIDGE -> new AugmentDefinition(type, "링크 브리지", "인용 메시지가 눈에 띄게 연결됩니다", "소셜");
            case HARMONY_AURA -> new AugmentDefinition(type, "하모니 오라", "공격적 표현이 자동 완화됩니다", "소셜");
            case CHAOS_ROULETTE -> new AugmentDefinition(type, "카오스 룰렛", "랜덤 이펙트가 적용됩니다", "메타");
            case QUEST_GIVER -> new AugmentDefinition(type, "퀘스트 기버", "채팅 미션을 생성합니다", "메타");
            case TIME_SHIFTER -> new AugmentDefinition(type, "타임 시프터", "관심 메시지가 상단에 표시됩니다", "메타");
            case INSIGHT_DROP -> new AugmentDefinition(type, "인사이트 드롭", "라운드 종료 시 하이라이트 제공", "메타");
        };
    }
}