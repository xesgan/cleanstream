package cat.dam.roig.cleanstream.services.prefs;

import cat.dam.roig.cleanstream.config.PreferencesData;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class responsible for validating {@link PreferencesData}.
 *
 * <p>
 * This validator centralizes all configuration validation logic to avoid
 * duplicating checks inside UI components.
 * </p>
 *
 * <p>
 * Validation strategy:
 * <ul>
 * <li>Checks for blank required fields</li>
 * <li>Checks logical constraints (e.g., speed > 0 if enabled)</li>
 * <li>Optionally verifies file existence</li>
 * </ul>
 *
 * <p>
 * This class returns a validation error message if invalid, or {@code null} if
 * the configuration is valid.
 *
 * <p>
 * Note: It performs lightweight validation only. It does not throw exceptions
 * and does not modify the data object.
 * </p>
 *
 * Designed to be called from UI layer before saving preferences.
 *
 * @author metku
 */
public class PreferencesValidator {

    /**
     * Validates the given PreferencesData instance.
     *
     * <p>
     * If any validation rule fails, a descriptive error message is returned. If
     * everything is valid, {@code null} is returned.
     *
     * @param d preferences data object to validate
     * @return error message if invalid; null if valid
     */
    public static String validate(PreferencesData d) {

        if (d.getDownloadDir() != null && d.getDownloadDir().isBlank()) {
            return "Download dir vacío.";
        }

        if (d.getYtDlpPath() != null && d.getYtDlpPath().isBlank()) {
            return "Ruta yt-dlp vacía.";
        }

        // Example of optional validation
        // if (d.getFfmpegPath() != null && d.getFfmpegPath().isBlank()) {
        //     return "Ruta ffmpeg vacía.";
        // }
        if (d.getScanFolderPath() != null && d.getScanFolderPath().isBlank()) {
            return "Ruta scan vacía.";
        }

        if (d.isLimitSpeedEnabled() && d.getSpeedKbps() <= 0) {
            return "La velocidad debe ser mayor que 0.";
        }

        // Optional filesystem validation
        if (d.getYtDlpPath() != null
                && !Files.exists(Path.of(d.getYtDlpPath()))) {
            return "yt-dlp no existe.";
        }

        return null; // Configuration is valid
    }
}
