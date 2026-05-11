package global;

import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import global.options.ComunOptions;
import global.options.TaskOptions;
import global.range.ParamRange;

/**
 * Clase base para las operaciones de transformación de Excel. Proporciona la
 * infraestructura común para gestionar opciones, estilos y la validación de
 * rangos de ejecución.
 * 
 * @author imc
 */
public class MasterOperation {

	/** Opciones comunes de configuración (ficheros, modos de rango, etc.). */
	protected ComunOptions opts;
	/** Opciones específicas de la tarea técnica a ejecutar. */
	protected TaskOptions optsTask;
	/** Mapa para cachear y reutilizar estilos de celda de Apache POI. */
	protected Map<Integer, CellStyle> styleMap = new HashMap<>();

	/**
	 * Constructor principal de la clase.
	 * 
	 * @param opts     Instancia de {@link ComunOptions} con la configuración
	 *                 global.
	 * @param optsTask Instancia de {@link TaskOptions} con la configuración de la
	 *                 tarea.
	 */
	public MasterOperation(ComunOptions opts, TaskOptions optsTask) {

		this.opts = opts;
		this.optsTask = optsTask;
		this.styleMap = new HashMap<>();
	}

	/**
	 * Verifica si una posición determinada (índice) debe ser procesada según el
	 * modo de rango configurado en {@link ComunOptions}.
	 * 
	 * <p>
	 * Los modos soportados incluyen:
	 * </p>
	 * <ul>
	 * <li><b>all</b>: Procesa todas las posiciones.</li>
	 * <li><b>odd/even</b>: Procesa posiciones impares o pares.</li>
	 * <li><b>range</b>: Procesa un rango simple (inicio-fin).</li>
	 * <li><b>rowsPlusX</b>: Procesa a partir de un inicio con un incremento
	 * fijo.</li>
	 * <li><b>xrange</b>: Procesa múltiples rangos discontinuos.</li>
	 * </ul>
	 * 
	 * @param pos La posición o índice de fila a evaluar.
	 * @return {@code true} si la posición cumple las condiciones para ser
	 *         procesada; {@code false} en caso contrario.
	 * @throws Exception Si ocurre un error en el parseo de los rangos.
	 */

	protected boolean checkConditions(int pos) throws Exception {
		boolean res = false;
		try {
			switch (this.opts.getModeRange()) {
			case all:
				res = true;
				break;
			case odd:
				res = (pos % 2 != 0);
				break;
			case even:
				res = (pos % 2 == 0);
				break;
			case range:
				String[] auxr = this.opts.getListRanges().get(0).toString().split("-");
				res = (pos >= Integer.parseInt(auxr[0].toString()) && pos <= Integer.parseInt(auxr[1].toString()));
				break;
			case rowsPlusX:
				Object[] auxfm = this.opts.getListRanges().get(0).toString().split("-");
				Integer inicio = Integer.parseInt(auxfm[0].toString());
				Integer incremento = Integer.parseInt(auxfm[1].toString());
				res = (pos >= inicio && ((pos - inicio) % incremento == 0));
				break;
			case xrange:
				for (ParamRange r : this.opts.getListRanges()) {
					String[] aux = r.toString().split("-");
					if (pos >= Integer.parseInt(aux[0].toString()) && pos <= Integer.parseInt(aux[1].toString())) {
						res = true;
					}
				}
				break;

			default:
				res = false;
				break;
			}
			return res;
		} catch (Exception e) {
			return false;
		}

	}
}
