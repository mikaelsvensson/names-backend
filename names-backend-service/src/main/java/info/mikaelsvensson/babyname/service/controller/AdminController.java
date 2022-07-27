package info.mikaelsvensson.babyname.service.controller;

import info.mikaelsvensson.babyname.service.repository.RepositoryHealthcheck;
import info.mikaelsvensson.babyname.service.repository.anonymousauthenticator.AnonymousAuthenticatorException;
import info.mikaelsvensson.babyname.service.repository.names.NameException;
import info.mikaelsvensson.babyname.service.repository.relationships.RelationshipException;
import info.mikaelsvensson.babyname.service.repository.votes.VoteException;
import info.mikaelsvensson.babyname.service.util.SyllableUpdater;
import info.mikaelsvensson.babyname.service.util.email.EmailSender;
import info.mikaelsvensson.babyname.service.util.email.EmailSenderException;
import info.mikaelsvensson.babyname.service.util.importer.DatabaseSnapshot;
import info.mikaelsvensson.babyname.service.util.importer.Importer;
import info.mikaelsvensson.babyname.service.util.nameprovider.*;
import info.mikaelsvensson.babyname.service.util.template.Template;
import info.mikaelsvensson.babyname.service.util.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("admin")
public class AdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

    private final EmailSender sender;

    private final RepositoryHealthcheck repositoryHealthcheck;
    private final AvoindataNameImporter avoindataNameImporter;
    private final DstNameImporter dstNameImporter;
    private final ScbNameImporter scbNameImporter;
    private final SsaNameImporter ssaNameImporter;
    private final SsbNameImporter ssbNameImporter;
    private final SyllableUpdater syllableUpdater;
    private final Importer importer;

    public AdminController(
            @Autowired EmailSender sender,
            @Autowired RepositoryHealthcheck repositoryHealthcheck,
            @Autowired AvoindataNameImporter avoindataNameImporter,
            @Autowired DstNameImporter dstNameImporter,
            @Autowired ScbNameImporter scbNameImporter,
            @Autowired SsaNameImporter ssaNameImporter,
            @Autowired SsbNameImporter ssbNameImporter,
            @Autowired SyllableUpdater syllableUpdater,
            @Autowired Importer importer) {
        this.sender = sender;
        this.repositoryHealthcheck = repositoryHealthcheck;
        this.avoindataNameImporter = avoindataNameImporter;
        this.dstNameImporter = dstNameImporter;
        this.scbNameImporter = scbNameImporter;
        this.ssaNameImporter = ssaNameImporter;
        this.ssbNameImporter = ssbNameImporter;
        this.syllableUpdater = syllableUpdater;
        this.importer = importer;
    }

    @PostMapping("import/avoindata")
    public void importAvoindata(Authentication authentication) {
        LOGGER.info("User {} wants to send import data from AVOINDATA", authentication.getName());
        avoindataNameImporter.load();
    }

    @PostMapping("import/dst")
    public void importDst(Authentication authentication) {
        LOGGER.info("User {} wants to import data from DST", authentication.getName());
        dstNameImporter.load();
    }

    @PostMapping("import/scb")
    public void importScb(Authentication authentication) {
        LOGGER.info("User {} wants to import data from SCB", authentication.getName());
        scbNameImporter.load();
    }

    @PostMapping("import/ssa")
    public void importSsa(Authentication authentication) {
        LOGGER.info("User {} wants to import data from SSA", authentication.getName());
        ssaNameImporter.load();
    }

    @PostMapping("import/ssb")
    public void importSsb(Authentication authentication) {
        LOGGER.info("User {} wants to import data from SSB", authentication.getName());
        ssbNameImporter.load();
    }

    @PostMapping(value = "import/postgres-sql-export", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void importPostgresExport(Authentication authentication, @RequestParam("file") MultipartFile file) {
        LOGGER.info("User {} wants to import data from a Postgres export file", authentication.getName());
        LOGGER.info(String.valueOf(file.getSize()));

        try {
            Path path = Files.createTempFile("postgres-sql-export", ".sql");
            file.transferTo(path);
            importer.doImport(DatabaseSnapshot.readSnapshot(path));
        } catch (IOException | RelationshipException | AnonymousAuthenticatorException | NameException | VoteException e) {
            LOGGER.error("Could import data", e);
        }
    }

    @PostMapping("update-syllable-counts")
    public void updateSyllableCounts(Authentication authentication) {
        LOGGER.info("User {} wants update the syllable counts", authentication.getName());
        syllableUpdater.load();
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
            repositoryHealthcheck.ping();
        } catch (Throwable e) {
            LOGGER.error("Could not ping database", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
