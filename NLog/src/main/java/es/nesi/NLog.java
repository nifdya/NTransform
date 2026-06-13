package es.nesi;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class NLog {
    
    public static void activate(String logFilePath) {
        try {
            // Append mode activated to allow both streams to share the same file safely
            FileOutputStream fileStream = new FileOutputStream(logFilePath, true);
            
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            
            DualPrintStream infoOutput = new DualPrintStream(fileStream, originalOut, "INFO");
            DualPrintStream errorOutput = new DualPrintStream(fileStream, originalErr, "ERROR");
            
            System.setOut(infoOutput);
            System.setErr(errorOutput);
            
        } catch (FileNotFoundException e) {
            System.err.println("Could not initialize the log file: " + e.getMessage());
        }
    }
}
