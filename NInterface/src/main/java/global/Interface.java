package global;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ui.MainFrame;

public class Interface {
    /**
     * Punto de entrada principal de la aplicación.
     */
    public static void main(String[] args) {
        // 1. Configurar el diseño visual nativo del sistema operativo (Windows, Mac, Linux)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Si falla, Swing utilizará de forma automática su diseño clásico por defecto
        }

        // 2. Iniciar la interfaz gráfica de forma segura en el hilo correcto de Swing
        SwingUtilities.invokeLater(() -> {
            try {
                // Crear e instanciar la ventana unificada
                MainFrame frame = new MainFrame();
                
                // Hacer visible la ventana en pantalla
                frame.setVisible(true);
                
            } catch (Exception e) {
                System.err.println("Error crítico al inicializar la ventana principal:");
                e.printStackTrace();
            }
        });
    }
}
