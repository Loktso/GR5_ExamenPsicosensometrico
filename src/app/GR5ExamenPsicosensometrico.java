/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package app;
import java.awt.CardLayout;
import javax.swing.SwingUtilities;
// IMPORTACIÓN NECESARIA PARA SERIAL COM
import com.fazecast.jSerialComm.SerialPort;
import java.awt.Color;
import java.io.InputStream;
// IMPORTACION PAA EXPORTAR EL ARCHIVO A CVS
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 * @author stev_
 */
public class GR5ExamenPsicosensometrico extends javax.swing.JFrame {
    /**
     * Creates new form GR5ExamenPsicosensometrico
     */
    // VARIABLES PARA COMUNICACIÓN SERIAL Y ESTADO DE PRUEBA
private SerialPort puertoGR5Seleccionado;
private Thread hiloEscuchaGR5;
private long tiempoInicioGR5;
private long tiempoFinalGR5 = 0;
private int contadorGR5Aciertos = 0;
private int contadorGR5Errores = 0;
private String ultimoEstimuloGR5 = ""; // Variable para almacenar el último estímulo enviado
private long sumaTiemposReaccionGR5 = 0; // Acumula los tiempos de reacción



    public GR5ExamenPsicosensometrico() {
        initComponents();
        setLocationRelativeTo(null); // Centrar ventana
        // Deshabilitar cierre con X
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    
    // Añadir listener personalizado para el cierre
    this.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent windowEvent) {
            GR5_MostrarConfirmacionSalir();
        }
    });
        // Mostrar inicialmente el panel de prueba
        mostrarPanel("prueba");
        configurarCombobox();
        lblGR5EstimuloActual.setText("—");
lblGR5ResultadoReaccion.setText("—");
lblGR5TiempoReaccion.setText("—");
lblGR5Aciertos.setText("0");
lblGR5Errores.setText("0");
lblGR5NombreUsuario.setText(GR5_Sesion.nombre + " " + GR5_Sesion.apellido);
lblGR5CedulaUsuario.setText("C.I: " + GR5_Sesion.cedula);
tablaHistorialGR5.setModel(new DefaultTableModel(
    new Object[][]{},
    new String[]{"Aciertos", "Errores", "Tiempo Promedio (ms)", "Fecha"}
));
    }
        /**
     * Cambia el panel visible en el contenido central.
     * @param nombrePanel Puede ser: "prueba", "historial", "configuracion"
     */
    private void mostrarPanel(String nombrePanel) {
        CardLayout cl = (CardLayout)(panelGR5Contenido.getLayout());
        cl.show(panelGR5Contenido, nombrePanel);
    }
    private void GR5_MostrarConfirmacionSalir() {
    int confirm = JOptionPane.showConfirmDialog(
        this,
        "¿Estás seguro que deseas salir?",
        "Confirmar salida",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );
    
    if (confirm == JOptionPane.YES_OPTION) {
        // Cerrar conexión serial si está abierta
        if (puertoGR5Seleccionado != null && puertoGR5Seleccionado.isOpen()) {
            try {
                puertoGR5Seleccionado.getOutputStream().write("DETENER\n".getBytes());
                puertoGR5Seleccionado.closePort();
            } catch (IOException ex) {
                System.err.println("Error al cerrar puerto: " + ex.getMessage());
            }
        }
        
        // Mostrar ventana de inicio
        new GR5_Inicio().setVisible(true);
        this.dispose();
    }
}
    /**
 * Detecta automáticamente todos los puertos disponibles y retorna el primero que tenga nombre similar a "USB" o "COM".
 * @return Puerto Serial detectado o null si no se encuentra.
 */
private SerialPort detectarPuertoGR5() {
    SerialPort[] puertos = SerialPort.getCommPorts();
    for (SerialPort puerto : puertos) {
        if (puerto.getSystemPortName().toLowerCase().contains("usb") ||
            puerto.getSystemPortName().toLowerCase().contains("com")) {
            return puerto;
        }
    }
    return null;
}
/**
 * Inicia la conexión con Arduino usando el puerto detectado automáticamente.
 */
private void conectarConArduinoGR5() {
    // Cierra hilo anterior si estaba activo
    if (hiloEscuchaGR5 != null && hiloEscuchaGR5.isAlive()) {
        hiloEscuchaGR5.interrupt();
        hiloEscuchaGR5 = null;
    }

    puertoGR5Seleccionado = detectarPuertoGR5();   
    if (puertoGR5Seleccionado != null) {
        puertoGR5Seleccionado.setBaudRate(9600);

        try {
            if (puertoGR5Seleccionado.isOpen()) {
                puertoGR5Seleccionado.closePort();
                Thread.sleep(1000);
            }

            if (puertoGR5Seleccionado.openPort()) {
                System.out.println("✅ Puerto abierto correctamente");
                lblGR5EstadoConexion.setText("🟢 Conectado a " + puertoGR5Seleccionado.getSystemPortName());

                // ✅ Iniciar escucha directa SIN esperar primer mensaje
                System.out.println("▶️ Iniciando escucha directa del puerto...");
                escucharPuertoSerialGR5();

            } else {
                lblGR5EstadoConexion.setText("🔴 Error al conectar");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            lblGR5EstadoConexion.setText("❌ Error al abrir puerto");
        }
    } else {
        lblGR5EstadoConexion.setText("🔴 No se encontró puerto USB");
    }
}


/**
 * Escucha el puerto serial y procesa los mensajes recibidos desde Arduino.
 */
/**
 * Escucha el puerto serial en segundo plano y procesa los mensajes de Arduino.
 */
private void escucharPuertoSerialGR5() {
    hiloEscuchaGR5 = new Thread(() -> {
        System.out.println("👂 Escuchando puerto serial...");
        try {
            InputStream in = puertoGR5Seleccionado.getInputStream();
            StringBuilder buffer = new StringBuilder();
            
            while (!Thread.interrupted() && puertoGR5Seleccionado.isOpen()) {
                while (puertoGR5Seleccionado.bytesAvailable() > 0) {
                    int data = in.read();
                    if (data == -1) continue;
                    
                    buffer.append((char) data);
                    
                    // Procesar cuando tenemos una línea completa
                    if (data == '\n') {
                        final String lineaCompleta = buffer.toString().trim();
                        buffer.setLength(0); // Limpiar buffer
                        
                        if (!lineaCompleta.isEmpty()) {
                            System.out.println("📌 RX: " + lineaCompleta);
                            
                            SwingUtilities.invokeLater(() -> {
                                procesarLineaDesdeArduinoGR5(lineaCompleta);
                            });
                        }
                    }
                }
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> 
                lblGR5EstadoConexion.setText("⚠️ Error leyendo datos"));
        }
    });
    hiloEscuchaGR5.setDaemon(true);
    hiloEscuchaGR5.start();
}



/**
 * Procesa una línea recibida desde Arduino.
 * @param linea Mensaje recibido.
 */
private void procesarLineaDesdeArduinoGR5(String linea) {
    System.out.println("🔍 Procesando: " + linea);

    try {
        if (linea.startsWith("GR5_ESTIMULO:")) {
            String[] partes = linea.substring(13).split(",");
            
            // Procesamiento seguro con verificación de índices
            String led = partes.length > 0 ? partes[0].trim().replace("LED", "").trim() : "OFF";
            String buzzer = partes.length > 1 ? partes[1].trim().replace("BZ", "").trim() : "OFF";
            String esCorrecto = partes.length > 2 ? partes[2].trim() : "";

            // Actualizar UI
            SwingUtilities.invokeLater(() -> {
                lblGR5EstimuloActual.setText(String.format("LED: %s | BZ: %s", 
                    led.equals("OFF") ? "OFF" : led, 
                    buzzer.equals("OFF") ? "OFF" : buzzer));
                
                if (esCorrecto.equals("CORRECTO")) {
                    lblGR5EstimuloActual.setForeground(Color.GREEN);
                    tiempoInicioGR5 = System.currentTimeMillis();
                } else {
                    lblGR5EstimuloActual.setForeground(Color.RED);
                }
            });
        } 
        else if (linea.startsWith("GR5_RESPUESTA:")) {
            String respuesta = linea.substring(14).trim();
            
            SwingUtilities.invokeLater(() -> {
                lblGR5ResultadoReaccion.setText(respuesta);
                
                if (respuesta.equals("ACIERTO")) {
                    lblGR5ResultadoReaccion.setForeground(Color.GREEN);
                    contadorGR5Aciertos++;
                    lblGR5Aciertos.setText(String.valueOf(contadorGR5Aciertos));
                    
                    // Calcular tiempo de reacción solo para respuestas correctas
                    if (tiempoInicioGR5 > 0) {
                        long tiempoReaccion = System.currentTimeMillis() - tiempoInicioGR5;
                        lblGR5TiempoReaccion.setText(tiempoReaccion + " ms");
                        sumaTiemposReaccionGR5 += tiempoReaccion;
                        tiempoInicioGR5 = 0; // Resetear para el próximo estímulo
                    }
                } else {
                    lblGR5ResultadoReaccion.setForeground(Color.RED);
                    contadorGR5Errores++;
                    lblGR5Errores.setText(String.valueOf(contadorGR5Errores));
                }
            });
        }
    } catch (Exception e) {
        System.err.println("⚠️ Error procesando línea: " + linea);
        e.printStackTrace();
    }
}
/**
 * Guarda en la base de datos los resultados del usuario actual
 */
private void guardarResultadosEnBD(int aciertos, int errores, double tiempoPromedio) {
    try (Connection conn = GR5_DBConnexion.GR5_getConnection()) {
        // Validar y ajustar el tiempo promedio
        double tiempoAjustado = tiempoPromedio;
        
        // Si no hay aciertos, establecer tiempo como 0 o NULL
        if (aciertos == 0) {
            tiempoAjustado = 0; // o puedes usar setNull(4, Types.DOUBLE)
        }
        
        // Verificar límites (ejemplo para columna DECIMAL(10,2))
        if (tiempoAjustado < 0) tiempoAjustado = 0;
        if (tiempoAjustado > 999999.99) tiempoAjustado = 999999.99;
        
        String sql = "INSERT INTO GR5_Resultados (GR5_UsuarioID, GR5_Aciertos, GR5_Errores, GR5_TiempoPromedio) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, GR5_Sesion.usuarioID);
        pst.setInt(2, aciertos);
        pst.setInt(3, errores);
        pst.setDouble(4, tiempoAjustado);
        
        pst.executeUpdate();
        System.out.println("✅ Resultado guardado correctamente. Tiempo: " + tiempoAjustado + " ms");
        
    } catch (Exception ex) {
        System.err.println("❌ Error al guardar resultados: " + ex.getMessage());
        ex.printStackTrace();
    }
}
/**
 * Elimina todos los campos usados durante la prueba
 * y los deja listos para una nueva evaluacion
 */
private void GR5_ResetearInterfaz() {
    lblGR5EstimuloActual.setText("—");
    lblGR5EstimuloActual.setForeground(Color.BLACK);
    lblGR5ResultadoReaccion.setText("—");
    lblGR5ResultadoReaccion.setForeground(Color.BLACK);
    lblGR5TiempoReaccion.setText("—");
    lblGR5Aciertos.setText("0");
    lblGR5Errores.setText("0");
    sumaTiemposReaccionGR5 = 0;
    tiempoInicioGR5 = 0;
}
/**
 * Carga el historial de resultados del usuario actual desde la base de datos
 * y los muestra en la tabla del historial.
 */
private void cargarHistorialDesdeBD() {
    DefaultTableModel modelo = (DefaultTableModel) tablaHistorialGR5.getModel();
    modelo.setColumnIdentifiers(new String[]{"Aciertos", "Errores", "Tiempo Promedio (ms)", "Fecha"}); // Nuevos encabezados
    
    modelo.setRowCount(0); // Limpiar tabla

    String sql = """
        SELECT GR5_Aciertos, GR5_Errores, GR5_TiempoPromedio, GR5_Fecha
        FROM GR5_Resultados
        WHERE GR5_UsuarioID = ?
        ORDER BY GR5_Fecha DESC
    """;

    try (Connection conn = GR5_DBConnexion.GR5_getConnection();
         PreparedStatement pst = conn.prepareStatement(sql)) {
        pst.setInt(1, GR5_Sesion.usuarioID);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            modelo.addRow(new Object[]{
                rs.getInt("GR5_Aciertos"),
                rs.getInt("GR5_Errores"),
                rs.getDouble("GR5_TiempoPromedio"),
                rs.getTimestamp("GR5_Fecha")
            });
        }
        btnExportarCSVGR5.setEnabled(modelo.getRowCount() > 0);

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error al cargar historial: " + ex.getMessage());
    }
}

/**
 * Exporta el contenido de la tabla del historial a un archivo CSV.
 */
private void exportarHistorialACSV() {
    if (tablaHistorialGR5.getRowCount() == 0) {
        JOptionPane.showMessageDialog(this, "No hay datos para exportar.");
        return;
    }
    
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Guardar historial como CSV");
    
    String nombreArchivoDefault = "historial_psicometrico_" + GR5_Sesion.cedula + ".csv";
    fileChooser.setSelectedFile(new File(nombreArchivoDefault));
    
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File archivo = fileChooser.getSelectedFile();
        if (!archivo.getName().toLowerCase().endsWith(".csv")) {
            archivo = new File(archivo.getAbsolutePath() + ".csv");
        }

        try (FileWriter fw = new FileWriter(archivo)) {
            // Escribir encabezado con datos del usuario
            fw.write("Usuario: " + GR5_Sesion.nombre + " " + GR5_Sesion.apellido + "\n");
            fw.write("Cédula: " + GR5_Sesion.cedula + "\n\n");
            
            // Escribir encabezados de columnas
            TableModel modelo = tablaHistorialGR5.getModel();
            fw.write("Aciertos;Errores;Tiempo Promedio (ms);Fecha\n");
            
            // Escribir datos
            for (int i = 0; i < modelo.getRowCount(); i++) {
                fw.write(
                    modelo.getValueAt(i, 0) + ";" +  // Aciertos
                    modelo.getValueAt(i, 1) + ";" +  // Errores
                    modelo.getValueAt(i, 2) + ";" +  // Tiempo
                    modelo.getValueAt(i, 3) + "\n"    // Fecha
                );
            }

            JOptionPane.showMessageDialog(this, "Historial exportado correctamente:\n" + archivo.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al exportar: " + ex.getMessage());
        }
    }
}

/**
 * Envía la configuración seleccionada al Arduino
 */
private void GR5_EnviarConfiguracionArduino() {
    if (puertoGR5Seleccionado == null || !puertoGR5Seleccionado.isOpen()) {
        JOptionPane.showMessageDialog(this, 
            "Arduino no está conectado", 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        // 1. Obtener valores seleccionados
        int intensidad = cbxGR5Intensidad.getSelectedIndex() + 1;
        int velocidad = cbxGR5Velocidad.getSelectedIndex() + 1;
        String combinacion = (String) cbxGR5Combinacion.getSelectedItem();
        
        // 2. Construir comando con lógica exacta
        StringBuilder comando = new StringBuilder("CONFIG:");
        comando.append("LI").append(intensidad).append(",");
        comando.append("BI").append(intensidad).append(",");
        
        // Lógica exacta para cada caso
        switch(combinacion) {
            case "SOLO LED":
                comando.append("LT,BF,"); // LT=LED True, BF=Buzzer False
                break;
            case "SOLO BUZZER":
                comando.append("LF,BT,"); // LF=LED False, BT=Buzzer True
                break;
            case "AMBOS":
                comando.append("LT,BT,");  // Ambos True
                break;
            default:
                comando.append("LF,BF,");  // Por defecto, ambos False
        }
        
        comando.append("V").append(velocidad).append("\n");
        
        // 3. Enviar comando
        puertoGR5Seleccionado.getOutputStream().write(comando.toString().getBytes());
        puertoGR5Seleccionado.getOutputStream().flush();
        
        // 4. Debug detallado
        System.out.println("[DEBUG] Comando enviado: " + comando.toString().trim());
        
    } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, 
            "Error al enviar configuración: " + ex.getMessage(), 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
    }
}
private void configurarCombobox() {
    // Configurar ComboBox de Intensidad
    cbxGR5Intensidad.setModel(new DefaultComboBoxModel<>(new String[]{
        "BAJA (1)", 
        "MEDIA (2)", 
        "ALTA (3)"
    }));
    cbxGR5Intensidad.setSelectedIndex(1); // MEDIA por defecto
    
    // Configurar ComboBox de Velocidad
    cbxGR5Velocidad.setModel(new DefaultComboBoxModel<>(new String[]{
        "LENTA (1)", 
        "MEDIA (2)", 
        "RÁPIDA (3)"
    }));
    cbxGR5Velocidad.setSelectedIndex(1); // MEDIA por defecto
    
    // Configurar ComboBox de Combinación
    cbxGR5Combinacion.setModel(new DefaultComboBoxModel<>(new String[]{
        "SOLO LED", 
        "SOLO BUZZER", 
        "AMBOS"
    }));
    cbxGR5Combinacion.setSelectedIndex(2); // AMBOS por defecto
    
    // Añadir tooltips
    cbxGR5Intensidad.setToolTipText("Intensidad para LED y Buzzer (1-3)");
    cbxGR5Velocidad.setToolTipText("Velocidad de cambio de estímulos");
    cbxGR5Combinacion.setToolTipText("Combinación requerida para respuesta correcta");
}




    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        panelGR5Izq = new javax.swing.JPanel();
        btnGR5Iniciar = new javax.swing.JButton();
        btnGR5Historial = new javax.swing.JButton();
        btnGR5Configuracion = new javax.swing.JButton();
        btnGR5Salir = new javax.swing.JButton();
        lblGR5NombreUsuario = new javax.swing.JLabel();
        lblGR5CedulaUsuario = new javax.swing.JLabel();
        panelGR5Contenido = new javax.swing.JPanel();
        panelGR5Historial = new javax.swing.JPanel();
        panelGR5ContenidoHistorial = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaHistorialGR5 = new javax.swing.JTable();
        btnExportarCSVGR5 = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        panelGR5Prueba = new javax.swing.JPanel();
        panelGR5ContenidoPrueba = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        lblGR5EstimuloActual = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        lblGR5TiempoReaccion = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        lblGR5ResultadoReaccion = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        lblGR5Aciertos = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        lblGR5Errores = new javax.swing.JLabel();
        btnGR5IniciarSesion = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        lblGR5EstadoConexion = new javax.swing.JLabel();
        btnGR5DetenerSesion = new javax.swing.JButton();
        btnGR5ConectarArduino = new javax.swing.JButton();
        panelGR5Configuracion = new javax.swing.JPanel();
        panelGR5CoontenidoConfiguracion = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        cbxGR5Intensidad = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        cbxGR5Velocidad = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        cbxGR5Combinacion = new javax.swing.JComboBox<>();
        btnGR5GuardarConfig = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Examen Psicosensometrico Grupo 5");
        setPreferredSize(new java.awt.Dimension(750, 530));

        jSplitPane1.setBackground(new java.awt.Color(0, 0, 0));
        jSplitPane1.setDividerLocation(180);

        panelGR5Izq.setBackground(new java.awt.Color(0, 0, 0));
        panelGR5Izq.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnGR5Iniciar.setBackground(new java.awt.Color(204, 0, 51));
        btnGR5Iniciar.setForeground(new java.awt.Color(255, 255, 255));
        btnGR5Iniciar.setText("Iniciar");
        btnGR5Iniciar.setPreferredSize(new java.awt.Dimension(150, 30));
        btnGR5Iniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5IniciarActionPerformed(evt);
            }
        });
        panelGR5Izq.add(btnGR5Iniciar, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 130, -1, -1));

        btnGR5Historial.setBackground(new java.awt.Color(204, 0, 51));
        btnGR5Historial.setForeground(new java.awt.Color(255, 255, 255));
        btnGR5Historial.setText("Ver Historial");
        btnGR5Historial.setPreferredSize(new java.awt.Dimension(150, 30));
        btnGR5Historial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5HistorialActionPerformed(evt);
            }
        });
        panelGR5Izq.add(btnGR5Historial, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 170, -1, -1));

        btnGR5Configuracion.setBackground(new java.awt.Color(204, 0, 51));
        btnGR5Configuracion.setForeground(new java.awt.Color(255, 255, 255));
        btnGR5Configuracion.setText("Configuracion");
        btnGR5Configuracion.setPreferredSize(new java.awt.Dimension(150, 30));
        btnGR5Configuracion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5ConfiguracionActionPerformed(evt);
            }
        });
        panelGR5Izq.add(btnGR5Configuracion, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 210, -1, -1));

        btnGR5Salir.setBackground(new java.awt.Color(204, 0, 51));
        btnGR5Salir.setForeground(new java.awt.Color(255, 255, 255));
        btnGR5Salir.setText("Salir");
        btnGR5Salir.setPreferredSize(new java.awt.Dimension(150, 30));
        btnGR5Salir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5SalirActionPerformed(evt);
            }
        });
        panelGR5Izq.add(btnGR5Salir, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 250, -1, -1));

        lblGR5NombreUsuario.setForeground(new java.awt.Color(255, 255, 255));
        lblGR5NombreUsuario.setText("jLabel8");
        panelGR5Izq.add(lblGR5NombreUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 410, -1, -1));

        lblGR5CedulaUsuario.setForeground(new java.awt.Color(255, 255, 255));
        lblGR5CedulaUsuario.setText("jLabel8");
        panelGR5Izq.add(lblGR5CedulaUsuario, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 440, -1, -1));

        jSplitPane1.setLeftComponent(panelGR5Izq);

        panelGR5Contenido.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        panelGR5Contenido.setLayout(new java.awt.CardLayout());

        panelGR5Historial.setBackground(new java.awt.Color(255, 255, 102));
        panelGR5Historial.setLayout(new java.awt.BorderLayout());

        panelGR5ContenidoHistorial.setBackground(new java.awt.Color(0, 0, 0));
        panelGR5ContenidoHistorial.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane1.setBackground(new java.awt.Color(255, 255, 255));

        tablaHistorialGR5.setBackground(new java.awt.Color(102, 102, 102));
        tablaHistorialGR5.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tablaHistorialGR5.setEnabled(false);
        tablaHistorialGR5.setGridColor(new java.awt.Color(102, 102, 102));
        jScrollPane1.setViewportView(tablaHistorialGR5);

        panelGR5ContenidoHistorial.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 490, 231));

        btnExportarCSVGR5.setBackground(new java.awt.Color(0, 204, 204));
        btnExportarCSVGR5.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        btnExportarCSVGR5.setText("Exportar como CVS");
        btnExportarCSVGR5.setEnabled(false);
        btnExportarCSVGR5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportarCSVGR5ActionPerformed(evt);
            }
        });
        panelGR5ContenidoHistorial.add(btnExportarCSVGR5, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 360, -1, -1));

        jLabel11.setFont(new java.awt.Font("SimSun", 1, 24)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("REGSTRO DE TODOS SUS EXAMENES");
        panelGR5ContenidoHistorial.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 30, -1, -1));

        panelGR5Historial.add(panelGR5ContenidoHistorial, java.awt.BorderLayout.CENTER);

        panelGR5Contenido.add(panelGR5Historial, "historial");

        panelGR5Prueba.setLayout(new java.awt.BorderLayout());

        panelGR5ContenidoPrueba.setBackground(new java.awt.Color(0, 0, 0));
        panelGR5ContenidoPrueba.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Modern No. 20", 3, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("PRUEBA PSICOSENSOMÉTRICA");
        panelGR5ContenidoPrueba.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 30, -1, -1));

        jLabel2.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Estimulo actual:");
        panelGR5ContenidoPrueba.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 90, -1, -1));

        lblGR5EstimuloActual.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        lblGR5EstimuloActual.setForeground(new java.awt.Color(255, 255, 255));
        panelGR5ContenidoPrueba.add(lblGR5EstimuloActual, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 90, 190, 20));

        jLabel3.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Tiempo de reacción:");
        panelGR5ContenidoPrueba.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 140, -1, -1));

        lblGR5TiempoReaccion.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        lblGR5TiempoReaccion.setForeground(new java.awt.Color(255, 255, 255));
        lblGR5TiempoReaccion.setText("jLabel4");
        panelGR5ContenidoPrueba.add(lblGR5TiempoReaccion, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 140, 180, -1));

        jLabel4.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Resultado:");
        panelGR5ContenidoPrueba.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 200, -1, -1));

        lblGR5ResultadoReaccion.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        lblGR5ResultadoReaccion.setForeground(new java.awt.Color(255, 255, 255));
        lblGR5ResultadoReaccion.setText("jLabel5");
        panelGR5ContenidoPrueba.add(lblGR5ResultadoReaccion, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 190, 190, -1));

        jLabel5.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("\tAciertos:");
        panelGR5ContenidoPrueba.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 260, -1, -1));

        lblGR5Aciertos.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        lblGR5Aciertos.setForeground(new java.awt.Color(255, 255, 255));
        lblGR5Aciertos.setText("jLabel6");
        panelGR5ContenidoPrueba.add(lblGR5Aciertos, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 260, -1, -1));

        jLabel6.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("\tErrores:");
        panelGR5ContenidoPrueba.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 260, -1, -1));

        lblGR5Errores.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        lblGR5Errores.setForeground(new java.awt.Color(255, 255, 255));
        lblGR5Errores.setText("jLabel7");
        panelGR5ContenidoPrueba.add(lblGR5Errores, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 260, -1, -1));

        btnGR5IniciarSesion.setBackground(new java.awt.Color(102, 255, 102));
        btnGR5IniciarSesion.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        btnGR5IniciarSesion.setForeground(new java.awt.Color(255, 255, 255));
        btnGR5IniciarSesion.setText("Correr");
        btnGR5IniciarSesion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5IniciarSesionActionPerformed(evt);
            }
        });
        panelGR5ContenidoPrueba.add(btnGR5IniciarSesion, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 320, -1, -1));

        jLabel7.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Estado conexión:");
        panelGR5ContenidoPrueba.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 380, -1, -1));

        lblGR5EstadoConexion.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        lblGR5EstadoConexion.setForeground(new java.awt.Color(255, 255, 255));
        lblGR5EstadoConexion.setText("No conectado ");
        panelGR5ContenidoPrueba.add(lblGR5EstadoConexion, new org.netbeans.lib.awtextra.AbsoluteConstraints(330, 370, -1, -1));

        btnGR5DetenerSesion.setBackground(new java.awt.Color(255, 0, 0));
        btnGR5DetenerSesion.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        btnGR5DetenerSesion.setForeground(new java.awt.Color(255, 255, 255));
        btnGR5DetenerSesion.setText("Detener");
        btnGR5DetenerSesion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5DetenerSesionActionPerformed(evt);
            }
        });
        panelGR5ContenidoPrueba.add(btnGR5DetenerSesion, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 320, -1, -1));

        btnGR5ConectarArduino.setBackground(new java.awt.Color(0, 204, 204));
        btnGR5ConectarArduino.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        btnGR5ConectarArduino.setForeground(new java.awt.Color(255, 255, 255));
        btnGR5ConectarArduino.setText("conectar");
        btnGR5ConectarArduino.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5ConectarArduinoActionPerformed(evt);
            }
        });
        panelGR5ContenidoPrueba.add(btnGR5ConectarArduino, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 420, -1, -1));

        panelGR5Prueba.add(panelGR5ContenidoPrueba, java.awt.BorderLayout.CENTER);

        panelGR5Contenido.add(panelGR5Prueba, "prueba");

        panelGR5Configuracion.setBackground(new java.awt.Color(0, 0, 0));
        panelGR5Configuracion.setLayout(new java.awt.BorderLayout());

        panelGR5CoontenidoConfiguracion.setBackground(new java.awt.Color(0, 0, 0));
        panelGR5CoontenidoConfiguracion.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel8.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Intensidad:");
        panelGR5CoontenidoConfiguracion.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 120, -1, -1));

        cbxGR5Intensidad.setBackground(new java.awt.Color(102, 102, 102));
        cbxGR5Intensidad.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        cbxGR5Intensidad.setForeground(new java.awt.Color(255, 255, 255));
        cbxGR5Intensidad.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        panelGR5CoontenidoConfiguracion.add(cbxGR5Intensidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 120, 180, -1));

        jLabel9.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("Velocidad del estímulo:");
        panelGR5CoontenidoConfiguracion.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 190, -1, -1));

        cbxGR5Velocidad.setBackground(new java.awt.Color(102, 102, 102));
        cbxGR5Velocidad.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        cbxGR5Velocidad.setForeground(new java.awt.Color(255, 255, 255));
        cbxGR5Velocidad.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        panelGR5CoontenidoConfiguracion.add(cbxGR5Velocidad, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 190, 180, -1));

        jLabel10.setFont(new java.awt.Font("SimSun", 1, 24)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("ESTABLECE LOS PARAMETROS DEL EXAMEN");
        panelGR5CoontenidoConfiguracion.add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 50, -1, -1));

        cbxGR5Combinacion.setBackground(new java.awt.Color(102, 102, 102));
        cbxGR5Combinacion.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        cbxGR5Combinacion.setForeground(new java.awt.Color(255, 255, 255));
        cbxGR5Combinacion.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        panelGR5CoontenidoConfiguracion.add(cbxGR5Combinacion, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 250, 180, -1));

        btnGR5GuardarConfig.setBackground(new java.awt.Color(0, 204, 204));
        btnGR5GuardarConfig.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        btnGR5GuardarConfig.setForeground(new java.awt.Color(255, 255, 255));
        btnGR5GuardarConfig.setText("Guardar Configuracion");
        btnGR5GuardarConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5GuardarConfigActionPerformed(evt);
            }
        });
        panelGR5CoontenidoConfiguracion.add(btnGR5GuardarConfig, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 340, -1, -1));

        jLabel12.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Combinación válida:");
        panelGR5CoontenidoConfiguracion.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 250, -1, -1));

        panelGR5Configuracion.add(panelGR5CoontenidoConfiguracion, java.awt.BorderLayout.CENTER);

        panelGR5Contenido.add(panelGR5Configuracion, "configuracion");

        jSplitPane1.setRightComponent(panelGR5Contenido);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGR5IniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5IniciarActionPerformed
        mostrarPanel("prueba");
    }//GEN-LAST:event_btnGR5IniciarActionPerformed

    private void btnGR5HistorialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5HistorialActionPerformed
        mostrarPanel("historial");
        cargarHistorialDesdeBD();
    }//GEN-LAST:event_btnGR5HistorialActionPerformed

    private void btnGR5ConfiguracionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5ConfiguracionActionPerformed
        mostrarPanel("configuracion");
    }//GEN-LAST:event_btnGR5ConfiguracionActionPerformed

    private void btnGR5IniciarSesionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5IniciarSesionActionPerformed
        if (puertoGR5Seleccionado != null && puertoGR5Seleccionado.isOpen()) {
        try {
            puertoGR5Seleccionado.getOutputStream().write("INICIAR\n".getBytes());
            puertoGR5Seleccionado.getOutputStream().flush();
            System.out.println("📤 Comando 'INICIAR' enviado a Arduino");
            lblGR5EstadoConexion.setText("▶️ Prueba en curso...");
        } catch (Exception ex) {
            System.err.println("❌ Error al enviar comando a Arduino: " + ex.getMessage());
        }
    } else {
        lblGR5EstadoConexion.setText("🔴 Puerto no conectado");
    }
    }//GEN-LAST:event_btnGR5IniciarSesionActionPerformed

    private void btnGR5DetenerSesionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5DetenerSesionActionPerformed
        if (puertoGR5Seleccionado != null && puertoGR5Seleccionado.isOpen()) {
        try {
            // 1. Enviar comando DETENER
            puertoGR5Seleccionado.getOutputStream().write("DETENER\n".getBytes());
            puertoGR5Seleccionado.getOutputStream().flush();
            
            // 2. Calcular tiempo promedio (con validación)
            double tiempoPromedio = 0;
            if (contadorGR5Aciertos > 0) {
                tiempoPromedio = (double) sumaTiemposReaccionGR5 / contadorGR5Aciertos;
                tiempoPromedio = Math.min(tiempoPromedio, 999999.99); // Asegurar límite máximo
            }
            
            // 3. Guardar resultados
            guardarResultadosEnBD(contadorGR5Aciertos, contadorGR5Errores, tiempoPromedio);
            
            // 4. Resetear interfaz
            GR5_ResetearInterfaz();
            
            // 5. Mensaje de estado
            lblGR5EstadoConexion.setText("⏹ Prueba finalizada");
            System.out.println("Prueba detenida. Resultados guardados.");
            
        } catch (Exception ex) {
            System.err.println("Error al detener: " + ex.getMessage());
        }
    }
    }//GEN-LAST:event_btnGR5DetenerSesionActionPerformed

    private void btnGR5ConectarArduinoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5ConectarArduinoActionPerformed
        if (puertoGR5Seleccionado == null || !puertoGR5Seleccionado.isOpen()) {
        conectarConArduinoGR5(); // SOLO se hace una vez
    } else {
        lblGR5EstadoConexion.setText("✅ Ya conectado");
    }// TODO add your handling code here:
    }//GEN-LAST:event_btnGR5ConectarArduinoActionPerformed

    private void btnExportarCSVGR5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarCSVGR5ActionPerformed
        exportarHistorialACSV();
    }//GEN-LAST:event_btnExportarCSVGR5ActionPerformed

    private void btnGR5GuardarConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5GuardarConfigActionPerformed
        GR5_EnviarConfiguracionArduino();
    }//GEN-LAST:event_btnGR5GuardarConfigActionPerformed

    private void btnGR5SalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5SalirActionPerformed
        // Cerrar conexión serial si está abierta
    if (puertoGR5Seleccionado != null && puertoGR5Seleccionado.isOpen()) {
        try {
            puertoGR5Seleccionado.getOutputStream().write("DETENER\n".getBytes());
            puertoGR5Seleccionado.closePort();
        } catch (IOException ex) {
            System.err.println("Error al cerrar puerto: " + ex.getMessage());
        }
    }
    
    // Mostrar ventana de inicio
    new GR5_Inicio().setVisible(true);
    
    // Cerrar esta ventana
    this.dispose();
    }//GEN-LAST:event_btnGR5SalirActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GR5ExamenPsicosensometrico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GR5ExamenPsicosensometrico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GR5ExamenPsicosensometrico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GR5ExamenPsicosensometrico.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GR5ExamenPsicosensometrico().setVisible(true);
            }
        });
    }
    @Override
public void dispose() {
    super.dispose();
    if (puertoGR5Seleccionado != null && puertoGR5Seleccionado.isOpen()) {
        puertoGR5Seleccionado.closePort();
    }
    if (hiloEscuchaGR5 != null) {
        hiloEscuchaGR5.interrupt();
    }
}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnExportarCSVGR5;
    private javax.swing.JButton btnGR5ConectarArduino;
    private javax.swing.JButton btnGR5Configuracion;
    private javax.swing.JButton btnGR5DetenerSesion;
    private javax.swing.JButton btnGR5GuardarConfig;
    private javax.swing.JButton btnGR5Historial;
    private javax.swing.JButton btnGR5Iniciar;
    private javax.swing.JButton btnGR5IniciarSesion;
    private javax.swing.JButton btnGR5Salir;
    private javax.swing.JComboBox<String> cbxGR5Combinacion;
    private javax.swing.JComboBox<String> cbxGR5Intensidad;
    private javax.swing.JComboBox<String> cbxGR5Velocidad;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel lblGR5Aciertos;
    private javax.swing.JLabel lblGR5CedulaUsuario;
    private javax.swing.JLabel lblGR5Errores;
    private javax.swing.JLabel lblGR5EstadoConexion;
    private javax.swing.JLabel lblGR5EstimuloActual;
    private javax.swing.JLabel lblGR5NombreUsuario;
    private javax.swing.JLabel lblGR5ResultadoReaccion;
    private javax.swing.JLabel lblGR5TiempoReaccion;
    private javax.swing.JPanel panelGR5Configuracion;
    private javax.swing.JPanel panelGR5Contenido;
    private javax.swing.JPanel panelGR5ContenidoHistorial;
    private javax.swing.JPanel panelGR5ContenidoPrueba;
    private javax.swing.JPanel panelGR5CoontenidoConfiguracion;
    private javax.swing.JPanel panelGR5Historial;
    private javax.swing.JPanel panelGR5Izq;
    private javax.swing.JPanel panelGR5Prueba;
    private javax.swing.JTable tablaHistorialGR5;
    // End of variables declaration//GEN-END:variables
}
