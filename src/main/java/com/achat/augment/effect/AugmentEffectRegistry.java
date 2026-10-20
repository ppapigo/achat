package com.achat.augment.effect;

import lombok.extern.slf4j.Slf4j;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class AugmentEffectRegistry {

    private final Map<AugmentType, AugmentEffect> effects = new EnumMap<>(AugmentType.class);
    private final Map<AugmentType, MessageEffectHandler> handlers = new EnumMap<>(AugmentType.class);

    public AugmentEffectRegistry(List<MessageEffectHandler> handlers) {
        handlers.forEach(handler -> handler.types().forEach(type -> this.handlers.put(type, handler)));
    }

    public MessageEffect createMetadata(MessageEffectHandler.Input input) {
        var type = input.progress().getType();
        var handler = handlers.get(type);
        return handler == null ? MessageEffect.visual(type) : handler.create(input);
    }

    @PostConstruct
    public void init() {
        // 기존 3개
        register(AugmentType.REVERSE_TEXT, new ReverseTextEffect());
        register(AugmentType.CAPS_LOCK, new CapsLockEffect());
        register(AugmentType.WHISPER, new WhisperEffect());

        // 새로운 12개
        register(AugmentType.TURBO_CASTER, new TurboCasterEffect());
        register(AugmentType.ECHO_BURST, new EchoBurstEffect());
        register(AugmentType.VISUAL_SPELL, new VisualSpellEffect());
        register(AugmentType.THREAD_WEAVER, new ThreadWeaverEffect());
        register(AugmentType.SHADOW_SPEAKER, new ShadowSpeakerEffect());
        register(AugmentType.SPOTLIGHT, new SpotlightEffect());
        register(AugmentType.LINK_BRIDGE, new LinkBridgeEffect());
        register(AugmentType.HARMONY_AURA, new HarmonyAuraEffect());
        register(AugmentType.CHAOS_ROULETTE, new ChaosRouletteEffect());
        register(AugmentType.QUEST_GIVER, new QuestGiverEffect());
        register(AugmentType.TIME_SHIFTER, new TimeShifterEffect());
        register(AugmentType.INSIGHT_DROP, new InsightDropEffect());

        // Only the actual text transforms change text. Every visual effect has structured metadata.
        for (AugmentType type : AugmentType.values()) {
            if (!effects.containsKey(type)) {
                register(type, new PresentationEffect(type));
            }
        }
        log.info("증강 {}개 등록 완료: {}", effects.size(), effects.keySet());
    }

    private void register(AugmentType type, AugmentEffect effect) {
        effects.put(type, effect);
    }

    public String applyEffect(AugmentType type, String content, AugmentContext context) {
        AugmentEffect effect = effects.get(type);
        if (effect == null) return content;
        return effect.apply(content, context);
    }

    public AugmentEffect getEffect(AugmentType type) {
        return effects.get(type);
    }
}
