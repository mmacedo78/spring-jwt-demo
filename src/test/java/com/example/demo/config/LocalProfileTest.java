package com.example.demo.config;

import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/** Garante que o perfil "local" usa H2 em memória e popula os usuários de demonstração. */
@SpringBootTest
@ActiveProfiles("local")
class LocalProfileTest {

    @Autowired
    private Environment env;
    @Autowired
    private UserRepository userRepository;

    @Test
    void usesInMemoryH2() {
        String url = env.getProperty("spring.datasource.url");

        assertNotNull(url);
        assertTrue(url.startsWith("jdbc:h2:mem:"), "URL inesperada: " + url);
        assertEquals("true", env.getProperty("spring.h2.console.enabled"));
    }

    @Test
    void seedsDemoUsers() {
        assertTrue(userRepository.existsByUsername("admin"));
        assertTrue(userRepository.existsByUsername("user"));
    }
}
