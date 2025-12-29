package com.achat.home;

import com.achat.common.ApiResponse;
import com.achat.home.dto.HomeDto.*;
import com.achat.user.User;
import com.achat.user.UserStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Home API (미니홈피)
 */
@RestController
@RequestMapping("/api")
public class HomeController {

    private final HomeService homeService;
    private final UserStore userStore;

    public HomeController(HomeService homeService, UserStore userStore) {
        this.homeService = homeService;
        this.userStore = userStore;
    }

    // ✅ 닉네임 조회 헬퍼
    private String getNickname(String userId) {
        return userStore.findById(userId)
                .map(User::getUsername)
                .orElse("User_" + userId.substring(0, Math.min(4, userId.length())));
    }

    /**
     * 내 홈 조회
     */
    @GetMapping("/me/home")
    public ResponseEntity<ApiResponse<HomeResponse>> getMyHome(
            @RequestAttribute("userId") String userId
    ) {
        String nickname = getNickname(userId);
        HomeResponse home = homeService.getMyHome(userId, nickname);
        return ResponseEntity.ok(ApiResponse.ok(home));
    }

    /**
     * 내 홈 수정
     */
    @PatchMapping("/me/home")
    public ResponseEntity<ApiResponse<HomeResponse>> updateMyHome(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody UpdateHomeRequest request
    ) {
        String nickname = getNickname(userId);
        HomeResponse home = homeService.updateMyHome(userId, request, nickname);
        return ResponseEntity.ok(ApiResponse.ok(home));
    }

    /**
     * 특정 유저 홈 조회 (방문)
     */
    @GetMapping("/users/{targetUserId}/home")
    public ResponseEntity<ApiResponse<HomeResponse>> getUserHome(
            @PathVariable String targetUserId,
            @RequestAttribute("userId") String viewerId
    ) {
        String nickname = getNickname(targetUserId);
        HomeResponse home = homeService.getHome(targetUserId, viewerId, nickname);
        return ResponseEntity.ok(ApiResponse.ok(home));
    }

    // ========== 가구 배치 API ==========

    /**
     * 내 가구 배치 조회
     */
    @GetMapping("/me/furniture")
    public ResponseEntity<ApiResponse<FurnitureLayoutResponse>> getMyFurniture(
            @RequestAttribute("userId") String userId
    ) {
        FurnitureLayoutResponse layout = homeService.getFurnitureLayout(userId);
        return ResponseEntity.ok(ApiResponse.ok(layout));
    }

    /**
     * 내 가구 배치 저장
     */
    @PutMapping("/me/furniture")
    public ResponseEntity<ApiResponse<FurnitureLayoutResponse>> saveMyFurniture(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody UpdateFurnitureRequest request
    ) {
        FurnitureLayoutResponse layout = homeService.saveFurnitureLayout(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(layout));
    }

    /**
     * 다른 유저 가구 배치 조회
     */
    @GetMapping("/users/{targetUserId}/furniture")
    public ResponseEntity<ApiResponse<FurnitureLayoutResponse>> getUserFurniture(
            @PathVariable String targetUserId
    ) {
        FurnitureLayoutResponse layout = homeService.getUserFurnitureLayout(targetUserId);
        return ResponseEntity.ok(ApiResponse.ok(layout));
    }
}