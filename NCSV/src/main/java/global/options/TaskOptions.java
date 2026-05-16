package global.options;

import java.util.Map;
/**
 * 
 *  Clase que representa las opciones propias de cada tarea, en ella se cargará la tarea el módulo y los 
 *  parámetros asociados a dicho tarea concreta. Indicado todo en fichero JSON
 *  
 *  
 *  
 */
public class TaskOptions {
	/** Nombre de la tarea, tiene que ser único para el proyecto */
    private String task;
    /** Módulo en el que se procesará la tarea */
    private String module; 
	/** Parámetros específicos de la tarea */
    private Map<String, Object> params;

    /**
     * Constructor de la clase en el que se carga toda la configuración 
     * @param task
     * @param module
     * @param params
     */
    public TaskOptions(String task, String module, Map<String, Object> params) {
        this.task = task;
        this.module = module;
        this.params = params;
    }

    /**
     * Devuelve la tarea cargada
     * @return
     */
    public String getTask() { return task; }
    /**
     * Devuelve el módulo relacionado con la tarea cargada
     * @return
     */
    public String getModule() { return module; } 
    /**
     * Obtiene el parámetro indexado por valor de  la lista de parámestros
     * @param <T>
     * @param clave
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String clave) {
        return (T) params.get(clave);
    }
}
