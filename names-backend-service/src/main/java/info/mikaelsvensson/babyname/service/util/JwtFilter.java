package info.mikaelsvensson.babyname.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
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

    private final AuthJwtService jwtService;
    private final String adminUsername;
    private final String adminPassword;

    public JwtFilter(@Autowired AuthJwtService jwtService,
                     @Value("${admin.user.username}") String adminUsername,
                     @Value("${admin.user.password}") String adminPassword) {
        this.jwtService = jwtService;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

//    @PostConstruct
//    public void printDemoToken() {
//        LOGGER.info("Token for test user: " + jwtService.encode(new JwtUser("test-user")));
//    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        final var authHeader = Optional.ofNullable(req.getHeader(HttpHeaders.AUTHORIZATION)).orElse("");
        if (authHeader.startsWith("Bearer ")) {
            final var userId = jwtService.decode(authHeader.substring("Bearer ".length())).getName();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new JwtUser(userId), null, Collections.singletonList(new SimpleGrantedAuthority("USER"))));
        } else if (authHeader.startsWith("Basic ")) {
            UsernamePasswordAuthenticationToken authRequest = new BasicAuthenticationConverter().convert(req);
            String password = authRequest.getCredentials().toString();
            String username = authRequest.getPrincipal().toString();
            if (username.equals(adminUsername) && password.equals(adminPassword)) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new JwtUser(username), null, Collections.singletonList(new SimpleGrantedAuthority("ADMIN"))));
            }
        }
        chain.doFilter(req, res);
    }
}
