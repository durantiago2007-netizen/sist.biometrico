-- Carga de datos reales del biométrico Dahua DHI-ASA3223A-W (SN AE06085PAJ13AAD)
USE sistema_biometrico_inteligente;
SET SQL_SAFE_UPDATES = 0;
-- 1) Quitar datos de ejemplo
DELETE FROM marcaciones WHERE empleado_id IN (SELECT id FROM (SELECT id FROM empleados WHERE codigo_biometrico IN ('1001','1002','1003')) x);
DELETE FROM empleados WHERE codigo_biometrico IN ('1001','1002','1003');
DELETE FROM dispositivos WHERE numero_serie = 'DAHUA-SN-0001';
-- 2) Dispositivo real
INSERT INTO dispositivos (nombre, marca, modelo, numero_serie, ip, puerto, ubicacion) VALUES ('Biométrico principal','Dahua','DHI-ASA3223A-W','AE06085PAJ13AAD','192.168.133.90',37777,'Ingreso') ON DUPLICATE KEY UPDATE ip=VALUES(ip), modelo=VALUES(modelo);
-- 3) Personas registradas en el equipo (documento provisional = DAHUA-<ID>, complétalo después)
INSERT INTO empleados (codigo_biometrico, documento, nombres, apellidos) VALUES
('1','DAHUA-1','cuevas','santiago'),
('2','DAHUA-2','Emir Lazaro','Alfaro'),
('3','DAHUA-3','Valentina Eluney Del Huerto','Baez'),
('4','DAHUA-4','Mario Asael Dionel','Baleriano'),
('5','DAHUA-5','Nayra De Los Angeles','Calisaya'),
('6','DAHUA-6','Santiago Josias','Castillo'),
('7','DAHUA-7','NatalI Solange','Cruz'),
('8','DAHUA-8','Joaquin Josue','Erazo'),
('9','DAHUA-9','Nahuel Elias','Esteban'),
('10','DAHUA-10','Valentina Dana','Fernandez'),
('11','DAHUA-11','Thiago Lionel','Flores'),
('12','DAHUA-12','Estrella Mailen','Fuertes'),
('13','DAHUA-13','Manuel Esteban','Gemetro'),
('14','DAHUA-14','Gissel Karen Celina','GUANCO'),
('15','DAHUA-15','Alvaro Nicolas','Gutierrez'),
('16','DAHUA-16','Jeremias Nahuel','Guerrero'),
('17','DAHUA-17','Jimena Jazmin','Lamas'),
('18','DAHUA-18','Santos Solange','Mamani'),
('19','DAHUA-19','Nahuel Ezequiel Rodrigo','Matinez'),
('20','DAHUA-20','Tatiana Sarai','Mendoza'),
('21','DAHUA-21','Natali Ana','Portal'),
('22','DAHUA-22','Eliseo Fabian','Quispe'),
('23','DAHUA-23','Alessandro Fabian','Rodriguez'),
('24','DAHUA-24','Ivan Mauro','Sanchez'),
('25','DAHUA-25','Lujan Pilar','Tejerina'),
('26','DAHUA-26','Dylan Natanael','Toconas'),
('27','DAHUA-27','Rafael Maximiliano Franco','Tolaba'),
('28','DAHUA-28','Matias Axel','Torres'),
('29','DAHUA-29','Tolaba Maile Nazeli','Uno'),
('30','DAHUA-30','Brian Dante Camilo','Valle'),
('31','DAHUA-31','Belinda Yexalen','Vilca'),
('32','DAHUA-32','Gisel Antonella','Vilca'),
('33','DAHUA-33','Paula Mailen','Vilte'),
('34','DAHUA-34','Alexander Eduardo','ALFARO'),
('35','DAHUA-35','Melani De la Salette','Asensio'),
('36','DAHUA-36','Julieta Lujan','AUCAPINA'),
('37','DAHUA-37','Rocio Aylin','CALISAYA'),
('38','DAHUA-38','GUERRA Benedicto','CAMACHO'),
('39','DAHUA-39','Micaela Mailen','CHAVARRIA'),
('40','DAHUA-40','RAMOS Francisco Tomas','CHAVEZ'),
('41','DAHUA-41','Edith Mariana','COLQUE'),
('42','DAHUA-42','MALDONADO Arian Gael','FLORES'),
('43','DAHUA-43','Alexis Benjamin','FLORES'),
('44','DAHUA-44','MORALES Maximo Gabriel','GALINDO'),
('45','DAHUA-45','Mailen Analia','GERONIMO'),
('46','DAHUA-46','QUISPE Angela Victoria','GOMEZ'),
('47','DAHUA-47','Jonas Alberto','MAIZARES'),
('48','DAHUA-48','Vanina Abril','MOGRO'),
('49','DAHUA-49','Alexander Gabriel','RAMOS'),
('50','DAHUA-50','Gustavo Ezequiel','RIVERO'),
('51','DAHUA-51','Jocelyn Jimena','LICANTICA'),
('52','DAHUA-52','Fernanda Araceli','NOLASCO'),
('53','DAHUA-53','Maiten','SAIQUITA'),
('54','DAHUA-54','Santiago','VASQUEZ'),
('55','DAHUA-55','Johanna Luz','CASIMIRO'),
('56','DAHUA-56','Santiago Alejandro','CRUZ'),
('57','DAHUA-57','Violeta Fiorella','QUISPE'),
('58','DAHUA-58','Burgos','Emiliano'),
('59','DAHUA-59','Caceres Leonel Fernando','Chana'),
('60','DAHUA-60','Ortega Jemima Consuelo','Diaz'),
('61','DAHUA-61','Pardo Candela','Dominguez'),
('62','DAHUA-62','Tiago Miguel Ezequiel','Duran'),
('63','DAHUA-63','Gianella Eyen','Flores'),
('64','DAHUA-64','Efrain Farid','Gaspar'),
('65','DAHUA-65','Emilia Victoria','Herrera'),
('66','DAHUA-66','Fabian Josue Joel','Patagua'),
('67','DAHUA-67','Tatiana de los Angeles','Tolaba'),
('68','DAHUA-68','Berenice Laura Camila','Trujillo'),
('69','DAHUA-69','Josias Uriel Reinaldo','Vargas'),
('70','DAHUA-70','Caterina Claribel','Velasquez'),
('71','DAHUA-71','Esteban Reinaldo','Villca'),
('72','DAHUA-72','Agustin Gabriel','Alarcon'),
('73','DAHUA-73','Jackeline Eve','Apaza'),
('74','DAHUA-74','Romina Estefania','Aucapina'),
('75','DAHUA-75','Liana Candela','Barcaya'),
('76','DAHUA-76','Gabriela Gissel','Calisaya'),
('77','DAHUA-77','Esteban Abel','Copa'),
('78','DAHUA-78','Araceli Daiana','Gaspar'),
('79','DAHUA-79','Andrea Abril','Geronimo'),
('80','DAHUA-80','Yuliana Aymara Joselyn','Guitian'),
('81','DAHUA-81','Zahira Neyen','Gutierrez'),
('82','DAHUA-82','Chalco Benjamin','Huarachi'),
('83','DAHUA-83','Gianella Zulema','Mamani'),
('84','DAHUA-84','Zahira Nicole','Mamani'),
('85','DAHUA-85','Brisa de los Angeles','Subelza'),
('86','DAHUA-86','Angelo Henry','Vidaure'),
('87','DAHUA-87','Alexis Ivan','Vilca'),
('88','DAHUA-88','Diaz Candela Lujan','Zapana'),
('89','DAHUA-89','','david'),
('90','DAHUA-90','Rosario','Angelo'),
('91','DAHUA-91','Emanuel','Aucapina'),
('92','DAHUA-92','Ingrid','Benitez'),
('93','DAHUA-93','Mayra','Cala'),
('94','DAHUA-94','Natalia','Calisaya'),
('95','DAHUA-95','Joel','Cari'),
('96','DAHUA-96','Gustavo','Carlos'),
('97','DAHUA-97','Natanael','Condori'),
('98','DAHUA-98','Yael','Coria'),
('99','DAHUA-99','Tiago','Farfan'),
('100','DAHUA-100','Ariana','Gaspar'),
('101','DAHUA-101','Fabricio Lucas','Gasar'),
('102','DAHUA-102','Eyen','Gaspar'),
('103','DAHUA-103','Josefina','Gutierres'),
('104','DAHUA-104','Oliver','Lazaro'),
('105','DAHUA-105','Luciana','Liquin'),
('106','DAHUA-106','Keila','Lopez'),
('107','DAHUA-107','Paola Carolina','Marcos'),
('108','DAHUA-108','Lautaro','Mendivil'),
('109','DAHUA-109','Lisandro','Mendivil'),
('110','DAHUA-110','Julieta','Morales'),
('111','DAHUA-111','Agustin','Parraga'),
('112','DAHUA-112','Fabian','Portal'),
('113','DAHUA-113','Agustina','Quispe'),
('114','DAHUA-114','Santino','Sosa'),
('115','DAHUA-115','Mateo','Valdiviezo'),
('116','DAHUA-116','Noelia','Valle'),
('117','DAHUA-117','Luis','Vilca'),
('118','DAHUA-118','Sebastian','Velazque'),
('119','DAHUA-119','Gustavo','ABALOS'),
('120','DAHUA-120','CLAVIJO Eunice','ACUNA'),
('121','DAHUA-121','Guillermo','ALFARO'),
('122','DAHUA-122','Adrian','ARAMAYO'),
('123','DAHUA-123','Ariadna','BARBOSA'),
('124','DAHUA-124','Annette','BERDON'),
('125','DAHUA-125','Diego','CACHAMBI'),
('126','DAHUA-126','Briana','CALISAYA'),
('127','DAHUA-127','Josue','CARRILLO'),
('128','DAHUA-128','Abigail','CONDORI'),
('129','DAHUA-129','Sabrina','CRUZ'),
('130','DAHUA-130','Camila','ESTEBAN'),
('131','DAHUA-131','ACEVEDO Avril','FERNANDEZ'),
('132','DAHUA-132','Maria','FERREIRA'),
('133','DAHUA-133','Avril','FLORES'),
('134','DAHUA-134','Jorfe','GREGORIO'),
('135','DAHUA-135','MOLINEDO Santiago','LUDUENA'),
('136','DAHUA-136','Candela','MAMANI'),
('137','DAHUA-137','VEGA Uma','MENDEZ'),
('138','DAHUA-138','Naim','MENDOZA'),
('139','DAHUA-139','FERNANDEZ Maria','MONTECINOS'),
('140','DAHUA-140','LEON Agustina','NINA'),
('141','DAHUA-141','Alejandrina','OCHOA'),
('142','DAHUA-142','Jonatan','QUISPE'),
('143','DAHUA-143','Jazmin','SEGOVIA'),
('144','DAHUA-144','Franco','SITARSKI'),
('145','DAHUA-145','Milagros','SUBELZA'),
('146','DAHUA-146','Lautaro','TABARES'),
('147','DAHUA-147','Cesar','TITO'),
('148','DAHUA-148','Victoria','TOLABA'),
('149','DAHUA-149','Dalila','VEDIA'),
('150','DAHUA-150','MIRSE Maite','VENTURA'),
('151','DAHUA-151','Raul','VENTURA'),
('152','DAHUA-152','Francisco Jorge','CARLOS'),
('153','DAHUA-153','Rolando David','Calisaya'),
('154','DAHUA-154','Silvia Susana','Lopez'),
('155','DAHUA-155','Solange Vanina','Rodriguez'),
('156','DAHUA-156','Daniela Patricia','Chiri'),
('157','DAHUA-157','Julieta Veronica','Ursagaste'),
('158','DAHUA-158','Ana','Carrazana'),
('159','DAHUA-159','Luciana Melin','Flores'),
('160','DAHUA-160','Soledad Vaquera','Luciana'),
('161','DAHUA-161','Natalia Quispe','Ivone'),
('162','DAHUA-162','Edelmira Sosa','Ofelia'),
('163','DAHUA-163','Eugenia Alvarez','Angela'),
('164','DAHUA-164','Rafael Rodriguez','Hector'),
('165','DAHUA-165','Angel Bitancor','Miguel'),
('166','DAHUA-166','Aldo Corso','Manuel'),
('167','DAHUA-167','Ariel Galvez','Oscar'),
('168','DAHUA-168','Martinez','Atilio'),
('169','DAHUA-169','Del Valle Penaloza','Alejandra'),
('170','DAHUA-170','apaza margarita','Miriam'),
('171','DAHUA-171','Julieta Nieve','Gala'),
('172','DAHUA-172','Abigail Coraite','Guadalupe'),
('173','DAHUA-173','Benjamin','LIQUIN'),
('174','DAHUA-174','Pedro Gustavo Ezequiel','MORALES'),
('175','DAHUA-175','Gaspar','Ariana'),
('176','DAHUA-176','','tomas')
ON DUPLICATE KEY UPDATE nombres=VALUES(nombres), apellidos=VALUES(apellidos);
-- 4) Marcaciones extraídas del equipo
INSERT IGNORE INTO marcaciones (empleado_id, dispositivo_id, fecha_hora, tipo, metodo, origen)
SELECT e.id, (SELECT id FROM dispositivos WHERE numero_serie='AE06085PAJ13AAD'), v.fh, v.tipo, v.metodo, 'IMPORTACION' FROM (
SELECT '58' AS cod, '2026-11-05 17:09:26' AS fh, 'SALIDA' AS tipo, 'ROSTRO' AS metodo
UNION ALL SELECT '58', '2026-11-05 17:00:44', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-11-05 17:00:37', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '143', '2026-11-04 16:21:45', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '36', '2026-11-04 16:21:41', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '62', '2026-11-04 16:13:16', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '19', '2026-11-04 16:13:03', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '12', '2026-10-22 17:39:18', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-21 16:22:38', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '70', '2026-10-21 16:18:11', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '70', '2026-10-21 16:17:48', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '70', '2026-10-21 16:17:41', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '70', '2026-10-21 16:17:38', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-21 16:09:13', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-21 16:09:10', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-21 16:08:14', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-21 16:06:28', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '70', '2026-10-21 16:05:20', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '70', '2026-10-21 16:04:50', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '70', '2026-10-21 16:04:47', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '70', '2026-10-21 16:04:32', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-21 15:59:53', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-21 15:59:33', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-21 15:59:30', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '68', '2026-10-15 18:24:32', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 18:13:08', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 18:08:00', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 18:07:57', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 18:07:51', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 18:07:48', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 18:07:45', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 17:48:58', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '68', '2026-10-15 17:40:29', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 17:35:36', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-15 17:33:36', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '43', '2026-10-15 17:27:58', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 17:26:33', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-15 17:26:30', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '62', '2026-10-15 17:26:12', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '62', '2026-10-15 17:26:11', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-13 12:29:26', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '59', '2026-10-13 12:28:46', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-13 12:24:51', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-13 12:22:58', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '69', '2026-10-13 12:22:04', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-13 12:21:09', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-13 12:20:15', 'SALIDA', 'OTRO'
UNION ALL SELECT '62', '2026-10-13 12:14:32', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '59', '2026-10-13 12:13:15', 'SALIDA', 'OTRO'
UNION ALL SELECT '66', '2026-10-13 12:04:40', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-13 12:04:37', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-13 12:04:19', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-13 12:04:11', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-13 12:04:08', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-13 12:04:05', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-13 12:04:01', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '59', '2026-10-13 11:56:40', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-08 18:36:27', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-08 18:36:24', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-08 18:33:25', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-08 18:33:20', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-08 18:33:17', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '153', '2026-10-08 18:33:13', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '59', '2026-10-08 18:32:34', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '59', '2026-10-08 18:31:46', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-08 18:31:43', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-08 18:31:38', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '59', '2026-10-08 18:30:51', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '59', '2026-10-08 18:30:37', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '69', '2026-10-08 18:30:02', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '36', '2026-10-08 18:29:59', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-08 18:29:55', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-08 18:28:34', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '62', '2026-10-08 18:28:15', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-08 18:26:45', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '58', '2026-10-08 18:26:10', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-08 18:26:07', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-08 18:25:22', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '66', '2026-10-08 18:25:18', 'ENTRADA', 'ROSTRO'
UNION ALL SELECT '69', '2026-10-08 18:24:10', 'SALIDA', 'ROSTRO'
UNION ALL SELECT '69', '2026-10-08 18:24:07', 'ENTRADA', 'ROSTRO'
) v JOIN empleados e ON e.codigo_biometrico = v.cod;
UPDATE dispositivos SET ultima_sync = NOW() WHERE numero_serie='AE06085PAJ13AAD';
SET SQL_SAFE_UPDATES = 1;
SELECT (SELECT COUNT(*) FROM empleados) AS empleados, (SELECT COUNT(*) FROM marcaciones) AS marcaciones;
