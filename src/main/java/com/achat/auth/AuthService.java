package com.achat.auth;

import com.achat.auth.dto.*;
import com.achat.sanction.Sanction;
import com.achat.sanction.SanctionService;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AuthService {

    private final UserStore userStore;
    private final VerificationCodeStore codeStore;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final SanctionService sanctionService;  // ✅ 추가
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserStore userStore, VerificationCodeStore codeStore,
                       PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
                       SanctionService sanctionService) {  // ✅ 추가
        this.userStore = userStore;
        this.codeStore = codeStore;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.sanctionService = sanctionService;  // ✅ 추가
    }

    public AuthResponse register(RegisterRequest request) {
        if (userStore.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), passwordHash);
        userStore.save(user);

        String code = generateCode();
        codeStore.save(request.email(), code);

        System.out.println("========================================");
        System.out.println("[인증코드] " + request.email() + " : " + code);
        System.out.println("========================================");

        return new AuthResponse(user.getId(), user.getEmail(), false, "인증 코드가 발송되었습니다");
    }

    public AuthResponse verifyEmail(VerifyCodeRequest request) {
        User user = userStore.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다"));

        String validCode = codeStore.getValidCode(request.email())
                .orElseThrow(() -> new IllegalArgumentException("인증 코드가 만료되었거나 존재하지 않습니다"));

        if (!validCode.equals(request.code())) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다");
        }

        user.verifyEmail();
        codeStore.remove(request.email());

        return new AuthResponse(user.getId(), user.getEmail(), true, "이메일 인증이 완료되었습니다");
    }

    public void resendCode(String email) {
        User user = userStore.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 이메일입니다"));

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("이미 인증된 이메일입니다");
        }

        String code = generateCode();
        codeStore.save(email, code);

        System.out.println("========================================");
        System.out.println("[인증코드 재발송] " + email + " : " + code);
        System.out.println("========================================");
    }

    public TokenResponse login(LoginRequest request) {
        User user = userStore.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 틀렸습니다");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다");
        }

        // ✅ 정지 상태 체크
        checkBanStatus(user.getId());

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getEmail());

        return new TokenResponse(accessToken, refreshToken, user.getId(), user.getEmail());
    }

    /**
     * ✅ 정지 상태 체크
     */
    private void checkBanStatus(String userId) {
        if (!sanctionService.isBanned(userId)) {
            return; // 정지 아님
        }

        // 활성 제재 중 BAN 타입 찾기
        List<Sanction> activeSanctions = sanctionService.getActiveSanctions(userId);

        for (Sanction sanction : activeSanctions) {
            String type = sanction.getType().name();

            if ("PERM_BAN".equals(type)) {
                throw new IllegalArgumentException(
                        "계정이 영구 정지되었습니다.\n사유: " + sanction.getReason()
                );
            }

            if ("TEMP_BAN".equals(type)) {
                String remaining = formatRemaining(sanction.getExpiresAt());
                throw new IllegalArgumentException(
                        "계정이 임시 정지되었습니다.\n" +
                                "남은 시간: " + remaining + "\n" +
                                "사유: " + sanction.getReason()
                );
            }
        }
    }

    /**
     * ✅ 남은 시간 포맷
     */
    private String formatRemaining(Instant expiresAt) {
        if (expiresAt == null) return "영구";

        Duration duration = Duration.between(Instant.now(), expiresAt);
        if (duration.isNegative()) return "만료됨";

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 24) {
            return (hours / 24) + "일 " + (hours % 24) + "시간";
        } else if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        } else {
            return minutes + "분";
        }
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다");
        }

        if (!"refresh".equals(jwtProvider.getTokenType(refreshToken))) {
            throw new IllegalArgumentException("Refresh 토큰이 아닙니다");
        }

        String userId = jwtProvider.getUserId(refreshToken);

        // ✅ 리프레시 시에도 정지 체크
        checkBanStatus(userId);

        User user = userStore.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId(), user.getEmail());

        return new TokenResponse(newAccessToken, newRefreshToken, user.getId(), user.getEmail());
    }

    public UserInfoResponse getUserInfo(String userId) {
        User user = userStore.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));
        return UserInfoResponse.from(user);
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1000000));
    }
}
