package global;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Opciones comunes del comando a ejecutar Que incluye los ficheros de entrada,
 * salida y el temporal y la lista de comandos a ejectuar.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComunOptions {
	public String inputFile;
	public String outputFile;
	public String tempFile;
	public List<Command> commands;
}
/**
 * Incluye la configuración de un comando, que puede incluir una o varias tareas. 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Command {
	/** Módulo jar ejecutable que procesará las tareas */
	public String jar;
	/** Número de paso en el computo global */
	public String step;
	/** Opciones del comando genéricas. */
	public String cmdOptions;
	/** Mantiene, o no, el fichero temporal en caso de error */
	public Boolean keepOnError;
	/** Listado de tareas a ejecutar */	
	public List<SubTarea> tasks;
}
/**
 * Tareas a ejecutar en un comando. Cadena con la tarea completa
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class SubTarea {
	public String task; // Ejemplo: "nomtarea1|p1=v1|p2=v2"

	public String getNombreLimpio() {
		if (task == null)
			return "Sin_Nombre";
		return task.split("\\|")[0];
	}

	@Override
	public String toString() {
		return this.task;
	}
}
