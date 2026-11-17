package com.sbi.servicio;

import com.sbi.dao.MarcacionDAO;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Importa el archivo de registros que exporta el biométrico Dahua
 * (menú del equipo: Registros/Records > Exportar a USB, o desde SmartPSS: Attendance > Export).
 * Guarda el archivo como CSV (si viene en .xlsx, ábrelo en Excel y "Guardar como CSV").
 *
 * Detecta automáticamente:
 *  - el separador ( , ; o tabulación )
 *  - la columna del ID de usuario (encabezados como "User ID", "Person ID", "ID", "No.", "Código")
 *  - la columna de fecha/hora ("Time", "Date Time", "Fecha", "Hora", "Attendance Time")
 */
public class ImportadorDahuaCSV {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));

    private final MarcacionDAO dao = new MarcacionDAO();

    public Map<String, Object> importar(String contenidoCsv, String numeroSerie) throws Exception {
        int ok = 0, duplicadas = 0, errores = 0;
        List<String> detalleErrores = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new StringReader(contenidoCsv))) {
            String cabecera = br.readLine();
            if (cabecera == null) throw new IllegalArgumentException("El archivo está vacío");
            cabecera = cabecera.replace("﻿", "");           // quita BOM de Excel
            String sep = detectarSeparador(cabecera);
            String[] cols = dividir(cabecera, sep);

            int colId = buscarColumna(cols, "user id", "person id", "userid", "id usuario", "código", "codigo", "no.", "id");
            int colFecha = buscarColumna(cols, "attendance time", "date time", "datetime", "time", "fecha hora", "fecha", "hora");
            if (colId < 0 || colFecha < 0)
                throw new IllegalArgumentException("No se encontraron las columnas de ID y Fecha/Hora en: " + cabecera);

            String linea;
            int n = 1;
            while ((linea = br.readLine()) != null) {
                n++;
                if (linea.isBlank()) continue;
                String[] v = dividir(linea, sep);
                if (v.length <= Math.max(colId, colFecha)) { errores++; detalleErrores.add("Línea " + n + ": columnas incompletas"); continue; }
                String codigo = v[colId].trim();
                LocalDateTime fecha = parsearFecha(v[colFecha].trim());
                if (codigo.isEmpty() || fecha == null) { errores++; detalleErrores.add("Línea " + n + ": dato inválido"); continue; }

                String r = dao.registrar(codigo, numeroSerie, fecha, "HUELLA", "IMPORTACION");
                if (r.startsWith("OK")) ok++;
                else if (r.equals("DUPLICADA")) duplicadas++;
                else { errores++; detalleErrores.add("Línea " + n + " (ID " + codigo + "): " + r); }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("importadas", ok);
        res.put("duplicadas", duplicadas);
        res.put("errores", errores);
        res.put("detalleErrores", detalleErrores.size() > 50 ? detalleErrores.subList(0, 50) : detalleErrores);
        return res;
    }

    public static LocalDateTime parsearFecha(String texto) {
        String t = texto.replace("T", " ").replace("\"", "").trim();
        for (DateTimeFormatter f : FORMATOS) {
            try { return LocalDateTime.parse(t, f); } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    private static String detectarSeparador(String cabecera) {
        if (cabecera.contains("\t")) return "\t";
        if (cabecera.contains(";")) return ";";
        return ",";
    }

    private static String[] dividir(String linea, String sep) {
        String[] p = linea.split(java.util.regex.Pattern.quote(sep), -1);
        for (int i = 0; i < p.length; i++) p[i] = p[i].replace("\"", "").trim();
        return p;
    }

    private static int buscarColumna(String[] cols, String... candidatos) {
        for (String c : candidatos)                       // primero coincidencia exacta
            for (int i = 0; i < cols.length; i++)
                if (cols[i].equalsIgnoreCase(c)) return i;
        for (String c : candidatos)                       // luego "contiene"
            for (int i = 0; i < cols.length; i++)
                if (c.length() > 3 && cols[i].toLowerCase().contains(c)) return i;
        return -1;
    }
}
