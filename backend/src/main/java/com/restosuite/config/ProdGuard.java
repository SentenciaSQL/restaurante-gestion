package com.restosuite.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@Order(-10)
public class ProdGuard implements ApplicationRunner {

    private final AppProperties properties;

    public ProdGuard(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String secret = properties.getJwt().getSecret() == null ? "" : properties.getJwt().getSecret();
        if (secret.length() < 32 || secret.contains("dev-only") || secret.contains("change-me")) {
            throw new IllegalStateException("En producción JWT_SECRET debe ser aleatorio y tener al menos 32 caracteres");
        }
        String password = properties.getAdmin().getPassword() == null ? "" : properties.getAdmin().getPassword();
        if (password.equals("Admin123!") || password.equals("change-me") || password.length() < 8) {
            throw new IllegalStateException("En producción ADMIN_PASSWORD no puede ser un valor de desarrollo");
        }
    }
}
