package com.example.quanliPT.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    // ✅ KHÔNG còn inject UserDetailsService — hoàn toàn Stateless

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Không có Bearer token → bỏ qua, tiếp tục filter chain
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);

            if (jwtUtils.validateToken(token)) {
                String username = jwtUtils.extractUsername(token);
                String rolesString = jwtUtils.extractRole(token);

                if (username != null && rolesString != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // ✅ Build authorities từ roles trong token — KHÔNG truy vấn DB
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                            .map(r -> r.toUpperCase())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("✅ [JWT Filter] Authenticated: " + username + " | Authorities: " + authorities);
                }
            } else {
                System.out.println("⚠️ [JWT Filter] Token không hợp lệ: " + request.getRequestURI());
            }

        } catch (Exception e) {
            System.err.println("❌ [JWT Filter error] " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
