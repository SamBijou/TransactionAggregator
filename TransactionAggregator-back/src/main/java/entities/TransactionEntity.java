package entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table
public class TransactionEntity implements Serializable {
    @Id
    @Column(nullable = false)
    private String primaryId;
    @Column
    private String secondaryId;
    @Column
    private String eventType;
    @Column
    private String date;
}
