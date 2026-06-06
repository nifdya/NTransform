package global;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import global.options.ComunOptions;
import global.options.TaskOptions;
import global.options.TaskOptionsConfig;
import global.task.FileConfigTaskConfiguration;
import global.task.Task;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import tunit.UnitaryTransformations;
import record.RecordDefinition;

public class NTextPos implements Callable<Integer> {

	@Option(names = { "-i", "--input" }, description = "Archivo de entrada")
	private String inputFile;

	@Option(names = { "-o", "--output" }, description = "Archivo de salida")
	private String outputFile;

	@Option(names = { "-d", "--fdefinitions" }, description = "Fichero con las definiciones XML")
	private String defFile;

	@Option(names = { "-t", "--task" }, description = "Tarea y su configuración")
	private List<String> listTaskInCommand;

	@Option(names = { "-c", "--charset" }, description = "Codificación de caracteres (UTF-8, ISO-8859-1...)", defaultValue = "UTF-8")
	private String charsetName;
	
	private Map<String, RecordDefinition> mapaDefiniciones = new HashMap<>();

	/**
	 * Lee el archivo XML de forma nativa ignorando validaciones de red y extrae las
	 * longitudes físicas de los campos.
	 */
	private void initializeDefinitions() {
	    try {
	        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        Document doc = dbf.newDocumentBuilder().parse(new File(this.defFile));
	        NodeList records = doc.getElementsByTagName("record");

	        for (int i = 0; i < records.getLength(); i++) {
	            Element record = (Element) records.item(i);

	            String type = record.getAttribute("type");
	            String posTypeStr = record.getAttribute("posType");
	            String lengthStr = record.getAttribute("length");

	            // CLAVE ÚNICA: Si no viene tipo en el XML, usamos estrictamente "t1"
	            String claveMap = (type != null && !type.isEmpty()) ? type : "default";

	            int posType = posTypeStr.isEmpty() ? 0 : Integer.parseInt(posTypeStr);
	            int lengthType = lengthStr.isEmpty() ? 0 : Integer.parseInt(lengthStr);

	            RecordDefinition def = new RecordDefinition(claveMap, posType, lengthType);

	            NodeList fields = record.getElementsByTagName("field");
	            for (int j = 0; j < fields.getLength(); j++) {
	                Element field = (Element) fields.item(j);
	                int length = Integer.parseInt(field.getAttribute("length"));
	                boolean ignore = "true".equals(field.getAttribute("ignore"));
	                def.addField(length, ignore);
	            }

	            this.mapaDefiniciones.put(claveMap, def);
	        }
	    } catch (Exception e) {
	        throw new RuntimeException("Error: " + e.getMessage(), e);
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
	 * Evalúa dinámicamente una línea de texto para determinar a qué tipo de registro pertenece.
	 * Si no se encuentra una coincidencia específica, devuelve el registro por defecto ("DEFAULT").
	 */
	public RecordDefinition determinarDefinicionParaLinea(String linea) {
		// Buscamos si existe un registro base para usar sus coordenadas de tipado de forma genérica
		RecordDefinition baseDef = this.mapaDefiniciones.get("default");
		if (baseDef == null && !this.mapaDefiniciones.isEmpty()) {
			// Si no hay un "DEFAULT" explícito, tomamos el primero disponible para inspeccionar la posición del tipo
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

	@Override
	public Integer call() {
		try {
			if (this.listTaskInCommand == null || this.listTaskInCommand.isEmpty())
				return 1;

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
				        UnitaryTransformations ut = new UnitaryTransformations(currentTask, opt, optTask, this.mapaDefiniciones);
				        ut.doTask();
				    }
				}


				currentInput = nextOutput;
			}

			return 0;

		} catch (Exception e) {
			System.err.println("Error en la cadena de procesamiento posicional: " + e.getMessage());
			e.printStackTrace();
			return 1;
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new NTextPos()).execute(args);
		System.exit(exitCode);
	}
}
