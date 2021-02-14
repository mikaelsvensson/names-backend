package info.mikaelsvensson.babyname.service.util.auth;

import com.auth0.jwt.JWT;
import info.mikaelsvensson.babyname.service.model.ActionStatus;
import info.mikaelsvensson.babyname.service.model.ActionType;
import info.mikaelsvensson.babyname.service.model.User;
import info.mikaelsvensson.babyname.service.model.UserProvider;
import info.mikaelsvensson.babyname.service.repository.actions.ActionException;
import info.mikaelsvensson.babyname.service.repository.actions.ActionsRepository;
import info.mikaelsvensson.babyname.service.repository.users.UserException;
import info.mikaelsvensson.babyname.service.repository.users.UserRepository;
import info.mikaelsvensson.babyname.service.util.email.EmailSender;
import info.mikaelsvensson.babyname.service.util.email.EmailSenderException;
import info.mikaelsvensson.babyname.service.util.template.Template;
import info.mikaelsvensson.babyname.service.util.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EmailAuthenticator implements UserAuthenticator {

    public static class EmailVerificationTemplateContext {
        public final String url;

        public EmailVerificationTemplateContext(String url) {
            this.url = url;
        }
    }

    private static final String SYSTEM_NAME = "emailAuthenticator";

    private static final String JWT_AUDIENCE = EmailAuthenticator.class.getSimpleName();

    private User user;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActionsRepository actionsRepository;

    @Autowired
    private EmailSender emailSender;

    @Value("${actions.verifyEmailUrlTemplate}")
    private String verifyEmailUrlTemplate;

    @Override
    public String getId(String token) throws UserAuthenticatorException {
        try {
            return getEmailAddressFromToken(token);
        } catch (Exception e) {
            throw new UserAuthenticatorException(e);
        }
    }

    private String getEmailAddressFromToken(String token) {
        return jwtUtils.decode(token, JWT_AUDIENCE).getSubject();
    }

    public String getTokenForEmailAddress(String emailAddress) {
        return jwtUtils.encode(JWT
                .create()
                .withSubject(emailAddress)
                .withAudience(JWT_AUDIENCE)
        );
    }

    public void sendEmailVerification(String emailAddress, String redirectTo) throws UserAuthenticatorException {
        try {
            // Create action to trigger when link in email is clicked
            final var actionParameters = Map.of(
                    "emailAddress", emailAddress,
                    "redirectTo", redirectTo
            );
            final var action = actionsRepository.add(
                    getUser(),
                    ActionType.VERIFY_EMAIL,
                    actionParameters,
                    ActionStatus.PENDING);

            final var template = new Template();

            final var context = new EmailVerificationTemplateContext(verifyEmailUrlTemplate.replace("{actionId}", action.getId()));

            // Send email with link to trigger action
            emailSender.send(
                    emailAddress,
                    "Sign in to Names",
                    template.render("AuthEmailText.mustache", context),
                    template.render("AuthEmailHtml.mustache", context)
            );
        } catch (ActionException | EmailSenderException | TemplateException e) {
            throw new UserAuthenticatorException(e);
        }
    }

    private User getUser() {
        if (user == null) {
            try {
                user = userRepository.getByProvider(UserProvider.INTERNAL, SYSTEM_NAME);
            } catch (UserException e) {
                try {
                    user = userRepository.addFromProvider(UserProvider.INTERNAL, SYSTEM_NAME);
                } catch (UserException userException) {
                    throw new RuntimeException(userException);
                }
            }
        }
        return user;
    }

}
