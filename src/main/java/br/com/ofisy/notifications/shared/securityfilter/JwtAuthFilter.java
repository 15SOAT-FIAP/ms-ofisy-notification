package br.com.ofisy.notifications.shared.securityfilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String CUSTOMER_ID_HEADER = "X-Customer-Id";
    private static final Set<String> EMPLOYEE_ROLES = Set.of("ADMIN", "ATTENDANT", "MECHANIC", "STOCKMAN");

    private final SecretKey secretKey;
    private final String issuer;

    @Value("${app.jwt.customer-secret:${app.jwt.secret}}")
    private String customerSecret;

    public JwtAuthFilter(@Value("${app.jwt.secret}") String secret,
                         @Value("${app.jwt.issuer:techchallenge-ofisy-api}") String issuer) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("app.jwt.secret must be configured externally");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("app.jwt.issuer must not be blank");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER.length());
        boolean customerRoute = NotificationRequestMatchers.CUSTOMER_NOTIFICATIONS.matches(request);

        try {
            if (customerRoute) {
                authenticateCustomer(request, token);
            } else {
                authenticateEmployee(request, token);
            }
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            logger.warn("Invalid JWT for notification route");
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateEmployee(HttpServletRequest request, String token) {
        Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            Object roleClaim = claims.get("role");
            if (email == null || email.isBlank() || claims.getExpiration() == null
                    || !(roleClaim instanceof String role) || !EMPLOYEE_ROLES.contains(role)) {
                throw new IllegalArgumentException("Invalid employee token claims");
            }
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(email, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateCustomer(HttpServletRequest request, String token) {
        var key = customerSecret == null ? secretKey
                : Keys.hmacShaKeyFor(customerSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer("techchallenge-ofisy-auth")
                .build().parseSignedClaims(token).getPayload();
        String subject = claims.getSubject();
        String forwardedId = request.getHeader(CUSTOMER_ID_HEADER);
        if (claims.getExpiration() == null || subject == null || !subject.equals(forwardedId)
                || !UUID.fromString(subject).toString().equals(subject)) {
            throw new IllegalArgumentException("Invalid customer identity");
        }
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                subject, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
    }
}


