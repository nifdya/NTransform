package global;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UtilsFileSystem {
	
    /**
     * Finds all .json files in a directory and returns their absolute paths as a List of Strings.
     */
    public static List<String> getJsonFiles(String directoryPath) throws IOException {
        Path directory = Paths.get(directoryPath);
        
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("The provided path does not exist or is not a directory.");
        }

        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                .filter(file -> !Files.isDirectory(file))
                .filter(file -> file.toString().toLowerCase().endsWith(".json"))
                .map(Path::toString)
                .collect(Collectors.toList());
        }
    }

    /**
     * Filters the configuration array in memory.
     * Keeps only the filenames that actually exist in the physical directory.
     */
    public static String[] getJsonFilesInResources(String directoryPath, String[] jsonFiles) throws IOException {
        Path directory = Paths.get(directoryPath);

        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("The provided path does not exist or is not a directory.");
        }

        // 1. Scan the physical directory to get the actual filenames present on disk
        List<String> physicalFilesOnDisk;
        try (Stream<Path> stream = Files.list(directory)) {
            physicalFilesOnDisk = stream
                .filter(file -> !Files.isDirectory(file))
                .map(file -> file.getFileName().toString()) // Get just the name (e.g., "config.json")
                .collect(Collectors.toList());
        }

        // 2. Filter the input array and convert it directly back to a String[]
        return Arrays.stream(jsonFiles)
                .filter(physicalFilesOnDisk::contains)
                .toArray(String[]::new); // Converts the stream back to String[]
    }
    
    public static String getJarPath()
    {
        try {
            // 1. Obtiene la ubicación absoluta desde donde se ejecuta el código
            String executionPath = UtilsFileSystem.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            
            File baseDir = new File(executionPath);
            
            // Si termina en un archivo .jar, obtenemos su carpeta contenedora
            if (executionPath.endsWith(".jar")) {
                baseDir = baseDir.getParentFile();
            } else {
                // Si estamos en el IDE (Maven/Gradle), subirá las carpetas necesarias 
                // para salir de "target/classes" o carpetas similares hacia la raíz del proyecto
                while (baseDir != null && (baseDir.getName().equals("classes") || baseDir.getName().equals("target") || baseDir.getName().equals("bin"))) {
                    baseDir = baseDir.getParentFile();
                }
            }
            
            
            return baseDir.getAbsolutePath();
            
        } catch (URISyntaxException e) {
            return new File("resources").getAbsolutePath();
        }   	
    }
    
    public static String getResourcesPath() {
        try {
            // 1. Obtiene la ubicación absoluta desde donde se ejecuta el código
            String executionPath = UtilsFileSystem.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            
            File baseDir = new File(executionPath);
            
            // Si termina en un archivo .jar, obtenemos su carpeta contenedora
            if (executionPath.endsWith(".jar")) {
                baseDir = baseDir.getParentFile();
            } else {
                // Si estamos en el IDE (Maven/Gradle), subirá las carpetas necesarias 
                // para salir de "target/classes" o carpetas similares hacia la raíz del proyecto
                while (baseDir != null && (baseDir.getName().equals("classes") || baseDir.getName().equals("target") || baseDir.getName().equals("bin"))) {
                    baseDir = baseDir.getParentFile();
                }
            }
            
            // 2. Construye la ruta apuntando a la carpeta "resources" en la raíz real
            File resourcesDirectory = new File(baseDir, "resources");
            
            return resourcesDirectory.getAbsolutePath();
            
        } catch (URISyntaxException e) {
            return new File("resources").getAbsolutePath();
        }
    }

}
