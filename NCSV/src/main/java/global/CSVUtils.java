package global;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVUtils {

    /**
     * 
     * @param record Registro de entrada
     * @param printer Impresora de salida
     */
    public static void copyRow(CSVRecord record, CSVPrinter printer) throws IOException {
        if (record == null) return;
        Object[] values = new Object[record.size()];
        for (int i = 0; i < record.size(); i++) {
            values[i] = record.get(i);
        }
        printer.printRecord(values);
    }
    public static boolean isRowEmpty(CSVRecord record) {
        if (record == null || record.size() == 0) {
            return true;
        }

        for (int i = 0; i < record.size(); i++) {
            String value = record.get(i);
            if (value != null && !value.trim().isEmpty()) {
                return false; 
            }
        }

        return true; // Si termina el bucle, todo estaba vacío
    }    
    public static void copyRow(CSVRecord record, CSVPrinter printer, String valueSearch, String valueReplace) throws IOException {
    	if (record == null) return;
    	
    	Object[] values = new Object[record.size()];
    	for (int i = 0; i < record.size(); i++) {
    		String cellValue = record.get(i);
    		values[i] = cellValue.replaceAll(valueSearch, valueReplace);
    	}
    	printer.printRecord(values);
    }

    public static boolean columnContains(CSVRecord record, Integer columnPosition, String valueStr) {
        if (columnPosition >= record.size()) return false;
        String cellValue = record.get(columnPosition);
        return valueStr.equalsIgnoreCase(cellValue);
    }

    public static boolean columnContains(CSVRecord record, Integer columnPosition, String[] valuesStr) {
        if (columnPosition >= record.size()) return false;
        String cellValue = record.get(columnPosition);
        return Arrays.stream(valuesStr).anyMatch(cellValue::equalsIgnoreCase);
    }

    public static boolean columnContains(CSVRecord record, Integer[] columnPositions, String[] valuesStr) {
        for (int pos : columnPositions) {
            if (columnContains(record, pos, valuesStr)) return true;
        }
        return false;
    }

    public static boolean rowContains(CSVRecord record, String valueStr, Integer[] positions) {
        for (int pos : positions) {
            if (columnContains(record, pos, valueStr)) return true;
        }
        return false;
    }

    public static boolean rowContains(CSVRecord record, String valueStr) {
        for (int i = 0; i < record.size(); i++) {
            if (columnContains(record, i, valueStr)) return true;
        }
        return false;
    }

    public static boolean isRecordEmpty(CSVRecord record) {
        if (record == null) return true;
        for (String s : record) {
            if (s != null && !s.trim().isEmpty()) return false;
        }
        return true;
    }

    /**
     * Une varias columnas en una sola cadena usando un template tipo $1/$2
     */
    public static String joinColumns(CSVRecord record, Integer[] targetPositions, String joinTemplate, String defaultValue) {
        String result = joinTemplate;
        for (int i = 0; i < targetPositions.length; i++) {
            int pos = targetPositions[i];
            String val = (pos < record.size()) ? record.get(pos) : defaultValue;
            result = result.replace("$" + (i + 1), val != null ? val : "");
        }
        return result;
    }
    /**
     * Retorna una lista con los valores de las columnas indicadas.
     */
    public static void copyIncludingColumns(CSVRecord record, CSVPrinter printer, Integer[] includePositions) throws IOException {
        List<String> values = new ArrayList<>();
        
        for (int pos : includePositions) {
            if (pos >= 0 && pos < record.size()) {
                values.add(record.get(pos));
            } else {
                values.add(""); // Opcional: añadir vacío si la posición no existe
            }
        }
        printer.printRecord(values);
    }
    
    /**
     * Retorna una lista con todos los valores excepto los de las posiciones indicadas.
     */
    public static void copyExcludingColumns(CSVRecord record, CSVPrinter printer, Integer[] excludePositions) throws IOException {
        List<String> values = new ArrayList<>();
        List<Integer> excludeList = Arrays.asList(excludePositions);

        for (int i = 0; i < record.size(); i++) {
            if (!excludeList.contains(i)) {
                values.add(record.get(i));
            }
        }
        printer.printRecord(values);
    }

}
