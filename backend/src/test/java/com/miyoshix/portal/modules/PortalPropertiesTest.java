package com.miyoshix.portal.modules;

import static org.assertj.core.api.Assertions.assertThat;

import com.miyoshix.portal.user.AppRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Validation des adresses déclarées pour chaque module. */
class PortalPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void accepteUnModuleEnHttpsSurSonSousDomaine() {
        assertThat(violations(module("https://exemple.miyoshiix.com/",
                "https://exemple.miyoshiix.com/login/oauth2/code/miyoshiix"))).isEmpty();
    }

    @Test
    void accepteHttpVersLocalhostEnDeveloppement() {
        assertThat(violations(module("http://exemple.localhost:8081/",
                "http://exemple.localhost:8081/login/oauth2/code/miyoshiix"))).isEmpty();
    }

    @Test
    void refuseUneUrlQuiNEstPasUneAdresseWebSure() {
        for (String url : List.of("javascript:alert(1)", "//evil.com/", "/modules/exemple",
                "http://exemple.miyoshiix.com/", "https://exemple.miyoshiix.com/#x")) {
            assertThat(violations(module(url, null))).as(url).extracting(ConstraintViolation::getMessage)
                    .containsExactly("l'url du module doit être une adresse HTTPS (HTTP réservé à localhost)");
        }
    }

    @Test
    void refuseDesAdressesDeRetourHorsDuSiteDuModule() {
        for (String redirect : List.of("https://autre.miyoshiix.com/login/oauth2/code/miyoshiix",
                "https://exemple.miyoshiix.com:8443/login/oauth2/code/miyoshiix")) {
            assertThat(violations(module("https://exemple.miyoshiix.com/", redirect))).as(redirect)
                    .extracting(ConstraintViolation::getMessage)
                    .containsExactly("les adresses de retour du client doivent être sur le même site que l'url du module");
        }
    }

    private Set<ConstraintViolation<PortalProperties.Module>> violations(PortalProperties.Module module) {
        return validator.validate(module);
    }

    private static PortalProperties.Module module(String url, String redirectUri) {
        PortalProperties.Client client = redirectUri == null
                ? null
                : new PortalProperties.Client("secret", List.of(redirectUri), List.of());
        return new PortalProperties.Module("exemple", "Exemple", null, null, url, AppRole.USER, true, client);
    }
}
