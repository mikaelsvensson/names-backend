package info.mikaelsvensson.babyname.service.util.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Profile("email-mailgun")
public class MailgunSender implements EmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailgunSender.class);

    final private String from;
    final private String endpoint;
    final private String apiKey;

    public MailgunSender(
            @Value("${mailgun.from}") String from,
            @Value("${mailgun.endpoint}") String endpoint,
            @Value("${mailgun.apiKey}") String apiKey) {
        this.from = from;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    @Override
    public void send(String to, String subject, String textBody, String htmlBody) throws EmailSenderException {

        try {
            final var body = ofFormData(Map.of(
                    "from", from,
                    "to", to,
                    "subject", subject,
                    "text", textBody,
                    "html", htmlBody
            ));
            final var request = HttpRequest.newBuilder(URI.create(endpoint))
                    .POST(body)
                    .setHeader("User-Agent", "Names HttpClient Bot") // add request header
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            final var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("api", apiKey.toCharArray());
                }
            }).build();

            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            final var rawBody = response.body();
            if (response.statusCode() == HttpStatus.OK.value()) {
                LOGGER.info("Sent email to {}", to);
            } else {
                LOGGER.warn("Failed to send email to {} with status code {} and body {}", to, response.statusCode(), rawBody);
            }
        } catch (IOException | InterruptedException e) {
            throw new EmailSenderException("Failed to send email", e);
        }
    }

    // Credits: https://mkyong.com/java/java-11-httpclient-examples/
    public static HttpRequest.BodyPublisher ofFormData(Map<String, String> data) {
        var builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

}
