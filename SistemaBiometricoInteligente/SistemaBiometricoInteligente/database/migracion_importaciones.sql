-- Agrega el historial de archivos importados (XML/CSV del pendrive).
-- Ejecutar UNA vez sobre la base existente (no borra datos).
USE sistema_biometrico_inteligente;

CREATE TABLE IF NOT EXISTS importaciones (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  fecha       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  archivo     VARCHAR(255) NOT NULL,
  formato     ENUM('XML','CSV') NOT NULL,
  registros   INT NOT NULL DEFAULT 0 COMMENT 'Registros encontrados en el archivo',
  importadas  INT NOT NULL DEFAULT 0 COMMENT 'Marcaciones nuevas guardadas',
  duplicadas  INT NOT NULL DEFAULT 0 COMMENT 'Ya existían (no se repiten)',
  errores     INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

SELECT 'Tabla importaciones lista' AS resultado;
