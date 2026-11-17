-- =====================================================================
--  BASE DE DATOS: sistema_biometrico_inteligente
--  Proyecto: Asistenciero Virtual (registro de asistencia con Dahua)
--  Motor: MySQL 8.x  (ejecutar desde HeidiSQL: Archivo > Cargar archivo SQL > F9)
-- =====================================================================

DROP DATABASE IF EXISTS sistema_biometrico_inteligente;
CREATE DATABASE sistema_biometrico_inteligente
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE sistema_biometrico_inteligente;

-- ---------------------------------------------------------------------
-- 1. ÁREAS (departamentos de la institución)
-- ---------------------------------------------------------------------
CREATE TABLE areas (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  nombre       VARCHAR(100) NOT NULL UNIQUE,
  descripcion  VARCHAR(255) NULL,
  activo       TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. HORARIOS (turnos de trabajo)
-- ---------------------------------------------------------------------
CREATE TABLE horarios (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(60)  NOT NULL UNIQUE,
  hora_entrada    TIME NOT NULL,
  hora_salida     TIME NOT NULL,
  tolerancia_min  INT  NOT NULL DEFAULT 10 COMMENT 'Minutos de tolerancia antes de marcar tardanza',
  activo          TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. EMPLEADOS / ESTUDIANTES registrados en el biométrico
--    codigo_biometrico = "User ID" que se asigna en el equipo Dahua
-- ---------------------------------------------------------------------
CREATE TABLE empleados (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  codigo_biometrico  VARCHAR(30)  NOT NULL UNIQUE,
  documento          VARCHAR(20)  NOT NULL UNIQUE,
  nombres            VARCHAR(100) NOT NULL,
  apellidos          VARCHAR(100) NOT NULL,
  email              VARCHAR(120) NULL,
  telefono           VARCHAR(20)  NULL,
  area_id            INT NULL,
  horario_id         INT NULL,
  activo             TINYINT(1) NOT NULL DEFAULT 1,
  creado_en          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_emp_area    FOREIGN KEY (area_id)    REFERENCES areas(id)    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT fk_emp_horario FOREIGN KEY (horario_id) REFERENCES horarios(id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. DISPOSITIVOS biométricos (Dahua)
-- ---------------------------------------------------------------------
CREATE TABLE dispositivos (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  nombre         VARCHAR(80)  NOT NULL,
  marca          VARCHAR(40)  NOT NULL DEFAULT 'Dahua',
  modelo         VARCHAR(60)  NULL,
  numero_serie   VARCHAR(60)  NOT NULL UNIQUE,
  ip             VARCHAR(45)  NULL,
  puerto         INT          NULL DEFAULT 80,
  ubicacion      VARCHAR(120) NULL,
  activo         TINYINT(1)   NOT NULL DEFAULT 1,
  ultima_sync    DATETIME     NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. MARCACIONES (cada registro que genera el biométrico)
-- ---------------------------------------------------------------------
CREATE TABLE marcaciones (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  empleado_id     INT NOT NULL,
  dispositivo_id  INT NULL,
  fecha_hora      DATETIME NOT NULL,
  tipo            ENUM('ENTRADA','SALIDA') NOT NULL,
  metodo          ENUM('HUELLA','ROSTRO','TARJETA','PASSWORD','OTRO') NOT NULL DEFAULT 'HUELLA',
  origen          ENUM('DISPOSITIVO','IMPORTACION','MANUAL') NOT NULL DEFAULT 'DISPOSITIVO',
  registrado_en   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_mar_empleado    FOREIGN KEY (empleado_id)    REFERENCES empleados(id)    ON DELETE CASCADE,
  CONSTRAINT fk_mar_dispositivo FOREIGN KEY (dispositivo_id) REFERENCES dispositivos(id) ON DELETE SET NULL,
  CONSTRAINT uq_marcacion UNIQUE (empleado_id, fecha_hora),   -- evita duplicados al importar 2 veces
  INDEX idx_mar_fecha (fecha_hora)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. JUSTIFICACIONES (faltas / tardanzas justificadas)
-- ---------------------------------------------------------------------
CREATE TABLE justificaciones (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  empleado_id  INT NOT NULL,
  fecha        DATE NOT NULL,
  motivo       VARCHAR(255) NOT NULL,
  estado       ENUM('PENDIENTE','APROBADA','RECHAZADA') NOT NULL DEFAULT 'PENDIENTE',
  creado_en    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_jus_empleado FOREIGN KEY (empleado_id) REFERENCES empleados(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. USUARIOS del sistema web (login)
--    password_hash = SHA-256 en hexadecimal
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  usuario        VARCHAR(50)  NOT NULL UNIQUE,
  password_hash  CHAR(64)     NOT NULL,
  nombre         VARCHAR(100) NOT NULL,
  rol            ENUM('ADMIN','SUPERVISOR') NOT NULL DEFAULT 'SUPERVISOR',
  activo         TINYINT(1) NOT NULL DEFAULT 1,
  creado_en      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. IMPORTACIONES (historial de archivos XML/CSV traídos por pendrive)
-- ---------------------------------------------------------------------
CREATE TABLE importaciones (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  archivo     VARCHAR(255) NOT NULL,
  formato     ENUM('XML','CSV') NOT NULL,
  registros   INT NOT NULL DEFAULT 0,
  importadas  INT NOT NULL DEFAULT 0,
  duplicadas  INT NOT NULL DEFAULT 0,
  errores     INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

-- =====================================================================
--  VISTA: asistencia diaria (primera entrada y última salida por día)
-- =====================================================================
CREATE OR REPLACE VIEW vista_asistencia_diaria AS
SELECT
  e.id                                   AS empleado_id,
  e.codigo_biometrico,
  CONCAT(e.apellidos, ', ', e.nombres)   AS empleado,
  a.nombre                               AS area,
  DATE(m.fecha_hora)                     AS fecha,
  MIN(CASE WHEN m.tipo = 'ENTRADA' THEN TIME(m.fecha_hora) END) AS hora_entrada,
  MAX(CASE WHEN m.tipo = 'SALIDA'  THEN TIME(m.fecha_hora) END) AS hora_salida,
  h.hora_entrada                         AS horario_entrada,
  CASE
    WHEN h.id IS NULL THEN 'SIN HORARIO'
    WHEN MIN(CASE WHEN m.tipo = 'ENTRADA' THEN TIME(m.fecha_hora) END)
         <= ADDTIME(h.hora_entrada, SEC_TO_TIME(h.tolerancia_min * 60)) THEN 'PUNTUAL'
    ELSE 'TARDANZA'
  END                                    AS estado
FROM marcaciones m
JOIN empleados e      ON e.id = m.empleado_id
LEFT JOIN areas a     ON a.id = e.area_id
LEFT JOIN horarios h  ON h.id = e.horario_id
GROUP BY e.id, e.codigo_biometrico, e.apellidos, e.nombres, a.nombre,
         DATE(m.fecha_hora), h.id, h.hora_entrada, h.tolerancia_min;

-- =====================================================================
--  PROCEDIMIENTO: registrar una marcación que llega del Dahua
--  - Busca al empleado por su código biométrico
--  - Si es su primera marcación del día => ENTRADA, si no => SALIDA
--  - Ignora duplicados exactos
-- =====================================================================
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_registrar_marcacion $$
CREATE PROCEDURE sp_registrar_marcacion(
  IN  p_codigo_biometrico VARCHAR(30),
  IN  p_numero_serie      VARCHAR(60),
  IN  p_fecha_hora        DATETIME,
  IN  p_metodo            VARCHAR(20),
  IN  p_origen            VARCHAR(20),
  OUT p_resultado         VARCHAR(100)
)
BEGIN
  DECLARE v_empleado_id    INT DEFAULT NULL;
  DECLARE v_dispositivo_id INT DEFAULT NULL;
  DECLARE v_previas        INT DEFAULT 0;
  DECLARE v_tipo           VARCHAR(10);

  SELECT id INTO v_empleado_id
    FROM empleados
   WHERE codigo_biometrico = p_codigo_biometrico AND activo = 1
   LIMIT 1;

  IF v_empleado_id IS NULL THEN
    SET p_resultado = 'ERROR: empleado no encontrado';
  ELSEIF EXISTS (SELECT 1 FROM marcaciones
                  WHERE empleado_id = v_empleado_id AND fecha_hora = p_fecha_hora) THEN
    SET p_resultado = 'DUPLICADA';
  ELSE
    SELECT id INTO v_dispositivo_id
      FROM dispositivos WHERE numero_serie = p_numero_serie LIMIT 1;

    SELECT COUNT(*) INTO v_previas
      FROM marcaciones
     WHERE empleado_id = v_empleado_id
       AND DATE(fecha_hora) = DATE(p_fecha_hora)
       AND fecha_hora < p_fecha_hora;

    SET v_tipo = IF(v_previas = 0, 'ENTRADA', 'SALIDA');

    INSERT INTO marcaciones (empleado_id, dispositivo_id, fecha_hora, tipo, metodo, origen)
    VALUES (v_empleado_id, v_dispositivo_id, p_fecha_hora, v_tipo,
            IFNULL(NULLIF(p_metodo, ''), 'HUELLA'),
            IFNULL(NULLIF(p_origen, ''), 'DISPOSITIVO'));

    IF v_dispositivo_id IS NOT NULL THEN
      UPDATE dispositivos SET ultima_sync = NOW() WHERE id = v_dispositivo_id;
    END IF;

    SET p_resultado = CONCAT('OK: ', v_tipo);
  END IF;
END $$

-- Reporte de asistencia por rango de fechas
DROP PROCEDURE IF EXISTS sp_reporte_asistencia $$
CREATE PROCEDURE sp_reporte_asistencia(IN p_desde DATE, IN p_hasta DATE)
BEGIN
  SELECT * FROM vista_asistencia_diaria
   WHERE fecha BETWEEN p_desde AND p_hasta
   ORDER BY fecha DESC, empleado;
END $$

DELIMITER ;

-- =====================================================================
--  DATOS DE PRUEBA
-- =====================================================================
INSERT INTO areas (nombre, descripcion) VALUES
  ('Administración', 'Personal administrativo'),
  ('Sistemas',       'Área de TI'),
  ('Docentes',       'Plana docente');

INSERT INTO horarios (nombre, hora_entrada, hora_salida, tolerancia_min) VALUES
  ('Mañana', '08:00:00', '13:00:00', 10),
  ('Tarde',  '14:00:00', '19:00:00', 10),
  ('Completo','08:00:00', '17:00:00', 15);

INSERT INTO empleados (codigo_biometrico, documento, nombres, apellidos, email, area_id, horario_id) VALUES
  ('1001', '70000001', 'Ana',    'Quispe Mamani', 'ana@correo.com',    1, 3),
  ('1002', '70000002', 'Luis',   'Flores Rojas',  'luis@correo.com',   2, 1),
  ('1003', '70000003', 'María',  'Condori Huanca','maria@correo.com',  3, 2);

INSERT INTO dispositivos (nombre, modelo, numero_serie, ip, puerto, ubicacion) VALUES
  ('Biométrico Puerta Principal', 'ASI1212F', 'DAHUA-SN-0001', '192.168.1.108', 80, 'Ingreso principal');

-- Usuario web por defecto: admin / admin123   (¡cámbialo después!)
INSERT INTO usuarios (usuario, password_hash, nombre, rol) VALUES
  ('admin', SHA2('admin123', 256), 'Administrador', 'ADMIN');

-- Marcaciones de ejemplo usando el procedimiento
CALL sp_registrar_marcacion('1001', 'DAHUA-SN-0001', CONCAT(CURDATE(), ' 07:55:00'), 'HUELLA', 'DISPOSITIVO', @r);
CALL sp_registrar_marcacion('1001', 'DAHUA-SN-0001', CONCAT(CURDATE(), ' 17:05:00'), 'HUELLA', 'DISPOSITIVO', @r);
CALL sp_registrar_marcacion('1002', 'DAHUA-SN-0001', CONCAT(CURDATE(), ' 08:25:00'), 'ROSTRO', 'DISPOSITIVO', @r);
CALL sp_registrar_marcacion('1003', 'DAHUA-SN-0001', CONCAT(CURDATE(), ' 13:58:00'), 'TARJETA','DISPOSITIVO', @r);

-- Comprobación rápida
SELECT * FROM vista_asistencia_diaria;
