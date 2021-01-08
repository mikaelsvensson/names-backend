package info.mikaelsvensson.babyname.service.util.email;

import info.mikaelsvensson.babyname.service.util.metrics.MetricEvent;
import info.mikaelsvensson.babyname.service.util.metrics.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;

@Service
@Profile("email-smtp")
public class SmtpSender implements EmailSender {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private Metrics metrics;

    @Override
    public void send(String to, String subject, String textBody, String htmlBody) throws EmailSenderException {
        try {
            final var message = mailSender.createMimeMessage();

            final var helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);

            mailSender.send(message);
            metrics.logEvent(MetricEvent.EMAIL_SENT);
        } catch (MessagingException e) {
            throw new EmailSenderException("Failed to send email", e);
        }
    }
}
