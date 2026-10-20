package com.achat.auth;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.auth.dto.*;
import com.achat.common.ApiResponse;
import com.achat.user.User;
import com.achat.user.UserStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserStore userStore;  // ✅ 추가

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record ChangePasswordRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String currentPassword,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 8, max = 72) String newPassword) {}

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestAttribute("userId") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 내 정보 조회
    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMe(@RequestAttribute("userId") String userId) {
        UserInfoResponse response = authService.getUserInfo(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 프로필 수정
     */
    @PatchMapping("/profile")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @RequestAttribute("userId") String userId,
            @RequestBody Map<String, String> request
    ) {
        String newUsername = request.get("username");

        if (newUsername == null || newUsername.trim().length() < 2 || newUsername.trim().length() > 50) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("닉네임은 2~50자여야 합니다"));
        }

        // 중복 체크
        if (userStore.findByUsername(newUsername.trim()).isPresent()) {
            User existing = userStore.findByUsername(newUsername.trim()).get();
            if (!existing.getId().equals(userId)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("이미 사용 중인 닉네임입니다"));
            }
        }

        User user = userStore.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));

        user.setUsername(newUsername.trim());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "프로필이 수정되었습니다",
                "username", user.getUsername()
        )));
    }
}
