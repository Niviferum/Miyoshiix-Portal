package com.miyoshix.portal.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Lecture de la clé de signature fournie par PORTAL_OIDC_SIGNING_KEY. */
class SigningKeyFactoryTest {

    @Test
    void litUneCleEnBase64SurUneLigne() throws Exception {
        RSAPrivateCrtKey privateKey = newPrivateKey();
        String base64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());

        RSAKey jwk = SigningKeyFactory.fromConfiguration(base64);

        assertThat(jwk.toRSAPrivateKey().getModulus()).isEqualTo(privateKey.getModulus());
        assertThat(jwk.getKeyID()).isNotBlank();
    }

    @Test
    void litUneCleAuFormatPemEtDonneLeMemeIdentifiantQuEnBase64() throws Exception {
        RSAPrivateCrtKey privateKey = newPrivateKey();
        String base64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        assertThat(SigningKeyFactory.fromConfiguration(pem).getKeyID())
                .isEqualTo(SigningKeyFactory.fromConfiguration(base64).getKeyID());
    }

    @Test
    void genereUneCleEphemereSansConfiguration() {
        assertThat(SigningKeyFactory.fromConfiguration("").isPrivate()).isTrue();
    }

    @Test
    void refuseUneCleIllisible() {
        assertThatThrownBy(() -> SigningKeyFactory.fromConfiguration("pas-une-cle"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PORTAL_OIDC_SIGNING_KEY");
    }

    private static RSAPrivateCrtKey newPrivateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return (RSAPrivateCrtKey) generator.generateKeyPair().getPrivate();
    }
}
