package global;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ui.MainFrame;

/**
 * Clase inicial que actúa como punto de entrada para la inicialización del
 * interfaz de edición de ficheros de secuencia (pipeline) en formato json.
 * Utiliza java.swing para gestionar la Interfaz Gráfica.
 *
 * @author imc
 * @version 1.0
 */
public class Interface {
	/**
	 * Punto de entrada de la aplicación.
	 *
	 * @param args - Argumentos de la línea de comandos.
	 */
	public static void main(String[] args) {
		/**
		 * Configurar el diseño visual nativo del sistema operativo (Windows, Mac,
		 * Linux)
		 */
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {
			/**
			 * Si falla, Swing utilizará de forma automática su diseño clásico por defecto
			 */
		}

		/** Iniciar la interfaz gráfica */
		SwingUtilities.invokeLater(() -> {
			try {
				/** Crear y mostrar la ventana principal */
				MainFrame frame = new MainFrame();
				frame.setVisible(true);

			} catch (Exception e) {
				System.err.println("❌ Error crítico al inicializar la ventana principal:");
				e.printStackTrace();
			}
		});
	}
}
