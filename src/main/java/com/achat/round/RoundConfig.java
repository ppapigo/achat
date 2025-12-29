package com.achat.round;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class RoundConfig {

    private final RoundService roundService;
    private final RoundScheduler roundScheduler;

    public RoundConfig(RoundService roundService, RoundScheduler roundScheduler) {
        this.roundService = roundService;
        this.roundScheduler = roundScheduler;
    }

    @PostConstruct
    public void init() {
        roundScheduler.setRoundService(roundService);
    }
}