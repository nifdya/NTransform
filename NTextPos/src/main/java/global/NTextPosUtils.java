package global;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;

/**
 * Clase que maneja las utilidades de tratamiento de los ficheros de texto
 * delimitados por posiciones
 * 
 * @version 1.0
 */
public class NTextPosUtils {

	/**
	 * Transforma un índice numérico en la clave del mapa (ej. 0 -> "p0").
	 * 
	 * @param position - Índice numérico a transformar
	 * @return String con la clave retornada.
	 */
	private static String toKey(int position) {
		return "p" + position;
	}

	/**
	 * Toma un mapa de campos y reconstruye la línea con las posiciones físicas
	 * exactas del JSON. Rellena con espacios a la derecha si el dato es más corto,
	 * o trunca si se pasa.
	 * 
	 * @param record          - Registro con el contenido
	 * @param campoLongitudes - Valores con la delimitación de posiciones
	 * @return String Cadena formateada.
	 */
	private static String formatToFixedLength(Map<String, Object> record, List<Integer> campoLongitudes) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < campoLongitudes.size(); i++) {
			int length = campoLongitudes.get(i);
			Object val = record.get("p" + i);
			String strVal = (val != null) ? val.toString() : "";

			if (strVal.length() > length) {
				sb.append(strVal.substring(0, length));
			} else {
				// Relleno ultra-eficiente con espacios a la derecha (formato posicional
				// estándar)
				sb.append(String.format("%-" + length + "s", strVal));
			}
		}
		return sb.toString();
	}

	/**
	 * Copia un registro completo al flujo de salida convirtiéndolo a longitud fija.
	 * 
	 * @param record          - Registro con los valores a convertir
	 * @param writer          - Buffer de salida
	 * @param campoLongitudes - longitudes que delimitan las posiciones del registro
	 * @throws IOException
	 */
	public static void copyRow(Map<String, Object> record, BufferedWriter writer, List<Integer> campoLongitudes)
			throws IOException {
		if (record == null)
			return;
		writer.write(formatToFixedLength(record, campoLongitudes));
		writer.newLine();
	}

	/**
	 * Comprueba si una línea posicional está completamente vacía o llena de
	 * espacios. Se mantiene idéntico.
	 * 
	 * @param record - Registro con los valores a comprobar
	 * @return - Booleano que indica si alguno de los campos del registro tiene
	 *         algún valor no vacío
	 */
	public static boolean isRowEmpty(Map<String, Object> record) {
		if (record == null || record.isEmpty())
			return true;
		for (Object value : record.values()) {
			if (value != null && !value.toString().trim().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Reemplaza cadenas mediante Regex en toda la fila y la escribe en el destino
	 * respetando las longitudes fujas.
	 * 
	 * @param record          - Registro con los valores a convertir
	 * @param writer          - Buffer de salida
	 * @param valueSearch     - Expresión regular con la estructura a remplazar
	 * @param valueReplace    - Valor a incluir después del reemplazo
	 * @param campoLongitudes - Longitudes que delimitan las posiciones del registro
	 * @throws IOException
	 */
	public static void copyRow(Map<String, Object> record, BufferedWriter writer, String valueSearch,
			String valueReplace, List<Integer> campoLongitudes) throws IOException {
		if (record == null)
			return;

		Map<String, Object> modifiedRecord = new HashMap<>(record.size());
		for (Map.Entry<String, Object> entry : record.entrySet()) {
			Object val = entry.getValue();
			modifiedRecord.put(entry.getKey(), val != null ? val.toString().replaceAll(valueSearch, valueReplace) : "");
		}
		writer.write(formatToFixedLength(modifiedRecord, campoLongitudes));
		writer.newLine();
	}

	/**
	 * Motor de búsqueda optimizado para texto plano.
	 * 
	 * @param cellValue   - Valor de la celda
	 * @param searchValue - Valor buscado
	 * @param mode        - Modo de búsqueda
	 * @return - Boolean con el resultado si la cadena buscada se encuentra en la
	 *         celda.
	 */
	private static boolean matchesMode(Object cellValue, String searchValue, int mode) {
		if (cellValue == null || searchValue == null)
			return false;

		String cellLower = cellValue.toString().toLowerCase();
		String searchLower = searchValue.toLowerCase();

		switch (mode) {
		case 0:
			return cellLower.equals(searchLower);
		case 1:
			return cellLower.contains(searchLower);
		case 2:
			return cellLower.startsWith(searchLower);
		case 3:
			return cellLower.endsWith(searchLower);
		default:
			throw new IllegalArgumentException("Modo de búsqueda no válido: " + mode);
		}
	}

	/**
	 * Comprueba si un registro contiene el valor buscado, para el modo de búsqueda
	 * especificado.
	 * 
	 * @param record         - Registro con los valores a buscar
	 * @param columnPosition - Columna en la que se buscará el valor indicado.
	 * @param valueStr       - Valor buscado
	 * @param mode           - Modo de búsqueda
	 * @return - Boolean con el resultado si la cadena buscada se encuentra en la
	 *         celda.
	 */
	public static boolean columnContains(Map<String, Object> record, Integer columnPosition, String valueStr,
			int mode) {
		if (record == null)
			return false;
		String key = toKey(columnPosition);
		if (!record.containsKey(key))
			return false;
		return matchesMode(record.get(key), valueStr, mode);
	}

	/**
	 * Comprueba si un registro contiene alguno de los valores de la lista de
	 * valores buscados, para el modo de búsqueda especificado, para la columna con
	 * la posición indicada.
	 * 
	 * @param record         - Registro con los valores a buscar
	 * @param columnPosition - Columna en la que se buscará el valor indicado.
	 * @param valuesStr      - Listado de valores buscados
	 * @param mode           - Modo de búsqueda
	 * @return - Boolean con el resultado si la cadena buscada se encuentra en la
	 *         celda.
	 */
	public static boolean columnContains(Map<String, Object> record, Integer columnPosition, String[] valuesStr,
			int mode) {
		if (record == null)
			return false;
		String key = toKey(columnPosition);
		if (!record.containsKey(key))
			return false;

		Object cellValue = record.get(key);
		for (String val : valuesStr) {
			if (matchesMode(cellValue, val, mode))
				return true;
		}
		return false;
	}

	/**
	 * Comprueba si un registro contiene alguno de los valores de la lista de
	 * valores buscados, para el modo de búsqueda específicado, en la lista de
	 * columnas indicadas.
	 * 
	 * @param record          - Registro con los valores a buscar
	 * @param columnPositions - Lista de columnas en las que se buscará el valor
	 *                        indicado.
	 * @param valuesStr       - Listado de valores buscados
	 * @param mode            - Modo de búsqueda
	 * @return - Boolean con el resultado si la cadena buscada se encuentra en la
	 *         celda.
	 */
	public static boolean columnContains(Map<String, Object> record, Integer[] columnPositions, String[] valuesStr,
			int mode) {
		if (record == null)
			return false;
		for (int pos : columnPositions) {
			if (columnContains(record, pos, valuesStr, mode))
				return true;
		}
		return false;
	}

	/**
	 * Comprueba si un registro contiene el valor indicado, para el modo de búsqueda
	 * específicado, en la lista de posiciones indicadas.
	 * 
	 * @param record          - Registro con los valores a buscar
	 * @param valueStr        - Valor a localizar
	 * @param columnPositions - Posiciones en las que se comprobará si se encuentra
	 * @param mode            - Modo de búsqueda
	 * @return - Boolean con el resultado si la cadena buscada se encuentra en la
	 *         celda.
	 */
	public static boolean rowContains(Map<String, Object> record, String valueStr, Integer[] columnPositions,
			int mode) {
		if (record == null)
			return false;
		for (int pos : columnPositions) {
			if (columnContains(record, pos, valueStr, mode))
				return true;
		}
		return false;
	}

	/**
	 * Comprueba si un registro contiene el valor indicado, para el modo de búsqueda
	 * específicado en alguno de los campos del registro.
	 * 
	 * @param record   - Registro con los valores a buscar
	 * @param valueStr - Valor a localizar
	 * @param mode     - Modo de búsqueda
	 * @return - Boolean con el resultado si la cadena buscada se encuentra en la
	 *         celda.
	 */
	public static boolean rowContains(Map<String, Object> record, String valueStr, int mode) {
		if (record == null)
			return false;
		for (Object value : record.values()) {
			if (matchesMode(value, valueStr, mode))
				return true;
		}
		return false;
	}

	/**
	 * Une varias columnas basándose en una plantilla indexada tipo $1/$2. Se
	 * mantiene idéntico.
	 * 
	 * @param record          - Registro con los valores a buscar
	 * @param targetPositions - Columnas a unir
	 * @param joinTemplate    - Patrón de unión de las columnas
	 * @param defaultValue    - Valor por defecto si la columna no contiene ningún
	 *                        valor.
	 * @return String con la cadena formateada
	 */
	public static String joinColumns(Map<String, Object> record, Integer[] targetPositions, String joinTemplate,
			String defaultValue) {
		if (record == null)
			return joinTemplate;
		StringBuilder result = new StringBuilder(joinTemplate);
		for (int i = 0; i < targetPositions.length; i++) {
			String key = toKey(targetPositions[i]);
			Object val = record.getOrDefault(key, defaultValue);

			String placeholder = "$" + (i + 1);
			String replacement = val != null ? val.toString() : "";

			int index = result.indexOf(placeholder);
			while (index != -1) {
				result.replace(index, index + placeholder.length(), replacement);
				index = result.indexOf(placeholder, index + replacement.length());
			}
		}
		return result.toString();
	}

	/**
	 * Copia en el destino únicamente las columnas seleccionadas. Vacía los campos
	 * omitidos.
	 * 
	 * @param record           - Registro con los valores a buscar
	 * @param writer           - Buffer de salida
	 * @param includePositions - Listado de las posiciones que se incluiran
	 * @param campoLongitudes  - Longitudes que delimitan las posiciones del
	 *                         registro
	 * @throws IOException
	 */
	public static void copyIncludingColumns(Map<String, Object> record, BufferedWriter writer,
			Integer[] includePositions, List<Integer> campoLongitudes) throws IOException {
		if (record == null)
			return;
		Map<String, Object> filteredRecord = new HashMap<>(campoLongitudes.size());

		// Inicializamos las posiciones deseadas, dejando las demás vacías
		java.util.List<Integer> includeList = java.util.Arrays.asList(includePositions);
		for (int i = 0; i < campoLongitudes.size(); i++) {
			String key = toKey(i);
			if (includeList.contains(i)) {
				filteredRecord.put(key, record.getOrDefault(key, ""));
			} else {
				filteredRecord.put(key, ""); // El formateador lo rellenará con espacios válidos
			}
		}
		writer.write(formatToFixedLength(filteredRecord, campoLongitudes));
		writer.newLine();
	}

	/**
	 * Copia la fila excluyendo las posiciones indicadas. Vacía los campos
	 * eliminados.
	 * 
	 * @param record           - Registro con los valores a buscar
	 * @param writer           - Buffer de salida
	 * @param excludePositions - Listado de las posiciones que se excluyen
	 * @param campoLongitudes  - Longitudes que delimitan las posiciones del
	 *                         registro
	 * @throws IOException
	 */
	public static void copyExcludingColumns(Map<String, Object> record, BufferedWriter writer,
			Set<Integer> excludePositions, List<Integer> campoLongitudes) throws IOException {
		if (record == null)
			return;
		Map<String, Object> filteredRecord = new HashMap<>(campoLongitudes.size());

		for (int i = 0; i < campoLongitudes.size(); i++) {
			String key = toKey(i);
			if (!excludePositions.contains(i)) {
				filteredRecord.put(key, record.getOrDefault(key, ""));
			} else {
				filteredRecord.put(key, ""); // Vacío para simular el borrado físico de la columna
			}
		}
		writer.write(formatToFixedLength(filteredRecord, campoLongitudes));
		writer.newLine();
	}
}
