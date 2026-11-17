package com.sbi.dao;

import com.sbi.config.Conexion;
import com.sbi.modelo.Dispositivo;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DispositivoDAO {

    public List<Dispositivo> listar() throws SQLException {
        List<Dispositivo> lista = new ArrayList<>();
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement("SELECT * FROM dispositivos ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Dispositivo d = new Dispositivo();
                d.id = rs.getInt("id");
                d.nombre = rs.getString("nombre");
                d.marca = rs.getString("marca");
                d.modelo = rs.getString("modelo");
                d.numeroSerie = rs.getString("numero_serie");
                d.ip = rs.getString("ip");
                d.puerto = (Integer) rs.getObject("puerto");
                d.ubicacion = rs.getString("ubicacion");
                d.activo = rs.getBoolean("activo");
                d.ultimaSync = rs.getString("ultima_sync");
                lista.add(d);
            }
        }
        return lista;
    }

    public int insertar(Dispositivo d) throws SQLException {
        String sql = "INSERT INTO dispositivos (nombre, marca, modelo, numero_serie, ip, puerto, ubicacion) VALUES (?,?,?,?,?,?,?)";
        try (Connection cn = Conexion.obtener();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.nombre);
            ps.setString(2, d.marca == null ? "Dahua" : d.marca);
            ps.setString(3, d.modelo);
            ps.setString(4, d.numeroSerie);
            ps.setString(5, d.ip);
            ps.setInt(6, d.puerto == null ? 80 : d.puerto);
            ps.setString(7, d.ubicacion);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }
}
