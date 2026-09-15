package com.miyoshix.portal.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.miyoshix.portal.config.SecurityConfig;
import com.miyoshix.portal.modules.ModuleCatalog;
import com.miyoshix.portal.modules.PortalProperties;
import com.miyoshix.portal.security.AccessDeniedLogger;
import com.miyoshix.portal.security.DiscordOAuth2UserService;
import com.miyoshix.portal.security.LoginFailureHandler;
import com.miyoshix.portal.security.LogoutSuccessLogger;
import com.miyoshix.portal.security.PortalPrincipal;
import com.miyoshix.portal.user.AppRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comportement de l'API du portail avec et sans session, et filtrage des modules
 * selon le rôle.
 */
@WebMvcTest(PortalController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, ModuleCatalog.class, LogoutSuccessLogger.class, AccessDeniedLogger.class,
        PortalControllerTest.PropertiesTestConfig.class})
class PortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Beans requis par SecurityConfig mais jamais sollicités dans ces tests.
    @MockitoBean
    private DiscordOAuth2UserService discordOAuth2UserService;

    @MockitoBean
    private LoginFailureHandler loginFailureHandler;

    @TestConfiguration
    @EnableConfigurationProperties(PortalProperties.class)
    static class PropertiesTestConfig {
    }

    @Test
    void refuseLAccesSansSession() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void renvoieLeProfilDeLaSession() throws Exception {
        mockMvc.perform(get("/api/me").with(authentication(sessionDe(AppRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123456789012345678"))
                .andExpect(jsonPath("$.displayName").value("Copine"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void masqueLesModulesAdminAUnUtilisateurSimple() throws Exception {
        mockMvc.perform(get("/api/modules").with(authentication(sessionDe(AppRole.USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].key").value("exemple"))
                .andExpect(jsonPath("$[0].url").value("http://exemple.localhost:9001/"));
    }

    @Test
    void montreLesModulesAdminAUnAdministrateur() throws Exception {
        mockMvc.perform(get("/api/modules").with(authentication(sessionDe(AppRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].key").value("exemple"))
                .andExpect(jsonPath("$[1].key").value("admin-only"));
    }

    @Test
    void nExposeJamaisUnModuleDesactive() throws Exception {
        mockMvc.perform(get("/api/modules").with(authentication(sessionDe(AppRole.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("desactive"))));
    }

    @Test
    void refuseUnPostSansJetonCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(authentication(sessionDe(AppRole.USER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void accepteLaDeconnexionAvecJetonCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(authentication(sessionDe(AppRole.USER)))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void poseLesEntetesDeSecurite() throws Exception {
        mockMvc.perform(get("/api/me").with(authentication(sessionDe(AppRole.USER))))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "same-origin"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
    }

    /** Construit une session équivalente à celle produite par une connexion Discord. */
    private static Authentication sessionDe(AppRole role) {
        PortalPrincipal principal = new PortalPrincipal(
                "123456789012345678", "Copine", null, role);
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "discord");
    }
}
