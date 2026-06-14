package global;

public class UtilsPipeline {
	public static String getPipelineField(String outputFile) {

		String res="";
	    if (outputFile == null || outputFile.trim().isEmpty()) {
	        return res;
	    }

	    // Buscamos la última aparición del punto para respetar extensiones largas o rutas con puntos
	    int lastDotPosition = outputFile.lastIndexOf('.');
	    
	    if (lastDotPosition != -1) {
	        String base = outputFile.substring(0, lastDotPosition);
	        String extension = outputFile.substring(lastDotPosition); // Incluye el punto (ej: ".json")
	        res=base + "_[[PASO]]" + extension;
	    } else {
	        // En caso de que la ruta no tenga extensión por alguna razón
	    	res=outputFile + "_[[PASO]]";
	    }
	    System.err.println("Ruta:"+res);
	    return res;
	}
}
