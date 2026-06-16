package ui;

import java.awt.BorderLayout;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import global.ConfigCatalog;
import global.FileSelectorManager;
import global.UtilsFileSystem;
import global.UtilsPipeline;
import global.task.TaskConfig;

/**
 * Clase con la interfaz principal de la aplicación de diseño. Provee el marco
 * básico para la gestión de las tareas: carga, selección del catálogo y la
 * gestión de los elementos comunes.
 * 
 * @version 1.0
 */
public class MainFrame extends JFrame {
	/** Combo con el selector de los ficheros de configuración */
	private JComboBox<String> configSelector;
	/**
	 * Elemento del interfaz en el que se cargan el listado de tareas suministrado
	 * en el catálogo.
	 */
	private JList<TaskConfig> taskList;

	private JPanel cardsPanel;
	private CardLayout cardLayout;
	private JLabel lblDescription;
	private JPanel rightPanel;
	private JPanel centerContainer;
	private Map<String, DynamicFormPanel> formsMap = new HashMap<>();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/** Campos de texto con los elementos de las rutas de los ficheros. */
	private JTextField txtOrigen, txtDestino, txtPipeline;
	/**
	 * Campos de texto con las opciones propias de algunos tipos de cátalogo,
	 * comunes a todo el procesamiento de ese catálogo
	 */
	private JTextField txtDefFile, txtDelimiter;
	private JComboBox<String> cmbCharset;

	/**
	 * Tabla y colección persistente de instrucciones, donde muestra el grid con las
	 * opciones del fichero secuencial
	 */
	private JTable pipelineTable;
	private DefaultTableModel tableModel;
	private DefaultListModel<TaskConfig> listModel;

	/**
	 * Tabla y colección persistente de instrucciones, donde muestra el grid con las
	 * opciones del fichero secuencial
	 */

	private String[] jsonFiles = { "config_convert.json", "config_csv.json", "config_xlsx.json", "config_text.json" };

	/**
	 * Constructor de la interfaz, carga los elementos básicos para iniciar la
	 * ventana.
	 * 
	 * @throws IOException
	 */
	public MainFrame() throws IOException {

		setTitle("Constructor de ficheros de secuencias para el Coordinador");
		setSize(1000, 750);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		// Cargamos los elementos de configuración necesarios para iniciar e
		// inicializamos los componentes visuales.
		initComponents();
		// Carga el primer archivo JSON de la lista de manera automática al iniciar
		if (jsonFiles.length > 0) {
			loadJsonConfiguration(jsonFiles[0]);
		}
	}

	/**
	 * Inicializar los elementos visuales del contenedor superior de la interfaz.
	 */
	private void initTopContainer() {

		JPanel topContainer = new JPanel(new BorderLayout(5, 5));
		topContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// --> Selector de los ficheros de los catálogos
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

		// --> Configuración de ficheros iniciales, finales y el intermedio
		JPanel fieldsPanel = new JPanel(new GridBagLayout());
		fieldsPanel.setBorder(BorderFactory.createTitledBorder("Estructura de Ficheros del Pipeline"));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// --> Fichero origen
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		fieldsPanel.add(new JLabel("Fichero Inicial (origen):"), gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		txtOrigen = new JTextField("");
		txtOrigen.setEditable(false);
		fieldsPanel.add(txtOrigen, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.weightx = 0.0;
		JButton btnBrowseOrigen = new JButton("...");
		btnBrowseOrigen.addActionListener(e -> {
			String path = FileSelectorManager.selectFile(UtilsFileSystem.getJarPath());
			if (path != null) {
				txtOrigen.setText(path);
			}
		});
		fieldsPanel.add(btnBrowseOrigen, gbc);

		// --> Fichero destino
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.0;
		fieldsPanel.add(new JLabel("Fichero Final (destino):"), gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		txtDestino = new JTextField("");
		txtDestino.setEditable(false);

		// Si se cambia el fichero destino --> se actualiza el fichero intermedio
		txtDestino.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				txtPipeline.setText(UtilsPipeline.getPipelineField(txtDestino.getText()));
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				txtPipeline.setText(UtilsPipeline.getPipelineField(txtDestino.getText()));
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				txtPipeline.setText(UtilsPipeline.getPipelineField(txtDestino.getText()));
			}
		});
		fieldsPanel.add(txtDestino, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.weightx = 0.0;
		JButton btnBrowseDestino = new JButton("...");
		btnBrowseDestino.addActionListener(e -> {
			String path = FileSelectorManager.selectFile(UtilsFileSystem.getJarPath());
			if (path != null) {
				txtDestino.setText(path); // Esto disparará automáticamente el listener de arriba
			}
		});
		fieldsPanel.add(btnBrowseDestino, gbc);

		// --> Fichero intermedio

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0.0;
		fieldsPanel.add(new JLabel("Estructura Pasos Intermedios:"), gbc);

		// Ocupa las columnas de campo y botón (gridwidth = 2)
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.weightx = 1.0;
		gbc.gridwidth = 2;
		txtPipeline = new JTextField("");
		txtPipeline.setEditable(false);
		txtPipeline.setBackground(new java.awt.Color(240, 240, 240)); // Visualmente deshabilitado
		fieldsPanel.add(txtPipeline, gbc);

		topContainer.add(fieldsPanel, BorderLayout.CENTER);
		add(topContainer, BorderLayout.NORTH);
	}

	/**
	 * Inicializar el panel gestiona las opciones del módulo individual a tratar.
	 * Contiene las secciones de la lista de tareas y el interfaz de los parámetros
	 */
	private void initJarPanel() {

		listModel = new DefaultListModel<>();
		taskList = new JListCustom(listModel);
		taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		cardLayout = new CardLayout();
		cardsPanel = new JPanel(cardLayout);

		lblDescription = new JLabel("Seleccione una tarea para ver sus parámetros");
		lblDescription.setFont(new Font("SansSerif", Font.ITALIC, 15));
		lblDescription.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 5));
		lblDescription.setForeground(Color.DARK_GRAY);

		rightPanel = new JPanel(new BorderLayout()); // <-- Quitamos el "JPanel" de delante
		rightPanel.add(lblDescription, BorderLayout.NORTH);
		rightPanel.add(cardsPanel, BorderLayout.CENTER);

		JSplitPane splitFormularios = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(taskList),
				rightPanel);
		splitFormularios.setDividerLocation(220);

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

	/**
	 * Gestiona el con las opciones de gestión del fichero resultante de secuencias
	 * (pipeline) Se muestra una representación de la configuración final de dicho
	 * fichero en una tabla, permite eliminar registros de la tabla. Se carga un
	 * fichero generado previamente y se guarda el fichero resultante.
	 * 
	 */
	private void initTaskPanel() {

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

		JButton btnLoadFile = new JButton("🗐 Cargar Pipeline Completo (.json)");
		btnLoadFile.setFont(new Font("SansSerif", Font.BOLD, 13));
		btnLoadFile.addActionListener(e -> loadPipelineFromFile());
		actionsPanel.add(btnLoadFile);

		JButton btnLimpiar = new JButton("🗷 Limpiar Todo");
		btnLimpiar.addActionListener(e -> tableModel.setRowCount(0));
		btnLimpiar.setFont(new Font("SansSerif", Font.BOLD, 13));
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

	/**
	 * Funcion de entrada para la inicialización de los diferentes componentes de la
	 * interfaz Punto de entrada de inicialización, llamará a los diferentes métodos
	 * auxiliares.
	 * 
	 * @throws IOException
	 */
	private void initComponents() throws IOException {
		/**
		 * Comprobamos que los ficheros de configuración están en los recursos y
		 * quitamos los recursos que no se encuen disponibles en el directorio
		 */
		this.jsonFiles = UtilsFileSystem.getJsonFilesInResources(UtilsFileSystem.getResourcesPath(), this.jsonFiles);
		setLayout(new BorderLayout(10, 10));

		this.initTopContainer();
		this.initJarPanel();
		this.initTaskPanel();

		if (configSelector.getItemCount() > 0) {
			String firstFile = configSelector.getItemAt(0);
			loadJsonConfiguration(firstFile);
		}
	}

	/**
	 * Carga el archivo JSON seleccionado, utiliz Jackson para obtener la
	 * configuración y reconstruir las pestañas.
	 */
	private void loadJsonConfiguration(String fileName) {
		try {
			// --> Obtiene el nuevo catálogo de tareas
			List<TaskConfig> tasksList = null;
			String absoluteFilePath = UtilsFileSystem.getResourcesPath() + "\\" + fileName;
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

			// --> Limpia los elementos del interfaz que contienen los datos anteriores
			listModel.clear();
			cardsPanel.removeAll();
			formsMap.clear();

			// --> Creamos un panel contenedor para los campos del JSON actual con un diseño
			// limpio
			JPanel jsonHeaderFieldsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

			// Gestionamos las particularidades (cmdOptions) de algunos catálogos, que al
			// ser comunes a todas las tareas posteriores
			// se deben incluir una única vez.
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
			// Nota: Para simplificar añadimos estos controles al contenedor 'rightPanel'
			// que limpiará su zona superior.
			actualizarCamposSuperioresDelJson(jsonHeaderFieldsPanel);

			// --> Carga de las tareas dinámicas
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

				// --> Seleccionamos la primera tarea del nuevo JSON
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

	/**
	 * Valida si la configuración de los elementos cmdOptions es correcta, pasa las
	 * validaciones.
	 * 
	 * @return boolean con el resultado de la validación.
	 */
	private boolean validateModuleCmdOptions() {
		boolean isValid = true;
		switch ((String) configSelector.getSelectedItem()) {
		case "config_csv.json":
			if (txtDelimiter.getText().trim().isEmpty()) {
				txtDelimiter.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
				txtDelimiter.setToolTipText("Este campo es obligatorio.");
				isValid = false;
			} else {
				txtDelimiter.setBorder(UIManager.getBorder("TextField.border"));
				txtDelimiter.setToolTipText("Delimitador de los elementos");
			}
			break;

		case "config_text.json":
			if (txtDefFile.getText().trim().isEmpty()) {
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

	/**
	 * Devuelve una cadena procesada para ser incluida en el fichero secuencial con
	 * las opciones de comando establecidas en el interfaz.
	 * 
	 * @return String con las opciones de comando (picocli) procesadas para poder
	 *         ser ejecutadas en el modulo/coordinador correspondiente.
	 */
	public String getSerializedCmdOptionsNode() {
		StringBuilder sb = new StringBuilder();
		switch ((String) configSelector.getSelectedItem()) {
		case "config_csv.json":
			sb.append(" -dc ").append(txtDelimiter.getText().trim());
			sb.append(" -c ").append(((String) cmbCharset.getSelectedItem()).trim());
			break;
		case "config_text.json":
			sb.append(" -dc ").append(txtDelimiter.getText().trim());
			sb.append(" -d ").append(txtDefFile.getText().trim());
			break;
		}
		return sb.toString();
	}

	/**
	 * Comprueba y añade una instrucción al pipeline.
	 * 
	 * @param siguientePaso        - numero con el valor del siguiente paso a inclur
	 * @param jsonOrigen           - fichero json con el catálogo de la tarea
	 * @param serializedCmdOptions - Cadena con las cmdOptions (si no tiene, será
	 *                             una cadena vacia)
	 * @param tareaCompleta        - String con la tarea completa
	 */
	private void addAndCheckInstruction(int siguientePaso, String jsonOrigen, String serializedCmdOptions,
			String tareaCompleta) {
		// --> Verificar si la tabla tiene al menos una fila guardada
		int totalFilas = tableModel.getRowCount();
		boolean esMismaInstruccion = false;
		boolean esTodoIgual = false;
		Object ultimoJson = null;
		Object ultimoCmd = null;
		Object ultimaTarea = null;

		if (totalFilas > 0) {
			int ultimaFilaIdx = totalFilas - 1;
			boolean cont = true;

			for (int i = ultimaFilaIdx; i >= 0 && cont; i--) {
				// --> Extraemos los valores de la última fila actual (la anterior a la que
				// queremos incluir)
				ultimoJson = tableModel.getValueAt(i, 1);
				ultimoCmd = tableModel.getValueAt(i, 2);
				ultimaTarea = tableModel.getValueAt(i, 3);
				if (ultimoJson != null) {
					cont = ultimoJson.equals("");
				}

			}

			// --> Comparamos las nuevas variables
			boolean jsonIgual = java.util.Objects.equals(ultimoJson, jsonOrigen);
			boolean cmdIgual = java.util.Objects.equals(ultimoCmd, serializedCmdOptions);
			boolean tareaIgual = java.util.Objects.equals(ultimaTarea, tareaCompleta);

			// --> Evaluamos las condiciones
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

	/**
	 * Incluye una instrucción a la tabla de instrucciones.
	 */
	private void addInstructionToPipeline() {

		TaskConfig selectedTask = taskList.getSelectedValue();
		String serializedCmdOptions = "";
		if (selectedTask == null) {
			JOptionPane.showMessageDialog(this, "Por favor, selecciona una tarea para añadir.", "Aviso",
					JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!this.validateModuleCmdOptions()) {
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
			int siguientePaso = tableModel.getRowCount();
			String jsonOrigen = (String) configSelector.getSelectedItem();
			this.addAndCheckInstruction(siguientePaso, jsonOrigen, serializedCmdOptions, tareaCompleta);

		}

	}

	/**
	 * Guarda la lista de tareas en un fichero.
	 */
	private void savePipelineToFile() {
		// --> Validamos para evitar generar un archivo vacío si no hay instrucciones
		if (tableModel.getRowCount() < 1) {
			JOptionPane.showMessageDialog(this,
					"La lista de instrucciones está vacía. Añade al menos una tarea antes de exportar.",
					"Validación de Datos", JOptionPane.WARNING_MESSAGE);
			return;
		}
		// --> Creamos un selector de archivos nativo de Swing
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Guardar Configuración de Pipeline Final");
		fileChooser.setSelectedFile(new File("pipeline_final.json"));

		int userSelection = fileChooser.showSaveDialog(this);

		// --> Si el usuario confirma la ruta y pulsa "Guardar"
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			try {
				// --> Construir el nodo raíz del JSON
				ObjectNode rootNode = objectMapper.createObjectNode();
				rootNode.put("inputFile", txtOrigen.getText());
				rootNode.put("outputFile", txtDestino.getText());
				rootNode.put("tempFile", txtPipeline.getText());

				// --> Crear el array contenedor de instrucciones principales
				ArrayNode instruccionesArray = objectMapper.createArrayNode();

				int filas = tableModel.getRowCount();
				int indexFile = 1; // Columna 1: Origen (JSON)
				int indexOptCmd = 2; // Columna 2: Opc Comando
				int indexTask = 3; // Columna 3: Instrucción / Tarea

				// --> Variables de control para la agrupación
				ObjectNode ultimoNodoPaso = null;
				ArrayNode ultimaListaTareas = null;

				String ultimoFicheroValido = "";
				String ultimoCmdValido = "";
				int contadorPasosReales = 0;

				for (int i = 0; i < filas; i++) {
					// --> Obtenemos los valores de la celda
					Object fileVal = tableModel.getValueAt(i, indexFile);
					Object cmdVal = tableModel.getValueAt(i, indexOptCmd);
					Object taskVal = tableModel.getValueAt(i, indexTask);

					String filaFichero = (fileVal != null) ? fileVal.toString().trim() : "";
					String filaCmd = (cmdVal != null) ? cmdVal.toString().trim() : "";
					String filaTarea = (taskVal != null) ? taskVal.toString().trim() : "";

					// --> Determinamos si es una fila agrupada (Fichero y Comando vacíos)
					boolean esFilaAgrupada = filaFichero.isEmpty() && filaCmd.isEmpty();

					if (esFilaAgrupada && ultimoNodoPaso != null && ultimaListaTareas != null) {
						// CASO A: Fila agrupada -> Añadir la tarea al array 'tasks' del paso anterior
						ObjectNode taskNode = objectMapper.createObjectNode();
						taskNode.put("task", filaTarea);
						ultimaListaTareas.add(taskNode);
					} else {
						// CASO B: Nuevo paso detectado (Hay datos o es la primera fila)
						contadorPasosReales++; // Solo incrementa si hay un cambio real

						// --> Si la fila actual vino en blanco pero es la primera o rompió el orden,
						// arrastramos los últimos valores válidos conocidos
						if (!filaFichero.isEmpty()) {
							ultimoFicheroValido = filaFichero;
						}
						if (!filaCmd.isEmpty()) {
							ultimoCmdValido = filaCmd;
						}

						// --> Crear el nuevo bloque de comando (paso)
						ultimoNodoPaso = objectMapper.createObjectNode();
						ultimoNodoPaso.put("jar", ConfigCatalog.getNodeJarForJsonFile(ultimoFicheroValido));
						ultimoNodoPaso.put("keepOnError", true);
						ultimoNodoPaso.put("step", String.valueOf(contadorPasosReales));

						if (!ultimoCmdValido.isEmpty()) {
							ultimoNodoPaso.put("cmdOptions", ultimoCmdValido);
						}

						// --> Inicializar el array de 'tasks' para este nuevo paso
						ultimaListaTareas = objectMapper.createArrayNode();
						ObjectNode taskNode = objectMapper.createObjectNode();
						taskNode.put("task", filaTarea);
						ultimaListaTareas.add(taskNode);

						// --> Vincular el array 'tasks' al paso actual
						ultimoNodoPaso.set("tasks", ultimaListaTareas);

						// --> Añadir el paso al listado global
						instruccionesArray.add(ultimoNodoPaso);
					}
				}

				// --> Vincular el array completo de instrucciones al nodo raíz
				rootNode.set("commands", instruccionesArray);
				try (FileWriter writer = new FileWriter(fileToSave)) {
					objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, rootNode);
				}

				JOptionPane.showMessageDialog(this,
						"¡Pipeline exportado con éxito!\nArchivo guardado en: " + fileToSave.getAbsolutePath(),
						"Operación Completada", JOptionPane.INFORMATION_MESSAGE);

			} catch (Exception ex) {
				// --> Captura y muestra de errores visuales en caso de fallo de escritura en
				// disco
				JOptionPane.showMessageDialog(this,
						"Error crítico al intentar escribir el archivo en el almacenamiento físico:\n"
								+ ex.getLocalizedMessage(),
						"Error de Guardado", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Cargamos un fichero de secuencias de disco a la aplicación.
	 * 
	 */
	private void loadPipelineFromFile() {
		// --> Crear el selector de archivos nativo de Swing para abrir
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Cargar Configuración de Pipeline");

		// Abrir por defecto en la carpeta de recursos
		File initialDir = new File(UtilsFileSystem.getJarPath());
		if (initialDir.exists() && initialDir.isDirectory()) {
			fileChooser.setCurrentDirectory(initialDir);
		}

		fileChooser
				.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos JSON (*.json)", "json"));

		int userSelection = fileChooser.showOpenDialog(this);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToLoad = fileChooser.getSelectedFile();
			try {
				// --> Leer y parsear el archivo JSON
				JsonNode rootNode = objectMapper.readTree(fileToLoad);

				// --> Rellenar los campos de texto principales
				txtOrigen.setText(rootNode.has("inputFile") ? rootNode.get("inputFile").asText() : "");
				txtDestino.setText(rootNode.has("outputFile") ? rootNode.get("outputFile").asText() : "");
				txtPipeline.setText(rootNode.has("tempFile") ? rootNode.get("tempFile").asText() : "");

				// --> Limpiar el modelo de la tabla antes de la carga
				tableModel.setRowCount(0);

				// --> Procesar el array de comandos ("commands")
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

								// Montamos la llamada a addAndCheckInstruction
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

								// Llamamos al método que incluye las tareas.
								this.addAndCheckInstruction(pasoId, jsonOrigenParam, cmdOptionsParam, taskText);

								// Incrementamos el contador global de filas
								filaActualIndex++;
							}
						}
					}
				}

				// --> Indicamos que se ha cargado correctamente
				JOptionPane.showMessageDialog(this, "¡Pipeline cargado con éxito!\nArchivo: " + fileToLoad.getName(),
						"Operación Completada", JOptionPane.INFORMATION_MESSAGE);

			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this,
						"Error crítico al intentar parsear o leer el archivo de configuración:\n"
								+ ex.getLocalizedMessage(),
						"Error de Carga", JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * Actualiza la zona de campos del cmdOptions
	 * 
	 * @param camposEspeciales
	 */
	private void actualizarCamposSuperioresDelJson(JPanel camposEspeciales) {
		if (rightPanel == null) {
			return;
		}

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

	/**
	 * Metodo auxiliar que permite eliminar una la inscurcción seleccionada en la
	 * tabla de la secuencia de comandos
	 */
	private void deleteSelectedInstruction() {
		int selectedRow = pipelineTable.getSelectedRow();

		// Validación: Verificar que haya una fila seleccionada
		if (selectedRow == -1) {
			JOptionPane.showMessageDialog(this, "Por favor, selecciona una fila de la tabla para eliminar.", "Aviso",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		// --> Elimina la fila del modelo
		tableModel.removeRow(selectedRow);

		// --> Recalcula secuencialmente la columna 0 (Número de Paso)
		int totRows = tableModel.getRowCount();
		for (int i = 0; i < totRows; i++) {
			// Asignamos el nuevo índice 'i' a la columna 0 de cada fila
			tableModel.setValueAt(i, i, 0);
		}
	}

	/*
	 * Crea el menú contextual de la tabla de tareas incluidas en la secuencia
	 */
	private void initTableContextMenu() {
		// --> Crea el menú flotante y su opción de borrado
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem deleteItem = new JMenuItem("Eliminar Instrucción");
		deleteItem.setIcon(javax.swing.UIManager.getIcon("InternalFrame.closeIcon")); // Icono nativo de aspa (opcional)

		// --> Asocia la acción de borrado al ítem de menú
		deleteItem.addActionListener(e -> deleteSelectedInstruction());
		popupMenu.add(deleteItem);

		// --> Se crea el escuchador de ratón en la tabla para detectar el clic derecho
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
