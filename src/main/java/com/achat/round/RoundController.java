package com.achat.round;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import com.achat.round.dto.RoundResponse;
import com.achat.round.dto.StartRoundRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms/{roomId}/round")
@RequiredArgsConstructor
public class RoundController {

    private final RoundService roundService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<RoundResponse>> startRound(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody StartRoundRequest request) {
        RoundResponse response = roundService.startRound(roomId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RoundResponse>> getRoundState(@PathVariable String roomId) {
        RoundResponse response = roundService.getRoundState(roomId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
