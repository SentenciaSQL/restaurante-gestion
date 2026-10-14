package com.restosuite.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Admin admin = new Admin();
    private Storage storage = new Storage();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMinutes = 720;
    }

    @Getter
    @Setter
    public static class Cors {
        private String origins = "http://localhost:4200";
    }

    @Getter
    @Setter
    public static class Admin {
        private String email = "admin@restosuite.local";
        private String password = "Admin123!";
        private String name = "Administrador";
    }

    @Getter
    @Setter
    public static class Storage {
        private String path = "./uploads";
    }
}
