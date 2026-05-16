package global.task;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * 
 *  Clase que en la que se carga la configuración config.json del fichero resources/config.json
 *  Lee el fichero  y carga la estructuras y propiedades del mism.  
 *  
 *  
 *  
 */
public class FileConfigTaskConfiguration {
	private ObjectMapper mapper;
	private JsonNode rootNode;

	/**
	 * Crea la clase, lee el fichero y monta la estructura
	 * @throws IOException
	 */
	public FileConfigTaskConfiguration() throws IOException 
	{
		this.mapper = new ObjectMapper();
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config_excel.json")) {
		    if (is == null) {
		        throw new FileNotFoundException("El archivo config.json no se encontró dentro del JAR");
		    }

			rootNode = mapper.readTree(is);
		}
		//try (InputStream is = FileConfigTaskConfiguration.class.getClassLoader().getResourceAsStream("config.json")){
		//try (InputStream is = FileConfigTaskConfiguration.class.getClassLoader().getResourceAsStream("config.json")){
		//File archivoExterno = new File("C:\\Users\\ines\\config.json");

	//	try (InputStream is = new FileInputStream(archivoExterno)) {	

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
