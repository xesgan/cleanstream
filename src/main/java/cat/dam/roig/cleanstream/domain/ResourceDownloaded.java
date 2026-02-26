package cat.dam.roig.cleanstream.domain;

import java.time.LocalDateTime;

/**
 * Represents a media resource downloaded by the user.
 *
 * <p>
 * This class acts as a Domain Model (POJO) that encapsulates all relevant
 * information about a media file, both local and cloud-related metadata.
 *
 * <p>
 * It is used in:
 * <ul>
 * <li>Local downloads management</li>
 * <li>Cloud synchronization (DI Media Net)</li>
 * <li>JList / JTable representations</li>
 * <li>Filtering and search functionality</li>
 * </ul>
 *
 * <p>
 * The object may represent:
 * <ul>
 * <li>A local file only</li>
 * <li>A cloud file only</li>
 * <li>A file present in both locations</li>
 * </ul>
 *
 * Note: This class contains no business logic. It is purely a data container
 * (Domain Entity).
 *
 * @author metku
 */
public class ResourceDownloaded {

    /**
     * File name (without full path).
     */
    private String name;

    /**
     * Absolute file path in the local filesystem.
     */
    private String route;

    /**
     * File size in bytes.
     */
    private long size;

    /**
     * MIME type of the file (e.g. audio/mpeg, video/mp4).
     */
    private String mimeType;

    /**
     * Date and time when the file was downloaded.
     */
    private LocalDateTime downloadDate;

    /**
     * File extension (e.g. mp3, mp4, wav).
     */
    private String extension;

    /**
     * Original source URL from where the media was downloaded.
     */
    private String sourceURL;

    /**
     * ID of the uploader in the DI Media Network (if uploaded to cloud). May be
     * null if the file is local only.
     */
    private Integer uploaderId;

    /**
     * Nickname of the uploader in the DI Media Network. May be null if the file
     * is local only.
     */
    private String uploaderNick;

    /**
     * Default constructor.
     *
     * <p>
     * Required for:
     * <ul>
     * <li>Serialization / Deserialization</li>
     * <li>Jackson JSON mapping</li>
     * <li>Framework compatibility</li>
     * </ul>
     */
    public ResourceDownloaded() {
    }

    /**
     * Creates a fully initialized local resource.
     *
     * @param name File name
     * @param route Absolute path
     * @param size Size in bytes
     * @param mimeType MIME type
     * @param downloadDate Download date
     * @param extension File extension
     * @param sourceUrl Original source URL
     */
    public ResourceDownloaded(String name,
            String route,
            long size,
            String mimeType,
            LocalDateTime downloadDate,
            String extension,
            String sourceUrl) {
        this.name = name;
        this.route = route;
        this.size = size;
        this.mimeType = mimeType;
        this.downloadDate = downloadDate;
        this.extension = extension;
        this.sourceURL = sourceUrl;
    }

    /**
     * @return File name (without path)
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the file name.
     *
     * @param name File name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Absolute local path
     */
    public String getRoute() {
        return route;
    }

    /**
     * Sets the local file path.
     *
     * @param route Absolute path
     */
    public void setRoute(String route) {
        this.route = route;
    }

    /**
     * @return File size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets file size.
     *
     * @param size Size in bytes
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return MIME type of the file
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets MIME type.
     *
     * @param mimeType MIME type string
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return Download date and time
     */
    public LocalDateTime getDownloadDate() {
        return downloadDate;
    }

    /**
     * Sets download date.
     *
     * @param downloadDate Date and time
     */
    public void setDownloadDate(LocalDateTime downloadDate) {
        this.downloadDate = downloadDate;
    }

    /**
     * @return File extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Sets file extension.
     *
     * @param extension Extension without dot
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * @return Original download source URL
     */
    public String getSourceURL() {
        return sourceURL;
    }

    /**
     * Sets original source URL.
     *
     * @param sourceURL Source URL
     */
    public void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL;
    }

    /**
     * @return Cloud uploader ID (nullable)
     */
    public Integer getUploaderId() {
        return uploaderId;
    }

    /**
     * Sets cloud uploader ID.
     *
     * @param uploaderId ID from backend
     */
    public void setUploaderId(Integer uploaderId) {
        this.uploaderId = uploaderId;
    }

    /**
     * @return Cloud uploader nickname (nullable)
     */
    public String getUploaderNick() {
        return uploaderNick;
    }

    /**
     * Sets cloud uploader nickname.
     *
     * @param uploaderNick Nickname
     */
    public void setUploaderNick(String uploaderNick) {
        this.uploaderNick = uploaderNick;
    }

    /**
     * Returns a debug-friendly string representation.
     *
     * <p>
     * Useful for logging and debugging.
     */
    @Override
    public String toString() {
        return "ResourceDownloaded{"
                + "name=" + name
                + ", route=" + route
                + ", size=" + size
                + ", mimeType=" + mimeType
                + ", downloadDate=" + downloadDate
                + ", extension=" + extension
                + ", sourceURL=" + sourceURL
                + '}';
    }
}
