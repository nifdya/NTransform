package test;


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import global.CSVUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CSVUtilsTest {

    private CSVRecord sampleRecord;

    @BeforeEach
    void setUp() throws IOException {
        // Creamos una fila simulada en memoria: "03,Agost,Alicante,Avenida"
        String csvData = "03,Agost,Alicante,Avenida";
        CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(csvData));
        sampleRecord = parser.getRecords().get(0);
    }

    @Test
    void testCopyRow() throws IOException {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            CSVUtils.copyRow(sampleRecord, printer);
        }

        // El formato por defecto añade saltos de línea estándar (\r\n)
        assertEquals("03,Agost,Alicante,Avenida\r\n", sw.toString());
    }

    @Test
    void testIsRowEmpty() throws IOException {
        assertTrue(CSVUtils.isRowEmpty(null));

        CSVRecord emptyRecord = CSVFormat.DEFAULT.parse(new StringReader(",  ,")).getRecords().get(0);
        assertTrue(CSVUtils.isRowEmpty(emptyRecord));

        assertFalse(CSVUtils.isRowEmpty(sampleRecord));
    }

    @Test
    void testCopyRowWithRegexReplacement() throws IOException {
        StringWriter sw = new StringWriter();
        // Fila con números para probar la expresión regular
        CSVRecord recordWithNumbers = CSVFormat.DEFAULT.parse(new StringReader("Juan123,30")).getRecords().get(0);

        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            // Reemplaza todos los dígitos por X
            CSVUtils.copyRow(recordWithNumbers, printer, "\\d", "X");
        }

        assertEquals("JuanXXX,XX\r\n", sw.toString());
    }

    @Test
    void testColumnContainsSingleValue() {
        // Modo 0: Exacto (Case-Insensitive por tu lógica de .toLowerCase())
        assertTrue(CSVUtils.columnContains(sampleRecord, 2, "alicante", 0));
        assertFalse(CSVUtils.columnContains(sampleRecord, 2, "Alican", 0));

        // Modo 1: Contiene
        assertTrue(CSVUtils.columnContains(sampleRecord, 3, "eni", 1));

        // Modo 2: Empieza por
        assertTrue(CSVUtils.columnContains(sampleRecord, 1, "Ago", 2));

        // Modo 3: Termina por
        assertTrue(CSVUtils.columnContains(sampleRecord, 1, "ost", 3));
    }

    @Test
    void testColumnContainsArrayOfValues() {
        String[] searchValues = {"Valencia", "Alicante", "Castellón"};
        assertTrue(CSVUtils.columnContains(sampleRecord, 2, searchValues, 0));

        String[] missingValues = {"Madrid", "Barcelona"};
        assertFalse(CSVUtils.columnContains(sampleRecord, 2, missingValues, 0));
    }

    @Test
    void testRowContainsInSpecificPositions() {
        Integer[] positions = {0, 2}; // Evaluamos solo las columnas "03" y "Alicante"
        assertTrue(CSVUtils.rowContains(sampleRecord, "03", positions, 0));
        assertFalse(CSVUtils.rowContains(sampleRecord, "Agost", positions, 0)); // Agost está en la posición 1
    }

    @Test
    void testRowContainsInAllColumns() {
        assertTrue(CSVUtils.rowContains(sampleRecord, "agost", 0));
        assertFalse(CSVUtils.rowContains(sampleRecord, "Inexistente", 0));
    }

    @Test
    void testJoinColumns() {
        Integer[] targets = {3, 1}; // "Avenida" y "Agost"
        String template = "Tipo: $1, Municipio: $2";

        String result = CSVUtils.joinColumns(sampleRecord, targets, template, "N/A");

        assertEquals("Tipo: Avenida, Municipio: Agost", result);
    }

    @Test
    void testCopyIncludingColumns() throws IOException {
        StringWriter sw = new StringWriter();
        Integer[] includePositions = {0, 2}; // Solo "03" y "Alicante"

        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            CSVUtils.copyIncludingColumns(sampleRecord, printer, includePositions);
        }

        assertEquals("03,Alicante\r\n", sw.toString());
    }

    @Test
    void testCopyExcludingColumns() throws IOException {
        StringWriter sw = new StringWriter();
        Integer[] excludePositions = {1, 3}; // Excluimos "Agost" y "Avenida"

        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            CSVUtils.copyExcludingColumns(sampleRecord, printer, excludePositions);
        }

        assertEquals("03,Alicante\r\n", sw.toString());
    }
}