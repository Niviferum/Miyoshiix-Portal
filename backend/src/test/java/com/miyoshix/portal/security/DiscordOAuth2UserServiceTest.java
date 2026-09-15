package com.miyoshix.portal.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.miyoshix.portal.user.AppRole;
import com.miyoshix.portal.user.AppUser;
import com.miyoshix.portal.user.AppUserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** Contrôle du portillon d'entrée : seule la liste blanche ouvre une session. */
class DiscordOAuth2UserServiceTest {

    private static final String ID_AUTORISE = "123456789012345678";
    private static final String ID_INCONNU = "987654321098765432";

    private final AppUserRepository users = mock(AppUserRepository.class);

    @Test
    void refuseUnIdentifiantAbsentDeLaListeBlanche() {
        when(users.findByDiscordIdAndEnabledTrue(ID_INCONNU)).thenReturn(Optional.empty());
        var service = serviceRepondant(reponseDiscord(ID_INCONNU, "intrus", null));

        assertThatThrownBy(() -> service.loadUser(mock(OAuth2UserRequest.class)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("liste blanche");
    }

    @Test
    void accepteUnIdentifiantAutoriseEtAppliqueLeRoleDeLaBase() {
        AppUser autorise = new AppUser(ID_AUTORISE, "Ancien pseudo", AppRole.ADMIN);
        when(users.findByDiscordIdAndEnabledTrue(ID_AUTORISE)).thenReturn(Optional.of(autorise));
        var service = serviceRepondant(reponseDiscord(ID_AUTORISE, "pseudo", "a".repeat(32)));

        OAuth2User principal = service.loadUser(mock(OAuth2UserRequest.class));

        assertThat(principal).isInstanceOf(PortalPrincipal.class);
        assertThat(principal.getName()).isEqualTo(ID_AUTORISE);
        // Le rôle vient de la base, jamais de Discord.
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void refuseUnIdentifiantMalForme() {
        var service = serviceRepondant(reponseDiscord("pas-un-snowflake", "intrus", null));

        assertThatThrownBy(() -> service.loadUser(mock(OAuth2UserRequest.class)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void ignoreUnHashDAvatarNonConforme() {
        AppUser autorise = new AppUser(ID_AUTORISE, "pseudo", AppRole.USER);
        when(users.findByDiscordIdAndEnabledTrue(ID_AUTORISE)).thenReturn(Optional.of(autorise));
        // Une valeur qui tenterait de sortir de l'URL du CDN ne doit pas être conservée.
        var service = serviceRepondant(reponseDiscord(ID_AUTORISE, "pseudo", "../../evil.example/x"));

        PortalPrincipal principal = (PortalPrincipal) service.loadUser(mock(OAuth2UserRequest.class));

        assertThat(principal.getAvatarUrl()).isNull();
        assertThat(autorise.getAvatarHash()).isNull();
    }

    @Test
    void retientLePseudoGlobalQuandIlExiste() {
        AppUser autorise = new AppUser(ID_AUTORISE, "ancien", AppRole.USER);
        when(users.findByDiscordIdAndEnabledTrue(ID_AUTORISE)).thenReturn(Optional.of(autorise));
        var service = serviceRepondant(new DefaultOAuth2User(
                List.of(),
                Map.of("id", ID_AUTORISE, "username", "pseudo_technique", "global_name", "Pseudo Affiché"),
                "id"));

        PortalPrincipal principal = (PortalPrincipal) service.loadUser(mock(OAuth2UserRequest.class));

        assertThat(principal.getDisplayName()).isEqualTo("Pseudo Affiché");
    }

    /** Construit le service en court-circuitant l'appel HTTP à Discord. */
    private DiscordOAuth2UserService serviceRepondant(OAuth2User reponse) {
        @SuppressWarnings("unchecked")
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = mock(OAuth2UserService.class);
        when(delegate.loadUser(any())).thenReturn(reponse);
        return new DiscordOAuth2UserService(users, delegate);
    }

    private static OAuth2User reponseDiscord(String id, String username, String avatar) {
        Map<String, Object> attributs = new java.util.HashMap<>();
        attributs.put("id", id);
        attributs.put("username", username);
        if (avatar != null) {
            attributs.put("avatar", avatar);
        }
        return new DefaultOAuth2User(List.of(), attributs, "id");
    }
}
