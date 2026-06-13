package convert;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.github.pjfanning.xlsx.StreamingReader; // <-- LECTOR EN STREAMING
import org.apache.poi.ss.usermodel.*;
import java.io.*;
import javax.xml.stream.XMLOutputFactory; // <-- STAX PARA XML EN STREAMING
import javax.xml.stream.XMLStreamWriter;

public class ExcelJsonXmlConverter {

    /**
     * CONVERSIÓN: EXCEL -> JSON (OPTIMIZADO PARA MEMORIA)
     * Combina el streaming de lectura de Excel con el streaming de escritura de Jackson.
     */
    public static void excelToJson(String xlsxPath, String jsonPath) throws IOException {
        JsonFactory factory = new JsonFactory();
        
        try (InputStream fileIn = new FileInputStream(xlsxPath);
             // Cambiado XSSFWorkbook por StreamingReader para no saturar la RAM
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(100)
                     .bufferSize(4096)
                     .open(fileIn);
             JsonGenerator jg = factory.createGenerator(new File(jsonPath), com.fasterxml.jackson.core.JsonEncoding.UTF8)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            jg.useDefaultPrettyPrinter();
            jg.writeStartArray(); // [

            for (Row row : sheet) {
                jg.writeStartObject(); // {

                int cellIdx = 0;
                for (Cell cell : row) {
                    if (cell != null) {
                        // En StreamingReader, evaluamos de forma segura la celda
                        String cellValue = formatter.formatCellValue(cell).trim();
                        jg.writeStringField("p" + cellIdx, cellValue);
                    } else {
                        jg.writeStringField("p" + cellIdx, "");
                    }
                    cellIdx++;
                }
                jg.writeEndObject(); // }
            }
            jg.writeEndArray(); // ]
        }
    }

    /**
     * CONVERSIÓN: EXCEL -> XML (OPTIMIZADO AL 100% CONTRA ERRORES DE MEMORIA)
     * Sustituye DOM por StAX (XMLStreamWriter), enviando las etiquetas al disco de forma inmediata.
     */
    public static void excelToXml(String xlsxPath, String xmlPath) throws Exception {
        // Inicializamos la factoría de StAX (API nativa de Java, no requiere librerías extra)
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        
        try (InputStream fileIn = new FileInputStream(xlsxPath);
             Workbook workbook = StreamingReader.builder()
                     .rowCacheSize(100)
                     .bufferSize(4096)
                     .open(fileIn);
             FileWriter fileWriter = new FileWriter(xmlPath)) {

            XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(fileWriter);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Inicio del documento XML
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeCharacters("\n");
            xmlWriter.writeStartElement("records"); // <records>
            xmlWriter.writeCharacters("\n");

            for (Row row : sheet) {
                xmlWriter.writeCharacters("    "); // Indentación manual para mantenerlo bonito
                xmlWriter.writeStartElement("record"); // <record>
                xmlWriter.writeCharacters("\n");

                int cellIdx = 0;
                for (Cell cell : row) {
                    String cellValue = "";
                    if (cell != null) {
                        cellValue = formatter.formatCellValue(cell).trim();
                    }
                    
                    // Escribe la etiqueta de la celda de forma secuencial: <p0>valor</p0>
                    xmlWriter.writeCharacters("        ");
                    xmlWriter.writeStartElement("p" + cellIdx);
                    xmlWriter.writeCharacters(cellValue);
                    xmlWriter.writeEndElement(); 
                    xmlWriter.writeCharacters("\n");
                    
                    cellIdx++;
                }
                
                xmlWriter.writeCharacters("    ");
                xmlWriter.writeEndElement(); // </record>
                xmlWriter.writeCharacters("\n");
            }

            xmlWriter.writeEndElement(); // </records>
            xmlWriter.writeCharacters("\n");
            xmlWriter.writeEndDocument();

            // Cerramos el flujo del escritor XML
            xmlWriter.flush();
            xmlWriter.close();
        }
    }
}
