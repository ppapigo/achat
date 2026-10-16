package com.achat.home;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.home.dto.HomeDto.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class HomeService {

    private final HomeStore homeStore;

    /**
     * 내 홈 조회
     */
    public HomeResponse getMyHome(String userId, String nickname) {
        Home home = homeStore.findOrCreate(userId);
        return HomeResponse.from(home, nickname);
    }

    /**
     * 특정 유저 홈 조회 (방문)
     */
    public HomeResponse getHome(String targetUserId, String viewerId, String nickname) {
        Home home = homeStore.findOrCreate(targetUserId);

        // 본인이 아니면 방문수 증가
        if (!targetUserId.equals(viewerId)) {
            home.incrementVisitCount();
        }

        return HomeResponse.from(home, nickname);
    }

    /**
     * 내 홈 수정 (상태메시지, 테마, 아바타 등)
     */
    public HomeResponse updateMyHome(String userId, UpdateHomeRequest request, String nickname) {
        Home home = homeStore.findOrCreate(userId);

        if (request.statusMessage() != null) {
            home.updateStatusMessage(request.statusMessage());
        }
        if (request.backgroundTheme() != null) {
            home.updateBackgroundTheme(request.backgroundTheme());
        }
        if (request.titleBadge() != null) {
            home.updateTitleBadge(request.titleBadge());
        }
        // ✅ 아바타 업데이트
        if (request.avatarStyle() != null) {
            home.setAvatarStyle(request.avatarStyle());
        }
        if (request.avatarSeed() != null) {
            home.setAvatarSeed(request.avatarSeed());
        }
        if (request.avatarBgColor() != null) {
            home.setAvatarBgColor(request.avatarBgColor());
        }

        homeStore.save(home);
        return HomeResponse.from(home, nickname);
    }

    // ========== 가구 배치 전용 API ==========

    /**
     * 가구 배치 조회
     */
    public FurnitureLayoutResponse getFurnitureLayout(String userId) {
        Home home = homeStore.findOrCreate(userId);
        return FurnitureLayoutResponse.from(home.getFurnitureLayout());
    }

    /**
     * 가구 배치 저장
     */
    public FurnitureLayoutResponse saveFurnitureLayout(String userId, UpdateFurnitureRequest request) {
        Home home = homeStore.findOrCreate(userId);

        if (request.layout() != null && !request.layout().isEmpty()) {
            Map<String, Home.FurniturePosition> layout = request.layout().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().toEntity()
                    ));
            home.updateFurnitureLayout(layout);
            homeStore.save(home);
        }

        return FurnitureLayoutResponse.from(home.getFurnitureLayout());
    }

    /**
     * 다른 유저 가구 배치 조회 (방문용)
     */
    public FurnitureLayoutResponse getUserFurnitureLayout(String targetUserId) {
        Home home = homeStore.findOrCreate(targetUserId);
        return FurnitureLayoutResponse.from(home.getFurnitureLayout());
    }

    // ========== ShopService에서 호출하는 메서드 ==========

    /**
     * 배경 테마 업데이트 (ShopService에서 호출)
     */
    public void updateBackgroundTheme(String userId, String theme) {
        Home home = homeStore.findOrCreate(userId);
        home.updateBackgroundTheme(theme);
        homeStore.save(home);
    }

    /**
     * 칭호 업데이트 (ShopService에서 호출)
     */
    public void updateTitleBadge(String userId, String badge) {
        Home home = homeStore.findOrCreate(userId);
        home.updateTitleBadge(badge);
        homeStore.save(home);
    }
}
