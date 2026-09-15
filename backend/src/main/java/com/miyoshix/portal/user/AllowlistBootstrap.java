package com.miyoshix.portal.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée le premier compte autorisé au démarrage à partir de {@code PORTAL_BOOTSTRAP_ADMIN}.
 * Idempotent : ne touche pas à une ligne existante.
 */
@Component
public class AllowlistBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AllowlistBootstrap.class);

    private final AppUserRepository users;
    private final String bootstrapDiscordId;

    public AllowlistBootstrap(
            AppUserRepository users,
            @Value("${portal.bootstrap-admin:}") String bootstrapDiscordId) {
        this.users = users;
        this.bootstrapDiscordId = bootstrapDiscordId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapDiscordId == null || bootstrapDiscordId.isBlank()) {
            if (users.count() == 0) {
                log.warn("Liste blanche vide et aucun PORTAL_BOOTSTRAP_ADMIN défini : "
                        + "personne ne pourra se connecter.");
            }
            return;
        }

        String discordId = bootstrapDiscordId.trim();
        if (!discordId.matches("^[0-9]{17,20}$")) {
            log.error("PORTAL_BOOTSTRAP_ADMIN='{}' n'est pas un identifiant Discord valide, ignoré.", discordId);
            return;
        }

        if (users.existsById(discordId)) {
            log.debug("Compte d'amorçage {} déjà présent.", discordId);
            return;
        }

        users.save(new AppUser(discordId, "Administrateur", AppRole.ADMIN));
        log.info("Compte d'amorçage créé pour l'identifiant Discord {} (rôle ADMIN).", discordId);
    }
}
