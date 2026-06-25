package ui;

import javax.swing.*;

import global.task.CmdOptionsConfig;
import global.task.ParamConfig;
import global.task.TaskConfig;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Extensión de la clase JPanel para gestionar el panel dinámico donde se
 * establece la información de los parámetros y opciones de comando de la tarea
 * que se está configurando.
 * 
 * @version 1.0
 */
public class DynamicFormPanel extends JPanel {
	/** Parámetros de la tarea, según el fichero de configuración */
	private final Map<String, ParamConfig> paramsConfig;
	/** Opciones de comando de la tarea */
	private final ArrayList<CmdOptionsConfig> cmdOptions;

	/** Componentes para las opciones de parámetros y opciones de comandos */
	private final Map<String, JComponent> fieldsMap = new HashMap<>();
	private final Map<String, JComponent> fieldsOptMap = new HashMap<>();

	/**
	 * Constructor de la clase
	 * 
	 * @param task - Tarea con la configuración a cargar
	 */
	public DynamicFormPanel(TaskConfig task) {
		this.paramsConfig = task.getParams();
		this.cmdOptions = (ArrayList<CmdOptionsConfig>) task.getCmdOptions();

		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buildForm();
	}

	private int buildFormParams(GridBagConstraints gbc, int row) {
		for (Map.Entry<String, ParamConfig> entry : paramsConfig.entrySet()) {
			String paramName = entry.getKey();
			ParamConfig prop = entry.getValue();
			String desc = prop.getDescription() != null ? prop.getDescription() : "";

			// Etiqueta con Tooltip
			gbc.gridx = 0;
			gbc.gridy = row;
			gbc.weightx = 0.3;
			String labelText = paramName + (prop.isRequired() ? " *" : "");
			JLabel label = new JLabel(labelText);
			label.setToolTipText(desc); // Asigna el tooltip a la etiqueta
			add(label, gbc);

			// Componente de entrada con Tooltip (Punto 2)
			gbc.gridx = 1;
			gbc.weightx = 0.7;
			JComponent inputComponent = createComponentForType(prop.getType(), desc);
			inputComponent.setToolTipText(desc); // Asigna el tooltip al campo de entrada
			add(inputComponent, gbc);

			fieldsMap.put(paramName, inputComponent);
			row++;
		}
		return row;
	}

	private int buildFormCmdOptions(GridBagConstraints gbc, int row) {
		for (CmdOptionsConfig cmdConfig : this.cmdOptions) {
			String paramName = cmdConfig.getName();

			String desc = cmdConfig.getDescription() != null ? cmdConfig.getDescription() : "";

			// 1. Etiqueta con Tooltip
			gbc.gridx = 0;
			gbc.gridy = row;
			gbc.weightx = 0.3;
			String labelText = paramName + (cmdConfig.isRequired() ? " *" : "");
			JLabel label = new JLabel(labelText);
			label.setToolTipText(desc); // Asigna el tooltip a la etiqueta
			add(label, gbc);

			// 2. Componente de entrada con Tooltip
			gbc.gridx = 1;
			gbc.weightx = 0.7;
			JComponent component = null;

			if (paramName.equals("charset")) {
				component = createComponentForType("Charset", desc);
			} else if (paramName.equals("firstLineHeaders")) {
				component = createComponentForType("Boolean", desc);
			} else {
				component = createComponentForType("String", desc);
			}
			component.setToolTipText(desc); // Asigna el tooltip al campo de entrada
			add(component, gbc);
			fieldsOptMap.put(paramName, component);
			row++;
		}
		return row;
	}

	private void buildForm() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		int row = 0;

		if (cmdOptions != null && cmdOptions.size() > 0) {
			row = this.buildFormCmdOptions(gbc, row);
		}
		if (paramsConfig != null) {
			row = this.buildFormParams(gbc, row);
		}

	}

	private JComponent createComponentForType(String type, String description) {
		switch (type) {
		case "Boolean":
			return new JCheckBox("Activar");
		case "String":
			if (description.contains("Valores posibles:")) {
				return new JComboBox<>(new String[] { "", "E", "I" });
			}
			return new JTextField(20);
		case "Integer":
			return new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
		case "ListInteger":
		case "ListString":
			return new JTextField(20);
		case "Charset":
			return CharsetCombo.createCharsetCombo();
		default:
			return new JTextField(20);
		}
	}

	/**
	 * Valida el formulario del panel
	 * 
	 * @return Boolean - true, si pasa las validaciones | false, en caso que no pase
	 *         las validaciones
	 */
	public boolean validateForm() {
		boolean isValid = true;

		for (Map.Entry<String, ParamConfig> entry : paramsConfig.entrySet()) {
			String paramName = entry.getKey();
			ParamConfig prop = entry.getValue();
			JComponent comp = fieldsMap.get(paramName);

			if (comp == null)
				continue;

			// Restaurar estado visual por defecto antes de validar de nuevo
			String defaultDesc = prop.getDescription() != null ? prop.getDescription() : "";
			comp.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
			comp.setToolTipText(defaultDesc);

			// Extrae el valor actual del componente en formato String
			String value = "";
			if (comp instanceof JTextField) {
				value = ((JTextField) comp).getText().trim();
			} else if (comp instanceof JComboBox) {
				Object selected = ((JComboBox<?>) comp).getSelectedItem();
				value = (selected != null) ? selected.toString().trim() : "";
			} else if (comp instanceof JSpinner) {
				value = ((JSpinner) comp).getValue().toString();
			} else if (comp instanceof JCheckBox) {
				value = ((JCheckBox) comp).isSelected() ? "true" : "false";
			}

			// Validación de los campos obligatorios
			if (prop.isRequired() && value.isEmpty()) {
				comp.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
				comp.setToolTipText("Este campo es obligatorio.");
				isValid = false;
				continue; // Pasa al siguiente campo
			}

			// Validación de los tipos de datos (solo si tiene contenido)
			if (!value.isEmpty()) {
				boolean typeError = false;
				String errorMsg = "";

				switch (prop.getType()) {
				case "Integer":
					try {
						Integer.parseInt(value);
					} catch (NumberFormatException e) {
						typeError = true;
						errorMsg = "Debe ser un número entero válido.";
					}
					break;

				case "ListInteger":
					if (!value.matches("^-?\\d+(;-?\\d+)*$")) {
						typeError = true;
						errorMsg = "Formato incorrecto. Use números separados por punto y coma (ej: 1;2;3).";
					}
					break;

				case "ListString":
					if (!value.matches("^[^;]+(;[^;]+)*$")) {
						typeError = true;
						errorMsg = "Formato incorrecto. Use textos separados por punto y coma (ej: texto1;texto2).";
					}
					break;
				}

				if (typeError) {
					comp.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
					comp.setToolTipText(errorMsg);
					isValid = false;
				}
			}
		}
		if (this.cmdOptions != null && this.cmdOptions.size() > 0) {
			for (CmdOptionsConfig config : this.cmdOptions) {
				String paramName = config.getName();
				JComponent comp = fieldsOptMap.get(paramName);

				if (comp == null)
					continue;

				// Restaurar estado visual por defecto antes de validar de nuevo
				String defaultDesc = config.getDescription() != null ? config.getDescription() : "";
				comp.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
				comp.setToolTipText(defaultDesc);

				// Extrae el valor actual del componente en formato String
				String value = "";
				if (comp instanceof JTextField) {
					value = ((JTextField) comp).getText().trim();
				} else if (comp instanceof JComboBox) {
					Object selected = ((JComboBox<?>) comp).getSelectedItem();
					value = (selected != null) ? selected.toString().trim() : "";
				} else if (comp instanceof JSpinner) {
					value = ((JSpinner) comp).getValue().toString();
				} else if (comp instanceof JCheckBox) {
					value = ((JCheckBox) comp).isSelected() ? "true" : "false";
				}

				// Valida los campos obligatorios
				if (config.isRequired() && value.isEmpty()) {
					comp.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
					comp.setToolTipText("Este campo es obligatorio.");
					isValid = false;
					continue; // Pasa al siguiente campo
				}

			}
		}

		// Refresca visualmente el panel para aplicar los cambios de borde de inmediato
		repaint();
		return isValid;
	}

	/**
	 * Devuelve los parámetros formateados en formato clave=valor unidos por
	 * tuberías.
	 * 
	 * @return Cadena con los parametros serializados. Ej:
	 *         "ObtenerFilaColumnaContiene|rowPositions=1;2;3;4|rowText=b112;a111;a112"
	 */
	public String getSerializedParams() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, JComponent> entry : fieldsMap.entrySet()) {
			String param = entry.getKey();
			JComponent comp = entry.getValue();
			String val = "";

			if (comp instanceof JCheckBox) {
				val = ((JCheckBox) comp).isSelected() ? "true" : "";
			} else if (comp instanceof JComboBox) {
				val = (String) ((JComboBox<?>) comp).getSelectedItem();
			} else if (comp instanceof JSpinner) {
				// Evitamos volcar ceros si no son obligatorios para mantener limpio el pipeline
				int num = (Integer) ((JSpinner) comp).getValue();
				val = num > 0 ? String.valueOf(num) : "";
			} else if (comp instanceof JTextField) {
				val = ((JTextField) comp).getText().trim();
			}

			if (val != null && !val.isEmpty()) {
				if (sb.length() > 0)
					sb.append("|");
				sb.append(param).append("=").append(val);
			}
		}
		return sb.toString();
	}

	/**
	 * Devuelve la opción del comando.
	 * 
	 * @param name - Nombre de la opcion
	 * @return - Opción de comando (atributo option)
	 */
	public String getOptionCmdOption(String name) {
		CmdOptionsConfig config = null;
		for (CmdOptionsConfig cmd : this.cmdOptions) {
			if (name.equals(cmd.getName())) {
				config = cmd;
				break; // Detiene la búsqueda al encontrar el primero
			}
		}
		return config.getOption();

	}

	/**
	 * Devuelve las opciones de comando para la tarea serializadas
	 * 
	 * @return Cadena con las opciones serializadas
	 */
	public String getSerializedCmdOptions() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, JComponent> entry : fieldsOptMap.entrySet()) {
			String param = entry.getKey();
			JComponent comp = entry.getValue();
			String val = "";
			String opt = "";

			if (comp instanceof JCheckBox) {
				val = ((JCheckBox) comp).isSelected() ? "true" : "";
			} else if (comp instanceof JComboBox) {
				val = (String) ((JComboBox<?>) comp).getSelectedItem();
			} else if (comp instanceof JSpinner) {
				int num = (Integer) ((JSpinner) comp).getValue();
				val = num > 0 ? String.valueOf(num) : "";
			} else if (comp instanceof JTextField) {
				val = ((JTextField) comp).getText().trim();
			}

			if (val != null && !val.isEmpty()) {
				opt = this.getOptionCmdOption(param);
				if (sb.length() > 0)
					sb.append(" ");
				sb.append(opt).append(" ").append(val);
			}
		}
		return sb.toString();
	}
}