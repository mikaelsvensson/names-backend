package info.mikaelsvensson.babyname.service.util.email;

public interface EmailSender {
    void send(String to, String subject, String textBody, String htmlBody) throws EmailSenderException;
}
