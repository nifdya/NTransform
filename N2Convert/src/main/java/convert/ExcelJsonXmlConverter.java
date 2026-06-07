package convert;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ExcelJsonXmlConverter {

    public static void excelToJson(String xlsxPath, String jsonPath) throws IOException {
        JsonFactory factory = new JsonFactory();
        try (InputStream fileIn = new FileInputStream(xlsxPath);
             Workbook workbook = new XSSFWorkbook(fileIn);
             JsonGenerator jg = factory.createGenerator(new File(jsonPath), com.fasterxml.jackson.core.JsonEncoding.UTF8)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            jg.useDefaultPrettyPrinter();
            jg.writeStartArray(); // [

            for (Row row : sheet) {
                jg.writeStartObject(); // {

                int cellIdx = 0;
                for (Cell cell : row) {
                    // Lee la celda de Excel exactamente con el formato visual que tiene
                    String cellValue = formatter.formatCellValue(cell).trim();
                    jg.writeStringField("p" + cellIdx, cellValue);
                    cellIdx++;
                }
                jg.writeEndObject(); // }
            }
            jg.writeEndArray(); // ]
        }
    }

    /**
     * CONVERSIÓN: EXCEL -> XML (Estructura pura y directa)
     */
    public static void excelToXml(String xlsxPath, String xmlPath) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("records");
        doc.appendChild(root);

        try (InputStream fileIn = new FileInputStream(xlsxPath);
             Workbook workbook = new XSSFWorkbook(fileIn)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                Element recordNode = doc.createElement("record");

                int cellIdx = 0;
                for (Cell cell : row) {
                    String cellValue = formatter.formatCellValue(cell).trim();
                    Element fieldNode = doc.createElement("p" + cellIdx);
                    fieldNode.setTextContent(cellValue);
                    recordNode.appendChild(fieldNode);
                    cellIdx++;
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
