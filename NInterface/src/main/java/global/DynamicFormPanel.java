package global;

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

        // Jackson nos da un mapa limpio listo para iterar
        for (Map.Entry<String, ParamConfig> entry : paramsConfig.entrySet()) {
            String paramName = entry.getKey();
            ParamConfig prop = entry.getValue();

            // Etiqueta del parámetro
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0.3;
            String labelText = paramName + (prop.isRequired() ? " *" : "");
            JLabel label = new JLabel(labelText);
            label.setToolTipText(prop.getDescription());
            add(label, gbc);

            // Componente de entrada
            gbc.gridx = 1;
            gbc.weightx = 0.7;
            JComponent inputComponent = createComponentForType(prop.getType(), prop.getDescription());
            add(inputComponent, gbc);

            fieldsMap.put(paramName, inputComponent);
            row++;
        }
    }

    private JComponent createComponentForType(String type, String description) {
        if (description == null) description = "";
        switch (type) {
            case "Boolean":
                return new JCheckBox(description);
            case "String":
                if (description.contains("Valores posibles:")) {
                    return new JComboBox<>(new String[]{"", "E", "I"});
                }
                return new JTextField(20);
            case "Integer":
                return new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
            case "ListInteger":
            case "ListString":
                JTextField listField = new JTextField(20);
                listField.setToolTipText("Separar valores por comas. " + description);
                return listField;
            default:
                return new JTextField(20);
        }
    }

    public String getSerializedParams() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JComponent> entry : fieldsMap.entrySet()) {
            String param = entry.getKey();
            JComponent comp = entry.getValue();
            String val = "";

            if (comp instanceof JCheckBox) {
                val = String.valueOf(((JCheckBox) comp).isSelected());
            } else if (comp instanceof JComboBox) {
                val = (String) ((JComboBox<?>) comp).getSelectedItem();
            } else if (comp instanceof JSpinner) {
                val = ((JSpinner) comp).getValue().toString();
            } else if (comp instanceof JTextField) {
                val = ((JTextField) comp).getText().trim();
            }

            if (!val.isEmpty() && !val.equals("false")) { 
                if (sb.length() > 0) sb.append("|");
                sb.append(param).append("=").append(val);
            }
        }
        return sb.toString();
    }
}
