package global;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

import global.options.ComunOptions;
import global.options.TaskOptions;
import global.options.TaskOptionsConfig;

import global.task.FileConfigTaskConfiguration;
import global.task.Task;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import tunit.UnitaryTransformations;
/**
 * Clase principal que actúa como nodo de transformación para archivos CSV.
 * Utiliza la librería Picocli para gestionar la interfaz de línea de comandos
 * (CLI). Permite realizar diversas transformaciones unitarias.
 * 
 */
public class CSV implements Callable<Integer> {

	/** Archivo de entrada. */
	@Option(names = { "-i", "--input" }, description = "Archivo de entrada ")
	private String inputFile;

	/** Archivo de salida. */
	@Option(names = { "-o", "--output" }, description = "Archivo de salida (si falta, usa stdout)")
	private String outputFile;
	
	/** Delimitador entre campos CSV */
	@Option(names = { "-d", "--delimiter" }, description = "Delimitador CSV")
	private String delimiter = ";";


	/** Indicar si hay cabeceras en la primera línea del fichero */
	@Option(names = { "-f", "--headers" }, description = "Si hay cabeceras en la primera línea")
	private boolean firstLineHeaders = false; // Por defecto es false. Escribir -f lo vuelve true.
	
	/** Indica si se mantiene el fichero de cabeceras en el producto final */
	@Option(names = { "-k", "--keeph" }, description = "Si se mantendrá la cabecera en el fichero destino")
	private boolean keepHeaders = false; // Por defecto es false. Escribir -k lo vuelve true.

	/** Lista de tareas y sus configuraciones específicas pasadas por comando. */
	// Acepta parámetros estilo: -T limpiar:columna=A -T formatear:columna=B
	@Option(names = { "-t", "--task" }, description = "Tarea y su configuración (tarea:param=valor;param2=valor2)")
	List<String> listTaskInCommand;


	/**
	 * Crea un objeto de opciones comunes basándose en los parámetros de la CLI.
	 * 
	 * @param parser
	 * @param printer
	 * @return Instancia de {@link ComunOptions} con la configuración actual.
	 */
	private ComunOptions createOptionsObject(CSVParser parser,CSVPrinter printer) {
		ComunOptions opt = new ComunOptions();
		opt.setInputFile(parser);
		opt.setOutputFile(printer);
		return opt;
	}

	/**
	 * Carga un fichero CSV, según al ruta de fichero pasada por parámetro
	 * 
	 * @param path
	 * @return
	 */
	protected CSVParser loadCSVFile(String path) {

		// Definimos el formato: Excel usa punto y coma (;), CSV estándar usa coma (,)
		CSVFormat formato = null;
		if (this.firstLineHeaders ) {

			formato = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(this.delimiter).setHeader() 
					.setSkipHeaderRecord(true).setTrim(true).build();
		} else {
			formato = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(this.delimiter).setSkipHeaderRecord(true)
					.setTrim(true).build();
		}
		Reader in;
		try {
			in = new FileReader(path, StandardCharsets.UTF_8);
			return formato.parse(in);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

	}
	/**
	 * Método auxiliar para cargar el parser desde un File específico (necesario para las pasadas)
	 */
	private CSVParser loadCSVFromFile(File file) throws IOException {
	    CSVFormat.Builder builder = CSVFormat.Builder.create(CSVFormat.DEFAULT)
	            .setDelimiter(this.delimiter)
	            .setTrim(true);
	    
	    if (this.firstLineHeaders) {
	        builder.setHeader().setSkipHeaderRecord(true);
	    }
	    
	    return builder.build().parse(new FileReader(file, StandardCharsets.UTF_8));
	}

	/**
	 * Método auxiliar para obtener el formato del printer basado en el parser (mantiene cabeceras)
	 */
	@SuppressWarnings("deprecation")
	private CSVFormat getFormatForPrinter(CSVParser parser) {
	    CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT).setDelimiter(this.delimiter).build();
	    if (this.firstLineHeaders && this.keepHeaders &&   parser.getHeaderMap() != null) {
	        return format.withHeader(parser.getHeaderMap().keySet().toArray(new String[0]));
	    }
	    return format;
	}
	@Override
	public Integer call() {
		try {
	
			FileConfigTaskConfiguration fctc = new FileConfigTaskConfiguration();

			// Archivo actual en la cadena de transformación
			File currentInput = new File(this.inputFile);

			// Bucle de tareas
			for (int i = 0; i < this.listTaskInCommand.size(); i++) {
				String iTask = this.listTaskInCommand.get(i);
				String[] inputDataTask = iTask.split("\\|");
				Task currentTask = Task.valueOf(inputDataTask[0]);

				// Configuración de la tarea
				String[] taskParams = Arrays.copyOfRange(inputDataTask, 1, inputDataTask.length);
				TaskOptionsConfig optTaskConfig = new TaskOptionsConfig(fctc, currentTask, taskParams);
				TaskOptions optTask = optTaskConfig.getTaskOptions();


				// Determinar salida de este paso:
				// Si es la última tarea, escribimos en el outputFile original.
				// Si no, escribimos en un archivo temporal.
				File nextOutput;
				if (i == this.listTaskInCommand.size() - 1) {
					nextOutput = new File(this.outputFile);
				} else {
					nextOutput = File.createTempFile("csv_step_" + i + "_", ".tmp");
					nextOutput.deleteOnExit(); // Se borra al cerrar el programa
				}

				// --- EJECUCIÓN DE LA PASADA ---
				// Cargamos el parser sobre el archivo que dejó la tarea anterior
				try (CSVParser parser = loadCSVFromFile(currentInput);
						BufferedWriter writer = new BufferedWriter(new FileWriter(nextOutput, StandardCharsets.UTF_8));
						CSVPrinter printer = new CSVPrinter(writer, getFormatForPrinter(parser))) {
					
					ComunOptions opt = this.createOptionsObject(parser, printer);
					switch (optTask.getModule()) {
					case "tunit":
						UnitaryTransformations ut = new UnitaryTransformations(currentTask, opt, optTask);
						// El método doTask ahora debe procesar el parser y escribir en el printer
						ut.doTask();
						break;
					default:
						break;
					}
					if (!this.keepHeaders) {
					    this.firstLineHeaders = false;
					}
				}

				// El output de ahora será el input del siguiente paso
				currentInput = nextOutput;
			}

			return 0;

		} catch (Exception e) {
			System.err.println("Error en la cadena de CSV: " + e.getMessage());
			e.printStackTrace();
			return 1;
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new CSV()).execute(args);
		System.exit(exitCode);

	}

}
