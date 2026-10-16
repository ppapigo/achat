package com.achat.round;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class RoundConfig {

    private final RoundService roundService;
    private final RoundScheduler roundScheduler;

    @PostConstruct
    public void init() {
        roundScheduler.setRoundService(roundService);
    }
}
