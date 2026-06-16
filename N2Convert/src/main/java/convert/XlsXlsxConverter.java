package convert;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook; // <-- ESCRITOR EN STREAMING
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase para la conversión de ficheros XSL (formato antiguo de Excel) a XLSX
 * (formato moderno)
 * 
 * 
 * @version 1.0
 */
public class XlsXlsxConverter {

	/**
	 * Convierte un libro antiguo de Excel (.xls) al formato moderno (.xlsx) en
	 * streaming.
	 * 
	 * @param xlsPath - Ruta del fichero XLS origen
	 * @param xlsxPath - Ruta del fichero XLSX destino, fichero convertido
	 * @throws IOException
	 */
	public void xlsToXlsx(String xlsPath, String xlsxPath) throws IOException {
		// Validación previa de extensiones
		if (!xlsPath.toLowerCase().endsWith(".xls") || !xlsxPath.toLowerCase().endsWith(".xlsx")) {
			throw new IllegalArgumentException(
					"Las extensiones deben ser estrictamente .xls para la entrada y .xlsx para la salida.");
		}

		try (InputStream fileIn = new FileInputStream(xlsPath);
				Workbook oldWorkbook = new HSSFWorkbook(fileIn); // El límite de .xls es de 65k filas, pero se procesará en memoria de forma optimizada
				// Con SXSSFWorkbook se escribe el .xlsx volcando al disco cada 100 filas
				SXSSFWorkbook newWorkbook = new SXSSFWorkbook(100)) {

			newWorkbook.setCompressTempFiles(true);

			// Mapa para gestionar eficientemente los estilos de celda (Clave: Índice
			// antiguo, Valor: Estilo nuevo)
			Map<Integer, CellStyle> styleMap = new HashMap<>();

			// Iterar por todas las pestañas/hojas del libro antiguo
			for (int i = 0; i < oldWorkbook.getNumberOfSheets(); i++) {
				Sheet oldSheet = oldWorkbook.getSheetAt(i);
				Sheet newSheet = newWorkbook.createSheet(oldSheet.getSheetName());

				// Usamos autoSizeColumn al final de la pestaña basándonos en la
				// primera fila,
				// habilitamos el rastreo de columnas en la hoja de streaming antes de
				// que salgan de la RAM.
				boolean columnsTracked = false;

				// Copiar filas y celdas utilizando el iterador limpio
				for (Row oldRow : oldSheet) {
					Row newRow = newSheet.createRow(oldRow.getRowNum());

					// Mantener la altura de la fila original
					newRow.setHeight(oldRow.getHeight());

					for (Cell oldCell : oldRow) {
						Cell newCell = newRow.createCell(oldCell.getColumnIndex());

						// Copiar y clonar el estilo de forma segura entre el libro viejo y el nuevo
						// de streaming
						int oldStyleIdx = oldCell.getCellStyle().getIndex();
						if (!styleMap.containsKey(oldStyleIdx)) {
							CellStyle newStyle = newWorkbook.createCellStyle();
							try {
								newStyle.cloneStyleFrom(oldCell.getCellStyle());
							} catch (IllegalArgumentException e) {
								// Mitigación para fuentes o paletas de colores incompatibles entre formatos
								// XLS/XLSX masivos
								// Conserva el formato básico si el clonado estricto de POI genera conflicto de
								// índices
							}
							styleMap.put(oldStyleIdx, newStyle);
						}
						newCell.setCellStyle(styleMap.get(oldStyleIdx));

						// Copiar el valor evaluando el tipo de celda original (Sintaxis compatible
						// universal)
						switch (oldCell.getCellType()) {
						case STRING:
							newCell.setCellValue(oldCell.getStringCellValue());
							break;

						case NUMERIC:
							if (DateUtil.isCellDateFormatted(oldCell)) {
								newCell.setCellValue(oldCell.getDateCellValue());
							} else {
								newCell.setCellValue(oldCell.getNumericCellValue());
							}
							break;

						case BOOLEAN:
							newCell.setCellValue(oldCell.getBooleanCellValue());
							break;

						case FORMULA:
							// Si la fórmula referencia celdas de un XLSX que aún no se han escrito,
							// se copiará la definición de la fórmula en texto. Excel la recalculará al
							// abrir el archivo.
							newCell.setCellFormula(oldCell.getCellFormula());
							break;

						case BLANK:
							newCell.setBlank();
							break;

						default:
							break;
						}
					}

					// Activación del rastreador automático de ancho de columnas para la hoja de
					// streaming
					if (!columnsTracked && newSheet instanceof org.apache.poi.xssf.streaming.SXSSFSheet) {
						org.apache.poi.xssf.streaming.SXSSFSheet sxSheet = (org.apache.poi.xssf.streaming.SXSSFSheet) newSheet;
						if (sxSheet.getRow(0) != null) {
							for (int col = 0; col < sxSheet.getRow(0).getLastCellNum(); col++) {
								sxSheet.trackColumnForAutoSizing(col);
							}
							columnsTracked = true;
						}
					}
				}

				// Ajustar automáticamente el ancho de las columnas utilizando las métricas
				// rastreadas
				if (columnsTracked && newSheet instanceof org.apache.poi.xssf.streaming.SXSSFSheet) {
					org.apache.poi.xssf.streaming.SXSSFSheet sxSheet = (org.apache.poi.xssf.streaming.SXSSFSheet) newSheet;
					if (oldSheet.getRow(0) != null) {
						for (int col = 0; col < oldSheet.getRow(0).getLastCellNum(); col++) {
							sxSheet.autoSizeColumn(col);
						}
					}
				}
			}

			// Escribir el nuevo archivo .xlsx limpio de memoria en el disco
			try (FileOutputStream fileOut = new FileOutputStream(xlsxPath)) {
				newWorkbook.write(fileOut);
			}
		}
	}
}
