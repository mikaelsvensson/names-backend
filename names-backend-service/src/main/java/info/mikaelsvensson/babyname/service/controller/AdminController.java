package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.util.email.EmailSender;
import info.mikaelsvensson.babyname.service.util.email.EmailSenderException;
import info.mikaelsvensson.babyname.service.util.template.Template;
import info.mikaelsvensson.babyname.service.util.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

@RestController
@RequestMapping("admin")
public class AdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

    private final EmailSender sender;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminController(@Autowired EmailSender sender,
                           @Autowired NamedParameterJdbcTemplate jdbcTemplate) {
        this.sender = sender;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("test-mail")
    @ResponseStatus(HttpStatus.CREATED)
    public void sendTestMail(Authentication authentication,
                             @Value("${admin.testMail.to}") String to
    ) {
        LOGGER.info("User {} wants to send test mail to {}", authentication.getName(), to);
        try {
            final var template = new Template();
            sender.send(
                    to,
                    "Test mail",
                    template.render("AdminTestMailText.mustache", null),
                    template.render("AdminTestMailHtml.mustache", null));
            LOGGER.info("Test mail sent to {}", to);
        } catch (EmailSenderException | TemplateException e) {
            LOGGER.warn("Failed to send test mail", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        }
    }

    @GetMapping("ping")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ping() {
        try {
            final var isResultReturned = jdbcTemplate.queryForRowSet("SELECT 1", Collections.emptyMap()).next();
            if (!isResultReturned) {
                throw new Exception("Could not ping database (no result returned)");
            }
        } catch (Throwable e) {
            LOGGER.error("Could not ping database", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
