package tunit;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.csv.CSVRecord;

import global.CSVUtils;
import global.options.ComunOptions;
import global.options.TaskOptions;
import global.task.Task;

public class UnitaryTransformations {

	private Task uTask;
	private ComunOptions opts;
	private TaskOptions optsTask;


	public UnitaryTransformations(Task task, ComunOptions opts, TaskOptions optsTask) {
		this.opts = opts;
		this.optsTask= optsTask;
		this.uTask = task;
	}



	/**
	 * 
	 * @param rowInput
	 * @param outSheet
	 * @param currentRow
	 * @param originalPositionRow
	 * @return
	 */
	private boolean processRowTask(CSVRecord rowInput,  int originalPositionRow) {
		boolean addRow = false;
		Integer modeFilterContains=0;
		try {
		switch (uTask) {
		case EliminarBlancos:
			if (!CSVUtils.isRowEmpty(rowInput)) {
				CSVUtils.copyRow(rowInput, this.opts.getOutputFile());
				addRow = true;
			}
			break;
		case CambiarPatron:
			CSVUtils.copyRow(rowInput, this.opts.getOutputFile(),  this.optsTask.get("oldValue"),
					this.optsTask.get("newValue"));
			addRow = true;
			break;
		case ObtenerFilasNMasX:
			if (((originalPositionRow + 1) >= (int) this.optsTask.get("N")
					&& ((originalPositionRow + 1) - (int) this.optsTask.get("N"))
							% (int) this.optsTask.get("X") == 0)) {
				CSVUtils.copyRow(rowInput, this.opts.getOutputFile());
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
			addRow = CSVUtils.rowContainsListStr(rowInput, this.optsTask.get("rowText"),
					this.optsTask.get("rowPositions"),modeFilterContains);
			if (addRow) {
				CSVUtils.copyRow(rowInput, this.opts.getOutputFile());
			}
			break;
		case BorrarFilaColumnaContiene:
			modeFilterContains=this.optsTask.get("mode")!=null?(Integer)this.optsTask.get("mode"):0;
			addRow = !(CSVUtils.rowContainsListStr(rowInput, this.optsTask.get("rowText"),
					this.optsTask.get("rowPositions"),modeFilterContains));
			if (addRow) {
				CSVUtils.copyRow(rowInput, this.opts.getOutputFile());
			}
			break;
		case ObtieneFilasPosiciones:
			if (Arrays.asList(this.optsTask.get("rowPositions")).contains(originalPositionRow)) {
				addRow = true;
				CSVUtils.copyRow(rowInput, this.opts.getOutputFile());
			}
			break;
		case BorrarFilasPosiciones:
			if (!(Arrays.asList(this.optsTask.get("rowPositions")).contains(originalPositionRow))) {
				addRow = true;
				CSVUtils.copyRow(rowInput, this.opts.getOutputFile());
			}
			break;
		case ObtieneColumnasPosiciones:
			addRow = true;
			CSVUtils.copyIncludingColumns(rowInput, this.opts.getOutputFile(),this.optsTask.get("colPositions"));
			break;
		case BorrarColumnasPosiciones:
			addRow = true;
			CSVUtils.copyExcludingColumns(rowInput, this.opts.getOutputFile(),this.optsTask.get("colPositions"));
			break;
	
		default:
			addRow = false;
			break;
		}
		} catch (IOException e) {
			e.printStackTrace();
			addRow=false;
		}
		return addRow;

	}



	/**
	 * Se realiza el procesamiento por fila para generar el fichero de salida
	 * @return
	 */
	public Boolean getOutputFileCheckByRow() {
		try {
			int contOriginal=0;
			for (CSVRecord record : this.opts.getInputFile()) {                
            	this.processRowTask(record,contOriginal);
            	contOriginal++;
            }
			return true;

		} catch (Exception e) {
			System.err.println(e);
			return false;

		}
	}




	public Boolean doTask() {

		switch (uTask) {
		case ObtenerRango:
			//this.getOutputRange(fIn);
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
			this.getOutputFileCheckByRow();
			break;
		default:
			break;
		}
		return true;

	}
}
