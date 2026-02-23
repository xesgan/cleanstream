package cat.dam.roig.cleanstream.services.prefs;

import java.util.prefs.Preferences;
import cat.dam.roig.cleanstream.config.PreferencesData;

/**
 * Gestiona las rutas y ajustes persistentes de la aplicación.
 */
public class UserPreferences {

    // Nodo de preferencias para toda la app
    private static final Preferences PREFS
        = Preferences.userRoot().node("cat/dam/roig/cleanstream/prefs");

    // Claves
    private static final String KEY_DOWNLOAD_DIR = "downloadDir";
    private static final String KEY_YTDLP_PATH = "ytDlpPath";
    private static final String KEY_FFMPEG_PATH = "ffmpegPath";
    private static final String KEY_SCAN_PATH = "scanFolderPath";
    private static final String KEY_OPEN_WHEN_DONE = "openWhenDone";
    private static final String KEY_LIMIT_SPEED_ENABLED = "limitSpeedEnabled";
    private static final String KEY_SPEED_KBPS = "speedKbps";
    private static final String KEY_CREATE_M3U = "createM3u";

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

    // --------- CHECK BOXES ---------
    public static boolean getOpenWhenDone() {
        return PREFS.getBoolean(KEY_OPEN_WHEN_DONE, false);
    }

    public static void setOpenWhenDone(boolean v) {
        PREFS.putBoolean(KEY_OPEN_WHEN_DONE, v);
    }

    public static boolean getLimitSpeedEnabled() {
        return PREFS.getBoolean(KEY_LIMIT_SPEED_ENABLED, false);
    }

    public static int getSpeedKbps() {
        return PREFS.getInt(KEY_SPEED_KBPS, 0);
    }

    public static void setLimitSpeedEnabled(boolean v) {
        PREFS.putBoolean(KEY_LIMIT_SPEED_ENABLED, v);
    }

    public static void setSpeedKbps(int v) {
        PREFS.putInt(KEY_SPEED_KBPS, v);
    }

    public static boolean getCreateM3u() {
        return PREFS.getBoolean(KEY_CREATE_M3U, false);
    }
    
    public static void setCreateM3u(boolean v) {
        PREFS.putBoolean(KEY_CREATE_M3U, v);
    }

    // --------- PREFERENCES PERSISTENCE---------
    public static PreferencesData load() {
        PreferencesData d = new PreferencesData();
        d.setDownloadDir(getDownloadDir());
        d.setYtDlpPath(getYtDlpPath());
        d.setFfmpegPath(getFfmpegPath());
        d.setScanFolderPath(getScanFolderPath());
        d.setOpenWhenDone(getOpenWhenDone());
        d.setLimitSpeedEnabled(getLimitSpeedEnabled());
        d.setSpeedKbps(getSpeedKbps());
        d.setCreateM3u(getCreateM3u());
        return d;
    }

    public static void save(PreferencesData d) {
        setDownloadDir(d.getDownloadDir());
        setYtDlpPath(d.getYtDlpPath());
        setFfmpegPath(d.getFfmpegPath());
        setScanFolderPath(d.getScanFolderPath());
        setOpenWhenDone(d.isOpenWhenDone());
        setLimitSpeedEnabled(d.isLimitSpeedEnabled());
        setSpeedKbps(d.getSpeedKbps());
        setCreateM3u(d.isCreateM3u());
    }

    private UserPreferences() {
        // evitar instanciación
    }
}
