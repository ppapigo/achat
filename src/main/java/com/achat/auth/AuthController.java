package com.achat.auth;

import com.achat.auth.dto.*;
import com.achat.common.ApiResponse;
import com.achat.user.User;
import com.achat.user.UserStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserStore userStore;  // ✅ 추가

    public AuthController(AuthService authService, UserStore userStore) {  // ✅ 수정
        this.authService = authService;
        this.userStore = userStore;  // ✅ 추가
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verify(@Valid @RequestBody VerifyCodeRequest request) {
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<ApiResponse<String>> resendCode(@RequestParam String email) {
        authService.resendCode(email);
        return ResponseEntity.ok(ApiResponse.ok("인증 코드가 재발송되었습니다"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response));
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @RequestAttribute("userId") String userId,
            @RequestBody Map<String, String> request
    ) {
        String newUsername = request.get("username");

        if (newUsername == null || newUsername.trim().length() < 2) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("닉네임은 2자 이상이어야 합니다"));
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