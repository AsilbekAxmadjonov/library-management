package com.library.management.config;

import com.library.management.domain.enums.MemberType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "library")
public class LibraryProperties {

    private Loan loan = new Loan();
    private MemberTypes memberTypes = new MemberTypes();
    private Scheduler scheduler = new Scheduler();


    @Data
    public static class Loan {
        private int defaultLoanDays = 14;
        private int extensionDays = 7;
    }

    @Data
    public static class MemberTypes {
        private MemberTypeConfig standard = new MemberTypeConfig(500,  5,  2, 0, 50000);
        private MemberTypeConfig student  = new MemberTypeConfig(250,  3,  1, 2, 25000);
        private MemberTypeConfig premium  = new MemberTypeConfig(750, 10,  4, 1, 100000);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemberTypeConfig {
        private long dailyRate;
        private int  maxBooks;
        private int  maxExtensions;
        private int  gracePeriodDays;
        private long maxUnpaidThreshold;
    }

    // ── Scheduler ──────────────────────────────────────────────────
    @Data
    public static class Scheduler {
        private String fineUpdateCron = "0 0 1 * * *";
    }

    // ── Helper — resolve config by member type ─────────────────────
    public MemberTypeConfig configFor(MemberType type) {
        return switch (type) {
            case STUDENT  -> memberTypes.getStudent();
            case PREMIUM  -> memberTypes.getPremium();
            case STANDARD -> memberTypes.getStandard();
        };
    }
}
