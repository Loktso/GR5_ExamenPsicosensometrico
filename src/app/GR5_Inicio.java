/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package app;
    
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import javax.swing.JOptionPane;

public class GR5_Inicio extends javax.swing.JFrame {

    private DefaultTableModel GR5_modeloTabla;

    /**
     * Constructor que inicializa la interfaz y carga usuarios.
     */
    public GR5_Inicio() {
        initComponents();
        setLocationRelativeTo(null); // Centrar ventana
        setDefaultCloseOperation(EXIT_ON_CLOSE); // Salir completamente al cerrar
        GR5_configurarTabla();
        GR5_cargarUsuarios();
    }
    /**
 * Verifica que todos los campos estén correctamente llenos y válidos.
 * @return true si los datos son válidos, false en caso contrario.
 */
private boolean GR5_camposValidos() {
    String nombre = txtGR5Nombre.getText().trim();
    String apellido = txtGR5Apellido.getText().trim();
    String cedula = txtGR5Cedula.getText().trim();

    // Validar campos vacíos
    if (nombre.isEmpty() || apellido.isEmpty() || cedula.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    // Validar nombre y apellido solo letras
    if (!nombre.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+")) {
        JOptionPane.showMessageDialog(this, "El nombre solo debe contener letras.", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }
    if (!apellido.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+")) {
        JOptionPane.showMessageDialog(this, "El apellido solo debe contener letras.", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    // Validar cédula: solo números y correcta
    if (!cedula.matches("\\d{10}")) {
        JOptionPane.showMessageDialog(this, "La cédula debe contener exactamente 10 dígitos.", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }
    if (!GR5_validarCedulaEcuatoriana(cedula)) {
        JOptionPane.showMessageDialog(this, "La cédula ingresada no es válida.", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    return true;
}
/**
 * Valida si una cédula ecuatoriana es correcta.
 * @param cedula Número de cédula como String de 10 dígitos.
 * @return true si la cédula es válida, false si no.
 */
private boolean GR5_validarCedulaEcuatoriana(String cedula) {
    int total = 0;
    int longitud = cedula.length();

    if (longitud != 10) return false;

    int digitoVerificador = Integer.parseInt(cedula.substring(9, 10));
    for (int i = 0; i < 9; i++) {
        int digito = Integer.parseInt(cedula.substring(i, i + 1));
        if (i % 2 == 0) {
            digito *= 2;
            if (digito > 9) digito -= 9;
        }
        total += digito;
    }

    int resultado = total % 10;
    if (resultado != 0) resultado = 10 - resultado;

    return resultado == digitoVerificador;
}


    /**
     * Configura el modelo de la tabla.
     */
    private void GR5_configurarTabla() {
        GR5_modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Apellido", "Cédula"}, 0
        ) {
            // Evitar edición directa en las celdas
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblGR5Usuarios.setModel(GR5_modeloTabla);
        // Ocultar columna ID si quieres
        tblGR5Usuarios.getColumnModel().getColumn(0).setMinWidth(0);
        tblGR5Usuarios.getColumnModel().getColumn(0).setMaxWidth(0);
    }
        /**
     * Carga todos los usuarios desde la base de datos y los muestra en la tabla.
     */
    private void GR5_cargarUsuarios() {
        GR5_modeloTabla.setRowCount(0); // Limpiar tabla

        try (Connection conn = GR5_DBConnexion.GR5_getConnection()) {
            String sql = "SELECT * FROM GR5_Usuarios";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Object[] fila = new Object[]{
                    rs.getInt("GR5_UsuarioID"),
                    rs.getString("GR5_Nombre"),
                    rs.getString("GR5_Apellido"),
                    rs.getString("GR5_Cedula")
                };
                GR5_modeloTabla.addRow(fila);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar usuarios: " + ex.getMessage());
        }
    }
        /**
     * Limpia los campos de texto.
     */
    private void GR5_limpiarCampos() {
        txtGR5Nombre.setText("");
        txtGR5Apellido.setText("");
        txtGR5Cedula.setText("");
    }
        /**
     * Agrega un nuevo usuario a la base de datos.
     */
    private void GR5_agregarUsuario() {
        if (!GR5_camposValidos()) {
    return; // Detiene el proceso si hay error
}
        String nombre = txtGR5Nombre.getText().trim();
        String apellido = txtGR5Apellido.getText().trim();
        String cedula = txtGR5Cedula.getText().trim();

        if (nombre.isEmpty() || apellido.isEmpty() || cedula.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, completa todos los campos.");
            return;
        }

        try (Connection conn = GR5_DBConnexion.GR5_getConnection()) {
            String sql = "INSERT INTO GR5_Usuarios (GR5_Nombre, GR5_Apellido, GR5_Cedula) VALUES (?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, nombre);
            pst.setString(2, apellido);
            pst.setString(3, cedula);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Usuario agregado correctamente.");
            GR5_cargarUsuarios();
            GR5_limpiarCampos();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al agregar usuario: " + ex.getMessage());
        }
    }

    /**
     * Modifica el usuario seleccionado.
     */
    private void GR5_modificarUsuario() {
        if (!GR5_camposValidos()) {
    return; // Detiene el proceso si hay error
}
        int filaSeleccionada = tblGR5Usuarios.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para modificar.");
            return;
        }

        int usuarioID = (int) GR5_modeloTabla.getValueAt(filaSeleccionada, 0);
        String nombre = txtGR5Nombre.getText().trim();
        String apellido = txtGR5Apellido.getText().trim();
        String cedula = txtGR5Cedula.getText().trim();

        if (nombre.isEmpty() || apellido.isEmpty() || cedula.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, completa todos los campos.");
            return;
        }

        try (Connection conn = GR5_DBConnexion.GR5_getConnection()) {
            String sql = "UPDATE GR5_Usuarios SET GR5_Nombre=?, GR5_Apellido=?, GR5_Cedula=? WHERE GR5_UsuarioID=?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, nombre);
            pst.setString(2, apellido);
            pst.setString(3, cedula);
            pst.setInt(4, usuarioID);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Usuario modificado correctamente.");
            GR5_cargarUsuarios();
            GR5_limpiarCampos();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al modificar usuario: " + ex.getMessage());
        }
    }

    /**
     * Elimina el usuario seleccionado.
     */
    private void GR5_eliminarUsuario() {
        int filaSeleccionada = tblGR5Usuarios.getSelectedRow();
        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar.");
            return;
        }

        int usuarioID = (int) GR5_modeloTabla.getValueAt(filaSeleccionada, 0);

        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Estás seguro de eliminar este usuario?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = GR5_DBConnexion.GR5_getConnection()) {
            String sql = "DELETE FROM GR5_Usuarios WHERE GR5_UsuarioID=?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, usuarioID);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Usuario eliminado correctamente.");
            GR5_cargarUsuarios();
            GR5_limpiarCampos();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar usuario: " + ex.getMessage());
        }
    }



    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtGR5Nombre = new javax.swing.JTextField();
        txtGR5Apellido = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtGR5Cedula = new javax.swing.JTextField();
        btnGR5Agregar = new javax.swing.JButton();
        btnGR5Modificar = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblGR5Usuarios = new javax.swing.JTable();
        btnGR5IniciarPrueba = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Pre-registro Examen");
        setPreferredSize(new java.awt.Dimension(800, 450));

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Elephant", 0, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 0, 0));
        jLabel1.setText("Registro de usuarios");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 10, 250, 40));

        jLabel2.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 0, 0));
        jLabel2.setText("Nombre:");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(33, 71, 120, -1));

        jLabel3.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 0, 0));
        jLabel3.setText("Apellido:");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(33, 111, 120, -1));

        txtGR5Nombre.setBackground(new java.awt.Color(153, 153, 153));
        jPanel1.add(txtGR5Nombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(157, 68, 150, -1));

        txtGR5Apellido.setBackground(new java.awt.Color(153, 153, 153));
        jPanel1.add(txtGR5Apellido, new org.netbeans.lib.awtextra.AbsoluteConstraints(157, 108, 150, -1));

        jLabel4.setFont(new java.awt.Font("SimSun", 1, 18)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 0, 0));
        jLabel4.setText("Cédula:");
        jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(33, 151, 110, -1));

        txtGR5Cedula.setBackground(new java.awt.Color(153, 153, 153));
        jPanel1.add(txtGR5Cedula, new org.netbeans.lib.awtextra.AbsoluteConstraints(157, 148, 150, -1));

        btnGR5Agregar.setBackground(new java.awt.Color(153, 153, 153));
        btnGR5Agregar.setFont(new java.awt.Font("SimSun-ExtB", 0, 12)); // NOI18N
        btnGR5Agregar.setText("Agregar");
        btnGR5Agregar.setPreferredSize(new java.awt.Dimension(90, 20));
        btnGR5Agregar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5AgregarActionPerformed(evt);
            }
        });
        jPanel1.add(btnGR5Agregar, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 210, 100, -1));

        btnGR5Modificar.setBackground(new java.awt.Color(153, 153, 153));
        btnGR5Modificar.setFont(new java.awt.Font("SimSun-ExtB", 0, 12)); // NOI18N
        btnGR5Modificar.setText("Actualizar");
        btnGR5Modificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5ModificarActionPerformed(evt);
            }
        });
        jPanel1.add(btnGR5Modificar, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 250, 100, -1));

        jButton1.setBackground(new java.awt.Color(153, 153, 153));
        jButton1.setFont(new java.awt.Font("SimSun-ExtB", 0, 12)); // NOI18N
        jButton1.setText("Eliminar");
        jButton1.setPreferredSize(new java.awt.Dimension(90, 20));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 290, 100, -1));

        tblGR5Usuarios.setBackground(new java.awt.Color(102, 102, 102));
        tblGR5Usuarios.setModel(new javax.swing.table.DefaultTableModel(
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
        tblGR5Usuarios.setGridColor(new java.awt.Color(102, 102, 102));
        tblGR5Usuarios.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblGR5UsuariosMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblGR5Usuarios);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(350, 60, 370, 210));

        btnGR5IniciarPrueba.setBackground(new java.awt.Color(153, 153, 153));
        btnGR5IniciarPrueba.setFont(new java.awt.Font("SimSun-ExtB", 0, 12)); // NOI18N
        btnGR5IniciarPrueba.setText("Acceder al examen");
        btnGR5IniciarPrueba.setEnabled(false);
        btnGR5IniciarPrueba.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGR5IniciarPruebaActionPerformed(evt);
            }
        });
        jPanel1.add(btnGR5IniciarPrueba, new org.netbeans.lib.awtextra.AbsoluteConstraints(340, 340, 150, -1));

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGR5AgregarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5AgregarActionPerformed
        GR5_agregarUsuario();
    }//GEN-LAST:event_btnGR5AgregarActionPerformed

    private void btnGR5ModificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5ModificarActionPerformed
        GR5_modificarUsuario();
    }//GEN-LAST:event_btnGR5ModificarActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        GR5_eliminarUsuario();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void tblGR5UsuariosMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblGR5UsuariosMouseClicked
        // Cargar datos de la fila seleccionada en los campos de texto
int filaSeleccionada = tblGR5Usuarios.getSelectedRow();
if (filaSeleccionada >= 0) {
    txtGR5Nombre.setText(tblGR5Usuarios.getValueAt(filaSeleccionada, 1).toString());
    txtGR5Apellido.setText(tblGR5Usuarios.getValueAt(filaSeleccionada, 2).toString());
    txtGR5Cedula.setText(tblGR5Usuarios.getValueAt(filaSeleccionada, 3).toString());
    btnGR5IniciarPrueba.setEnabled(true); // Habilitar solo si se selecciona usuario
}
    }//GEN-LAST:event_tblGR5UsuariosMouseClicked

    private void btnGR5IniciarPruebaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGR5IniciarPruebaActionPerformed
        int fila = tblGR5Usuarios.getSelectedRow();
    if (fila >= 0) {
        // Guardar datos del usuario seleccionado en la sesión global
        GR5_Sesion.usuarioID = (int) GR5_modeloTabla.getValueAt(fila, 0);
        GR5_Sesion.nombre = GR5_modeloTabla.getValueAt(fila, 1).toString();
        GR5_Sesion.apellido = GR5_modeloTabla.getValueAt(fila, 2).toString();
        GR5_Sesion.cedula = GR5_modeloTabla.getValueAt(fila, 3).toString();

        // Abrir la ventana del examen
        new GR5ExamenPsicosensometrico().setVisible(true);
        this.dispose(); // Opcional: cerrar la ventana actual
    } else {
        JOptionPane.showMessageDialog(this, "Por favor selecciona un usuario.");
    }// TODO add your handling code here:
    }//GEN-LAST:event_btnGR5IniciarPruebaActionPerformed

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
            java.util.logging.Logger.getLogger(GR5_Inicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GR5_Inicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GR5_Inicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GR5_Inicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GR5_Inicio().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGR5Agregar;
    private javax.swing.JButton btnGR5IniciarPrueba;
    private javax.swing.JButton btnGR5Modificar;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblGR5Usuarios;
    private javax.swing.JTextField txtGR5Apellido;
    private javax.swing.JTextField txtGR5Cedula;
    private javax.swing.JTextField txtGR5Nombre;
    // End of variables declaration//GEN-END:variables
}
