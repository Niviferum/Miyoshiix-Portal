package com.miyoshix.portal.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/** Journalise la déconnexion et répond 204. */
@Component
public class LogoutSuccessLogger implements LogoutSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(LogoutSuccessLogger.class);

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof PortalPrincipal principal) {
            log.info("Déconnexion de {} ({}).", principal.getDisplayName(), principal.getDiscordId());
        } else {
            log.info("Déconnexion demandée sans session active.");
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }
}
