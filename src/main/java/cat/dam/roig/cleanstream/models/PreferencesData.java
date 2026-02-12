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
