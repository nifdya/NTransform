package merge;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import global.ExcelUtilRowMode;
import global.ExcelUtils;
import global.MasterOperation;
import global.options.ComunOptions;
import global.options.TaskOptions;

/**
 * Clase para la unión de hojas de un excel
 */

public class MergeSheets extends MasterOperation {

	/**
	 * Constructor de la clase
	 * 
	 * @param opts     Instancia de {@link ComunOptions} con la configuración
	 *                 global.
	 * @param optsTask Instancia de {@link TaskOptions} con la configuración de la
	 *                 tarea.
	 */
	public MergeSheets(ComunOptions opts, TaskOptions optsTask) {
		super(opts, optsTask);
	}

	/**
	 * Una las hojas según las condiciones del comando
	 * 
	 * @param fIn
	 * @return Devuelve el objeto Workbook combinado
	 */
	public XSSFWorkbook getOutputFile(XSSFWorkbook fIn) {
		try {
			XSSFWorkbook fOut = new XSSFWorkbook();
			Sheet outSheet = fOut.createSheet("Hoja combinada");
			int currentRow = 0;

			// Recorrer todas las hojas del libro original
			for (int i = 0; i < fIn.getNumberOfSheets(); i++) {
				if (this.checkConditions(i + 1)) {
					// Leemos la fila origen
					Sheet sourceSheet = fIn.getSheetAt(i);
					// Obtenemos el nombre de la hoja
					String sheetName = sourceSheet.getSheetName();

					// parámetro "-CB" activado
					if ((boolean) this.optsTask.get("header")) {
						outSheet.createRow(currentRow++);
						Row headerRow = outSheet.createRow(currentRow++);
						Cell headerCell = headerRow.createCell(0);
						headerCell.setCellValue(sheetName);
						outSheet.createRow(currentRow++);
					}
					// copiamos el cuerpo de la hoja
					for (Row oldRow : sourceSheet) {
						// Generamos y tratamos cada fila de la hoja
						ExcelUtilRowMode excelOpt = new ExcelUtilRowMode(oldRow, outSheet.createRow(currentRow++));
						ExcelUtils.copyRow(excelOpt, fOut, styleMap);
					}
				}
			}
			return fOut;

		} catch (Exception e) {
			return null;

		}
	}

}
