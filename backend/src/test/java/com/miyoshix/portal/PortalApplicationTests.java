package com.miyoshix.portal;

import static org.assertj.core.api.Assertions.assertThat;

import com.miyoshix.portal.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Vérifie que le contexte complet démarre et que les migrations Flyway s'appliquent
 * sur un vrai PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestSupport.class)
class PortalApplicationTests {

    @Autowired
    private AppUserRepository users;

    @Test
    void leContexteDemarreEtLeSchemaExiste() {
        // Si la migration V1 n'était pas passée, cette requête échouerait.
        assertThat(users.count()).isZero();
    }
}
