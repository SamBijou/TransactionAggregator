package infrastructure;

import entities.TransactionChainEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionChainRepository extends JpaRepository<TransactionChainEntity, String> {
    TransactionChainEntity findTransactionChainEntityByFirstId(@Nonnull String id);
}
