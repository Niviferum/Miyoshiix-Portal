package com.miyoshix.portal.api;

import com.miyoshix.portal.modules.ModuleCatalog;
import com.miyoshix.portal.modules.PortalProperties;
import com.miyoshix.portal.security.PortalPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API du portail : identité de l'utilisateur et tuiles à afficher. Chaque module apporte
 * son propre contrôleur sous {@code /api/<module>}.
 */
@RestController
@RequestMapping("/api")
public class PortalController {

    private final ModuleCatalog catalog;

    public PortalController(ModuleCatalog catalog) {
        this.catalog = catalog;
    }

    /** Profil de la session courante. Répond 401 s'il n'y a pas de session. */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal PortalPrincipal principal) {
        return new MeResponse(
                principal.getDiscordId(),
                principal.getDisplayName(),
                principal.getAvatarUrl(),
                principal.getRole().name());
    }

    /** Modules visibles par l'utilisateur courant, dans l'ordre de configuration. */
    @GetMapping("/modules")
    public List<ModuleResponse> modules(@AuthenticationPrincipal PortalPrincipal principal) {
        return catalog.visibleTo(principal.getRole()).stream()
                .map(ModuleResponse::from)
                .toList();
    }

    /** DTO de sortie, distinct de l'entité pour ne publier que les champs voulus. */
    public record MeResponse(String id, String displayName, String avatarUrl, String role) {
    }

    public record ModuleResponse(String key, String label, String description, String icon, String path) {

        static ModuleResponse from(PortalProperties.Module module) {
            return new ModuleResponse(
                    module.key(), module.label(), module.description(), module.icon(), module.path());
        }
    }
}
