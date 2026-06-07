package record;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MapDefinitionsTextPos {

    /**
     * PROCESO CENTRALIZADO: Lee el XML de definiciones y construye el mapa indexado por tipos.
     * Hecho público para que cualquier módulo (Procesamiento o Conversión) pueda inicializar sus layouts.
     */
    public static Map<String, RecordDefinitionTextPos> getDefinitions(String defFile) {
        Map<String, RecordDefinitionTextPos> mapDefinitions = new HashMap<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(new File(defFile));
            NodeList records = doc.getElementsByTagName("record");

            for (int i = 0; i < records.getLength(); i++) {
                Element record = (Element) records.item(i);

                String type = record.getAttribute("type");
                String posTypeStr = record.getAttribute("posType");
                String lengthStr = record.getAttribute("length");

                // CLAVE ÚNICA: Si no viene tipo en el XML, usamos estrictamente "default"
                String claveMap = (type != null && !type.isEmpty()) ? type : "default";

                int posType = posTypeStr.isEmpty() ? 0 : Integer.parseInt(posTypeStr);
                int lengthType = lengthStr.isEmpty() ? 0 : Integer.parseInt(lengthStr);

                RecordDefinitionTextPos def = new RecordDefinitionTextPos(claveMap, posType, lengthType);

                NodeList fields = record.getElementsByTagName("field");
                for (int j = 0; j < fields.getLength(); j++) {
                    Element field = (Element) fields.item(j);
                    int length = Integer.parseInt(field.getAttribute("length"));
                    boolean ignore = "true".equals(field.getAttribute("ignore"));
                    def.addField(length, ignore);
                }

                mapDefinitions.put(claveMap, def);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cargando definiciones de ancho fijo: " + e.getMessage(), e);
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
