package com.achat.match;

import com.achat.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinQueue(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "8") int playerCount) {
        Map<String, Object> result = matchService.joinQueue(userId, playerCount);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/leave")
    public ResponseEntity<ApiResponse<String>> leaveQueue(
            @AuthenticationPrincipal String userId) {
        matchService.leaveQueue(userId);
        return ResponseEntity.ok(ApiResponse.ok("매칭 취소됨"));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(
            @AuthenticationPrincipal String userId) {
        Map<String, Object> status = matchService.getQueueStatus(userId);
        String roomId = matchService.getMatchedRoom(userId);
        if (roomId != null) {
            status = new java.util.HashMap<>(status);
            status.put("matched", true);
            status.put("roomId", roomId);
        }
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}