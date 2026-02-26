package cat.dam.roig.cleanstream.domain;

/**
 * Represents the synchronization state of a media resource between the local
 * machine and the DI Media Network (cloud).
 *
 * <p>
 * This enum is used to determine:
 * <ul>
 * <li>Which actions are available (upload / download)</li>
 * <li>How the resource should be visually represented in the UI</li>
 * <li>Filtering logic in lists and tables</li>
 * </ul>
 *
 * <p>
 * State meaning:
 * <ul>
 * <li><b>LOCAL_ONLY</b> → File exists only on the local filesystem</li>
 * <li><b>CLOUD_ONLY</b> → File exists only in the cloud</li>
 * <li><b>BOTH</b> → File exists both locally and in the cloud</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * <pre>
 * if (state == ResourceState.LOCAL_ONLY) {
 *     enableUploadButton();
 * }
 * </pre>
 *
 * This enum contains no behavior, only classification logic.
 *
 * @author metku
 */
public enum ResourceState {

    /**
     * Resource exists only on the local machine. It can be uploaded to the
     * cloud.
     */
    LOCAL_ONLY,
    /**
     * Resource exists only in the cloud. It can be downloaded locally.
     */
    CLOUD_ONLY,
    /**
     * Resource exists both locally and in the cloud. No upload/download action
     * is needed.
     */
    BOTH
}
