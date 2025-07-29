/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package app;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase para gestionar la conexión con la base de datos MySQL.
 * Usa el patrón singleton para evitar múltiples conexiones simultáneas.
 * Prefijo GR5 en clase y métodos para estandarización.
 */
public class GR5_DBConnexion {

    // URL de conexión a la base de datos (cambia usuario, pass, host si es necesario)
    private static final String GR5_DB_URL = "jdbc:mysql://localhost:3306/PsicoAppDB";
    private static final String GR5_DB_USER = "root";
    private static final String GR5_DB_PASS = "geobixsis";

    // Instancia única de la conexión
    private static Connection GR5_connection = null;

    /**
     * Obtiene la conexión a la base de datos MySQL.
     * @return Connection objeto de conexión
     * @throws SQLException si falla la conexión
     */
    public static Connection GR5_getConnection() throws SQLException {
        if (GR5_connection == null || GR5_connection.isClosed()) {
            GR5_connection = DriverManager.getConnection(GR5_DB_URL, GR5_DB_USER, GR5_DB_PASS);
        }
        return GR5_connection;
    }
}
