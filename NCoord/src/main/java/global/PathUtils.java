package global;



public class PathUtils {

    /**
     * Removes the extension from a file path string based on the last dot.
     * Example: "C:/logs/app.v1.log" -> "C:/logs/app.v1"
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
     * Replaces the extension of a file path string from the last dot.
     * Example: "logs/app.log" + ".bak" -> "logs/app.bak"
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