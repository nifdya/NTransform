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

import global.NTextPosUtils; 
import global.options.ComunOptions;
import global.options.TaskOptions;
import global.task.Task;
import record.RecordDefinition; // Importamos el contenedor
/**
 * Clase principal para la gestión de las tareas unitarias del módulo
 */
public class UnitaryTransformations {

	private Task uTask;
	private ComunOptions opts;
	private TaskOptions optsTask;
	// Nueva propiedad para almacenar el mapa de configuraciones posicionales
	private Map<String, RecordDefinition> mapaDefiniciones;

	// Modificamos el constructor para recibir el mapa de definiciones desde NTextPos
	public UnitaryTransformations(Task task, ComunOptions opts, TaskOptions optsTask, Map<String, RecordDefinition> mapaDefiniciones) {
		this.opts = opts;
		this.optsTask = optsTask;
		this.uTask = task;
		this.mapaDefiniciones = mapaDefiniciones;
	}

	/**
	 * Procesa de forma atómica una única fila mapeada.
	 * 
	 * @param rowInput            Mapa con los datos indexados por posición ("p0", "p1"...)
	 * @param originalPositionRow Índice secuencial de la línea actual en el archivo.
	 * @param campoLongitudes     Lista de longitudes obtenidas dinámicamente para ESTE tipo de registro.
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
			    
			    String textoBuscado = "";
			    if (this.optsTask.get("rowText") instanceof String[]) {
			        String[] arr = (String[]) this.optsTask.get("rowText");
			        textoBuscado = (arr.length > 0) ? arr[0] : "";
			    } else {
			        textoBuscado = (String) this.optsTask.get("rowText");
			    }

			    addRow = NTextPosUtils.rowContains(rowInput, textoBuscado,
			            (Integer[]) this.optsTask.get("colPositions"), modeFilterContains);
			    if (addRow) {
			        NTextPosUtils.copyRow(rowInput, writer, campoLongitudes);
			    }
			    break;

			case BorrarFilaColumnaContiene:
			    modeFilterContains = this.optsTask.get("mode") != null ? (Integer) this.optsTask.get("mode") : 0;
			    
			    String textoBorrar = "";
			    if (this.optsTask.get("rowText") instanceof String[]) {
			        String[] arr = (String[]) this.optsTask.get("rowText");
			        textoBorrar = (arr.length > 0) ? arr[0] : "";
			    } else {
			        textoBorrar = (String) this.optsTask.get("rowText");
			    }

			    addRow = !(NTextPosUtils.rowContains(rowInput, textoBorrar,
			            (Integer[]) this.optsTask.get("colPositions"), modeFilterContains));
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
	
	private String checkTypeRow(String line) {
	    // 1. Si solo hay un tipo en el mapa, devolvemos "t1" directamente sin evaluar la línea
	    if (this.mapaDefiniciones.size() <= 1) {
	        return "default";
	    }

	    // 2. Usamos las coordenadas de "t1" para saber en qué posición de la línea viene el tipo
	    RecordDefinition ref = this.mapaDefiniciones.get("default");

	    if (ref != null && ref.getLengthType() > 0) {
	        int inicio = ref.getPosType() - 1;
	        int fin = inicio + ref.getLengthType();

	        if (line.length() >= fin) {
	            String tipoExtraido = line.substring(inicio, fin);
	            // Si el tipo extraído de la línea existe en el mapa (ej: "t2"), lo devolvemos
	            if (this.mapaDefiniciones.containsKey(tipoExtraido)) {
	                return tipoExtraido;
	            }
	        }
	    }
	    // 3. Si no coincide con ninguna estructura específica, es un registro genérico
	    return "default";
	}
	private Boolean workWithLineType(String line)
	{
		if(this.mapaDefiniciones.size()<=1)
		{
			return true;
		}
		else
		{			
			String[] typeInChange;
			Object typeObj = this.optsTask.get("type");
			
			if (typeObj instanceof String[]) {
				typeInChange = (String[]) typeObj;
			} else if (typeObj instanceof String) {
				typeInChange = new String[]{(String) typeObj};
			} else {
				typeInChange = null;
			}
			if (typeInChange != null && Arrays.asList(typeInChange).contains(this.checkTypeRow(line))) {
				return true;
			}
		}
		return false;
	}
	private RecordDefinition obtenerDefinicionDeLinea(String linea) {
		   return this.mapaDefiniciones.get(checkTypeRow(linea));
	}


	/**
	 * Realiza la lectura secuencial de texto plano y mapea dinámicamente según el XML por cada tipo.
	 */
	public Boolean getOutputFileCheckByRow() {
		try {
			BufferedReader reader = (BufferedReader) this.opts.getInputFile();
			int contOriginal = 0;
			String line;


			// Procesamos línea a línea de forma ultra-eficiente (Memoria constante)
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) continue;
				if(!this.workWithLineType(line)) continue;
				// 1. Resolvemos dinámicamente el layout de campos para ESTA línea concreta (O(1))
				RecordDefinition defActual = this.obtenerDefinicionDeLinea(line);
				if (defActual == null) {
					throw new RuntimeException("No se encontró una estructura de definición válida para la línea: " + contOriginal);
				}

				List<Integer> campoLongitudes = defActual.getLongitudes();
				List<Boolean> campoIgnorados = defActual.getIgnorados();

				Map<String, Object> row = new HashMap<>();
				int currentPointer = 0;

				// 2. Troceamos la línea basándonos exclusivamente en su tipo de registro correspondiente
				for (int i = 0; i < campoLongitudes.size(); i++) {
					if (currentPointer >= line.length()) break;
					
					int length = campoLongitudes.get(i);
					int endPointer = Math.min(currentPointer + length, line.length());
					
					if (campoIgnorados == null || i >= campoIgnorados.size() || !campoIgnorados.get(i)) {
						row.put("p" + i, line.substring(currentPointer, endPointer));
					}
					currentPointer += length;
				}
				
				// 3. Pasamos las longitudes del tipo de registro mapeado para que NTextPosUtils re-escriba con el formato exacto
				
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
		Boolean result=false;
		switch (uTask) {
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
				result=this.getOutputFileCheckByRow();
				break;
			default:
				break;
		}
		return result;
	}
}
