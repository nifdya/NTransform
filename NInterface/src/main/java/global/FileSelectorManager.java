package global;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileFilter;

import java.awt.BorderLayout;
import java.io.File;
/**
 * Clase par la gestión de un selector de ficheros
 * 
 * @version 1.0
 */
public class FileSelectorManager {

    /**
     * 
     * Abre un JFileChooser dialog para selecionar un fichero y devolver su ruta
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
    /**
     * 
     * Abre un JFileChooser dialog para selecionar la ruta y el nombre de u n nuevo fichero
     * 
     * @param initialDirectoryPath - Ruta del directorio de apertura pro defecto
     * @return Cadena con la ruta absoluta del fichero seleccionado o null, en caso de que se cancele
     * 
     */

    public static String saveFile(String initialDirectoryPath) {
        JFileChooser fileChooser = new JFileChooser();


        // --> Configurar el directorio inicial si existe
        if (initialDirectoryPath != null) {
            File initialDir = new File(initialDirectoryPath);
            if (initialDir.exists() && initialDir.isDirectory()) {
                fileChooser.setCurrentDirectory(initialDir);
            }
        }
        
        // --> Crear los filtros específicos solicitados
        FileNameExtensionFilter xlsxFilter = new FileNameExtensionFilter("Libro de Excel (*.xlsx)", "xlsx");
        FileNameExtensionFilter xlsFilter = new FileNameExtensionFilter("Libro de Excel 97-2003 (*.xls)", "xls");
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("Archivo JSON (*.json)", "json");
        FileNameExtensionFilter xmlFilter = new FileNameExtensionFilter("Archivo XML (*.xml)", "xml");
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Archivo de texto (*.txt)", "txt");
        
        // Añadirlos al selector
        fileChooser.addChoosableFileFilter(xlsxFilter);
        fileChooser.addChoosableFileFilter(xlsFilter);
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.addChoosableFileFilter(xmlFilter);
        fileChooser.addChoosableFileFilter(txtFilter);
        
        // Configurar el filtro por defecto (ej: xlsx)
        fileChooser.setFileFilter(xlsxFilter);
        
        // Permitir la opción "Todos los archivos" para guardarlo sin extensión
        fileChooser.setAcceptAllFileFilterUsed(true); 
        
        // --> Mostrar el diálogo de guardado
        int result = fileChooser.showSaveDialog(null);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String absolutePath = selectedFile.getAbsolutePath();
            
            // --> Obtener el filtro que el usuario ha seleccionado activamente
            FileFilter currentFilter = fileChooser.getFileFilter();
            
            // --> Auto-añadir la extensión correspondiente según el filtro si el usuario no la escribió
            if (currentFilter == xlsxFilter && !absolutePath.toLowerCase().endsWith(".xlsx")) {
                absolutePath += ".xlsx";
            } else if (currentFilter == xlsFilter && !absolutePath.toLowerCase().endsWith(".xls")) {
                absolutePath += ".xls";
            } else if (currentFilter == jsonFilter && !absolutePath.toLowerCase().endsWith(".json")) {
                absolutePath += ".json";
            } else if (currentFilter == xmlFilter && !absolutePath.toLowerCase().endsWith(".xml")) {
                absolutePath += ".xml";
            } else if (currentFilter == txtFilter && !absolutePath.toLowerCase().endsWith(".txt")) {
                absolutePath += ".txt";
            }
            // Si seleccionó "Todos los archivos" (AcceptAll), se respeta exactamente lo que escribió sin añadir nada (sin extensión)
            
            return absolutePath;
        }
        
        return null; // El usuario ha cancelado la selección
    }
    /**
	 * Abre un JFileChooser dialog para selecionar la ruta y el nombre de un nuevo
	 * fichero
	 * 
	 * @param initialDirectoryPath - Ruta del directorio de apertura pro defecto
	 * @param defaultFileName      - nombre por defcto
	 * @param description          - Descripción
	 * @param extension            - Extensión
	 * @returnCadena con la ruta absoluta del fichero seleccionado o null, en caso
	 *               de que se cancele
	 */

	public static String saveFile(String initialDirectoryPath, String defaultFileName, String description,
			String extension) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Guardar Archivo");

		if (initialDirectoryPath != null) {
			File initialDir = new File(initialDirectoryPath);
			if (initialDir.exists() && initialDir.isDirectory()) {
				fileChooser.setCurrentDirectory(initialDir);
			}
		}

		if (defaultFileName != null) {
			fileChooser.setSelectedFile(new File(defaultFileName));
		}

		// Filtro dinámico según lo que necesites guardar
		FileNameExtensionFilter filter = new FileNameExtensionFilter(description, extension);
		fileChooser.setFileFilter(filter);

		int result = fileChooser.showSaveDialog(null);

		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			String absolutePath = selectedFile.getAbsolutePath();

			if (!absolutePath.toLowerCase().endsWith("." + extension.toLowerCase())) {
				absolutePath += "." + extension;
			}
			return absolutePath;
		}
		return null;
	}
	public static JComponent createFileSelectorComponent(String txtName) {
	    JPanel panel = new JPanel(new BorderLayout(5, 0)); // 5 píxeles de separación horizontal
	    JTextField txtOrigen = new JTextField(20);
	    JButton btnBrowseOrigen = new JButton("...");

	    btnBrowseOrigen.addActionListener(e -> {
	        String path = FileSelectorManager.selectFile(UtilsFileSystem.getJarPath());
	        if (path != null) {
	            txtOrigen.setText(path);
	        }
	    });
	    if(txtName!=null)
	    {	    	
	    	txtOrigen.setName(txtName); 
	    }
	    panel.add(txtOrigen, BorderLayout.CENTER);
	    
	    panel.add(btnBrowseOrigen, BorderLayout.EAST);
	   
	    panel.setName("fileSelectorPanel"); 
	    
	    return panel;
	}
}
