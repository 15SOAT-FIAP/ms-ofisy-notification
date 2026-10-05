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
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String ISSUER = "techchallenge-ofisy-auth"; // Should match the issuer of the core API
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

    private final SecretKey secretKey;

    public JwtAuthFilter(@Value("${app.jwt.secret}") String secret,
                         @Value("${app.jwt.issuer:techchallenge-ofisy-auth}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer("techchallenge-ofisy-api") // The original code uses "techchallenge-ofisy-api" as issuer
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            if (email != null && roles != null) {
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT Token: " + e.getMessage()); response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.getWriter().write("Unauthorized: Invalid token"); return;
        }

        filterChain.doFilter(request, response);
    }
}


