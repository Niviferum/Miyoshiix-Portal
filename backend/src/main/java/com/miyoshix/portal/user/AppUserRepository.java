package com.miyoshix.portal.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès à la liste blanche. Règle du projet : requêtes dérivées du nom ou {@code @Query}
 * à paramètres nommés, jamais de concaténation de saisie utilisateur.
 */
public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByDiscordIdAndEnabledTrue(String discordId);
}
