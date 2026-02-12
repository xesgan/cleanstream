package cat.dam.roig.cleanstream.services;

import java.util.prefs.Preferences;
import cat.dam.roig.cleanstream.models.PreferencesData;

/**
 * Gestiona las rutas y ajustes persistentes de la aplicación.
 */
public class UserPreferences {

    // Nodo de preferencias para toda la app
    private static final Preferences PREFS
            = Preferences.userNodeForPackage(UserPreferences.class);

    // Claves
    private static final String KEY_DOWNLOAD_DIR = "downloadDir";
    private static final String KEY_YTDLP_PATH = "ytDlpPath";
    private static final String KEY_FFMPEG_PATH = "ffmpegPath";
    private static final String KEY_SCAN_PATH = "scanFolderPath";

    // --------- DOWNLOAD DIR ---------
    public static String getDownloadDir() {
        return PREFS.get(KEY_DOWNLOAD_DIR, null);
    }

    public static void setDownloadDir(String path) {
        if (path == null || path.isBlank()) {
            PREFS.remove(KEY_DOWNLOAD_DIR);
        } else {
            PREFS.put(KEY_DOWNLOAD_DIR, path);
        }
    }

    // --------- YT-DLP PATH ---------
    public static String getYtDlpPath() {
        return PREFS.get(KEY_YTDLP_PATH, null);
    }

    public static void setYtDlpPath(String path) {
        if (path == null || path.isBlank()) {
            PREFS.remove(KEY_YTDLP_PATH);
        } else {
            PREFS.put(KEY_YTDLP_PATH, path);
        }
    }

    // --------- FFMPEG PATH ---------
    public static String getFfmpegPath() {
        return PREFS.get(KEY_FFMPEG_PATH, null);
    }

    public static void setFfmpegPath(String path) {
        if (path == null || path.isBlank()) {
            PREFS.remove(KEY_FFMPEG_PATH);
        } else {
            PREFS.put(KEY_FFMPEG_PATH, path);
        }
    }

    // --------- SCAN PATH ---------
    public static String getScanFolderPath() {
        return PREFS.get(KEY_SCAN_PATH, null);
    }

    public static void setScanFolderPath(String path) {
        if (path == null || path.isBlank()) {
            PREFS.remove(KEY_SCAN_PATH);
        } else {
            PREFS.put(KEY_SCAN_PATH, path);
        }
    }

    // --------- SCAN PATH ---------
    public static PreferencesData load() {
        PreferencesData d = new PreferencesData();
        d.setDownloadDir(getDownloadDir());
        d.setYtDlpPath(getYtDlpPath());
        d.setFfmpegPath(getFfmpegPath());
        d.setScanFolderPath(getScanFolderPath());
        return d;
    }

    public static void save(PreferencesData d) {
        setDownloadDir(d.getDownloadDir());
        setYtDlpPath(d.getYtDlpPath());
        setFfmpegPath(d.getFfmpegPath());
        setScanFolderPath(d.getScanFolderPath());
    }

    private UserPreferences() {
        // evitar instanciación
    }
}
