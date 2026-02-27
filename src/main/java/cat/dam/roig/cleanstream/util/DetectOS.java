package cat.dam.roig.cleanstream.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class responsible for:
 * <ul>
 * <li>Detecting the current operating system</li>
 * <li>Resolving the system Downloads directory</li>
 * <li>Providing a safe default download folder for the application</li>
 * </ul>
 *
 * <p>
 * This class ensures that CleanStream always works with a valid, existing
 * download directory across Windows, macOS and Linux.
 * </p>
 *
 * <p>
 * Design goals:
 * <ul>
 * <li>Be OS-aware without external libraries (no JNA)</li>
 * <li>Respect Linux XDG standards when possible</li>
 * <li>Always return a valid directory (never null)</li>
 * </ul>
 * </p>
 *
 * <p>
 * All methods are static. This class cannot be instantiated.
 * </p>
 *
 * @author metku
 */
public class DetectOS {

    /**
     * Private constructor to prevent instantiation.
     */
    private DetectOS() {
    }

    /**
     * Supported operating systems.
     */
    enum OS {
        WINDOWS, MAC, LINUX, OTHER
    }

    /**
     * Detects the current operating system using {@code os.name}.
     *
     * @return detected {@link OS}
     */
    public static OS detectOS() {
        String os = System.getProperty("os.name", "generic").toLowerCase();

        if (os.contains("win")) {
            return OS.WINDOWS;
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return OS.MAC;
        }
        if (os.contains("nux") || os.contains("nix")) {
            return OS.LINUX;
        }
        return OS.OTHER;
    }

    /**
     * Returns the system Downloads directory depending on OS.
     *
     * <p>
     * Strategy:
     * <ul>
     * <li><b>Windows</b>: USERPROFILE\Downloads (fallback: user.home)</li>
     * <li><b>Linux</b>: tries {@code xdg-user-dir DOWNLOAD}</li>
     * <li><b>macOS</b>: ~/Downloads</li>
     * <li><b>Other</b>: ~/Downloads</li>
     * </ul>
     * </p>
     *
     * @return system downloads directory (may not yet exist)
     */
    private static Path getSystemDownloadsDir() {
        OS os = detectOS();
        String userHome = System.getProperty("user.home");

        try {
            switch (os) {
                case WINDOWS -> {
                    String userProfile = System.getenv("USERPROFILE");
                    if (userProfile == null || userProfile.isBlank()) {
                        userProfile = userHome;
                    }

                    Path downloads = Paths.get(userProfile).resolve("Downloads");
                    if (Files.isDirectory(downloads)) {
                        return downloads;
                    }

                    return Paths.get(userHome).resolve("Downloads");
                }

                case LINUX -> {
                    // Try XDG standard
                    Path xdg = runXdgUserDir("Download");
                    if (xdg != null && Files.isDirectory(xdg)) {
                        return xdg;
                    }

                    return Paths.get(userHome).resolve("Downloads");
                }

                case MAC, OTHER -> {
                    return Paths.get(userHome).resolve("Downloads");
                }
            }
        } catch (Exception e) {
            // Fallback in case of unexpected error
            return Paths.get(userHome).resolve("Downloads");
        }

        return Paths.get(userHome).resolve("Downloads");
    }

    /**
     * Executes {@code xdg-user-dir <NAME>} (Linux only).
     *
     * <p>
     * This respects user customizations and localization (e.g., "Descargas"
     * instead of "Downloads").
     * </p>
     *
     * @param name directory type (e.g. "Download")
     * @return resolved Path if valid, otherwise null
     */
    private static Path runXdgUserDir(String name) {
        try {
            Process proc = new ProcessBuilder("xdg-user-dir", name).start();

            try (BufferedReader br
                    = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {

                String line = br.readLine();
                int exit = proc.waitFor();

                if (exit == 0 && line != null && !line.isBlank()) {
                    Path p = Paths.get(line.trim());
                    if (Files.exists(p)) {
                        return p;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Ensures that a directory exists.
     *
     * <p>
     * If creation fails, it falls back to the working directory.
     * </p>
     *
     * @param dir directory to ensure
     * @return guaranteed existing directory
     */
    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (Exception e) {
            Path wd = Paths.get(System.getProperty("user.dir"));
            try {
                Files.createDirectories(wd);
            } catch (Exception ignored) {
            }
            return wd;
        }
    }

    /**
     * Default application download directory:
     *
     * <pre>
     *     System Downloads + /yt
     * </pre>
     *
     * Always ensured to exist.
     */
    private static final Path DEFAULT_DOWNLOAD_DIR_PATH
            = ensureDir(getSystemDownloadsDir().resolve("yt"));

    /**
     * Resolves the final download directory:
     * <ul>
     * <li>If candidate is null/blank → default directory</li>
     * <li>If provided → ensures it exists</li>
     * </ul>
     *
     * @param candidate user preference path (may be null)
     * @return valid directory path as String
     */
    public static String resolveDownloadDir(String candidate) {
        Path resolved;

        if (candidate == null || candidate.isBlank()) {
            resolved = DEFAULT_DOWNLOAD_DIR_PATH;
        } else {
            resolved = ensureDir(Paths.get(candidate.trim()));
        }

        return resolved.toString();
    }
}
