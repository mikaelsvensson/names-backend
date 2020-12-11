package info.mikaelsvensson.babyname.service.util;

import com.auth0.jwt.JWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthJwtService {
    @Autowired
    private JwtUtils jwtUtils;

    public String encode(JwtUser user) {
        return jwtUtils.encode(JWT.create().withSubject(user.getName()));
    }

    public JwtUser decode(String token) {
        return new JwtUser(jwtUtils.decode(token).getSubject());
    }
}
