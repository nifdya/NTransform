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
import record.MapDefinitionsTextPos;
import record.RecordDefinitionTextPos;

@Command(name = "convert", mixinStandardHelpOptions = true, version = "1.0", description = "Módulo completo de transformación universal de formatos.")
public class Convert implements Callable<Integer> {

	@Option(names = { "-i", "--input" }, required = true, description = "Ruta del archivo de entrada.")
	private File inputFile;

	@Option(names = { "-o", "--output" }, required = true, description = "Ruta del archivo de salida.")
	private File outputFile;

	@Option(names = { "-d", "--fdefinitions" }, description = "Fichero con las definiciones XML de Ancho Fijo.")
	private String defFile;

	@Option(names = { "-t", "--task" }, description = "Operación. Valores: ${COMPLETION-CANDIDATES}", required = true)
	private Task task;

	@Option(names = { "-c", "--charset" }, description = "Codificación (UTF-8, ISO-8859-1...)", defaultValue = "UTF-8")
	private String charsetName;
	
	@Option(names = { "-dc", "--delimiter" }, description = "Delimitador de elementos csv, por defercto ';'", defaultValue = ";")
	private String delimiterCSV;

	private void convertJson2X(String in, String out, Charset cs) throws IOException
	{
		JsonToAllConverter jsonInverter = new JsonToAllConverter(cs);
		switch (task) {
	    case JSON2TXT:
	        jsonInverter.jsonToTxtPos(in, out, MapDefinitionsTextPos.getDefinitions(this.defFile));
	        break;
	    case JSON2CSV:
	        jsonInverter.jsonToCsv(in, out); // Limpio de dependencias XML
	        break;
	    case JSON2XLSX:
	        jsonInverter.jsonToExcel(in, out); // Limpio de dependencias XML
	        break;

		default:
			break;
		}
		
	}
	private void convertXml2X(String in, String out, Charset cs) throws Exception
	{
		XmlToAllConverter xmlInverter = new XmlToAllConverter(cs);	
		switch (task) {
	    case XML2TXT:
	        xmlInverter.xmlToTxtPos(in, out, MapDefinitionsTextPos.getDefinitions(this.defFile));
	        break;
	    case XML2CSV:
	        xmlInverter.xmlToCsv(in, out); // Limpio de dependencias XML
	        break;
	    case XML2XLSX:
	        xmlInverter.xmlToExcel(in, out); // Limpio de dependencias XML
	        break;
		}
	}
	
	
	@Override
	public Integer call() {
		try {
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
				new TxtPosCsvConverter(mapDefs, cs).txtPosToCsv(in, out);
				break;
			case CSV2TXT:
				new TxtPosCsvConverter(mapDefs, cs).csvToTxtPos(in, out);
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

			System.out.println("🚀 ¡Operación completada con éxito!");
			return 0;

		} catch (Exception e) {
			System.err.println("❌ Error en la transformación: " + e.getMessage());
			e.printStackTrace();
			return 1;
		}
	}

	public static void main(String[] args) {
		CommandLine cmd = new CommandLine(new Convert());
		cmd.setCaseInsensitiveEnumValuesAllowed(true);
		System.exit(cmd.execute(args));
	}
}
