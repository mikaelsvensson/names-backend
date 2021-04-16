package info.mikaelsvensson.babyname.service.util.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Map;

@Service
public class FacebookAuthenticator implements UserAuthenticator {

    private final Base64.Decoder urlDecoder;
    private final Mac mac;
    private final ObjectMapper objectMapper;
    private final Base64.Encoder urlEncoder;

    public FacebookAuthenticator(@Value("${facebook.secret}") String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        this.urlDecoder = Base64.getUrlDecoder();
        this.urlEncoder = Base64.getUrlEncoder();
        this.mac = Mac.getInstance("HmacSHA256");
        this.mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getId(String token) throws UserAuthenticatorException {
        try {
            final var signature = urlDecoder.decode(token.substring(0, token.indexOf('.')));
            final var encodedPayload = token.substring(token.indexOf('.') + 1);
            final var payload = urlDecoder.decode(encodedPayload);
            if (Arrays.equals(signature, mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)))) {
                final var payloadObject = objectMapper.readValue(payload, new TypeReference<HashMap<String, String>>() {
                });
                return payloadObject.get("user_id");
            } else {
                throw new UserAuthenticatorException("Invalid signature");
            }
        } catch (IOException e) {
            throw new UserAuthenticatorException(e);
        }
    }

    public String getToken(String id) throws UserAuthenticatorException {
        try {
            final var payloadObject = Map.of("user_id", id);
            final var encodedPayload = urlEncoder.encodeToString(objectMapper.writeValueAsString(payloadObject).getBytes(StandardCharsets.UTF_8));
            final var signature = mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8));
            final var encodedSignature = urlEncoder.encodeToString(signature);
            return encodedSignature + '.' + encodedPayload;
        } catch (JsonProcessingException e) {
            throw new UserAuthenticatorException(e);
        }
    }
}
