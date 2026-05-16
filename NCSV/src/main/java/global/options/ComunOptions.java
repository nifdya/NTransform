package global.options;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

/**
 * Opciones comunes de entrada a todas las operaciones del modulo
 */
public class ComunOptions {

    /** Fichero de entrada */
	private CSVParser inputFile;
    /** Fichero de salida */
	private CSVPrinter outputFile;

	/**
	 * Obtener el fichero de entrada
	 * @return String  con el fichero de entrada
	 */
	public CSVParser getInputFile() {
		return inputFile;
	}
	/**
	 * Se establece el fichero de entrada
	 * @param String fichero de entrada
	 */
	public void setInputFile(CSVParser inputFile) {
		this.inputFile = inputFile;
	}
	/**
	 * Obtener el fichero de salida
	 * @return String  con el fichero de salida
	 */
	public CSVPrinter getOutputFile() {
		return outputFile;
	}
	/**
	 * Se establece el fichero de salida
	 * @param String fichero de salida
	 */	
	public void setOutputFile(CSVPrinter outputFile) {
		this.outputFile = outputFile;
	}

}
