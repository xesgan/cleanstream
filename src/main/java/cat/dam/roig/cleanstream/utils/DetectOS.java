package cat.dam.roig.cleanstream.utils;

import java.nio.file.Path;

/**
 *
 * @author metku
 */
public class DetectOS {
    
    enum OS {WINDOWS, MAC, LINUX, OTHER};
    
    private static OS decectOs(){
        String os = System.getProperty("os.name", "generic").toLowerCase();
        if(os.contains("win")) return OS.WINDOWS;
        if(os.contains("mac") || os.contains("darwin")) return OS.MAC;
        if(os.contains("nux") || os.contains("nix")) return OS.LINUX;
        return OS.OTHER;
    }
    
    // Returns system downloads folder without the subfolder yt
//    private static Path getSystemDownloadDir() {
//        seguir aqui
//    }
}
