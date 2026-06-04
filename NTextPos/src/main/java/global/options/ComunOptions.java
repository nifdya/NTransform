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
	
	// Añadimos la metadata de posiciones fijas extraída del XML
	private List<Integer> campoLongitudes;
	private List<Boolean> campoIgnorados;

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

	public List<Integer> getCampoLongitudes() {
		return campoLongitudes;
	}

	public void setCampoLongitudes(List<Integer> campoLongitudes) {
		this.campoLongitudes = campoLongitudes;
	}

	public List<Boolean> getCampoIgnorados() {
		return campoIgnorados;
	}

	public void setCampoIgnorados(List<Boolean> campoIgnorados) {
		this.campoIgnorados = campoIgnorados;
	}
}
