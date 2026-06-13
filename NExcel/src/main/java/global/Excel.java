package global;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import global.options.ComunOptions;
import global.options.TaskOptions;
import global.options.TaskOptionsConfig;
import global.range.ModeRange;
import global.range.ParamRange;
import global.range.RangeConverter;
import global.task.FileConfigTaskConfiguration;
import global.task.Task;
import merge.MergeSheets;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import tunit.UnitaryTransformations;

/**
 * Clase principal que actúa como nodo de transformación para archivos Excel.
 * Utiliza la librería Picocli para gestionar la interfaz de línea de comandos
 * (CLI). Permite realizar diversas tareas como unir hojas o transformaciones
 * unitarias.
 * 
 * @author imc
 * @version 1.0
 */
@Command(name = "nes-excel", mixinStandardHelpOptions = true, version = "1.0", description = "Nodo para la transformación Excel")

public class Excel implements Callable<Integer> {

	/** Archivo de entrada. */
	@Option(names = { "-i", "--input" }, description = "Archivo de entrada")
	private File inputFile;

	/** Archivo de salid. */
	@Option(names = { "-o", "--output" }, description = "Archivo de salida")
	private File outputFile;

	/** Modo de procesamiento de los rangos (por defecto procesa todo). */
	@Option(names = { "-rm", "--moderange" }, description = "Modo de procesamiento")
	private ModeRange modeRange = ModeRange.all;

	/** Lista de rangos específicos a procesar definidos por el usuario. */
	@Option(names = { "-rp",
			"--paramrange" }, converter = RangeConverter.class, description = "Rangos de procesado. Format: start-end#start-end Ejemplo: '1-3#6-11'")
	private List<ParamRange> listRanges;

	/** Lista de tareas y sus configuraciones específicas pasadas por comando. */
	// Acepta parámetros estilo: -T limpiar:columna=A -T formatear:columna=B
	@Option(names = { "-t", "--task" }, description = "Tarea y su configuración (tarea:param=valor;param2=valor2)")
	List<String> listTaskInCommand;

	/**
	 * Crea un objeto de opciones comunes basándose en los parámetros de la CLI.
	 * 
	 * @return Instancia de {@link ComunOptions} con la configuración actual.
	 */
	private ComunOptions createOptionsObject() {
		ComunOptions opt = new ComunOptions();
		opt.setInputFile(this.inputFile);
		opt.setListRanges(this.listRanges);
		opt.setOutputFile(this.outputFile);
		opt.setModeRange(this.modeRange);
		return opt;
	}

	/**
	 * Determina el archivo de entrada
	 * 
	 * @return El archivo de entrada .
	 */
	private File getEffectiveInputFile() {
	    return this.inputFile.getAbsoluteFile();
	}

	/**
	 * Determina el archivo de salida real
	 * 
	 * @return El archivo de salida.
	 */
	private File getEffectiveOutputFile() {
	    return this.outputFile.getAbsoluteFile();
	}

	/**
	 * Carga un libro de trabajo Excel (.xlsx) desde un archivo.
	 * 
	 * @param file Archivo a cargar.
	 * @return Instancia de {@link XSSFWorkbook} o null si ocurre un error.
	 */

	protected XSSFWorkbook loadIputWorkbook(File file) {
	    try (FileInputStream fis = new FileInputStream(file)) {
	        return new XSSFWorkbook(fis);
	    } catch (Exception e) {
			Excel.printModuleLogSpace(true, false);
			Excel.printModuleLog("❌ Error fatal al cargar el fichero de origen: " + e.getMessage(), true);
			e.printStackTrace();
			Excel.printModuleLogSpace(true, false);	
			throw new IllegalStateException("Error al cargar el fichero: "+file.getAbsolutePath());
	    }
	} 

	/**
	 * Lógica principal de ejecución de la tarea. Gestiona el flujo de lectura,
	 * procesamiento de tareas y escritura del Excel.
	 * 
	 * @return Código de salida (0 éxito, 1 error).
	 */
	@Override
	public Integer call() {
		// 1. Resolvemos nombres de los ficheros de entrada y salida
		File fileIn = this.getEffectiveInputFile();
		File fileOut = this.getEffectiveOutputFile();

		// 2. Iniciamos el proceso
		try (InputStream in = new FileInputStream(fileIn); OutputStream out = new FileOutputStream(fileOut)) {

			// Creamos las opciones estableciendo los parámetros insertados
			ComunOptions opt = this.createOptionsObject();

			// Cargamos la estructura de parámetros según tarea
			FileConfigTaskConfiguration fctc = new FileConfigTaskConfiguration();
			// Establecemos los ficheros de entrada y salida y cargamos el fichero de
			// entrada en un objeto Workbook
			opt.setInputFile(fileIn);
			opt.setOutputFile(fileOut);
			XSSFWorkbook workbook = loadIputWorkbook(opt.getInputFile());

	        Excel.printModuleLogSpace(false, true);
	        Excel.printModuleLog("🚀 Iniciando Tratamiento Excel -->", false);
	        Excel.printModuleLog("📥 Fichero Inicial:" + this.inputFile.getAbsolutePath(), false);
	        Excel.printModuleLog("📤 Fichero Final:  " + this.outputFile.getAbsolutePath(), false);
	        Excel.printModuleLogSpace(false, false);

			// Procesar cada tarea solicitada por línea de comandos
			for (String iTask : listTaskInCommand) {
				//Obtenemos los parámetros de la tarea específica a ejecutar y la tarea
				String[] inputDataTask = iTask.split("\\|");
				Task currentTask = Task.valueOf(inputDataTask[0]);
				inputDataTask = Arrays.copyOfRange(inputDataTask, 1, inputDataTask.length); //el primer parámetro es la tarea, ya no nos hace falta
				TaskOptionsConfig optTaskConfig = new TaskOptionsConfig(fctc, currentTask, inputDataTask); //cargamos los parámetros establecidos en el fichero de configuración y los combinamos con los establecidos en la línea de comandos
				TaskOptions optTask = optTaskConfig.getTaskOptions(); //obtenemos los parametros de la tarea ya montados en el objeto.
				Excel.printModuleLog("📌 Iniciando Tarea:  " + currentTask, false);
				Excel.printModuleLog("🎛️ Parámetros de la Tarea:  " + String.join(", ", inputDataTask), false);
				switch (optTask.getModule()) {
				case "merge":
					MergeSheets cmb = new MergeSheets(opt, optTask);
					workbook = cmb.getOutputFile(workbook);
					break;
				case "tunit":
					UnitaryTransformations ut = new UnitaryTransformations(currentTask, opt, optTask);
					workbook = ut.doTask(workbook);
					break;

				default:
					break;
				}
				Excel.printModuleLog("✅ Finalizada Tarea:  " + currentTask, false);
			}
			//Escribimos el fichero final, depués de haber procesado todas las tareas. 
			workbook.write(out);
	        Excel.printModuleLogSpace(false, true);
	        Excel.printModuleLog("🚀 ¡Operación completada con éxito! Excel generado correctamente en:" +fileOut.getName(), false);
	        Excel.printModuleLogSpace(false, false);			
			return 0;

		} catch (Exception e) {
			Excel.printModuleLogSpace(true, false);
			Excel.printModuleLog("❌ Error fatal al general el Excel: " + e.getMessage(), true);
			e.printStackTrace();
			Excel.printModuleLogSpace(true, false);	
			return 1;
		}
	}

	/**
	 * Punto de entrada de la aplicación.
	 * 
	 * @param args Argumentos de la línea de comandos.
	 */
	public static void main(String[] args) {
		int exitCode = new CommandLine(new Excel()).execute(args);
		System.exit(exitCode);

	}
	
	/**
	 * Prints structured module message output directly to the system console tracks.
	 * 
	 * @param message Target alphanumeric text to log.
	 * @param isError Switch flag determining if log hits standard stream {@code false} or error stream {@code true}.
	 */
	public static void printModuleLog(String message, Boolean isError) {
		if (isError) {
			System.err.println("XSLX - " + message);
		} else {
			System.out.println("XSLX - " + message);
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
}
