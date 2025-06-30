package entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table
public class TransactionChainEntity implements Serializable {
    @Id
    @Column(nullable = false)
    private String firstId;
    @Column(nullable = false)
    @OneToMany(targetEntity = TransactionEntity.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransactionEntity> transactions;
}
