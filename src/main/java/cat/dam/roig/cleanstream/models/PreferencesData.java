package cat.dam.roig.cleanstream.models;

/**
 *
 * @author metku
 */
public class PreferencesData {

    private String downloadDir;
    private String ytDlpPath;
    private String ffmpegPath;
    private String scanFolderPath;
    private boolean openWhenDone;
    private boolean limitSpeedEnabled;
    private int speedKbps;
    private boolean createM3u;

    public boolean isOpenWhenDone() {
        return openWhenDone;
    }

    public void setOpenWhenDone(boolean openWhenDone) {
        this.openWhenDone = openWhenDone;
    }

    public boolean isLimitSpeedEnabled() {
        return limitSpeedEnabled;
    }

    public void setLimitSpeedEnabled(boolean limitSpeedEnabled) {
        this.limitSpeedEnabled = limitSpeedEnabled;
    }

    public int getSpeedKbps() {
        return speedKbps;
    }

    public void setSpeedKbps(int speedKbps) {
        this.speedKbps = speedKbps;
    }

    public boolean isCreateM3u() {
        return createM3u;
    }

    public void setCreateM3u(boolean createM3u) {
        this.createM3u = createM3u;
    }

    public String getDownloadDir() {
        return downloadDir;
    }

    public void setDownloadDir(String downloadDir) {
        this.downloadDir = downloadDir;
    }

    public String getYtDlpPath() {
        return ytDlpPath;
    }

    public void setYtDlpPath(String ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public void setFfmpegPath(String ffmpegPath) {
        this.ffmpegPath = ffmpegPath;
    }

    public String getScanFolderPath() {
        return scanFolderPath;
    }

    public void setScanFolderPath(String scanFolderPath) {
        this.scanFolderPath = scanFolderPath;
    }
}
