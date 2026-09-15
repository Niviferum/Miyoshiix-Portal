package com.miyoshix.portal.oidc;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Construit la clé RSA qui signe les jetons, depuis la configuration ou à la volée. */
final class SigningKeyFactory {

    private static final Logger log = LoggerFactory.getLogger(SigningKeyFactory.class);

    private SigningKeyFactory() {
    }

    /**
     * Lit une clé privée RSA PKCS#8, au format PEM ou en DER encodé en base64. Sans clé
     * configurée, génère une clé éphémère : les jetons émis ne survivront pas à un redémarrage.
     */
    static RSAKey fromConfiguration(String configuredKey) {
        try {
            if (configuredKey == null || configuredKey.isBlank()) {
                log.warn("Aucune PORTAL_OIDC_SIGNING_KEY : clé de signature éphémère, "
                        + "les jetons des modules seront invalidés au prochain redémarrage.");
                return toJwk(generate());
            }
            return toJwk(parse(configuredKey));
        } catch (GeneralSecurityException | JOSEException | IllegalArgumentException e) {
            throw new IllegalStateException("PORTAL_OIDC_SIGNING_KEY illisible : clé RSA PKCS#8 attendue.", e);
        }
    }

    private static RSAPrivateCrtKey parse(String configuredKey) throws GeneralSecurityException {
        String base64 = configuredKey
                .replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return (RSAPrivateCrtKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static RSAPrivateCrtKey generate() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return (RSAPrivateCrtKey) generator.generateKeyPair().getPrivate();
    }

    private static RSAKey toJwk(RSAPrivateCrtKey privateKey) throws GeneralSecurityException, JOSEException {
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
        // Identifiant dérivé de la clé publique : stable tant que la clé ne change pas.
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyIDFromThumbprint()
                .build();
    }
}
