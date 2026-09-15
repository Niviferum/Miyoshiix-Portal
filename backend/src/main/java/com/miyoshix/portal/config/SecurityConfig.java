package com.miyoshix.portal.config;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import com.miyoshix.portal.security.AccessDeniedLogger;
import com.miyoshix.portal.security.DiscordOAuth2UserService;
import com.miyoshix.portal.security.LoginFailureHandler;
import com.miyoshix.portal.security.LogoutSuccessLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

/**
 * Chaîne de filtres de sécurité : authentification OAuth2 Discord, session serveur avec
 * cookie HttpOnly, protection CSRF et en-têtes de sécurité.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' https://cdn.discordapp.com data:",
            "font-src 'self'",
            "connect-src 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "object-src 'none'");

    private static final String PERMISSIONS_POLICY = "camera=(), microphone=(), geolocation=(), payment=()";

    private final DiscordOAuth2UserService discordUserService;
    private final LoginFailureHandler loginFailureHandler;
    private final LogoutSuccessLogger logoutSuccessLogger;
    private final AccessDeniedLogger accessDeniedLogger;
    private final boolean cookieSecure;
    private final String loginPage;

    public SecurityConfig(
            DiscordOAuth2UserService discordUserService,
            LoginFailureHandler loginFailureHandler,
            LogoutSuccessLogger logoutSuccessLogger,
            AccessDeniedLogger accessDeniedLogger,
            @Value("${server.servlet.session.cookie.secure:false}") boolean cookieSecure,
            @Value("${portal.login-page:/login}") String loginPage) {
        this.discordUserService = discordUserService;
        this.loginFailureHandler = loginFailureHandler;
        this.logoutSuccessLogger = logoutSuccessLogger;
        this.accessDeniedLogger = accessDeniedLogger;
        this.cookieSecure = cookieSecure;
        this.loginPage = loginPage;
    }

    @Bean
    @Order(2)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        HttpSessionRequestCache requestCache = authorizationRequestCache();
        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/actuator/**").hasRole("ADMIN")
                    .requestMatchers("/api/**").authenticated()
                    // Coquille du SPA : index.html, JS, CSS et routes Angular.
                    .anyRequest().permitAll())

            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))

            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                    .frameOptions(frame -> frame.deny())
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.SAME_ORIGIN))
                    .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY)))

            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(fixation -> fixation.newSession()))

            .requestCache(cache -> cache.requestCache(requestCache))

            .oauth2Login(oauth2 -> oauth2
                    // Page Angular : désactive la page de connexion générée par Spring Security.
                    .loginPage(loginPage)
                    .userInfoEndpoint(userInfo -> userInfo.userService(discordUserService))
                    .successHandler(successHandler(requestCache))
                    .failureHandler(loginFailureHandler))

            .logout(logout -> logout
                    .logoutRequestMatcher(pathPattern(HttpMethod.POST, "/api/auth/logout"))
                    .deleteCookies("MIYOSHIX_SESSION", "XSRF-TOKEN")
                    .invalidateHttpSession(true)
                    .logoutSuccessHandler(logoutSuccessLogger))

            .exceptionHandling(exceptions -> exceptions
                    .accessDeniedHandler(accessDeniedLogger)
                    // 401 JSON sur l'API au lieu d'une redirection vers Discord.
                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            pathPattern("/api/**")));

        return http.build();
    }

    /**
     * Dépôt du jeton CSRF dans un cookie lisible par Angular, qui le recopie dans
     * l'en-tête {@code X-XSRF-TOKEN}.
     */
    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // SameSite et Secure ne sont pas hérités de server.servlet.session.cookie.*.
        repository.setCookieCustomizer(cookie -> cookie
                .sameSite("Lax")
                .secure(cookieSecure)
                .path("/"));
        return repository;
    }

    /**
     * Mémorise uniquement les demandes d'autorisation des modules, pour y revenir après la
     * connexion Discord. Les autres requêtes refusées (par exemple un 401 sur /api/me) ne
     * sont jamais rejouées.
     */
    private static HttpSessionRequestCache authorizationRequestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setRequestMatcher(pathPattern(HttpMethod.GET, "/oauth2/authorize"));
        return requestCache;
    }

    /**
     * Après connexion : reprend la demande d'autorisation d'un module si elle a été
     * mémorisée, sinon renvoie à la racine du SPA.
     */
    private static SavedRequestAwareAuthenticationSuccessHandler successHandler(HttpSessionRequestCache requestCache) {
        SavedRequestAwareAuthenticationSuccessHandler handler = new SavedRequestAwareAuthenticationSuccessHandler();
        handler.setDefaultTargetUrl("/");
        handler.setRequestCache(requestCache);
        return handler;
    }
}
