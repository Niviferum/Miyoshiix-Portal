package com.miyoshix.portal.modules;

import com.miyoshix.portal.user.AppRole;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Filtre les modules visibles selon le rôle. Sert à l'affichage des tuiles ; chaque
 * module protège en plus ses propres routes.
 */
@Service
public class ModuleCatalog {

    private final List<PortalProperties.Module> modules;

    public ModuleCatalog(PortalProperties properties) {
        this.modules = properties.modules();
    }

    /** Les modules actifs et accessibles au rôle donné, dans l'ordre de configuration. */
    public List<PortalProperties.Module> visibleTo(AppRole role) {
        return modules.stream()
                .filter(PortalProperties.Module::enabled)
                .filter(module -> isAllowed(role, module.minimumRole()))
                .toList();
    }

    /** Le module actif portant cette clé, s'il existe. */
    public Optional<PortalProperties.Module> findEnabled(String key) {
        return modules.stream()
                .filter(PortalProperties.Module::enabled)
                .filter(module -> module.key().equals(key))
                .findFirst();
    }

    /** ADMIN voit tout ce que voit USER : la hiérarchie suit l'ordre de l'énumération. */
    public static boolean isAllowed(AppRole actual, AppRole required) {
        return actual.ordinal() >= required.ordinal();
    }
}
