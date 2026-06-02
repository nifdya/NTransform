package ui;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.ToolTipManager;

import global.task.TaskConfig;

import java.awt.event.MouseEvent;


public class JListCustom extends JList<TaskConfig> {
	 // Constructor estándar
    public JListCustom(ListModel<TaskConfig> datos) {
        super(datos);
        // El componente se registra a sí mismo en el gestor de ToolTips
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int index = locationToIndex(event.getPoint());
        
        if (index >= 0 && getCellBounds(index, index).contains(event.getPoint())) {
            // Obtenemos el objeto genérico del modelo
            Object elemento = getModel().getElementAt(index);
            
            // Validamos si es una TaskConfig y extraemos su descripción
            if (elemento instanceof TaskConfig) {
                return ((TaskConfig) elemento).getDescription();
            }
        }
        return null;
    }
}
