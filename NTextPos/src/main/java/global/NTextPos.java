package global;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

public class NTextPos implements Callable<Integer> {

	@Option(names = { "-i", "--input" }, description = "Archivo de entrada")
	private String inputFile;

	@Option(names = { "-o", "--output" }, description = "Archivo de salida")
	private String outputFile;
	
	@Option(names = { "-d", "--fdefinitions" }, description = "Fichero con las definiciones XML")
	private String defFile;

	@Option(names = { "-t", "--task" }, description = "Tarea y su configuración")
	private List<String> listTaskInCommand;

	// Listas locales para almacenar la estructura de posiciones fijas
	private final List<Integer> campoLongitudes = new ArrayList<>();
	private final List<Boolean> campoIgnorados = new ArrayList<>();

	/**
	 * Lee el archivo XML de forma nativa ignorando validaciones de red
	 * y extrae las longitudes físicas de los campos.
	 */
	private void initializeDefinitions() {
		if (!this.campoLongitudes.isEmpty()) return; // Evita re-leer en cada pasada del bucle

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setValidating(false);
			//dbf.setFeature("http://apache.org", false);
			
			Document doc = dbf.newDocumentBuilder().parse(new File(this.defFile));
			NodeList fields = doc.getElementsByTagName("field");
			
			for (int i = 0; i < fields.getLength(); i++) {
				Element field = (Element) fields.item(i);
				int length = Integer.parseInt(field.getAttribute("length"));
				boolean ignore = "true".equals(field.getAttribute("ignore"));
				
				this.campoLongitudes.add(length);
				this.campoIgnorados.add(ignore);
			}
		} catch (Exception e) {
			throw new RuntimeException("Error leyendo tu XML de definiciones: " + e.getMessage(), e);
		}
	}

	/**
	 * Configura el objeto de opciones comunes inyectando flujos y metadatos.
	 */
	private ComunOptions createOptionsObject(BufferedReader reader, BufferedWriter writer) {
		ComunOptions opt = new ComunOptions();
		opt.setInputFile(reader);
		opt.setOutputFile(writer);
		// Inyectamos las listas leídas del XML para que UnitaryTransformations tenga acceso
		opt.setCampoLongitudes(this.campoLongitudes);
		opt.setCampoIgnorados(this.campoIgnorados);
		return opt;
	}

	@Override
	public Integer call() {
		try {
			if (this.listTaskInCommand == null || this.listTaskInCommand.isEmpty()) return 1;

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

				// --- EJECUCIÓN CON FLUJOS NATIVOS DE JAVA ---
				try (BufferedReader reader = new BufferedReader(new FileReader(currentInput, StandardCharsets.UTF_8));
					 BufferedWriter writer = new BufferedWriter(new FileWriter(nextOutput, StandardCharsets.UTF_8))) {
					
					ComunOptions opt = this.createOptionsObject(reader, writer);
					
					if ("tunit".equals(optTask.getModule())) {
						UnitaryTransformations ut = new UnitaryTransformations(currentTask, opt, optTask);
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
