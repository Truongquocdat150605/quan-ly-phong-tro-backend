package com.example.quanliPT.security;

import com.example.quanliPT.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("contractSecurity")
@RequiredArgsConstructor
public class ContractSecurity {

    private final UserRepository userRepository;

    public boolean canAccessTenantContracts(Long tenantId, Authentication authentication) {
        if (authentication == null || tenantId == null) return false;
        if (isAdmin(authentication)) return true;

        return userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId().equals(tenantId))
                .orElse(false);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}

