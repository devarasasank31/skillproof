package com.skillproof.security;

import com.skillproof.config.JwtProperties;
import com.skillproof.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final String TYPE_CLAIM = "typ";
    private static final String UID_CLAIM = "uid";

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(Long userId) {
        return issue(userId, "access", props.accessTokenMinutes() * 60L);
    }

    public String issueRefreshToken(Long userId) {
        return issue(userId, "refresh", props.refreshTokenDays() * 86400L);
    }

    private String issue(Long userId, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claim(UID_CLAIM, userId)
                .claim(TYPE_CLAIM, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
                throw ApiException.unauthorized("Invalid token type");
            }
            Long uid = claims.get(UID_CLAIM, Long.class);
            if (uid == null) throw ApiException.unauthorized("Malformed token");
            return uid;
        } catch (ApiException e) {
            throw e;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new ApiException(401, "TOKEN_EXPIRED", "Token has expired");
        } catch (RuntimeException e) {
            throw new ApiException(401, "INVALID_TOKEN", "Invalid or expired token");
        }
    }
}
