package global;

import javax.swing.JFileChooser;
import java.io.File;
/**
 * Clase par la gestión de un selector de ficheros
 * 
 * @version 1.0
 */
public class FileSelectorManager {

    /**
     * 
     * Abre un JFileChooser dialog para selecionar un fichero JSON y devolver su ruta absoluta
     * 
     * @param initialDirectoryPath - Ruta del directorio de apertura pro defecto
     * @return Cadena con la ruta absoluta del fichero seleccionado o null, en caso de que se cancele
     * 
     */

    public static String selectFile(String initialDirectoryPath) {

    	JFileChooser fileChooser = new JFileChooser();

    	if (initialDirectoryPath != null) {
    		File initialDir = new File(initialDirectoryPath);
    		if (initialDir.exists() && initialDir.isDirectory()) {
    			fileChooser.setCurrentDirectory(initialDir);
    		}
    	}
    	
    	int result = fileChooser.showOpenDialog(null);
    	
    	if (result == JFileChooser.APPROVE_OPTION) {
    		File selectedFile = fileChooser.getSelectedFile();
    		return selectedFile.getAbsolutePath(); // Returns the selected path
    	}
    	
    	return null; // el usuario ha cancelado la selección
    }
}
