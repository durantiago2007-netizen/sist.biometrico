package com.sbi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Entrega conexiones JDBC a la base de datos sistema_biometrico_inteligente. */
public final class Conexion {

    private Conexion() { }

    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(
                Configuracion.get("db.url"),
                Configuracion.get("db.usuario"),
                Configuracion.get("db.password "));
    }

    /** Prueba rápida: ejecuta este archivo solo (Shift+F6 en NetBeans). */
    public static void main(String[] args) {
        try (Connection cn = obtener()) {
            System.out.println("Conexión exitosa a: " + cn.getCatalog());
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        }
    }
}
