package convert;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.*;
import java.nio.charset.Charset;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CsvJsonXmlConverter {

    private final Charset charset;
    private final String delimiter;

    // El constructor ahora solo pide el juego de caracteres, NADA de mapas
    public CsvJsonXmlConverter(Charset charset, String delimiter) {
        this.charset = charset;
        this.delimiter = delimiter;
    }

    /**
     * CONVERSIÓN: CSV -> JSON (Sin dependencias externas)
     */
    public void csvToJson(String csvPath, String jsonPath) throws IOException {
        JsonFactory factory = new JsonFactory();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath, charset));
             JsonGenerator jg = factory.createGenerator(new File(jsonPath), com.fasterxml.jackson.core.JsonEncoding.UTF8)) {

            jg.useDefaultPrettyPrinter();
            jg.writeStartArray(); // [

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                // Separamos por comas respetando celdas que tengan comillas dobles
                String[] columns = line.split(delimiter + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                jg.writeStartObject(); // {

                int fieldIdx = 0;
                for (String col : columns) {
                    // Limpieza de comillas de escape del CSV
                    String cleanVal = col.startsWith("\"") && col.endsWith("\"") 
                                      ? col.substring(1, col.length() - 1).replace("\"\"", "\"") 
                                      : col;
                    
                    // Genera propiedades p0, p1, p2 de forma secuencial y limpia
                    jg.writeStringField("p" + fieldIdx, cleanVal.trim());
                    fieldIdx++;
                }
                jg.writeEndObject(); // }
            }
            jg.writeEndArray(); // ]
        }
    }

    /**
     * CONVERSIÓN: CSV -> XML (Sin dependencias externas)
     */
    public void csvToXml(String csvPath, String xmlPath) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("records");
        doc.appendChild(root);

        try (BufferedReader reader = new BufferedReader(new FileReader(csvPath, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                String[] columns = line.split(delimiter + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                Element recordNode = doc.createElement("record");

                int fieldIdx = 0;
                for (String col : columns) {
                    String cleanVal = col.startsWith("\"") && col.endsWith("\"") 
                                      ? col.substring(1, col.length() - 1).replace("\"\"", "\"") 
                                      : col;
                    
                    Element fieldNode = doc.createElement("p" + fieldIdx);
                    fieldNode.setTextContent(cleanVal.trim());
                    recordNode.appendChild(fieldNode);
                    fieldIdx++;
                }
                root.appendChild(recordNode);
            }
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://apache.org}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(new File(xmlPath)));
    }
}
