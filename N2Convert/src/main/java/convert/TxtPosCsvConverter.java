package convert;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import record.MapDefinitionsTextPos; // Importamos el resolvedor centralizado
import record.RecordDefinitionTextPos;

public class TxtPosCsvConverter {

    private final Map<String, RecordDefinitionTextPos> mapaDefiniciones;
    private final Charset charset;
    private final String delimiter;

    public TxtPosCsvConverter(Map<String, RecordDefinitionTextPos> mapaDefiniciones, Charset charset) {
        this(mapaDefiniciones, charset, ",");
    }

    public TxtPosCsvConverter(Map<String, RecordDefinitionTextPos> mapaDefiniciones, Charset charset, String delimiter) {
        this.mapaDefiniciones = mapaDefiniciones;
        this.charset = charset;
        this.delimiter = delimiter;
    }

    /**
     * PROCESO 1: TEXTO POSICIONAL -> CSV
     */
    public void txtPosToCsv(String txtPath, String csvPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(txtPath, charset));
             BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath, charset))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                // REUTILIZACIÓN GLOBAL: Invocamos de forma estática la lógica centralizada de tipado
                String tipo = MapDefinitionsTextPos.obtenerTipoDeLinea(line, this.mapaDefiniciones);
                RecordDefinitionTextPos def = this.mapaDefiniciones.get(tipo);
                if (def == null) continue;

                List<Integer> longitudes = def.getLongitudes();
                List<Boolean> ignorados = def.getIgnorados();
                List<String> tokens = new ArrayList<>();
                int currentPointer = 0;

                for (int i = 0; i < longitudes.size(); i++) {
                    if (currentPointer >= line.length()) break;
                    int length = longitudes.get(i);
                    int endPointer = Math.min(currentPointer + length, line.length());

                    if (ignorados == null || i >= ignorados.size() || !ignorados.get(i)) {
                        String value = line.substring(currentPointer, endPointer).trim();
                        if (value.contains(delimiter) || value.contains("\"")) {
                            value = "\"" + value.replace("\"", "\"\"") + "\"";
                        }
                        tokens.add(value);
                    }
                    currentPointer += length;
                }
                writer.write(String.join(delimiter, tokens));
                writer.newLine();
            }
        }
    }

    /**
     * PROCESO 2: CSV -> TEXTO POSICIONAL
     */
    public void csvToTxtPos(String csvPath, String txtPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath, charset));
             BufferedWriter writer = new BufferedWriter(new FileWriter(txtPath, charset))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                String[] columns = line.split(delimiter + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                
                for (int i = 0; i < columns.length; i++) {
                    if (columns[i].startsWith("\"") && columns[i].endsWith("\"")) {
                        columns[i] = columns[i].substring(1, columns[i].length() - 1).replace("\"\"", "\"");
                    }
                }

                // Identificamos el tipo mapeando de forma directa la primera columna del CSV estructurado
                String tipoCsv = "default";
                if (this.mapaDefiniciones.size() > 1 && columns.length > 0) {
                    String tipoExtraido = columns[0].trim();
                    if (this.mapaDefiniciones.containsKey(tipoExtraido)) {
                        tipoCsv = tipoExtraido;
                    }
                }

                RecordDefinitionTextPos def = this.mapaDefiniciones.get(tipoCsv);
                if (def == null) continue;

                List<Integer> longitudes = def.getLongitudes();
                List<Boolean> ignorados = def.getIgnorados();
                StringBuilder sbLine = new StringBuilder();
                int csvColIdx = 0;

                for (int i = 0; i < longitudes.size(); i++) {
                    int targetLen = longitudes.get(i);
                    String rawValue = "";

                    if (ignorados == null || i >= ignorados.size() || !ignorados.get(i)) {
                        if (csvColIdx < columns.length) {
                            rawValue = columns[csvColIdx].trim();
                            csvColIdx++;
                        }
                    }

                    if (rawValue.length() > targetLen) {
                        sbLine.append(rawValue.substring(0, targetLen));
                    } else {
                        sbLine.append(String.format("%-" + targetLen + "s", rawValue));
                    }
                }
                sbLine.append(System.lineSeparator());
                writer.write(sbLine.toString());
            }
        }
    }
}
