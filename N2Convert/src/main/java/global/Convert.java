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
 * Universal format transformation module.
 * <p>
 * This class implements a command-line interface (CLI) application using Picocli
 * to orchestrate file conversions between multiple formats including JSON, XML,
 * CSV, XLSX (Excel), and fixed-width positional text files (TXT).
 * </p>
 * 
 * @version 1.0
 * @since 2026-06-13
 */
@Command(name = "convert", mixinStandardHelpOptions = true, version = "1.0", description = "Módulo completo de transformación universal de formatos.")
public class Convert implements Callable<Integer> {

	/**
	 * Target input file to be processed.
	 */
	@Option(names = { "-i", "--input" }, required = true, description = "Ruta del archivo de entrada.")
	private File inputFile;

	/**
	 * Output file destination where results will be stored.
	 */
	@Option(names = { "-o", "--output" }, required = true, description = "Ruta del archivo de salida.")
	private File outputFile;

	/**
	 * Configuration file containing XML specifications for fixed-width positional files.
	 */
	@Option(names = { "-d", "--fdefinitions" }, description = "Fichero con las definiciones JSON para el procesamiento de los ficheros de tipo de Ancho Fijo.")
	private String defFile;

	/**
	 * Transformation task strategy to execute.
	 */
	@Option(names = { "-t", "--task" }, description = "Operación. Valores: ${COMPLETION-CANDIDATES}", required = true)
	private Task task;

	/**
	 * Text encoding character set used during reading and writing operations.
	 */
	@Option(names = { "-c", "--charset" }, description = "Codificación (UTF-8, ISO-8859-1...)", defaultValue = "UTF-8")
	private String charsetName;
	
	/**
	 * Character sequence used to separate structural values within CSV operations.
	 */
	@Option(names = { "-dc", "--delimiter" }, description = "Delimitador de elementos csv, por defecto ';'", defaultValue = ";")
	private String delimiterCSV;
	
	/**
	 * 
	 */
	@Option(names = { "-ft", "--trace" }, description = "Fichero de traza, parámetro opcional", defaultValue = "")
	private String trace;

	/**
	 * Routes JSON structures into concrete data outputs based on the assigned task strategy.
	 * 
	 * @param inputPathStr  Absolute path mapping the target input source.
	 * @param outputPathStr Absolute path mapping the output destination.
	 * @param charset       Character encoding framework applied to data writing streams.
	 * @throws IOException If underlying converter fails to write or access file structures.
	 */
	private void convertJson2X(String inputPathStr, String outputPathStr, Charset charset) throws IOException
	{
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
	 * Routes XML structures into concrete data outputs based on the assigned task strategy.
	 * 
	 * @param inputPathStr  Absolute path mapping the target input source.
	 * @param outputPathStr Absolute path mapping the output destination.
	 * @param charset       Character encoding framework applied to data writing streams.
	 * @throws Exception If XML processing node resolution yields parsing faults.
	 */
	private void convertXml2X(String inputPathStr, String outputPathStr, Charset charset) throws Exception
	{
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
	 * Execution core for the transformation logic. Intercepts CLI parameters,
	 * prints module structural diagnostics, and routes format processing schemas.
	 * 
	 * @return {@code 0} upon successful translation pipeline termination, {@code 1} on file absence or processing failures.
	 */
	@Override
	public Integer call() {
		try {
			if(this.trace!=null && this.trace!="")
			{
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
	        if(task==Task.CSV2JSON || task==Task.CSV2TXT 
	          || task==Task.CSV2XLSX || task==Task.CSV2XML
	          || task==Task.XLSX2CSV || task==Task.TXT2CSV
	          || task==Task.JSON2CSV || task==Task.XML2CSV
	          )
	        {
	        	Convert.printModuleLog("⚙️ Delimitador CSV:'" + this.delimiterCSV+"'", false);	        	
	        }
	        if(cs!=null)
	        {
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
			Convert.printModuleLog("❌ Error en la transformación: " + e.getMessage(),false);
			e.printStackTrace();
			Convert.printModuleLogSpace(true, false);
			return 1;
		}
	}

	/**
	 * Main application bootstrap mechanism. Dispatches parameter parsing to Picocli framework.
	 * 
	 * @param args Application terminal runtime arguments.
	 */
	public static void main(String[] args) {
		CommandLine cmd = new CommandLine(new Convert());
		cmd.setCaseInsensitiveEnumValuesAllowed(true);
		System.exit(cmd.execute(args));
	}
	
	/**
	 * Prints structured module message output directly to the system console tracks.
	 * 
	 * @param message Target alphanumeric text to log.
	 * @param isError Switch flag determining if log hits standard stream {@code false} or error stream {@code true}.
	 */
	public static void printModuleLog(String message, Boolean isError) {
		if (isError) {
			System.err.println("CONVT - " + message);
		} else {
			System.out.println("CONVT - " + message);
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
