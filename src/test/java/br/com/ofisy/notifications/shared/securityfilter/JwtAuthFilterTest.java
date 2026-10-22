package br.com.ofisy.notifications.shared.securityfilter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import io.jsonwebtoken.security.WeakKeyException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String SECRET = "supersecretkeythatisverylongandsecureforhs256";
    private static final String ISSUER = "configured-notification-issuer";

    private JwtAuthFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter printWriter;

    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthFilter(SECRET, ISSUER);

        validToken = Jwts.builder()
                .subject("123")
                .claim("roles", List.of("ROLE_USER"))
                .issuer(ISSUER)
                .expiration(new Date(System.currentTimeMillis() + 100000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsIssuerDifferentFromConfiguration() throws Exception {
        String token = Jwts.builder().subject("123").issuer("techchallenge-ofisy-api")
                .claim("roles", List.of("ROLE_ADMIN"))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertRejected(token);
    }

    @Test
    void rejectsSignatureWithAnotherSecret() throws Exception {
        String token = Jwts.builder().subject("123").issuer(ISSUER)
                .claim("roles", List.of("ROLE_ADMIN"))
                .signWith(Keys.hmacShaKeyFor("another-secret-at-least-32-bytes-long".getBytes(StandardCharsets.UTF_8))).compact();
        assertRejected(token);
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String token = Jwts.builder().subject("123").issuer(ISSUER)
                .claim("roles", List.of("ROLE_ADMIN"))
                .expiration(new Date(0))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertRejected(token);
    }

    @Test
    void refusesMissingBlankOrWeakSecretsAndBlankIssuer() {
        assertThrows(IllegalArgumentException.class, () -> new JwtAuthFilter(null, ISSUER));
        assertThrows(IllegalArgumentException.class, () -> new JwtAuthFilter(" ", ISSUER));
        assertThrows(WeakKeyException.class, () -> new JwtAuthFilter("short", ISSUER));
        assertThrows(IllegalArgumentException.class, () -> new JwtAuthFilter(SECRET, " "));
        assertThrows(IllegalArgumentException.class, () -> new JwtAuthFilter(SECRET, null));
    }

    private void assertRejected(String token) throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(response.getWriter()).thenReturn(printWriter);
        filter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testNoToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

