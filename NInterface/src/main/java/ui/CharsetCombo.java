package ui;

import java.nio.charset.StandardCharsets;

import javax.swing.JComboBox;

/**
 * Combo con las opciones de charset disponibles para la selección por los
 * usuarios
 * 
 * @version 1.0
 */
public class CharsetCombo {

	public static JComboBox<String> createCharsetCombo() {

		// Arreglo con los nombres de los charsets nativos
		String[] charsets = { StandardCharsets.US_ASCII.name(), StandardCharsets.ISO_8859_1.name(),
				StandardCharsets.UTF_8.name(), StandardCharsets.UTF_16BE.name(), StandardCharsets.UTF_16LE.name(),
				StandardCharsets.UTF_16.name() };

		JComboBox<String> combo = new JComboBox<>(charsets);

		// Seleccionar UTF-8 por defecto de forma segura
		combo.setSelectedItem(StandardCharsets.UTF_8.name());
		return combo;
	}
}
