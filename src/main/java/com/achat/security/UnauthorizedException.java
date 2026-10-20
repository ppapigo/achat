package com.achat.security;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() { super("인증 정보가 만료되었거나 유효하지 않습니다"); }
    public UnauthorizedException(String message) { super(message); }
}
