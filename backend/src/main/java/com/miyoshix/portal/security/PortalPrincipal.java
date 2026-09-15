package com.miyoshix.portal.security;

import com.miyoshix.portal.user.AppRole;
import com.miyoshix.portal.user.AppUser;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Utilisateur connecté, stocké en session. Ne conserve que les attributs nécessaires au
 * portail ; le rôle vient de la base, pas de Discord.
 */
public class PortalPrincipal implements OAuth2User, Serializable {

    private static final long serialVersionUID = 1L;

    private final String discordId;
    private final String displayName;
    private final String avatarUrl;
    private final AppRole role;

    public PortalPrincipal(String discordId, String displayName, String avatarUrl, AppRole role) {
        this.discordId = discordId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }

    @Override
    public Map<String, Object> getAttributes() {
        // Map.of refuse les valeurs null, d'où le repli sur une chaîne vide pour l'avatar.
        return Map.of(
                "id", discordId,
                "displayName", displayName,
                "avatarUrl", avatarUrl == null ? "" : avatarUrl,
                "role", role.name());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    /** Identifiant Discord : stable, et c'est lui qui apparaît dans les logs d'audit. */
    @Override
    public String getName() {
        return discordId;
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public AppRole getRole() {
        return role;
    }
}
