package com.miyoshix.portal.oidc;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/** Dépôt sans aucun client, utilisé quand aucun module n'est enregistré. */
final class EmptyRegisteredClientRepository implements RegisteredClientRepository {

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Les clients sont déclarés dans la configuration.");
    }

    @Override
    public RegisteredClient findById(String id) {
        return null;
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return null;
    }
}
