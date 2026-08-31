package com.example.quanliPT.security;

import com.example.quanliPT.repository.contract.ContractRepository;
import com.example.quanliPT.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("contractSecurity")
@RequiredArgsConstructor
public class ContractSecurity {

    private final UserRepository userRepository;
    private final ContractRepository contractRepository;

    public boolean canAccessTenantContracts(Long tenantId, Authentication authentication) {
        if (authentication == null || tenantId == null) return false;
        if (isAdmin(authentication)) return true;

        return userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId().equals(tenantId))
                .orElse(false);
    }

    public boolean canAccessContract(Long contractId, Authentication authentication) {
        if (authentication == null || contractId == null) return false;
        if (isAdmin(authentication)) return true;

        return userRepository.findByUsername(authentication.getName())
                .flatMap(user -> contractRepository.findById(contractId)
                        .map(contract -> contract.getTenant() != null
                                && contract.getTenant().getId().equals(user.getId())))
                .orElse(false);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}

