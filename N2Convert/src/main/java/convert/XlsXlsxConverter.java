package convert;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class XlsXlsxConverter {

    /**
     * PROCESO: XLS -> XLSX
     * Convierte un libro antiguo de Excel (.xls) al formato moderno (.xlsx)
     * Reutiliza el mapa de estilos para evitar errores de memoria.
     */
    public void xlsToXlsx(String xlsPath, String xlsxPath) throws IOException {
        // Validación previa de extensiones
        if (!xlsPath.toLowerCase().endsWith(".xls") || !xlsxPath.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Las extensiones deben ser estrictamente .xls para la entrada y .xlsx para la salida.");
        }

        try (InputStream fileIn = new FileInputStream(xlsPath);
             Workbook oldWorkbook = new HSSFWorkbook(fileIn); // Lee formato binario antiguo
             Workbook newWorkbook = new XSSFWorkbook()) {    // Crea formato moderno XML

            // Mapa para gestionar eficientemente los estilos de celda (Clave: Índice antiguo, Valor: Estilo nuevo)
            Map<Integer, CellStyle> styleMap = new HashMap<>();

            // Iterar por todas las pestañas/hojas del libro antiguo
            for (int i = 0; i < oldWorkbook.getNumberOfSheets(); i++) {
                Sheet oldSheet = oldWorkbook.getSheetAt(i);
                Sheet newSheet = newWorkbook.createSheet(oldSheet.getSheetName());
          
                // Copiar filas y celdas utilizando el iterador limpio
                for (Row oldRow : oldSheet) {
                    Row newRow = newSheet.createRow(oldRow.getRowNum());

                    // Mantener la altura de la fila original
                    newRow.setHeight(oldRow.getHeight());

                    for (Cell oldCell : oldRow) {
                        Cell newCell = newRow.createCell(oldCell.getColumnIndex());

                        // 1. Copiar y clonar el estilo de forma segura
                        int oldStyleIdx = oldCell.getCellStyle().getIndex();
                        if (!styleMap.containsKey(oldStyleIdx)) {
                            CellStyle newStyle = newWorkbook.createCellStyle();
                            newStyle.cloneStyleFrom(oldCell.getCellStyle());
                            styleMap.put(oldStyleIdx, newStyle);
                        }
                        newCell.setCellStyle(styleMap.get(oldStyleIdx));

                        // 2. Copiar el valor evaluando el tipo de celda original (Sintaxis compatible universal)
                        switch (oldCell.getCellType()) {
                            case STRING:
                                String stringCellValue = oldCell.getStringCellValue();
                                newCell.setCellValue(stringCellValue);
                                break;
                                
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(oldCell)) {
                                    newCell.setCellValue(oldCell.getDateCellValue());
                                } else {
                                    newCell.setCellValue(oldCell.getNumericCellValue());
                                }
                                break;
                                
                            case BOOLEAN:
                                newCell.setCellValue(oldCell.getBooleanCellValue());
                                break;
                                
                            case FORMULA:
                                newCell.setCellFormula(oldCell.getCellFormula());
                                break;
                                
                            case BLANK:
                                newCell.setBlank();
                                break;
                                
                            default:
                                // Ignora tipos de celdas desconocidos o con errores
                                break;
                        }
                    }
                }

                // Ajustar automáticamente el ancho de las columnas en la nueva hoja
                if (oldSheet.getRow(0) != null) {
                    for (int col = 0; col < oldSheet.getRow(0).getLastCellNum(); col++) {
                        newSheet.autoSizeColumn(col);
                    }
                }
            }

            // Escribir el nuevo archivo .xlsx en disco
            try (FileOutputStream fileOut = new FileOutputStream(xlsxPath)) {
                newWorkbook.write(fileOut);
            }
        }
    }
}
