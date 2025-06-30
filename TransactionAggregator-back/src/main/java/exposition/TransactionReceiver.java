package exposition;

import domains.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TransactionReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReceiver.class);
    private final TransactionProcessor transactionProcessor;

    @Autowired
    public TransactionReceiver(TransactionProcessor transactionProcessor) {
        this.transactionProcessor = transactionProcessor;
    }

    @KafkaListener(topics = "transactions", groupId = "reconciliation")
    public void consume(Transaction record) {
        LOG.debug("Transaction read on kafka queue: {}", record);
        transactionProcessor.processKafkaRecordAsync(record)
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    LOG.error("Error async timeout: {}", ex.toString());
                    return null;
                });
    }
}
