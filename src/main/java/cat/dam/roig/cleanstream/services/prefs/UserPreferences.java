package cat.dam.roig.cleanstream.services.prefs;

import java.util.prefs.Preferences;
import cat.dam.roig.cleanstream.config.PreferencesData;

/**
 * Centralized manager for persistent user preferences.
 *
 * <p>
 * This class wraps {@link Preferences} to provide a clean and controlled way to
 * store and retrieve application configuration values.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Persist configuration settings across application restarts</li>
 * <li>Encapsulate preference keys</li>
 * <li>Provide typed getters and setters</li>
 * <li>Convert between {@link PreferencesData} and stored values</li>
 * </ul>
 *
 * <p>
 * Design decisions:
 * <ul>
 * <li>All methods are static (utility-style access)</li>
 * <li>Keys are centralized as private constants</li>
 * <li>Blank values remove the preference instead of storing empty strings</li>
 * </ul>
 *
 * <p>
 * This class does not perform validation. Validation is handled separately by
 * {@code PreferencesValidator}.
 * </p>
 */
public final class UserPreferences {

    /**
     * Preferences node used for the entire application configuration.
     */
    private static final Preferences PREFS
            = Preferences.userRoot().node("cat/dam/roig/cleanstream/prefs");

    // Preference keys
    private static final String KEY_DOWNLOAD_DIR = "downloadDir";
    private static final String KEY_YTDLP_PATH = "ytDlpPath";
    private static final String KEY_FFMPEG_PATH = "ffmpegPath";
    private static final String KEY_SCAN_PATH = "scanFolderPath";
    private static final String KEY_OPEN_WHEN_DONE = "openWhenDone";
    private static final String KEY_LIMIT_SPEED_ENABLED = "limitSpeedEnabled";
    private static final String KEY_SPEED_KBPS = "speedKbps";
    private static final String KEY_CREATE_M3U = "createM3u";

    // ---------------------------------------------------------------------
    // Download directory
    // ---------------------------------------------------------------------
    /**
     * @return configured download directory or null if not set
     */
    public static String getDownloadDir() {
        return PREFS.get(KEY_DOWNLOAD_DIR, null);
    }

    /**
     * Stores the download directory path.
     *
     * @param path directory path; removed if null or blank
     */
    public static void setDownloadDir(String path) {
        if (path == null || path.isBlank()) {
            PREFS.remove(KEY_DOWNLOAD_DIR);
        } else {
            PREFS.put(KEY_DOWNLOAD_DIR, path);
        }
    }

    // ---------------------------------------------------------------------
    // yt-dlp path
    // ---------------------------------------------------------------------
    /**
     * @return configured yt-dlp executable path or null
     */
    public static String getYtDlpPath() {
        return PREFS.get(KEY_YTDLP_PATH, null);
    }

    /**
     * Stores yt-dlp executable path.
     *
     * @param path executable path; removed if null or blank
     */
    public static void setYtDlpPath(String path) {
        if (path == null || path.isBlank()) {
            PREFS.remove(KEY_YTDLP_PATH);
        } else {
            PREFS.put(KEY_YTDLP_PATH, path);
        }
    }

    // ---------------------------------------------------------------------
    // FFmpeg path
    // ---------------------------------------------------------------------
    /**
     * @return configured ffmpeg executable path or null
     */
    public static String getFfmpegPath() {
        return PREFS.get(KEY_FFMPEG_PATH, null);
    }

    /**
     * Stores ffmpeg executable path.
     *
     * @param path executable path; removed if null or blank
     */
    public static void setFfmpegPath(String path) {
        if (path == null || path.isBlank()) {
            PREFS.remove(KEY_FFMPEG_PATH);
        } else {
            PREFS.put(KEY_FFMPEG_PATH, path);
        }
    }

    // ---------------------------------------------------------------------
    // Scan folder
    // ---------------------------------------------------------------------
    /**
     * @return configured folder path used for scanning local files
     */
    public static String getScanFolderPath() {
        return PREFS.get(KEY_SCAN_PATH, null);
    }

    /**
     * Stores scan folder path.
     *
     * @param path folder path; removed if null or blank
     */
    public static void setScanFolderPath(String path) {
        if (path == null || path.isBlank()) {
            PREFS.remove(KEY_SCAN_PATH);
        } else {
            PREFS.put(KEY_SCAN_PATH, path);
        }
    }

    // ---------------------------------------------------------------------
    // Boolean preferences
    // ---------------------------------------------------------------------
    /**
     * @return true if downloaded files should open automatically
     */
    public static boolean getOpenWhenDone() {
        return PREFS.getBoolean(KEY_OPEN_WHEN_DONE, false);
    }

    public static void setOpenWhenDone(boolean v) {
        PREFS.putBoolean(KEY_OPEN_WHEN_DONE, v);
    }

    /**
     * @return true if download speed limitation is enabled
     */
    public static boolean getLimitSpeedEnabled() {
        return PREFS.getBoolean(KEY_LIMIT_SPEED_ENABLED, false);
    }

    public static void setLimitSpeedEnabled(boolean v) {
        PREFS.putBoolean(KEY_LIMIT_SPEED_ENABLED, v);
    }

    /**
     * @return configured speed limit in KB/s
     */
    public static int getSpeedKbps() {
        return PREFS.getInt(KEY_SPEED_KBPS, 0);
    }

    public static void setSpeedKbps(int v) {
        PREFS.putInt(KEY_SPEED_KBPS, v);
    }

    /**
     * @return true if M3U playlist creation is enabled
     */
    public static boolean getCreateM3u() {
        return PREFS.getBoolean(KEY_CREATE_M3U, false);
    }

    public static void setCreateM3u(boolean v) {
        PREFS.putBoolean(KEY_CREATE_M3U, v);
    }

    // ---------------------------------------------------------------------
    // Bulk load/save
    // ---------------------------------------------------------------------
    /**
     * Loads all persisted preferences into a {@link PreferencesData} object.
     *
     * @return populated PreferencesData instance
     */
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

    /**
     * Persists all values from the provided {@link PreferencesData}.
     *
     * @param d configuration object to persist
     */
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

    /**
     * Private constructor to prevent instantiation.
     */
    private UserPreferences() {
    }
}
