package convert;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import record.RecordDefinitionTextPos;

public class XmlToAllConverter {

    private final Charset charset;
    private final String delimiter = ",";

    public XmlToAllConverter(Charset charset) {
        this.charset = charset;
    }

    /**
     * CONVERSIÓN INVERSA: XML -> TEXTO POSICIONAL 
     * (Único método que requiere mapeo de longitudes físicas)
     */
    public void xmlToTxtPos(String xmlPath, String txtPath, Map<String, RecordDefinitionTextPos> mapaDefiniciones) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xmlPath));
        NodeList records = doc.getElementsByTagName("record");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(txtPath, charset))) {
            for (int i = 0; i < records.getLength(); i++) {
                Element recordNode = (Element) records.item(i);
                String tipo = recordNode.hasAttribute("type") ? recordNode.getAttribute("type") : "default";
                RecordDefinitionTextPos def = mapaDefiniciones.get(tipo);
                if (def == null) continue;

                List<Integer> longitudes = def.getLongitudes();
                List<Boolean> ignorados = def.getIgnorados();
                StringBuilder sbLine = new StringBuilder();
                int fieldIdx = 0;

                for (int j = 0; j < longitudes.size(); j++) {
                    int targetLen = longitudes.get(j);
                    String rawValue = "";

                    if (ignorados == null || j >= ignorados.size() || !ignorados.get(j)) {
                        NodeList fields = recordNode.getElementsByTagName("p" + fieldIdx);
                        if (fields.getLength() > 0) {
                            rawValue = fields.item(0).getTextContent().trim();
                        }
                        fieldIdx++;
                    }

                    if (rawValue.length() > targetLen) {
                        sbLine.append(rawValue.substring(0, targetLen));
                    } else {
                        sbLine.append(String.format("%-" + targetLen + "s", rawValue));
                    }
                }
                writer.write(sbLine.toString());
                writer.newLine();
            }
        }
    }

    /**
     * CONVERSIÓN INVERSA: XML -> CSV (Estructura directa sin definiciones)
     */
    public void xmlToCsv(String xmlPath, String csvPath) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xmlPath));
        NodeList records = doc.getElementsByTagName("record");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath, charset))) {
            for (int i = 0; i < records.getLength(); i++) {
                Element recordNode = (Element) records.item(i);
                java.util.List<String> tokens = new java.util.ArrayList<>();
                
                int fieldIdx = 0;
                NodeList fields;
                while ((fields = recordNode.getElementsByTagName("p" + fieldIdx)).getLength() > 0) {
                    String value = fields.item(0).getTextContent().trim();
                    if (value.contains(delimiter) || value.contains("\"")) {
                        value = "\"" + value.replace("\"", "\"\"") + "\"";
                    }
                    tokens.add(value);
                    fieldIdx++;
                }

                writer.write(String.join(delimiter, tokens));
                writer.newLine();
            }
        }
    }

    /**
     * CONVERSIÓN INVERSA: XML -> EXCEL (Estructura directa sin definiciones)
     */
    public void xmlToExcel(String xmlPath, String xlsxPath) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(xmlPath));
        NodeList records = doc.getElementsByTagName("record");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Datos desde XML");
            int rowNum = 0;

            for (int i = 0; i < records.getLength(); i++) {
                Element recordNode = (Element) records.item(i);
                Row row = sheet.createRow(rowNum++);
                
                int cellIdx = 0;
                NodeList fields;
                while ((fields = recordNode.getElementsByTagName("p" + cellIdx)).getLength() > 0) {
                    Cell cell = row.createCell(cellIdx);
                    cell.setCellValue(fields.item(0).getTextContent().trim());
                    cellIdx++;
                }
            }

            if (sheet.getRow(0) != null) {
                for (int col = 0; col < sheet.getRow(0).getLastCellNum(); col++) {
                    sheet.autoSizeColumn(col);
                }
            }

            try (FileOutputStream fileOut = new FileOutputStream(xlsxPath)) {
                workbook.write(fileOut);
            }
        }
    }
}
