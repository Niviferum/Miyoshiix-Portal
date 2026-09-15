package com.miyoshix.portal.security;

import com.miyoshix.portal.user.AppUser;
import com.miyoshix.portal.user.AppUserRepository;
import java.time.Instant;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Charge le profil Discord après l'échange du code d'autorisation, puis refuse la
 * connexion si l'identifiant n'est pas dans la liste blanche {@code app_user}.
 */
@Service
public class DiscordOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(DiscordOAuth2UserService.class);

    /** Hash d'avatar Discord : hexadécimal, préfixé « a_ » si animé. */
    private static final Pattern AVATAR_HASH = Pattern.compile("^(a_)?[a-f0-9]{32}$");

    /** Snowflake Discord : entier décimal de 17 à 20 chiffres. */
    private static final Pattern SNOWFLAKE = Pattern.compile("^[0-9]{17,20}$");

    private static final String CDN_BASE = "https://cdn.discordapp.com/avatars/";

    private final AppUserRepository users;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public DiscordOAuth2UserService(AppUserRepository users) {
        this(users, new DefaultOAuth2UserService());
    }

    /** Constructeur de test, permettant de substituer l'appel HTTP à Discord. */
    DiscordOAuth2UserService(AppUserRepository users, OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate) {
        this.users = users;
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User discordUser = delegate.loadUser(request);

        String discordId = asString(discordUser.getAttributes().get("id"));
        if (discordId == null || !SNOWFLAKE.matcher(discordId).matches()) {
            throw reject("invalid_user", "Réponse Discord sans identifiant exploitable.");
        }

        AppUser user = users.findByDiscordIdAndEnabledTrue(discordId)
                .orElseThrow(() -> {
                    log.warn("Connexion refusée : l'identifiant Discord {} n'est pas autorisé.", discordId);
                    return reject("not_allowed", "Compte Discord absent de la liste blanche.");
                });

        // Champs d'affichage rafraîchis à chaque connexion, sans effet sur les droits.
        String displayName = firstNonBlank(
                asString(discordUser.getAttributes().get("global_name")),
                asString(discordUser.getAttributes().get("username")),
                "Utilisateur");
        user.setDisplayName(truncate(displayName, 100));

        String avatarHash = asString(discordUser.getAttributes().get("avatar"));
        user.setAvatarHash(avatarHash != null && AVATAR_HASH.matcher(avatarHash).matches() ? avatarHash : null);
        user.setLastLoginAt(Instant.now());

        log.info("Connexion acceptée pour {} ({}).", user.getDisplayName(), discordId);

        return new PortalPrincipal(
                user.getDiscordId(), user.getDisplayName(), avatarUrl(user), user.getRole());
    }

    private static String avatarUrl(AppUser user) {
        if (user.getAvatarHash() == null) {
            return null;
        }
        String extension = user.getAvatarHash().startsWith("a_") ? ".gif" : ".png";
        return CDN_BASE + user.getDiscordId() + "/" + user.getAvatarHash() + extension;
    }

    /** Construit un refus. Le motif reste dans les logs, jamais dans la réponse HTTP. */
    private static OAuth2AuthenticationException reject(String code, String logMessage) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, logMessage, null), logMessage);
    }

    private static String asString(Object value) {
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
