package com.auth.security;

import com.auth.dto.TokenResponse;
import com.auth.entity.Provider;
import com.auth.entity.RefreshToken;
import com.auth.entity.User;
import com.auth.repository.UserRepository;
import com.auth.service.RefreshTokenService;
import com.auth.service.TokenResponseService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenResponseService tokenResponseService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.info("Authentication successful");
        log.info(authentication.toString());

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId = extractRegistrationId(authentication);
        log.info("Registration Id: " + registrationId);
        log.info("User : " + oAuth2User.getAttributes().toString());


        User user = null;
        switch (registrationId) {
            case "google" -> {
                String googleId = oAuth2User.getAttributes().getOrDefault("sub","").toString();
                String email = oAuth2User.getAttributes().getOrDefault("email","").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name","").toString();
                String picture = oAuth2User.getAttributes().getOrDefault("picture","").toString();

                user = findOrCreateUser(email,name,picture,Provider.GOOGLE,googleId);

            }

            case "github" -> {
                String githubId = oAuth2User.getAttributes().getOrDefault("id","").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name","").toString();
                String email = oAuth2User.getAttributes().get("email").toString() != null
                        ? oAuth2User.getAttributes().get("email").toString()
                        : oAuth2User.getAttributes().get("login").toString() + "@github.com";
                String picture = oAuth2User.getAttributes().getOrDefault("avatar_url","").toString();

                user = findOrCreateUser(email,name,picture,Provider.GITHUB,githubId);
            }

            default -> {
                throw new RuntimeException("Invalid registration id");
            }
        }

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        TokenResponse tokenResponse = tokenResponseService.createTokenAndBuildResponse(user, refreshToken.getJti(), response);

        response.getWriter().write("Authentication successful");

    }

    private String extractRegistrationId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId();
        }
        throw new IllegalStateException("Authentication is not an OAuth2AuthenticationToken");
    }

    private User findOrCreateUser(String email, String name, String picture, Provider provider, String providerId) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if(existingUser.isPresent()) {
            log.info("User already exists");
            User user = existingUser.get();
            log.info(existingUser.toString());
            return user;
        }
        else {
            User user = new User(name, email, picture, true, provider, providerId);
            userRepository.save(user);
            log.info("User created using " + provider.name().toLowerCase());
            return user;
        }
    }
}
