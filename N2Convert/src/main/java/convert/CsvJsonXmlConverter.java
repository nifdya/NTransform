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

/**
 * Clase para la conversión de ficheros de formatos de CSV a los formatos
 * estructurados: JSON y XML y viceversa.
 * 
 * @version 1.0
 */
public class CsvJsonXmlConverter {
	/** Charset del fichero del fichero CSV */
	private final Charset charset;
	/** Delimitador de las columnas CSV */
	private final String delimiter;

	/**
	 * Constructor con los parámetros necesarios para el trabajo con los ficheros
	 * CSV
	 * 
	 * @param charset   - Charset del fichero CSV
	 * @param delimiter - Delimitador a utilizar en los ficheros CSV
	 */
	public CsvJsonXmlConverter(Charset charset, String delimiter) {
		this.charset = charset;
		this.delimiter = delimiter;
	}

	/**
	 * Convierte un fichero en CSV a JSON.
	 * 
	 * @param csvPath  - Ruta del fichero CSV origen.
	 * @param jsonPath - Ruta del fichero JSON destino, resultado de la conversión.
	 * @throws IOException
	 */
	public void csvToJson(String csvPath, String jsonPath) throws IOException {
		JsonFactory factory = new JsonFactory();
		try (BufferedReader reader = new BufferedReader(new FileReader(csvPath, charset));
				JsonGenerator jg = factory.createGenerator(new File(jsonPath),
						com.fasterxml.jackson.core.JsonEncoding.UTF8)) {

			jg.useDefaultPrettyPrinter();
			jg.writeStartArray(); // [

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty())
					continue;

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
	 * Convierte un fichero en JSON a CSV.
	 * 
	 * @param csvPath - Ruta del fichero CSV origen.
	 * @param xmlPath - Ruta del fichero XML destino, resultado de la conversión.
	 * @throws Exception
	 */
	public void csvToXml(String csvPath, String xmlPath) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("records");
		doc.appendChild(root);

		try (BufferedReader reader = new BufferedReader(new FileReader(csvPath, charset))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty())
					continue;

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
