package info.mikaelsvensson.babyname.service.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

@Service
public class FacebookAuthenticator implements UserAuthenticator {

    @Value("${facebook.secret}")
    private String secret;

    @Override
    public String getId(String token) throws UserAuthenticatorException {
        try {
            final var signature = Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.')));
            final var encodedPayload = token.substring(token.indexOf('.') + 1);
            final var payload = Base64.getUrlDecoder().decode(encodedPayload);
            final var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            if (Arrays.equals(signature, mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)))) {
                final var payloadObject = new ObjectMapper().readValue(payload, new TypeReference<HashMap<String, String>>() {
                });
                return payloadObject.get("user_id");
            } else {
                throw new UserAuthenticatorException("Invalid signature");
            }
        } catch (InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            throw new UserAuthenticatorException(e);
        }
    }
}
