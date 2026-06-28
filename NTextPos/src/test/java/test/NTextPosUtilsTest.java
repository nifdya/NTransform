package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import global.NTextPosUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NTextPosUtilsTest {

    @Mock
    private BufferedWriter writer;

    private Map<String, Object> sampleRecord;
    private List<Integer> sampleLengths;

    @BeforeEach
    void setUp() {
        sampleRecord = new HashMap<>();
        sampleRecord.put("p0", "Juan");
        sampleRecord.put("p1", "Perez");
        sampleRecord.put("p2", 30);

        sampleLengths = Arrays.asList(6, 6, 4); // Total esperado: 16 caracteres
    }

    @Test
    void testCopyRowWithValidData() throws IOException {
        NTextPosUtils.copyRow(sampleRecord, writer, sampleLengths);

        // "Juan  " (6) + "Perez " (6) + "30  " (4)
        verify(writer).write("Juan  Perez 30  ");
        verify(writer).newLine();
    }

    @Test
    void testCopyRowTruncatingLongValues() throws IOException {
        sampleRecord.put("p0", "Alejandro"); // Longitud 9, debe truncarse a 6
        NTextPosUtils.copyRow(sampleRecord, writer, sampleLengths);

        verify(writer).write("AlejanPerez 30  ");
    }

    @Test
    void testCopyRowWithNullRecordDoesNothing() throws IOException {
        NTextPosUtils.copyRow(null, writer, sampleLengths);
        verifyNoInteractions(writer);
    }

    @Test
    void testIsRowEmpty() {
        assertTrue(NTextPosUtils.isRowEmpty(null));
        assertTrue(NTextPosUtils.isRowEmpty(new HashMap<>()));

        Map<String, Object> emptyFieldsRecord = new HashMap<>();
        emptyFieldsRecord.put("p0", "   ");
        emptyFieldsRecord.put("p1", null);
        assertTrue(NTextPosUtils.isRowEmpty(emptyFieldsRecord));

        assertFalse(NTextPosUtils.isRowEmpty(sampleRecord));
    }

    @Test
    void testCopyRowWithRegexReplacement() throws IOException {
        sampleRecord.put("p0", "Juan123");
        // p1 ya es "Perez" (sin dígitos)
        sampleRecord.put("p2", 30); // Contiene dígitos, cambiará a XX
        
        sampleLengths = Arrays.asList(8, 6, 4);

        // Reemplaza todos los dígitos por X en toda la fila
        NTextPosUtils.copyRow(sampleRecord, writer, "\\d", "X", sampleLengths);

        // Explicación del resultado esperado:
        // p0: "Juan123" -> "JuanXXX " (8 caracteres)
        // p1: "Perez"   -> "Perez "   (6 caracteres)
        // p2: 30        -> "XX  "     (4 caracteres, el 30 se convierte en XX)
        verify(writer).write("JuanXXX Perez XX  ");
        verify(writer).newLine();
    }

    @Test
    void testColumnContainsSingleValue() {
        // Modo 0: Equals exacto (ignora mayúsculas en la lógica del método)
        assertTrue(NTextPosUtils.columnContains(sampleRecord, 0, "juan", 0));
        assertFalse(NTextPosUtils.columnContains(sampleRecord, 0, "Jua", 0));

        // Modo 1: Contains
        assertTrue(NTextPosUtils.columnContains(sampleRecord, 0, "ua", 1));

        // Modo 2: StartsWith
        assertTrue(NTextPosUtils.columnContains(sampleRecord, 1, "per", 2));

        // Modo 3: EndsWith
        assertTrue(NTextPosUtils.columnContains(sampleRecord, 1, "rez", 3));
    }

    @Test
    void testColumnContainsInvalidModeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            NTextPosUtils.columnContains(sampleRecord, 0, "Juan", 99);
        });
    }

    @Test
    void testColumnContainsMultipleValues() {
        String[] searchValues = {"Carlos", "Juan", "Pedro"};
        assertTrue(NTextPosUtils.columnContains(sampleRecord, 0, searchValues, 0));

        String[] missingValues = {"Carlos", "Pedro"};
        assertFalse(NTextPosUtils.columnContains(sampleRecord, 0, missingValues, 0));
    }

    @Test
    void testColumnContainsMultiplePositionsAndValues() {
        Integer[] positions = {0, 1};
        String[] searchValues = {"Perez", "Inexistente"};
        
        assertTrue(NTextPosUtils.columnContains(sampleRecord, positions, searchValues, 0));
    }

    @Test
    void testRowContainsInSpecificPositions() {
        Integer[] positions = {0, 2}; // Busca en Juan y 30
        assertTrue(NTextPosUtils.rowContains(sampleRecord, "30", positions, 0));
        assertFalse(NTextPosUtils.rowContains(sampleRecord, "Perez", positions, 0));
    }

    @Test
    void testRowContainsInAllColumns() {
        assertTrue(NTextPosUtils.rowContains(sampleRecord, "perez", 0));
        assertFalse(NTextPosUtils.rowContains(sampleRecord, "Inexistente", 0));
    }

    @Test
    void testJoinColumns() {
        Integer[] targets = {0, 1};
        String template = "Empleado: $1, Apellido: $2";
        
        String result = NTextPosUtils.joinColumns(sampleRecord, targets, template, "N/A");
        
        assertEquals("Empleado: Juan, Apellido: Perez", result);
    }

    @Test
    void testJoinColumnsWithMissingKeysUsesDefaultValue() {
        Integer[] targets = {0, 99}; // 99 no existe en el mapa
        String template = "$1 - $2";
        
        String result = NTextPosUtils.joinColumns(sampleRecord, targets, template, "VACIO");
        
        assertEquals("Juan - VACIO", result);
    }

    @Test
    void testCopyIncludingColumns() throws IOException {
        Integer[] includePositions = {0, 2}; // Incluye Juan (p0) y 30 (p2). Borra Perez (p1)
        
        NTextPosUtils.copyIncludingColumns(sampleRecord, writer, includePositions, sampleLengths);

        // "Juan  " (6) + "      " (6 espacios vacíos) + "30  " (4)
        verify(writer).write("Juan        30  ");
        verify(writer).newLine();
    }
}
