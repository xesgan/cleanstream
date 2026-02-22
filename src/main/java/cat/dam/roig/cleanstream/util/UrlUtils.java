package cat.dam.roig.cleanstream.util;

public final class UrlUtils {

    private UrlUtils() {}

    public static String normalizeBaseUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return s;

        // Si ya tiene esquema, OK
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return stripTrailingSlash(s);
        }

        // Si parece host:puerto o dominio, le ponemos http:// por defecto
        return stripTrailingSlash("http://" + s);
    }

    private static String stripTrailingSlash(String s) {
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}