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
            case SEALED_MESSAGE -> new AugmentDefinition(type, "비밀 봉투", "클릭하거나 Enter로 봉투를 열어 메시지를 확인해요", "이펙트");
            case TIME_CAPSULE -> new AugmentDefinition(type, "타임 캡슐", "서버 기준 10초 뒤 메시지가 공개돼요", "시간");
            case TIME_BOMB -> new AugmentDefinition(type, "시한폭탄", "점수 인정 메시지에 15초 안에 상대가 답장하면 10점! 구간당 최대 50점", "미션");
            case JACKPOT_MESSENGER -> new AugmentDefinition(type, "잭팟 메신저", "점수 인정 메시지에서 10% 확률로 10점. 최대 10회 추첨·3회 당첨", "보상");
            case FORBIDDEN_WORD -> new AugmentDefinition(type, "금칙어 저주", "다음 5개 메시지의 금칙어 사용은 각 -3점, 모두 피하면 15점", "미션");
            case GHOST_MESSAGE -> new AugmentDefinition(type, "유령 메시지", "반투명 말풍선에 마우스를 올리거나 터치하면 선명해져요", "이펙트");
            case ZERO_GRAVITY -> new AugmentDefinition(type, "무중력 채팅", "말풍선이 부드럽게 떠올라요. 마우스를 올리면 멈춰요", "이펙트");
            case DOPPELGANGER -> new AugmentDefinition(type, "도플갱어", "2초 뒤 시각적 잔상이 나타나요. 메시지와 보상은 한 번만", "이펙트");
            case LIKE_OVERDRIVE -> new AugmentDefinition(type, "공감 폭주", "공감 1·3·5개마다 변화! 점수 인정 메시지의 5개 달성은 10점, 구간당 최대 3회", "보상");
            case FATE_DICE -> new AugmentDefinition(type, "운명의 주사위", "1~4는 시각 효과, 5는 5점, 6은 10점! 점수 인정 메시지 최대 10회·50점", "보상");
            case MAIN_CHARACTER -> new AugmentDefinition(type, "주인공 모드", "다음 메시지 하나를 6초 동안 금빛 카드로 강조해요", "이펙트");
            case ICE_SEAL -> new AugmentDefinition(type, "얼음 봉인", "클릭하거나 Enter로 얼음을 깨고 메시지를 확인해요", "이펙트");
        };
    }
}
