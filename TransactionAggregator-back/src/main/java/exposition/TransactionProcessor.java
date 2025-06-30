package exposition;

import domains.Event;
import domains.Transaction;
import entities.TransactionChainEntity;
import entities.TransactionEntity;
import jakarta.annotation.PostConstruct;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import services.TransactionServices;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class TransactionProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionProcessor.class);
    private static final List<String> STEP_RANKS = List.of("AcquisitionCB", "Acquisition");
    private static final List<String> STEP_1_EVENT_RANKS = List.of("CBAcquired", "CBPGPAcquired", "CBAcquiredForBatch", "CBAcquiredToPfmt101");
    private static final List<String> STEP_2_EVENT_RANKS = List.of("Reception", "Duplicate", "DuplicateVerification", "Duplicate4EyesVerification", "100%VirementHold", "100%VirementVerify", "OnHold", "Repair");
    private static final List<List<String>> EVENT_RANKS_BY_STEPS = List.of(STEP_1_EVENT_RANKS, STEP_2_EVENT_RANKS);
    private static final int TOTAL_NUMBER_OF_EVENT_STEPS = EVENT_RANKS_BY_STEPS.get(0).size() + EVENT_RANKS_BY_STEPS.get(1).size() * 2;
    private static final String FIRST_STEP = EVENT_RANKS_BY_STEPS.get(0).get(0);
    private static final Map<String, TransactionNode> TRANSACTIONS_FIRST_ELEMENTS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, TransactionNode> TRANSACTIONS_OTHER_ELEMENTS_MAP = new ConcurrentHashMap<>();
    private static final Map<String, List<TransactionEntity>> TRANSACTIONS_CHAINS_TO_SAVE_MAP = new ConcurrentHashMap<>();
    private final TransactionServices transactionServices;
    // private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired
    public TransactionProcessor(TransactionServices transactionServices) {
        this.transactionServices = transactionServices;
    }

    public static String getStep(String event) {
        if (EVENT_RANKS_BY_STEPS.get(0).contains(event)) {
            return STEP_RANKS.get(0);
        } else {
            return STEP_RANKS.get(1);
        }
    }

    public static void reconcile() {
        // On parcourt la map des transactions qui ne sont pas les premiers éléments de la chaîne
        Map<String, TransactionNode> transactionsOtherElementsMapCopy = new ConcurrentHashMap<>(TRANSACTIONS_OTHER_ELEMENTS_MAP);
        transactionsOtherElementsMapCopy
                // Pas utile avec la suppression de l'élément dans la map après reconciliation
                /*
                .entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isReconciled())
                */
                .forEach((key, node) -> {
                    // On cherche le parent
                    if (node.getTransaction() != null) {
                        String parentId = node.getTransaction().getSecondary_id();
                        if (parentId != null) {
                            TransactionNode parent = TRANSACTIONS_OTHER_ELEMENTS_MAP.get(parentId);
                            if (parent != null) {
                                parent.setChildren(node);
                                node.setReconciled(true);
                                // Suppression du parent dans la map des éléments de chaînes s'il est déjà réconcilié
                                if (parent.isReconciled()) {
                                    TRANSACTIONS_OTHER_ELEMENTS_MAP.remove(parentId);
                                }
                            }
                        }
                    }
                });

        // On parcourt la map des transactions qui sont les premiers éléments de la chaîne afin de la sauvegarder
        Map<String, TransactionNode> transactionsFirstElementsMapCopy = new ConcurrentHashMap<>(TRANSACTIONS_FIRST_ELEMENTS_MAP);
        transactionsFirstElementsMapCopy.values()
                .forEach(root -> {
                    List<Transaction> transactionsList = new ArrayList<>();
                    root.flatten(transactionsList);
                    List<TransactionEntity> transactionsEntitiesList = mapToEntities(transactionsList);
                    try {
                        int numberOfSteps = transactionsEntitiesList.size();
                        if (numberOfSteps == TOTAL_NUMBER_OF_EVENT_STEPS) {
                            if (root.getTransaction() != null) {
                                // Ajout de la chaîne de transactions dans la map des chaînes à sauvegarder
                                TRANSACTIONS_CHAINS_TO_SAVE_MAP.put(root.getTransaction().getPrimary_id(), transactionsEntitiesList);
                                for (TransactionEntity transactionEntity : transactionsEntitiesList) {
                                    String transactionId = transactionEntity.getPrimaryId();
                                    TRANSACTIONS_FIRST_ELEMENTS_MAP.remove(transactionId);
                                    TRANSACTIONS_OTHER_ELEMENTS_MAP.remove(transactionId);
                                }
                            }
                        } else if (numberOfSteps > TOTAL_NUMBER_OF_EVENT_STEPS) {
                            throw new Exception("ERROR: incorrect number of event steps");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public static List<TransactionEntity> mapToEntities(List<Transaction> transactions) {
        return transactions.stream().map(t -> {
            Event event = t.getEvent();
            TransactionEntity entity = new TransactionEntity();
            entity.setPrimaryId(t.getPrimary_id());
            entity.setSecondaryId(t.getSecondary_id());
            if (event != null) {
                entity.setEventType(event.getEventType());
            }
            entity.setDate(t.getDate());
            return entity;
        }).collect(Collectors.toList());
    }

    @Async
    public CompletableFuture<Void> processKafkaRecordAsync(Transaction transaction) {
        if (transaction != null) {
            try {
                LOG.debug("Adding transaction");
                addTransaction(transaction);
            } catch (Exception e) {
                LOG.error("Error with async treatment: {}", e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    // Ancien traitement asynchrone (autre façon)
    /*
    public void processKafkaRecord(Transaction transaction) {
        if (transaction != null) {
            CompletableFuture.runAsync(() -> {
                        try {
                            log.debug("Adding transaction");
                            addTransaction(transaction);
                        } catch (Exception e) {
                            log.error("Error with async treatment: {}", e.getMessage());
                        }
                    })
                    .orTimeout(10, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        log.error("Error async timeout: {}", ex.toString());
                        return null;
                    });
        }
    }
     */

    public void addTransaction(Transaction transaction) {
        if (transaction.getPrimary_id() != null) {
            String transactionId = transaction.getPrimary_id();
            Event event = transaction.getEvent();
            if (event != null) {
                TransactionNode newTransactionNode = new TransactionNode(transaction);
                // Si la transaction n'est pas un premier élément de chaîne, on essaye déjà de la réconcilier avec ce qu'on a.
                if (!FIRST_STEP.equals(event.getEventType())) {
                    String parentId = transaction.getSecondary_id();
                    if (parentId != null) {
                        // On cherche parmi les premiers éléments de chaînes
                        TransactionNode parent = TRANSACTIONS_FIRST_ELEMENTS_MAP.get(parentId);
                        if (parent != null) {
                            parent.setChildren(newTransactionNode);
                            newTransactionNode.setReconciled(true);
                        } else {
                            // On cherche parmi les autres éléments de chaînes
                            parent = TRANSACTIONS_OTHER_ELEMENTS_MAP.get(parentId);
                            if (parent != null) {
                                parent.setChildren(newTransactionNode);
                                newTransactionNode.setReconciled(true);
                                // Suppression du parent dans la map des éléments de chaînes s'il est déjà réconcilié
                                if (parent.isReconciled()) {
                                    TRANSACTIONS_OTHER_ELEMENTS_MAP.remove(parentId);
                                }
                            }
                        }

                        // On ajoute à la map des autres éléments
                        TRANSACTIONS_OTHER_ELEMENTS_MAP.put(transactionId, newTransactionNode);
                    }
                } else {
                    TRANSACTIONS_FIRST_ELEMENTS_MAP.put(transactionId, newTransactionNode);
                    newTransactionNode.setReconciled(true);
                }
            } else {
                LOG.warn("Transaction event is null");
            }
        }
    }

    @PostConstruct
    public void startReconciliationTask() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!TRANSACTIONS_FIRST_ELEMENTS_MAP.isEmpty()) {
                    LOG.info("Reconciliation processing");
                    reconcile();
                }
            }
        }, 1000, 1000); // toutes les secondes après un délai initial
    }

    @PostConstruct
    public void saveTransactionChainsTask() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!TRANSACTIONS_CHAINS_TO_SAVE_MAP.isEmpty()) {
                    Map<String, List<TransactionEntity>> transactionsChainsToSaveMapCopy = new HashMap<>(TRANSACTIONS_CHAINS_TO_SAVE_MAP);

                    transactionsChainsToSaveMapCopy.forEach((key, value) -> {
                        try {
                            TransactionChainEntity transactionChainEntity = new TransactionChainEntity();
                            transactionChainEntity.setFirstId(key);
                            transactionChainEntity.setTransactions(value);
                            TransactionChainEntity result = transactionServices.saveChain(transactionChainEntity);

                            if (result != null) {
                                // Suppression de la chaîne de transactions sauvegardée dans la map des chaînes de transactions à sauvegarder
                                TRANSACTIONS_CHAINS_TO_SAVE_MAP.remove(key);
                                LOG.info("Transaction chain saved: {}", result);
                            } else {
                                throw new Exception("ERROR: transaction chain save failed");
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }, 1000, 1000); // toutes les secondes après un délai initial
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    static class TransactionNode {
        private Transaction transaction;
        private TransactionNode children;
        private boolean reconciled = false;

        TransactionNode(Transaction transaction) {
            this.transaction = transaction;
        }

        void flatten(List<Transaction> accumulator) {
            accumulator.add(transaction);
            if (children != null) {
                children.flatten(accumulator);
            }
        }
    }
}
