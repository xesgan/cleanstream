package cat.dam.roig.cleanstream.models;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author metku
 */
public class MetadataTableModel extends AbstractTableModel {
    private final List<String[]> rows = new ArrayList<>();
    private final String[] cols = {"Propiedad","Valor"};

    public void setResource(ResourceDownloaded r){
        rows.clear();
        if (r != null) {
            rows.add(new String[]{"Nombre", r.getName()});
            rows.add(new String[]{"Ruta", r.getRoute()});
            rows.add(new String[]{"Tamaño", humanReadable(r.getSize())});
            rows.add(new String[]{"MIME", String.valueOf(r.getMimeType())});
            rows.add(new String[]{"Extensión", String.valueOf(r.getExtension())});
            rows.add(new String[]{"Fecha descarga", r.getDownloadDate()!=null? r.getDownloadDate().toString() : ""});
        }
        fireTableDataChanged();
    }

    @Override public int getRowCount(){ return rows.size(); }
    @Override public int getColumnCount(){ return cols.length; }
    @Override public String getColumnName(int c){ return cols[c]; }
    @Override public Object getValueAt(int r, int c){ return rows.get(r)[c]; }

    private static String humanReadable(long bytes){
        String[] u={"B","KB","MB","GB"};
        double v=bytes; int i=0;
        while(v>=1024 && i<u.length-1){ v/=1024; i++; }
        return String.format("%.1f %s", v, u[i]);
    }
}

