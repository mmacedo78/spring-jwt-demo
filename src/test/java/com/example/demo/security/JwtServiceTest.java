package com.example.demo.security;

import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "c2VncmVkby1kZS10ZXN0ZS1jb20tbWFpcy1kZS0zMi1ieXRlcy1vayE=";
    private static final String OTHER_SECRET = "b3V0cm8tc2VncmVkby10b3RhbG1lbnRlLWRpZmVyZW50ZS0zMi1ieXRlcyE=";

    private final JwtService service = new JwtService(SECRET, 3_600_000);

    @Test
    void generatedTokenIsValidAndCarriesUsername() {
        String token = service.generateToken("alice");

        assertTrue(service.isValid(token));
        assertEquals("alice", service.extractUsername(token));
    }

    @Test
    void tokenHasThreeJwtParts() {
        assertEquals(3, service.generateToken("alice").split("\\.").length);
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService expiring = new JwtService(SECRET, -1_000);

        assertFalse(expiring.isValid(expiring.generateToken("alice")));
    }

    @Test
    void tokenSignedWithAnotherKeyIsInvalid() {
        JwtService other = new JwtService(OTHER_SECRET, 3_600_000);

        assertFalse(service.isValid(other.generateToken("alice")));
    }

    @Test
    void tamperedSignatureIsInvalid() {
        String[] parts = service.generateToken("alice").split("\\.");
        char first = parts[2].charAt(0);
        String tamperedSignature = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        assertFalse(service.isValid(parts[0] + "." + parts[1] + "." + tamperedSignature));
    }

    @Test
    void garbageAndEmptyTokensAreInvalid() {
        assertFalse(service.isValid("isto-nao-e-um-jwt"));
        assertFalse(service.isValid(""));
    }

    @Test
    void secretShorterThan256BitsIsRejected() {
        String weak = Base64.getEncoder().encodeToString("curto".getBytes());

        assertThrows(WeakKeyException.class, () -> new JwtService(weak, 1000));
    }
}
