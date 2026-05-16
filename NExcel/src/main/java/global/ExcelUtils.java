package global;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Utilidades para la manipulación y copia de datos en libros de Excel. Incluye
 * funciones para copiar filas, celdas, rangos y realizar búsquedas de texto.
 */
public class ExcelUtils {

	/**
	 * Aplica el estilo de una celda de origen a una de destino reutilizando el mapa
	 * de estilos. Esto evita superar el límite máximo de estilos permitidos por
	 * Excel.
	 * 
	 * @param oldCell      Celda de origen con el estilo original.
	 * @param newCell      Celda de destino donde se aplicará el estilo.
	 * @param destWorkbook Libro de destino para crear nuevos estilos si es
	 *                     necesario.
	 * @param styleMap     Mapa de correspondencia entre índices de estilo antiguos
	 *                     y nuevos.
	 */
	private static void applyStyle(Cell oldCell, Cell newCell, Workbook destWorkbook,
			Map<Integer, CellStyle> styleMap) {
		int styleIdx = oldCell.getCellStyle().getIndex();
		if (!styleMap.containsKey(styleIdx)) {
			CellStyle newStyle = destWorkbook.createCellStyle();
			newStyle.cloneStyleFrom(oldCell.getCellStyle());
			styleMap.put(styleIdx, newStyle);
		}
		newCell.setCellStyle(styleMap.get(styleIdx));
	}

	/**
	 * Copia una fila completa de un libro a otro, permitiendo transformaciones de
	 * reemplazo.
	 * 
	 * @param row          Objeto de configuración que contiene la fila de entrada,
	 *                     salida y parámetros de búsqueda.
	 * @param destWorkbook Libro de destino.
	 * @param styleMap     Mapa para la gestión eficiente de estilos.
	 */
	public static void copyRow(ExcelUtilRowMode row, Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {
		if (row.rowInput == null) {
			return;
		}

		for (int j = 0; j < row.rowInput.getLastCellNum(); j++) {
			Cell oldCell = row.rowInput.getCell(j);
			if (oldCell != null) {
				Cell newCell = row.rowOutput.createCell(j);

				// Gestión de estilos estática
				ExcelUtils.applyStyle(oldCell, newCell, destWorkbook, styleMap);
				switch (row.mode) {
				case 2:
					copyCellValueAndReplace(oldCell, newCell, row.valueSearch, row.valueReplace);
					break;

				default:
					copyCellValue(oldCell, newCell);
					break;
				}

			}
		}
	}

	private static boolean matchesMode(String cellValue, String searchValue, int modo) {
	    if (cellValue == null || searchValue == null) return false;
	    
	    String cellLower = cellValue.toLowerCase();
	    String searchLower = searchValue.toLowerCase();
	    
	    switch (modo) {
	        case 0: // Palabra completa
	            return cellLower.equals(searchLower);
	        case 1: // Contiene el texto en cualquier posición
	            return cellLower.contains(searchLower);
	        case 2: // La columna empieza por
	            return cellLower.startsWith(searchLower);
	        case 3: // La columna finaliza por
	            return cellLower.endsWith(searchLower);
	        default:
	            throw new IllegalArgumentException("Modo de búsqueda no válido: " + modo);
	    }
	}

	/**
	 * Comprueba si una celda específica en una fila coincide con un valor de texto
	 * según el modo seleccionado.
	 * 
	 * @param rowInput       Fila a evaluar.
	 * @param columnPosition Índice de la columna.
	 * @param valueStr       Valor a buscar.
	 * @param modo           Modo de búsqueda (0: Completo, 1: Contiene, 2: Empieza, 3: Termina).
	 * @return true si el valor coincide.
	 */
	public static boolean columnContains(Row rowInput, Integer columnPosition, String valueStr, int modo) {
		Cell cell = rowInput.getCell(columnPosition);
		DataFormatter dataFormatter = new DataFormatter();
		String cellValue = dataFormatter.formatCellValue(cell);

		return matchesMode(cellValue, valueStr, modo);
	}
	/**
	 * Comprueba si una celda específica coincide con cualquiera de los valores
	 * proporcionados en un array según el modo seleccionado.
	 * 
	 * @param rowInput       Fila a evaluar.
	 * @param columnPosition Índice de la columna.
	 * @param valuesStr      Array de posibles valores coincidentes.
	 * @param modo           Modo de búsqueda (0: Completo, 1: Contiene, 2: Empieza, 3: Termina).
	 * @return true si coincide con alguno de los valores.
	 */
	public static boolean columnContains(Row rowInput, Integer columnPosition, String[] valuesStr, int modo) {
		Cell cell = rowInput.getCell(columnPosition);
		DataFormatter dataFormatter = new DataFormatter();
		String cellValue = dataFormatter.formatCellValue(cell);

		return Arrays.stream(valuesStr).anyMatch(val -> matchesMode(cellValue, val, modo));
	}

	/**
	 * Busca varios posibles valores en múltiples posiciones de columna según el modo seleccionado.
	 * 
	 * @param rowInput        Fila a evaluar.
	 * @param columnPositions Array con los índices de las columnas a revisar.
	 * @param valueStr        Array con los valores a buscar.
	 * @param modo            Modo de búsqueda (0: Completo, 1: Contiene, 2: Empieza, 3: Termina).
	 * @return true si se encuentra alguna coincidencia en cualquiera de las
	 *         columnas especificadas.
	 */
	public static boolean columnContains(Row rowInput, Integer[] columnPositions, String[] valueStr, int modo) {
		boolean exist = false;
		DataFormatter dataFormatter = new DataFormatter();
		
		for (int i = 0; i < columnPositions.length; i++) {
			Cell cell = rowInput.getCell(columnPositions[i]);
			String cellValue = dataFormatter.formatCellValue(cell);
			
			exist = Arrays.stream(valueStr).anyMatch(val -> matchesMode(cellValue, val, modo));
			if (exist) {
				break; // Detener el bucle si ya encontramos una coincidencia
			}
		}
		return exist;
	}

	/**
	 * Busca un valor de texto en columnas específicas de una fila según el modo seleccionado.
	 * 
	 * @param rowInput  Fila a evaluar.
	 * @param valueStr  Valor a buscar.
	 * @param positions Array con los índices de las columnas a revisar.
	 * @param modo      Modo de búsqueda (0: Completo, 1: Contiene, 2: Empieza, 3: Termina).
	 */
	public static boolean rowContains(Row rowInput, String valueStr, Integer[] positions, int modo) {
		for (int i = 0; i < positions.length; i++) {
			if (ExcelUtils.columnContains(rowInput, positions[i], valueStr, modo)) {
				return true; // Retorna true inmediatamente al encontrar coincidencia
			}
		}
		return false;
	}
	
	/**
	 * Comprueba si una celda específica en una fila contiene un valor de texto
	 * exacto (case-insensitive).
	 * 
	 * @param rowInput       Fila a evaluar.
	 * @param columnPosition Índice de la columna.
	 * @param valueStr       Valor a buscar.
	 * @return true si el valor coincide.
	 */
	public static boolean columnContains(Row rowInput, Integer columnPosition, String valueStr) {
		Cell cell = rowInput.getCell(columnPosition);
		DataFormatter dataFormatter = new DataFormatter();
		// 2. Obtener el valor de la celda siempre como String
		String cellValue = dataFormatter.formatCellValue(cell);

		// 3. Comparar usando .equalsIgnoreCase()
		if (valueStr.equalsIgnoreCase(cellValue)) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * Verifica si una fila contiene una lista de valores en posiciones específicas
	 * o en toda la fila según el modo seleccionado.
	 * 
	 * @param rowInput  Fila a evaluar.
	 * @param valueStrs Valores a buscar.
	 * @param positions Posiciones específicas (si es null o vacío, busca en toda la fila).
	 * @param modo      Modo de búsqueda (0: Completo, 1: Contiene, 2: Empieza, 3: Termina).
	 * @return true si encuentra coincidencias.
	 */
	public static boolean rowContainsListValues(Row rowInput, String[] valueStrs, Integer[] positions, int modo) {
		// Caso A: Buscar en posiciones específicas
		if (positions != null && positions.length > 0) {
			for (int i = 0; i < positions.length; i++) {
				if (ExcelUtils.columnContains(rowInput, positions[i], valueStrs, modo)) {
					return true;
				}
			}
		} 
		// Caso B: Buscar en toda la fila (positions es null o vacío)
		else {
			// Creamos un array con los índices de todas las celdas que contienen datos en la fila
			Integer[] allPositions = new Integer[(int) rowInput.getLastCellNum()];
			for (int i = 0; i < allPositions.length; i++) {
				allPositions[i] = i;
			}
			
			// Buscamos cada valor de la lista en todas las posiciones de la fila
			for (int i = 0; i < valueStrs.length; i++) {
				if (ExcelUtils.rowContains(rowInput, valueStrs[i], allPositions, modo)) {
					return true;
				}
			}
		}
		return false;
	}	
	/**
	 * Comprueba si una celda específica contiene cualquiera de los valores
	 * proporcionados en un array.
	 * 
	 * @param rowInput       Fila a evaluar.
	 * @param columnPosition Índice de la columna.
	 * @param valuesStr      Array de posibles valores coincidentes.
	 * @return true si coincide con alguno de los valores.
	 */
	@Deprecated
	public static boolean columnContains(Row rowInput, Integer columnPosition, String[] valuesStr) {

		Cell cell = rowInput.getCell(columnPosition);
		DataFormatter dataFormatter = new DataFormatter();
		// Obtener el valor de la celda siempre como String
		String cellValue = dataFormatter.formatCellValue(cell);
		// Comparar usando .equalsIgnoreCase()
		return Arrays.stream(valuesStr).anyMatch(cellValue::equalsIgnoreCase);

	}

	/**
	 * Busca varios posibles valores en múltiples posiciones de columna.
	 * 
	 * @param rowInput        Fila a evaluar.
	 * @param columnPositions Array con los índices de las columnas a revisar.
	 * @param valueStr        Array con los valores a buscar.
	 * @return true si se encuentra alguna coincidencia en cualquiera de las
	 *         columnas especificadas.
	 */
	@Deprecated
	public static boolean columnContains(Row rowInput, Integer[] columnPositions, String[] valueStr) {

		boolean exist = false;
		DataFormatter dataFormatter = new DataFormatter();
		for (int i = 0; i < columnPositions.length && exist == false; i++) {
			Cell cell = rowInput.getCell(columnPositions[i]);
			// Obtener el valor de la celda siempre como String
			String cellValue = dataFormatter.formatCellValue(cell);
			// Comparar usando .equalsIgnoreCase()
			exist = Arrays.stream(valueStr).anyMatch(cellValue::equalsIgnoreCase);
		}
		return exist;

	}

	/**
	 * Busca un valor de texto en columnas específicas de una fila.
	 */
	@Deprecated
	public static boolean rowContains(Row rowInput, String valueStr, Integer[] positions) {
		boolean exist = false;
		for (int i = 0; i < positions.length && exist == false; i++) {
			exist = ExcelUtils.columnContains(rowInput, positions[i], valueStr);
		}
		return exist;
	}

	/**
	 * Verifica si una fila contiene una lista de valores en posiciones específicas
	 * o en toda la fila.
	 * 
	 * @param rowInput  Fila a evaluar.
	 * @param valueStrs Valores a buscar.
	 * @param positions Posiciones específicas (si es null o vacío, busca en toda la
	 *                  fila).
	 * @return true si encuentra coincidencias.
	 */
	@Deprecated
	public static boolean rowContainsListValues(Row rowInput, String[] valueStrs, Integer[] positions) {
		boolean exist = false;
		if (positions != null && positions.length > 0) {
			for (int i = 0; i < positions.length && exist == false; i++) {
				exist = ExcelUtils.columnContains(rowInput, positions[i], valueStrs);
			}
		} else {
			for (int i = 0; i < valueStrs.length && exist == false; i++) {
				exist = ExcelUtils.rowContains(rowInput, valueStrs[i]);
			}
		}
		return exist;
	}

	/**
	 * Comprueba si un valor de texto existe en cualquier celda de la fila.
	 * 
	 * @param rowInput Fila donde buscar.
	 * @param valueStr Valor buscado.
	 * @return true si aparece en alguna columna.
	 */
	@Deprecated
	public static boolean rowContains(Row rowInput, String valueStr) {
		boolean exist = false;
		for (int i = 0; i < rowInput.getLastCellNum() && exist == false; i++) {
			exist = ExcelUtils.columnContains(rowInput, i, valueStr);
		}
		return exist;
	}

	/**
	 * Copia el valor de una celda respetando su tipo de dato original (String,
	 * Numérico, Fecha, etc.).
	 * 
	 * @param cInput  Celda origen.
	 * @param cOutput Celda destino.
	 */
	
	public static void copyCellValue(Cell cInput, Cell cOutput) {
		switch (cInput.getCellType()) {
		case STRING:
			cOutput.setCellValue(cInput.getStringCellValue());
			break;
		case NUMERIC:
			// Verificamos si el número representa una fecha
			if (DateUtil.isCellDateFormatted(cInput)) {
				cOutput.setCellValue(cInput.getDateCellValue());
			} else {
				cOutput.setCellValue(cInput.getNumericCellValue());
			}
			break;
		case BOOLEAN:
			cOutput.setCellValue(cInput.getBooleanCellValue());
			break;
		case FORMULA:
			cOutput.setCellFormula(cInput.getCellFormula());
			break;
		case BLANK:
			cOutput.setBlank();
			break;
		case ERROR:
			cOutput.setCellErrorValue(cInput.getErrorCellValue());
			break;
		default:
			break;
		}
	}

	/**
	 * Copia el valor de una celda permitiendo realizar un reemplazo mediante
	 * expresiones regulares si es texto.
	 * 
	 * @param cInput   Celda origen.
	 * @param cOutput  Celda destino.
	 * @param oldValue Valor o Regex a buscar.
	 * @param newValue Valor de reemplazo.
	 */
	public static void copyCellValueAndReplace(Cell cInput, Cell cOutput, String oldValue, String newValue) {
		if (cInput == null) {
			cOutput.setBlank();
			return;
		}
		switch (cInput.getCellType()) {
		case STRING:
			String text = cInput.getStringCellValue();
			// Mejora 2: Aplicar transformación si hay regex, si no, limpiar espacios extra
			if (oldValue != null && !oldValue.isEmpty()) {
				cOutput.setCellValue(text.replaceAll(oldValue, newValue));
			} else {
				cOutput.setCellValue(text.trim());
			}
			break;

		case NUMERIC:
			if (DateUtil.isCellDateFormatted(cInput)) {
				processDateCellForReplace(cInput, cOutput, oldValue, newValue);
			} else {
				cOutput.setCellValue(cInput.getNumericCellValue());
			}
			break;

		case FORMULA:
			// Mejora 3: Copiamos la fórmula, pero Excel la recalculará al abrir
			cOutput.setCellFormula(cInput.getCellFormula());
			break;

		case BOOLEAN:
			cOutput.setCellValue(cInput.getBooleanCellValue());
			break;

		case BLANK:
			cOutput.setBlank();
			break;

		case ERROR:
			cOutput.setCellErrorValue(cInput.getErrorCellValue());
			break;

		default:
			cOutput.setCellValue(cInput.toString());
			break;
		}
	}

	/**
	 * 
	 * Lógica interna para manejar fechas durante un reemplazo. Si hay una cadena de
	 * búsqueda, convierte la fecha a formato yyyy-MM-dd para aplicar el reemplazo.
	 * 
	 * @param cInput   Celda de entrada
	 * @param cOutput  Celda de salida
	 * @param oldValue Valor de entrada
	 * @param newValue Valor a sustuir
	 */
	private static void processDateCellForReplace(Cell cInput, Cell cOutput, String oldValue, String newValue) {
		java.util.Date date = cInput.getDateCellValue();
		if (oldValue != null && !oldValue.isEmpty()) {
			// Si hay regex, convertimos a texto para transformar
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			String formatted = sdf.format(date).replaceAll(oldValue, newValue);
			cOutput.setCellValue(formatted);
		} else {
			// Si no hay regex, mantenemos el valor como Fecha real de Excel
			cOutput.setCellValue(date);
		}
	}

	/**
	 * Determina si una fila está completamente vacía (sin celdas creadas o celdas
	 * de tipo BLANK).
	 * 
	 * @param row Fila a validar.
	 * @return {@code true} si la fila está vacia {@code false} si la fila contiene
	 *         algún valor
	 */
	public static boolean isRowEmpty(Row row) {
		if (row == null) {
			return true;
		}
		for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
			Cell cell = row.getCell(c);
			if (cell != null && cell.getCellType() != CellType.BLANK) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Copia un rango completo de celdas de una hoja a otra con desplazamientos
	 * opcionales.
	 * 
	 * @param sourceSheet  Hoja de origen.
	 * @param destSheet    Hoja de destino.
	 * @param range        Coordenadas del rango a copiar.
	 * @param rowOffset    Desplazamiento de filas en el destino.
	 * @param colOffset    Desplazamiento de columnas en el destino.
	 * @param destWorkbook Libro de destino.
	 * @param styleMap     Mapa para la gestión de estilos.
	 */
	public static void copyRange(Sheet sourceSheet, Sheet destSheet, CellRangeAddress range, int rowOffset,
			int colOffset, Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {

		for (int i = range.getFirstRow(); i <= range.getLastRow(); i++) {
			Row rowInput = sourceSheet.getRow(i);
			if (rowInput == null) {
				continue;
			}

			// Calculamos la fila de destino sumando el desplazamiento
			int targetRowIdx = i + rowOffset;
			Row rowOutput = destSheet.getRow(targetRowIdx);
			if (rowOutput == null) {
				rowOutput = destSheet.createRow(targetRowIdx);
			}

			for (int j = range.getFirstColumn(); j <= range.getLastColumn(); j++) {
				Cell oldCell = rowInput.getCell(j);
				if (oldCell != null) {
					// Calculamos la columna de destino sumando el desplazamiento
					int targetColIdx = j + colOffset;
					Cell newCell = rowOutput.createCell(targetColIdx);

					// Gestión de Estilos
					int styleIdx = oldCell.getCellStyle().getIndex();
					if (!styleMap.containsKey(styleIdx)) {
						CellStyle newStyle = destWorkbook.createCellStyle();
						newStyle.cloneStyleFrom(oldCell.getCellStyle());
						styleMap.put(styleIdx, newStyle);
					}
					newCell.setCellStyle(styleMap.get(styleIdx));

					// Copia el valor 
					copyCellValue(oldCell, newCell);
				}
			}
		}

		// Copiar celdas combinadas dentro del rango
		copyMergedRegions(sourceSheet, destSheet, range, rowOffset, colOffset);
	}

	/**
	 * Copia las regiones combinadas (celdas unidas) de una hoja a otra dentro de un
	 * rango específico.
	 * 
	 * @param sourceSheet Hoja de origen.
	 * @param destSheet   Hoja de destino.
	 * @param range       Rango de celdas que se está procesando.
	 * @param rowOff      Desplazamiento de filas en el destino.
	 * @param colOff      Desplazamiento de columnas en el destino.
	 */
	private static void copyMergedRegions(Sheet sourceSheet, Sheet destSheet, CellRangeAddress range, int rowOff,
			int colOff) {
		for (int i = 0; i < sourceSheet.getNumMergedRegions(); i++) {
			CellRangeAddress region = sourceSheet.getMergedRegion(i);
			// Si la región combinada está dentro del rango que estamos copiando
			if (range.isInRange(region.getFirstRow(), region.getFirstColumn())) {
				CellRangeAddress newRegion = new CellRangeAddress(region.getFirstRow() + rowOff,
						region.getLastRow() + rowOff, region.getFirstColumn() + colOff,
						region.getLastColumn() + colOff);
				destSheet.addMergedRegion(newRegion);
			}
		}
	}

	/**
	 * Rellena una fila de destino utilizando solo las columnas especificadas en el
	 * array de posiciones.
	 * 
	 * @param row          Objeto con la configuración de las filas de entrada y
	 *                     salida.
	 * @param positions    Índices de las columnas de origen que se desean copiar.
	 * @param destWorkbook Libro de destino para la gestión de estilos.
	 * @param styleMap     Mapa para reutilizar estilos existentes.
	 */
	public static void fillRowWithPositions(ExcelUtilRowMode row, Integer[] positions, Workbook destWorkbook,
			Map<Integer, CellStyle> styleMap) {
		if (row.rowInput == null || row.rowOutput == null || positions == null)
			return;

		for (int i = 0; i < positions.length; i++) {
			Cell oldCell = row.rowInput.getCell(positions[i]);
			if (oldCell != null) {
				Cell newCell = row.rowOutput.createCell(i);
				// Gestión de estilos idéntica a copyRow
				ExcelUtils.copyCell(oldCell, newCell, destWorkbook, styleMap);

			}
		}
	}

	/**
	 * Rellena una fila de destino utilizando solo las columnas especificadas en el
	 * array de posiciones.
	 * 
	 * @param row          Objeto con la configuración de las filas de entrada y
	 *                     salida.
	 * @param positions    Índices de las columnas de origen que se desean copiar.
	 * @param destWorkbook Libro de destino para la gestión de estilos.
	 * @param styleMap     Mapa para reutilizar estilos existentes.
	 */
	public static void fillRowExcludingPositions(ExcelUtilRowMode row, Integer[] excludePositions,
			Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {
		if (row.rowInput == null || row.rowOutput == null)
			return;

		List<Integer> excludedList = (excludePositions != null) ? Arrays.asList(excludePositions) : new ArrayList<>();
		int targetCol = 0;

		for (int j = 0; j < row.rowInput.getLastCellNum(); j++) {
			if (!excludedList.contains(j)) {
				Cell oldCell = row.rowInput.getCell(j);
				if (oldCell != null) {
					Cell newCell = row.rowOutput.createCell(targetCol);
					ExcelUtils.copyCell(oldCell, newCell, destWorkbook, styleMap);
				}
				targetCol++;
			}
		}
	}

	/**
	 * Método auxiliar para copiar una celda completa (Estilo + Valor).
	 * 
	 * @param oldCell Celda de origen.
	 * @param newCell Celda de destino.
	 * @param wb      Libro de destino.
	 * @param styles  Mapa de estilos.
	 */
	private static void copyCell(Cell oldCell, Cell newCell, Workbook wb, Map<Integer, CellStyle> styles) {
		ExcelUtils.applyStyle(oldCell, newCell, wb, styles);
		ExcelUtils.copyCellValue(oldCell, newCell);
	}

	/**
	 * Divide el contenido de columnas específicas basándose en una expresión
	 * regular (Regex). Genera nuevas columnas adicionales en la fila de destino por
	 * cada fragmento encontrado.
	 * 
	 * @param row             Objeto con la configuración de las filas.
	 * @param targetPositions Columnas sobre las que se aplicará el split.
	 * @param regex           Expresión regular para realizar la división.
	 * @param keepOriginal    Si es true, mantiene la columna original antes de los
	 *                        fragmentos divididos.
	 * @param destWorkbook    Libro de destino.
	 * @param styleMap        Mapa de estilos.
	 */
	public static void splitColumnsByRegex(ExcelUtilRowMode row, Integer[] targetPositions, String regex,
			boolean keepOriginal, Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {

		if (row.rowInput == null || row.rowOutput == null)
			return;

		List<Integer> positionsList = (targetPositions != null) ? Arrays.asList(targetPositions) : new ArrayList<>();
		int targetCol = 0;

		for (int j = 0; j < row.rowInput.getLastCellNum(); j++) {
			Cell oldCell = row.rowInput.getCell(j);

			// Si la columna actual debe ser dividida
			if (positionsList.contains(j) && oldCell != null) {
				String cellValue = oldCell.toString(); // O usa ExcelUtils.getCellValueAsString si lo tienes
				String[] parts = cellValue.split(regex);

				// Si decidimos mantener la original, la copiamos primero
				if (keepOriginal) {
					ExcelUtils.copyCell(oldCell, row.rowOutput.createCell(targetCol++), destWorkbook, styleMap);
				}

				// Creamos una nueva columna por cada parte resultante del split
				for (String part : parts) {
					Cell newCell = row.rowOutput.createCell(targetCol++);
					ExcelUtils.applyStyle(oldCell, newCell, destWorkbook, styleMap);
					newCell.setCellValue(part.trim());
				}
			} else if (oldCell != null) {
				// Columna normal (no se divide)
				ExcelUtils.copyCell(oldCell, row.rowOutput.createCell(targetCol++), destWorkbook, styleMap);
			}
		}
	}

	/**
	 * Obtiene el valor de cualquier celda representado como una cadena de texto.
	 * Gestiona formatos de fecha predefinidos (yyyy/MM/dd) para facilitar procesos
	 * de Regex.
	 * 
	 * @param cell Celda de la que extraer el valor.
	 * @return Representación en String del contenido de la celda.
	 */
	public static String getCellValueAsString(Cell cell) {
		if (cell == null)
			return "";
		switch (cell.getCellType()) {
		case STRING:
			return cell.getStringCellValue();
		case NUMERIC:
			if (DateUtil.isCellDateFormatted(cell)) {
				// Usamos un formato predecible para el regex
				return new SimpleDateFormat("yyyy/MM/dd").format(cell.getDateCellValue());
			}
			return String.valueOf(cell.getNumericCellValue());
		case BOOLEAN:
			return String.valueOf(cell.getBooleanCellValue());
		case FORMULA:
			return cell.getCellFormula();
		default:
			return "";
		}
	}

	/**
	 * Divide columnas conforme a una expresión regular
	 * 
	 * 
	 * @param row             Objeto de configuración que contiene la fila de
	 *                        entrada, salida y parámetros de búsqueda.
	 * @param targetPositions Listado de posiciones (índices) en los que se evaluará
	 *                        las posiciones
	 * @param pattern         Patrón a buscar
	 * @param groupTemplates  Plantillas generadas según el patrón
	 * @param keepOriginal    Si se conserva el valor original
	 * @param defaultValue    Valor por defecto de sustitución, si no se encuentra
	 *                        uno del patrón
	 * @param destWorkbook    Libro de destino.
	 * @param styleMap        Mapa para la gestión eficiente de estilos.
	 */

	public static void splitColumnsByRegexGroups(ExcelUtilRowMode row, Integer[] targetPositions,
			java.util.regex.Pattern pattern, String[] groupTemplates, boolean keepOriginal, String defaultValue,
			Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {

		if (row.rowInput == null || row.rowOutput == null)
			return;

		List<Integer> positionsList = (targetPositions != null) ? Arrays.asList(targetPositions) : new ArrayList<>();

		int targetCol = 0;

		for (int j = 0; j < row.rowInput.getLastCellNum(); j++) {
			Cell oldCell = row.rowInput.getCell(j);

			if (positionsList.contains(j) && oldCell != null) {
				String cellValue = getCellValueAsString(oldCell);
				java.util.regex.Matcher matcher = pattern.matcher(cellValue);

				if (keepOriginal) {
					copyCellValue(oldCell, row.rowOutput.createCell(targetCol++));
				}

				boolean matches = matcher.find();
				for (String template : groupTemplates) {
					Cell newCell = row.rowOutput.createCell(targetCol++);
					ExcelUtils.applyStyle(oldCell, newCell, destWorkbook, styleMap);

					if (matches) {
						try {
							newCell.setCellValue(matcher.replaceFirst(template));
						} catch (Exception e) {
							newCell.setCellValue("ERR_GROUP");
						}
					} else {
						newCell.setCellValue(defaultValue != null ? defaultValue : "");
					}
				}
			} else if (oldCell != null) {
				copyCellValue(oldCell, row.rowOutput.createCell(targetCol++));
			}
		}
	}

	/**
	 * Une múltiples columnas en una sola utilizando una plantilla personalizada.
	 * Permite inyectar los valores de las columnas fuente en posiciones específicas
	 * de un texto usando marcadores de posición (ej: "$1 - $2").
	 * 
	 * @param row             Objeto de configuración que contiene la fila de
	 *                        entrada, salida y parámetros de búsqueda.
	 * @param targetPositions Listado de posiciones (índices) en los que se evaluará
	 *                        las posiciones
	 * @param joinTemplate    Patrón de unión, cadena con marcadores ($1, $2, etc.)
	 *                        que serán reemplazados por los valores de las
	 *                        columnas.
	 * @param keepOriginal    Si se conserva el valor original
	 * @param defaultValue    Valor por defecto de sustitución, si no se encuentra
	 *                        uno del patrón
	 * @param destWorkbook    Libro de destino.
	 * @param styleMap        Mapa para la gestión eficiente de estilos.
	 * 
	 * 
	 */

	public static void joinColumnsByRegex(ExcelUtilRowMode row, Integer[] targetPositions, String joinTemplate,
			boolean keepOriginal, String defaultValue, Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {

		if (row.rowInput == null || row.rowOutput == null)
			return;

		List<Integer> positionsList = (targetPositions != null) ? Arrays.asList(targetPositions) : new ArrayList<>();
		int targetCol = 0;

		// 1. Recolectamos los valores de las columnas indicadas en targetPositions
		String resultValue = joinTemplate;

		for (int i = 0; i < positionsList.size(); i++) {
			int pos = positionsList.get(i);
			Cell cell = row.rowInput.getCell(pos);
			String val = (cell != null) ? getCellValueAsString(cell) : (defaultValue != null ? defaultValue : "");

			// Reemplazamos $1 con el valor de la primera posición, $2 con la segunda, etc.
			String placeholder = "$" + (i + 1);
			resultValue = resultValue.replace(placeholder, val);
		}

		// 2. Generamos el output recorriendo todas las columnas
		for (int j = 0; j < row.rowInput.getLastCellNum(); j++) {
			Cell oldCell = row.rowInput.getCell(j);

			if (positionsList.contains(j)) {
				// Si es una de las columnas "fuente"
				if (keepOriginal) {
					copyCellValue(oldCell, row.rowOutput.createCell(targetCol++));
				}

				// Solo insertamos la columna unida
				if (j == positionsList.get(positionsList.size() - 1)) {
					Cell newCell = row.rowOutput.createCell(targetCol++);
					ExcelUtils.applyStyle(oldCell, newCell, destWorkbook, styleMap);
					newCell.setCellValue(resultValue);
				}
			} else if (oldCell != null) {
				// Columnas que no participan en la unión
				copyCellValue(oldCell, row.rowOutput.createCell(targetCol++));
			}
		}
	}

	/**
	 * Fusiona el contenido de varias columnas en una sola celda separada por un
	 * delimitador. A diferencia de join, este método concatena todos los valores
	 * secuencialmente.
	 * 
	 * @param row              Objeto con la configuración de filas.
	 * @param positionsToMerge Índices de las columnas que se van a concatenar.
	 * @param targetPosition   Posición específica en el destino para la celda
	 *                         fusionada (si es null, va al final).
	 * @param separator        Cadena de texto que separará cada valor (ej: ", ", "
	 *                         - ").
	 * @param keepOriginal     Si es true, conserva las columnas originales en el
	 *                         destino.
	 * @param destWorkbook     Libro de destino.
	 * @param styleMap         Mapa de estilos.
	 */
	public static void mergeColumns(ExcelUtilRowMode row, Integer[] positionsToMerge, Integer targetPosition,
			String separator, boolean keepOriginal, Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {

		if (row.rowInput == null || row.rowOutput == null)
			return;

		List<Integer> mergeList = (positionsToMerge != null) ? Arrays.asList(positionsToMerge) : new ArrayList<>();
		StringBuilder mergedValue = new StringBuilder();
		int lastInputCol = row.rowInput.getLastCellNum();

		// 1. Decidir la posición de destino de la celda fusionada
		// Si es null, se irá al final del flujo de columnas
		int finalMergePos = (targetPosition != null) ? targetPosition : -1;

		int targetCol = 0;
		Cell firstMergeCell = null; // Para copiar el estilo de la primera celda del merge

		// 2. Iterar para construir el valor fusionado y copiar las no fusionadas
		for (int j = 0; j < lastInputCol; j++) {
			Cell oldCell = row.rowInput.getCell(j);
			if (oldCell == null)
				continue;

			if (mergeList.contains(j)) {
				// Guardamos el estilo de la primera celda que encontremos para la fusión
				if (firstMergeCell == null)
					firstMergeCell = oldCell;

				// Concatenar valor
				String val = oldCell.toString(); 
				if (mergedValue.length() > 0)
				{					
					mergedValue.append(separator);
				}
				mergedValue.append(val);

				// Si queremos mantener la original, la copiamos al destino
				if (keepOriginal) {
					copyCell(oldCell, row.rowOutput.createCell(targetCol++), destWorkbook, styleMap);
				}
			} else {
				// Es una celda que no entra en la fusión
				copyCell(oldCell, row.rowOutput.createCell(targetCol++), destWorkbook, styleMap);
			}
		}

		// 3. Insertar la celda fusionada
		if (mergedValue.length() > 0) {
			int outputPos = (finalMergePos == -1) ? targetCol : finalMergePos;
			Cell mergedCell = row.rowOutput.createCell(outputPos);

			if (firstMergeCell != null) {
				ExcelUtils.applyStyle(firstMergeCell, mergedCell, destWorkbook, styleMap);
			}
			mergedCell.setCellValue(mergedValue.toString());
		}
	}

}
