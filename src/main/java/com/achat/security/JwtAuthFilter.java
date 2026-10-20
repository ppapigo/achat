package com.achat.security;

import lombok.RequiredArgsConstructor;

import com.achat.auth.AuthSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthSessionService sessions;
    private final RateLimitService limits;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // permitAll 경로만 스킵 (개별 지정!)
        if (path.equals("/auth/login") ||
                path.equals("/auth/register") ||
                path.equals("/auth/refresh") ||
                path.equals("/auth/logout") ||
                path.startsWith("/api/reports/reasons/") ||
                path.equals("/api/seasons/current") ||
                path.equals("/ws") || path.startsWith("/ws/") || path.equals("/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                var identity = sessions.authenticate(token);
                var user = sessions.validate(identity);
                String userId = identity.userId();
                request.setAttribute("userId", userId);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userId, null, user.isAdmin()
                                ? List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")) : List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
                limits.consume("api:" + userId, 600, java.time.Duration.ofMinutes(1));
                if (!List.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
                    limits.consume("mutation:" + userId, 120, java.time.Duration.ofMinutes(1));
                    if (path.equals("/rooms") || path.equals("/api/party/create") || path.matches("/rooms/[^/]+/subrooms"))
                        limits.consume("room-create:" + userId, 5, java.time.Duration.ofMinutes(1));
                    if (path.matches("/rooms/[^/]+/join")) limits.consume("room-join:" + userId, 30, java.time.Duration.ofMinutes(1));
                    if (path.startsWith("/api/reports")) limits.consume("report:" + userId, 10, java.time.Duration.ofMinutes(10));
                }
            } catch (UnauthorizedException e) {
                SecurityResponses.write(response, 401, e.getMessage()); return;
            } catch (org.springframework.security.access.AccessDeniedException e) {
                SecurityResponses.write(response, 403, e.getMessage()); return;
            } catch (RateLimitService.RateLimitedException e) {
                response.setHeader("Retry-After", Long.toString(e.retryAfter));
                SecurityResponses.write(response, 429, e.getMessage()); return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
