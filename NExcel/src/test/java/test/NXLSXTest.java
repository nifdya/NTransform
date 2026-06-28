package test;


import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import global.NXLSX;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NXLSXTest {

    @TempDir
    Path tempDir;

    private File excelInputFile;
    private File excelOutputFile;

    @BeforeEach
    void setUp() throws IOException {
        // 1. Creamos las rutas físicas dentro del directorio temporal de JUnit 5
        excelInputFile = tempDir.resolve("vias_input.xlsx").toFile();
        excelOutputFile = tempDir.resolve("vias_output.xlsx").toFile();

        // 2. Fabricamos un archivo Excel real válido con Apache POI para que el comando pueda leerlo
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelInputFile)) {
            
            XSSFSheet sheet = workbook.createSheet("Hoja1");
            // Fila 0 (Cabeceras o datos base)
            var row = sheet.createRow(0);
            row.createCell(0).setCellValue("03");    // codProv (Alicante)
            row.createCell(1).setCellValue("001");   // codMun
            row.createCell(2).setCellValue("00002"); // codVia
            row.createCell(3).setCellValue("FUENTE (LA)");
            
            // Fila 1 (Otra fila de muestra)
            var row2 = sheet.createRow(1);
            row2.createCell(0).setCellValue("08");
            row2.createCell(1).setCellValue("019");
            row2.createCell(2).setCellValue("00123");
            row2.createCell(3).setCellValue("Paseo de Gracia");

            workbook.write(fos);
        }
    }

    @Test
    void testCommandLineBindingAndMapeoPicocli() {
        NXLSX nxlsx = new NXLSX();
        CommandLine cmd = new CommandLine(nxlsx);

        // Validamos que Picocli reconoce y asume los argumentos sin ejecutar la lógica del método call()
        cmd.parseArgs(
            "-i", excelInputFile.getAbsolutePath(),
            "-o", excelOutputFile.getAbsolutePath(),
            "-t", "ObtenerFilaColumnaContiene|rowPositions=1|rowText=03|mode=0"
        );

        CommandLine.ParseResult result = cmd.getParseResult();
        assertNotNull(result);
        assertEquals(excelInputFile.getAbsolutePath(), ((File) result.matchedOption("i").getValue()).getAbsolutePath());
        assertEquals(excelOutputFile.getAbsolutePath(), ((File) result.matchedOption("o").getValue()).getAbsolutePath());
    }

    @Test
    void testCallReturnsOneWhenNoTasksProvided() {
        NXLSX nxlsx = new NXLSX();
        CommandLine cmd = new CommandLine(nxlsx);

        // Al no proveer el parámetro '-t' (listTaskInCommand será null), el bucle for no se ejecutará.
        // Si tu programa arroja una NullPointerException en el bucle 'for (String iTask : listTaskInCommand)',
        // el catch la capturará e imprimirá la traza regresando un código de salida 1.
        int exitCode = cmd.execute(
            "-i", excelInputFile.getAbsolutePath(),
            "-o", excelOutputFile.getAbsolutePath()
        );

        assertEquals(1, exitCode, "Debe retornar código de error 1 al no pasarle tareas");
    }

    @Test
    void testCallExecutionWithInvalidExcelThrowsException() throws IOException {
        // Corrompemos el archivo de entrada escribiendo texto plano en lugar de la estructura binaria de un zip/xlsx
        Files.writeString(excelInputFile.toPath(), "Fichero corrupto");

        NXLSX nxlsx = new NXLSX();
        CommandLine cmd = new CommandLine(nxlsx);

        int exitCode = cmd.execute(
            "-i", excelInputFile.getAbsolutePath(),
            "-o", excelOutputFile.getAbsolutePath(),
            "-t", "ObtenerFilaColumnaContiene|rowPositions=1|rowText=03|mode=0"
        );

        // El método 'loadIputWorkbook' lanzará un IllegalStateException al no poder parsear el archivo corrupto.
        // El bloque catch general de 'call()' interceptará la excepción y regresará un código 1.
        assertEquals(1, exitCode);
    }

    @Test
    void testCallSuccessFlow() {
        NXLSX nxlsx = new NXLSX();
        CommandLine cmd = new CommandLine(nxlsx);

        // Tu argumento de comando real utilizando la tubería '|'
        String taskArgument = "ObtenerFilaColumnaContiene|rowPositions=1|rowText=03|mode=0";

        int exitCode = cmd.execute(
            "-i", excelInputFile.getAbsolutePath(),
            "-o", excelOutputFile.getAbsolutePath(),
            "-t", taskArgument
        );

        // 1. Verificamos el código de terminación exitosa (0)
        assertEquals(0, exitCode, "El comando debería finalizar con código 0");

        // 2. Verificamos que el archivo Excel de salida se ha generado en el disco duro
        assertTrue(excelOutputFile.exists(), "El archivo Excel de salida debería haber sido creado");
        assertTrue(excelOutputFile.length() > 0, "El tamaño del archivo de salida debería ser mayor que cero");
    }
}