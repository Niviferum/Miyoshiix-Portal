package com.miyoshix.portal.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Personne autorisée à ouvrir une session sur le portail : une ligne de la liste blanche.
 * La clé primaire est le snowflake Discord, stocké en texte.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @Column(name = "discord_id", nullable = false, updatable = false, length = 32)
    private String discordId;

    /** Pseudo Discord au dernier login, informatif uniquement. */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** Hash de l'avatar Discord, ou null si la personne n'en a pas. */
    @Column(name = "avatar_hash", length = 64)
    private String avatarHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private AppRole role = AppRole.USER;

    /** Coupe l'accès sans supprimer la ligne ni les données liées. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Requis par JPA. */
    protected AppUser() {
    }

    public AppUser(String discordId, String displayName, AppRole role) {
        this.discordId = Objects.requireNonNull(discordId);
        this.displayName = Objects.requireNonNull(displayName);
        this.role = Objects.requireNonNull(role);
    }

    public String getDiscordId() {
        return discordId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarHash() {
        return avatarHash;
    }

    public void setAvatarHash(String avatarHash) {
        this.avatarHash = avatarHash;
    }

    public AppRole getRole() {
        return role;
    }

    public void setRole(AppRole role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /** Égalité sur la clé métier immuable, comme attendu par JPA. */
    @Override
    public boolean equals(Object o) {
        return o instanceof AppUser other && Objects.equals(discordId, other.discordId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(discordId);
    }
}
