package com.restosuite.security;

import com.restosuite.domain.UserAccount;
import com.restosuite.repo.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;

@Service
public class AuthUserService implements UserDetailsService {

    private final UserAccountRepository users;

    public AuthUserService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        UserAccount user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException(email));
        return new AuthUser(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getRestaurantId(),
                user.getBranchId(),
                user.isActive(),
                new LinkedHashSet<>(users.roleCodes(user.getId())),
                new LinkedHashSet<>(users.permissionCodes(user.getId())));
    }
}
