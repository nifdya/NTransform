package global;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CSVUtils {

    /**
     * Copia de forma ultra eficiente un registro completo.
     * CSVRecord implementa Iterable<String>, permitiendo delegar directo al printer sin crear arrays.
     */
    public static void copyRow(CSVRecord record, CSVPrinter printer) throws IOException {
        if (record == null) return;
        printer.printRecord(record);
    }

    /**
     * Comprueba si una fila está completamente vacía.
     * Se unificaron los dos métodos redundantes en una sola comprobación funcional rápida.
     */
    public static boolean isRowEmpty(CSVRecord record) {
        if (record == null || record.size() == 0) return true;
        for (String value : record) {
            if (value != null && !value.trim().isEmpty()) {
                return false; 
            }
        }
        return true;
    }    

    /**
     * Reemplaza cadenas mediante Regex en toda la fila y la escribe en el destino.
     */
    public static void copyRow(CSVRecord record, CSVPrinter printer, String valueSearch, String valueReplace) throws IOException {
        if (record == null) return;
        
        Object[] values = new Object[record.size()];
        for (int i = 0; i < record.size(); i++) {
            String cellValue = record.get(i);
            values[i] = cellValue != null ? cellValue.replaceAll(valueSearch, valueReplace) : "";
        }
        printer.printRecord(values);
    }

    /**
     * Motor de búsqueda unificado y estandarizado con los mismos modos que tu ExcelUtils.
     */
    private static boolean matchesMode(String cellValue, String searchValue, int mode) {
        if (cellValue == null || searchValue == null) return false;
        
        String cellLower = cellValue.toLowerCase();
        String searchLower = searchValue.toLowerCase();
        
        switch (mode) {
            case 0: // Coincidencia exacta
                return cellLower.equals(searchLower);
            case 1: // Contiene (Substring)
                return cellLower.contains(searchLower);
            case 2: // Empieza por
                return cellLower.startsWith(searchLower);
            case 3: // Termina por (Se añade para homologar con ExcelUtils)
                return cellLower.endsWith(searchLower);
            default:
                throw new IllegalArgumentException("Modo de búsqueda no válido: " + mode);
        }
    }

    // --- Versión 1: Celda única vs Valor único ---
    public static boolean columnContains(CSVRecord record, Integer columnPosition, String valueStr, int mode) {
        if (record == null || columnPosition >= record.size() || columnPosition < 0) return false;
        return matchesMode(record.get(columnPosition), valueStr, mode);
    }

    // --- Versión 2: Celda única vs Lista de valores ---
    public static boolean columnContains(CSVRecord record, Integer columnPosition, String[] valuesStr, int mode) {
        if (record == null || columnPosition >= record.size() || columnPosition < 0) return false;
        String cellValue = record.get(columnPosition);
        return Arrays.stream(valuesStr).anyMatch(val -> matchesMode(cellValue, val, mode));
    }

    // --- Versión 3: Lista de celdas vs Lista de valores ---
    public static boolean columnContains(CSVRecord record, Integer[] columnPositions, String[] valuesStr, int mode) {
        if (record == null) return false;
        for (int pos : columnPositions) {
            if (columnContains(record, pos, valuesStr, mode)) return true;
        }
        return false;
    }

    // --- Versión 4: Fila completa (posiciones específicas) vs Valor único ---
    public static boolean rowContains(CSVRecord record, String valueStr, Integer[] positions, int mode) {
        if (record == null) return false;
        for (int pos : positions) {
            if (columnContains(record, pos, valueStr, mode)) return true;
        }
        return false;
    }

    // --- Versión 5: Fila completa (todas las columnas) vs Valor único ---
    public static boolean rowContains(CSVRecord record, String valueStr, int mode) {
        if (record == null) return false;
        for (int i = 0; i < record.size(); i++) {
            if (matchesMode(record.get(i), valueStr, mode)) return true;
        }
        return false;
    }    

    /**
     * Une varias columnas basándose en una plantilla indexada tipo $1/$2.
     * Se optimiza usando StringBuilder para evitar crear strings en memoria dentro del bucle.
     */
    public static String joinColumns(CSVRecord record, Integer[] targetPositions, String joinTemplate, String defaultValue) {
        if (record == null) return joinTemplate;
        StringBuilder result = new StringBuilder(joinTemplate);
        for (int i = 0; i < targetPositions.length; i++) {
            int pos = targetPositions[i];
            String val = (pos >= 0 && pos < record.size()) ? record.get(pos) : defaultValue;
            
            String placeholder = "$" + (i + 1);
            String replacement = val != null ? val : "";
            
            int index = result.indexOf(placeholder);
            while (index != -1) {
                result.replace(index, index + placeholder.length(), replacement);
                index = result.indexOf(placeholder, index + replacement.length());
            }
        }
        return result.toString();
    }

    /**
     * Copia en el destino únicamente las columnas seleccionadas.
     */
    public static void copyIncludingColumns(CSVRecord record, CSVPrinter printer, Integer[] includePositions) throws IOException {
        if (record == null) return;
        List<String> values = new ArrayList<>(includePositions.length);
        for (int pos : includePositions) {
            values.add((pos >= 0 && pos < record.size()) ? record.get(pos) : "");
        }
        printer.printRecord(values);
    }
    
    /**
     * Copia la fila excluyendo las columnas indicadas.
     * Optimizado: Se usa Set O(1) en lugar de List.contains() O(N) para evitar lentitud en archivos grandes.
     */
    public static void copyExcludingColumns(CSVRecord record, CSVPrinter printer, Integer[] excludePositions) throws IOException {
        if (record == null) return;
        Set<Integer> excludeSet = Arrays.stream(excludePositions).collect(Collectors.toSet());
        List<String> values = new ArrayList<>(record.size());

        for (int i = 0; i < record.size(); i++) {
            if (!excludeSet.contains(i)) {
                values.add(record.get(i));
            }
        }
        printer.printRecord(values);
    }

}