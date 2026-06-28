package test;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import global.NXLSXUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class NXLSXUtilsTest {

    private Workbook workbook;
    private Row sampleRow;

    @BeforeEach
    void setUp() {
        // Creamos un libro de Excel y una fila en memoria para las pruebas
        workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("TestSheet");
        sampleRow = sheet.createRow(0);

        // Poblamos celdas con diferentes tipos de datos y textos para probar los modos
        sampleRow.createCell(0).setCellValue("Alicante");    // Texto simple
        sampleRow.createCell(1).setCellValue("03001");       // Código numérico como texto
        sampleRow.createCell(2).setCellValue(12345);         // Número puro (evaluado por DataFormatter)
        sampleRow.createCell(3).setCellValue("Gran Vía");    // Texto con tildes y espacios
    }

    @AfterEach
    void tearDown() throws IOException {
        if (workbook != null) {
            workbook.close(); // Liberamos el recurso en memoria
        }
    }

    @Test
    void testColumnContainsSingleValueWithModes() {
        // Modo 0: Palabra completa (Case-Insensitive por el .toLowerCase() de tu código)
        assertTrue(NXLSXUtils.columnContains(sampleRow, 0, "alicante", 0));
        assertFalse(NXLSXUtils.columnContains(sampleRow, 0, "Alican", 0));

        // Modo 1: Contiene el texto
        assertTrue(NXLSXUtils.columnContains(sampleRow, 3, "an Ví", 1));
        assertFalse(NXLSXUtils.columnContains(sampleRow, 3, "Castellana", 1));

        // Modo 2: La columna empieza por
        assertTrue(NXLSXUtils.columnContains(sampleRow, 0, "ali", 2));
        assertFalse(NXLSXUtils.columnContains(sampleRow, 0, "cante", 2));

        // Modo 3: La columna finaliza por
        assertTrue(NXLSXUtils.columnContains(sampleRow, 0, "ante", 3));
        assertFalse(NXLSXUtils.columnContains(sampleRow, 0, "ali", 3));
    }

    @Test
    void testColumnContainsDataFormattedValues() {
        // Validación de celdas numéricas: DataFormatter convierte el número 12345 a String "12345"
        assertTrue(NXLSXUtils.columnContains(sampleRow, 2, "12345", 0));
        assertTrue(NXLSXUtils.columnContains(sampleRow, 2, "234", 1)); // Contiene
    }

    @Test
    void testColumnContainsInvalidModeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            NXLSXUtils.columnContains(sampleRow, 0, "Alicante", 99); // Modo 99 no existe
        });
    }

    @Test
    void testColumnContainsArrayOfValues() {
        String[] searchValues = {"Madrid", "Alicante", "Barcelona"};
        
        // Debe dar true porque "Alicante" está en la columna 0
        assertTrue(NXLSXUtils.columnContains(sampleRow, 0, searchValues, 0));

        // Debe dar false porque ninguno coincide con la columna 0
        String[] missingValues = {"Madrid", "Valencia"};
        assertFalse(NXLSXUtils.columnContains(sampleRow, 0, missingValues, 0));
    }

    @Test
    void testColumnContainsMultiplePositionsAndMultipleValues() {
        Integer[] positions = {0, 1}; // Columnas: "Alicante", "03001"
        String[] searchValues = {"03001", "Inexistente"};

        // Encuentra "03001" en la posición de columna 1
        assertTrue(NXLSXUtils.columnContains(sampleRow, positions, searchValues, 0));
    }

    @Test
    void testRowContainsInSpecificPositions() {
        Integer[] positionsToSearch = {0, 3}; // Buscaremos solo en "Alicante" y "Gran Vía"

        assertTrue(NXLSXUtils.rowContains(sampleRow, "Gran Vía", positionsToSearch, 0));
        assertTrue(NXLSXUtils.rowContains(sampleRow, "ali", positionsToSearch, 2)); // Empieza por
        
        // Debe dar false porque "12345" está en la columna 2, la cual omitimos del array de posiciones
        assertFalse(NXLSXUtils.rowContains(sampleRow, "12345", positionsToSearch, 0));
    }

    @Test
    void testColumnContainsLegacyEqualsIgnoreCaseMethod() {
        // Prueba el método sobrecargado sin parámetro de modo: 'columnContains(Row, Integer, String)'
        assertTrue(NXLSXUtils.columnContains(sampleRow, 0, "ALICANTE"));
        assertTrue(NXLSXUtils.columnContains(sampleRow, 0, "alicante"));
        assertFalse(NXLSXUtils.columnContains(sampleRow, 0, "Alican"));
    }
}
