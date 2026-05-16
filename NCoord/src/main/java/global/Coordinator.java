package global;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;

public class Coordinator {
    public static void main(String[] args) {
        File jsonFile = new File(args[0]);
        ObjectMapper mapper = new ObjectMapper();

        try {
            System.out.println("Cargando archivo de instrucciones JSON...");
            
            // Jackson parsea la estructura jerárquica compleja en una sola línea
            ComunOptions config = mapper.readValue(jsonFile, ComunOptions.class);

            // Ejecutar el orquestador
            CoordExecutor executor = new CoordExecutor();
            executor.executePipeline(config);

        } catch (Exception e) {
            System.err.println("❌ Error fatal de Jackson al procesar el JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
