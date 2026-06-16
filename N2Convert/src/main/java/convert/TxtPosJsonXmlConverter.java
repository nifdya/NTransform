package convert;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import record.MapDefinitionsTextPos;
import record.RecordDefinitionTextPos;

/**
 * Clase para la conversión de ficheros Texto por posiciones a los formatos
 * estructurados JSON y XML.
 * 
 * @version 1.0
 */
public class TxtPosJsonXmlConverter {

	/** Definiciones del fichero de texto por posiciones */
	private final Map<String, RecordDefinitionTextPos> mapaDefiniciones;
	/** Charset del fichero de texto por posiciones */
	private final Charset charset;

	/**
	 * Cargamos en el constructor el mapa de definiciones y el charset del fichero
	 * 
	 * @param mapaDefiniciones - Definiciones del fichero de texto por posiciones	
	 * @param charset - Charset del fichero de texto por posiciones.
	 */
	public TxtPosJsonXmlConverter(Map<String, RecordDefinitionTextPos> mapaDefiniciones, Charset charset) {
		this.mapaDefiniciones = mapaDefiniciones;
		this.charset = charset;
	}

	/**
	 * Método para la conversión de un fichero de texto por posiciones a JSON
	 * 
	 * @param txtPath - Ruta del fichero de texto origen
	 * @param jsonPath - Ruta del fichero destino json, resultante de la conversión
	 * @throws IOException
	 */
	public void txtPosToJson(String txtPath, String jsonPath) throws IOException {
		JsonFactory factory = new JsonFactory();
		try (BufferedReader reader = new BufferedReader(new FileReader(txtPath, charset));
				JsonGenerator jg = factory.createGenerator(new File(jsonPath),
						com.fasterxml.jackson.core.JsonEncoding.UTF8)) {

			jg.useDefaultPrettyPrinter();
			jg.writeStartArray();

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty())
					continue;

				String tipo = MapDefinitionsTextPos.obtenerTipoDeLinea(line, this.mapaDefiniciones);
				RecordDefinitionTextPos def = this.mapaDefiniciones.get(tipo);
				if (def == null)
					continue;

				List<Integer> longitudes = def.getLongitudes();
				List<Boolean> ignorados = def.getIgnorados();

				jg.writeStartObject();
				jg.writeStringField("recordType", tipo);

				int currentPointer = 0;
				int fieldIdx = 0;
				for (int i = 0; i < longitudes.size(); i++) {
					if (currentPointer >= line.length())
						break;
					int length = longitudes.get(i);
					int endPointer = Math.min(currentPointer + length, line.length());

					if (ignorados == null || i >= ignorados.size() || !ignorados.get(i)) {
						jg.writeStringField("p" + fieldIdx, line.substring(currentPointer, endPointer).trim());
						fieldIdx++;
					}
					currentPointer += length;
				}
				jg.writeEndObject();
			}
			jg.writeEndArray();
		}
	}

	/**
	 * Método para la conversión de texto por posiciones a XML
	 * 
	 * @param txtPath - Ruta del fichero de texto origen
	 * @param xmlPath - Ruta del fichero destino xml, resultante de la conversión	
	 * 
	 * @throws Exception
	 */
	public void txtPosToXml(String txtPath, String xmlPath) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("records");
		doc.appendChild(root);

		try (BufferedReader reader = new BufferedReader(new FileReader(txtPath, charset))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty())
					continue;

				String tipo = MapDefinitionsTextPos.obtenerTipoDeLinea(line, this.mapaDefiniciones);
				RecordDefinitionTextPos def = this.mapaDefiniciones.get(tipo);
				if (def == null)
					continue;

				List<Integer> longitudes = def.getLongitudes();
				List<Boolean> ignorados = def.getIgnorados();

				Element recordNode = doc.createElement("record");
				recordNode.setAttribute("type", tipo);

				int currentPointer = 0;
				int fieldIdx = 0;
				for (int i = 0; i < longitudes.size(); i++) {
					if (currentPointer >= line.length())
						break;
					int length = longitudes.get(i);
					int endPointer = Math.min(currentPointer + length, line.length());

					if (ignorados == null || i >= ignorados.size() || !ignorados.get(i)) {
						Element fieldNode = doc.createElement("p" + fieldIdx);
						fieldNode.setTextContent(line.substring(currentPointer, endPointer).trim());
						recordNode.appendChild(fieldNode);
						fieldIdx++;
					}
					currentPointer += length;
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
