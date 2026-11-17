package com.sbi.dao;

import com.sbi.config.Conexion;
import com.sbi.modelo.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.HexFormat;

public class UsuarioDAO {

    /** Devuelve el usuario si usuario/contraseña son correctos; si no, null. */
    public Usuario login(String usuario, String password) throws SQLException {
        String sql = "SELECT id, usuario, nombre, rol FROM usuarios WHERE usuario = ? AND password_hash = ? AND activo = 1";
        try (Connection cn = Conexion.obtener(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Usuario u = new Usuario();
                u.id = rs.getInt("id");
                u.usuario = rs.getString("usuario");
                u.nombre = rs.getString("nombre");
                u.rol = rs.getString("rol");
                return u;
            }
        }
    }

    public static String sha256(String texto) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
