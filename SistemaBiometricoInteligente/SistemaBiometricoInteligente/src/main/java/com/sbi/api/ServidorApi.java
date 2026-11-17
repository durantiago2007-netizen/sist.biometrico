package com.sbi.api;

import com.google.gson.*;
import com.sbi.config.Configuracion;
import com.sbi.dao.*;
import com.sbi.modelo.*;
import com.sbi.servicio.ImportadorDahuaCSV;
import com.sbi.servicio.ImportadorDahuaXML;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP (incluido en el JDK, no necesita Tomcat).
 * - Sirve la interfaz web desde src/main/resources/web
 * - Expone la API REST en /api/...
 */
public class ServidorApi {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final Map<String, Usuario> sesiones = new ConcurrentHashMap<>();

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final MarcacionDAO marcacionDAO = new MarcacionDAO();
    private final AsistenciaDAO asistenciaDAO = new AsistenciaDAO();
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private HttpServer server;

    public void iniciar(int puerto) throws IOException {
        server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/api/", this::api);
        server.createContext("/", this::estatico);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    public void detener() { if (server != null) server.stop(0); }

    // ------------------------------------------------------------------ API
    private void api(HttpExchange ex) throws IOException {
        String metodo = ex.getRequestMethod();
        String ruta = ex.getRequestURI().getPath().replaceAll("/+$", "");
        Map<String, String> q = query(ex);
        try {
            if (metodo.equals("OPTIONS")) { responder(ex, 204, null); return; }

            // --- Rutas públicas
            if (ruta.equals("/api/login") && metodo.equals("POST")) { login(ex); return; }
            if (ruta.equals("/api/dahua/evento") && metodo.equals("POST")) { eventoDahua(ex, q); return; }

            // --- Rutas protegidas
            if (usuarioActual(ex) == null) { error(ex, 401, "No autorizado. Inicie sesión."); return; }

            switch (metodo + " " + ruta.replaceAll("/\\d+$", "/{id}")) {
                case "GET /api/resumen"          -> responder(ex, 200, asistenciaDAO.resumenHoy());
                case "GET /api/empleados"        -> responder(ex, 200, empleadoDAO.listar());
                case "GET /api/empleados/{id}"   -> responder(ex, 200, empleadoDAO.buscarPorId(id(ruta)));
                case "POST /api/empleados"       -> {
                    Empleado e = leer(ex, Empleado.class);
                    responder(ex, 201, Map.of("id", empleadoDAO.insertar(e)));
                }
                case "PUT /api/empleados/{id}"   -> {
                    Empleado e = leer(ex, Empleado.class);
                    e.id = id(ruta);
                    responder(ex, 200, Map.of("actualizado", empleadoDAO.actualizar(e)));
                }
                case "DELETE /api/empleados/{id}" -> responder(ex, 200, Map.of("eliminado", empleadoDAO.desactivar(id(ruta))));
                case "GET /api/dispositivos"     -> responder(ex, 200, dispositivoDAO.listar());
                case "POST /api/dispositivos"    -> responder(ex, 201, Map.of("id", dispositivoDAO.insertar(leer(ex, Dispositivo.class))));
                case "GET /api/marcaciones"      -> responder(ex, 200, marcacionDAO.listar(fecha(q, "desde"), fecha(q, "hasta")));
                case "POST /api/marcaciones"     -> marcacionManual(ex);
                case "GET /api/asistencia"       -> responder(ex, 200, asistenciaDAO.reporte(fecha(q, "desde"), fecha(q, "hasta")));
                case "POST /api/importar"        -> {
                    String serie = q.getOrDefault("serie", "");
                    String archivo = q.getOrDefault("archivo", "sin_nombre");
                    String contenido = cuerpo(ex).replace("\uFEFF", "");
                    boolean esXml = contenido.stripLeading().startsWith("<") || archivo.toLowerCase().endsWith(".xml");
                    int ajuste = 0;
                    try { ajuste = Integer.parseInt(q.getOrDefault("ajusteHoras", "0").trim()); } catch (NumberFormatException ignored) { }
                    Map<String, Object> r = esXml
                            ? new ImportadorDahuaXML().importar(contenido, serie, ajuste)
                            : new ImportadorDahuaCSV().importar(contenido, serie);
                    marcacionDAO.registrarImportacion(archivo, esXml ? "XML" : "CSV", r);
                    responder(ex, 200, r);
                }
                case "POST /api/importar/vista-previa" -> responder(ex, 200, new ImportadorDahuaXML().vistaPrevia(cuerpo(ex)));
                case "GET /api/importaciones"    -> responder(ex, 200, marcacionDAO.historialImportaciones());
                default -> error(ex, 404, "Ruta no encontrada: " + metodo + " " + ruta);
            }
        } catch (IllegalArgumentException | JsonParseException e) {
            error(ex, 400, e.getMessage());
        } catch (Throwable e) {
            e.printStackTrace();
            error(ex, 500, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            ex.close();
        }
    }

    private void login(HttpExchange ex) throws Exception {
        JsonObject b = leer(ex, JsonObject.class);
        Usuario u = usuarioDAO.login(texto(b, "usuario"), texto(b, "password"));
        if (u == null) { error(ex, 401, "Usuario o contraseña incorrectos"); return; }
        String token = UUID.randomUUID().toString();
        sesiones.put(token, u);
        responder(ex, 200, Map.of("token", token, "usuario", u));
    }

    /** Recibe una marcación en tiempo real (middleware/script conectado al Dahua). */
    private void eventoDahua(HttpExchange ex, Map<String, String> q) throws Exception {
        String clave = Optional.ofNullable(ex.getRequestHeaders().getFirst("X-Api-Key")).orElse(q.get("key"));
        if (!Configuracion.get("dahua.apiKey").equals(clave)) { error(ex, 401, "API key inválida"); return; }
        JsonObject b = leer(ex, JsonObject.class);
        LocalDateTime fh = b.has("fechaHora") ? ImportadorDahuaCSV.parsearFecha(texto(b, "fechaHora")) : LocalDateTime.now();
        if (fh == null) throw new IllegalArgumentException("fechaHora inválida");
        String r = marcacionDAO.registrar(texto(b, "codigoBiometrico"), texto(b, "numeroSerie"), fh,
                                          texto(b, "metodo"), "DISPOSITIVO");
        responder(ex, r.startsWith("ERROR") ? 404 : 200, Map.of("resultado", r));
    }

    private void marcacionManual(HttpExchange ex) throws Exception {
        JsonObject b = leer(ex, JsonObject.class);
        LocalDateTime fh = b.has("fechaHora") && !texto(b, "fechaHora").isBlank()
                ? ImportadorDahuaCSV.parsearFecha(texto(b, "fechaHora")) : LocalDateTime.now();
        if (fh == null) throw new IllegalArgumentException("fechaHora inválida (use yyyy-MM-dd HH:mm)");
        String r = marcacionDAO.registrar(texto(b, "codigoBiometrico"), "", fh, texto(b, "metodo"), "MANUAL");
        responder(ex, r.startsWith("ERROR") ? 404 : 200, Map.of("resultado", r));
    }

    // ------------------------------------------------------------------ Archivos web
    private void estatico(HttpExchange ex) throws IOException {
        String ruta = ex.getRequestURI().getPath();
        if (ruta.equals("/")) ruta = "/index.html";
        if (ruta.contains("..")) { error(ex, 400, "Ruta inválida"); return; }
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        try (InputStream in = ServidorApi.class.getResourceAsStream("/web" + ruta)) {
            if (in == null) { error(ex, 404, "No encontrado"); return; }
            byte[] datos = in.readAllBytes();
            ex.getResponseHeaders().set("Content-Type", tipo(ruta));
            ex.sendResponseHeaders(200, datos.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(datos); }
        }
    }

    private static String tipo(String r) {
        if (r.endsWith(".html")) return "text/html; charset=utf-8";
        if (r.endsWith(".css")) return "text/css; charset=utf-8";
        if (r.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (r.endsWith(".png")) return "image/png";
        if (r.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    // ------------------------------------------------------------------ utilidades
    private Usuario usuarioActual(HttpExchange ex) {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        if (h == null || !h.startsWith("Bearer ")) return null;
        return sesiones.get(h.substring(7).trim());
    }

    private static String cuerpo(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }

    private static <T> T leer(HttpExchange ex, Class<T> clase) throws IOException {
        T obj = GSON.fromJson(cuerpo(ex), clase);
        if (obj == null) throw new IllegalArgumentException("Cuerpo JSON vacío");
        return obj;
    }

    private static String texto(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private static int id(String ruta) { return Integer.parseInt(ruta.substring(ruta.lastIndexOf('/') + 1)); }

    private static LocalDate fecha(Map<String, String> q, String k) {
        String v = q.get(k);
        return (v == null || v.isBlank()) ? LocalDate.now() : LocalDate.parse(v);
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> m = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw == null) return m;
        for (String par : raw.split("&")) {
            String[] kv = par.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                  kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    private static void error(HttpExchange ex, int codigo, String msg) throws IOException {
        responder(ex, codigo, Map.of("error", msg == null ? "Error interno" : msg));
    }

    private static void responder(HttpExchange ex, int codigo, Object cuerpo) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Api-Key");
        h.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        h.set("Cache-Control", "no-store");
        if (cuerpo == null) { ex.sendResponseHeaders(codigo, -1); ex.close(); return; }
        byte[] datos = GSON.toJson(cuerpo).getBytes(StandardCharsets.UTF_8);
        h.set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(codigo, datos.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(datos); }
    }
}
