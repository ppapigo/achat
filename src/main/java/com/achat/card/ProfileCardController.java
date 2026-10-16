package com.achat.card;

import lombok.RequiredArgsConstructor;

import com.achat.card.dto.DrawResultResponse;
import com.achat.card.dto.ProfileCardResponse;
import com.achat.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class ProfileCardController {

    private final ProfileCardService profileCardService;

    // 카드 드로우
    @PostMapping("/draw")
    public ApiResponse<DrawResultResponse> drawCard(@AuthenticationPrincipal String userId) {
        DrawResultResponse result = profileCardService.drawCard(userId);
        return ApiResponse.ok(result);
    }

    // 드로우 상태 조회
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getDrawStatus(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(profileCardService.getDrawStatus(userId));
    }

    // 내가 뽑은 카드 목록
    @GetMapping("/my")
    public ApiResponse<List<ProfileCardResponse>> getMyCards(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(profileCardService.getMyCards(userId));
    }
}
