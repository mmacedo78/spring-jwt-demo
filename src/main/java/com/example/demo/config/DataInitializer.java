package com.example.demo.config;

import com.example.demo.model.AppUser;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

// Usuários de demonstração: somente no perfil local (nunca em produção)
@Profile("local")
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (!repo.existsByUsername("admin")) {
                repo.save(new AppUser("admin", encoder.encode("admin123"), "ADMIN"));
            }
            if (!repo.existsByUsername("user")) {
                repo.save(new AppUser("user", encoder.encode("user123"), "USER"));
            }
        };
    }
}
