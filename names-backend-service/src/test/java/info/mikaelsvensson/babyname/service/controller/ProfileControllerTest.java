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
@ActiveProfiles({"test", "email-smtp"})
class ProfileControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FacebookAuthenticator facebookAuthenticator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void name() throws Exception {
        // ARRANGE by creating a user
        mockMvc.perform(
                post("/token")
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "provider", UserProvider.FACEBOOK.name(),
                                        "data", facebookAuthenticator.getToken("alice"))))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", matchesPattern("[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")));

        // ACT by requesting data-deletion process to be started
        final var requestDeletionResponseRaw = mockMvc.perform(
                post("/profile/delete-facebook-data-request")
                        .content("signed_request=" + facebookAuthenticator.getToken("alice"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final var requestDeletionResponse = objectMapper.readValue(requestDeletionResponseRaw, FacebookDeleteDataResponse.class);
        final var actionId = requestDeletionResponse.confirmation_code;

        assertThat(requestDeletionResponse.url).isEqualTo("https://example.com/profile/delete-facebook-data-request/" + actionId);
        assertThat(requestDeletionResponse.confirmation_code).hasSize(IdUtils.random().length());

        // ACT by visiting status URL
        final var requestStatusRedirectLocation = mockMvc.perform(
                get(requestDeletionResponse.url.substring("https://example.com".length())))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getHeader("Location");

        assertThat(requestStatusRedirectLocation).isEqualTo("https://example.com/action/" + actionId);
    }
}