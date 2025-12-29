package com.achat.room;

import com.achat.common.ApiResponse;
import com.achat.room.dto.CreateRoomRequest;
import com.achat.room.dto.CreateSubRoomRequest;
import com.achat.room.dto.RoomResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal String userId) {
        RoomResponse response = roomService.createRoom(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> listRooms() {
        List<RoomResponse> rooms = roomService.listRooms();
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@PathVariable String roomId) {
        RoomResponse response = roomService.getRoom(roomId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId) {
        RoomResponse response = roomService.joinRoom(roomId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<String>> leaveRoom(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId) {
        roomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok(ApiResponse.ok("방을 나갔습니다"));
    }

    // 하위 방 생성
    @PostMapping("/{roomId}/subrooms")
    public ResponseEntity<ApiResponse<RoomResponse>> createSubRoom(
            @PathVariable String roomId,
            @RequestBody CreateSubRoomRequest request,
            @AuthenticationPrincipal String userId) {
        RoomResponse response = roomService.createSubRoom(roomId, request.name(), userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 하위 방 목록
    @GetMapping("/{roomId}/subrooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getSubRooms(@PathVariable String roomId) {
        List<RoomResponse> subRooms = roomService.getSubRooms(roomId);
        return ResponseEntity.ok(ApiResponse.ok(subRooms));
    }

    // 하위 방 삭제
    @DeleteMapping("/{roomId}/subrooms/{subRoomId}")
    public ResponseEntity<ApiResponse<String>> deleteSubRoom(
            @PathVariable String roomId,
            @PathVariable String subRoomId,
            @AuthenticationPrincipal String userId) {
        roomService.deleteSubRoom(roomId, subRoomId, userId);
        return ResponseEntity.ok(ApiResponse.ok("하위 방이 삭제되었습니다"));
    }
}