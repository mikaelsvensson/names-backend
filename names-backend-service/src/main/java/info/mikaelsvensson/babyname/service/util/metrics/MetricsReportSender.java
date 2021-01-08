package info.mikaelsvensson.babyname.service.util.metrics;

import info.mikaelsvensson.babyname.service.util.email.EmailSender;
import info.mikaelsvensson.babyname.service.util.email.EmailSenderException;
import info.mikaelsvensson.babyname.service.util.template.Template;
import info.mikaelsvensson.babyname.service.util.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MetricsReportSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsReportSender.class);

//    @Autowired
//    private TaskScheduler scheduler;

    @Autowired
    private EmailSender emailSender;

    @Value("${metricsReport.to}")
    private String metricsReportTo;

    @Autowired
    private Metrics metrics;

//    @Scheduled(fixedRateString = "PT5S")
//    private void testTask() {
//        LOGGER.info("PONG");
//        scheduler.schedule(() -> LOGGER.info("Executed PONG"), Instant.now());
//    }

    @Scheduled(cron = "${metricsReport.schedule}") // Every minute
    private synchronized void dailyReport() {
        LOGGER.info("Once per day...");

        final var context = new MetricsReportTemplateContext();

        metrics.getAndReset().forEach((key, value) -> context.addEvent(key.name(), value));

        final var template = new Template();

        try {
            LOGGER.info(template.render("DailyReportText.mustache", context));

            emailSender.send(
                    metricsReportTo,
                    "Daily Stats",
                    template.render("DailyReportText.mustache", context),
                    template.render("DailyReportHtml.mustache", context)
            );
        } catch (TemplateException | EmailSenderException e) {
            LOGGER.warn("Could not send daily report", e);
        }
    }
}
