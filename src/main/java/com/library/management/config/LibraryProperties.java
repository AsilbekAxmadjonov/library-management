package com.library.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "library")
public class LibraryProperties {

    private Loan loan = new Loan();
    private Fine fine = new Fine();
    private Scheduler scheduler = new Scheduler();

    @Data
    public static class Loan {
        private int maxBooksPerMember = 5;
        private int defaultLoanDays = 14;
        private int maxExtensions = 2;
        private int extensionDays = 7;
    }

    @Data
    public static class Fine {
        private long dailyRate = 500;
        private long maxUnpaidThreshold = 50000;
    }

    @Data
    public static class Scheduler {
        private String fineUpdateCron = "0 0 1 * * *";
    }
}
