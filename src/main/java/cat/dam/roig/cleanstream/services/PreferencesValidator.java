package cat.dam.roig.cleanstream.services;

import cat.dam.roig.cleanstream.models.PreferencesData;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author metku
 */
public class PreferencesValidator {

    public static String validate(PreferencesData d) {
        if (d.getDownloadDir() != null && d.getDownloadDir().isBlank()) {
            return "Download dir vacío.";
        }
        if (d.getYtDlpPath() != null && d.getYtDlpPath().isBlank()) {
            return "Ruta yt-dlp vacía.";
        }
//        if (d.getFfmpegPath() != null && d.getFfmpegPath().isBlank()) {
//            return "Ruta ffmpeg vacía.";
//        }
        if (d.getScanFolderPath() != null && d.getScanFolderPath().isBlank()) {
            return "Ruta scan vacía.";
        }
        if (d.isLimitSpeedEnabled() && d.getSpeedKbps() <= 0) {
            return "La velocidad debe ser mayor que 0.";
        }

        // (Opcional) validar que existen como archivo/carpeta
        if (d.getYtDlpPath() != null && !Files.exists(Path.of(d.getYtDlpPath()))) {
            return "yt-dlp no existe.";
        }
        return null; // OK
    }
}
