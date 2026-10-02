package com.resturant.management.rms.security;

import com.resturant.management.rms.auth.JwtService;
import com.resturant.management.rms.branch.BranchContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the {@code Authorization: Bearer …} header and, when the token is a
 * valid <em>access</em> token, populates the security context.
 *
 * <p>Never rejects a request itself — an unauthenticated context is handled
 * downstream by the authorization rules, so public endpoints still work.
 *
 * <p>It also establishes the branch for the request, from the signed claim and
 * from nowhere else. Every scoped query reads it through
 * {@code BranchTenantResolver}, so this is the single point where a request
 * acquires a shop — and the {@code finally} below is what stops the next
 * request on this thread inheriting it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } finally {
            // Threads are pooled. A branch left behind here is the next
            // request's branch, which is the bug this whole package exists to
            // prevent arriving by the back door. One finally around
            // everything, rather than one per early return.
            BranchContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return;
        }

        String token = header.substring(PREFIX.length());

        // Already authenticated on this request — nothing to do.
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        try {
            if (jwtService.isValid(token, true)) {
                String username = jwtService.extractUsername(token);
                UserDetails user = userDetailsService.loadUserByUsername(username);

                if (user.isEnabled() && user.isAccountNonLocked()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // From the claim, not from the request. A token issued
                    // before V17 carries none, which falls back to the branch
                    // everything was backfilled to rather than failing a
                    // cashier's session mid-shift.
                    Long branchId = jwtService.extractBranchId(token);
                    BranchContext.set(branchId != null ? branchId : BranchContext.DEFAULT_BRANCH);
                }
            }
        } catch (Exception e) {
            // A bad token must not produce a 500 — leave the context empty and
            // let the authorization rules return 401/403.
            log.debug("Could not authenticate from token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}
