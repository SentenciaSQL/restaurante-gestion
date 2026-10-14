package com.restosuite.config;

import com.restosuite.domain.Role;
import com.restosuite.domain.UserAccount;
import com.restosuite.repo.RestaurantRepository;
import com.restosuite.repo.RoleRepository;
import com.restosuite.repo.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppProperties properties;
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final RestaurantRepository restaurants;
    private final PasswordEncoder encoder;

    public AdminBootstrap(AppProperties properties, UserAccountRepository users, RoleRepository roles,
                          RestaurantRepository restaurants, PasswordEncoder encoder) {
        this.properties = properties;
        this.users = users;
        this.roles = roles;
        this.restaurants = restaurants;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.getAdmin().getEmail().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        if (restaurants.findById(1L).isEmpty()) {
            throw new IllegalStateException("La migración inicial no creó el restaurante");
        }
        Role admin = roles.findByCode("ADMIN").orElseThrow(() -> new IllegalStateException("Falta el rol ADMIN"));
        UserAccount user = new UserAccount();
        user.setRestaurantId(1L);
        user.setEmail(email);
        user.setFullName(properties.getAdmin().getName());
        user.setPasswordHash(encoder.encode(properties.getAdmin().getPassword()));
        user.setActive(true);
        users.saveAndFlush(user);
        users.addRole(user.getId(), admin.getId());
        log.info("Usuario administrador inicial creado para {}", email);
    }
}
