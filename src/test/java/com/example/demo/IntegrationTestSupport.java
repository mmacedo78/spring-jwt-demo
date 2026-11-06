package com.example.demo;

import com.example.demo.model.AppUser;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base dos testes de integração: sobe o contexto completo (perfil "test", H2),
 * monta o MockMvc com a cadeia do Spring Security e faz rollback após cada teste.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestSupport {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected PasswordEncoder encoder;
    @Autowired
    protected JwtService jwtService;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    protected AppUser createUser(String username, String rawPassword, String role) {
        return userRepository.save(new AppUser(username, encoder.encode(rawPassword), role));
    }

    protected String bearerFor(String username) {
        return "Bearer " + jwtService.generateToken(username);
    }
}
