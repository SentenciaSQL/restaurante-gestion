package com.restosuite.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public class AuthUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final Long restaurantId;
    private final Long branchId;
    private final boolean active;
    private final Set<String> roles;
    private final Set<String> permissions;

    public AuthUser(Long id, String email, String password, String fullName, Long restaurantId, Long branchId,
                    boolean active, Set<String> roles, Set<String> permissions) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.restaurantId = restaurantId;
        this.branchId = branchId;
        this.active = active;
        this.roles = Set.copyOf(roles);
        this.permissions = Set.copyOf(permissions);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public boolean has(String permission) {
        return permissions.contains(permission);
    }
}
