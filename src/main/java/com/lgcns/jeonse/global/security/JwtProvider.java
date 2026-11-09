package com.lgcns.jeonse.global.security;

import com.lgcns.jeonse.global.exception.ApiException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 회원 JWT 발급·검증 (docs/03 6.1). subject = 회원 id, 유효 24시간. */
@Component
public class JwtProvider {
    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);
    private static final Duration VALIDITY = Duration.ofHours(24);
    private final SecretKey key;

    public JwtProvider(@Value("${jwt.secret:}") String secret) {
        if (secret.isBlank()) {
            log.warn("JWT_SECRET이 없어 임시 서명 키를 만들었어요. 서버를 다시 켜면 기존 로그인이 풀려요. 로컬 개발이 아니면 JWT_SECRET을 설정하세요.");
            key = Jwts.SIG.HS256.key().build();
        } else {
            // 32바이트 미만이면 WeakKeyException으로 서버 시작이 실패한다 (약한 키로 운영하지 않게)
            key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String issue(long memberId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(VALIDITY)))
                .signWith(key, Jwts.SIG.HS256) // 키 길이와 상관없이 docs/03 6.1의 HS256으로 고정
                .compact();
    }

    /** 서명이 틀리거나 만료됐거나 형식이 잘못되면 401. */
    public long parse(String token) {
        try {
            return Long.parseLong(Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw ApiException.unauthorized();
        }
    }
}
