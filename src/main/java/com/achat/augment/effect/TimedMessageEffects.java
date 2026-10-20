package com.achat.augment.effect;

import com.achat.augment.*;
import org.springframework.stereotype.Component;
import java.util.Set;
import static com.achat.augment.AugmentPolicy.*;

@Component
public class TimedMessageEffects implements MessageEffectHandler {
    public Set<AugmentType> types() {
        return Set.of(AugmentType.TIME_CAPSULE, AugmentType.TIME_BOMB, AugmentType.DOPPELGANGER, AugmentType.MAIN_CHARACTER);
    }
    public MessageEffect create(Input input) {
        var type = input.progress().getType();
        return switch (type) {
            case TIME_CAPSULE -> new MessageEffect(type, "LOCKED", input.now().plusSeconds(CAPSULE_SECONDS), null, 0, 0, null, 0, 0);
            case TIME_BOMB -> new MessageEffect(type, "ACTIVE", null, input.now().plusSeconds(BOMB_SECONDS), 0, 0, null, 0, 0);
            case DOPPELGANGER -> new MessageEffect(type, "ACTIVE", input.now().plusSeconds(2), null, 0, 0, null, 0, 0);
            case MAIN_CHARACTER -> new MessageEffect(type, input.progress().getMessagesSeen() == 0 ? "ACTIVE" : "SPENT",
                    null, input.now().plusSeconds(MAIN_CHARACTER_SECONDS), 0, 0, null, 0, 0);
            default -> throw new IllegalArgumentException("지원하지 않는 시간 효과");
        };
    }
}
