package com.example.quanliPT.security;

import com.example.quanliPT.repository.finance.InvoiceRepository;
import com.example.quanliPT.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("invoiceSecurity")
@RequiredArgsConstructor
public class InvoiceSecurity {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    public boolean canAccessTenantInvoices(Long tenantId, Authentication authentication) {
        if (authentication == null || tenantId == null) return false;
        if (isAdmin(authentication)) return true;

        return userRepository.findByUsername(authentication.getName())
                .map(user -> user.getId().equals(tenantId))
                .orElse(false);
    }

    public boolean canAccessInvoice(Long invoiceId, Authentication authentication) {
        if (authentication == null || invoiceId == null) return false;
        if (isAdmin(authentication)) return true;

        return invoiceRepository.findById(invoiceId)
                .map(invoice -> invoice.getContract() != null
                        && invoice.getContract().getTenant() != null
                        && userRepository.findByUsername(authentication.getName())
                                .map(user -> invoice.getContract().getTenant().getId().equals(user.getId()))
                                .orElse(false))
                .orElse(false);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}

