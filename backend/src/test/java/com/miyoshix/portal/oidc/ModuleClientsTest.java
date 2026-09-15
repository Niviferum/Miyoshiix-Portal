package com.miyoshix.portal.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import com.miyoshix.portal.modules.PortalProperties;
import com.miyoshix.portal.user.AppRole;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/** Conversion des modules configurés en clients OpenID Connect. */
class ModuleClientsTest {

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void enregistreUnModuleActifAvecSecret() {
        List<RegisteredClient> clients = ModuleClients.from(List.of(module("exemple", true, "secret")), encoder);

        assertThat(clients).hasSize(1);
        RegisteredClient client = clients.getFirst();
        assertThat(client.getClientId()).isEqualTo("exemple");
        assertThat(client.getRedirectUris()).containsExactly("https://exemple.miyoshiix.com/login/oauth2/code/miyoshiix");
        assertThat(client.getAuthorizationGrantTypes()).containsExactly(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(client.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(client.getClientSettings().isRequireAuthorizationConsent()).isFalse();
        // Le secret n'est jamais conservé en clair.
        assertThat(client.getClientSecret()).startsWith("{bcrypt}").doesNotContain("secret");
        assertThat(encoder.matches("secret", client.getClientSecret())).isTrue();
    }

    @Test
    void ignoreLesModulesDesactivesSansSecretOuSansClient() {
        List<PortalProperties.Module> modules = List.of(
                module("desactive", false, "secret"),
                module("sans-secret", true, ""),
                new PortalProperties.Module("sans-client", "Sans client", null, null, "https://sans-client.miyoshiix.com/", AppRole.USER, true, null));

        assertThat(ModuleClients.from(modules, encoder)).isEmpty();
    }

    private static PortalProperties.Module module(String key, boolean enabled, String secret) {
        return new PortalProperties.Module(key, key, null, null, "https://" + key + ".miyoshiix.com/", AppRole.USER, enabled,
                new PortalProperties.Client(secret,
                        List.of("https://" + key + ".miyoshiix.com/login/oauth2/code/miyoshiix"), List.of()));
    }
}
