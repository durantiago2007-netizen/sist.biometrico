# Sistema Biométrico Inteligente (Asistenciero virtual)

Contexto para Claude Code. Responde y comenta el código en español.

## Stack
- Java 17+, proyecto **Maven** (se abre directo en Apache NetBeans: File > Open Project).
- MySQL 8, base `sistema_biometrico_inteligente`, administrada con HeidiSQL.
- JDBC puro (`mysql-connector-j`) + Gson. Servidor HTTP del JDK (`com.sun.net.httpserver`), sin Tomcat ni Spring.
- Frontend estático en `src/main/resources/web` (HTML + JS sin frameworks).
- Biométrico: Dahua. El `codigo_biometrico` del empleado = "User ID" registrado en el equipo.

## Estructura
- `database/sistema_biometrico_inteligente.sql` — script completo (tablas, vista, procedimientos, datos de prueba). Si cambias la BD, actualiza este script.
- `com.sbi.config` — `Configuracion` (lee `db.properties`) y `Conexion`.
- `com.sbi.modelo` — POJOs con campos públicos (Gson los serializa).
- `com.sbi.dao` — un DAO por tabla; siempre `PreparedStatement`/`CallableStatement` y try-with-resources.
- `com.sbi.servicio.ImportadorDahuaCSV` — importa el CSV exportado del Dahua.
- `com.sbi.api.ServidorApi` — rutas REST en el `switch` del método `api()`.

## Reglas
- Toda marcación se inserta mediante `sp_registrar_marcacion` (decide ENTRADA/SALIDA y evita duplicados).
- Nunca concatenar SQL con datos del usuario.
- Contraseñas: SHA-256 hex (`UsuarioDAO.sha256`).

## Comandos
- Compilar: `mvn clean package` → `target/SistemaBiometricoInteligente-1.0.jar`
- Ejecutar: `java -jar target/SistemaBiometricoInteligente-1.0.jar` → http://localhost:8080 (admin / admin123)

## Importación XML (pendrive)
- El Dahua exporta hojas Excel 2003 en XML (SpreadsheetML). Ver `DOCUMENTACION_XML.md`.
- `ImportadorDahuaXML` detecta: Card_Punch_Records (marcaciones), Attendance_Shift (horarios); los reportes se ignoran.
- El reloj del equipo está +11 h: la web envía `ajusteHoras=-11`.
- Historial en tabla `importaciones` (`database/migracion_importaciones.sql` para bases ya creadas).
