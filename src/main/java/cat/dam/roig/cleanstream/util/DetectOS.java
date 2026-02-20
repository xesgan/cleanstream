package cat.dam.roig.cleanstream.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author metku
 */
public class DetectOS {
    
    private DetectOS() {};
    String dir;

    enum OS {
        WINDOWS, MAC, LINUX, OTHER
    }

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
     * Devuelve la carpeta de descargas del sistema (sin la subcarpeta
     */
    private static Path getSystemDownloadsDir() {
        OS os = detectOS();
        String userHome = System.getProperty("user.home");

        try {
            switch (os) {
                case WINDOWS: {
                    // Fallback robusto sin JNA: USERPROFILE\Downloads
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
                case LINUX: {
                    // Intenta xdg-user-dir DOWNLOAD (respeta la localizacion y reubicaciones)
                    Path xdg = runXdgUserDir("Download");
                    if (xdg != null && Files.isDirectory(xdg)) {
                        return xdg;
                    }
                    // Fallback tipico
                    return Paths.get(userHome).resolve("Downloads");
                }
                case MAC: {
                    // En macOS suele ser ~/Downloads
                    return Paths.get(userHome).resolve("Downloads");
                }
                default: {
                    return Paths.get(userHome).resolve("Downloads");
                }
            }
        } catch (Exception e) {
            // Si algo raro pasa vuelve al home
            return Paths.get(userHome).resolve("Downloads");
        }
    }

    /**
     * Ejecuta 'xdg-user-dir <NAME>' y devuelve la ruta si existe. Solo Linux
     */
    private static Path runXdgUserDir(String name) {
        try {
            Process proc = new ProcessBuilder("xdg-user-dir", name).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
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
     * Crea la carpepta si no existe. Devuelve siempre una ruta valida
     */
    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (Exception e) {
            // Último recurso: directorio de trabajo
            Path wd = Paths.get(System.getProperty("user.dir"));
            try {
                Files.createDirectories(wd);
            } catch (Exception ignored) {
            }
            return wd;
        }
    }

    /**
     * Tu default real: Descargas del sistema + subcarpeta 'yt'.
     */
    private static final Path DEFAULT_DOWNLOAD_DIR_PATH
            = ensureDir(getSystemDownloadsDir().resolve("yt"));
    private static final String DEFAULT_DOWNLOAD_DIR = DEFAULT_DOWNLOAD_DIR_PATH.toString();

    /**
     * Resuelve la ruta final (preferencias → default) y la crea si no existe.
     * @param candidate
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
