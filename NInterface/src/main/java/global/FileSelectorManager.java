package global;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class FileSelectorManager {

    /**
     * Opens a JFileChooser dialog to select a JSON file.
     * @param initialDirectoryPath The path where the chooser should open by default.
     * @return The absolute path of the selected file, or null if the user cancelled.
     */
    public static String selectJsonFile(String initialDirectoryPath) {
        // 1. Create the file chooser
        JFileChooser fileChooser = new JFileChooser();
        
        // 2. Set the initial directory (if it exists)
        if (initialDirectoryPath != null) {
            File initialDir = new File(initialDirectoryPath);
            if (initialDir.exists() && initialDir.isDirectory()) {
                fileChooser.setCurrentDirectory(initialDir);
            }
        }
        
        // 3. Set a filter so the user only sees and selects .json files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JSON Configuration Files (*.json)", "json");
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false); // Disables the "All Files" option
        
        // 4. Show the "Open" dialog (null centers it on screen, or pass your JFrame)
        int result = fileChooser.showOpenDialog(null);
        
        // 5. Process the user selection
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            return selectedFile.getAbsolutePath(); // Returns the selected path
        }
        
        return null; // User closed or cancelled the dialog
    }
}
