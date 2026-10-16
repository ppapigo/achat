package com.achat.roomrank;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import com.achat.roomrank.RoomRankService.RoomRankResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms/rank")
@RequiredArgsConstructor
public class RoomRankController {

    private final RoomRankService roomRankService;

    /**
     * 🏆 전체 TOP
     */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<RoomRankResponse>>> getTopRooms(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RoomRankResponse> rooms = roomRankService.getTopRooms(limit);
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    /**
     * 🔥 24시간 HOT
     */
    @GetMapping("/hot")
    public ResponseEntity<ApiResponse<List<RoomRankResponse>>> getHotRooms(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RoomRankResponse> rooms = roomRankService.getHotRooms(limit);
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    /**
     * 🌱 성장중 (생성 7일 이내)
     */
    @GetMapping("/rising")
    public ResponseEntity<ApiResponse<List<RoomRankResponse>>> getRisingRooms(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RoomRankResponse> rooms = roomRankService.getRisingRooms(limit);
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    /**
     * 👥 소규모 방 (≤ 20명)
     */
    @GetMapping("/small")
    public ResponseEntity<ApiResponse<List<RoomRankResponse>>> getSmallRooms(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RoomRankResponse> rooms = roomRankService.getSmallRooms(limit);
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    /**
     * 👑 시즌 랭킹
     */
    @GetMapping("/season")
    public ResponseEntity<ApiResponse<List<RoomRankResponse>>> getSeasonRooms(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<RoomRankResponse> rooms = roomRankService.getSeasonRooms(limit);
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }
}
