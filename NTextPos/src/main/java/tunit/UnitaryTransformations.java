package tunit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import global.NTextPosUtils; // Tu librería nativa corregida para texto fijo
import global.options.ComunOptions;
import global.options.TaskOptions;
import global.task.Task;

public class UnitaryTransformations {

	private Task uTask;
	private ComunOptions opts;
	private TaskOptions optsTask;

	public UnitaryTransformations(Task task, ComunOptions opts, TaskOptions optsTask) {
		this.opts = opts;
		this.optsTask = optsTask;
		this.uTask = task;
	}

	/**
	 * Procesa de forma atómica una única fila mapeada.
	 * 
	 * @param rowInput            Mapa con los datos indexados por posición ("p0", "p1"...)
	 * @param originalPositionRow Índice secuencial de la línea actual en el archivo.
	 * @param campoLongitudes     Lista de longitudes obtenidas del XML.
	 * @return true si la fila procesada fue válida o guardada.
	 */
	private boolean processRowTask(Map<String, Object> rowInput, int originalPositionRow, List<Integer> campoLongitudes) throws IOException {
		boolean addRow = false;
		Integer modeFilterContains = 0;
		BufferedWriter writer = (BufferedWriter) this.opts.getOutputFile();

		switch (uTask) {
			case EliminarBlancos:
				if (!NTextPosUtils.isRowEmpty(rowInput)) {
					NTextPosUtils.copyRow(rowInput, writer, campoLongitudes);
					addRow = true;
				}
				break;

			case CambiarPatron:
				NTextPosUtils.copyRow(rowInput, writer, (String) this.optsTask.get("oldValue"), (String) this.optsTask.get("newValue"), campoLongitudes);
				addRow = true;
				break;

			case ObtenerFilasNMasX:
				if (((originalPositionRow + 1) >= (int) this.optsTask.get("N")
						&& ((originalPositionRow + 1) - (int) this.optsTask.get("N"))
								% (int) this.optsTask.get("X") == 0)) {
					NTextPosUtils.copyRow(rowInput, writer, campoLongitudes);
					addRow = true;
				}
				break;

			case BorrarFilasNMasX:
				if (!((originalPositionRow + 1) >= (int) this.optsTask.get("N")
						&& ((originalPositionRow + 1) - (int) this.optsTask.get("N"))
								% (int) this.optsTask.get("X") == 0)) {
					NTextPosUtils.copyRow(rowInput, writer, campoLongitudes);
					addRow = true;
				}
				break;

			case ObtenerFilaColumnaContiene:
			    modeFilterContains = this.optsTask.get("mode") != null ? (Integer) this.optsTask.get("mode") : 0;
			    
			    // 1. Extraemos de forma segura el texto de búsqueda. 
			    // Si viene como array [Ljava.lang.String;, tomamos la primera posición [0]
			    String textoBuscado = "";
			    if (this.optsTask.get("rowText") instanceof String[]) {
			        String[] arr = (String[]) this.optsTask.get("rowText");
			        textoBuscado = (arr.length > 0) ? arr[0] : "";
			    } else {
			        textoBuscado = (String) this.optsTask.get("rowText");
			    }

			    addRow = NTextPosUtils.rowContains(rowInput, textoBuscado,
			            (Integer[]) this.optsTask.get("rowPositions"), modeFilterContains);
			    if (addRow) {
			        NTextPosUtils.copyRow(rowInput, writer, campoLongitudes);
			    }
			    break;

			case BorrarFilaColumnaContiene:
			    modeFilterContains = this.optsTask.get("mode") != null ? (Integer) this.optsTask.get("mode") : 0;
			    
			    // Aplicamos la misma extracción segura para el borrado
			    String textoBorrar = "";
			    if (this.optsTask.get("rowText") instanceof String[]) {
			        String[] arr = (String[]) this.optsTask.get("rowText");
			        textoBorrar = (arr.length > 0) ? arr[0] : "";
			    } else {
			        textoBorrar = (String) this.optsTask.get("rowText");
			    }

			    addRow = !(NTextPosUtils.rowContains(rowInput, textoBorrar,
			            (Integer[]) this.optsTask.get("rowPositions"), modeFilterContains));
			    if (addRow) {
			        NTextPosUtils.copyRow(rowInput, writer, campoLongitudes);
			    }
			    break;

			case ObtieneFilasPosiciones:
				if (Arrays.asList((Integer[]) this.optsTask.get("rowPositions")).contains(originalPositionRow)) {
					addRow = true;
					NTextPosUtils.copyRow(rowInput, writer, campoLongitudes);
				}
				break;

			case BorrarFilasPosiciones:
				if (!(Arrays.asList((Integer[]) this.optsTask.get("rowPositions")).contains(originalPositionRow))) {
					addRow = true;
					NTextPosUtils.copyRow(rowInput, writer, campoLongitudes);
				}
				break;

			case ObtieneColumnasPosiciones:
				addRow = true;
				NTextPosUtils.copyIncludingColumns(rowInput, writer, (Integer[]) this.optsTask.get("colPositions"), campoLongitudes);
				break;

			case BorrarColumnasPosiciones:
				addRow = true;
				Integer[] positions = (Integer[]) this.optsTask.get("colPositions");
				Set<Integer> excludeSet = Arrays.stream(positions).collect(Collectors.toSet());
				NTextPosUtils.copyExcludingColumns(rowInput, writer, excludeSet, campoLongitudes);
				break;

			default:
				addRow = false;
				break;
		}
		return addRow;
	}

	/**
	 * Realiza la lectura secuencial de texto plano y mapea dinámicamente según el XML.
	 */
	public Boolean getOutputFileCheckByRow() {
		try {
			BufferedReader reader = (BufferedReader) this.opts.getInputFile();
			
			// IMPORTANTE: Asegúrate de añadir el getter getCampoLongitudes() o pasarlo en las opciones comunes.
			List<Integer> campoLongitudes = this.opts.getCampoLongitudes();
			List<Boolean> campoIgnorados = this.opts.getCampoIgnorados();
			
			int contOriginal = 0;
			String line;

			// Leemos línea a línea el archivo sin procesar de forma ultra-eficiente
			while ((line = reader.readLine()) != null) {
				Map<String, Object> row = new HashMap<>();
				int currentPointer = 0;

				// Troceamos dinámicamente la fila usando el mapa de longitudes cargado del XML
				for (int i = 0; i < campoLongitudes.size(); i++) {
					if (currentPointer >= line.length()) break;
					
					int length = campoLongitudes.get(i);
					int endPointer = Math.min(currentPointer + length, line.length());
					
					if (campoIgnorados == null || !campoIgnorados.get(i)) {
						row.put("p" + i, line.substring(currentPointer, endPointer));
					}
					currentPointer += length;
				}
				
				this.processRowTask(row, contOriginal, campoLongitudes);
				contOriginal++;
			}
			return true;

		} catch (Exception e) {
			System.err.println("Error procesando registros en UnitaryTransformations: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	public Boolean doTask() {
		switch (uTask) {
			case ObtenerRango:
				break;
			case CambiarPatron:
			case EliminarBlancos:
			case ObtenerFilasNMasX:
			case BorrarFilasNMasX:
			case ObtenerFilaColumnaContiene:
			case BorrarFilaColumnaContiene:
			case ObtieneFilasPosiciones:
			case BorrarFilasPosiciones:
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
