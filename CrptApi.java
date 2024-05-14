import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class CrptApi {
    private TimeUnit timeUnit;
    private int requestLimit;

    private Semaphore semaphore;
    private ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit, true);
        this.scheduler = Executors.newScheduledThreadPool(1);

        long delay = timeUnit.toMillis(1);

        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, delay, delay, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) {

        String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        try {
            semaphore.acquire();
            try (CloseableHttpClient client = HttpClients.createDefault()) {

                ObjectMapper objectMapper = new ObjectMapper();
                String jsonDocument = objectMapper.writeValueAsString(document);

                var entityBuilder = MultipartEntityBuilder.create()
                        .addTextBody("document", jsonDocument, ContentType.APPLICATION_JSON)
                        .addTextBody("signature", signature, ContentType.TEXT_PLAIN);

                HttpPost request = new HttpPost(URI.create(url));
                request.setEntity(entityBuilder.build());

                client.execute(request);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class Document {
    private Description description;

    @JsonProperty("doc_id")
    private String docId;

    @JsonProperty("doc_status")
    private String docStatus;

    @JsonProperty("doc_type")
    private Object[] docType;

    @JsonProperty("importRequest")
    private boolean importRequest;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("participant_inn")
    private String participantInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private LocalDate productionDate;

    @JsonProperty("production_type")
    private String productionType;

    @JsonProperty("products")
    private List<Product> products;

    @JsonProperty("reg_date")
    private LocalDate regDate;

    @JsonProperty("reg_number")
    private String regNumber;
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class Product {
    @JsonProperty("certificate_document")
    private String certificateDocument;

    @JsonProperty("certificate_document_date")
    private LocalDate certificateDocumentDate;

    @JsonProperty("certificate_document_number")
    private String certificateDocumentNumber;

    @JsonProperty("owner_inn")
    private String ownerInn;

    @JsonProperty("producer_inn")
    private String producerInn;

    @JsonProperty("production_date")
    private LocalDate productionDate;

    @JsonProperty("tnved_code")
    private String tnvedCode;

    @JsonProperty("uit_code")
    private String uitCode;

    @JsonProperty("uitu_code")
    private String uituCode;
}

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class Description {
    private String participantInn;
}
