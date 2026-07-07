package global.task;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Clase en la que se carga la configuración config.json desde un fichero externo.
 * Lee el fichero en la ruta absoluta del JAR y carga las estructuras y propiedades.
 */
public class FileConfigTaskConfiguration {
	private ObjectMapper mapper;
	private JsonNode rootNode;

	/**
	 * Crea la clase, localiza el fichero externo al JAR y monta la estructura.
	 * 
	 * @throws IOException
	 */
	public FileConfigTaskConfiguration() throws IOException {
		this.mapper = new ObjectMapper();
		
		try {
			// 1. Obtiene la ubicación física del JAR en ejecución
			File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
			String directorioDelJar = jarFile.getParent();
			System.out.println(directorioDelJar);
			
			// 2. Construye la ruta absoluta hacia: [directorio_del_jar]/resources/config_text.json
			Path rutaConfig = Paths.get(directorioDelJar, "resources", "config_csv.json");
			
			// 3. Abre el flujo de lectura del archivo externo
			try (InputStream is = Files.newInputStream(rutaConfig)) {
				rootNode = mapper.readTree(is);
			}
			
		} catch (URISyntaxException e) {
			// Encapsula el error de URI en una IOException para respetar la firma del método
			throw new IOException("Error al calcular la ruta absoluta del JAR", e);
		}
	}

	public ObjectMapper getMapper() {
		return mapper;
	}

	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public JsonNode getRootNode() {
		return rootNode;
	}

	public void setRootNode(JsonNode rootNode) {
		this.rootNode = rootNode;
	}
}
