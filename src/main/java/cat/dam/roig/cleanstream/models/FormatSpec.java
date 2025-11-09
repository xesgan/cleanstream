package cat.dam.roig.cleanstream.models;

/**
 *
 * @author metku
 */
/**
 * Selector -f y, opcionalmente, merge final (--merge-output-format).
 */
public record FormatSpec(String formatSelector, String mergeOutputFormat) {

    public boolean hasMerge() {
        return mergeOutputFormat != null && !mergeOutputFormat.isBlank();
    }
}
