package cat.dam.roig.cleanstream.services;

import cat.dam.roig.cleanstream.models.ResourceDownloaded;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author metku
 */
public class DownloadsScanner {
    /** Extensiones temporales que conviene ignorar al listar descargas en curso. */
    private static final Set<String> TEMP_EXTS = Set.of(
            "part", "crdownload", "ytdl", "tmp"
    );

    /** Mapa de fallback para mimeType cuando probeContentType devuelve null. */
    private static final Map<String, String> MIME_FALLBACK = Map.ofEntries(
            Map.entry("mp4", "video/mp4"),
            Map.entry("mkv", "video/x-matroska"),
            Map.entry("webm", "video/webm"),
            Map.entry("avi", "video/x-msvideo"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("flac", "audio/flac"),
            Map.entry("aac", "audio/aac"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("opus", "audio/opus"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("zip", "application/zip"),
            Map.entry("rar", "application/vnd.rar"),
            Map.entry("7z", "application/x-7z-compressed"),
            Map.entry("txt", "text/plain"),
            Map.entry("srt", "application/x-subrip"),
            Map.entry("ass", "text/plain"),
            Map.entry("csv", "text/csv")
    );

    /**
     * Escanea una carpeta (no recursivo por defecto) y devuelve los recursos descargados.
     * @param dir Carpeta a recorrer.
     * @param recursive true para recorrer subdirectorios (cuidado con rendimiento).
     */
    public List<ResourceDownloaded> scan(Path dir, boolean recursive) throws IOException {
        if (dir == null) throw new IllegalArgumentException("dir is null");
        if (!Files.exists(dir)) return List.of();
        if (!Files.isDirectory(dir)) return List.of();

        final int maxDepth = recursive ? Integer.MAX_VALUE : 1;

        try (Stream<Path> stream = Files.walk(dir, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::notHiddenSafe)
                    .filter(this::notTempFile)
                    .map(this::toRecurso)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(ResourceDownloaded::getDownloadDate).reversed())
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    /** Conversión de Path -> RecursoDescargado con metadatos. */
    private ResourceDownloaded toRecurso(Path p) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            long size = attrs.size();

            // Preferimos creationTime si el FS lo soporta; si no, lastModifiedTime.
            LocalDateTime date = fileTimeToLdt(
                    attrs.creationTime() != null ? attrs.creationTime() : attrs.lastModifiedTime()
            );

            String fileName = p.getFileName().toString();
            String ext = getExtension(fileName);
            String mime = detectMime(p, ext);

            ResourceDownloaded r = new ResourceDownloaded();
            r.setName(fileName);
            r.setRoute(p.toAbsolutePath().toString());
            r.setSize(size);
            r.setMimeType(mime);
            r.setDownloadDate(date);
            r.setExtension(ext);
            return r;

        } catch (Exception e) {
            // Si algo falla con este archivo, lo omitimos (o loguea si tienes un Logger)
            return null;
        }
    }

    /** Intenta probeContentType y, si falla, usa fallback por extensión. */
    private String detectMime(Path p, String ext) {
        try {
            String probed = Files.probeContentType(p);
            if (probed != null && !probed.isBlank()) return probed;
        } catch (IOException ignored) { }
        // fallback por extensión (lowercase, sin punto)
        if (ext != null) {
            String lower = ext.toLowerCase(Locale.ROOT);
            String mime = MIME_FALLBACK.get(lower);
            if (mime != null) return mime;
            // fallback genérico
            if (isVideoExt(lower)) return "video/*";
            if (isAudioExt(lower)) return "audio/*";
            if (isImageExt(lower)) return "image/*";
            if ("pdf".equals(lower)) return "application/pdf";
            if ("txt".equals(lower) || "srt".equals(lower) || "ass".equals(lower)) return "text/plain";
        }
        return "application/octet-stream";
    }

    private boolean isVideoExt(String e) {
        return Set.of("mp4","mkv","webm","avi","mov").contains(e);
    }
    private boolean isAudioExt(String e) {
        return Set.of("mp3","m4a","wav","flac","aac","ogg","opus").contains(e);
    }
    private boolean isImageExt(String e) {
        return Set.of("jpg","jpeg","png","gif","webp").contains(e);
    }

    /** Devuelve false para ocultos o que no se puedan evaluar. */
    private boolean notHiddenSafe(Path p) {
        try {
            return !Files.isHidden(p);
        } catch (IOException e) {
            return true; // si no podemos saberlo, no lo filtramos
        }
    }

    /** Filtra extensiones temporales típicas (.part, .crdownload, .ytdl, .tmp). */
    private boolean notTempFile(Path p) {
        String ext = getExtension(p.getFileName().toString());
        if (ext == null) return true;
        return !TEMP_EXTS.contains(ext.toLowerCase(Locale.ROOT));
    }

    /** Devuelve extensión sin el punto, o null si no hay. */
    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i <= 0 || i == fileName.length() - 1) return null;
        return fileName.substring(i + 1);
    }

    private LocalDateTime fileTimeToLdt(FileTime ft) {
        return LocalDateTime.ofInstant(ft.toInstant(), ZoneId.systemDefault());
    }
}
