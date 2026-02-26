package cat.dam.roig.cleanstream.config;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) that encapsulates all user preferences
 * related to the CleanStream application.
 *
 * <p>This class acts as a plain data holder and represents the
 * configurable settings defined in the Preferences panel.
 *
 * <p>It does not contain business logic. Its responsibility is only
 * to store and transport configuration values between:
 * <ul>
 *     <li>The Preferences UI panel</li>
 *     <li>The persistence layer (e.g., Preferences API or properties)</li>
 *     <li>The services layer (download, scanning, playback)</li>
 * </ul>
 *
 * <p>All fields have corresponding getters and setters following
 * JavaBean conventions, allowing integration with UI frameworks
 * and potential serialization mechanisms.
 *
 * @author metku
 */
public class PreferencesData implements Serializable{

    /**
     * Directory where downloaded media files will be stored.
     */
    private String downloadDir;

    /**
     * Absolute path to the yt-dlp executable.
     */
    private String ytDlpPath;

    /**
     * Absolute path to the ffmpeg executable.
     */
    private String ffmpegPath;

    /**
     * Directory that will be scanned for existing downloaded media.
     */
    private String scanFolderPath;

    /**
     * Indicates whether the downloaded file should be automatically
     * opened with the system default media player after completion.
     */
    private boolean openWhenDone;

    /**
     * Indicates whether download speed limitation is enabled.
     */
    private boolean limitSpeedEnabled;

    /**
     * Maximum download speed in kilobytes per second (KB/s).
     * Only applied if {@code limitSpeedEnabled} is true.
     */
    private int speedKbps;

    /**
     * Indicates whether an M3U playlist file should be generated
     * when downloading playlists.
     */
    private boolean createM3u;

    /**
     * Returns whether downloaded files should open automatically.
     *
     * @return true if auto-open is enabled
     */
    public boolean isOpenWhenDone() {
        return openWhenDone;
    }

    /**
     * Sets whether downloaded files should open automatically.
     *
     * @param openWhenDone true to enable auto-open
     */
    public void setOpenWhenDone(boolean openWhenDone) {
        this.openWhenDone = openWhenDone;
    }

    /**
     * Returns whether speed limiting is enabled.
     *
     * @return true if speed limiting is active
     */
    public boolean isLimitSpeedEnabled() {
        return limitSpeedEnabled;
    }

    /**
     * Enables or disables download speed limitation.
     *
     * @param limitSpeedEnabled true to enable speed limit
     */
    public void setLimitSpeedEnabled(boolean limitSpeedEnabled) {
        this.limitSpeedEnabled = limitSpeedEnabled;
    }

    /**
     * Returns the maximum download speed in KB/s.
     *
     * @return speed limit in KB/s
     */
    public int getSpeedKbps() {
        return speedKbps;
    }

    /**
     * Sets the maximum download speed in KB/s.
     *
     * @param speedKbps speed limit value
     */
    public void setSpeedKbps(int speedKbps) {
        this.speedKbps = speedKbps;
    }

    /**
     * Returns whether an M3U playlist file should be created.
     *
     * @return true if M3U generation is enabled
     */
    public boolean isCreateM3u() {
        return createM3u;
    }

    /**
     * Enables or disables M3U playlist creation.
     *
     * @param createM3u true to enable playlist creation
     */
    public void setCreateM3u(boolean createM3u) {
        this.createM3u = createM3u;
    }

    /**
     * Returns the download directory path.
     *
     * @return download directory
     */
    public String getDownloadDir() {
        return downloadDir;
    }

    /**
     * Sets the download directory path.
     *
     * @param downloadDir target download directory
     */
    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    /**
     * Returns the path to the yt-dlp executable.
     *
     * @return yt-dlp path
     */
    public String getYtDlpPath() {
        return ytDlpPath;
    }

    /**
     * Sets the path to the yt-dlp executable.
     *
     * @param ytDlpPath absolute path to yt-dlp
     */
    public void setYtDlpPath(String ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
    }

    /**
     * Returns the path to the ffmpeg executable.
     *
     * @return ffmpeg path
     */
    public String getFfmpegPath() {
        return ffmpegPath;
    }

    /**
     * Sets the path to the ffmpeg executable.
     *
     * @param ffmpegPath absolute path to ffmpeg
     */
    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    /**
     * Returns the folder path used for scanning local media files.
     *
     * @return scan folder path
     */
    public String getScanFolderPath() {
        return scanFolderPath;
    }

    /**
     * Sets the folder path used for scanning local media files.
     *
     * @param scanFolderPath directory to scan
     */
    public void setScanFolderPath(String scanFolderPath) {
        this.scanFolderPath = scanFolderPath;
    }
}