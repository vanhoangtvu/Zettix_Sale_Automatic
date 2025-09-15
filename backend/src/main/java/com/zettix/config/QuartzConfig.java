package com.zettix.config;

import com.zettix.scheduler.EmailProcessingJob;
import com.zettix.scheduler.TransactionCleanupJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail emailProcessingJobDetail() {
        return JobBuilder.newJob(EmailProcessingJob.class)
                .withIdentity("emailProcessingJob")
                .withDescription("Process new emails for transaction confirmation")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger emailProcessingTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(emailProcessingJobDetail())
                .withIdentity("emailProcessingTrigger")
                .withDescription("Trigger for email processing job")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(2) // Check every 2 minutes
                        .repeatForever())
                .build();
    }

    @Bean
    public JobDetail transactionCleanupJobDetail() {
        return JobBuilder.newJob(TransactionCleanupJob.class)
                .withIdentity("transactionCleanupJob")
                .withDescription("Clean up expired transactions")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger transactionCleanupTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(transactionCleanupJobDetail())
                .withIdentity("transactionCleanupTrigger")
                .withDescription("Trigger for transaction cleanup job")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(5) // Check every 5 minutes
                        .repeatForever())
                .build();
    }
}
