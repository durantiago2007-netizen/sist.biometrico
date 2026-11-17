package com.sbi;

import com.sbi.api.ServidorApi;
import com.sbi.config.Configuracion;
import com.sbi.config.Conexion;
import java.sql.Connection;

/** Punto de entrada. En NetBeans: clic derecho al proyecto > Run (F6). */
public class Main {
    public static void main(String[] args) throws Exception {
        try (Connection cn = Conexion.obtener()) {
            System.out.println("[OK] Conectado a MySQL: " + cn.getCatalog());
        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo conectar a MySQL: " + e.getMessage());
            System.err.println("Revisa que MySQL esté encendido y los datos de src/main/resources/db.properties");
            return;
        }
        int puerto = Configuracion.getInt("servidor.puerto", 8080);
        new ServidorApi().iniciar(puerto);
        System.out.println("[OK] Sistema Biométrico Inteligente en http://localhost:" + puerto);
        System.out.println("     Usuario: admin   Contraseña: admin123");
    }
}
