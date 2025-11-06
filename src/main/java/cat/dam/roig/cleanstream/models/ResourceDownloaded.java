package cat.dam.roig.cleanstream.models;

import java.time.LocalDateTime;

/**
 *
 * @author metku
 */
public class ResourceDownloaded {
    
    private String name;
    private String route;
    private long size;
    private String mimeType;
    private LocalDateTime downloadDate;
    private String extension;

    public ResourceDownloaded() {
    }

    public ResourceDownloaded(String name, String route, long size, String mimeType, LocalDateTime downloadDate, String extension) {
        this.name = name;
        this.route = route;
        this.size = size;
        this.mimeType = mimeType;
        this.downloadDate = downloadDate;
        this.extension = extension;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoute() {
        return route;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
    
    public void setRoute(String route) {
        this.route = route;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public LocalDateTime getDownloadDate() {
        return downloadDate;
    }

    public void setDownloadDate(LocalDateTime downloadDate) {
        this.downloadDate = downloadDate;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        return name + " || Size=" + size + ", mimeType=" + mimeType + ", downloadDate=" + downloadDate + ", extension=" + extension + '}';
    }
    
}
