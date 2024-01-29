import java.util.concurrent.*;
import java.util.List;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {
    private final Semaphore rateLimiter;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.rateLimiter = new Semaphore(requestLimit);
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            rateLimiter.release(requestLimit - rateLimiter.availablePermits());
        }, 0, 1, timeUnit);
    }

    public synchronized String createDocument(Document document, String signature) throws Exception {
        if (!rateLimiter.tryAcquire()) {
            throw new IllegalStateException("Request limit exceeded");
        }

        try {
            HttpPost post = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
            String json = objectMapper.writeValueAsString(document);
            StringEntity entity = new StringEntity(json);
            post.setEntity(entity);
            post.setHeader("Content-type", "application/json");
            post.setHeader("Signature", signature);

            return EntityUtils.toString(httpClient.execute(post).getEntity());
        } finally {
            rateLimiter.release();
        }
    }

    private static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products;
        private String regDate;
        private String regNumber;

        // getters, setters and constructors

        private static class Description {
            private String participantInn;

            // getters, setters and constructors
        }

        private static class Product {
            private String certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private String productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

            // getters, setters and constructors
        }
    }


    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);
        Document document = new Document();
        api.createDocument(document, "signature");
    }
}
