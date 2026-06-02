package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    
    // Tabla y colección persistente de instrucciones (Punto 3)
    private JTable pipelineTable;
    private DefaultTableModel tableModel;
    private final List<String> instruccionesAgregadas = new ArrayList<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JLabel lblDescription; 
    
    // Listado de tus 6 archivos de configuración JSON
    private final String[] jsonFiles = {
        "config_csv.json", 
        "config_excel.json",
        "config_convert.json"
    };

    public MainFrame() {
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

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

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

        JPanel fieldsPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        fieldsPanel.setBorder(BorderFactory.createTitledBorder("Estructura de Ficheros del Pipeline"));
        fieldsPanel.add(new JLabel("Fichero Inicial (origen):")); 
        txtOrigen = new JTextField("origen.xlsx"); 
        fieldsPanel.add(txtOrigen);
        
        fieldsPanel.add(new JLabel("Fichero Final (destino):")); 
        txtDestino = new JTextField("destino.xlsx"); 
        fieldsPanel.add(txtDestino);
        
        fieldsPanel.add(new JLabel("Estructura Pasos Intermedios:")); 
        txtPipeline = new JTextField("pipeline_[[PASO]].xlsx"); 
        fieldsPanel.add(txtPipeline);
        
        topContainer.add(fieldsPanel, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // =============================================================
        // 2. PANEL CENTRAL: Lista de Tareas e Interfaz de Parámetros
        // =============================================================
        listModel = new DefaultListModel<>();
        taskList = new JListCustom(listModel);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        // NUEVO: Inicializar la etiqueta de descripción con estilo limpio
        lblDescription = new JLabel("Seleccione una tarea para ver sus parámetros");
        lblDescription.setFont(new Font("SansSerif", Font.ITALIC, 13));
        lblDescription.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 5));
        lblDescription.setForeground(Color.DARK_GRAY);

        // NUEVO: Panel derecho que junta la descripción (arriba) y los formularios (centro)
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(lblDescription, BorderLayout.NORTH);
        rightPanel.add(cardsPanel, BorderLayout.CENTER);

        // MODIFICADO: Ahora el JSplitPane incluye 'rightPanel' en vez de 'cardsPanel' directamente
        JSplitPane splitFormularios = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(taskList), rightPanel);
        splitFormularios.setDividerLocation(220);
 
        
        taskList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
            	
                TaskConfig selected = taskList.getSelectedValue();
                if (selected != null) {
                    lblDescription.setText("<html><b>Descripción:</b> " + selected.getDescription() + "</html>");
                    	
                    // CORRECCIÓN: CardLayout requiere un String identificador, no el objeto completo
                    cardLayout.show(cardsPanel, selected.getTask()); 

                    
                }
            }
        });


        // Botón para registrar la instrucción actual en la cola
        JButton btnAddInstruction = new JButton("➕ Añadir Instrucción al Listado");
        btnAddInstruction.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnAddInstruction.addActionListener(e -> addInstructionToPipeline());

        JPanel centerContainer = new JPanel(new BorderLayout(5, 5));
        centerContainer.add(splitFormularios, BorderLayout.CENTER);
        centerContainer.add(btnAddInstruction, BorderLayout.SOUTH);

        // =============================================================
        // 3. PANEL INFERIOR: Tabla Acumulativa y Botón Guardar (Punto 1 y 3)
        // =============================================================
        JPanel bottomContainer = new JPanel(new BorderLayout(5, 5));
        bottomContainer.setBorder(BorderFactory.createTitledBorder("Instrucciones Acumuladas en el Pipeline Actual"));

        String[] columnas = {"Paso", "Origen (JSON)", "Instrucción / Tarea Generada"};
        tableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { 
                return false; 
            }
        };
        pipelineTable = new JTable(tableModel);
        pipelineTable.setPreferredScrollableViewportSize(new Dimension(500, 150));
        bottomContainer.add(new JScrollPane(pipelineTable), BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnLimpiar = new JButton("Limpiar Todo");
        btnLimpiar.addActionListener(e -> {
            tableModel.setRowCount(0);
            instruccionesAgregadas.clear();
        });
        actionsPanel.add(btnLimpiar);

        JButton btnSaveFile = new JButton("💾 Guardar Pipeline Completo (.json)");
        btnSaveFile.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSaveFile.addActionListener(e -> savePipelineToFile()); // Ejecuta el guardado físico
        actionsPanel.add(btnSaveFile);

        bottomContainer.add(actionsPanel, BorderLayout.SOUTH);
        
        // Separador maestro vertical para organizar formularios arriba e histórico abajo
        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerContainer, bottomContainer);
        mainSplit.setDividerLocation(360);
        add(mainSplit, BorderLayout.CENTER);
    }

    /**
     * Carga el archivo JSON seleccionado usando Jackson y reconstruye las pestañas.
     */
    private void loadJsonConfiguration(String fileName) {
        try {
            List<TaskConfig> tasksList = null;
            File externalFile = new File(fileName);
            
            if (externalFile.exists()) {
                tasksList = objectMapper.readValue(externalFile, new TypeReference<List<TaskConfig>>() {});
            } else {
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
                    if (is == null) {
                        throw new java.io.FileNotFoundException("No se encontró el fichero: " + fileName);
                    }
                    tasksList = objectMapper.readValue(is, new TypeReference<List<TaskConfig>>() {});
                }
            }

            listModel.clear();
            cardsPanel.removeAll();
            formsMap.clear();

            if (tasksList != null) {
                for (TaskConfig task : tasksList) {
                    String name = task.getTask();
                    
                    // CORRECCIÓN: Agrega el objeto TaskConfig en lugar de la variable String 'name'
                    listModel.addElement(task); 
                    
                    // Inyección automática de parámetros
                    DynamicFormPanel form = new DynamicFormPanel(task.getParams());
                    formsMap.put(name, form);
                    cardsPanel.add(form, name);
                }
                cardsPanel.revalidate();
                cardsPanel.repaint();
                taskList.setSelectedIndex(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar la configuración: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Métodos mock para evitar errores de compilación locales
    private void addInstructionToPipeline() { 
    	
    	
        TaskConfig selectedTask = taskList.getSelectedValue();
        if (selectedTask == null) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona una tarea para añadir.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DynamicFormPanel activeForm = formsMap.get(selectedTask.getTask());
        if(activeForm.validateForm())
        {
            String serializedParams = activeForm.getSerializedParams();

            String tareaCompleta = selectedTask.getTask();
            if (!serializedParams.isEmpty()) {
                tareaCompleta += "|" + serializedParams;
            }

            // Se guarda en la lista interna sin verse afectada si el usuario cambia de JSON
            instruccionesAgregadas.add(tareaCompleta);
            int siguientePaso = instruccionesAgregadas.size();
            String jsonOrigen = (String) configSelector.getSelectedItem();
            
            tableModel.addRow(new Object[]{siguientePaso, jsonOrigen, tareaCompleta});	
        }

    }
    
    private void savePipelineToFile() { 
        // Validación: Evitar generar un archivo vacío si no hay instrucciones añadidas
        if (instruccionesAgregadas.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "La lista de instrucciones está vacía. Añade al menos una tarea antes de exportar.", 
                "Validación de Datos", 
                JOptionPane.WARNING_MESSAGE);
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
                rootNode.put("ficheroInicial", txtOrigen.getText());
                rootNode.put("ficheroFinal", txtDestino.getText());
                rootNode.put("ficherosIntermedios", txtPipeline.getText());

                // 2. Crear el array contenedor de instrucciones principales
                ArrayNode instruccionesArray = objectMapper.createArrayNode();
                
                // 3. Iterar sobre la lista de instrucciones acumuladas (mantenidas de forma persistente)
                for (int i = 0; i < instruccionesAgregadas.size(); i++) {
                    ObjectNode pasoNode = objectMapper.createObjectNode();
                    pasoNode.put("jar", "NExcel.jar");
                    pasoNode.put("manteneSiError", true);
                    pasoNode.put("paso", String.valueOf(i + 1)); // El paso se autoincrementa según su orden (1, 2, 3...)

                    // Crear el array 'c' interno solicitado en tu formato plantilla
                    ArrayNode cArray = objectMapper.createArrayNode();
                    ObjectNode tareaNode = objectMapper.createObjectNode();
                    tareaNode.put("tarea", instruccionesAgregadas.get(i)); // Ej: "BorrarColumnasPosiciones|colPositions=3"
                    cArray.add(tareaNode);

                    // Vincular el array 'c' a la instrucción de este paso
                    pasoNode.set("c", cArray);
                    
                    // Añadir el bloque del paso al listado global de instrucciones
                    instruccionesArray.add(pasoNode);
                }

                // Vincular el array completo de instrucciones al nodo raíz
                rootNode.set("instrucciones", instruccionesArray);

                // 4. Escritura física final al disco duro aplicando sangrado/formato visual limpio (Pretty Print)
                try (FileWriter writer = new FileWriter(fileToSave)) {
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, rootNode);
                }

                // Notificación de éxito al usuario
                JOptionPane.showMessageDialog(this, 
                    "¡Pipeline exportado con éxito!\nArchivo guardado en: " + fileToSave.getAbsolutePath(), 
                    "Operación Completada", 
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                // Captura y muestra de errores visuales en caso de fallo de escritura en disco
                JOptionPane.showMessageDialog(this, 
                    "Error crítico al intentar escribir el archivo en el almacenamiento físico:\n" + ex.getMessage(), 
                    "Error de Guardado", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
