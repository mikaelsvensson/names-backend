package info.mikaelsvensson.babyname.service.util.importer;

import org.slf4j.Logger;

import java.time.Instant;

public class ProgressLogger {
    private final Logger logger;
    private final int max;
    private int progress;
    private Instant lastPrint;
    private String progressFormatPattern;

    public ProgressLogger(Logger logger, int max, String progressFormatPattern) {
        this.logger = logger;
        this.max = max;
        this.progressFormatPattern = progressFormatPattern;
    }

    public void increment() {
        progress++;
        print();
    }

    private void print() {
        Instant now = Instant.now();
        if (lastPrint == null || progress == max || lastPrint.plusSeconds(1).isBefore(now)) {
            lastPrint = now;
            logger.info(String.format(progressFormatPattern, 100.0 * progress / max));
        }
    }
}
