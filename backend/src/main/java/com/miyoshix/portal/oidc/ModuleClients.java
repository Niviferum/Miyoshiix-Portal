package com.miyoshix.portal.oidc;

import com.miyoshix.portal.modules.PortalProperties;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/** Convertit les modules configurés en clients OpenID Connect enregistrés. */
final class ModuleClients {

    private static final Logger log = LoggerFactory.getLogger(ModuleClients.class);

    private ModuleClients() {
    }

    /** Un client par module actif doté d'un bloc {@code client} et d'un secret non vide. */
    static List<RegisteredClient> from(List<PortalProperties.Module> modules, PasswordEncoder encoder) {
        return modules.stream()
                .filter(module -> {
                    if (!module.enabled() || module.client() == null) {
                        return false;
                    }
                    if (module.client().clientSecret() == null || module.client().clientSecret().isBlank()) {
                        log.warn("Module « {} » sans secret client : non enregistré auprès du fournisseur d'identité.",
                                module.key());
                        return false;
                    }
                    return true;
                })
                .map(module -> toRegisteredClient(module, encoder))
                .toList();
    }

    private static RegisteredClient toRegisteredClient(PortalProperties.Module module, PasswordEncoder encoder) {
        PortalProperties.Client client = module.client();
        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.nameUUIDFromBytes(module.key().getBytes()).toString())
                .clientId(module.key())
                .clientName(module.label())
                .clientSecret(encoder.encode(client.clientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .authorizationCodeTimeToLive(Duration.ofMinutes(1))
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build());
        client.redirectUris().forEach(builder::redirectUri);
        client.postLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        return builder.build();
    }
}
