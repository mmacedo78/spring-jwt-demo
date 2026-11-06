package com.example.demo.controller;

import com.example.demo.IntegrationTestSupport;
import com.example.demo.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends IntegrationTestSupport {

    private static String credentials(String username, String password) {
        return """
                {"username":"%s","password":"%s"}""".formatted(username, password);
    }

    // ---------- /auth/register ----------

    @Test
    void registerCreatesUserWithRoleUserAndEncodedPassword() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("novo", "segredo123")))
                .andExpect(status().isCreated());

        AppUser saved = userRepository.findByUsername("novo").orElseThrow();
        assertEquals("USER", saved.getRole());
        assertNotEquals("segredo123", saved.getPassword());
        assertTrue(encoder.matches("segredo123", saved.getPassword()));
    }

    @Test
    void registerWithExistingUsernameReturnsConflict() throws Exception {
        createUser("duplicado", "x123", "USER");

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("duplicado", "outra")))
                .andExpect(status().isConflict());
    }

    @Test
    void registerWithBlankFieldsReturnsBadRequest() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("", "")))
                .andExpect(status().isBadRequest());
    }

    // ---------- /auth/login ----------

    @Test
    void loginWithValidCredentialsReturnsBearerToken() throws Exception {
        createUser("maria", "senha123", "USER");

        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("maria", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        assertTrue(jwtService.isValid(token));
        assertEquals("maria", jwtService.extractUsername(token));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        createUser("maria", "senha123", "USER");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("maria", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownUserReturnsUnauthorized() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("fantasma", "qualquer")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBlankFieldsReturnsBadRequest() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registeredUserCanLogIn() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("fluxo", "senha123")))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("fluxo", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
