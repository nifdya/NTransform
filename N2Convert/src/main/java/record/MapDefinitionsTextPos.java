package record;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MapDefinitionsTextPos {

    /**
     * PROCESO CENTRALIZADO: Lee el JSON de definiciones y construye el mapa indexado por tipos.
     * Hecho público para que cualquier módulo (Procesamiento o Conversión) pueda inicializar sus layouts.
     */
    public static Map<String, RecordDefinitionTextPos> getDefinitions(String defFile) {
        Map<String, RecordDefinitionTextPos> mapDefinitions = new HashMap<>();
        try {
            // Instanciamos el motor de Jackson para procesar el JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(defFile));
            
            // Accedemos a la lista raíz de definiciones (Array "definitions")
            JsonNode records = rootNode.path("definitions");

            if (records.isArray()) {
                for (JsonNode record : records) {

                    // Extraemos propiedades de forma segura (con valores por defecto si no existen)
                    String type = record.path("type").asText(null);
                    int posType = record.path("posType").asInt(0);
                    int lengthType = record.path("length").asInt(0);

                    // CLAVE ÚNICA: Si no viene tipo en el JSON, usamos estrictamente "default"
                    String claveMap = (type != null && !type.isEmpty()) ? type : "default";

                    RecordDefinitionTextPos def = new RecordDefinitionTextPos(claveMap, posType, lengthType);

                    // Procesamos la lista interna de campos (Array "fields")
                    JsonNode fields = record.path("fields");
                    if (fields.isArray()) {
                        for (JsonNode field : fields) {
                            int length = field.path("length").asInt(0);
                            boolean ignore = field.path("ignore").asBoolean(false);
                            
                            def.addField(length, ignore);
                        }
                    }

                    mapDefinitions.put(claveMap, def);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cargando definiciones de ancho fijo desde JSON: " + e.getMessage(), e);
        }
        return mapDefinitions;
    }

    /**
     * MÉTODO COMPARTIDO DE RESOLUCIÓN: Analiza una línea física y devuelve el tipo correspondiente.
     * Al estar centralizado aquí, tus convertidores de CSV, Excel, JSON y XML pueden usarlo de forma homogénea.
     */
    public static String obtenerTipoDeLinea(String linea, Map<String, RecordDefinitionTextPos> mapaDefiniciones) {
        // 1. Si el mapa tiene una única estructura, evitamos el substring (Eficiencia máxima)
        if (mapaDefiniciones == null || mapaDefiniciones.size() <= 1) {
            return "default";
        }

        // 2. Extraemos las coordenadas usando el registro de configuración base
        RecordDefinitionTextPos ref = mapaDefiniciones.get("default");

        if (ref != null && ref.getLengthType() > 0) {
            int inicio = ref.getPosType() - 1; // Ajuste base 0 de Java
            int fin = inicio + ref.getLengthType();

            if (linea.length() >= fin) {
                String tipoExtraido = linea.substring(inicio, fin);
                // Si el código extraído de la línea está registrado, lo devolvemos
                if (mapaDefiniciones.containsKey(tipoExtraido)) {
                    return tipoExtraido;
                }
            }
        }

        // 3. Fallback absoluto de la aplicación ante líneas corruptas o sin match
        return "default";
    }
}
