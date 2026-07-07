package global.task;
/**
 * Clase para la interacción con Jackson para el parseo y trabajo con el fichero de configuración json
 * En este caso, para el manejo de las opciones cmdOptionsConfig
 * 
 * @version 1.0
 */
public class CmdOptionsConfig {
	private String name;
	private boolean required;
	private String description;
	private String option;
	private Object defaultValue;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	public String getOption() {
		return option;
	}
	
	public void setOption(String option) {
		this.option = option;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}
}
