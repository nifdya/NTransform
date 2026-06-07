package convert;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import record.MapDefinitionsTextPos; // Importamos el resolvedor centralizado
import record.RecordDefinitionTextPos;

public class TxtPosExcelConverter {

    private final Map<String, RecordDefinitionTextPos> mapaDefiniciones;
    private final Charset charset;

    public TxtPosExcelConverter(Map<String, RecordDefinitionTextPos> mapaDefiniciones, Charset charset) {
        this.mapaDefiniciones = mapaDefiniciones;
        this.charset = charset;
    }

    /**
     * PROCESO 3: TEXTO POSICIONAL -> EXCEL (.XLSX)
     */
    public void txtPosToExcel(String txtPath, String xlsxPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(txtPath, charset));
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Datos Posicionales");
            int rowNum = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                // REUTILIZACIÓN GLOBAL: Invocamos de forma estática la lógica centralizada de tipado
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

            // Auto-ajustar columnas basándonos en la primera fila de datos generada
            if (sheet.getRow(0) != null) {
                for (int col = 0; col < sheet.getRow(0).getLastCellNum(); col++) {
                    sheet.autoSizeColumn(col);
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(xlsxPath)) {
                workbook.write(fileOut);
            }
        }
    }

    /**
     * PROCESO 4: EXCEL (.XLSX) -> TEXTO POSICIONAL
     */
    public void excelToTxtPos(String xlsxPath, String txtPath) throws IOException {
        try (InputStream fileIn = new FileInputStream(xlsxPath);
             Workbook workbook = new XSSFWorkbook(fileIn);
             BufferedWriter writer = new BufferedWriter(new FileWriter(txtPath, charset))) {

            Sheet sheet = workbook.getSheetAt(0); // Procesa la primera pestaña
            DataFormatter formatter = new DataFormatter(); // Garantiza leer el texto tal y como se ve en Excel

            for (Row row : sheet) {
                // Averiguar el tipo leyendo la primera celda (Celda indice 0)
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

                List<Integer> longitudes = def.getLengthType() > 0 ? def.getLongitudes() : def.getLongitudes();
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
