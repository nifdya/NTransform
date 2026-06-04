package global.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
/**
 * 
 *  Clase para el mapeo de jackson de los nodos tareas del json config.json
 *  
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigTask {
	/** Nombre de la tarea, debe ser único para el fichero */	
    private String task;
    /** Módulo que lo va a procesar */	
    private String module; 
    /** Lista de parámetros indexado por el nombre del atributo */	
    private Map<String, ConfigParam> params;

    // Getters y Setters
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
    public Map<String, ConfigParam> getParams() { return params; }
    public void setParams(Map<String, ConfigParam> params) { this.params = params; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }    
}

