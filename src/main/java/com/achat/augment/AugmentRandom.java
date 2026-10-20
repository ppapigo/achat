package com.achat.augment;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class AugmentRandom {
    private final SecureRandom random = new SecureRandom();
    public int nextInt(int bound) { return random.nextInt(bound); }
}
