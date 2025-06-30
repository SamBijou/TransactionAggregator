package services;

import entities.TransactionChainEntity;
import infrastructure.TransactionChainRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionServices {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionServices.class);
    private final TransactionChainRepository transactionChainRepository;

    @Autowired
    public TransactionServices(TransactionChainRepository transactionChainRepository) {
        this.transactionChainRepository = transactionChainRepository;
    }

    //@Transactional // <= pas utile
    public TransactionChainEntity saveChain(TransactionChainEntity transactionChainEntity) {
        LOG.info("Save transaction chain entity: {}", transactionChainEntity);
        return transactionChainRepository.save(transactionChainEntity);
    }

    public List<TransactionChainEntity> getAllTransactionChainEntities() {
        LOG.info("Find all transaction chain entities");
        return transactionChainRepository.findAll();
    }

    public TransactionChainEntity getTransactionChainEntity(String id) {
        LOG.info("Find transaction chain entity with id: {}", id);
        return transactionChainRepository.findTransactionChainEntityByFirstId(id);
    }
}
