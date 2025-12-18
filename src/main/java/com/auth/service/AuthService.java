package com.auth.service;

import com.auth.dto.LoginRequest;
import com.auth.dto.TokenResponse;
import com.auth.dto.UserDto;
import com.auth.entity.RefreshToken;
import com.auth.entity.User;
import com.auth.repository.RefreshTokenRepository;
import com.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenResponseService tokenResponseService;
    private final CookieService cookieService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserDto registerUser(UserDto userDto) {
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        return userService.createUser(userDto);
    }

    public TokenResponse loginUser(LoginRequest loginRequest, HttpServletResponse response) {
        authenticateUser(loginRequest);

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        TokenResponse tokenResponse = tokenResponseService.
                createTokenAndBuildResponse(user, refreshToken.getJti(), response);
        return tokenResponse;
    }

    public Authentication authenticateUser(LoginRequest loginRequest) {

        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("User or Password invalid!");
        }

    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieService.getRefreshTokenJwt(request).ifPresent(token -> {
            try {
                if(jwtService.isRefreshToken(token)) {
                    String jti = jwtService.getJti(token);
                    refreshTokenRepository.findByJti(jti).ifPresent(refreshToken -> {
                        refreshToken.setRevoked(true);
                        refreshTokenRepository.save(refreshToken);
                    });
                }
            } catch (Exception e) {
            }
        });

        cookieService.clearRefreshCookie(response);
        cookieService.addNoStoreHeaders(response);
        SecurityContextHolder.clearContext();
    }
}
