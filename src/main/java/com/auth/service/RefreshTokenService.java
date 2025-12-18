package com.auth.service;

import com.auth.dto.TokenResponse;
import com.auth.entity.RefreshToken;
import com.auth.entity.User;
import com.auth.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieService cookieService;
    private final TokenResponseService tokenResponseService;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .jti(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        RefreshToken newToken = RefreshToken.builder()
                .jti(UUID.randomUUID().toString())
                .user(oldToken.getUser())
                .expiresAt(oldToken.getExpiresAt())
                .revoked(false)
                .build();

        oldToken.setRevoked(true);
        oldToken.setReplacedByToken(newToken.getJti());
        refreshTokenRepository.save(oldToken);

        return refreshTokenRepository.save(newToken);
    }

    @Transactional
    public TokenResponse renewAccessToken(HttpServletRequest request, HttpServletResponse response) {

        String refreshTokenJwt = cookieService.getRefreshTokenJwt(request)
                .orElseThrow(() -> new BadCredentialsException("Refresh token missing"));

        RefreshToken storedToken = validateAndGetRefreshToken(refreshTokenJwt);
        RefreshToken newToken = rotateRefreshToken(storedToken);

        User user = newToken.getUser();
        TokenResponse tokenResponse =
                tokenResponseService.createTokenAndBuildResponse(user, newToken.getJti(), response);
        return tokenResponse;
    }

    public RefreshToken validateAndGetRefreshToken(String refreshTokenJwt) {

        if (!jwtService.isRefreshToken(refreshTokenJwt)) {
            throw new BadCredentialsException("Invalid refresh token type");
        }
        String jti = jwtService.getJti(refreshTokenJwt);
        UUID userId = jwtService.getUserId(refreshTokenJwt);

        RefreshToken storedToken = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not recognized"));

        if (storedToken.isRevoked()) {
//            refreshTokenRepository.revokeAllByUser(storedToken.getUser());
            throw new BadCredentialsException("Refresh token reused");
        }
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }
        if (!storedToken.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Token does not belong to user");
        }
        return storedToken;
    }
}
