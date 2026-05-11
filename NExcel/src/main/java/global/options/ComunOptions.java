package global.options;

import java.io.File;
import java.util.List;

import global.range.ModeRange;
import global.range.ParamRange;

/**
 * Opciones comunes de entrada para todas las operaciones del módulo. Almacena
 * la configuración de archivos y los criterios de filtrado por rango.
 * 
 */
public class ComunOptions {

	/** Fichero de entrada */
	private File inputFile;
	/** Fichero de salida */
	private File outputFile;
	/** Modo del rango de trabajo */
	private ModeRange modeRange = ModeRange.all;
	/** Lista de rangos específicos de filas o elementos. */
	private List<ParamRange> listRanges;

	/**
	 * Obtiene el fichero de entrada.
	 * 
	 * @return Objeto {@link File} con la ruta del fichero de entrada.
	 */
	public File getInputFile() {
		return inputFile;
	}

	/**
	 * Establece el fichero de entrada.
	 * 
	 * @param inputFile El archivo de origen.
	 */
	public void setInputFile(File inputFile) {
		this.inputFile = inputFile;
	}

	/**
	 * Obtiene el fichero de salida.
	 * 
	 * @return Objeto {@link File} con la ruta del fichero de salida.
	 */
	public File getOutputFile() {
		return outputFile;
	}

	/**
	 * Establece el fichero de salida.
	 * 
	 * @param outputFile El archivo de destino.
	 */
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	/**
	 * Obtiene el modo de rango de trabajo.
	 * 
	 * @return Valor del enumerado {@link ModeRange}.
	 */
	public ModeRange getModeRange() {
		return modeRange;
	}

	/**
	 * Establece el modo de rango de trabajo.
	 * 
	 * @param modeRange El modo de rango a aplicar (all, odd, even, etc.).
	 */
	public void setModeRange(ModeRange modeRange) {
		this.modeRange = modeRange;
	}

	/**
	 * Obtiene la lista de rangos configurados.
	 * 
	 * @return Lista de objetos {@link ParamRange}.
	 */
	public List<ParamRange> getListRanges() {
		return listRanges;
	}

	/**
	 * Establece la lista de rangos para el procesamiento.
	 * 
	 * @param listRanges Lista con los rangos definidos.
	 */
	public void setListRanges(List<ParamRange> listRanges) {
		this.listRanges = listRanges;
	}
}
