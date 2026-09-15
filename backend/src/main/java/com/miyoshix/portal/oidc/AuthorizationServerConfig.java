package com.miyoshix.portal.oidc;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import com.miyoshix.portal.modules.ModuleCatalog;
import com.miyoshix.portal.modules.PortalProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Fournisseur d'identité OpenID Connect du portail : les modules s'y connectent pour
 * obtenir l'identité et le rôle de l'utilisateur, authentifié par la session Discord.
 */
@Configuration
public class AuthorizationServerConfig {

    /** Démarre la connexion Discord quand un module demande une autorisation sans session. */
    static final String LOGIN_ENTRY_POINT = "/oauth2/authorization/discord";

    /**
     * Chaîne dédiée aux endpoints du fournisseur (/oauth2/authorize, /oauth2/token,
     * /oauth2/jwks, /userinfo, /.well-known/openid-configuration…), évaluée avant la
     * chaîne du portail.
     */
    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerFilterChain(HttpSecurity http, ModuleCatalog catalog) throws Exception {
        http
            .oauth2AuthorizationServer(authorizationServer -> {
                http.securityMatcher(authorizationServer.getEndpointsMatcher());
                authorizationServer
                        .authorizationEndpoint(endpoint -> endpoint.authenticationProviders(providers ->
                                providers.forEach(provider -> {
                                    if (provider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider codeRequest) {
                                        codeRequest.setAuthenticationValidator(
                                                new OAuth2AuthorizationCodeRequestAuthenticationValidator()
                                                        .andThen(new ModuleAccessValidator(catalog)));
                                    }
                                })))
                        .oidc(oidc -> oidc.userInfoEndpoint(userInfo -> userInfo.userInfoMapper(IdentityClaims::userInfo)));
            })
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            // Endpoints JSON et redirections uniquement : aucune ressource à charger.
            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny()))
            .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint(LOGIN_ENTRY_POINT),
                    pathPattern("/oauth2/authorize")));

        return http.build();
    }

    @Bean
    RegisteredClientRepository registeredClientRepository(PortalProperties properties) {
        var clients = ModuleClients.from(properties.modules(), PasswordEncoderFactories.createDelegatingPasswordEncoder());
        // InMemoryRegisteredClientRepository refuse une liste vide.
        return clients.isEmpty()
                ? new EmptyRegisteredClientRepository()
                : new InMemoryRegisteredClientRepository(clients);
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(PortalProperties properties) {
        return new ImmutableJWKSet<>(new JWKSet(SigningKeyFactory.fromConfiguration(properties.oidc().signingKey())));
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(PortalProperties properties) {
        return AuthorizationServerSettings.builder()
                .issuer(properties.oidc().issuer())
                .build();
    }

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> idTokenCustomizer() {
        return IdentityClaims.idTokenCustomizer();
    }
}
