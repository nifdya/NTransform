package global.task;
/**
 * Clase para la interacción con Jackson para el parseo y trabajo con el fichero de configuración json
 * En este caso, para el manejo de los parametros a configurar en el json
 * 
 * @version 1.0
 */
public class ParamConfig {
    private String type;
    private boolean required;
    private String description;
    private boolean index; 

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
