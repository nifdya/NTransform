package global;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CoordExecutor {

    public void executePipeline(ComunOptions config) {
        List<Command> commands = config.commands;
        if (commands == null || commands.isEmpty()) {
            System.out.println("⚠ No hay instrucciones para ejecutar.");
            return;
        }

        // El origen arranca con el fichero inicial global
        String orgCurrentFile = config.inputFile;

        System.out.println("🚀 Iniciando Pipeline Complejo...");
        System.out.println("📥 Fichero Inicial: " + config.inputFile);
        System.out.println("📤 Fichero Final:   " + config.outputFile);

        for (int i = 0; i < commands.size(); i++) {
            Command block = commands.get(i);
     
            
            System.out.println("\n==================================================");
            System.out.println("📦 INSTRUCCIÓN PASO " + block.step + " | Ejecutable: " + block.jar);
            System.out.println("==================================================");

            String subTaskStr="";
            String subTaskStrCMD="";
            boolean isLastCommand = (i == commands.size() - 1);
            String destCurrentFile;            
            
            if (block.tasks == null || block.tasks.isEmpty()) {
                System.out.println("⚠ Este bloque no contiene tareas internas. Saltando...");
                continue;
            }
            else
            {           	
            	for (SubTarea subTask : block.tasks) {
            		subTaskStr += " " + subTask.toString();
            		subTaskStrCMD += " " + subTask.toString();
            	}	
            }

            try {
            	System.out.println("Ficheros intermedios:  " + config.tempFile);
            	System.out.println("Paso:  " +  block.step);
            	System.out.println("Replace:  " +  config.tempFile.replace("[[PASO]]", block.step)); 
            	destCurrentFile = isLastCommand ? config.outputFile : config.tempFile.replace("[[PASO]]", block.step);

                System.out.println("\n  🔹 Ejecutando Instrucción [" + (i + 1) + "/" + subTaskStr+ "]: " +  subTaskStrCMD);
                System.out.println("     📥 Entrada: " + orgCurrentFile + " (" + (orgCurrentFile.length() / 1024) + " KB)");
                System.out.println("     📤 Salida:  " + destCurrentFile);

                // Ejecutar el comando JAR correspondiente a esta instrucción
                boolean ok = executeCommand(block, orgCurrentFile, destCurrentFile);

                if (!ok) {
                    System.err.println("❌ Fallo en la subtarea: " + block.step + ". Pipeline abortado.");
                    return;
                }

                 // Pequeña pausa de 150ms para permitir que el sistema de archivos de Windows asiente el Excel generado
                 Thread.sleep(150); 

                // Avanzar el puntero del fichero para la siguiente subtarea
                 orgCurrentFile = destCurrentFile;

            } catch (Exception e) {
                System.err.println("❌ Error crítico de infraestructura en " + block.step + ": " + e.getMessage());
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

    private boolean executeCommand(Command cmd, String origen, String destino) {
        try {
            // 1. Forzar rutas absolutas para evitar que NExcel se pierda de directorio
            File fileJar = new File(cmd.jar).getAbsoluteFile();
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
            
            // Incluimos las opciones de la instrucción
            if(!cmd.cmdOptions.isEmpty())
            {
            	command.add(cmd.cmdOptions);
            }

            // 2. Agregar cada subtarea con su propio prefijo "-t"
            // Reemplaza 'listaTareas' por el nombre real de tu List
            for (Object subTask : cmd.tasks) {
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
                    System.out.println("      [" + cmd.step + "] " + line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("     🏁 [" + cmd.step + "] Finalizado con código: " + exitCode);
            return exitCode == 0;

        } catch (Exception e) {
            System.err.println("❌ Error al invocar ProcessBuilder para " + cmd.step + ": " + e.getMessage());
            return false;
        }
    }
}
