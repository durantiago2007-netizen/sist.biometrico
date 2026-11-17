package com.sbi.config;

import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Lee db.properties (primero junto al .jar, si no existe, el que está dentro del proyecto). */
public final class Configuracion {

    private static final Properties PROPS = new Properties();

    static {
        try {
            Path externo = Path.of("db.properties");
            if (Files.exists(externo)) {
                try (InputStream in = new FileInputStream(externo.toFile())) { PROPS.load(in); }
            } else {
                try (InputStream in = Configuracion.class.getResourceAsStream("/db.properties")) {
                    if (in == null) throw new IllegalStateException("No se encontró db.properties");
                    PROPS.load(in);
                }
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Configuracion() { }

    public static String get(String clave) { return PROPS.getProperty(clave, "").trim(); }

    public static int getInt(String clave, int porDefecto) {
        try { return Integer.parseInt(get(clave)); } catch (NumberFormatException e) { return porDefecto; }
    }
}
