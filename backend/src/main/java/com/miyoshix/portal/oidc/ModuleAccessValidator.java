package com.miyoshix.portal.oidc;

import com.miyoshix.portal.modules.ModuleCatalog;
import com.miyoshix.portal.security.PortalPrincipal;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;

/**
 * Refuse d'émettre un code d'autorisation si le rôle de l'utilisateur est inférieur au
 * rôle minimum du module demandé. Le module reçoit {@code error=access_denied}.
 */
final class ModuleAccessValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    private static final Logger log = LoggerFactory.getLogger(ModuleAccessValidator.class);

    private final ModuleCatalog catalog;

    ModuleAccessValidator(ModuleCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
        OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
        // Utilisateur pas encore connecté : le parcours de connexion passe d'abord.
        if (!(request.getPrincipal() instanceof Authentication user)
                || !user.isAuthenticated()
                || !(user.getPrincipal() instanceof PortalPrincipal principal)) {
            return;
        }

        String clientId = context.getRegisteredClient().getClientId();
        boolean allowed = catalog.findEnabled(clientId)
                .map(module -> ModuleCatalog.isAllowed(principal.getRole(), module.minimumRole()))
                .orElse(false);

        if (!allowed) {
            log.warn("Accès refusé au module « {} » pour {} (rôle {}).",
                    clientId, principal.getDiscordId(), principal.getRole());
            throw new OAuth2AuthorizationCodeRequestAuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, "Rôle insuffisant pour ce module.", null),
                    request);
        }
    }
}
