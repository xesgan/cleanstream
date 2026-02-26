package cat.dam.roig.cleanstream.config;

/**
 * Centralized application configuration class.
 *
 * <p>
 * This class contains global configuration constants used across the
 * application. It is designed as a utility class:
 *
 * <ul>
 * <li>It is {@code final} to prevent inheritance.</li>
 * <li>It has a private constructor to avoid instantiation.</li>
 * <li>All members are {@code static}.</li>
 * </ul>
 *
 * <p>
 * Keeping configuration values centralized improves:
 * <ul>
 * <li>Maintainability (changes in one place).</li>
 * <li>Readability (clear separation between logic and configuration).</li>
 * <li>Scalability (future environment support: dev, test, production).</li>
 * </ul>
 *
 * <p>
 * <b>Note:</b> In a production-grade application, these values could be
 * externalized to a properties file or environment variables.
 *
 * @author metku
 */
public final class AppConfig {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>
     * This class is intended to be used statically.
     */
    private AppConfig() {
    }

    /**
     * Base URL of the DI Media Net REST API.
     *
     * <p>
     * This value must:
     * <ul>
     * <li>Include the protocol (http or https).</li>
     * <li>Not end with a trailing slash (unless required).</li>
     * </ul>
     *
     * <p>
     * Used by the polling component and other services that perform HTTP
     * requests.
     */
    public static final String API_BASE_URL
            = "https://dimedianetapi9.azurewebsites.net";
}
