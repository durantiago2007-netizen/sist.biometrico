package com.sbi.dao;

import com.sbi.config.Conexion;
import com.sbi.modelo.AsistenciaDiaria;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AsistenciaDAO {

    public List<AsistenciaDiaria> reporte(LocalDate desde, LocalDate hasta) throws SQLException {
        List<AsistenciaDiaria> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener();
             CallableStatement cs = cn.prepareCall("{CALL sp_reporte_asistencia(?,?)}")) {
            cs.setDate(1, Date.valueOf(desde));
            cs.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    AsistenciaDiaria a = new AsistenciaDiaria();
                    a.empleadoId = rs.getInt("empleado_id");
                    a.codigoBiometrico = rs.getString("codigo_biometrico");
                    a.empleado = rs.getString("empleado");
                    a.area = rs.getString("area");
                    a.fecha = rs.getString("fecha");
                    a.horaEntrada = rs.getString("hora_entrada");
                    a.horaSalida = rs.getString("hora_salida");
                    a.horarioEntrada = rs.getString("horario_entrada");
                    a.estado = rs.getString("estado");
                    lista.add(a);
                }
            }
        }
        return lista;
    }

    /** Totales del día para el panel principal. */
    public Map<String, Integer> resumenHoy() throws SQLException {
        Map<String, Integer> r = new LinkedHashMap<>();
        try (Connection cn = Conexion.obtener(); Statement st = cn.createStatement()) {
            r.put("empleados", contar(st, "SELECT COUNT(*) FROM empleados WHERE activo = 1"));
            r.put("presentes", contar(st, "SELECT COUNT(*) FROM vista_asistencia_diaria WHERE fecha = CURDATE()"));
            r.put("tardanzas", contar(st, "SELECT COUNT(*) FROM vista_asistencia_diaria WHERE fecha = CURDATE() AND estado = 'TARDANZA'"));
            r.put("marcaciones", contar(st, "SELECT COUNT(*) FROM marcaciones WHERE DATE(fecha_hora) = CURDATE()"));
        }
        r.put("ausentes", Math.max(0, r.get("empleados") - r.get("presentes")));
        return r;
    }

    private int contar(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) { return rs.next() ? rs.getInt(1) : 0; }
    }
}
