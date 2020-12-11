package info.mikaelsvensson.babyname.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private AuthJwtService jwtService;

    @PostConstruct
    public void printDemoToken() {
        LOGGER.info("Token for test user: " + jwtService.encode(new JwtUser("test-user")));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        final var authHeader = Optional.ofNullable(req.getHeader(HttpHeaders.AUTHORIZATION)).orElse("");
        if (authHeader.startsWith("Bearer ")) {
            final var userId = jwtService.decode(authHeader.substring("Bearer ".length())).getName();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new JwtUser(userId), null, Collections.emptyList()));
        }
        chain.doFilter(req, res);
    }
}
