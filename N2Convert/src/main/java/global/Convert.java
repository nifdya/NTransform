package global;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import convert.*;
import es.nesi.NLog;
import record.MapDefinitionsTextPos;
import record.RecordDefinitionTextPos;

/**
 * Módulo de transformación de formato universal.
 * <p>
 * Esta clase implementa una aplicación de interfaz de línea de comandos (CLI)
 * utilizando Picocli para orquestar conversiones de archivos entre múltiples
 * formatos, incluyendo JSON, XML, CSV, XLSX (Excel) y archivos de texto
 * posicional de ancho fijo (TXT).
 * </p>
 * 
 * @version 1.0
 * @since 2026-06-13
 */
@Command(name = "convert", mixinStandardHelpOptions = true, version = "1.0", description = "Módulo completo de transformación universal de formatos.")
public class Convert implements Callable<Integer> {

	/**
	 * Fichero de entrada a procesar
	 */
	@Option(names = { "-i", "--input" }, required = true, description = "Ruta del archivo de entrada.")
	private File inputFile;

	/**
	 * Fichero de salida que almacenará los resultados
	 */
	@Option(names = { "-o", "--output" }, required = true, description = "Ruta del archivo de salida.")
	private File outputFile;

	/**
	 * Fichero con las especificaciones JSON para ficheros delimitados por
	 * posiciones
	 */
	@Option(names = { "-d",
			"--fdefinitions" }, description = "Fichero con las definiciones JSON para el procesamiento de los ficheros de tipo de Ancho Fijo.")
	private String defFile;

	/**
	 * Tarea a ejecutar
	 */
	@Option(names = { "-t", "--task" }, description = "Operación. Valores: ${COMPLETION-CANDIDATES}", required = true)
	private Task task;

	/**
	 * Codificación de carácteres del fichero a trabajar
	 */
	@Option(names = { "-c", "--charset" }, description = "Codificación (UTF-8, ISO-8859-1...)", defaultValue = "UTF-8")
	private String charsetName;

	/**
	 * Caracter delimitador de columanas para ficheros CSV
	 */
	@Option(names = { "-dc",
			"--delimiter" }, description = "Delimitador de elementos csv, por defecto ';'", defaultValue = ";")
	private String delimiterCSV;

	/**
	 * Ruta del fichero de traza
	 */
	@Option(names = { "-ft", "--trace" }, description = "Fichero de traza, parámetro opcional", defaultValue = "")
	private String trace;

	/**
	 * Enruta estructuras JSON hacia salidas de datos concretas según la estrategia
	 * de la tarea asignada.
	 * 
	 * @param inputPathStr  Ruta absoluta que mapea la fuente de entrada de destino.
	 * @param outputPathStr Ruta absoluta que mapea el destino de la salida.
	 * @param charset       Marco de codificación de caracteres aplicado a los
	 *                      flujos de escritura de datos.
	 * @throws IOException Si el convertidor subyacente falla al escribir o acceder
	 *                     a las estructuras de archivos.
	 */
	private void convertJson2X(String inputPathStr, String outputPathStr, Charset charset) throws IOException {
		JsonToAllConverter jsonInverter = new JsonToAllConverter(charset);
		switch (task) {
		case JSON2TXT:
			jsonInverter.jsonToTxtPos(inputPathStr, outputPathStr, MapDefinitionsTextPos.getDefinitions(this.defFile));
			break;
		case JSON2CSV:
			jsonInverter.jsonToCsv(inputPathStr, outputPathStr); // Limpio de dependencias XML
			break;
		case JSON2XLSX:
			jsonInverter.jsonToExcel(inputPathStr, outputPathStr); // Limpio de dependencias XML
			break;

		default:
			break;
		}

	}

	/**
	 * Enruta estructuras XML hacia salidas de datos concretas según la estrategia
	 * de la tarea asignada.
	 * 
	 * @param inputPathStr  Ruta absoluta que mapea la fuente de entrada de destino.
	 * @param outputPathStr Ruta absoluta que mapea el destino de la salida.
	 * @param charset       Marco de codificación de caracteres aplicado a los
	 *                      flujos de escritura de datos.
	 * @throws Exception Si la resolución de nodos del procesamiento XML genera
	 *                   fallos de análisis (parsing).
	 */
	private void convertXml2X(String inputPathStr, String outputPathStr, Charset charset) throws Exception {
		XmlToAllConverter xmlInverter = new XmlToAllConverter(charset);
		switch (task) {
		case XML2TXT:
			xmlInverter.xmlToTxtPos(inputPathStr, outputPathStr, MapDefinitionsTextPos.getDefinitions(this.defFile));
			break;
		case XML2CSV:
			xmlInverter.xmlToCsv(inputPathStr, outputPathStr); // Limpio de dependencias XML
			break;
		case XML2XLSX:
			xmlInverter.xmlToExcel(inputPathStr, outputPathStr); // Limpio de dependencias XML
			break;
		}
	}

	/**
	 * Núcleo de ejecución para la lógica de transformación. Intercepta los
	 * parámetros de la CLI, imprime diagnósticos estructurales del módulo y enruta
	 * los esquemas de procesamiento de formato.
	 * 
	 * @return {@code 0} tras la terminación exitosa de la canalización de
	 *         traducción, {@code 1} en caso de ausencia de archivos o fallos de
	 *         procesamiento.
	 */
	@Override
	public Integer call() {
		try {
			if (this.trace != null && this.trace != "") {
				NLog.activate(trace);
			}
			if (!inputFile.exists()) {
				System.err.println("❌ El archivo de entrada no existe.");
				return 1;
			}

			String in = inputFile.getAbsolutePath();
			String out = outputFile.getAbsolutePath();
			Charset cs = Charset.forName(this.charsetName);

			// Lazy-load de definiciones solo si la tarea involucra formato posicional (TXT)
			// o tipado estructurado
			Map<String, RecordDefinitionTextPos> mapDefs = (this.defFile != null)
					? MapDefinitionsTextPos.getDefinitions(this.defFile)
					: null;
			Convert.printModuleLogSpace(false, true);
			Convert.printModuleLog("🚀 Iniciando Conversión -->", false);
			Convert.printModuleLog("📥 Fichero Inicial:" + in, false);
			Convert.printModuleLog("📤 Fichero Final:  " + out, false);
			Convert.printModuleLog("📌 Tarea:  " + task, false);
			if (task == Task.CSV2JSON || task == Task.CSV2TXT || task == Task.CSV2XLSX || task == Task.CSV2XML
					|| task == Task.XLSX2CSV || task == Task.TXT2CSV || task == Task.JSON2CSV || task == Task.XML2CSV) {
				Convert.printModuleLog("⚙️ Delimitador CSV:'" + this.delimiterCSV + "'", false);
			}
			if (cs != null) {
				Convert.printModuleLog("🔤 Charset:  " + cs, false);
			}
			Convert.printModuleLogSpace(false, false);

			switch (task) {
			case XLS2XLSX:
				new XlsXlsxConverter().xlsToXlsx(in, out);
				break;
			case CSV2XLSX:
				CsvExcelConverter.csvToXlsx(in, out, cs);
				break;
			case XLSX2CSV:
				CsvExcelConverter.xlsxToCsv(in, out, cs);
				break;
			case TXT2CSV:
				new TxtPosCsvConverter(mapDefs, cs, this.delimiterCSV).txtPosToCsv(in, out);
				break;
			case CSV2TXT:
				new TxtPosCsvConverter(mapDefs, cs, this.delimiterCSV).csvToTxtPos(in, out);
				break;
			case TXT2XLSX:
				new TxtPosExcelConverter(mapDefs, cs).txtPosToExcel(in, out);
				break;
			case XLSX2TXT:
				new TxtPosExcelConverter(mapDefs, cs).excelToTxtPos(in, out);
				break;
			case TXT2JSON:
				new TxtPosJsonXmlConverter(mapDefs, cs).txtPosToJson(in, out);
				break;
			case TXT2XML:
				new TxtPosJsonXmlConverter(mapDefs, cs).txtPosToXml(in, out);
				break;
			case CSV2JSON:
				new CsvJsonXmlConverter(cs, this.delimiterCSV).csvToJson(in, out);
				break;
			case CSV2XML:
				new CsvJsonXmlConverter(cs, this.delimiterCSV).csvToXml(in, out);
				break;
			case XLSX2JSON:
				ExcelJsonXmlConverter.excelToJson(in, out);
				break;
			case XLSX2XML:
				ExcelJsonXmlConverter.excelToXml(in, out);
				break;
			// INVERSAS DESDE JSON
			case JSON2TXT:
			case JSON2CSV:
			case JSON2XLSX:
				this.convertJson2X(in, out, cs);
				break;

			// INVERSAS DESDE XML
			case XML2TXT:
			case XML2CSV:
			case XML2XLSX:
				this.convertXml2X(in, out, cs);
				break;
			}
			Convert.printModuleLogSpace(false, true);
			Convert.printModuleLog("🚀 ¡Operación completada con éxito!", false);
			Convert.printModuleLogSpace(false, false);

			return 0;

		} catch (Exception e) {
			Convert.printModuleLogSpace(true, true);
			Convert.printModuleLog("❌ Error en la transformación: " + e.getMessage(), false);
			e.printStackTrace();
			Convert.printModuleLogSpace(true, false);
			return 1;
		}
	}

	/**
	 * Punto de entrada de la aplicación
	 * 
	 * @param args parámetros introducidos por la línea de comandos
	 */
	public static void main(String[] args) {
		CommandLine cmd = new CommandLine(new Convert());
		cmd.setCaseInsensitiveEnumValuesAllowed(true);
		System.exit(cmd.execute(args));
	}

	/**
	 * Imprime la salida de mensajes estructurados del módulo directamente en las
	 * vías de la consola del sistema.
	 * 
	 * @param message Texto alfanumérico de destino a registrar.
	 * @param isError Bandera de conmutación que determina si el registro se dirige
	 *                al flujo estándar {@code false} o al flujo de errores
	 *                {@code true}.
	 */
	public static void printModuleLog(String message, Boolean isError) {
		if (isError) {
			System.err.println("CONVT - " + message);
		} else {
			System.out.println("CONVT - " + message);
		}
	}

	/**
	 * Imprime una línea divisoria operativa decorativa a través de los canales de
	 * E/S del sistema.
	 * 
	 * @param isError      Bandera de conmutación que mapea la salida de destino
	 *                     directamente a los flujos de errores.
	 * @param addLineBreak Antepone un salto de línea operativo a la secuencia de la
	 *                     línea de diseño cuando es {@code true}.
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
