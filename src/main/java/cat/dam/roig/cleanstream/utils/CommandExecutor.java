package cat.dam.roig.cleanstream.utils;

import cat.dam.roig.cleanstream.MainFrame;
import cat.dam.roig.cleanstream.PreferencesPanel;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author metku
 */
public class CommandExecutor {
    
    private static MainFrame MFrame = new MainFrame();
    private PreferencesPanel pnlPreferencesPanel = new PreferencesPanel(MFrame);
    
    public static void runCommand(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Leer salida del proceso
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    MFrame.getTxaLogArea().append(line + "\n");
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Proceso finalizado con código: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
