package convert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import record.RecordDefinitionTextPos;

/**
 * Clase para la conversión de los ficheros de formato JSON a cualquiera de los
 * otros formatos. En estos momentos el resto de formatos (formatos destino)
 * son: CSV, TXT por posiciones y XSLX
 * 
 * @version 1.0
 */
public class JsonToAllConverter {

	private final Charset charset;
	private final String delimiter = ";";

	/**
	 * Constructor básico para conversiones que NO requieren posiciones fijas
	 * 
	 * @param charset - Charset a utilizar en los ficheros
	 */
	public JsonToAllConverter(Charset charset) {
		this.charset = charset;
	}

	/**
	 * Conversión de JSON a Fichero de texto por posiciones.
	 * 
	 * @param jsonPath         - Ruta del fichero JSON a convertir
	 * @param txtPath          - Ruta del fichero TXT final, el fichero convertido
	 * @param mapaDefiniciones - Los ficheros TXT por posiciones requieren de un
	 *                         fichero de definiciones, en formato json
	 * @throws Exception
	 */
	public void jsonToTxtPos(String jsonPath, String txtPath, Map<String, RecordDefinitionTextPos> mapaDefiniciones)
			throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		try (InputStream is = new FileInputStream(jsonPath);
				BufferedWriter writer = new BufferedWriter(new FileWriter(txtPath, charset))) {

			JsonNode rootArray = mapper.readTree(is);
			if (!rootArray.isArray())
				return;

			for (JsonNode rowNode : rootArray) {
				String tipo = rowNode.has("recordType") ? rowNode.get("recordType").asText() : "default";
				RecordDefinitionTextPos def = mapaDefiniciones.get(tipo);
				if (def == null)
					continue;

				List<Integer> longitudes = def.getLongitudes();
				List<Boolean> ignorados = def.getIgnorados();
				StringBuilder sbLine = new StringBuilder();
				int fieldIdx = 0;

				for (int i = 0; i < longitudes.size(); i++) {
					int targetLen = longitudes.get(i);
					String rawValue = "";

					if (ignorados == null || i >= ignorados.size() || !ignorados.get(i)) {
						String key = "p" + fieldIdx;
						if (rowNode.has(key)) {
							rawValue = rowNode.get(key).asText().trim();
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
	 * Conversión de JSON a CSV.
	 * 
	 * @param jsonPath - Ruta del fichero JSON a convertir.
	 * @param csvPath  - Ruta del fichero CSV final, el fichero convertido.
	 * @throws Exception
	 */
	public void jsonToCsv(String jsonPath, String csvPath) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		try (InputStream is = new FileInputStream(jsonPath);
				BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath, charset))) {

			JsonNode rootArray = mapper.readTree(is);
			if (!rootArray.isArray())
				return;

			for (JsonNode rowNode : rootArray) {
				List<String> tokens = new java.util.ArrayList<>();
				int fieldIdx = 0;

				// Lee dinámicamente lo que venga en el JSON de forma secuencial
				while (rowNode.has("p" + fieldIdx)) {
					String value = rowNode.get("p" + fieldIdx).asText().trim();
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
	 * Conversión de JSON a formato XLSX.
	 * 
	 * @param jsonPath - Ruta del fichero JSON a convertir.
	 * @param xlsxPath - Ruta del fichero xlsx final, el fichero convertido.
	 * @throws Exception
	 */
	public void jsonToExcel(String jsonPath, String xlsxPath) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		try (InputStream is = new FileInputStream(jsonPath); Workbook workbook = new XSSFWorkbook()) {

			Sheet sheet = workbook.createSheet("Datos desde JSON");
			JsonNode rootArray = mapper.readTree(is);
			if (!rootArray.isArray())
				return;

			int rowNum = 0;
			for (JsonNode rowNode : rootArray) {
				Row row = sheet.createRow(rowNum++);
				int cellIdx = 0;

				while (rowNode.has("p" + cellIdx)) {
					Cell cell = row.createCell(cellIdx);
					cell.setCellValue(rowNode.get("p" + cellIdx).asText().trim());
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
