package global.task;

import java.util.List;
import java.util.Map;
/**
 * Clase utilizada en la interacción con Jackson para el parseo y trabajo con el fichero de configuración json
 * En este caso, para el manejo de la tarea global: incluyendo el listado de parámetros y opciones de consola
 * 
 * @version 1.0
 */
public class TaskConfig {
    private String task;
    private String module;
    private String description;
    private Map<String, ParamConfig> params; 
    private List<CmdOptionsConfig> cmdOptions;

    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, ParamConfig> getParams() { return params; }
    public void setParams(Map<String, ParamConfig> params) { this.params = params; }


    public List<CmdOptionsConfig> getCmdOptions() { return cmdOptions; }
    public void setCmdOptions(List<CmdOptionsConfig> cmdOptions) { this.cmdOptions = cmdOptions; }

    @Override
    public String toString() {
        return this.getTask(); 
    }
}
