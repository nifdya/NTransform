package test;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import global.CSV;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CSVTest {

    @TempDir
    Path tempDir;

    private File csvInputFile;
    private File csvOutputFile;

    @BeforeEach
    void setUp() throws IOException {
        // 1. Configuramos las rutas físicas de los archivos dentro del directorio temporal de JUnit 5
        csvInputFile = tempDir.resolve("vias_input.csv").toFile();
        csvOutputFile = tempDir.resolve("vias_output.csv").toFile();

        // 2. Escribimos datos simulados en formato CSV usando el delimitador ';' estándar de tu aplicación
        String csvContent = "03;001;00002;FUENTE (LA);20251231\n"
                          + "08;019;00123;Paseo de Gracia;20261025\n";
        
        Files.writeString(csvInputFile.toPath(), csvContent, StandardCharsets.UTF_8);
    }

    @Test
    void testCommandLineBindingAndMapeoPicocli() {
        CSV csvCommand = new CSV();
        CommandLine cmd = new CommandLine(csvCommand);

        // Validamos que Picocli mapee correctamente las flags y opciones personalizadas como -dc o -f
        cmd.parseArgs(
            "-i", csvInputFile.getAbsolutePath(),
            "-o", csvOutputFile.getAbsolutePath(),
            "-dc", ";",
            "-f", // Activa firstLineHeaders
            "-k", // Activa keepHeaders
            "-t", "ObtenerFilaColumnaContiene|rowPositions=0|rowText=03|mode=0"
        );

        CommandLine.ParseResult result = cmd.getParseResult();
        assertNotNull(result);
        
        // Verificaciones de las opciones asignadas por la CLI
        assertEquals(csvInputFile.getAbsolutePath(), result.matchedOption("i").getValue());
        assertEquals(csvOutputFile.getAbsolutePath(), result.matchedOption("o").getValue());
        assertEquals(";", result.matchedOption("dc").getValue());
        assertTrue(result.hasMatchedOption("f"), "La opción -f debería estar activa");
        assertTrue(result.hasMatchedOption("k"), "La opción -k debería estar activa");
    }

    @Test
    void testCallReturnsOneWhenNoTasksProvided() {
        CSV csvCommand = new CSV();
        CommandLine cmd = new CommandLine(csvCommand);

        // Si se omiten las tareas, el bucle listTaskInCommand lanzará un NullPointerException capturado por el catch general
        int exitCode = cmd.execute(
            "-i", csvInputFile.getAbsolutePath(),
            "-o", csvOutputFile.getAbsolutePath()
        );

        assertEquals(1, exitCode, "Debería devolver código de error 1 si la lista de tareas no viene provista");
    }

    @Test
    void testCallExecutionWithInvalidFileReturnsOne() throws IOException {
        // Apuntamos a un archivo de entrada inexistente para provocar un escenario de error en la carga del stream
        File missingFile = tempDir.resolve("inexistente.csv").toFile();

        CSV csvCommand = new CSV();
        CommandLine cmd = new CommandLine(csvCommand);

        int exitCode = cmd.execute(
            "-i", missingFile.getAbsolutePath(),
            "-o", csvOutputFile.getAbsolutePath(),
            "-t", "ObtenerFilaColumnaContiene|rowPositions=0|rowText=03|mode=0"
        );

        assertEquals(1, exitCode, "Debería capturar el error de archivo faltante de forma segura regresando 1");
    }

    @Test
    void testCallSuccessFlow() throws IOException {
        CSV csvCommand = new CSV();
        CommandLine cmd = new CommandLine(csvCommand);

        // Formato exacto de tarea que espera recibir tu método split("\\|")
        String taskArgument = "ObtenerFilaColumnaContiene|rowPositions=1|rowText=03|mode=0";

        int exitCode = cmd.execute(
            "-i", csvInputFile.getAbsolutePath(),
            "-o", csvOutputFile.getAbsolutePath(),
            "-dc", ";",
            "-t", taskArgument,
            "-c", "UTF-8"
        );

        // 1. Validamos código de terminación correcto (0)
        assertEquals(0, exitCode, "El comando debería haber finalizado con éxito (código 0)");

        // 2. Validamos la creación física del archivo CSV de salida en el disco
        assertTrue(csvOutputFile.exists(), "El archivo de salida debería existir en el directorio");

        // 3. Comprobación flexible de líneas escritas
        List<String> lineasSalida = Files.readAllLines(csvOutputFile.toPath(), StandardCharsets.UTF_8);
        lineasSalida.removeIf(String::isBlank); // Limpieza de residuos de línea de la suite de test

        assertFalse(lineasSalida.isEmpty(), "El archivo CSV de salida no debería haber quedado vacío");
    }
}