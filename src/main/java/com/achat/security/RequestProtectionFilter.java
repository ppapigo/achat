package com.achat.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.time.Duration;
import java.util.*;

@Component
public class RequestProtectionFilter extends OncePerRequestFilter {
    private final RateLimitService limits;
    private final List<IpAddressMatcher> trustedProxies;
    public RequestProtectionFilter(RateLimitService limits, @Value("${app.security.trusted-proxies:}") String proxies) {
        this.limits = limits;
        trustedProxies = Arrays.stream(proxies.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(IpAddressMatcher::new).toList();
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String ip = request.getRemoteAddr();
            if (trustedProxies.stream().anyMatch(proxy -> proxy.matches(request.getRemoteAddr()))) {
                String forwarded = request.getHeader("X-Real-IP");
                if (forwarded != null && forwarded.matches("[0-9a-fA-F:.]{3,45}")) ip = forwarded;
            }
            String path = request.getServletPath();
            if (path.isEmpty()) path = request.getRequestURI();
            if ("POST".equals(request.getMethod())) {
                if (path.equals("/auth/register")) limits.consume("signup-ip:" + ip, 5, Duration.ofMinutes(10));
                if (path.equals("/auth/login")) limits.consume("login-ip:" + ip, 30, Duration.ofMinutes(1));
                if (path.equals("/auth/refresh") || path.equals("/auth/logout"))
                    limits.consume("session-ip:" + ip, 60, Duration.ofMinutes(1));
            }
            if (path.equals("/ws")) limits.consume("ws-ip:" + ip, 30, Duration.ofMinutes(1));
            if (Set.of("POST", "PUT", "PATCH").contains(request.getMethod())) {
                // Read at most 64 KiB + 1, including chunked requests. Never log the body.
                byte[] body = request.getInputStream().readNBytes(65_537);
                if (body.length > 65_536) {
                    SecurityResponses.write(response, 413, "요청 본문이 너무 큽니다");
                    return;
                }
                chain.doFilter(new BufferedRequest(request, body), response);
            } else chain.doFilter(request, response);
        } catch (RateLimitService.RateLimitedException e) {
            response.setHeader("Retry-After", Long.toString(e.retryAfter));
            SecurityResponses.write(response, 429, e.getMessage());
        }
    }
    private static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        BufferedRequest(HttpServletRequest request, byte[] body) { super(request); this.body = body; }
        @Override public ServletInputStream getInputStream() {
            ByteArrayInputStream stream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return stream.read(); }
                @Override public int read(byte[] bytes, int offset, int length) { return stream.read(bytes, offset, length); }
                @Override public boolean isFinished() { return stream.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
            };
        }
        @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8)); }
    }
}
