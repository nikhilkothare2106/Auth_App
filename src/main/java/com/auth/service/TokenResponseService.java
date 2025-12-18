package com.auth.service;

import com.auth.dto.TokenResponse;
import com.auth.entity.User;
import com.auth.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TokenResponseService {

    private final CookieService cookieService;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public TokenResponse createTokenAndBuildResponse(User user, String jti, HttpServletResponse response) {

        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenJwt = jwtService.generateRefreshToken(user, jti);

        cookieService.attachRefreshCookie(response, refreshTokenJwt, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoStoreHeaders(response);

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenJwt)
                .expiresIn(jwtService.getAccessTtlSeconds())
                .user(userMapper.mapToUserDto(user))
                .tokenType("access")
                .build();
        return tokenResponse;
    }
}
