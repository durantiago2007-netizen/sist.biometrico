package com.sbi.servicio;

import com.sbi.dao.MarcacionDAO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/**
 * Lee el archivo XML que genera el biométrico Dahua (copiado por pendrive) y
 * registra cada marcación en la base de datos.
 *
 * Como el formato exacto puede variar según el modelo/firmware, el lector es flexible:
 * recorre el XML y toma como "registro" cualquier elemento que tenga (como atributo o
 * como sub-elemento) un ID de usuario y una fecha/hora. Nombres reconocidos (sin importar
 * mayúsculas, guiones o guiones bajos):
 *
 *   ID usuario : UserID, PersonID, EmployeeNo, CodigoBiometrico, IdUsuario, Codigo, CardNo
 *   Fecha/hora : CreateTime (epoch en segundos), Time, DateTime, AttendanceTime, FechaHora,
 *                Timestamp, o Fecha + Hora por separado
 *   Nombre     : CardName, Name, UserName, PersonName, Nombre
 *   Evento     : Type (Entry/Exit), AttendanceState (0=entrada, 1=salida...), Evento, CheckType
 *   Método     : Method (15=rostro, 4/6=huella, 1=tarjeta, 0=contraseña), Metodo, VerifyMode
 *   Equipo     : SerialNo, DeviceSN, NumeroSerie (si no viene, se usa el que elijas en la web)
 */
public class ImportadorDahuaXML {

    private static final List<String> K_ID = List.of("userid", "personid", "employeeno", "codigobiometrico",
            "idusuario", "idusuario", "codigo", "usercode", "pin", "cardno");
    private static final List<String> K_TIEMPO = List.of("createtime", "attendancetime", "datetime", "fechahora",
            "time", "timestamp", "tiempo", "punchtime", "recordtime");
    private static final List<String> K_FECHA = List.of("date", "fecha");
    private static final List<String> K_HORA = List.of("hora", "hour", "clocktime");
    private static final List<String> K_NOMBRE = List.of("cardname", "username", "personname", "name", "nombre");
    private static final List<String> K_EVENTO = List.of("type", "attendancestate", "evento", "eventodeasistencia",
            "inout", "direction", "checktype");
    private static final List<String> K_METODO = List.of("method", "metodo", "verifymode", "verifytype", "metododeasistencia");
    private static final List<String> K_SERIE = List.of("serialno", "devicesn", "sn", "numeroserie", "deviceserial");

    private final MarcacionDAO dao = new MarcacionDAO();

    /** Solo lee el XML (sin tocar la BD): útil para vista previa y pruebas. */
    public List<Map<String, Object>> vistaPrevia(String xml) throws Exception {
        Document doc = parsearSeguro(xml);
        List<Map<String, String>> registros = new ArrayList<>();
        recolectar(doc.getDocumentElement(), registros);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, String> r : registros) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigoBiometrico", valor(r, K_ID));
            m.put("nombre", valor(r, K_NOMBRE));
            LocalDateTime fh = fechaHora(r);
            m.put("fechaHora", fh == null ? null : fh.toString().replace('T', ' '));
            m.put("tipo", tipo(valor(r, K_EVENTO)));
            m.put("metodo", metodo(valor(r, K_METODO)));
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> importar(String xml, String serieElegida) throws Exception {
        return importar(xml, serieElegida, 0);
    }

    /**
     * @param ajusteHoras horas a sumar a cada fecha (ej. -11 si el reloj del equipo está 11 h adelantado).
     */
    public Map<String, Object> importar(String xml, String serieElegida, int ajusteHoras) throws Exception {
        Document doc = parsearSeguro(xml);
        if (esHojaExcel(doc)) return importarHojaExcel(doc, serieElegida, ajusteHoras);
        List<Map<String, String>> registros = new ArrayList<>();
        recolectar(doc.getDocumentElement(), registros);
        if (registros.isEmpty())
            throw new IllegalArgumentException("El XML no tiene registros reconocibles (se buscó un ID de usuario y una fecha/hora en cada elemento).");

        String serieArchivo = buscarSerieGlobal(doc.getDocumentElement());
        int ok = 0, duplicadas = 0, errores = 0, nuevos = 0;
        List<String> detalle = new ArrayList<>();
        int n = 0;
        for (Map<String, String> r : registros) {
            n++;
            String codigo = valor(r, K_ID);
            LocalDateTime fh = fechaHora(r);
            if (codigo == null || fh == null) { errores++; detalle.add("Registro " + n + ": falta ID o fecha válida"); continue; }
            fh = fh.plusHours(ajusteHoras);
            String serie = Optional.ofNullable(valor(r, K_SERIE)).orElse(serieArchivo != null ? serieArchivo : serieElegida);
            String res = dao.registrarConTipo(codigo.trim(), valor(r, K_NOMBRE), serie, fh,
                    tipo(valor(r, K_EVENTO)), metodo(valor(r, K_METODO)), "IMPORTACION");
            if (res.startsWith("OK")) { ok++; if (res.contains("NUEVO")) nuevos++; }
            else if (res.equals("DUPLICADA")) duplicadas++;
            else { errores++; detalle.add("Registro " + n + " (ID " + codigo + "): " + res); }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("formato", "XML");
        res.put("registrosEnArchivo", registros.size());
        res.put("importadas", ok);
        res.put("duplicadas", duplicadas);
        res.put("errores", errores);
        res.put("empleadosNuevos", nuevos);
        res.put("detalleErrores", detalle.size() > 50 ? detalle.subList(0, 50) : detalle);
        return res;
    }


    // =====================================================================
    //  FORMATO REAL DEL EQUIPO: hoja de Excel 2003 en XML (SpreadsheetML)
    //  El Dahua exporta al pendrive estos archivos:
    //   - Card_Punch_Records_N.xml  -> "Registros de asistencia" (marcaciones crudas)  => SE IMPORTA
    //   - Attendance_Shift.xml      -> "Turno de trabajo" (horarios)                   => SE IMPORTA a horarios
    //   - Statistics_Report_MM.xml  -> "Informe de asistencia" (resumen mensual)       => solo informativo
    //   - Time_Card1_MM.xml         -> "Tabla de Tarjetas de Asistencia" (grilla/mes)  => solo informativo
    // =====================================================================
    private static final String NS_SS = "urn:schemas-microsoft-com:office:spreadsheet";

    private static boolean esHojaExcel(Document d) {
        Element r = d.getDocumentElement();
        return "Workbook".equals(r.getLocalName() != null ? r.getLocalName() : r.getTagName())
                && NS_SS.equals(r.getNamespaceURI());
    }

    /** Convierte la primera hoja en una lista de filas (respeta ss:Index y ss:MergeAcross). */
    static List<List<String>> filas(Document d) {
        List<List<String>> out = new ArrayList<>();
        NodeList hojas = d.getElementsByTagNameNS(NS_SS, "Worksheet");
        if (hojas.getLength() == 0) return out;
        Element hoja = (Element) hojas.item(0);
        NodeList rows = hoja.getElementsByTagNameNS(NS_SS, "Row");
        for (int i = 0; i < rows.getLength(); i++) {
            List<String> fila = new ArrayList<>();
            for (Node c = rows.item(i).getFirstChild(); c != null; c = c.getNextSibling()) {
                if (!(c instanceof Element celda) || !"Cell".equals(celda.getLocalName())) continue;
                String idx = celda.getAttributeNS(NS_SS, "Index");
                if (!idx.isEmpty()) while (fila.size() < Integer.parseInt(idx) - 1) fila.add("");
                NodeList data = celda.getElementsByTagNameNS(NS_SS, "Data");
                fila.add(data.getLength() > 0 ? data.item(0).getTextContent().trim() : "");
                String merge = celda.getAttributeNS(NS_SS, "MergeAcross");
                if (!merge.isEmpty()) for (int k = 0; k < Integer.parseInt(merge); k++) fila.add("");
            }
            out.add(fila);
        }
        return out;
    }

    private static String nombreHoja(Document d) {
        NodeList hojas = d.getElementsByTagNameNS(NS_SS, "Worksheet");
        return hojas.getLength() == 0 ? "" : ((Element) hojas.item(0)).getAttributeNS(NS_SS, "Name");
    }

    private static String celda(List<String> f, int i) { return i >= 0 && i < f.size() ? f.get(i).trim() : ""; }

    private static int columna(List<String> cab, String... nombres) {
        for (int i = 0; i < cab.size(); i++) {
            String c = norm(cab.get(i));
            for (String n : nombres) if (c.equals(norm(n))) return i;
        }
        return -1;
    }

    private Map<String, Object> importarHojaExcel(Document doc, String serie, int ajusteHoras) throws Exception {
        List<List<String>> filas = filas(doc);
        String hoja = nombreHoja(doc);
        String titulo = filas.isEmpty() ? "" : celda(filas.get(0), 0);

        // ---- Registros de asistencia (marcaciones)
        for (int i = 0; i < Math.min(filas.size(), 10); i++) {
            List<String> cab = filas.get(i);
            int cId = columna(cab, "Núm.", "No. de usuario", "ID de usuario", "User ID");
            int cTiempo = columna(cab, "Tiempo registrado", "Tiempo", "Hora de registro", "Time");
            if (cId >= 0 && cTiempo >= 0) return importarMarcacionesExcel(filas, i, cab, serie, ajusteHoras);
        }
        // ---- Turno de trabajo (horarios)
        if (norm(hoja + titulo).contains("turnodetrabajo")) return importarTurnos(filas);

        // ---- Reportes calculados por el equipo (no traen marcaciones)
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("formato", "XML (Excel 2003)");
        r.put("tipoArchivo", hoja.isEmpty() ? titulo : hoja);
        r.put("registrosEnArchivo", 0);
        r.put("importadas", 0); r.put("duplicadas", 0); r.put("errores", 0); r.put("empleadosNuevos", 0);
        r.put("detalleErrores", List.of("Este archivo es un REPORTE calculado por el equipo (" + (hoja.isEmpty() ? titulo : hoja)
                + "). No contiene marcaciones: importa el archivo Card_Punch_Records_*.xml"));
        return r;
    }

    private Map<String, Object> importarMarcacionesExcel(List<List<String>> filas, int filaCab, List<String> cab,
                                                         String serie, int ajusteHoras) throws Exception {
        int cId = columna(cab, "Núm.", "No. de usuario", "ID de usuario", "User ID");
        int cNombre = columna(cab, "Nombre", "Name");
        int cTiempo = columna(cab, "Tiempo registrado", "Tiempo", "Hora de registro", "Time");
        int cModo = columna(cab, "Modo de reconocimiento", "Método de Asistencia", "Verify Mode");
        int cEstado = columna(cab, "Estado", "Status", "Result");
        int cEvento = columna(cab, "Estado de Asistencia", "Evento de Asistencia", "Attendance Status");

        int ok = 0, dup = 0, err = 0, nuevos = 0, fallidos = 0, total = 0;
        List<String> detalle = new ArrayList<>();
        for (int i = filaCab + 1; i < filas.size(); i++) {
            List<String> f = filas.get(i);
            String tiempo = celda(f, cTiempo);
            if (tiempo.isEmpty()) continue;
            total++;
            String codigo = celda(f, cId);
            String estado = celda(f, cEstado).toLowerCase();
            if (codigo.isEmpty() || estado.contains("error") || estado.contains("fail")) { fallidos++; continue; }
            LocalDateTime fh = ImportadorDahuaCSV.parsearFecha(tiempo);
            if (fh == null) { err++; detalle.add("Fila " + (i + 1) + ": fecha inválida '" + tiempo + "'"); continue; }
            fh = fh.plusHours(ajusteHoras);
            String res = dao.registrarConTipo(codigo, celda(f, cNombre), serie, fh,
                    tipo(celda(f, cEvento).isEmpty() ? null : celda(f, cEvento)), metodo(celda(f, cModo)), "IMPORTACION");
            if (res.startsWith("OK")) { ok++; if (res.contains("NUEVO")) nuevos++; }
            else if (res.equals("DUPLICADA")) dup++;
            else { err++; detalle.add("Fila " + (i + 1) + " (ID " + codigo + "): " + res); }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("formato", "XML (Excel 2003)");
        r.put("tipoArchivo", "Registros de asistencia");
        r.put("registrosEnArchivo", total);
        r.put("importadas", ok);
        r.put("duplicadas", dup);
        r.put("intentosFallidos", fallidos);
        r.put("errores", err);
        r.put("empleadosNuevos", nuevos);
        r.put("ajusteHoras", ajusteHoras);
        r.put("detalleErrores", detalle.size() > 50 ? detalle.subList(0, 50) : detalle);
        return r;
    }

    /** Attendance_Shift.xml -> tabla horarios (solo turnos con nombre propio, no los "Predet."). */
    private Map<String, Object> importarTurnos(List<List<String>> filas) throws Exception {
        int guardados = 0;
        List<String> detalle = new ArrayList<>();
        for (List<String> f : filas) {
            String num = celda(f, 0), nombre = celda(f, 1), ent = celda(f, 2), sal = celda(f, 3), tarde = celda(f, 8);
            if (!num.matches("\\d+") || nombre.isEmpty() || !ent.matches("\\d{1,2}:\\d{2}")) continue;
            if (nombre.toLowerCase().startsWith("predet") && !num.equals("2")) continue;   // evita 20 copias del turno por defecto
            String n = "Turno " + num + " - " + nombre;
            int tol = tarde.matches("\\d+") ? Integer.parseInt(tarde) : 5;
            dao.guardarHorario(n, ent + ":00", sal + ":00", tol);
            guardados++;
            detalle.add(n + " (" + ent + " a " + sal + ", tolerancia " + tol + " min)");
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("formato", "XML (Excel 2003)");
        r.put("tipoArchivo", "Turno de trabajo");
        r.put("registrosEnArchivo", guardados);
        r.put("importadas", 0); r.put("duplicadas", 0); r.put("errores", 0); r.put("empleadosNuevos", 0);
        r.put("horariosGuardados", guardados);
        r.put("detalleErrores", detalle);
        return r;
    }

    /** Vista previa genérica: primeras filas de cualquier hoja Excel-XML. */
    public static List<List<String>> filasDeHoja(String xml, int max) throws Exception {
        List<List<String>> f = filas(parsearSeguro(xml));
        return f.size() > max ? f.subList(0, max) : f;
    }

    // ------------------------------------------------------------ lectura del XML
    /** Parser con protección contra XXE (archivos XML maliciosos). */
    static Document parsearSeguro(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        DocumentBuilder b = f.newDocumentBuilder();
        String limpio = xml.replace("﻿", "").trim();
        return b.parse(new ByteArrayInputStream(limpio.getBytes(StandardCharsets.UTF_8)));
    }

    /** Recorre el árbol: si un elemento tiene ID + fecha, es un registro; si no, baja a sus hijos. */
    private void recolectar(Element e, List<Map<String, String>> salida) {
        Map<String, String> campos = campos(e);
        if (valor(campos, K_ID) != null && (valor(campos, K_TIEMPO) != null || valor(campos, K_FECHA) != null)) {
            salida.add(campos);
            return;
        }
        for (Node h = e.getFirstChild(); h != null; h = h.getNextSibling())
            if (h instanceof Element hijo) recolectar(hijo, salida);
    }

    /** Atributos + sub-elementos de texto directos, con nombre normalizado. */
    private static Map<String, String> campos(Element e) {
        Map<String, String> m = new LinkedHashMap<>();
        NamedNodeMap at = e.getAttributes();
        for (int i = 0; i < at.getLength(); i++) m.put(norm(at.item(i).getNodeName()), at.item(i).getNodeValue());
        for (Node h = e.getFirstChild(); h != null; h = h.getNextSibling()) {
            if (h instanceof Element hijo && !tieneHijosElemento(hijo))
                m.putIfAbsent(norm(hijo.getTagName()), hijo.getTextContent());
        }
        return m;
    }

    private static boolean tieneHijosElemento(Element e) {
        for (Node h = e.getFirstChild(); h != null; h = h.getNextSibling()) if (h instanceof Element) return true;
        return false;
    }

    private String buscarSerieGlobal(Element raiz) {
        String s = valor(campos(raiz), K_SERIE);
        if (s != null) return s;
        for (String k : List.of("SerialNo", "DeviceSN", "NumeroSerie")) {
            NodeList nl = raiz.getElementsByTagName(k);
            if (nl.getLength() > 0 && !nl.item(0).getTextContent().isBlank()) return nl.item(0).getTextContent().trim();
        }
        return null;
    }

    private static String norm(String s) {
        String n = s.contains(":") ? s.substring(s.indexOf(':') + 1) : s;   // quita prefijos de namespace
        return n.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String valor(Map<String, String> m, List<String> claves) {
        for (String k : claves) {
            String v = m.get(k);
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    // ------------------------------------------------------------ interpretación de valores
    private static LocalDateTime fechaHora(Map<String, String> r) {
        String t = valor(r, K_TIEMPO);
        if (t != null) {
            if (t.matches("\\d{9,13}")) {                                   // epoch (segundos o milisegundos)
                long v = Long.parseLong(t);
                Instant i = t.length() > 10 ? Instant.ofEpochMilli(v) : Instant.ofEpochSecond(v);
                return LocalDateTime.ofInstant(i, ZoneId.systemDefault());
            }
            LocalDateTime fh = ImportadorDahuaCSV.parsearFecha(t);
            if (fh != null) return fh;
        }
        String f = valor(r, K_FECHA), h = valor(r, K_HORA);
        if (f != null && h != null) return ImportadorDahuaCSV.parsearFecha(f + " " + h);
        return f != null ? ImportadorDahuaCSV.parsearFecha(f) : null;
    }

    /** ENTRADA / SALIDA, o null si el XML no lo dice (entonces se calcula: 1ª del día = entrada). */
    static String tipo(String v) {
        if (v == null) return null;
        String s = v.toLowerCase();
        if (s.matches(".*(exit|salida|out|check-?out|egreso).*")) return "SALIDA";
        if (s.matches(".*(entry|entrada|ingreso|in|check-?in).*")) return "ENTRADA";
        if (s.matches("\\d+")) {                    // AttendanceState numérico
            int n = Integer.parseInt(s);
            return (n == 1 || n == 2 || n == 5) ? "SALIDA" : "ENTRADA";
        }
        return null;
    }

    static String metodo(String v) {
        if (v == null) return "OTRO";
        String s = v.toLowerCase();
        if (s.contains("face") || s.contains("facial") || s.contains("rostro") || s.contains("cara") || s.matches(".*\\b15\\b.*")) return "ROSTRO";
        if (s.contains("finger") || s.contains("huella") || s.matches("^(4|6)$")) return "HUELLA";
        if (s.contains("card") || s.contains("tarjeta") || s.equals("1")) return "TARJETA";
        if (s.contains("password") || s.contains("contrase") || s.equals("0")) return "PASSWORD";
        return "OTRO";
    }
}
