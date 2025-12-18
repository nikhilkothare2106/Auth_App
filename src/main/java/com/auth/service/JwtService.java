package com.auth.service;

import com.auth.entity.Role;
import com.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Getter
@Setter
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(
            @Value("${security.jwt.secret}") String secretKey,
            @Value("${security.jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
            @Value("${security.jwt.issuer}") String issuer) {

        if (secretKey == null || secretKey.isBlank() || secretKey.length() < 64) {
            throw new IllegalArgumentException("Invalid JWT Secret Key");
        }

        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
    }

    //generate access token
    public String generateAccessToken(User user) {

        List<String> roles = user.getRoles() == null ? List.of() :
                user.getRoles().stream().map(Role::getName).toList();
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claim("userId", user.getId().toString())
                .claim("roles", roles)
                .claim("typ", "access")
                .signWith(secretKey)
                .compact();
    }

    //generate refresh token
    public String generateRefreshToken(User user, String jti) {

        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .subject(user.getEmail())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("userId", user.getId().toString())
                .claim("typ", "refresh")
                .signWith(secretKey)
                .compact();
    }

    // parse the token
    public Jws<Claims> parse(String token) {
        try {
            return Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token has expired, " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            throw new JwtException("Unsupported JWT token, " + e.getMessage());
        } catch (MalformedJwtException e) {
            throw new JwtException("Malformed JWT token, " + e.getMessage());
        } catch (SignatureException e) {
            throw new JwtException("Invalid JWT signature, " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new JwtException("JWT claims string is empty, " + e.getMessage());
        }
    }

    public boolean isAccessToken(String token) {
        Claims payload = parse(token).getPayload();
        return "access".equals(payload.get("typ"));
    }

    public boolean isRefreshToken(String token) {
        Claims payload = parse(token).getPayload();
        return "refresh".equals(payload.get("typ"));
    }

    public UUID getUserId(String token) {
        Claims payload = parse(token).getPayload();
        String userId = (String) payload.get("userId");
        if (userId == null) {
            throw new JwtException("User Id claim not found in token");
        }
        return UUID.fromString(userId);
    }

    public String getJti(String token) {
        return parse(token).getPayload().getId();
    }

    public String getUsername(String token) {
        Jws<Claims> parse = this.parse(token);
        Claims payload = parse.getPayload();
        return payload.getSubject();
    }
}
