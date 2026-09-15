package com.miyoshix.portal.oidc;

import com.miyoshix.portal.security.PortalPrincipal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * Informations transmises aux modules : pseudo, avatar et rôle, ajoutés au jeton d'identité
 * et renvoyés à l'identique par l'endpoint userinfo. Le {@code sub} est l'identifiant Discord.
 */
final class IdentityClaims {

    static final String ROLE = "role";

    private IdentityClaims() {
    }

    static OAuth2TokenCustomizer<JwtEncodingContext> idTokenCustomizer() {
        return context -> {
            if (!OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                return;
            }
            Authentication user = context.getPrincipal();
            if (user.getPrincipal() instanceof PortalPrincipal principal) {
                context.getClaims().claims(claims -> claims.putAll(profileOf(principal)));
            }
        };
    }

    static OidcUserInfo userInfo(OidcUserInfoAuthenticationContext context) {
        OidcIdToken idToken = context.getAuthorization().getToken(OidcIdToken.class).getToken();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", idToken.getSubject());
        for (String name : new String[] {"name", "picture", ROLE}) {
            Object value = idToken.getClaims().get(name);
            if (value != null) {
                claims.put(name, value);
            }
        }
        return new OidcUserInfo(claims);
    }

    private static Map<String, Object> profileOf(PortalPrincipal principal) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("name", principal.getDisplayName());
        if (principal.getAvatarUrl() != null) {
            claims.put("picture", principal.getAvatarUrl());
        }
        claims.put(ROLE, principal.getRole().name());
        return claims;
    }
}
