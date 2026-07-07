package global;

/**
 * Clase de utilidades para trabajr con las rutas
 */

public class PathUtils {


	/**
	 * Elimina la extensión del fichero indicado en la ruta, basándose en el último
	 * punto. Ejemplo: "C:/logs/app.v1.log" -> "C:/logs/app.v1"
	 * 
	 * @param pathStr
	 * @return
	 */
	public static String removeExtension(String pathStr) {
		if (pathStr == null) {
			return null;
		}

		int lastDotIndex = pathStr.lastIndexOf('.');
		int lastSeparatorIndex = Math.max(pathStr.lastIndexOf('/'), pathStr.lastIndexOf('\\'));

		// Ensure the dot is part of the file name, not part of the directory path
		if (lastDotIndex > lastSeparatorIndex) {
			return pathStr.substring(0, lastDotIndex);
		}

		return pathStr; // Return original if no extension found
	}


	/**
	 * Remplaza la extensión de un fichero, proporcionando la ruta del fichero,
	 * desde el último punto. Ejemplo: "logs/app.log" + ".bak" -> "logs/app.bak"
	 * @param pathStr
	 * @param newExtension
	 * @return
	 */
	public static String replaceExtension(String pathStr, String newExtension) {
		String pathWithoutExtension = removeExtension(pathStr);

		if (pathWithoutExtension == null) {
			return null;
		}

		// Ensure the new extension starts with a dot if not provided
		String formattedExtension = newExtension.startsWith(".") ? newExtension : "." + newExtension;

		return pathWithoutExtension + formattedExtension;
	}
}