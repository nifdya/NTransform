package global;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.nesi.NLog;
import global.options.ComunOptions;
import global.options.TaskOptions;
import global.options.TaskOptionsConfig;
import global.task.FileConfigTaskConfiguration;
import global.task.Task;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import tunit.UnitaryTransformations;
import record.RecordDefinition;

/**
 * Clase principal que actúa como nodo de transformación para archivos de texto
 * delimitados por posiciones. Utiliza la librería Picocli para gestionar la
 * interfaz de línea de comandos (CLI). Permite realizar diversas
 * transformaciones unitarias.
 * 
 */
public class NTextPos implements Callable<Integer> {

	/** Archivo de entrada. */
	@Option(names = { "-i", "--input" }, description = "Archivo de entrada")
	private String inputFile;

	/** Archivo de salida. */
	@Option(names = { "-o", "--output" }, description = "Archivo de salida")
	private String outputFile;

	/** Fichero de definiciones, para obtener las características del fichero */
	@Option(names = { "-d", "--fdefinitions" }, description = "Fichero con las definiciones en formato JSON")
	private String defFile;

	/** Listado de tareas a ejecutar */
	@Option(names = { "-t", "--task" }, description = "Tarea y su configuración")
	private List<String> listTaskInCommand;

	/** Juego de caracteres del fichero de entrada y salida */
	@Option(names = { "-c",
			"--charset" }, description = "Codificación de caracteres (UTF-8, ISO-8859-1...)", defaultValue = "UTF-8")
	private String charsetName;

	/**
	 * Fichero de traza
	 */
	@Option(names = { "-ft", "--trace" }, description = "Fichero de traza, parámetro opcional", defaultValue = "log")
	private String trace;

	private Map<String, RecordDefinition> mapaDefiniciones = new HashMap<>();

	private void initializeDefinitions() {
		try {
			// Instanciamos el mapeador de Jackson
			ObjectMapper mapper = new ObjectMapper();

			// Leemos el archivo JSON directamente a un árbol de nodos astuto
			JsonNode rootNode = mapper.readTree(new File(this.defFile));

			// Obtenemos la lista de "records" (equivalente a
			// doc.getElementsByTagName("record"))
			JsonNode records = rootNode.path("definitions");

			if (records.isArray()) {
				for (JsonNode record : records) {

					// Extraemos atributos/propiedades (si no existen, path() devuelve un nodo vacío
					// seguro)
					String type = record.path("type").asText(null);
					int posType = record.path("posType").asInt(0);
					int lengthType = record.path("length").asInt(0);

					// CLAVE ÚNICA: Conservamos tu lógica exacta de "default" si viene vacío
					String claveMap = (type != null && !type.isEmpty()) ? type : "default";

					// Instanciamos tu objeto de definición original sin tocar tu clase Java
					RecordDefinition def = new RecordDefinition(claveMap, posType, lengthType);

					// Procesamos el array interno de "fields"
					JsonNode fields = record.path("fields");
					if (fields.isArray()) {
						for (JsonNode field : fields) {
							int length = field.path("length").asInt();
							boolean ignore = field.path("ignore").asBoolean(false);

							def.addField(length, ignore);
						}
					}

					// Guardamos en tu mapa existente
					this.mapaDefiniciones.put(claveMap, def);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Error cargando JSON: " + e.getMessage(), e);
		}
	}

	/**
	 * Configura el objeto de opciones comunes inyectando flujos y metadatos.
	 */
	private ComunOptions createOptionsObject(BufferedReader reader, BufferedWriter writer) {
		ComunOptions opt = new ComunOptions();
		opt.setInputFile(reader);
		opt.setOutputFile(writer);
		return opt;
	}

	/**
	 * Evalúa dinámicamente una línea de texto para determinar a qué tipo de
	 * registro pertenece. Si no se encuentra una coincidencia específica, devuelve
	 * el registro por defecto ("DEFAULT").
	 */
	public RecordDefinition determinarDefinicionParaLinea(String linea) {
		// Buscamos si existe un registro base para usar sus coordenadas de tipado de
		// forma genérica
		RecordDefinition baseDef = this.mapaDefiniciones.get("default");
		if (baseDef == null && !this.mapaDefiniciones.isEmpty()) {
			// Si no hay un "DEFAULT" explícito, tomamos el primero disponible para
			// inspeccionar la posición del tipo
			baseDef = this.mapaDefiniciones.values().iterator().next();
		}

		if (baseDef != null && baseDef.getLengthType() > 0) {
			int inicio = baseDef.getPosType() - 1; // Ajuste base 0 para Java substring
			int fin = inicio + baseDef.getLengthType();

			if (linea.length() >= fin) {
				String tipoLinea = linea.substring(inicio, fin);
				RecordDefinition defEncontrada = this.mapaDefiniciones.get(tipoLinea);
				if (defEncontrada != null) {
					return defEncontrada;
				}
			}
		}
		return this.mapaDefiniciones.get("default");
	}

	/**
	 * Función de entrada principal para la ejecución
	 */
	@Override
	public Integer call() {
		try {
			if (this.trace != null && this.trace != "") {
				NLog.activate(trace);
			}
			if (this.listTaskInCommand == null || this.listTaskInCommand.isEmpty()) {
				return 1;
			}
			NTextPos.printModuleLogSpace(false, true);
			NTextPos.printModuleLog("🚀 Iniciando Tratamiento del fichero de texto por posiciones -->", false);
			NTextPos.printModuleLog("📥 Fichero Inicial:" + this.inputFile, false);
			NTextPos.printModuleLog("📥 Fichero de definiciones:" + this.defFile, false);
			NTextPos.printModuleLog("🔤 Charset:  " + this.charsetName, false);
			NTextPos.printModuleLog("📤 Fichero Final:  " + this.outputFile, false);
			NTextPos.printModuleLogSpace(false, false);

			// 1. Cargamos la estructura del archivo XML
			this.initializeDefinitions();

			File currentInput = new File(this.inputFile);

			// 2. Bucle secuencial de ejecución por pasadas temporales
			for (int i = 0; i < this.listTaskInCommand.size(); i++) {
				String iTask = this.listTaskInCommand.get(i);
				String[] inputDataTask = iTask.split("\\|");
				Task currentTask = Task.valueOf(inputDataTask[0]);

				String[] taskParams = Arrays.copyOfRange(inputDataTask, 1, inputDataTask.length);
				FileConfigTaskConfiguration fctc = new FileConfigTaskConfiguration();
				TaskOptionsConfig optTaskConfig = new TaskOptionsConfig(fctc, currentTask, taskParams);
				TaskOptions optTask = optTaskConfig.getTaskOptions();
				NTextPos.printModuleLog("📌 Iniciando Tarea:  " + currentTask, false);
				NTextPos.printModuleLog("🎛️ Parámetros de la Tarea:  " + String.join(", ", inputDataTask), false);
				File nextOutput;
				if (i == this.listTaskInCommand.size() - 1) {
					nextOutput = new File(this.outputFile);
				} else {
					nextOutput = File.createTempFile("pos_step_" + i + "_", ".tmp");
					nextOutput.deleteOnExit();
				}

				// --- REEMPLAZA EL BLOQUE DE FLUJOS EN NTextPos.java POR ESTE ---
				java.nio.charset.Charset charset = java.nio.charset.Charset.forName(this.charsetName);

				try (BufferedReader reader = new BufferedReader(new FileReader(currentInput, charset));
						BufferedWriter writer = new BufferedWriter(new FileWriter(nextOutput, charset))) {

					ComunOptions opt = this.createOptionsObject(reader, writer);

					if ("tunit".equals(optTask.getModule())) {
						UnitaryTransformations ut = new UnitaryTransformations(currentTask, opt, optTask,
								this.mapaDefiniciones);
						ut.doTask();
					}
				}

				currentInput = nextOutput;
			}
			NTextPos.printModuleLogSpace(false, true);
			NTextPos.printModuleLog(
					"🚀 ¡Operación completada con éxito! Fichero generado correctamente en:" + this.outputFile, false);
			NTextPos.printModuleLogSpace(false, false);
			return 0;

		} catch (Exception e) {
			NTextPos.printModuleLogSpace(true, false);
			NTextPos.printModuleLog("❌ Error fatal al general el nuevo fichero posicional: " + e.getMessage(), true);
			e.printStackTrace();
			NTextPos.printModuleLogSpace(true, false);
			return 1;
		}
	}

	/**
	 * Función de entrada de la clase
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		int exitCode = new CommandLine(new NTextPos()).execute(args);
		System.exit(exitCode);
	}

	/**
	 * Prints structured module message output directly to the system console
	 * tracks.
	 * 
	 * @param message Target alphanumeric text to log.
	 * @param isError Switch flag determining if log hits standard stream
	 *                {@code false} or error stream {@code true}.
	 */
	public static void printModuleLog(String message, Boolean isError) {
		if (isError) {
			System.err.println("TEXTP - " + message);
		} else {
			System.out.println("TEXTP - " + message);
		}
	}

	/**
	 * Prints a decorative operational separation line boundary across system IO
	 * channels.
	 * 
	 * @param isError      Switch flag mapping targeted output straight to error
	 *                     streams.
	 * @param addLineBreak Prefixes the layout line sequence with an operational new
	 *                     line skip when {@code true}.
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
}
