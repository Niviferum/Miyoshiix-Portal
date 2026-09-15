package com.miyoshix.portal.user;

/** Rôles de l'application. {@link #authority()} produit la forme attendue par hasRole(). */
public enum AppRole {
    /** Accès aux modules, c'est le rôle de base une fois la personne autorisée. */
    USER,
    /** Peut gérer la liste blanche et voir les modules réservés à l'admin. */
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
