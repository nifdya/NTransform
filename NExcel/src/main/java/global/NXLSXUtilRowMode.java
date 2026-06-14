package global;

import org.apache.poi.ss.usermodel.Row;

/**
 * Clase para organizar la creación de las filas del excel
 * 
 */

public class NXLSXUtilRowMode {
	/** Modo de copiado de la celda */
	Integer mode = 0;
	/** Fila de entrada */
	Row rowInput;
	/** Fila de salida */
	Row rowOutput;
	/** Valor a buscar */
	String valueSearch;
	/** Valor a Reemplazar */
	String valueReplace;

	/** Constructor de la clase */
	public NXLSXUtilRowMode(Row rIn, Row rOut) {
		this.mode = 0;
		this.rowInput = rIn;
		this.rowOutput = rOut;
	}

	/**
	 * Establece el modo de reemplazo de valores y sus valores asociados.
	 * 
	 * @param search
	 * @param replace
	 * @return {@code true} si la ejecución ha finalizado correctamente;
	 *         {@code false} si se ha producido alguna excepción
	 */
	public boolean setReplaceMode(String search, String replace) {
		try {
			this.mode = 2;
			this.valueSearch = search;
			this.valueReplace = replace;
			return true;

		} catch (Exception e) {
			return false;
		}
	}

}
