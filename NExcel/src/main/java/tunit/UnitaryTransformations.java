package tunit;


import java.util.Arrays;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import global.NXLSXUtilRowMode;
import global.NXLSXUtils;
import global.MasterOperation;
import global.options.ComunOptions;
import global.options.TaskOptions;
import global.task.Task;

/**
 * Clase base para las transformaciones unitarias
 * 
 */
public class UnitaryTransformations extends MasterOperation {

	/** Tarea a ejecutar */
	private Task uTask;

	/** Workbook de salida */
	private XSSFWorkbook fOut;

	/**
	 * Constructor de la clase
	 * 
	 * @param task     Tarea a ejecutar {@link Task}
	 * @param opts     Instancia de {@link ComunOptions} con la configuración
	 *                 global.
	 * @param optsTask Instancia de {@link TaskOptions} con la configuración de la
	 *                 tarea.
	 */
	public UnitaryTransformations(Task task, ComunOptions opts, TaskOptions optsTask) {
		super(opts, optsTask);
		this.uTask = task;
	}

	/**
	 * Divide columnas conforme a una expresión regular
	 * 
	 * @param rowInput   Fila de entrada
	 * @param outSheet   Hoja de salida
	 * @param currentRow Índice de la fila actual
	 * @param pattern    Patrón
	 */
	private void splitColumnsByRegexGroups(Row rowInput, Sheet outSheet, int currentRow,
			java.util.regex.Pattern pattern) {
		NXLSXUtilRowMode excelOpt = new NXLSXUtilRowMode(rowInput, outSheet.createRow(currentRow++));
		NXLSXUtils.splitColumnsByRegexGroups(excelOpt, this.optsTask.get("sheetsPositions"), pattern,
				this.optsTask.get("newValue"), this.optsTask.get("keepOriginalColumn"),
				this.optsTask.get("notFoundValue"), fOut, styleMap);
	}

	/**
	 * Une columnas conforme a una expresión regular
	 * 
	 * @param rowInput   Fila de entrada
	 * @param outSheet   Hoja de salida
	 * @param currentRow Índice de la fila actual
	 */
	private void joinColumnsByRegex(Row rowInput, Sheet outSheet, int currentRow) {
		NXLSXUtilRowMode excelOpt = new NXLSXUtilRowMode(rowInput, outSheet.createRow(currentRow++));
		NXLSXUtils.joinColumnsByRegex(excelOpt, this.optsTask.get("sheetsPositions"), this.optsTask.get("newValue"),
				this.optsTask.get("keepOriginalColumn"), this.optsTask.get("notFoundValue"), fOut, styleMap);
	}

	/**
	 * Crea una nueva fila añadiendo el contenido proporcionado en la fila de
	 * entrada
	 * 
	 * @param rowInput   Fila de entrada
	 * @param outSheet   Hoja de salida
	 * @param currentRow Índice de la fila actual
	 */
	private void newRow(Row rowInput, Sheet outSheet, int currentRow) {
		NXLSXUtilRowMode excelOpt = new NXLSXUtilRowMode(rowInput, outSheet.createRow(currentRow++));
		NXLSXUtils.copyRow(excelOpt, fOut, styleMap);
	}

	/**
	 * Copia una fila incluyendo o excluyendo las posiciones indicadas.
	 * 
	 * @param rowInput   Fila de entrada
	 * @param outSheet   Hoja de salida
	 * @param currentRow Índice de la fila actual
	 * @param include    Booleano que indica si se incluye o excluye las posiciones
	 * @param pColumns   Posiciones a incluir o excluir
	 */
	private void newRow(Row rowInput, Sheet outSheet, int currentRow, Boolean include, Integer[] pColumns) {
		NXLSXUtilRowMode excelOpt = new NXLSXUtilRowMode(rowInput, outSheet.createRow(currentRow++));
		if (include) {
			NXLSXUtils.fillRowWithPositions(excelOpt, pColumns, fOut, styleMap);
		} else {
			NXLSXUtils.fillRowExcludingPositions(excelOpt, pColumns, fOut, styleMap);
		}
	}

	/**
	 * Genera una fila conforme a la fila de entrada, reemplazando las ocurrencias
	 * de la cadena indicada por el nuevo valor
	 * 
	 * @param rowInput   Fila de entrada
	 * @param outSheet   Hoja de salida
	 * @param currentRow Índice de la fila actual
	 * @param oldValue   Valor a remplazar
	 * @param newValue   Nuevo valor en la sustitución
	 */
	private void newRowWithReplace(Row rowInput, Sheet outSheet, int currentRow, String oldValue, String newValue) {
		NXLSXUtilRowMode excelOpt = new NXLSXUtilRowMode(rowInput, outSheet.createRow(currentRow++));
		excelOpt.setReplaceMode(oldValue, newValue);
		NXLSXUtils.copyRow(excelOpt, fOut, styleMap);
	}

	/**
	 * Gestión de una fila, según la tarea que estamos procesando
	 * 
	 * @param rowInput            Fila de entrada
	 * @param outSheet            Hoja de salida
	 * @param currentRow          Índice de la fila actual
	 * @param originalPositionRow Posicion en el fichero original
	 * @return {@code true} se añadió la columna {@code false} no se ha añadido la
	 *         nueva columna
	 */
	private boolean processRowTask(Row rowInput, Sheet outSheet, int currentRow, int originalPositionRow) {
		boolean addRow = false;
		Integer modeFilterContains=0;
		switch (uTask) {
		case EliminarBlancos:
			if (!NXLSXUtils.isRowEmpty(rowInput)) {
				this.newRow(rowInput, outSheet, currentRow);
				addRow = true;
			}
			break;
		case CambiarPatron:
			this.newRowWithReplace(rowInput, outSheet, currentRow, this.optsTask.get("oldValue"),
					this.optsTask.get("newValue"));
			addRow = true;
			break;
		case ObtenerFilasNMasX:
			if (((originalPositionRow + 1) >= (int) this.optsTask.get("N")
					&& ((originalPositionRow + 1) - (int) this.optsTask.get("N"))
							% (int) this.optsTask.get("X") == 0)) {
				this.newRowWithReplace(rowInput, outSheet, currentRow, this.optsTask.get("oldValue"),
						this.optsTask.get("newValue"));
				addRow = true;
			}
			break;
		case BorrarFilasNMasX:
			if (!((originalPositionRow + 1) >= (int) this.optsTask.get("N")
					&& ((originalPositionRow + 1) - (int) this.optsTask.get("N"))
							% (int) this.optsTask.get("X") == 0)) {
				addRow = false;
			}
			break;
		case ObtenerFilaColumnaContiene:
			modeFilterContains=this.optsTask.get("mode")!=null?(Integer)this.optsTask.get("mode"):0;
			addRow = NXLSXUtils.rowContainsListValues(rowInput, this.optsTask.get("rowText"),
					this.optsTask.get("rowPositions"),modeFilterContains);
			if (addRow) {
				this.newRow(rowInput, outSheet, currentRow);
			}
			break;
		case BorrarFilaColumnaContiene:
			modeFilterContains=this.optsTask.get("mode")!=null?(Integer)this.optsTask.get("mode"):0;
			addRow = !(NXLSXUtils.rowContainsListValues(rowInput, this.optsTask.get("rowText"),
					this.optsTask.get("rowPositions"),modeFilterContains));
			if (addRow) {
				this.newRow(rowInput, outSheet, currentRow);
			}
			break;
		case ObtieneFilasPosiciones:
			if (Arrays.asList(this.optsTask.get("rowPositions")).contains(originalPositionRow)) {
				addRow = true;
				this.newRow(rowInput, outSheet, currentRow);
			}
			break;
		case BorrarFilasPosiciones:
			if (!(Arrays.asList(this.optsTask.get("rowPositions")).contains(originalPositionRow))) {
				addRow = true;
				this.newRow(rowInput, outSheet, currentRow);
			}
			break;
		case ObtieneColumnasPosiciones:
			addRow = true;
			this.newRow(rowInput, outSheet, currentRow, true, this.optsTask.get("colPositions"));
			break;
		case BorrarColumnasPosiciones:
			addRow = true;
			this.newRow(rowInput, outSheet, currentRow, false, this.optsTask.get("colPositions"));
			break;
		case UnirColumnasSecuencia:
			addRow = true;
			if (Arrays.asList(this.optsTask.get("rowPositions")).contains(currentRow)) {
				this.joinColumnsByRegex(rowInput, outSheet, currentRow);
			} else {
				this.newRow(rowInput, outSheet, currentRow);
			}

			break;
		case DividirColumnaPatron:
			if (Arrays.asList(this.optsTask.get("rowPositions")).contains(currentRow)) {
				this.splitColumnsByRegexGroups(rowInput, outSheet, currentRow, this.optsTask.get("originalPattern"));
			} else {
				this.newRow(rowInput, outSheet, currentRow);
			}
			addRow = true;
			break;
		default:
			addRow = false;
			break;
		}
		return addRow;

	}

	/**
	 * Comprobar la acción para la hoja
	 * 
	 * @param sheetPosition Posición de la hoja
	 * @param sheetName     Nombre de la hoja
	 * @return {@code "Check"} se tiene que procesar/tratar la hoja
	 *         {@code "No-Copy"} no se copiará la hoja {@code "Copy"} se copiará el
	 *         contenido integro de la hoja
	 */
	private String checkSheetAction(int sheetPosition, String sheetName) {
		String res = "Check";
		if (this.optsTask.get("sheetsPositions") != null
				&& ((Integer[]) this.optsTask.get("sheetsPositions")).length > 0) {
			if (Arrays.asList(this.optsTask.get("sheetsPositions")).contains(sheetPosition)) {
				res = "Check";
			} else if (this.optsTask.get("otherSheetsAction") != null
					&& this.optsTask.get("otherSheetsAction").equals("E")) {
				res = "No-Copy";
			} else {
				res = "Copy";

			}
		} else if (this.optsTask.get("sheetsName") != null && ((String[]) this.optsTask.get("sheetsName")).length > 0) {
			if (Arrays.asList(this.optsTask.get("sheetsName")).contains(sheetName)) {
				res = "Check";
			} else if (this.optsTask.get("otherSheetsAction") != null
					&& this.optsTask.get("otherSheetsAction").equals("E")) {
				res = "No-Copy";
			} else {
				res = "Copy";
			}
		}

		return res;
	}

	/**
	 * Procesa la tarea según las condiciones del comando
	 * 
	 * @param fIn
	 * @return Devuelve el objeto Workbook combinado
	 */
	public XSSFWorkbook getOutputFileCheckByRow(XSSFWorkbook fIn) {
		try {
			fOut = new XSSFWorkbook();
			int currentRow = 0;
			int originalPositionRow = 0;
			// Recorrer todas las hojas del libro original
			for (int i = 0; i < fIn.getNumberOfSheets(); i++) {
				if (this.checkConditions(i + 1)) {
					currentRow = 0;
					originalPositionRow = 0;
					// Leemos la fila origen
					Sheet hojaOrigen = fIn.getSheetAt(i);
					// Obtenemos el nombre de la hoja
					String nombreDeLaHoja = hojaOrigen.getSheetName();
					String process = this.checkSheetAction(i, nombreDeLaHoja);
					if (process.equals("Check") || process.equals("Copy")) {
						Sheet outSheet = fOut.createSheet(nombreDeLaHoja);
						//System.err.println("Procesando la hoja: " + nombreDeLaHoja);

						// trabajamos cada fila
						for (Row oldRow : hojaOrigen) {
							if (process.equals("Check")) {
								// Generamos y tratamos cada fila de la hoja

								if (this.processRowTask(oldRow, outSheet, currentRow, originalPositionRow)) {
									currentRow++;
								}
							} else {
								this.newRow(oldRow, outSheet, currentRow);
								currentRow++;
							}
							originalPositionRow++;
						}
					}
				}
			}
			return fOut;

		} catch (Exception e) {
			return null;

		}
	}

	/**
	 * Procesa la tarea según las condiciones del comando para rangos
	 * 
	 * @param fIn
	 * @return Devuelve el objeto Workbook combinado
	 */
	public XSSFWorkbook getOutputRange(XSSFWorkbook fIn) {
		try {
			fOut = new XSSFWorkbook();
			// Recorrer todas las hojas del libro original
			for (int i = 0; i < fIn.getNumberOfSheets(); i++) {
				if (this.checkConditions(i + 1)) {
					// Leemos la fila origen
					Sheet hojaOrigen = fIn.getSheetAt(i);
					// Obtenemos el nombre de la hoja
					String nombreDeLaHoja = hojaOrigen.getSheetName();
					String process = this.checkSheetAction(i, nombreDeLaHoja);
					if (process.equals("Check") || process.equals("Copy")) {
						Sheet outSheet = fOut.createSheet(nombreDeLaHoja);
						CellRangeAddress range = CellRangeAddress.valueOf(this.optsTask.get("range"));
						int rowOffset = -range.getFirstRow() + (int) this.optsTask.get("newRowInit") - 1;
						int colOffset = -range.getFirstColumn() + (int) this.optsTask.get("newColInit") - 1;
						NXLSXUtils.copyRange(hojaOrigen, outSheet, range, rowOffset, colOffset, fOut, styleMap);
					}
				}
			}
			return fOut;

		} catch (Exception e) {
			return null;

		}
	}

	/**
	 * Punto de entrada de la clase
	 */
	public XSSFWorkbook doTask(XSSFWorkbook fIn) {
		switch (uTask) {
		case ObtenerRango:
			this.getOutputRange(fIn);
			break;
		case CambiarPatron:
		case EliminarBlancos:
		case ObtenerFilasNMasX:
		case BorrarFilasNMasX:
		case ObtenerFilaColumnaContiene:
		case BorrarFilaColumnaContiene:
		case ObtieneFilasPosiciones:
		case ObtieneColumnasPosiciones:
		case BorrarColumnasPosiciones:
		case DividirColumnaPatron:
		case BorrarFilasPosiciones:
			this.getOutputFileCheckByRow(fIn);
			break;
		default:
			break;
		}
		return fOut;

	}
}
