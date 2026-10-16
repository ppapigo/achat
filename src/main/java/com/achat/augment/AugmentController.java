package com.achat.augment;

import lombok.RequiredArgsConstructor;

import com.achat.augment.dto.AugmentOptionResponse;
import com.achat.augment.dto.PickAugmentRequest;
import com.achat.augment.dto.ApplyTestRequest;
import com.achat.augment.dto.ApplyTestResponse;
import com.achat.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms/{roomId}/augments")
@RequiredArgsConstructor
public class AugmentController {

    private final AugmentService augmentService;

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<AugmentOptionResponse>> getOptions(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId) {
        AugmentOptionResponse response = augmentService.getOptions(roomId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/pick")
    public ResponseEntity<ApiResponse<AugmentOptionResponse>> pick(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody PickAugmentRequest request) {
        AugmentOptionResponse response = augmentService.pick(roomId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/test-apply")
    public ResponseEntity<ApiResponse<ApplyTestResponse>> testApply(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ApplyTestRequest request) {
        String result = augmentService.applyAugment(roomId, userId, request.message());
        return ResponseEntity.ok(ApiResponse.ok(new ApplyTestResponse(request.message(), result)));
    }
}
