package com.miyoshix.portal.modules;

import com.miyoshix.portal.user.AppRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Catalogue des modules du portail, déclaré sous le préfixe {@code portal} dans
 * {@code application.yml}. Les contraintes sont validées au démarrage.
 *
 * @param loginPage page du front vers laquelle rediriger en cas d'échec de connexion
 * @param modules   les modules déclarés
 */
@ConfigurationProperties(prefix = "portal")
@Validated
public record PortalProperties(
        String loginPage,
        List<@Valid Module> modules) {

    public PortalProperties {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    /**
     * @param key         identifiant stable du module, utilisé en base et dans les logs
     * @param label       nom affiché sur la tuile
     * @param description phrase d'accroche affichée sous le nom
     * @param icon        emoji ou nom d'icône affiché sur la tuile
     * @param path        route Angular vers laquelle pointe la tuile
     * @param minimumRole rôle minimum requis pour voir le module
     * @param enabled     permet de masquer un module sans supprimer sa configuration
     */
    public record Module(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,30}$",
                    message = "la clé doit être en minuscules, chiffres et tirets") String key,
            @NotBlank String label,
            String description,
            String icon,
            // Contraint à une route interne, jamais une URL externe.
            @NotBlank @Pattern(regexp = "^/[a-zA-Z0-9/_-]*$",
                    message = "le chemin doit être une route interne commençant par /") String path,
            @NotNull AppRole minimumRole,
            boolean enabled) {
    }
}
