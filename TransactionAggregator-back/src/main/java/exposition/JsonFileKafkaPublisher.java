package exposition;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import domains.Transaction;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class JsonFileKafkaPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(JsonFileKafkaPublisher.class);
    private final KafkaTemplate<String, Transaction> kafkaTemplate;
    @Value("${app.transaction.kafka-topic:transactions}")
    private String kafkaTopic;
    @Value("${app.transaction.json-file-path}")
    private String jsonFilePath;

    public JsonFileKafkaPublisher(KafkaTemplate<String, Transaction> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    public void publishTransactionsFromFile() {
        LOG.info("Reading transactions from JSON file");
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream input = getClass().getClassLoader().getResourceAsStream(jsonFilePath);
            if (input != null) {

                JsonFactory factory = mapper.getFactory();
                try (JsonParser parser = factory.createParser(input)) {

                    if (parser.nextToken() != JsonToken.START_ARRAY) {
                        throw new RuntimeException("JSON file not start with an array !");
                    }

                    while (parser.nextToken() == JsonToken.START_OBJECT) {
                        Transaction transaction = mapper.readValue(parser, Transaction.class);
                        kafkaTemplate.send(kafkaTopic, transaction);
                        LOG.debug("Transaction send on Kakfa queue: {}", transaction);
                    }
                }
            } else {
                LOG.warn("JSON file not found at: {}", jsonFilePath);
            }
        } catch (IOException e) {
            LOG.error("JSON file reading error: {}", e.getMessage());
        }
    }
}
