package convert;

import com.github.pjfanning.xlsx.StreamingReader; 
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import record.MapDefinitionsTextPos;
import record.RecordDefinitionTextPos;

/**
 * Clase para la conversión de ficheros Texto por posiciones al formato XSLX y viceversa
 * 
 * @version 1.0
 */
public class TxtPosExcelConverter {

	/** Definiciones del fichero de texto por posiciones */
    private final Map<String, RecordDefinitionTextPos> mapaDefiniciones;
    /** Charset del fichero de texto por posiciones */
    private final Charset charset;
    
	/**
	 * Cargamos en el constructor el mapa de definiciones y el charset del fichero
	 * 
	 * @param mapaDefiniciones - Definiciones del fichero de texto por posiciones	
	 * @param charset - Charset del fichero de texto por posiciones.
	 */
    public TxtPosExcelConverter(Map<String, RecordDefinitionTextPos> mapaDefiniciones, Charset charset) {
        this.mapaDefiniciones = mapaDefiniciones;
        this.charset = charset;
    }

    /**
     * Método para la conversión de un fichero de texto por posiciones a XLSX
     * 
     * @param txtPath - Ruta del fichero de texto origen
     * @param xlsxPath - Ruta del fichero destino xlsx, resultante de la conversión
     * @throws IOException
     */
    public void txtPosToExcel(String txtPath, String xlsxPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(txtPath, charset));
             SXSSFWorkbook workbook = new SXSSFWorkbook(100)) { 

            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("Datos Posicionales");
            int rowNum = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                String tipo = MapDefinitionsTextPos.obtenerTipoDeLinea(line, this.mapaDefiniciones);
                RecordDefinitionTextPos def = this.mapaDefiniciones.get(tipo);
                if (def == null) continue;

                List<Integer> longitudes = def.getLongitudes();
                List<Boolean> ignorados = def.getIgnorados();
                Row row = sheet.createRow(rowNum++);
                int currentPointer = 0;
                int cellNum = 0;

                for (int i = 0; i < longitudes.size(); i++) {
                    if (currentPointer >= line.length()) break;
                    int length = longitudes.get(i);
                    int endPointer = Math.min(currentPointer + length, line.length());

                    if (ignorados == null || i >= ignorados.size() || !ignorados.get(i)) {
                        String cellValue = line.substring(currentPointer, endPointer).trim();
                        Cell cell = row.createCell(cellNum++);
                        cell.setCellValue(cellValue);
                    }
                    currentPointer += length;
                }
            }

            if (sheet instanceof org.apache.poi.xssf.streaming.SXSSFSheet) {
                org.apache.poi.xssf.streaming.SXSSFSheet sxSheet = (org.apache.poi.xssf.streaming.SXSSFSheet) sheet;
                if (sxSheet.getRow(0) != null) {
                    for (int col = 0; col < sxSheet.getRow(0).getLastCellNum(); col++) {
                        sxSheet.trackColumnForAutoSizing(col);
                        sxSheet.autoSizeColumn(col);
                    }
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(xlsxPath)) {
                workbook.write(fileOut);
            }
            // Eliminado workbook.dispose() por estar deprecated (el try-with-resources se encargará)
        }
    }

    /**
     * Método para la conversión de un fichero XLSX a un ficher de texto por posiciones 
     * 
     * @param xlsxPath - Ruta del fichero origen xlsx
     * @param txtPath - Ruta del fichero de texto origen, resultante de la conversión
     * @throws IOException
     */
    public void excelToTxtPos(String xlsxPath, String txtPath) throws IOException {
        // Abrimos el archivo a través de StreamingReader
        try (InputStream fileIn = new FileInputStream(xlsxPath);
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(100)    // Número de filas a mantener en memoria RAM simultáneamente
                     .bufferSize(4096)     // Tamaño del buffer de lectura del archivo físico
                     .open(fileIn);        // Abre el flujo en modo SAX/Streaming
             BufferedWriter writer = new BufferedWriter(new FileWriter(txtPath, charset))) {

            Sheet sheet = workbook.getSheetAt(0); 
            DataFormatter formatter = new DataFormatter(); 

            // El bucle interno itera fila por fila a medida que se parsea el XML del disco
            for (Row row : sheet) {
                String tipoExcel = "default";
                Cell firstCell = row.getCell(0);
                if (firstCell != null && this.mapaDefiniciones.size() > 1) {
                    String extractedType = formatter.formatCellValue(firstCell).trim();
                    if (this.mapaDefiniciones.containsKey(extractedType)) {
                        tipoExcel = extractedType;
                    }
                }

                RecordDefinitionTextPos def = this.mapaDefiniciones.get(tipoExcel);
                if (def == null) continue;

                List<Integer> longitudes = def.getLongitudes();
                List<Boolean> ignorados = def.getIgnorados();
                StringBuilder sbLine = new StringBuilder();
                int excelCellIdx = 0;

                for (int i = 0; i < longitudes.size(); i++) {
                    int targetLen = longitudes.get(i);
                    String rawValue = "";

                    if (ignorados == null || i >= ignorados.size() || !ignorados.get(i)) {
                        Cell cell = row.getCell(excelCellIdx);
                        if (cell != null) {
                            rawValue = formatter.formatCellValue(cell).trim();
                        }
                        excelCellIdx++;
                    }

                    // Padding exacto por la derecha para mantener la estructura fija
                    if (rawValue.length() > targetLen) {
                        sbLine.append(rawValue.substring(0, targetLen));
                    } else {
                        sbLine.append(String.format("%-" + targetLen + "s", rawValue));
                    }
                }
                writer.write(sbLine.toString());
                writer.newLine();
            }
        }
    }
}
