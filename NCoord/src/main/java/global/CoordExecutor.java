package global;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CoordExecutor {

    public void executePipeline(ComunOptions config) {
        List<Instruccion> commands = config.instrucciones;
        if (commands == null || commands.isEmpty()) {
            System.out.println("⚠ No hay instrucciones para ejecutar.");
            return;
        }

        // El origen arranca con el fichero inicial global
        String orgCurrentFile = config.ficheroInicial;

        System.out.println("🚀 Iniciando Pipeline Complejo...");
        System.out.println("📥 Fichero Inicial: " + config.ficheroInicial);
        System.out.println("📤 Fichero Final:   " + config.ficheroFinal);

        for (int i = 0; i < commands.size(); i++) {
            Instruccion block = commands.get(i);
     
            
            System.out.println("\n==================================================");
            System.out.println("📦 INSTRUCCIÓN PASO " + block.paso + " | Ejecutable: " + block.jar);
            System.out.println("==================================================");

            String subTaskStr="";
            String subTaskStrCMD="";
            boolean isLastCommand = (i == commands.size() - 1);
            String destCurrentFile;            
            
            if (block.tareas == null || block.tareas.isEmpty()) {
                System.out.println("⚠ Este bloque no contiene tareas internas. Saltando...");
                continue;
            }
            else
            {           	
            	for (SubTarea subTask : block.tareas) {
            		subTaskStr += " " + subTask.toString();
            		subTaskStrCMD += " " + subTask.toString();
            	}	
            }

            try {
            	System.out.println("Ficheros intermedios:  " + config.ficherosIntermedios);
            	System.out.println("Paso:  " +  block.paso);
            	System.out.println("Replace:  " +  config.ficherosIntermedios.replace("[[PASO]]", block.paso)); 
            	destCurrentFile = isLastCommand ? config.ficheroFinal : config.ficherosIntermedios.replace("[[PASO]]", block.paso);

                System.out.println("\n  🔹 Ejecutando Instrucción [" + (i + 1) + "/" + subTaskStr+ "]: " +  subTaskStrCMD);
                System.out.println("     📥 Entrada: " + orgCurrentFile + " (" + (orgCurrentFile.length() / 1024) + " KB)");
                System.out.println("     📤 Salida:  " + destCurrentFile);

                // Ejecutar el comando JAR correspondiente a esta instrucción
                boolean ok = executeCommand(block, orgCurrentFile, destCurrentFile);

                if (!ok) {
                    System.err.println("❌ Fallo en la subtarea: " + block.paso + ". Pipeline abortado.");
                    return;
                }

                 // Pequeña pausa de 150ms para permitir que el sistema de archivos de Windows asiente el Excel generado
                 Thread.sleep(150); 

                // Avanzar el puntero del fichero para la siguiente subtarea
                 orgCurrentFile = destCurrentFile;

            } catch (Exception e) {
                System.err.println("❌ Error crítico de infraestructura en " + block.paso + ": " + e.getMessage());
                return;
            }      
            

            // Validar estrictamente la existencia del archivo de entrada en disco
            File archivoEntrada = new File(orgCurrentFile);
            if (!archivoEntrada.exists()) {
                System.err.println("\n❌ ERROR: El archivo de entrada no existe: " + archivoEntrada.getAbsolutePath());
               // System.err.println("Abortando el pipeline en: " + nombreLog);
                return;
            }            
        }



        System.out.println("\n🎉 ¡Proceso finalizado por completo! Fichero final generado exitosamente.");
    }

    private boolean executeCommand(Instruccion instruccion, String origen, String destino) {
        try {
            // 1. Forzar rutas absolutas para evitar que NExcel se pierda de directorio
            File fileJar = new File(instruccion.jar).getAbsoluteFile();
            File fileOrigen = new File(origen).getAbsoluteFile();
            File fileDestino = new File(destino).getAbsoluteFile();

            // 2. Construir el ProcessBuilder con rutas reales completas
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("-jar");
            command.add(fileJar.getAbsolutePath());
            command.add("-i");
            command.add(fileOrigen.getAbsolutePath());
            command.add("-o");
            command.add(fileDestino.getAbsolutePath());

            // 2. Agregar cada subtarea con su propio prefijo "-t"
            // Reemplaza 'listaTareas' por el nombre real de tu List
            for (Object subTask : instruccion.tareas) {
                command.add("-t");
                command.add(subTask.toString()); 
            }

            // 3. Construir el ProcessBuilder con la lista dinámica
            ProcessBuilder pb = new ProcessBuilder(command);


            // 3. Forzar el directorio de ejecución en la carpeta donde vive el JAR
            pb.directory(fileJar.getParentFile());
            System.out.println("      🛠 [COMANDO REAL]: " + String.join(" ", pb.command()));
            
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Captura el log del proceso hijo en tiempo real
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("      [" + instruccion.paso + "] " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("     🏁 [" + instruccion.paso + "] Finalizado con código: " + exitCode);
            return exitCode == 0;

        } catch (Exception e) {
            System.err.println("❌ Error al invocar ProcessBuilder para " + instruccion.paso + ": " + e.getMessage());
            return false;
        }
    }
}
