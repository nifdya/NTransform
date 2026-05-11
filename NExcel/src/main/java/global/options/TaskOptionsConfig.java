package global.options;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import global.task.ConfigParam;
import global.task.ConfigTask;
import global.task.FileConfigTaskConfiguration;
import global.task.Task;
/**
 * 
 *  Clase en la que tendremos la tareas así como su configuración y los valores que se establecieron 
 *  por la consola
 *  
 *  
 *  
 */
public class TaskOptionsConfig {

	/** Nombre de la tarea, identificador para el proyecto */
	private String nameTask;
	/** Configuración de la tarea, concretamente la info de configuracion del fichero config.json */
	private ConfigTask configTask;
	/** Objeto con los datos suministrados por consola de los parámetros */
	private Map<String, String> commandParams;
	/** Objeto con los datos del fichero de configuración */
	private FileConfigTaskConfiguration fctc; 
	/** Opciones de configuración de la tarea con valores y montadas */
	private TaskOptions taskOptions;

	/**
	 * Montamos la configuración de la tarea
	 * @param fctc - valores del fichero de configuración del proyecto y sus tareas
	 * @param task - nombre de la tarea a procesar
	 * @param params - Listado de parámetros proporcionados por comandos
	 * @throws IOException
	 */
	public TaskOptionsConfig(FileConfigTaskConfiguration fctc, Task task, String[] params) throws IOException {
		this.nameTask = task.toString().trim();
		this.fctc = fctc;
		this.loadCommandParams(params);
		this.loadListOptions();
	}

	/**
	 * Caramos los parametros suministrado en formato en var1=val1 en un HashMap indexado por nombre de parámetro  
	 * @param params
	 * @return
	 */
	private boolean loadCommandParams(String[] params) {
		this.commandParams = new HashMap<>();
		for (String par : params) {
			par = par.replace("'", "\"");
			String[] kv = par.split("=", 2);
			if (kv.length == 2)
				this.commandParams.put(kv[0].trim(), kv[1].trim());
			//System.err.println("Par: " + kv[0].trim() + "=" + kv[1].trim());
		}
		return true;
	}
	/**
	 * 
	 * @param cmdVal cadena con el valor introducido por linea de comandos
	 * @param detail configuración del parametro en el json 
	 * @return valor tipo Object convertido al  tipo de datos definido en json
	 */
	private Object convertType(String cmdVal, ConfigParam detail) {
		if (cmdVal == null)
			return null;

		// Al ser un Enum, el switch es mucho más limpio
		switch (detail.getType()) {
		case Boolean:
			return Boolean.parseBoolean(cmdVal);
		case Integer:
			return Integer.parseInt(cmdVal);
		case ListString:
			return Arrays.asList(cmdVal.split(";")).toArray(new String[0]);
		case ListInteger:
			List<String> temp = Arrays.asList(cmdVal.split(";"));
			Integer[] result = new Integer[temp.size()];
			for (int i = 0; i < temp.size(); i++) {
				result[i] = Integer.parseInt(temp.get(i).trim());
				//ApachePOI trabaja inicia con 0, pero excel no. Cómo por comandos pedimos que inicie por 1, en la carga restamos el desfase
				if(detail.isIndex())
				{					
					result[i] = result[i] - 1;
				}
			}
			return result;
		case Pattern:
			return java.util.regex.Pattern.compile(cmdVal);
		default:
			return cmdVal;
		}
	}

	/**
	 * Carga la lista de opciones segun la configuracion del json y los parámetros recibido como comandos para la tarea
	 * los valida y carga
	 * @return booleano con el resultado de la carga
	 * @throws JsonProcessingException
	 * @throws IllegalArgumentException
	 */
	private boolean loadListOptions() throws JsonProcessingException, IllegalArgumentException {

		if (this.fctc.getRootNode().isArray()) {
			JsonNode tareaNode = StreamSupport.stream(this.fctc.getRootNode().spliterator(), false)
					.filter(nodo -> nodo.has("task") && nodo.get("task").asText().equalsIgnoreCase(this.nameTask))
					.findFirst().orElse(null);
			// Convertimos el nodo a nuestra clase ConfigTask
			this.configTask = this.fctc.getMapper().treeToValue(tareaNode, ConfigTask.class);

			System.err.println("Tarea: " + this.configTask.getTask());
			System.err.println("Parametros: " + this.configTask.getParams());

			Map<String, Object> values = new HashMap<>();
			//recorre los parámetros específicos de la tarea, los carga convertidos y si falta alguno obligatorio lanza una excepción.
			this.configTask.getParams().forEach((name, detail) -> {
				if (this.commandParams.containsKey(name)) {
					String rawValue = this.commandParams.get(name);
	
					Object finalValue = convertType(rawValue, detail);
					System.err.println("Valor convertido para " + name + ": " + finalValue.toString());
					values.put(name, finalValue);
				} else if (detail.isRequired()) {
					throw new RuntimeException("Falta el parámetro obligatorio: " + name);
				}
			});
			this.taskOptions = new TaskOptions(configTask.getTask(), configTask.getModule(), values);
		}
		return true;
	}

	public String getNameTask() {
		return nameTask;
	}

	public void setNameTask(String nameTask) {
		this.nameTask = nameTask;
	}

	public ConfigTask getConfigTask() {
		return configTask;
	}

	public void setConfigTask(ConfigTask configTask) {
		this.configTask = configTask;
	}

	public TaskOptions getTaskOptions() {
		return taskOptions;
	}

	public void setTaskOptions(TaskOptions taskOptions) {
		this.taskOptions = taskOptions;
	}
}
