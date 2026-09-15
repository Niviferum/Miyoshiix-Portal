package com.miyoshix.portal.modules;

import com.miyoshix.portal.user.AppRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration du portail, déclarée sous le préfixe {@code portal} dans
 * {@code application.yml}. Les contraintes sont validées au démarrage.
 *
 * @param loginPage page du front vers laquelle rediriger en cas d'échec de connexion
 * @param oidc      réglages du fournisseur d'identité OpenID Connect
 * @param modules   les modules déclarés
 */
@ConfigurationProperties(prefix = "portal")
@Validated
public record PortalProperties(
        String loginPage,
        @Valid @NotNull Oidc oidc,
        List<@Valid Module> modules) {

    /**
     * Adresse absolue en HTTPS, ou en HTTP uniquement vers {@code localhost} et ses
     * sous-domaines pour le développement. Pas de fragment.
     */
    static final String SAFE_URL =
            "^(https://[a-z0-9.-]+(:[0-9]+)?|http://([a-z0-9-]+\\.)?localhost(:[0-9]+)?)(/[^\\s#]*)?$";

    public PortalProperties {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    /**
     * @param issuer     identifiant public du fournisseur, repris dans le champ {@code iss} des jetons
     * @param signingKey clé privée RSA PKCS#8 (PEM ou DER en base64) qui signe les jetons ;
     *                   vide en développement : une clé éphémère est générée au démarrage
     */
    public record Oidc(
            @NotBlank @Pattern(regexp = SAFE_URL, message = "l'issuer doit être une adresse HTTPS (HTTP réservé à localhost)")
            String issuer,
            String signingKey) {
    }

    /**
     * @param key         identifiant stable du module, utilisé comme client_id OpenID Connect
     * @param label       nom affiché sur la tuile
     * @param description phrase d'accroche affichée sous le nom
     * @param icon        nom du fichier d'icône de la tuile, sans extension (optionnel)
     * @param url         adresse du site du module, ouverte par la tuile
     * @param minimumRole rôle minimum requis pour voir le module et s'y connecter
     * @param enabled     permet de masquer un module sans supprimer sa configuration
     * @param client      enregistrement du module auprès du fournisseur d'identité (optionnel)
     */
    public record Module(
            @NotBlank @Pattern(regexp = "^[a-z][a-z0-9-]{1,30}$",
                    message = "la clé doit être en minuscules, chiffres et tirets") String key,
            @NotBlank String label,
            String description,
            @Pattern(regexp = "^[a-z0-9-]{1,40}$",
                    message = "l'icône est un nom de fichier : minuscules, chiffres et tirets") String icon,
            @NotBlank @Pattern(regexp = SAFE_URL,
                    message = "l'url du module doit être une adresse HTTPS (HTTP réservé à localhost)") String url,
            @NotNull AppRole minimumRole,
            boolean enabled,
            @Valid Client client) {

        /** Les adresses de retour du client sont sur le même site (schéma, hôte, port) que l'url du module. */
        @AssertTrue(message = "les adresses de retour du client doivent être sur le même site que l'url du module")
        public boolean isClientOnModuleSite() {
            if (client == null || url == null || !url.matches(SAFE_URL)) {
                return true;
            }
            String site = siteOf(url);
            return Stream.concat(client.redirectUris().stream(), client.postLogoutRedirectUris().stream())
                    .filter(uri -> uri != null && uri.matches(SAFE_URL))
                    .allMatch(uri -> siteOf(uri).equals(site));
        }

        private static String siteOf(String address) {
            URI uri = URI.create(address);
            return uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
        }
    }

    /**
     * @param clientSecret           secret du module ; vide : le module n'est pas enregistré
     * @param redirectUris           adresses de retour autorisées, comparées à l'identique
     * @param postLogoutRedirectUris adresses autorisées après une déconnexion OpenID Connect
     */
    public record Client(
            String clientSecret,
            @NotEmpty List<@Pattern(regexp = SAFE_URL,
                    message = "adresse de retour HTTPS obligatoire (HTTP réservé à localhost)") String> redirectUris,
            List<@Pattern(regexp = SAFE_URL,
                    message = "adresse de retour HTTPS obligatoire (HTTP réservé à localhost)") String> postLogoutRedirectUris) {

        public Client {
            redirectUris = redirectUris == null ? List.of() : List.copyOf(redirectUris);
            postLogoutRedirectUris = postLogoutRedirectUris == null ? List.of() : List.copyOf(postLogoutRedirectUris);
        }
    }
}
