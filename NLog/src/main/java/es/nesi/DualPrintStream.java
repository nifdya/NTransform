package es.nesi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class DualPrintStream extends PrintStream {
    private final PrintStream originalOut;
    private final String prefix;
    // Format: Year-Month-Day Hour:Minute:Second.Millisecond
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public DualPrintStream(OutputStream fileStream, PrintStream originalOut, String prefix) {
        super(fileStream);
        this.originalOut = originalOut;
        this.prefix = prefix;
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        String message = new String(buf, off, len);
        
        // Avoid adding timestamps and prefixes to empty lines or system line breaks
        if (!message.trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(formatter);
            message = "[" + timestamp + "] [" + prefix + "] " + message;
        }

        // Write to the log file with timestamp and prefix
        try {
            super.out.write(message.getBytes());
        } catch (IOException e) {
            super.write(buf, off, len); // Fallback mechanism
        }

        // Write to the original console without modification
        originalOut.write(buf, off, len);   
    }
}
