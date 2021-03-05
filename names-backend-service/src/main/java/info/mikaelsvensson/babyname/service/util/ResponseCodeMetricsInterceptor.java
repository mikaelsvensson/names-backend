package info.mikaelsvensson.babyname.service.util;

import info.mikaelsvensson.babyname.service.util.metrics.MetricEvent;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static info.mikaelsvensson.babyname.service.util.auth.JwtFilter.ROLE_ADMIN;

@Component
public class ResponseCodeMetricsInterceptor implements HandlerInterceptor {

    @Autowired
    private Metrics metrics;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        final var status = response.getStatus();

        if (request.getUserPrincipal() instanceof Authentication) {
            Authentication principal = (Authentication) request.getUserPrincipal();
            if (principal.getAuthorities().stream().anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(ROLE_ADMIN))) {
                metrics.logEvent(MetricEvent.HTTP_ADMIN);
                return;
            }
        }

        if (200 <= status && status < 400) {
            metrics.logEvent(MetricEvent.HTTP_OK);
        } else if (400 <= status && status < 500) {
            metrics.logEvent(MetricEvent.HTTP_CLIENT_ERROR);
        } else if (500 <= status && status < 600) {
            metrics.logEvent(MetricEvent.HTTP_SERVER_ERROR);
        } else {
            metrics.logEvent(MetricEvent.HTTP_OTHER);
        }
    }
}
