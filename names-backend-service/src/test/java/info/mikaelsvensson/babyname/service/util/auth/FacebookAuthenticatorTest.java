package info.mikaelsvensson.babyname.service.util.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;

class FacebookAuthenticatorTest {

    private FacebookAuthenticator authenticator;

    @BeforeEach
    void setUp() throws InvalidKeyException, NoSuchAlgorithmException {
        authenticator = new FacebookAuthenticator("secret");
    }

    @Test
    void getToken() throws UserAuthenticatorException {
        assertThat(authenticator.getId(authenticator.getToken("userId"))).isEqualTo("userId");
    }
}