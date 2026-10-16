package com.achat.sanction;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me/sanctions")
@RequiredArgsConstructor
public class SanctionController {

    private final SanctionService sanctionService;

    /**
     * 내 제재 이력 전체
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Sanction>>> getMySanctions(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<Sanction> sanctions = sanctionService.getUserSanctions(userId);
        return ResponseEntity.ok(ApiResponse.ok(sanctions));
    }

    /**
     * 내 활성 제재
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Sanction>>> getMyActiveSanctions(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<Sanction> sanctions = sanctionService.getActiveSanctions(userId);
        return ResponseEntity.ok(ApiResponse.ok(sanctions));
    }

    /**
     * 내 현재 상태 (채팅/접속 가능 여부)
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SanctionStatus>> getMyStatus(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");

        boolean canChat = sanctionService.canChat(userId);
        boolean canAccess = sanctionService.canAccess(userId);
        List<Sanction> activeSanctions = sanctionService.getActiveSanctions(userId);

        return ResponseEntity.ok(ApiResponse.ok(new SanctionStatus(canChat, canAccess, activeSanctions)));
    }

    public record SanctionStatus(
            boolean canChat,
            boolean canAccess,
            List<Sanction> activeSanctions
    ) {}
}
