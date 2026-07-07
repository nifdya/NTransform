package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import global.NTextPos;
import picocli.CommandLine;
import record.RecordDefinition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NTextPosTest {

	 @TempDir
	    Path tempDir;

	    private Path jsonDefFile;
	    private Path inputFile;
	    private Path outputFile;

	    @BeforeEach
	    void setUp() throws IOException {
	        // 1. JSON real con la estructura de campos que enviaste
	        String jsonReal = "{\n"
	                + "  \"definitions\": [\n"
	                + "    {\n"
	                + "      \"name\": \"linea1\",\n"
	                + "      \"fields\": [\n"
	                + "        { \"name\": \"codProv\", \"length\": 2, \"label\": \"Código Provincia\" },\n"
	                + "        { \"name\": \"codMun\", \"length\": 3, \"label\": \"Código Municipio\" },\n"
	                + "        { \"name\": \"codVia\", \"length\": 5, \"label\": \"Código Vía\" },\n"
	                + "        { \"name\": \"nombre\", \"length\": 30, \"label\": \"Nombre Corto\" },\n"
	                + "        { \"name\": \"fechRef\", \"length\": 8, \"label\": \"Fecha Referencia\" }\n"
	                + "      ]\n"
	                + "    }\n"
	                + "  ]\n"
	                + "}";
	        jsonDefFile = tempDir.resolve("vias.json");
	        Files.writeString(jsonDefFile, jsonReal);

	        // 2. Fichero de entrada simulado en formato ISO-8859-1 (como en tu comando)
	        // Fila 1: codProv="28", codMun="03 " (Cumple la condición: columna 1 exacta es "03 ")
	        // Fila 2: codProv="08", codMun="079" (No la cumple)
	        // Nota: Como la longitud de codMun es 3, al escribir "03" el sistema posicional añade un espacio "03 "
	        String inputContent = "0300100002FUENTE (LA)                 20251231 0 002CALLEFUENTE (LA)\n"
                    + "0801900123Paseo de Gracia                 20261025\n";


	        inputFile = tempDir.resolve("vias.txt");
	        Files.writeString(inputFile, inputContent, java.nio.charset.Charset.forName("ISO-8859-1"));

	        outputFile = tempDir.resolve("vias_3.txt");
	    }

	    @Test
	    void testCallWithRealObtenerFilaColumnaContieneTask() throws IOException {
	        NTextPos nTextPos = new NTextPos();
	        CommandLine cmd = new CommandLine(nTextPos);

	        // Tu argumento de comando real exacto
	        String taskArgument = "ObtenerFilaColumnaContiene|colPositions=1|rowText=03|mode=0";
	        String traceFile = tempDir.resolve("traza_xlsx.txt").toString();

	        // Ejecutamos la simulación del comando completo a través de Picocli
	        int exitCode = cmd.execute(
	            "-i", inputFile.toString(),
	            "-o", outputFile.toString(),
	            "-d", jsonDefFile.toString(),
	            "-c", "ISO-8859-1",
	            "-t", taskArgument
	        );

	        // 1. Validamos que el código de salida sea 0 (Operación completada con éxito)
	        assertEquals(0, exitCode, "El comando debería haber finalizado con código 0");

	        // 2. Validamos que el archivo de salida se haya creado
	        assertTrue(Files.exists(outputFile), "El archivo de salida debería existir");
	     // 3. Comprobación del resultado de la tarea (UnitaryTransformations):
	        List<String> lineasSalida = Files.readAllLines(outputFile, java.nio.charset.Charset.forName("ISO-8859-1"));
	        
	        // Eliminamos posibles líneas vacías o residuos que genere el escritor al final del fichero
	        lineasSalida.removeIf(String::isBlank);
	        
	        // Ahora sí, validamos de forma segura el contenido
	        assertFalse(lineasSalida.isEmpty(), "El archivo de salida no debería estar vacío");
	        assertEquals(1, lineasSalida.size(), "Debería quedar exactamente 1 fila válida filtrada");
	        assertTrue(lineasSalida.get(0).startsWith("03001"), "La línea resultante debe empezar por el código de Alicante");
    
	        }
}