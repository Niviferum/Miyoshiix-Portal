package com.miyoshix.portal.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/** Journalise les requêtes refusées (403), en distinguant les refus CSRF, puis répond 403. */
@Component
public class AccessDeniedLogger implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedLogger.class);

    private final AccessDeniedHandler delegate = new AccessDeniedHandlerImpl();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
            throws IOException, ServletException {
        String reason = exception instanceof CsrfException ? "jeton CSRF absent ou invalide" : "droits insuffisants";
        log.warn("Accès refusé ({}) : {} {} par {}.",
                reason, request.getMethod(), request.getRequestURI(), currentUser());
        delegate.handle(request, response, exception);
    }

    private static String currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof PortalPrincipal principal
                ? principal.getDiscordId()
                : "un visiteur anonyme";
    }
}
