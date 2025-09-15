package com.zettix.scheduler;

import com.zettix.service.GmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailProcessingJob implements Job {

    private final GmailService gmailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting email processing job");
            gmailService.processNewEmails();
            log.info("Email processing job completed successfully");
        } catch (Exception e) {
            log.error("Error in email processing job: {}", e.getMessage(), e);
            throw new JobExecutionException("Email processing job failed", e);
        }
    }
}
