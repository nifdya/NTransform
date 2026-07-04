package global;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Se encarga de la ejecución y coordinación de las tareas, clase principal de
 * ejecución
 */
public class CoordExecutor {

	/**
	 * Ejecuta la secucencia
	 * 
	 * @param config    - Configuración de la secuencia
	 * @param traceFile - Fichero con la traza
	 */
	public void executePipeline(ComunOptions config, String traceFile) {
		List<Command> commands = config.commands;
		if (commands == null || commands.isEmpty()) {
			Coordinator.printModuleLog("⚠ No hay instrucciones para ejecutar.", false);
			return;
		}

		// El origen arranca con el fichero inicial global
		String orgCurrentFile = config.inputFile;
		Coordinator.printModuleLogSpace(false, true);
		Coordinator.printModuleLog("🚀 Iniciando Coordinador General", false);
		Coordinator.printModuleLog("📥 Fichero Inicial:" + config.inputFile, false);
		Coordinator.printModuleLog("📤 Fichero Final:  " + config.outputFile, false);
		Coordinator.printModuleLogSpace(false, false);

		for (int i = 0; i < commands.size(); i++) {
			Command block = commands.get(i);

			Coordinator.printModuleLogSpace(false, true);
			Coordinator.printModuleLog("ℹ️ INSTRUCCIÓN PASO " + block.step + " | Ejecutable: " + block.jar, false);
			Coordinator.printModuleLogSpace(false, false);

			String subTaskStr = "";
			String subTaskStrCMD = "";
			boolean isLastCommand = (i == commands.size() - 1);
			String destCurrentFile;

			if (block.tasks == null || block.tasks.isEmpty()) {
				Coordinator.printModuleLog("⚠ Este comando no contiene subtareas.", false);
				continue;
			} else {
				Coordinator.printModuleLog("ℹ️ Procesando subtareas.", false);
				for (SubTarea subTask : block.tasks) {
					subTaskStr += " " + subTask.toString();
					subTaskStrCMD += " " + subTask.toString();
				}
			}

			try {
				Coordinator.printModuleLog("Ficheros intermedios:  " + config.tempFile, false);
				Coordinator.printModuleLog("Paso:  " + block.step, false);
				Coordinator.printModuleLog("Fichero temporal: " + config.tempFile.replace("[[PASO]]", block.step),
						false);

				destCurrentFile = isLastCommand ? config.outputFile : config.tempFile.replace("[[PASO]]", block.step);

				Coordinator.printModuleLog(
						"   🔹 Ejecutando Instrucción [" + (i + 1) + "/" + subTaskStr + "]: " + subTaskStrCMD, false);
				Coordinator.printModuleLog(
						"      📥 Entrada: " + orgCurrentFile + " (" + (orgCurrentFile.length() / 1024) + " KB)",
						false);
				Coordinator.printModuleLog("      📤 Salida:  " + destCurrentFile, false);

				// Ejecutar el comando JAR correspondiente a esta instrucción
				boolean ok = executeCommand(block, orgCurrentFile, destCurrentFile, traceFile);

				if (!ok) {
					Coordinator.printModuleLogSpace(true, true);
					Coordinator.printModuleLog("❌ Fallo en la subtarea: " + block.step + ". Pipeline abortado.", true);
					Coordinator.printModuleLogSpace(true, false);
					return;
				}

				// Pequeña pausa de 150ms para permitir que el sistema de archivos de Windows
				// asiente el Excel generado
				Thread.sleep(150);

				// Avanzar el puntero del fichero para la siguiente subtarea
				orgCurrentFile = destCurrentFile;

			} catch (Exception e) {
				Coordinator.printModuleLogSpace(true, true);
				Coordinator.printModuleLog(
						"❌ Error crítico de infraestructura en " + block.step + ": " + e.getMessage(), true);
				Coordinator.printModuleLogSpace(true, false);
				return;
			}

			// Validar estrictamente la existencia del archivo de entrada en disco
			File archivoEntrada = new File(orgCurrentFile);
			if (!archivoEntrada.exists()) {
				Coordinator.printModuleLogSpace(true, true);
				Coordinator.printModuleLog(
						"❌ ERROR: El archivo de entrada no existe: " + archivoEntrada.getAbsolutePath(), true);
				Coordinator.printModuleLogSpace(true, false);
				return;
			}
		}

		Coordinator.printModuleLogSpace(false, true);
		System.out.println("🎉 ¡Proceso finalizado por completo! Fichero final generado exitosamente.");
		Coordinator.printModuleLogSpace(false, false);
	}

	/**
	 * Ejecuta el comando especificado
	 * 
	 * @param cmd       - configuración del comando a ejecutar
	 * @param origen    - fichero origen a especificar en el comando
	 * @param destino   - fichero destino a especificar en el comando
	 * @param traceFile - fichero con la traza
	 * @return Boolean - true| a finalizado correctamente o false| se produjó un
	 *         error durante la ejecución.
	 */
	private boolean executeCommand(Command cmd, String origen, String destino, String traceFile) {
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
			command.add("-ft");
			command.add(traceFile);

			// --> Incluimos las opciones de la instrucción
			if (cmd.cmdOptions != null && !cmd.cmdOptions.isEmpty()) {
				command.addAll(Arrays.asList(cmd.cmdOptions.split("\\s+")));
			}

			// --> Agregar cada subtarea con su propio prefijo "-t"
			// Reemplaza 'listaTareas' por el nombre real de tu List
			for (Object subTask : cmd.tasks) {
				command.add("-t");
				command.add(subTask.toString());
			}

			// --> Construir el ProcessBuilder con la lista dinámica
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.inheritIO();

			// --> Forzar el directorio de ejecución en la carpeta donde vive el JAR
			pb.directory(fileJar.getParentFile());
			Coordinator.printModuleLog("      🛠 [COMANDO REAL]: " + String.join(" ", pb.command()), false);

			pb.redirectErrorStream(true);
			Process process = pb.start();

			// --> Captura el log del proceso hijo en tiempo real
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					Coordinator.printModuleLog("       [" + cmd.step + "] " + line, false);
				}
			}

			int exitCode = process.waitFor();
			Coordinator.printModuleLog("      💾 [" + cmd.step + "] Finalizado con código: " + exitCode, false);
			if (exitCode != 0) {
				Coordinator.printModuleLogSpace(true, true);
				Coordinator.printModuleLog("❌ Error al ejecutar el comando. exitCode=" + exitCode, true);
				Coordinator.printModuleLogSpace(true, false);
			}
			return exitCode == 0;

		} catch (Exception e) {
			Coordinator.printModuleLog("❌ Error al invocar ProcessBuilder para " + cmd.step + ": " + e.getMessage(),
					true);
			return false;
		}
	}
}
