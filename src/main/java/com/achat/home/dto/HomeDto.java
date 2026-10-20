package com.achat.home.dto;

import com.achat.home.Home;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;
import java.util.stream.Collectors;

public class HomeDto {

    // ========== 가구 위치 DTO ==========

    public record FurniturePositionDto(
            @Min(0) @Max(100) double x,
            @Min(0) @Max(100) double y
    ) {
        public static FurniturePositionDto from(Home.FurniturePosition pos) {
            return new FurniturePositionDto(pos.getX(), pos.getY());
        }

        public Home.FurniturePosition toEntity() {
            return new Home.FurniturePosition(x, y);
        }
    }

    // ========== 홈 응답 ==========

    public record HomeResponse(
            String ownerId,
            String nickname,
            String statusMessage,
            String backgroundTheme,
            String titleBadge,
            int visitCount,
            String avatarStyle,
            String avatarSeed,
            String avatarBgColor
    ) {
        // ✅ 2개 파라미터 버전
        public static HomeResponse from(Home home, String nickname) {
            return new HomeResponse(
                    home.getUserId(),
                    nickname,
                    home.getStatusMessage(),
                    home.getBackgroundTheme(),
                    home.getTitleBadge(),
                    (int) home.getVisitCount(),
                    home.getAvatarStyle(),
                    home.getAvatarSeed() != null ? home.getAvatarSeed() : home.getUserId(),
                    home.getAvatarBgColor()
            );
        }
    }

    // ========== 홈 업데이트 요청 ==========

    public record UpdateHomeRequest(
            @jakarta.validation.constraints.Size(max = 200) String statusMessage,
            @jakarta.validation.constraints.Size(max = 100) String backgroundTheme,
            @jakarta.validation.constraints.Size(max = 100) String titleBadge,
            @jakarta.validation.constraints.Size(max = 50) String avatarStyle,
            @jakarta.validation.constraints.Size(max = 100) String avatarSeed,
            @jakarta.validation.constraints.Pattern(regexp = "transparent|#?[0-9a-fA-F]{6}") String avatarBgColor
    ) {}

    // ========== 가구 배치만 업데이트 ==========

    public record UpdateFurnitureRequest(
            @Valid
            @jakarta.validation.constraints.Size(max = 50) Map<@jakarta.validation.constraints.Size(max = 64) String, FurniturePositionDto> layout
    ) {}

    // ========== 가구 배치 응답 ==========

    public record FurnitureLayoutResponse(
            Map<String, FurniturePositionDto> layout
    ) {
        public static FurnitureLayoutResponse from(Map<String, Home.FurniturePosition> layout) {
            if (layout == null) {
                return new FurnitureLayoutResponse(Map.of());
            }
            Map<String, FurniturePositionDto> dto = layout.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> FurniturePositionDto.from(e.getValue())
                    ));
            return new FurnitureLayoutResponse(dto);
        }
    }
}
