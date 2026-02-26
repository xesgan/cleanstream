package cat.dam.roig.cleanstream.services.scan;

import cat.dam.roig.cleanstream.domain.ResourceDownloaded;
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
 * Scans a downloads directory and builds {@link ResourceDownloaded} objects
 * from files found.
 *
 * <p>
 * This service is used to:
 * <ul>
 * <li>List local downloaded media files stored in the user's downloads
 * folder</li>
 * <li>Extract basic metadata (name, absolute path, size, date, extension, MIME
 * type)</li>
 * <li>Ignore temporary/incomplete downloads (e.g., yt-dlp .part files)</li>
 * <li>Optionally scan subdirectories (recursive mode)</li>
 * </ul>
 *
 * <p>
 * Output ordering: Returned resources are sorted by download date descending
 * (most recent first).
 *
 * <p>
 * MIME type detection strategy:
 * <ol>
 * <li>Try {@link Files#probeContentType(Path)}</li>
 * <li>If it returns null/blank or throws, use a fallback map by extension</li>
 * <li>If extension is unknown, return a generic type (video/*, audio/*,
 * image/*) or {@code application/octet-stream}</li>
 * </ol>
 *
 * <p>
 * Performance note: Recursive scanning can be expensive on large directory
 * trees. Use with care.
 *
 * @author metku
 */
public class DownloadsScanner {

    /**
     * Temporary extensions usually created during ongoing downloads. These
     * files are ignored to avoid listing incomplete resources.
     */
    private static final Set<String> TEMP_EXTS = Set.of(
            "part", "crdownload", "ytdl", "tmp"
    );

    /**
     * Fallback map for MIME type detection when
     * {@link Files#probeContentType(Path)} returns null. Keys are file
     * extensions without the dot, in lowercase.
     */
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
     * Scans a directory and returns a list of {@link ResourceDownloaded} found
     * in that folder.
     *
     * <p>
     * Default behavior is non-recursive scanning. If {@code recursive} is true,
     * subdirectories are included (which may impact performance).
     *
     * <p>
     * Hidden files and temporary download files are filtered out.
     *
     * @param dir folder to scan (must be an existing directory)
     * @param recursive true to scan subfolders; false to scan only the root
     * folder
     * @return an immutable list of resources sorted by date (desc). Returns an
     * empty list if dir does not exist or is not a directory.
     * @throws IOException if file traversal fails (e.g., permission issues)
     * @throws IllegalArgumentException if {@code dir} is null
     */
    public List<ResourceDownloaded> scan(Path dir, boolean recursive) throws IOException {
        if (dir == null) {
            throw new IllegalArgumentException("dir is null");
        }
        if (!Files.exists(dir)) {
            return List.of();
        }
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        final int maxDepth = recursive ? Integer.MAX_VALUE : 1;

        try (Stream<Path> stream = Files.walk(dir, maxDepth, FileVisitOption.FOLLOW_LINKS)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::notHiddenSafe)
                    .filter(this::notTempFile)
                    .map(this::toResource)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(ResourceDownloaded::getDownloadDate).reversed())
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    /**
     * Converts a file path into a {@link ResourceDownloaded} by reading basic
     * file metadata.
     *
     * <p>
     * Extracted fields:
     * <ul>
     * <li>name: file name</li>
     * <li>route: absolute path</li>
     * <li>size: in bytes</li>
     * <li>downloadDate: creation time if supported, otherwise last modified
     * time</li>
     * <li>extension: extension without dot</li>
     * <li>mimeType: detected using {@link #detectMime(Path, String)}</li>
     * </ul>
     *
     * <p>
     * If metadata cannot be read, returns null (caller filters it out).
     *
     * @param p file path
     * @return a populated ResourceDownloaded or null if an error occurs
     */
    private ResourceDownloaded toResource(Path p) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(
                    p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS
            );

            long size = attrs.size();

            // Prefer creationTime if supported; fall back to lastModifiedTime.
            FileTime ft = (attrs.creationTime() != null)
                    ? attrs.creationTime()
                    : attrs.lastModifiedTime();

            LocalDateTime date = fileTimeToLdt(ft);

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
            return null;
        }
    }

    /**
     * Detects a file MIME type.
     *
     * <p>
     * Strategy:
     * <ol>
     * <li>Try {@link Files#probeContentType(Path)}</li>
     * <li>If missing/blank, try {@link #MIME_FALLBACK} by extension</li>
     * <li>If still unknown, return a generic type or
     * {@code application/octet-stream}</li>
     * </ol>
     *
     * @param p file path
     * @param ext extension without dot (may be null)
     * @return a MIME type string (never null)
     */
    private String detectMime(Path p, String ext) {
        try {
            String probed = Files.probeContentType(p);
            if (probed != null && !probed.isBlank()) {
                return probed;
            }
        } catch (IOException ignored) {
        }

        // Extension-based fallback (lowercase, without dot)
        if (ext != null) {
            String lower = ext.toLowerCase(Locale.ROOT);

            String mime = MIME_FALLBACK.get(lower);
            if (mime != null) {
                return mime;
            }

            // Generic fallback based on family
            if (isVideoExt(lower)) {
                return "video/*";
            }
            if (isAudioExt(lower)) {
                return "audio/*";
            }
            if (isImageExt(lower)) {
                return "image/*";
            }
            if ("pdf".equals(lower)) {
                return "application/pdf";
            }
            if ("txt".equals(lower) || "srt".equals(lower) || "ass".equals(lower)) {
                return "text/plain";
            }
        }

        return "application/octet-stream";
    }

    /**
     * @param e extension in lowercase
     * @return true if extension is a known video extension
     */
    private boolean isVideoExt(String e) {
        return Set.of("mp4", "mkv", "webm", "avi", "mov").contains(e);
    }

    /**
     * @param e extension in lowercase
     * @return true if extension is a known audio extension
     */
    private boolean isAudioExt(String e) {
        return Set.of("mp3", "m4a", "wav", "flac", "aac", "ogg", "opus").contains(e);
    }

    /**
     * @param e extension in lowercase
     * @return true if extension is a known image extension
     */
    private boolean isImageExt(String e) {
        return Set.of("jpg", "jpeg", "png", "gif", "webp").contains(e);
    }

    /**
     * Safe hidden-file check.
     *
     * <p>
     * If the hidden status cannot be evaluated (IOException), the file is not
     * filtered out (returns true).
     *
     * @param p path to check
     * @return true if file should be kept; false if hidden
     */
    private boolean notHiddenSafe(Path p) {
        try {
            return !Files.isHidden(p);
        } catch (IOException e) {
            return true; // if unknown, don't filter it out
        }
    }

    /**
     * Filters typical temporary download files (.part, .crdownload, .ytdl,
     * .tmp).
     *
     * @param p file path
     * @return true if file should be kept; false if it is a temp/incomplete
     * download file
     */
    private boolean notTempFile(Path p) {
        String ext = getExtension(p.getFileName().toString());
        if (ext == null) {
            return true;
        }
        return !TEMP_EXTS.contains(ext.toLowerCase(Locale.ROOT));
    }

    /**
     * Extracts the extension from a file name.
     *
     * @param fileName file name (not path)
     * @return extension without dot, or null if none exists
     */
    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i <= 0 || i == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(i + 1);
    }

    /**
     * Converts {@link FileTime} into {@link LocalDateTime} using system default
     * zone.
     *
     * @param ft file time
     * @return local date time
     */
    private LocalDateTime fileTimeToLdt(FileTime ft) {
        return LocalDateTime.ofInstant(ft.toInstant(), ZoneId.systemDefault());
    }
}
