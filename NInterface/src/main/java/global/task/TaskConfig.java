package global.task;

import java.util.Map;

public class TaskConfig {
    private String task;
    private String module;
    private String description;
    private Map<String, ParamConfig> params; // Jackson mapeará el objeto dinámico aquí

    // Getters y Setters
    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, ParamConfig> getParams() { return params; }
    public void setParams(Map<String, ParamConfig> params) { this.params = params; }
    @Override
    public String toString() {
        return this.getTask(); // Esto hace que el JList siga mostrando el texto visible correctamente
    }
}