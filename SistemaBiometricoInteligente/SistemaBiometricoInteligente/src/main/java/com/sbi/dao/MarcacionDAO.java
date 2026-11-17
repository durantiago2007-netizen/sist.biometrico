package com.sbi.dao;

import com.sbi.config.Conexion;
import com.sbi.modelo.Marcacion;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarcacionDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Registra una marcación usando el procedimiento sp_registrar_marcacion.
     * @return "OK: ENTRADA", "OK: SALIDA", "DUPLICADA" o "ERROR: ..."
     */
    public String registrar(String codigoBiometrico, String numeroSerie, LocalDateTime fechaHora,
                            String metodo, String origen) throws SQLException {
        try (Connection cn = Conexion.obtener();
             CallableStatement cs = cn.prepareCall("{CALL sp_registrar_marcacion(?,?,?,?,?,?)}")) {
            cs.setString(1, codigoBiometrico);
            cs.setString(2, numeroSerie);
            cs.setTimestamp(3, Timestamp.valueOf(fechaHora));
            cs.setString(4, metodo);
            cs.setString(5, origen);
            cs.registerOutParameter(6, Types.VARCHAR);
            cs.execute();
            return cs.getString(6);
        }
    }

    /**
     * Registra una marcación cuyo tipo (ENTRADA/SALIDA) ya viene del equipo.
     * Si el empleado no existe y hay nombre, lo crea (documento provisional DAHUA-<ID>).
     * Si tipo es null, se calcula: primera marcación del día = ENTRADA.
     * @return "OK: ENTRADA", "OK: SALIDA (NUEVO)", "DUPLICADA" o "ERROR: ..."
     */
    public String registrarConTipo(String codigo, String nombre, String numeroSerie, LocalDateTime fechaHora,
                                   String tipo, String metodo, String origen) throws SQLException {
        try (Connection cn = Conexion.obtener()) {
            boolean nuevo = false;
            Integer empId = buscarEmpleado(cn, codigo);
            if (empId == null) {
                if (nombre == null || nombre.isBlank()) return "ERROR: empleado no encontrado";
                String limpio = nombre.trim().replaceAll("\\s+", " ");
                int sp = limpio.indexOf(' ');
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO empleados (codigo_biometrico, documento, apellidos, nombres) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, codigo);
                    ps.setString(2, "DAHUA-" + codigo);
                    ps.setString(3, sp > 0 ? limpio.substring(0, sp) : limpio);
                    ps.setString(4, sp > 0 ? limpio.substring(sp + 1) : "");
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); empId = rs.getInt(1); }
                }
                nuevo = true;
            }
            try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM marcaciones WHERE empleado_id=? AND fecha_hora=?")) {
                ps.setInt(1, empId); ps.setTimestamp(2, Timestamp.valueOf(fechaHora));
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return "DUPLICADA"; }
            }
            if (tipo == null) {
                try (PreparedStatement ps = cn.prepareStatement(
                        "SELECT COUNT(*) FROM marcaciones WHERE empleado_id=? AND DATE(fecha_hora)=DATE(?) AND fecha_hora<?")) {
                    ps.setInt(1, empId); ps.setTimestamp(2, Timestamp.valueOf(fechaHora)); ps.setTimestamp(3, Timestamp.valueOf(fechaHora));
                    try (ResultSet rs = ps.executeQuery()) { rs.next(); tipo = rs.getInt(1) == 0 ? "ENTRADA" : "SALIDA"; }
                }
            }
            Integer dispId = null;
            if (numeroSerie != null && !numeroSerie.isBlank()) {
                try (PreparedStatement ps = cn.prepareStatement("SELECT id FROM dispositivos WHERE numero_serie=?")) {
                    ps.setString(1, numeroSerie);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) dispId = rs.getInt(1); }
                }
            }
            try (PreparedStatement ps = cn.prepareStatement(
                    "INSERT INTO marcaciones (empleado_id, dispositivo_id, fecha_hora, tipo, metodo, origen) VALUES (?,?,?,?,?,?)")) {
                ps.setInt(1, empId);
                if (dispId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, dispId);
                ps.setTimestamp(3, Timestamp.valueOf(fechaHora));
                ps.setString(4, tipo);
                ps.setString(5, metodo == null ? "OTRO" : metodo);
                ps.setString(6, origen);
                ps.executeUpdate();
            }
            if (dispId != null) {
                try (PreparedStatement ps = cn.prepareStatement("UPDATE dispositivos SET ultima_sync=NOW() WHERE id=?")) {
                    ps.setInt(1, dispId); ps.executeUpdate();
                }
            }
            return "OK: " + tipo + (nuevo ? " (NUEVO)" : "");
        }
    }

    /** Crea o actualiza un horario por nombre (usado al importar Attendance_Shift.xml). */
    public void guardarHorario(String nombre, String entrada, String salida, int tolerancia) throws SQLException {
        String sql = "INSERT INTO horarios (nombre, hora_entrada, hora_salida, tolerancia_min) VALUES (?,?,?,?) " +
                     "ON DUPLICATE KEY UPDATE hora_entrada=VALUES(hora_entrada), hora_salida=VALUES(hora_salida), tolerancia_min=VALUES(tolerancia_min)";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre.length() > 60 ? nombre.substring(0, 60) : nombre);
            ps.setString(2, entrada); ps.setString(3, salida); ps.setInt(4, tolerancia);
            ps.executeUpdate();
        }
    }

    private Integer buscarEmpleado(Connection cn, String codigo) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement("SELECT id FROM empleados WHERE codigo_biometrico=?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
        }
    }

    /** Guarda el historial de cada archivo importado (tabla importaciones). */
    public void registrarImportacion(String archivo, String formato, Map<String, Object> r) {
        String sql = "INSERT INTO importaciones (archivo, formato, registros, importadas, duplicadas, errores) VALUES (?,?,?,?,?,?)";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, archivo);
            ps.setString(2, formato);
            int imp = num(r.get("importadas")), dup = num(r.get("duplicadas")), err = num(r.get("errores"));
            ps.setInt(3, r.containsKey("registrosEnArchivo") ? num(r.get("registrosEnArchivo")) : imp + dup + err);
            ps.setInt(4, imp); ps.setInt(5, dup); ps.setInt(6, err);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AVISO] No se guardó el historial de importación: " + e.getMessage());
        }
    }

    private static int num(Object o) { return o instanceof Number n ? n.intValue() : 0; }

    public List<Map<String, Object>> historialImportaciones() throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement("SELECT * FROM importaciones ORDER BY id DESC LIMIT 50");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("fecha", rs.getString("fecha"));
                m.put("archivo", rs.getString("archivo"));
                m.put("formato", rs.getString("formato"));
                m.put("registros", rs.getInt("registros"));
                m.put("importadas", rs.getInt("importadas"));
                m.put("duplicadas", rs.getInt("duplicadas"));
                m.put("errores", rs.getInt("errores"));
                l.add(m);
            }
        }
        return l;
    }

    public List<Marcacion> listar(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT m.*, e.codigo_biometrico, CONCAT(e.apellidos, ', ', e.nombres) AS empleado, d.nombre AS dispositivo " +
                     "FROM marcaciones m JOIN empleados e ON e.id = m.empleado_id " +
                     "LEFT JOIN dispositivos d ON d.id = m.dispositivo_id " +
                     "WHERE DATE(m.fecha_hora) BETWEEN ? AND ? ORDER BY m.fecha_hora DESC LIMIT 2000";
        List<Marcacion> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Marcacion m = new Marcacion();
                    m.id = rs.getLong("id");
                    m.empleadoId = rs.getInt("empleado_id");
                    m.codigoBiometrico = rs.getString("codigo_biometrico");
                    m.empleado = rs.getString("empleado");
                    m.dispositivo = rs.getString("dispositivo");
                    m.fechaHora = rs.getTimestamp("fecha_hora").toLocalDateTime().format(FMT);
                    m.tipo = rs.getString("tipo");
                    m.metodo = rs.getString("metodo");
                    m.origen = rs.getString("origen");
                    lista.add(m);
                }
            }
        }
        return lista;
    }
}
