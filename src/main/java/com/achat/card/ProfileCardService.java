package com.achat.card;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.card.dto.DrawResultResponse;
import com.achat.card.dto.ProfileCardResponse;
import com.achat.friend.FriendService;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileCardService {

    private final UserStore userStore;
    private final FriendService friendService;
    private final ProfileCardRepository repository;

    // 오늘 이미 뽑았는지 확인
    public boolean hasDrawnToday(String userId) {
        LocalDate today = LocalDate.now();
        return repository.existsByOwnerIdAndDrawnDate(userId, today);
    }

    // 카드 드로우
    public DrawResultResponse drawCard(String userId) {
        // 오늘 이미 뽑았으면 실패
        if (hasDrawnToday(userId)) {
            return DrawResultResponse.alreadyDrawn();
        }

        // 뽑을 수 있는 유저 목록 (본인 제외, 이미 뽑은 유저 제외)
        Set<String> alreadyDrawnIds = repository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(ProfileCard::getTargetId)
                .collect(Collectors.toSet());

        List<User> availableUsers = userStore.findAll().stream()
                .filter(u -> !u.getId().equals(userId))            // 본인 제외
                .filter(u -> !alreadyDrawnIds.contains(u.getId())) // 이미 뽑은 유저 제외
                .collect(Collectors.toList());

        if (availableUsers.isEmpty()) {
            return DrawResultResponse.noUsersAvailable();
        }

        // 랜덤 선택
        Random random = new Random();
        User targetUser = availableUsers.get(random.nextInt(availableUsers.size()));

        // 카드 생성
        ProfileCard card = repository.save(new ProfileCard(null, userId, targetUser.getId()));
        Long cardId = card.getId();

        // 친구 여부 확인 (areFriends 사용)
        boolean isFriend = friendService.areFriends(userId, targetUser.getId());

        // 온라인 여부
        boolean isOnline = friendService.isOnline(targetUser.getId());

        ProfileCardResponse response = ProfileCardResponse.from(
                cardId,
                targetUser,
                isOnline,
                isFriend,
                card.getDrawnDate().toString()
        );

        return DrawResultResponse.success(response);
    }

    // 내가 뽑은 카드 목록
    public List<ProfileCardResponse> getMyCards(String userId) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .sorted(Comparator.comparing(ProfileCard::getCreatedAt).reversed())
                .map(card -> {
                    User targetUser = userStore.findById(card.getTargetId()).orElse(null);
                    if (targetUser == null) return null;

                    boolean isFriend = friendService.areFriends(userId, targetUser.getId());
                    boolean isOnline = friendService.isOnline(targetUser.getId());

                    return ProfileCardResponse.from(
                            card.getId(),
                            targetUser,
                            isOnline,
                            isFriend,
                            card.getDrawnDate().toString()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 오늘 남은 드로우 횟수
    public int getRemainingDraws(String userId) {
        return hasDrawnToday(userId) ? 0 : 1;
    }

    // 드로우 상태 조회
    public Map<String, Object> getDrawStatus(String userId) {
        boolean drawnToday = hasDrawnToday(userId);
        int totalCards = Math.toIntExact(repository.countByOwnerId(userId));

        return Map.of(
                "drawnToday", drawnToday,
                "remainingDraws", drawnToday ? 0 : 1,
                "totalCards", totalCards
        );
    }
}
