package br.com.ofisy.notifications.shared.securityfilter;

import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Shared routing boundary for customer notification endpoints authenticated by the API Gateway. */
public final class NotificationRequestMatchers {
    public static final RequestMatcher CUSTOMER_NOTIFICATIONS = PathPatternRequestMatcher.withDefaults()
            .matcher("/api/v1/notifications/service-orders/**");

    private NotificationRequestMatchers() {}
}
