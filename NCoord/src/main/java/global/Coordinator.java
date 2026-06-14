package global;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import es.nesi.NLog;

/**
 * Clase principal que actúa como nodo coordinador para la ejecución de la
 * secuencia de transformadores.
 * 
 * @author imartinez.cerda
 * @version 1.0
 */
public class Coordinator {
	/**
	 * Prints structured module message output directly to the system console tracks.
	 * 
	 * @param message Target alphanumeric text to log.
	 * @param isError Switch flag determining if log hits standard stream {@code false} or error stream {@code true}.
	 */
	public static void printModuleLog(String message, Boolean isError) {
		if (isError) {
			System.err.println("COORD - " + message);
		} else {
			System.out.println("COORD - " + message);
		}
	}

	/**
	 * Prints a decorative operational separation line boundary across system IO channels.
	 * 
	 * @param isError    Switch flag mapping targeted output straight to error streams.
	 * @param addLineBreak Prefixes the layout line sequence with an operational new line skip when {@code true}.
	 */
	public static void printModuleLogSpace(Boolean isError, Boolean addLineBreak) {
		String boundaryLayout = "====================================================================================================";
		if (addLineBreak) {
			boundaryLayout = "\n" + boundaryLayout;
		}
		if (isError) {
			System.err.println(boundaryLayout);
		} else {
			System.out.println(boundaryLayout);
		}
	}

	public static void main(String[] args) {
		// El primer argumento es el nombre del fichero de secuencias que tiene que
		// estar en formato json
		File jsonFile = new File(args[0]);
		ObjectMapper mapper = new ObjectMapper();
		try {
			// Activamos el sistema de logs, y generemoa un fichero con el mismo nombre del
			// orquestador
			NLog.activate(PathUtils.replaceExtension(args[0], "txt"));

			// INICIAMOS
			Coordinator.printModuleLog("Cargando archivo de instrucciones JSON...", false);

			// Jackson parsea la estructura jerárquica
			ComunOptions config = mapper.readValue(jsonFile, ComunOptions.class);

			Coordinator.printModuleLog("Iniciamos el orquestador", false);
			// Ejecutar el orquestador
			CoordExecutor executor = new CoordExecutor();
			executor.executePipeline(config);

		} catch (Exception e) {
			Coordinator.printModuleLogSpace(true, false);
			Coordinator.printModuleLog("❌ Error fatal de Jackson al procesar el JSON: ", true);
			Coordinator.printModuleLogSpace(true, false);
			e.printStackTrace();
		}
	}
}
