package com.achat.augment.effect;

import com.achat.augment.*;
import com.achat.message.Message;
import java.time.Instant;
import java.util.Set;

public interface MessageEffectHandler {
    Set<AugmentType> types();
    MessageEffect create(Input input);
    record Input(Message message, AugmentProgress progress, boolean eligible, Instant now,
                 AugmentRandom random, EffectRewards rewards) {}
}
