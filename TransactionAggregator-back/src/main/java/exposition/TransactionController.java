package exposition;

import entities.TransactionChainEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import services.TransactionServices;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionServices transactionServices;
    private final JsonFileKafkaPublisher jsonFileKafkaPublisher;
    /*
    @Value("${app.transaction.json-file-path}")
    private String jsonFilePath;
     */

    @Autowired
    public TransactionController(TransactionServices transactionServices, JsonFileKafkaPublisher jsonFileKafkaPublisher) {
        this.transactionServices = transactionServices;
        this.jsonFileKafkaPublisher = jsonFileKafkaPublisher;
    }

    @RequestMapping(value = "/reconciled", method = RequestMethod.GET)
    public ResponseEntity<List<TransactionChainEntity>> getReconciledTransactions() {
        LOG.info("Get reconciled transactions");
        return new ResponseEntity<>(transactionServices.getAllTransactionChainEntities(), HttpStatus.OK);
    }

    @RequestMapping(value = "/reconciled/{id}", method = RequestMethod.GET)
    public ResponseEntity<TransactionChainEntity> getReconciledTransaction(@PathVariable("id") String id) {
        LOG.info("Get all reconciled transactions with id: {}", id);
        return new ResponseEntity<>(transactionServices.getTransactionChainEntity(id), HttpStatus.OK);
    }

    /*
    @RequestMapping(value = "/runReconciliation", method = RequestMethod.POST)
    public ResponseEntity<Void> runReconciliation() {
        LOG.info("Run JSON file treatment");
        jsonFileKafkaPublisher.publishTransactionsFromFile();
        return new ResponseEntity<>(HttpStatus.OK);
    }
     */

    // Uploader un fichier JSON
    /*
    @PostMapping("/upload")
    public ResponseEntity<String> uploadJsonFile(@RequestParam("file") MultipartFile file) {
        if (file != null) {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Le fichier est vide.");
            }

            try {
                String fileName = file.getOriginalFilename();
                if (fileName != null) {
                    // On s'assure que l'extension est bien .json
                    String fileNameCleaned = StringUtils.cleanPath(fileName);
                    if (!fileNameCleaned.endsWith(".json")) {
                        return ResponseEntity.badRequest().body("Seuls les fichiers .json sont acceptés.");
                    }

                    // Écrit le fichier à l'emplacement configuré
                    Path destination = Path.of(jsonFilePath);
                    //Files.createDirectories(destination.getParent()); // Au cas où
                    Files.write(destination, file.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                    log.info("Fichier JSON uploadé avec succès : {}", fileNameCleaned);
                    return ResponseEntity.ok("Fichier JSON uploadé avec succès.");
                }
            } catch (IOException e) {
                log.error("Error on file uploading: {}", e.getMessage());
                return ResponseEntity.internalServerError().body("Error while file saving.");
            }
        }
    }
     */
}
