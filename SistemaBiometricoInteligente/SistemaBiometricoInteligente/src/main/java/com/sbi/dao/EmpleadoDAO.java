package com.sbi.dao;

import com.sbi.config.Conexion;
import com.sbi.modelo.Empleado;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {

    private static final String SELECT_BASE =
        "SELECT e.*, a.nombre AS area, h.nombre AS horario FROM empleados e " +
        "LEFT JOIN areas a ON a.id = e.area_id LEFT JOIN horarios h ON h.id = e.horario_id ";

    public List<Empleado> listar() throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + "WHERE e.activo = 1 ORDER BY e.apellidos, e.nombres");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Empleado buscarPorId(int id) throws SQLException {
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + "WHERE e.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapear(rs) : null; }
        }
    }

    public int insertar(Empleado e) throws SQLException {
        String sql = "INSERT INTO empleados (codigo_biometrico, documento, nombres, apellidos, email, telefono, area_id, horario_id) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenar(ps, e);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public boolean actualizar(Empleado e) throws SQLException {
        String sql = "UPDATE empleados SET codigo_biometrico=?, documento=?, nombres=?, apellidos=?, email=?, telefono=?, " +
                     "area_id=?, horario_id=? WHERE id=?";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            llenar(ps, e);
            ps.setInt(9, e.id);
            return ps.executeUpdate() > 0;
        }
    }

    /** Baja lógica: no se borra para conservar el historial de marcaciones. */
    public boolean desactivar(int id) throws SQLException {
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement("UPDATE empleados SET activo = 0 WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private void llenar(PreparedStatement ps, Empleado e) throws SQLException {
        ps.setString(1, e.codigoBiometrico);
        ps.setString(2, e.documento);
        ps.setString(3, e.nombres);
        ps.setString(4, e.apellidos);
        ps.setString(5, e.email);
        ps.setString(6, e.telefono);
        if (e.areaId == null) ps.setNull(7, Types.INTEGER); else ps.setInt(7, e.areaId);
        if (e.horarioId == null) ps.setNull(8, Types.INTEGER); else ps.setInt(8, e.horarioId);
    }

    private Empleado mapear(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.id = rs.getInt("id");
        e.codigoBiometrico = rs.getString("codigo_biometrico");
        e.documento = rs.getString("documento");
        e.nombres = rs.getString("nombres");
        e.apellidos = rs.getString("apellidos");
        e.email = rs.getString("email");
        e.telefono = rs.getString("telefono");
        e.areaId = (Integer) rs.getObject("area_id");
        e.area = rs.getString("area");
        e.horarioId = (Integer) rs.getObject("horario_id");
        e.horario = rs.getString("horario");
        e.activo = rs.getBoolean("activo");
        return e;
    }
}
