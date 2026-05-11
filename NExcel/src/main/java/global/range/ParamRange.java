package global.range;
/**
 * 
 * Clase para la definición de un rango de parámetros
 * 
 */
public class ParamRange {
    private final int start;
    private final int end;
    /**
     * Constructor de la clase con los valores que delimitan el rango
     * @param start
     * @param end
     */
    public ParamRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Convierte el rango a una cadena 
     */
    @Override
    public String toString() {
        return String.format("Rango[%d - %d]", start, end);
    }
}
