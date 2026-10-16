package com.restosuite.service;

import com.restosuite.common.ApiException;
import com.restosuite.common.Codes;
import com.restosuite.common.PageResponse;
import com.restosuite.domain.Branch;
import com.restosuite.domain.Role;
import com.restosuite.domain.UserAccount;
import com.restosuite.repo.BranchRepository;
import com.restosuite.repo.RoleRepository;
import com.restosuite.repo.UserAccountRepository;
import com.restosuite.security.AuthUser;
import com.restosuite.security.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Service
public class IdentityService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final BranchRepository branches;
    private final PasswordEncoder encoder;
    private final Support support;
    private final AuditService audit;

    public IdentityService(AuthenticationManager authenticationManager, JwtService jwt, UserAccountRepository users,
                           RoleRepository roles, BranchRepository branches, PasswordEncoder encoder, Support support, AuditService audit) {
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
        this.users = users;
        this.roles = roles;
        this.branches = branches;
        this.encoder = encoder;
        this.support = support;
        this.audit = audit;
    }

    public LoginResponse login(String email, String password) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email.trim().toLowerCase(Locale.ROOT), password));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        AuthUser user = (AuthUser) authentication.getPrincipal();
        audit.record("LOGIN", "USER", user.getId(), user.getEmail());
        return new LoginResponse(jwt.issue(user), jwt.expiresInSeconds(), profile(user));
    }

    public Profile me() {
        return profile(support.user());
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        UserAccount account = users.findById(support.user().getId()).orElseThrow();
        if (!encoder.matches(currentPassword, account.getPasswordHash())) {
            throw ApiException.badRequest("La contraseña actual no coincide.");
        }
        Support.password(newPassword);
        account.setPasswordHash(encoder.encode(newPassword));
        audit.record("PASSWORD_CHANGE", "USER", account.getId(), account.getEmail());
    }

    @Transactional(readOnly = true)
    public PageResponse<UserView> list(String q, int page, int size) {
        Long restaurantId = support.user().getRestaurantId();
        var result = users.findAll((root, query, cb) -> {
            var restaurant = cb.equal(root.get("restaurantId"), restaurantId);
            if (q == null || q.isBlank()) {
                return restaurant;
            }
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.and(restaurant, cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like)));
        }, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("fullName")));
        return new PageResponse<>(result.getContent().stream().map(this::toView).toList(), result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Transactional(readOnly = true)
    public List<StaffView> staff() {
        return directory(null);
    }

    @Transactional(readOnly = true)
    public List<StaffView> waiters() {
        return directory(Codes.ROLE_WAITER);
    }

    private List<StaffView> directory(String requiredRole) {
        Long restaurantId = support.user().getRestaurantId();
        return users.findAll().stream()
                .filter(user -> user.isActive() && restaurantId.equals(user.getRestaurantId()))
                .map(user -> new StaffView(user.getId(), user.getFullName(), users.roleCodes(user.getId())))
                .filter(user -> requiredRole == null || user.roles().contains(requiredRole))
                .sorted((left, right) -> left.fullName().compareToIgnoreCase(right.fullName()))
                .toList();
    }

    @Transactional
    public UserView create(UserWrite request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Ya existe un usuario con ese correo.");
        }
        Support.password(request.password());
        UserAccount account = new UserAccount();
        account.setRestaurantId(support.user().getRestaurantId());
        account.setEmail(email);
        account.setPasswordHash(encoder.encode(request.password()));
        account.setActive(true);
        apply(account, request);
        users.saveAndFlush(account);
        replaceRoles(account, request.roleCodes());
        audit.record("USER_CREATE", "USER", account.getId(), email);
        return toView(account);
    }

    @Transactional
    public UserView update(Long id, UserWrite request) {
        UserAccount account = owned(id);
        apply(account, request);
        if (request.password() != null && !request.password().isBlank()) {
            Support.password(request.password());
            account.setPasswordHash(encoder.encode(request.password()));
        }
        replaceRoles(account, request.roleCodes());
        audit.record("USER_UPDATE", "USER", account.getId(), account.getEmail());
        return toView(account);
    }

    @Transactional
    public UserView setActive(Long id, boolean active) {
        UserAccount account = owned(id);
        if (!active && account.getId().equals(support.user().getId())) {
            throw ApiException.badRequest("No puede desactivar su propio usuario.");
        }
        if (!active && users.roleCodes(account.getId()).contains(Codes.ROLE_ADMIN)) {
            long admins = users.findAll().stream()
                    .filter(user -> user.isActive() && user.getRestaurantId().equals(account.getRestaurantId()))
                    .filter(user -> users.roleCodes(user.getId()).contains(Codes.ROLE_ADMIN))
                    .count();
            if (admins <= 1) {
                throw ApiException.conflict("Debe quedar al menos un administrador activo.");
            }
        }
        account.setActive(active);
        audit.record(active ? "USER_ACTIVATE" : "USER_DEACTIVATE", "USER", account.getId(), account.getEmail());
        return toView(account);
    }

    public List<RoleView> roles() {
        return roles.findAllByOrderByNameAsc().stream()
                .map(role -> new RoleView(role.getId(), role.getCode(), role.getName(), role.getDescription(), roles.permissionCodes(role.getId())))
                .toList();
    }

    private void apply(UserAccount account, UserWrite request) {
        account.setFullName(Support.required(request.fullName(), "El nombre", 160));
        account.setPhone(Support.clean(request.phone(), 40));
        if (request.branchId() != null) {
            Branch branch = branches.findByIdAndRestaurantId(request.branchId(), support.user().getRestaurantId())
                    .orElseThrow(() -> ApiException.notFound("La sucursal"));
            account.setBranchId(branch.getId());
        } else {
            account.setBranchId(null);
        }
    }

    private void replaceRoles(UserAccount account, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw ApiException.badRequest("Asigne al menos un rol.");
        }
        boolean actorIsAdmin = support.user().getRoles().contains(Codes.ROLE_ADMIN);
        boolean currentlyAdmin = users.roleCodes(account.getId()).contains(Codes.ROLE_ADMIN);
        boolean wantsAdmin = roleCodes.stream().anyMatch(Codes.ROLE_ADMIN::equals);
        if ((wantsAdmin || currentlyAdmin) && !actorIsAdmin) {
            throw ApiException.forbidden("Solo un administrador puede asignar o retirar el rol administrador.");
        }
        users.clearRoles(account.getId());
        for (String code : new LinkedHashSet<>(roleCodes)) {
            Role role = roles.findByCode(code).orElseThrow(() -> ApiException.badRequest("El rol " + code + " no existe."));
            users.addRole(account.getId(), role.getId());
        }
    }

    private UserAccount owned(Long id) {
        UserAccount account = users.findById(id).orElseThrow(() -> ApiException.notFound("El usuario"));
        if (!account.getRestaurantId().equals(support.user().getRestaurantId())) {
            throw ApiException.notFound("El usuario");
        }
        return account;
    }

    private UserView toView(UserAccount account) {
        return new UserView(account.getId(), account.getFullName(), account.getEmail(), account.getPhone(), account.getBranchId(),
                account.isActive(), users.roleCodes(account.getId()));
    }

    private Profile profile(AuthUser user) {
        var restaurant = support.restaurant();
        return new Profile(user.getId(), user.getFullName(), user.getEmail(), user.getRestaurantId(), restaurant.getName(),
                restaurant.getCurrencyCode(), restaurant.getTimezone(), restaurant.getLocale(), user.getBranchId(),
                List.copyOf(user.getRoles()), List.copyOf(user.getPermissions()));
    }

    public record Login(String email, String password) {
    }

    public record LoginResponse(String accessToken, long expiresIn, Profile user) {
    }

    public record Profile(Long id, String fullName, String email, Long restaurantId, String restaurantName, String currency,
                          String timezone, String locale, Long branchId, List<String> roles, List<String> permissions) {
    }

    public record UserWrite(@Email @NotBlank String email, @NotBlank String fullName, String phone, String password,
                            Long branchId, @NotEmpty List<String> roleCodes) {
    }

    public record UserView(Long id, String fullName, String email, String phone, Long branchId, boolean active, List<String> roles) {
    }

    public record PasswordChange(@NotBlank String currentPassword, @NotBlank String newPassword) {
    }

    public record RoleView(Long id, String code, String name, String description, List<String> permissions) {
    }

    public record StaffView(Long id, String fullName, List<String> roles) {
    }

    public record ActiveRequest(boolean active) {
    }
}
