package info.mikaelsvensson.babyname.service.util;

import org.springframework.security.core.AuthenticatedPrincipal;

public class JwtUser implements AuthenticatedPrincipal {
    private final String userId;

    public JwtUser(String userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return userId;
    }
}
