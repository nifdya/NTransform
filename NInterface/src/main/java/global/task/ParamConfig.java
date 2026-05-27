package global.task;

public class ParamConfig {
    private String type;
    private boolean required;
    private String description;
    private boolean index; // Por si procesas la propiedad "index" del JSON

    // Getters y Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isIndex() { return index; }
    public void setIndex(boolean index) { this.index = index; }
}
