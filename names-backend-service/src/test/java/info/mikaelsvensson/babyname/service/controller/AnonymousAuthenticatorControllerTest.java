package info.mikaelsvensson.babyname.service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.util.IdUtils;
import info.mikaelsvensson.babyname.service.util.auth.FacebookAuthenticator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Credits: https://www.baeldung.com/spring-boot-testing

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "email-smtp", "db"})
class AnonymousAuthenticatorControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createAndReadProfile() throws Exception {
        // ARRANGE by creating a user
        final var createUserResponseRaw = mockMvc.perform(
                post("/anonymous-authenticator/id"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", matchesPattern("[a-zA-Z0-9_-]+")))
                .andReturn().getResponse().getContentAsString();

        final var createUserResponse = objectMapper.readValue(createUserResponseRaw, CreateAnonymousUserResponse.class);
        final var userId = createUserResponse.id;

        final var tokenResponseRaw = mockMvc.perform(
                post("/token")
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "provider", UserProvider.ANONYMOUS.name(),
                                        "data", userId)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", matchesPattern("[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")))
                .andReturn().getResponse().getContentAsString();

        final var tokenResponse = objectMapper.readValue(tokenResponseRaw, AuthTokenResponse.class);
        final var token = tokenResponse.getToken();

        // ACT by requesting data-deletion process to be started
        final var profileResponseRaw = mockMvc.perform(
                get("/profile").header("Authorization", "Bearer " + token))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final var profileResponse = objectMapper.readValue(profileResponseRaw, ProfileResponse.class);

        assertThat(profileResponse.provider).isEqualTo(UserProvider.ANONYMOUS);
    }
}