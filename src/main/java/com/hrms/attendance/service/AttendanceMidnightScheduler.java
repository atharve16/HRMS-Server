package com.hrms.attendance.service;

import com.hrms.common.utils.CompanyTimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AttendanceMidnightScheduler {
    private static final Logger log = LoggerFactory.getLogger(AttendanceMidnightScheduler.class);

    private final AttendanceService attendanceService;
    private final CompanyTimeProvider timeProvider;

    public AttendanceMidnightScheduler(AttendanceService attendanceService, CompanyTimeProvider timeProvider) {
        this.attendanceService = attendanceService;
        this.timeProvider = timeProvider;
    }

    /**
     * Executes at midnight (00:00:00) every day in the company's configured timezone (defaults to Asia/Kolkata).
     * Automatically terminates all unfinished sessions from previous dates as INCOMPLETE_CHECKOUT.
     */
    @Scheduled(cron = "${app.attendance.midnight-cron:0 0 0 * * *}", zone = "${app.company.time-zone:Asia/Kolkata}")
    public void runMidnightClosure() {
        try {
            LocalDate today = timeProvider.companyDate();
            log.info("Starting automatic midnight attendance closure job for dates before {} in timezone {}",
                    today, timeProvider.companyZoneId());
            int closed = attendanceService.closeUnfinishedSessionsBefore(today);
            log.info("Automatic midnight attendance closure job completed. Terminated {} open sessions.", closed);
        } catch (Exception ex) {
            log.error("Automatic midnight attendance closure job failed: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Recovers from application restarts or missed scheduled executions by checking for any
     * unfinished sessions from past dates immediately when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            LocalDate today = timeProvider.companyDate();
            log.info("Application ready. Checking for any unclosed attendance sessions before {}", today);
            int closed = attendanceService.closeUnfinishedSessionsBefore(today);
            if (closed > 0) {
                log.info("Recovered and automatically closed {} unfinished attendance sessions from previous dates.", closed);
            }
        } catch (Exception ex) {
            log.warn("Attendance recovery check skipped on startup: {}", ex.getMessage());
        }
    }
}
