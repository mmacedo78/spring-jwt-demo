package com.example.demo.controller;

import com.example.demo.IntegrationTestSupport;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest extends IntegrationTestSupport {

    @Autowired
    private ProductRepository productRepository;
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private String userAuth;
    private String adminAuth;

    @BeforeEach
    void createUsers() {
        createUser("comum", "pw123", "USER");
        createUser("chefe", "pw123", "ADMIN");
        userAuth = bearerFor("comum");
        adminAuth = bearerFor("chefe");
    }

    private Product saveProduct(String name, String price) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        return productRepository.save(p);
    }

    // ---------- autenticação ----------

    @Test
    void requestWithoutTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithInvalidTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithNonBearerSchemeReturnsUnauthorized() throws Exception {
        mvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46YWRtaW4="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithExpiredTokenReturnsUnauthorized() throws Exception {
        String expired = new JwtService(jwtSecret, -1_000).generateToken("comum");

        mvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenOfNonExistentUserReturnsUnauthorized() throws Exception {
        mvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION, bearerFor("fantasma")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- leitura ----------

    @Test
    void listReturnsAllProducts() throws Exception {
        saveProduct("Teclado", "199.90");
        saveProduct("Mouse", "89.50");

        mvc.perform(get("/products").header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByIdReturnsProduct() throws Exception {
        Product p = saveProduct("Monitor", "999.00");

        mvc.perform(get("/products/{id}", p.getId()).header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId()))
                .andExpect(jsonPath("$.name").value("Monitor"));
    }

    @Test
    void getByIdOfMissingProductReturnsNotFound() throws Exception {
        mvc.perform(get("/products/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isNotFound());
    }

    // ---------- criação ----------

    @Test
    void createPersistsProductAndReturnsCreated() throws Exception {
        mvc.perform(post("/products")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Teclado","price":199.90}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Teclado"))
                .andExpect(jsonPath("$.price").value(199.9));

        assertEquals(1, productRepository.count());
    }

    @Test
    void createWithBlankNameReturnsBadRequest() throws Exception {
        mvc.perform(post("/products")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","price":10}"""))
                .andExpect(status().isBadRequest());

        assertEquals(0, productRepository.count());
    }

    @Test
    void createWithNonPositivePriceReturnsBadRequest() throws Exception {
        mvc.perform(post("/products")
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Grátis","price":-5}"""))
                .andExpect(status().isBadRequest());
    }

    // ---------- atualização ----------

    @Test
    void updateChangesExistingProduct() throws Exception {
        Product p = saveProduct("Antigo", "10.00");

        mvc.perform(put("/products/{id}", p.getId())
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Novo","price":20.00}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo"));

        Product reloaded = productRepository.findById(p.getId()).orElseThrow();
        assertEquals("Novo", reloaded.getName());
        assertEquals(0, new BigDecimal("20.00").compareTo(reloaded.getPrice()));
    }

    @Test
    void updateOfMissingProductReturnsNotFound() throws Exception {
        mvc.perform(put("/products/{id}", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X","price":1}"""))
                .andExpect(status().isNotFound());
    }

    // ---------- remoção (somente ADMIN) ----------

    @Test
    void deleteAsAdminRemovesProduct() throws Exception {
        Product p = saveProduct("Descartável", "5.00");

        mvc.perform(delete("/products/{id}", p.getId()).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNoContent());

        assertFalse(productRepository.existsById(p.getId()));
    }

    @Test
    void deleteAsUserIsForbiddenAndKeepsProduct() throws Exception {
        Product p = saveProduct("Protegido", "5.00");

        mvc.perform(delete("/products/{id}", p.getId()).header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isForbidden());

        assertTrue(productRepository.existsById(p.getId()));
    }

    @Test
    void deleteWithoutTokenReturnsUnauthorized() throws Exception {
        Product p = saveProduct("Anônimo", "5.00");

        mvc.perform(delete("/products/{id}", p.getId())).andExpect(status().isUnauthorized());

        assertTrue(productRepository.existsById(p.getId()));
    }

    @Test
    void deleteOfMissingProductAsAdminReturnsNotFound() throws Exception {
        mvc.perform(delete("/products/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNotFound());
    }
}
