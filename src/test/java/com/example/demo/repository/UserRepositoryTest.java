package com.example.demo.repository;

import com.example.demo.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    void findByUsernameReturnsSavedUser() {
        repository.save(new AppUser("ana", "hash", "USER"));

        AppUser found = repository.findByUsername("ana").orElseThrow();

        assertNotNull(found.getId());
        assertEquals("USER", found.getRole());
    }

    @Test
    void findByUsernameReturnsEmptyWhenMissing() {
        assertTrue(repository.findByUsername("ninguem").isEmpty());
    }

    @Test
    void existsByUsernameReflectsPersistedState() {
        assertFalse(repository.existsByUsername("bia"));

        repository.save(new AppUser("bia", "hash", "ADMIN"));

        assertTrue(repository.existsByUsername("bia"));
    }

    @Test
    void duplicateUsernameViolatesUniqueConstraint() {
        repository.saveAndFlush(new AppUser("carlos", "hash", "USER"));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(new AppUser("carlos", "outro", "USER")));
    }
}
