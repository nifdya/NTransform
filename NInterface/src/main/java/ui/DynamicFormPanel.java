package ui;

import javax.swing.*;
import global.task.ParamConfig;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class DynamicFormPanel extends JPanel {
    private final Map<String, ParamConfig> paramsConfig;
    private final Map<String, JComponent> fieldsMap = new HashMap<>();

    public DynamicFormPanel(Map<String, ParamConfig> paramsConfig) {
        this.paramsConfig = paramsConfig;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buildForm();
    }

    private void buildForm() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        if (paramsConfig == null) return;

        for (Map.Entry<String, ParamConfig> entry : paramsConfig.entrySet()) {
            String paramName = entry.getKey();
            ParamConfig prop = entry.getValue();
            String desc = prop.getDescription() != null ? prop.getDescription() : "";

            // 1. Etiqueta con Tooltip (Punto 2)
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.3;
            String labelText = paramName + (prop.isRequired() ? " *" : "");
            JLabel label = new JLabel(labelText);
            label.setToolTipText(desc); // Asigna el tooltip a la etiqueta
            add(label, gbc);

            // 2. Componente de entrada con Tooltip (Punto 2)
            gbc.gridx = 1;
            gbc.weightx = 0.7;
            JComponent inputComponent = createComponentForType(prop.getType(), desc);
            inputComponent.setToolTipText(desc); // Asigna el tooltip al campo de entrada
            add(inputComponent, gbc);

            fieldsMap.put(paramName, inputComponent);
            row++;
        }
    }

    private JComponent createComponentForType(String type, String description) {
        switch (type) {
            case "Boolean":
                return new JCheckBox("Activar");
            case "String":
                if (description.contains("Valores posibles:")) {
                    return new JComboBox<>(new String[]{"", "E", "I"});
                }
                return new JTextField(20);
            case "Integer":
                return new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
            case "ListInteger":
            case "ListString":
                return new JTextField(20);
            default:
                return new JTextField(20);
        }
    }

    /**
     * Devuelve los parámetros formateados en formato clave=valor unidos por tuberías.
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
                if (sb.length() > 0) sb.append("|");
                sb.append(param).append("=").append(val);
            }
        }
        return sb.toString();
    }
}