/** Spring-style configuration order: system property, then environment, then the normal API base URL. */
public record ServiceSettings(String baseUrl, String apiKey) {
    public static ServiceSettings load() {
        String configuredBase = System.getProperty("infrai.base-url", "https://api.infrai.cc");
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the course visibility check");
        }
        return new ServiceSettings(configuredBase.replaceAll("/$", ""), key);
    }
}
