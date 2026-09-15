package com.miyoshix.portal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Redirige vers la page de login du front avec un code d'erreur pris dans une liste
 * fermée. Le détail de l'échec reste dans les logs.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginFailureHandler.class);

    private static final Set<String> KNOWN_CODES = Set.of("not_allowed", "invalid_user");
    private static final String FALLBACK_CODE = "login_failed";

    private final String loginPage;

    public LoginFailureHandler(
            @org.springframework.beans.factory.annotation.Value("${portal.login-page:/login}") String loginPage) {
        this.loginPage = loginPage;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {

        String code = FALLBACK_CODE;
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String errorCode = oauthException.getError().getErrorCode();
            if (KNOWN_CODES.contains(errorCode)) {
                code = errorCode;
            }
        }

        log.warn("Échec de connexion ({}) : {}", code, exception.getMessage());
        response.sendRedirect(loginPage + "?error=" + code);
    }
}
