package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.domain.AppSetting;
import com.restosuite.domain.Restaurant;
import com.restosuite.repo.AppSettingRepository;
import com.restosuite.repo.RestaurantRepository;
import com.restosuite.security.AuthUser;
import com.restosuite.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class Support {

    private final CurrentUser currentUser;
    private final RestaurantRepository restaurants;
    private final AppSettingRepository settings;

    public Support(CurrentUser currentUser, RestaurantRepository restaurants, AppSettingRepository settings) {
        this.currentUser = currentUser;
        this.restaurants = restaurants;
        this.settings = settings;
    }

    public AuthUser user() {
        return currentUser.require();
    }

    public CurrentUser access() {
        return currentUser;
    }

    public Restaurant restaurant() {
        return restaurants.findById(user().getRestaurantId()).orElseThrow(() -> ApiException.notFound("El restaurante"));
    }

    public ZoneId zone() {
        return ZoneId.of(restaurant().getTimezone());
    }

    public String setting(String key, String fallback) {
        return settings.findByRestaurantIdAndSettingKey(restaurant().getId(), key)
                .map(AppSetting::getSettingValue)
                .orElse(fallback);
    }

    public boolean flag(String key, boolean fallback) {
        return Boolean.parseBoolean(setting(key, Boolean.toString(fallback)));
    }

    public Period period(LocalDate from, LocalDate to) {
        ZoneId zone = zone();
        LocalDate today = LocalDate.now(zone);
        LocalDate start = from == null ? today : from;
        LocalDate end = to == null ? start : to;
        if (end.isBefore(start)) {
            throw ApiException.badRequest("La fecha final es anterior a la inicial. Corrija el período.");
        }
        return new Period(start, end, start.atStartOfDay(zone).toInstant(), end.plusDays(1).atStartOfDay(zone).toInstant(), zone);
    }

    public void checkVersion(Long current, Long expected) {
        if (expected == null || current == null || !current.equals(expected)) {
            throw ApiException.conflict("Otro usuario modificó este registro. Actualice la pantalla e intente de nuevo.");
        }
    }

    public static String required(String value, String label, int max) {
        String clean = clean(value, max);
        if (clean == null) {
            throw ApiException.badRequest(label + " es obligatorio.");
        }
        return clean;
    }

    public static String clean(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > max) {
            throw ApiException.badRequest("El texto supera los " + max + " caracteres.");
        }
        return trimmed;
    }

    public static void password(String raw) {
        if (raw == null || raw.length() < 8 || raw.chars().noneMatch(Character::isLetter) || raw.chars().noneMatch(Character::isDigit)) {
            throw ApiException.badRequest("La contraseña debe tener al menos 8 caracteres e incluir letras y números.");
        }
    }

    public record Period(LocalDate from, LocalDate to, Instant start, Instant end, ZoneId zone) {
    }
}
