package global;

/**
 * Clase que gestiona opciones de configuración relacionadas con los catálogos
 * json de los módulos individuales.
 * 
 * @version 1.0
 */
public class ConfigCatalog {
	/**
	 * Devuelve el jar que gestiona el fichero de configuración proporcionado
	 * 
	 * @param jsonFile - Fichero de catálogo (configuración) a evaluar
	 * @return String con el jar que ejecuta las tareas del catálogo
	 */
	public static String getNodeJarForJsonFile(String jsonFile) {
		String node = "";
		switch (jsonFile) {
		case "config_csv.json":
			node = "NCSV.jar";
			break;
		case "config_xlsx.json":
			node = "NXLSX.jar";
			break;
		case "config_convert.json":
			node = "N2Convert.jar";
			break;
		case "config_text.json":
			node = "NTextPos.jar";
			break;
		}
		return node;
	}
}
