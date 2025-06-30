package domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Transaction {
    private String primary_id;
    private String secondary_id;
    private Event event;
    private String date;
}
