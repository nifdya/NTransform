package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import global.FileSelectorManager;
import global.UtilsFileSystem;
import global.UtilsPipeline;
import global.task.TaskConfig;

public class MainFrame extends JFrame {
	private JComboBox<String> configSelector;
	private JList<TaskConfig> taskList;
	private DefaultListModel<TaskConfig> listModel;
	private JPanel cardsPanel;
	private CardLayout cardLayout;
	private Map<String, DynamicFormPanel> formsMap = new HashMap<>();

	// Campos de texto para la cabecera del Pipeline
	private JTextField txtOrigen, txtDestino, txtPipeline;
	private JTextField txtDefFile, txtDelimiter;
	private JComboBox<String> cmbCharset;

	// Tabla y colección persistente de instrucciones (Punto 3)
	private JTable pipelineTable;
	private DefaultTableModel tableModel;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private JLabel lblDescription;

	private JPanel rightPanel;
	private JPanel centerContainer;

	// Listado de tus 6 archivos de configuración JSON
	private String[] jsonFiles = { "config_convert.json", "config_csv.json", "config_xlsx.json",
			"config_text.json" };

	private String getNodeJarForJsonFile(String jsonFile) {
		String node = "";
		switch (jsonFile) {
		case "config_csv.json":
			node = "NCSV.jar";
			break;
		case "config_xlsx.json":
			node = "NXLSX.jar";
			break;
		case "config_convert.json":
			node = "N2Convert.jar";
			break;
		case "config_text.json":
			node = "NTextPos.jar";
			break;
		}
		return node;
	}

	public MainFrame() throws IOException {
		
		setTitle("Constructor de Pipelines Multi-JSON NExcel");
		setSize(1000, 750);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		initComponents();
		// Carga el primer archivo JSON de la lista de manera automática al iniciar
		if (jsonFiles.length > 0) {
			loadJsonConfiguration(jsonFiles[0]);
		}
	}

	private void initTopContainer() {
	    // =============================================================
	    // 1. PANEL SUPERIOR: Selector Multi-JSON y Datos Base
	    // =============================================================
	    JPanel topContainer = new JPanel(new BorderLayout(5, 5));
	    topContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	    JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    selectorPanel.add(new JLabel("Origen de datos dinámicos:"));

	    configSelector = new JComboBox<>(jsonFiles);
	    configSelector.addActionListener(e -> {
	        String file = (String) configSelector.getSelectedItem();
	        if (file != null) {
	            loadJsonConfiguration(file);
	        }
	    });
	    selectorPanel.add(configSelector);
	    topContainer.add(selectorPanel, BorderLayout.NORTH);

	    // GridBagLayout para alinear perfectamente la estructura
	    JPanel fieldsPanel = new JPanel(new GridBagLayout());
	    fieldsPanel.setBorder(BorderFactory.createTitledBorder("Estructura de Ficheros del Pipeline"));
	    
	    GridBagConstraints gbc = new GridBagConstraints();
	    gbc.insets = new Insets(5, 5, 5, 5);
	    gbc.fill = GridBagConstraints.HORIZONTAL;

	    // --- FILA 1: FICHERO INICIAL ---
	    gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
	    fieldsPanel.add(new JLabel("Fichero Inicial (origen):"), gbc);

	    gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
	    txtOrigen = new JTextField("");
	    txtOrigen.setEditable(false);
	    fieldsPanel.add(txtOrigen, gbc);

	    gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
	    JButton btnBrowseOrigen = new JButton("...");
	    btnBrowseOrigen.addActionListener(e -> {
	        String path = FileSelectorManager.selectJsonFile(UtilsFileSystem.getJarPath());
	        if (path != null) txtOrigen.setText(path);
	    });
	    fieldsPanel.add(btnBrowseOrigen, gbc);

	    // --- FILA 2: FICHERO FINAL ---
	    gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
	    fieldsPanel.add(new JLabel("Fichero Final (destino):"), gbc);

	    gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
	    txtDestino = new JTextField("");
	    txtDestino.setEditable(false);
	    
	    // Automatización: Escucha cambios en txtDestino para generar el intermedio
	    txtDestino.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
	        public void insertUpdate(javax.swing.event.DocumentEvent e) {txtPipeline.setText(UtilsPipeline.getPipelineField(txtDestino.getText())); }
	        public void removeUpdate(javax.swing.event.DocumentEvent e) {txtPipeline.setText(UtilsPipeline.getPipelineField(txtDestino.getText()));}
	        public void changedUpdate(javax.swing.event.DocumentEvent e) {txtPipeline.setText(UtilsPipeline.getPipelineField(txtDestino.getText())); }
	    });
	    fieldsPanel.add(txtDestino, gbc);

	    gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
	    JButton btnBrowseDestino = new JButton("...");
	    btnBrowseDestino.addActionListener(e -> {
	        String path = FileSelectorManager.selectJsonFile(UtilsFileSystem.getJarPath());
	        if (path != null) {
	            txtDestino.setText(path); // Esto disparará automáticamente el listener de arriba
	        }
	    });
	    fieldsPanel.add(btnBrowseDestino, gbc);

	    // --- FILA 3: PASOS INTERMEDIOS (AUTOMÁTICO) ---
	    gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
	    fieldsPanel.add(new JLabel("Estructura Pasos Intermedios:"), gbc);

	    // Ocupa las columnas de campo y botón (gridwidth = 2) porque ya no necesita botón propio
	    gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0; gbc.gridwidth = 2;
	    txtPipeline = new JTextField("");
	    txtPipeline.setEditable(false);
	    txtPipeline.setBackground(new java.awt.Color(240, 240, 240)); // Visualmente deshabilitado
	    fieldsPanel.add(txtPipeline, gbc);

	    topContainer.add(fieldsPanel, BorderLayout.CENTER);
	    add(topContainer, BorderLayout.NORTH);
	}

	private void initJarPanel() {
		// =============================================================
		// 2. PANEL CENTRAL: Lista de Tareas e Interfaz de Parámetros
		// =============================================================
		listModel = new DefaultListModel<>();
		taskList = new JListCustom(listModel);
		taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		cardLayout = new CardLayout();
		cardsPanel = new JPanel(cardLayout);

		lblDescription = new JLabel("Seleccione una tarea para ver sus parámetros");
		lblDescription.setFont(new Font("SansSerif", Font.ITALIC, 13));
		lblDescription.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 5));
		lblDescription.setForeground(Color.DARK_GRAY);

		rightPanel = new JPanel(new BorderLayout()); // <-- Quitamos el "JPanel" de delante
		rightPanel.add(lblDescription, BorderLayout.NORTH);
		rightPanel.add(cardsPanel, BorderLayout.CENTER);

		JSplitPane splitFormularios = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(taskList),
				rightPanel);
		splitFormularios.setDividerLocation(220);

		// CORRECCIÓN: Control de estado nulo cuando la lista se vacía temporalmente en
		// la recarga
		taskList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				TaskConfig selected = taskList.getSelectedValue();
				if (selected != null) {
					lblDescription.setText("<html><b>Descripción:</b> " + selected.getDescription() + "</html>");
					cardLayout.show(cardsPanel, selected.getTask());
				} else {
					lblDescription.setText("Seleccione una tarea para ver sus parámetros");
				}
			}
		});

		JButton btnAddInstruction = new JButton("➕ Añadir Instrucción al Listado");
		btnAddInstruction.setFont(new Font("SansSerif", Font.BOLD, 12));
		btnAddInstruction.addActionListener(e -> addInstructionToPipeline());
		centerContainer = new JPanel(new BorderLayout(5, 5));
		centerContainer.add(splitFormularios, BorderLayout.CENTER);
		centerContainer.add(btnAddInstruction, BorderLayout.SOUTH);

	}

	private void initTaskPanel() {

		// =============================================================
		// 3. PANEL INFERIOR: Tabla Acumulativa y Botón Guardar
		// =============================================================
		JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));
		bottomContainer.setBorder(BorderFactory.createTitledBorder("Instrucciones Acumuladas en el Pipeline Actual"));

		String[] columnas = { "Paso", "Origen (JSON)", "Opc Comando", "Instrucción / Tarea Generada" };
		tableModel = new DefaultTableModel(columnas, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		pipelineTable = new JTable(tableModel);
		pipelineTable.setPreferredScrollableViewportSize(new Dimension(500, 150));
		bottomContainer.add(new JScrollPane(pipelineTable), BorderLayout.CENTER);
		this.initTableContextMenu();

		JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton btnLoadFile = new JButton("💾 Cargar Pipeline Completo (.json)");
		btnLoadFile.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnLoadFile.addActionListener(e -> loadPipelineFromFile());
		actionsPanel.add(btnLoadFile);		
		
		
		JButton btnLimpiar = new JButton("Limpiar Todo");
		btnLimpiar.addActionListener(e -> tableModel.setRowCount(0));
		actionsPanel.add(btnLimpiar);

		JButton btnSaveFile = new JButton("💾 Guardar Pipeline Completo (.json)");
		btnSaveFile.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnSaveFile.addActionListener(e -> savePipelineToFile());
		actionsPanel.add(btnSaveFile);

		bottomContainer.add(actionsPanel, BorderLayout.SOUTH);

		JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerContainer, bottomContainer);
		mainSplit.setDividerLocation(360);
		add(mainSplit, BorderLayout.CENTER);
	}

	private void initComponents() throws IOException {
		//1 - Comprobamos que los ficheros de configuración están en los recursos y quitamos los recursos que no se encuenn
		//    disponibles en el directorio
		this.jsonFiles=UtilsFileSystem.getJsonFilesInResources(UtilsFileSystem.getResourcesPath(), this.jsonFiles);
		
		setLayout(new BorderLayout(10, 10));

		this.initTopContainer();
		this.initJarPanel();
		this.initTaskPanel();


		if (configSelector.getItemCount() > 0) {
			String firstFile = (String) configSelector.getItemAt(0);
			loadJsonConfiguration(firstFile);
		}
	}

	/**
	 * Carga el archivo JSON seleccionado usando Jackson y reconstruye las pestañas.
	 */
	private void loadJsonConfiguration(String fileName) {
		try {
			List<TaskConfig> tasksList = null;
			String absoluteFilePath=UtilsFileSystem.getResourcesPath()+"\\"+fileName;
			File externalFile = new File(absoluteFilePath);

			if (externalFile.exists()) {
				tasksList = objectMapper.readValue(externalFile, new TypeReference<List<TaskConfig>>() {
				});
			} else {
				try (InputStream is = getClass().getClassLoader().getResourceAsStream(absoluteFilePath)) {
					if (is == null) {
						throw new java.io.FileNotFoundException("No se encontró el fichero: " + absoluteFilePath);
					}
					tasksList = objectMapper.readValue(is, new TypeReference<List<TaskConfig>>() {
					});
				}
			}

			// 1. Limpieza total de lo anterior
			listModel.clear();
			cardsPanel.removeAll();
			formsMap.clear();

			
			// Creamos un panel contenedor para los campos del JSON actual con un diseño
			// limpio
			JPanel jsonHeaderFieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

			switch (fileName) {
			case "config_csv.json":
				jsonHeaderFieldsPanel.add(new JLabel("Delimitador:"));
				txtDelimiter = new JTextField(";", 3);
				jsonHeaderFieldsPanel.add(txtDelimiter);

				jsonHeaderFieldsPanel.add(new JLabel("Charset:"));
				cmbCharset = CharsetCombo.createCharsetCombo();
				jsonHeaderFieldsPanel.add(cmbCharset);
				break;

			case "config_text.json":
				jsonHeaderFieldsPanel.add(new JLabel("Fichero de definiciones:"));
				txtDefFile = new JTextField(20);
				jsonHeaderFieldsPanel.add(txtDefFile);

				jsonHeaderFieldsPanel.add(new JLabel("Charset:"));
				cmbCharset = CharsetCombo.createCharsetCombo();
				jsonHeaderFieldsPanel.add(cmbCharset);
				break;

			}

			// Si el archivo tenía campos especiales, los inyectamos arriba del panel de
			// descripción
			// Para que esto funcione, 'lblDescription' y 'jsonHeaderFieldsPanel' se
			// actualizan en cascada.
			// Nota: Para simplificar y que no rompa el CardLayout, añadiremos estos
			// controles al contenedor 'rightPanel'
			// que limpiará su zona superior.
			actualizarCamposSuperioresDelJson(jsonHeaderFieldsPanel);

			// 2. Carga de las tareas dinámicas (Tu lógica original)
			if (tasksList != null && !tasksList.isEmpty()) {
				for (TaskConfig task : tasksList) {
					String name = task.getTask();
					listModel.addElement(task);

					DynamicFormPanel form = new DynamicFormPanel(task);
					formsMap.put(name, form);
					cardsPanel.add(form, name);
				}

				cardsPanel.revalidate();
				cardsPanel.repaint();

				// Autoseleccionar la primera tarea del nuevo JSON
				taskList.setSelectedIndex(0);
				cardLayout.show(cardsPanel, tasksList.get(0).getTask());
			} else {
				lblDescription.setText("Seleccione una tarea para ver sus parámetros");
				cardsPanel.revalidate();
				cardsPanel.repaint();
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error al cargar la configuración: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private boolean validateModuleCmdOptions() {
		boolean isValid = true;
		switch ((String) configSelector.getSelectedItem()) {
		case "config_csv.json":
			if (((JTextField) txtDelimiter).getText().trim().isEmpty()) {
				txtDelimiter.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
				txtDelimiter.setToolTipText("Este campo es obligatorio.");
				isValid = false;
			} else {
				txtDelimiter.setBorder(UIManager.getBorder("TextField.border"));
				txtDelimiter.setToolTipText("Delimitador de los elementos");
			}
			break;

		case "config_text.json":
			if (((JTextField) txtDefFile).getText().trim().isEmpty()) {
				txtDefFile.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
				txtDefFile.setToolTipText("Este campo es obligatorio.");
				isValid = false;
			} else {
				txtDefFile.setBorder(UIManager.getBorder("TextField.border"));
				txtDefFile.setToolTipText("Fichero del campo de definiciones de las posiciones");
			}
			break;
		}

		return isValid;
	}

	public String getSerializedCmdOptionsNode() {
		StringBuilder sb = new StringBuilder();
		switch ((String) configSelector.getSelectedItem()) {
		case "config_csv.json":
			sb.append(" -dc ").append(((JTextField) txtDelimiter).getText().trim());
			sb.append(" -c ").append(((String) cmbCharset.getSelectedItem()).trim());
			break;
		case "config_text.json":
			sb.append(" -dc ").append(((JTextField) txtDelimiter).getText().trim());
			sb.append(" -d ").append(((JTextField) txtDefFile).getText().trim());
			break;
		}
		return sb.toString();
	}

	private void addAndCheckInstruction(int siguientePaso, String jsonOrigen, String serializedCmdOptions,
			String tareaCompleta) {
		// 1. Verificar si la tabla tiene al menos una fila guardada
		int totalFilas = tableModel.getRowCount();
		boolean esMismaInstruccion = false;
		boolean esTodoIgual = false;
		Object ultimoJson = null;
		Object ultimoCmd = null;
		Object ultimaTarea = null;

		if (totalFilas > 0) {
			int ultimaFilaIdx = totalFilas - 1;
			boolean cont=true;

			for (int i = ultimaFilaIdx; i >= 0  && cont; i--) {
				// 2. Extraer los valores de la última fila actual
				ultimoJson = tableModel.getValueAt(i, 1);
				ultimoCmd = tableModel.getValueAt(i, 2);
				ultimaTarea = tableModel.getValueAt(i, 3);
				if(ultimoJson!=null)
				{
					cont=ultimoJson.equals("");
				}
					
			}

			// 3. Comparar de forma segura contra tus nuevas variables (manejando posibles
			// nulos)
			boolean jsonIgual = java.util.Objects.equals(ultimoJson, jsonOrigen);
			boolean cmdIgual = java.util.Objects.equals(ultimoCmd, serializedCmdOptions);
			boolean tareaIgual = java.util.Objects.equals(ultimaTarea, tareaCompleta);

			// 4. Evaluar tus condiciones
			esMismaInstruccion = jsonIgual && cmdIgual;
			esTodoIgual = esMismaInstruccion && tareaIgual;
		}

		// =========================================================================
		// 5. Ejemplo de uso con condicionales antes de insertar
		// =========================================================================
		if (esTodoIgual) {
			JOptionPane.showMessageDialog(this, "Esta instrucción ya existe exactamente igual en el pipeline.",
					"Registro Duplicado", JOptionPane.WARNING_MESSAGE);
		} else if (esMismaInstruccion) {
			tableModel.addRow(new Object[] { siguientePaso, "", "", tareaCompleta });
		} else {
			// Si pasa tus filtros de control, se añade limpiamente a la tabla
			tableModel.addRow(new Object[] { siguientePaso, jsonOrigen, serializedCmdOptions, tareaCompleta });
		}

	}

	// Métodos mock para evitar errores de compilación locales
	private void addInstructionToPipeline() {

		TaskConfig selectedTask = taskList.getSelectedValue();
		String serializedCmdOptions = "";
		if (selectedTask == null) {
			JOptionPane.showMessageDialog(this, "Por favor, selecciona una tarea para añadir.", "Aviso",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (this.validateModuleCmdOptions() == false) {
			JOptionPane.showMessageDialog(this, "Por favor, seleccione las opciones de comando de la tarea.", "Aviso",
					JOptionPane.WARNING_MESSAGE);
			return;

		}
		DynamicFormPanel activeForm = formsMap.get(selectedTask.getTask());
		if (activeForm.validateForm()) {
			String serializedParams = activeForm.getSerializedParams();
			serializedCmdOptions = this.getSerializedCmdOptionsNode();
			if (serializedCmdOptions.trim().isEmpty()) {
				serializedCmdOptions = activeForm.getSerializedCmdOptions();
			}

			String tareaCompleta = selectedTask.getTask();
			if (!serializedParams.isEmpty()) {
				tareaCompleta += "|" + serializedParams;
			}
			// Se guarda en la lista interna sin verse afectada si el usuario cambia de JSON
			int siguientePaso = tableModel.getRowCount();
			String jsonOrigen = (String) configSelector.getSelectedItem();
			this.addAndCheckInstruction(siguientePaso, jsonOrigen, serializedCmdOptions, tareaCompleta);
			// tableModel.addRow(new Object[] { siguientePaso, jsonOrigen,
			// serializedCmdOptions, tareaCompleta });
		}

	}

private void savePipelineToFile() {
    // Validación: Evitar generar un archivo vacío si no hay instrucciones añadidas
    if (tableModel.getRowCount() < 1) {
        JOptionPane.showMessageDialog(this,
                "La lista de instrucciones está vacía. Añade al menos una tarea antes de exportar.",
                "Validación de Datos", JOptionPane.WARNING_MESSAGE);
        return;
    }
    // Crear el selector de archivos nativo de Swing
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Guardar Configuración de Pipeline Final");
    fileChooser.setSelectedFile(new File("pipeline_final.json"));

    int userSelection = fileChooser.showSaveDialog(this);

    // Si el usuario confirma la ruta y pulsa "Guardar"
    if (userSelection == JFileChooser.APPROVE_OPTION) {
        File fileToSave = fileChooser.getSelectedFile();
        try {
            // 1. Construir el nodo raíz del JSON
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("inputFile", txtOrigen.getText());
            rootNode.put("outputFile", txtDestino.getText());
            rootNode.put("tempFile", txtPipeline.getText());

            // 2. Crear el array contenedor de instrucciones principales
            ArrayNode instruccionesArray = objectMapper.createArrayNode();

            int filas = tableModel.getRowCount();
            int indexFile = 1;   // Columna 1: Origen (JSON)
            int indexOptCmd = 2; // Columna 2: Opc Comando
            int indexTask = 3;   // Columna 3: Instrucción / Tarea

            // Variables de control para la agrupación dinámicos
            ObjectNode ultimoNodoPaso = null;
            ArrayNode ultimaListaTareas = null;
            
            String ultimoFicheroValido = "";
            String ultimoCmdValido = "";
            int contadorPasosReales = 0;

            for (int i = 0; i < filas; i++) {
                // Obtener valores de la celda de forma segura manejando nulos
                Object fileVal = tableModel.getValueAt(i, indexFile);
                Object cmdVal = tableModel.getValueAt(i, indexOptCmd);
                Object taskVal = tableModel.getValueAt(i, indexTask);

                String filaFichero = (fileVal != null) ? fileVal.toString().trim() : "";
                String filaCmd = (cmdVal != null) ? cmdVal.toString().trim() : "";
                String filaTarea = (taskVal != null) ? taskVal.toString().trim() : "";

                // Determinar si es una fila agrupada (Fichero y Comando vacíos)
                boolean esFilaAgrupada = filaFichero.isEmpty() && filaCmd.isEmpty();

                if (esFilaAgrupada && ultimoNodoPaso != null && ultimaListaTareas != null) {
                    // CASO A: Fila agrupada -> Añadir la tarea al array 'tasks' del paso anterior
                    ObjectNode taskNode = objectMapper.createObjectNode();
                    taskNode.put("task", filaTarea);
                    ultimaListaTareas.add(taskNode);
                } else {
                    // CASO B: Nuevo paso detectado (Hay datos o es la primera fila)
                    contadorPasosReales++; // Solo incrementa si hay un cambio real
                    
                    // Si la fila actual vino en blanco pero es la primera o rompió el orden, 
                    // arrastramos los últimos valores válidos conocidos
                    if (!filaFichero.isEmpty()) ultimoFicheroValido = filaFichero;
                    if (!filaCmd.isEmpty()) ultimoCmdValido = filaCmd;

                    // Crear el nuevo bloque de comando (paso)
                    ultimoNodoPaso = objectMapper.createObjectNode();
                    ultimoNodoPaso.put("jar", this.getNodeJarForJsonFile(ultimoFicheroValido));
                    ultimoNodoPaso.put("keepOnError", true);
                    ultimoNodoPaso.put("step", String.valueOf(contadorPasosReales));
                    
                    if (!ultimoCmdValido.isEmpty()) {
                        ultimoNodoPaso.put("cmdOptions", ultimoCmdValido);
                    }

                    // Inicializar el array de 'tasks' para este nuevo paso
                    ultimaListaTareas = objectMapper.createArrayNode();
                    ObjectNode taskNode = objectMapper.createObjectNode();
                    taskNode.put("task", filaTarea);
                    ultimaListaTareas.add(taskNode);

                    // Vincular el array 'tasks' al paso actual
                    ultimoNodoPaso.set("tasks", ultimaListaTareas);

                    // Añadir el paso al listado global
                    instruccionesArray.add(ultimoNodoPaso);
                }
            }

            // Vincular el array completo de instrucciones al nodo raíz
            rootNode.set("commands", instruccionesArray);

            // 4. Escritura física final al disco duro aplicando sangrado/formato visual limpio
            try (FileWriter writer = new FileWriter(fileToSave)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, rootNode);
            }

            // Notificación de éxito al usuario
            JOptionPane.showMessageDialog(this,
                    "¡Pipeline exportado con éxito!\nArchivo guardado en: " + fileToSave.getAbsolutePath(),
                    "Operación Completada", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            // Captura y muestra de errores visuales en caso de fallo de escritura en disco
            JOptionPane.showMessageDialog(this,
                    "Error crítico al intentar escribir el archivo en el almacenamiento físico:\n"
                            + ex.getLocalizedMessage(),
                    "Error de Guardado", JOptionPane.ERROR_MESSAGE);
        }
    }
}

private void loadPipelineFromFile() {
    // 1. Crear el selector de archivos nativo de Swing para abrir
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Cargar Configuración de Pipeline");
    
    // Abrir por defecto en la carpeta de recursos
    File initialDir = new File(UtilsFileSystem.getJarPath());
    if (initialDir.exists() && initialDir.isDirectory()) {
        fileChooser.setCurrentDirectory(initialDir);
    }
    
    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos JSON (*.json)", "json"));

    int userSelection = fileChooser.showOpenDialog(this);

    if (userSelection == JFileChooser.APPROVE_OPTION) {
        File fileToLoad = fileChooser.getSelectedFile();
        try {
            // 2. Leer y parsear el archivo JSON
            JsonNode rootNode = objectMapper.readTree(fileToLoad);

            // 3. Rellenar los campos de texto principales
            txtOrigen.setText(rootNode.has("inputFile") ? rootNode.get("inputFile").asText() : "");
            txtDestino.setText(rootNode.has("outputFile") ? rootNode.get("outputFile").asText() : "");
            txtPipeline.setText(rootNode.has("tempFile") ? rootNode.get("tempFile").asText() : "");

            // 4. Limpiar el modelo de la tabla antes de la carga
            tableModel.setRowCount(0);

            // 5. Procesar el array de comandos ("commands")
            if (rootNode.has("commands") && rootNode.get("commands").isArray()) {
                ArrayNode commandsArray = (ArrayNode) rootNode.get("commands");

                // Mantener el contador de filas totales para asignar el 'siguientePaso'
                int filaActualIndex = 0;

                for (JsonNode commandNode : commandsArray) {
                    
                    // Recuperar metadatos del paso
                    String jarFile = commandNode.has("jar") ? commandNode.get("jar").asText() : "";
                    String cmdOptions = commandNode.has("cmdOptions") ? commandNode.get("cmdOptions").asText() : "";

                    // Procesar las tareas internas de este paso agrupado
                    if (commandNode.has("tasks") && commandNode.get("tasks").isArray()) {
                        ArrayNode tasksArray = (ArrayNode) commandNode.get("tasks");
                        
                        boolean esPrimeraFilaDelPaso = true;

                        for (JsonNode taskNode : tasksArray) {
                            String taskText = taskNode.has("task") ? taskNode.get("task").asText() : "";

                            // Variables que enviaremos a tu función addAndCheckInstruction
                            int pasoId = filaActualIndex; 
                            String jsonOrigenParam;
                            String cmdOptionsParam;

                            if (esPrimeraFilaDelPaso) {
                                // La primera fila del bloque muestra los datos reales
                                jsonOrigenParam = jarFile;
                                cmdOptionsParam = cmdOptions;
                                esPrimeraFilaDelPaso = false;
                            } else {
                                // Las filas agrupadas secundarias envían vacíos a la tabla
                                jsonOrigenParam = "";
                                cmdOptionsParam = "";
                            }

                            // REUTILIZACIÓN: Llamamos a tu método nativo de inserción y validación
                            this.addAndCheckInstruction(pasoId, jsonOrigenParam, cmdOptionsParam, taskText);
                            
                            // Incrementamos el contador global de filas
                            filaActualIndex++;
                        }
                    }
                }
            }

            // Notificación de éxito
            JOptionPane.showMessageDialog(this,
                    "¡Pipeline cargado con éxito!\nArchivo: " + fileToLoad.getName(),
                    "Operación Completada", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error crítico al intentar parsear o leer el archivo de configuración:\n"
                            + ex.getLocalizedMessage(),
                    "Error de Carga", JOptionPane.ERROR_MESSAGE);
        }
    }
}


	private void actualizarCamposSuperioresDelJson(JPanel camposEspeciales) {
		if (rightPanel == null)
			return;

		// CORREGIDO: Todo junto, sin espacios intermedios
		JPanel topContainerInRight = new JPanel(new BorderLayout());
		topContainerInRight.add(lblDescription, BorderLayout.NORTH);

		// Si el switch generó componentes (ej: delimitador), los añade abajo de la
		// descripción
		if (camposEspeciales.getComponentCount() > 0) {
			topContainerInRight.add(camposEspeciales, BorderLayout.CENTER);
		}

		// Quitamos lo que hubiera en la zona norte de rightPanel y ponemos el nuevo
		// bloque completo
		java.awt.Component antiguoComponente = ((BorderLayout) rightPanel.getLayout())
				.getLayoutComponent(BorderLayout.NORTH);
		if (antiguoComponente != null) {
			rightPanel.remove(antiguoComponente);
		}

		rightPanel.add(topContainerInRight, BorderLayout.NORTH);
		rightPanel.revalidate();
		rightPanel.repaint();
	}
	private void deleteSelectedInstruction() {
	    int selectedRow = pipelineTable.getSelectedRow();

	    // Validación: Verificar que haya una fila seleccionada
	    if (selectedRow == -1) {
	        JOptionPane.showMessageDialog(this, 
	                "Por favor, selecciona una fila de la tabla para eliminar.", 
	                "Aviso", JOptionPane.WARNING_MESSAGE);
	        return;
	    }

	    // 1. Eliminar la fila del modelo
	    tableModel.removeRow(selectedRow);

	    // 2. Recalcular secuencialmente la columna 0 (Número de Paso)
	    int totRows = tableModel.getRowCount();
	    for (int i = 0; i < totRows; i++) {
	        // Asignamos el nuevo índice 'i' a la columna 0 de cada fila
	        tableModel.setValueAt(i, i, 0); 
	    }
	}
	private void initTableContextMenu() {
	    // 1. Crear el menú flotante y su opción de borrado
	    JPopupMenu popupMenu = new JPopupMenu();
	    JMenuItem deleteItem = new JMenuItem("Eliminar Instrucción");
	    deleteItem.setIcon(javax.swing.UIManager.getIcon("InternalFrame.closeIcon")); // Icono nativo de aspa (opcional)
	    
	    // Asociar la acción de borrado al ítem de menú
	    deleteItem.addActionListener(e -> deleteSelectedInstruction());
	    popupMenu.add(deleteItem);

	    // 2. Escuchador de ratón en la tabla para detectar el clic derecho
	    this.pipelineTable.addMouseListener(new java.awt.event.MouseAdapter() {
	        @Override
	        public void mousePressed(java.awt.event.MouseEvent e) {
	            showPopup(e);
	        }

	        @Override
	        public void mouseReleased(java.awt.event.MouseEvent e) {
	            showPopup(e);
	        }

	        // Método auxiliar para detectar el clic derecho según el Sistema Operativo
	        private void showPopup(java.awt.event.MouseEvent e) {
	            if (e.isPopupTrigger()) {
	                // Seleccionar automáticamente la fila sobre la que se hizo clic derecho
	                int row = pipelineTable.rowAtPoint(e.getPoint());
	                if (row >= 0 && row < pipelineTable.getRowCount()) {
	                	pipelineTable.setRowSelectionInterval(row, row);
	                }
	                
	                // Mostrar el menú contextual en las coordenadas exactas del cursor
	                popupMenu.show(e.getComponent(), e.getX(), e.getY());
	            }
	        }
	    });
	}


}
