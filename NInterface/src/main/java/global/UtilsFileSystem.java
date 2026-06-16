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

/**
 * Clase auxiliar con las utilidades para la gestión del sistema de ficheros.
 * 
 * @version 1.0
 */
public class UtilsFileSystem {

	/**
	 * 
	 * Devuelve una lista con todos los ficheros json que hay en el directorio
	 * especificado
	 * 
	 * @param directoryPath - Cadena con el directorio a trabajar.
	 * @return Listado de ficheros en el directorio.
	 * @throws IOException
	 */
	public static List<String> getJsonFiles(String directoryPath) throws IOException {
		Path directory = Paths.get(directoryPath);

		if (!Files.exists(directory) || !Files.isDirectory(directory)) {
			throw new IllegalArgumentException("The provided path does not exist or is not a directory.");
		}

		try (Stream<Path> stream = Files.list(directory)) {
			return stream.filter(file -> !Files.isDirectory(file))
					.filter(file -> file.toString().toLowerCase().endsWith(".json")).map(Path::toString)
					.collect(Collectors.toList());
		}
	}

	/**
	 * 
	 * Se le proporciona un directorio y un listado de ficheros json, devuelve la
	 * intersección de los nombres que existen en el directorio y se han suministrado en el
	 * listado.
	 * 
	 * 
	 * @param directoryPath - Cadena con el directorio a trabajar.
	 * @param jsonFiles     - Listado de ficheros que queremos comprobar con la
	 *                      intersección de los que hay en el directorio
	 * @return Listado de ficheros resultantes de la intersección entre los que hay
	 *         en el directorio especificado y el listado suministrado
	 * @throws IOException
	 */
	public static String[] getJsonFilesInResources(String directoryPath, String[] jsonFiles) throws IOException {
		Path directory = Paths.get(directoryPath);

		if (!Files.exists(directory) || !Files.isDirectory(directory)) {
			throw new IllegalArgumentException("The provided path does not exist or is not a directory.");
		}

		// Comprueba en el directorio físico los nombres de los ficheros
		List<String> physicalFilesOnDisk;
		try (Stream<Path> stream = Files.list(directory)) {
			physicalFilesOnDisk = stream.filter(file -> !Files.isDirectory(file))
					.map(file -> file.getFileName().toString()) 
					.collect(Collectors.toList());
		}

		// Los compara con los que hay en el array suministrada
		return Arrays.stream(jsonFiles).filter(physicalFilesOnDisk::contains).toArray(String[]::new); // Conversión a String[]
	}
	/**
	 * Obtiene la ruta absoluta del directorio donde se encuentra el jar
	 * 
	 * @return Cadena con la ruta absoluta del directorio donde se encuentra el jar
	 */
	public static String getJarPath() {
		try {
			// Obtiene la ruta absoluta desde donde se ejecuta el código
			String executionPath = UtilsFileSystem.class.getProtectionDomain().getCodeSource().getLocation().toURI()
					.getPath();

			File baseDir = new File(executionPath);

			// Si termina en un archivo .jar, obtenemos su carpeta contenedora
			if (executionPath.endsWith(".jar")) {
				baseDir = baseDir.getParentFile();
			} else {
				// Si estamos en el IDE (Maven/Gradle), subirá las carpetas necesarias
				// para salir de "target/classes" o carpetas similares hacia la raíz del
				// proyecto
				while (baseDir != null && (baseDir.getName().equals("classes") || baseDir.getName().equals("target")
						|| baseDir.getName().equals("bin"))) {
					baseDir = baseDir.getParentFile();
				}
			}

			return baseDir.getAbsolutePath();

		} catch (URISyntaxException e) {
			return new File("resources").getAbsolutePath();
		}
	}

	/**
	 * Obtiene la ruta absoluta del directorio de recursos
	 * 
	 * @return Cadena con la ruta absoluta del directorio de recursos
	 */
	public static String getResourcesPath() {
		try {
			// --> Obtiene la ubicación absoluta desde donde se ejecuta el código
			String executionPath = UtilsFileSystem.class.getProtectionDomain().getCodeSource().getLocation().toURI()
					.getPath();

			File baseDir = new File(executionPath);

			// Si termina en un archivo .jar, obtenemos su carpeta contenedora
			if (executionPath.endsWith(".jar")) {
				baseDir = baseDir.getParentFile();
			} else {
				// Si estamos en el IDE (Maven/Gradle), subirá las carpetas necesarias
				// para salir de "target/classes" o carpetas similares hacia la raíz del
				// proyecto
				while (baseDir != null && (baseDir.getName().equals("classes") || baseDir.getName().equals("target")
						|| baseDir.getName().equals("bin"))) {
					baseDir = baseDir.getParentFile();
				}
			}

			// --> Construye la ruta apuntando a la carpeta "resources" en la raíz real
			File resourcesDirectory = new File(baseDir, "resources");

			return resourcesDirectory.getAbsolutePath();

		} catch (URISyntaxException e) {
			return new File("resources").getAbsolutePath();
		}
	}

}
