package cat.dam.roig.cleanstream.ui.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;

public final class HelpFilesOpener {

    private HelpFilesOpener() {
    }

    public static void openApiDocs(java.awt.Component parent) {
        File apiDocFile = resolveApiDocsFile();

        if (!apiDocFile.exists()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "API Docs file not found:\n" + apiDocFile.getAbsolutePath(),
                    "Help",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        openFile(parent, apiDocFile);
    }

    public static void openUserManual(java.awt.Component parent) {
        File manualFile = resolveUserManualFile();

        if (!manualFile.exists()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "User Manual file not found:\n" + manualFile.getAbsolutePath(),
                    "Help",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        openFile(parent, manualFile);
    }

    private static File resolveApiDocsFile() {
        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData != null && !localAppData.isBlank()) {
            File installedFile = new File(localAppData + File.separator
                    + "CleanStream" + File.separator
                    + "doc" + File.separator
                    + "index.html");

            if (installedFile.exists()) {
                return installedFile;
            }
        }

        return new File("doc/index.html");
    }

    private static File resolveUserManualFile() {
        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData != null && !localAppData.isBlank()) {
            File installedFile = new File(localAppData + File.separator
                    + "CleanStream" + File.separator
                    + "manual" + File.separator
                    + "CleanStream_Manual.pdf");

            if (installedFile.exists()) {
                return installedFile;
            }
        }

        return new File("manual/CleanStream_Manual.pdf");
    }

    private static void openFile(java.awt.Component parent, File file) {
        if (!Desktop.isDesktopSupported()) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Desktop operations are not supported on this system.",
                    "Help",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    parent,
                    "Could not open file:\n" + file.getAbsolutePath() + "\n\n" + ex.getMessage(),
                    "Help",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
