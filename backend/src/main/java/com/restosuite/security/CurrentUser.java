package com.restosuite.security;

import com.restosuite.common.ApiException;
import com.restosuite.domain.Branch;
import com.restosuite.repo.BranchRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final BranchRepository branches;

    public CurrentUser(BranchRepository branches) {
        this.branches = branches;
    }

    public AuthUser require() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "AUTH", "Inicie sesión para continuar.");
        }
        return user;
    }

    public void checkBranch(Long branchId) {
        AuthUser user = require();
        Branch branch = branches.findById(branchId).orElseThrow(() -> ApiException.notFound("La sucursal"));
        if (!branch.getRestaurantId().equals(user.getRestaurantId())) {
            throw ApiException.forbidden("No tiene acceso a esta sucursal.");
        }
        if (user.has("branches.switch")) {
            return;
        }
        if (user.getBranchId() == null || !user.getBranchId().equals(branchId)) {
            throw ApiException.forbidden("Su usuario solo puede operar la sucursal asignada.");
        }
    }

    public Long resolveBranch(Long requested) {
        AuthUser user = require();
        if (requested != null) {
            checkBranch(requested);
            return requested;
        }
        if (user.has("branches.switch")) {
            return null;
        }
        if (user.getBranchId() == null) {
            throw ApiException.badRequest("Seleccione una sucursal para continuar.");
        }
        return user.getBranchId();
    }
}
