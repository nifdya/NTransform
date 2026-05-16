package global.task;
/**
 * 
 *  Clase para el mapeo de jackson de los parámetros del json config.json
 *  
 */
public class ConfigParam {
	/** El tipo indicado en el json al parámetro */
    private TypeParam type;
    /** Si el parámetro es requerido */
    private boolean required;
    /** Si el parámetro contiene un índice */
    private boolean index;
    /** Descripción del parámetro */
    private String description;
    /** Valor que vendrá de los argumentos introducidos por comandos */
    private Object value; // El valor que Jackson mapeó


	// Getters y Setters
    public TypeParam getType() { return type; }
    public void setType(TypeParam type) { this.type = type; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isIndex() { return index; }
    public void setIndex(boolean index) { this.index = index; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Object getValue() { return value; }
	public void setValue(Object value) { this.value = value; }

}
