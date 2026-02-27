package cat.dam.roig.cleanstream.util;

/**
 * Utility class for normalizing and sanitizing base URLs.
 *
 * <p>
 * This helper ensures that:
 * <ul>
 * <li>The URL always has a scheme (http:// or https://)</li>
 * <li>Trailing slashes are removed</li>
 * <li>Null values are handled safely</li>
 * </ul>
 * </p>
 *
 * <p>
 * Typical use case:
 * <pre>
 *     String base = UrlUtils.normalizeBaseUrl(userInput);
 *     String full = base + "/api/media";
 * </pre>
 * </p>
 *
 * <p>
 * This prevents subtle bugs such as:
 * <ul>
 * <li>Double slashes (e.g. http://host//api)</li>
 * <li>Missing protocol (e.g. localhost:8080)</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class cannot be instantiated.
 * </p>
 *
 * @author metku
 */
public final class UrlUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private UrlUtils() {
    }

    /**
     * Normalizes a raw base URL string.
     *
     * <p>
     * Rules:
     * <ul>
     * <li>If {@code null} → returns null</li>
     * <li>If blank → returns blank</li>
     * <li>If no scheme → prefixes with {@code http://}</li>
     * <li>Removes trailing slashes</li>
     * </ul>
     * </p>
     *
     * <p>
     * Examples:
     * <pre>
     *     "localhost:8080"     → "http://localhost:8080"
     *     "http://host/"       → "http://host"
     *     "https://api.com//"  → "https://api.com"
     * </pre>
     * </p>
     *
     * @param raw raw user-provided base URL
     * @return normalized URL string
     */
    public static String normalizeBaseUrl(String raw) {
        if (raw == null) {
            return null;
        }

        String s = raw.trim();

        if (s.isEmpty()) {
            return s;
        }

        // If already has a scheme, keep it
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return stripTrailingSlash(s);
        }

        // Otherwise assume host:port or domain and prefix http://
        return stripTrailingSlash("http://" + s);
    }

    /**
     * Removes all trailing slashes from a string.
     *
     * <p>
     * Example:
     * <pre>
     *     "http://host///" → "http://host"
     * </pre>
     * </p>
     *
     * @param s input string
     * @return string without trailing '/'
     */
    private static String stripTrailingSlash(String s) {
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
