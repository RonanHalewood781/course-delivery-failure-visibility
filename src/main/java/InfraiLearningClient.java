import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/** A deliberately small REST client shared by queue and educator-report calls. */
public final class InfraiLearningClient {
    private final HttpClient http;
    private final String baseUrl;
    private final String apiKey;

    public InfraiLearningClient(ServiceSettings settings) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.baseUrl = settings.baseUrl();
        this.apiKey = settings.apiKey();
    }

    public String recentCourseRuns(String jobId) throws IOException, InterruptedException {
        return request("GET", "/v1/cron/runs/list/" + jobId, null, false);
    }

    public String stalledDeliveryLetters(String queue) throws IOException, InterruptedException {
        return request("GET", "/v1/queue/dlq/list/" + queue, null, false);
    }

    public String capturedTeachingErrors() throws IOException, InterruptedException {
        return request("GET", "/v1/errors/groups", null, false);
    }

    public String captureDeliveryException(String exceptionPayload)
            throws IOException, InterruptedException {
        return request("POST", "/v1/errors/capture", exceptionPayload, true);
    }

    private String request(String method, String path, String json, boolean write)
            throws IOException, InterruptedException {
        String idempotencyKey = write ? UUID.randomUUID().toString() : null;
        for (int attempt = 0; attempt < 3; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json");
            if (write) {
                builder.header("Content-Type", "application/json")
                        .header("Idempotency-Key", idempotencyKey);
            }
            HttpRequest request = builder.method(method,
                    json == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            Envelope envelope = Envelope.decode(response.body()); // Decode before judging HTTP status.
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(retryDelayMillis(response, attempt));
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiCallException(envelope.error());
            }
            if (response.statusCode() >= 400) {
                throw new IOException("Unexpected transport response: " + response.statusCode());
            }
            return envelope.data();
        }
        throw new IOException("Request did not complete after retrying");
    }

    private static long retryDelayMillis(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try {
            return Long.parseLong(retryAfter) * 1_000L;
        } catch (NumberFormatException ignored) {
            return 250L * (1L << attempt);
        }
    }

    private record Envelope(boolean ok, String data, String error) {
        static Envelope decode(String body) throws IOException {
            if (body == null || !body.contains("\"ok\"")) {
                throw new IOException("Response was not an Infrai envelope");
            }
            boolean ok = body.matches("(?s).*\\\"ok\\\"\\s*:\\s*true.*");
            String data = section(body, "data");
            String error = section(body, "error");
            return new Envelope(ok, data, error);
        }

        private static String section(String body, String name) {
            int start = body.indexOf("\"" + name + "\"");
            return start < 0 ? "" : body.substring(start);
        }
    }

    public static final class InfraiCallException extends IOException {
        public InfraiCallException(String error) {
            super(error.isBlank() ? "Infrai rejected the request" : error);
        }
    }
}
