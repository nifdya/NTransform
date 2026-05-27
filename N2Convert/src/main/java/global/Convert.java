package global;

import java.io.File;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import convert.CsvExcelConverter;
// Importamos la otra clase conversora si está en un paquete distinto
import convert.XlsXlsxConverter; 

@Command(
    name = "convert", 
    mixinStandardHelpOptions = true, 
    version = "1.0",
    description = "Transforma datos entre formatos CSV y Excel."
)
public class Convert implements Callable<Integer> {

    @Option(names = { "-i", "--input" }, required = true, description = "Ruta del archivo de entrada.")
    private File inputFile;

    @Option(names = { "-o", "--output" }, required = true, description = "Ruta del archivo de salida.")
    private File outputFile;

    @Option(names = {"-t", "--task"}, 
            description = "Operación a realizar. Valores válidos: ${COMPLETION-CANDIDATES}",
            required = true)
    private Task task; // Vinculado correctamente al Enum de abajo

    @Override
    public Integer call() {
        try {
            // Validaciones de infraestructura
            if (!inputFile.exists()) {
                System.err.println("❌ El archivo de entrada no existe.");
                return 1;
            }

            String inputPath = inputFile.getAbsolutePath();
            String outputPath = outputFile.getAbsolutePath();

            // EL SWITCH DEBE USAR LOS VALORES DEL ENUM DIRECTAMENTE (SIN COMILLAS "")
            switch (task) {
                case XLS2XLSX:
                    System.out.println("🔄 Convirtiendo XLS viejo a XLSX moderno...");
                    XlsXlsxConverter xlsConverter = new XlsXlsxConverter();
                    xlsConverter.xlsToXlsx(inputPath, outputPath);
                    break;

                case CSV2XLSX:
                    System.out.println("🔄 Convirtiendo CSV a XLSX...");
                    CsvExcelConverter.csvToXlsx(inputPath, outputPath);
                    break;

                case XLSX2CSV:
                    System.out.println("🔄 Convirtiendo XLSX a CSV...");
                    CsvExcelConverter.xlsxToCsv(inputPath, outputPath);
                    break;
            }

            System.out.println("🚀 ¡Operación completada con éxito!");
            return 0;

        } catch (Exception e) {
            System.err.println("❌ Error en la cadena de transformación: " + e.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Convert());
        // Esto permite que si el usuario escribe -t xls2xlsx (minúscula) funcione igual
        cmd.setCaseInsensitiveEnumValuesAllowed(true); 
        
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
