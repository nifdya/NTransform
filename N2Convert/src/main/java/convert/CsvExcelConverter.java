package convert;

import com.github.pjfanning.xlsx.StreamingReader; // <-- LECTOR EN STREAMING
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook; // <-- ESCRITOR EN STREAMING

import java.io.*;
import java.nio.charset.Charset;

public class CsvExcelConverter {

    // Configuración centralizada del CSV compatible con Excel estándar
    private static final CSVFormat EXCEL_CSV_FORMAT = CSVFormat.EXCEL.builder()
            .setDelimiter(';') // Cambiar por ',' si tu entorno usa coma
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();

    /**
     * PROCESO 1: CSV -> XLSX (OPTIMIZADO PARA MEMORIA)
     * Lee un CSV línea a línea y escribe el Excel volcando datos al disco de forma constante.
     */
    public static void csvToXlsx(String csvPath, String xlsxPath, Charset charset) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath, charset));
             CSVParser csvParser = new CSVParser(reader, EXCEL_CSV_FORMAT);
             // Mantener solo 100 filas en memoria RAM simultáneamente
             SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {

            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("Datos");
            int rowNum = 0;

            for (CSVRecord record : csvParser) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < record.size(); i++) {
                    Cell cell = row.createCell(i);
                    String value = record.get(i);
                    
                    processCellValue(cell, value);
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(xlsxPath)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * PROCESO INVERSO: XLSX -> CSV (OPTIMIZADO PARA MEMORIA)
     * Lee un Excel masivo directamente del disco y genera el archivo CSV sobre la marcha.
     */
    public static void xlsxToCsv(String xlsxPath, String csvPath, Charset charset) throws IOException {
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream fileIn = new FileInputStream(xlsxPath);
             // Abre el archivo usando la arquitectura de streaming SAX por debajo
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(100)    // Filas simultáneas en RAM
                     .bufferSize(4096)     // Buffer de lectura
                     .open(fileIn);
             BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath, charset));
             CSVPrinter csvPrinter = new CSVPrinter(writer, EXCEL_CSV_FORMAT)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                // Evaluamos dinámicamente el total real de columnas de la fila
                int lastColumn = Math.max(row.getLastCellNum(), 0);
                String[] lineData = new String[lastColumn];

                for (int cn = 0; cn < lastColumn; cn++) {
                    // En StreamingReader el comportamiento por defecto devuelve null si la celda no tiene datos
                    Cell cell = row.getCell(cn);
                    if (cell == null) {
                        lineData[cn] = "";
                    } else {
                        // Formatea números, fechas y textos según la configuración de la celda
                        lineData[cn] = dataFormatter.formatCellValue(cell);
                    }
                }
                // Escribe la línea escapando comillas o delimitadores automáticamente
                csvPrinter.printRecord((Object[]) lineData);
            }
            csvPrinter.flush();
        }
    }

    /**
     * Asigna tipos de datos dinámicos evitando corromper textos que parecen números
     */
    private static void processCellValue(Cell cell, String value) {
        if (value == null || value.isEmpty()) {
            cell.setBlank();
            return;
        }
        // Conserva códigos de identificación con ceros iniciales (Ej: "08001") como texto
        if (value.length() > 1 && value.startsWith("0") && value.matches("\\d+")) {
            cell.setCellValue(value);
            return;
        }
        try {
            cell.setCellValue(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            cell.setCellValue(value);
        }
    }
}
