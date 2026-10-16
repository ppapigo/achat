package com.achat.season;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentSeason() {
        SeasonService.SeasonInfo info = seasonService.getCurrentSeasonInfo();
        if (info == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of(
                "seasonId", info.seasonId(),
                "name", info.name(),
                "startDate", info.startDate().toString(),
                "endDate", info.endDate().toString(),
                "remainingDays", info.remainingDays(),
                "active", info.active(),
                "progressPercent", info.progressPercent()
        ));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSeasons() {
        List<Map<String, Object>> seasons = seasonService.getAllSeasons().stream()
                .map(s -> Map.<String, Object>of(
                        "seasonId", s.getId(), "name", s.getName(),
                        "startDate", s.getStartDate().toString(),
                        "endDate", s.getEndDate().toString(),
                        "active", s.isActive(), "remainingDays", s.getRemainingDays()
                )).toList();
        return ResponseEntity.ok(seasons);
    }

    @PostMapping("/end")
    public ResponseEntity<?> endSeason() {
        try {
            SeasonService.SeasonEndResult result = seasonService.endCurrentSeason();
            return ResponseEntity.ok(Map.of(
                    "message", "시즌이 종료되었습니다",
                    "endedSeasonId", result.endedSeasonId(),
                    "newSeasonId", result.newSeasonId(),
                    "usersProcessed", result.usersProcessed(),
                    "totalRewardsGiven", result.totalRewardsGiven(),
                    "roomsReset", result.roomsReset()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<UserSeasonHistory>> getMyHistory(@RequestAttribute("userId") String userId) {
        return ResponseEntity.ok(seasonService.getUserSeasonHistory(userId));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<UserSeasonHistory>> getUserHistory(@PathVariable String userId) {
        return ResponseEntity.ok(seasonService.getUserSeasonHistory(userId));
    }
}
