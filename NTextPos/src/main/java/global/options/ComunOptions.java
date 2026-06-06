package global.options;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.List;

/**
 * Opciones comunes de entrada a todas las operaciones del módulo.
 * Centraliza los descriptores de archivos nativos y la metadata del XML.
 */
public class ComunOptions {

	private BufferedReader inputFile;
	private BufferedWriter outputFile;
	


	public BufferedReader getInputFile() {
		return inputFile;
	}

	public void setInputFile(BufferedReader inputFile) {
		this.inputFile = inputFile;
	}

	public BufferedWriter getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(BufferedWriter outputFile) {
		this.outputFile = outputFile;
	}

}
