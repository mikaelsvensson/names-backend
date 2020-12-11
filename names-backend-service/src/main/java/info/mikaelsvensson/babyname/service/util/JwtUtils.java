package info.mikaelsvensson.babyname.service.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtUtils {

    private final Algorithm algorithm;

    public JwtUtils(@Value("${jwt.secret}") String secret) {
        algorithm = Algorithm.HMAC512(secret);
    }

    public String encode(JWTCreator.Builder jwt) {
        return jwt.sign(algorithm);
    }

    public DecodedJWT decode(String token) {
        return JWT.require(algorithm).build().verify(token);
    }
}
