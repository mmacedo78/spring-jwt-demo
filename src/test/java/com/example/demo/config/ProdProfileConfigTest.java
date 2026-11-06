package com.example.demo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valida o application-prod.properties sem subir o contexto (não exige um PostgreSQL rodando):
 * confere o driver, que o H2 console está desligado e que segredos vêm só de variáveis de ambiente.
 */
class ProdProfileConfigTest {

    private final Properties prod = new Properties();

    @BeforeEach
    void load() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application-prod.properties")) {
            assertNotNull(in, "application-prod.properties não encontrado");
            prod.load(in);
        }
    }

    @Test
    void usesPostgresql() {
        assertTrue(prod.getProperty("spring.datasource.url").contains("jdbc:postgresql:"));
        assertEquals("org.postgresql.Driver", prod.getProperty("spring.datasource.driver-class-name"));
    }

    @Test
    void postgresDriverIsOnTheClasspath() {
        assertDoesNotThrow(() -> Class.forName("org.postgresql.Driver"));
    }

    @Test
    void secretsComeFromEnvironmentWithoutDefaults() {
        assertEquals("${DB_USERNAME}", prod.getProperty("spring.datasource.username"));
        assertEquals("${DB_PASSWORD}", prod.getProperty("spring.datasource.password"));
        assertEquals("${JWT_SECRET}", prod.getProperty("app.jwt.secret"));
    }

    @Test
    void h2ConsoleIsDisabledAndSqlLoggingIsOff() {
        assertEquals("false", prod.getProperty("spring.h2.console.enabled"));
        assertEquals("false", prod.getProperty("spring.jpa.show-sql"));
    }
}
