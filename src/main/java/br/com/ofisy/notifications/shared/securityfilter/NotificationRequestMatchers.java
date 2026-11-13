package br.com.ofisy.notifications.shared.securityfilter;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Shared routing boundary for customer notification endpoints authenticated by the API Gateway. */
public final class NotificationRequestMatchers {
    private static final String ROOT = "/api/v1/notifications/service-orders";
    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();
    public static final RequestMatcher CUSTOMER_NOTIFICATIONS = new OrRequestMatcher(
            PATHS.matcher(HttpMethod.GET, ROOT),
            PATHS.matcher(HttpMethod.GET, ROOT + "/unread"),
            PATHS.matcher(HttpMethod.GET, ROOT + "/{id}"),
            PATHS.matcher(HttpMethod.PATCH, ROOT + "/{id}/read"));

    private NotificationRequestMatchers() {}
}
