package com.zettix.scheduler;

import com.zettix.entity.Transaction;
import com.zettix.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionCleanupJob implements Job {

    private final TransactionRepository transactionRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting transaction cleanup job");
            
            // Find expired pending transactions
            List<Transaction> expiredTransactions = transactionRepository.findExpiredTransactions(LocalDateTime.now());
            
            for (Transaction transaction : expiredTransactions) {
                transaction.setStatus(Transaction.TransactionStatus.EXPIRED);
                transactionRepository.save(transaction);
                log.info("Marked transaction {} as expired", transaction.getId());
            }
            
            log.info("Transaction cleanup job completed. Processed {} expired transactions", expiredTransactions.size());
        } catch (Exception e) {
            log.error("Error in transaction cleanup job: {}", e.getMessage(), e);
            throw new JobExecutionException("Transaction cleanup job failed", e);
        }
    }
}
