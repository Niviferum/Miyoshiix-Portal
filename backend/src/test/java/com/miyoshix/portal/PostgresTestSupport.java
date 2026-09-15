package com.miyoshix.portal;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Fournit un PostgreSQL jetable aux tests d'intégration. {@code @ServiceConnection}
 * injecte automatiquement l'URL, l'utilisateur et le mot de passe du conteneur.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestSupport {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        // Même version majeure qu'en développement pour que Flyway soit testé à l'identique.
        return new PostgreSQLContainer("postgres:17.6-alpine");
    }
}
