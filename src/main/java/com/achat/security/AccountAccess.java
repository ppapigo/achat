package com.achat.security;

import com.achat.sanction.SanctionService;
import com.achat.user.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountAccess {
    private final UserStore users;
    private final SanctionService sanctions;
    public void active(String userId) {
        if (userId == null || users.findById(userId).isEmpty()) throw new UnauthorizedException();
        if (sanctions.isBanned(userId)) throw new AccessDeniedException("정지된 계정입니다");
    }
    public void admin(String userId) {
        active(userId);
        if (!users.findById(userId).orElseThrow().isAdmin()) throw new AccessDeniedException("관리자 권한이 필요합니다");
    }
}
