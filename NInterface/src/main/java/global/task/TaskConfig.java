package global.task;

import java.util.List;
import java.util.Map;

public class TaskConfig {
    private String task;
    private String module;
    private String description;
    private Map<String, ParamConfig> params; 
    
    // NUEVO PARÁMETRO: Captura de forma dinámica el array del JSON de conversión
    private List<CmdOptionsConfig> cmdOptions;

    // Getters y Setters existentes
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, ParamConfig> getParams() { return params; }
    public void setParams(Map<String, ParamConfig> params) { this.params = params; }

    // NUEVOS Métodos Getter y Setter para cmdOptions
    public List<CmdOptionsConfig> getCmdOptions() { return cmdOptions; }
    public void setCmdOptions(List<CmdOptionsConfig> cmdOptions) { this.cmdOptions = cmdOptions; }

    @Override
    public String toString() {
        return this.getTask(); 
    }
}
