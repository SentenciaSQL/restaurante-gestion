package com.restosuite.web;

import com.restosuite.service.IdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentityController {

    private final IdentityService identity;

    public IdentityController(IdentityService identity) {
        this.identity = identity;
    }

    @PostMapping("/api/auth/login")
    public IdentityService.LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return identity.login(request.email(), request.password());
    }

    @GetMapping("/api/auth/me")
    public IdentityService.Profile me() {
        return identity.me();
    }

    @PutMapping("/api/auth/password")
    public void password(@Valid @RequestBody IdentityService.PasswordChange request) {
        identity.changePassword(request.currentPassword(), request.newPassword());
    }

    @GetMapping("/api/users")
    @PreAuthorize("hasAuthority('users.manage')")
    public Object users(@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size) {
        return identity.list(q, page, size);
    }

    @PostMapping("/api/users")
    @PreAuthorize("hasAuthority('users.manage')")
    public IdentityService.UserView create(@Valid @RequestBody IdentityService.UserWrite request) {
        return identity.create(request);
    }

    @PutMapping("/api/users/{id}")
    @PreAuthorize("hasAuthority('users.manage')")
    public IdentityService.UserView update(@PathVariable Long id, @Valid @RequestBody IdentityService.UserWrite request) {
        return identity.update(id, request);
    }

    @PatchMapping("/api/users/{id}/active")
    @PreAuthorize("hasAuthority('users.manage')")
    public IdentityService.UserView active(@PathVariable Long id, @RequestBody IdentityService.ActiveRequest request) {
        return identity.setActive(id, request.active());
    }

    @GetMapping("/api/staff")
    @PreAuthorize("hasAuthority('tables.operate') or hasAuthority('orders.create') or hasAuthority('cash.operate') or hasAuthority('users.manage')")
    public Object staff() {
        return identity.staff();
    }

    @GetMapping("/api/roles")
    @PreAuthorize("hasAuthority('users.manage')")
    public Object roles() {
        return identity.roles();
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }
}
