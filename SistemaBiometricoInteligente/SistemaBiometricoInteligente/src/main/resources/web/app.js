const $ = s => document.querySelector(s);
// Si abres index.html con doble clic (file://) igual apunta al servidor Java
const BASE = location.protocol === 'file:' ? 'http://localhost:8080' : '';
let token = null;
try { token = sessionStorage.getItem('token'); } catch (e) { }

async function api(ruta, opciones = {}) {
  let r;
  try {
    r = await fetch(BASE + ruta, {
      ...opciones,
      headers: { 'Content-Type': 'application/json', ...(token ? { 'Authorization': 'Bearer ' + token } : {}), ...(opciones.headers || {}) }
    });
  } catch (e) {
    throw new Error('No hay conexión con el servidor. ¿Está corriendo el proyecto en NetBeans (botón ▶)?');
  }
  const texto = await r.text();
  let data = null;
  try { data = texto ? JSON.parse(texto) : null; } catch (e) { data = { error: texto }; }
  if (r.status === 401 && ruta !== '/api/login') { salir(); throw new Error('Sesión expirada, vuelve a ingresar'); }
  if (!r.ok) throw new Error((data && data.error) || ('Error ' + r.status));
  return data;
}

function mostrarError(err) { console.error(err); alert(err.message || err); }

function tabla(el, filas, columnas) {
  if (!filas.length) { el.innerHTML = '<tr><td>Sin registros</td></tr>'; return; }
  el.innerHTML = '<tr>' + columnas.map(c => `<th>${c[1]}</th>`).join('') + '</tr>' +
    filas.map(f => '<tr>' + columnas.map(c => {
      const v = typeof c[0] === 'function' ? c[0](f) : (f[c[0]] ?? '');
      return `<td class="${c[0] === 'estado' ? v : ''}">${v}</td>`;
    }).join('') + '</tr>').join('');
}

const fechaLocal = d => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const hoy = () => fechaLocal(new Date());
const haceDias = n => fechaLocal(new Date(Date.now() - n * 86400000));

// ---------- LOGIN ----------
$('#form-login').addEventListener('submit', async e => {
  e.preventDefault();
  const btn = e.target.querySelector('button');
  const f = Object.fromEntries(new FormData(e.target));
  $('#login-error').textContent = '';
  btn.disabled = true; btn.textContent = 'Ingresando...';
  try {
    const r = await api('/api/login', { method: 'POST', body: JSON.stringify(f) });
    token = r.token;
    try { sessionStorage.setItem('token', token); } catch (x) { }
    mostrarApp();
  } catch (err) {
    $('#login-error').textContent = err.message;
  } finally {
    btn.disabled = false; btn.textContent = 'Ingresar';
  }
});
function salir() {
  try { sessionStorage.removeItem('token'); } catch (x) { }
  token = null; $('#vista-app').hidden = true; $('#vista-login').hidden = false;
}
$('#btn-salir').onclick = salir;

function mostrarApp() { $('#vista-login').hidden = true; $('#vista-app').hidden = false; cargarPanel().catch(mostrarError); }

// ---------- PESTAÑAS ----------
document.querySelectorAll('nav button').forEach(b => b.onclick = () => {
  document.querySelectorAll('nav button').forEach(x => x.classList.remove('activo'));
  b.classList.add('activo');
  document.querySelectorAll('.tab').forEach(t => t.hidden = true);
  $('#tab-' + b.dataset.tab).hidden = false;
  const f = ({ panel: cargarPanel, asistencia: cargarAsistencia, marcaciones: cargarMarcaciones, empleados: cargarEmpleados, importar: cargarImportaciones })[b.dataset.tab];
  if (f) f().catch(mostrarError);
});

// ---------- PANEL ----------
async function cargarPanel() {
  const r = await api('/api/resumen');
  $('#kpis').innerHTML = Object.entries(r).map(([k, v]) => `<div class="kpi"><b>${v}</b>${k}</div>`).join('');
}
$('#form-manual').onsubmit = async e => {
  e.preventDefault();
  try {
    const r = await api('/api/marcaciones', { method: 'POST', body: JSON.stringify(Object.fromEntries(new FormData(e.target))) });
    $('#manual-msg').textContent = r.resultado; cargarPanel();
  } catch (err) { $('#manual-msg').textContent = err.message; }
};

// ---------- ASISTENCIA ----------
['as', 'ma'].forEach(p => { $(`#${p}-desde`).value = haceDias(30); $(`#${p}-hasta`).value = hoy(); });
async function cargarAsistencia() {
  const d = await api(`/api/asistencia?desde=${$('#as-desde').value}&hasta=${$('#as-hasta').value}`);
  tabla($('#tabla-asistencia'), d, [['fecha','Fecha'],['codigoBiometrico','Código'],['empleado','Empleado'],['area','Área'],
    ['horarioEntrada','Horario'],['horaEntrada','Entrada'],['horaSalida','Salida'],['estado','Estado']]);
}
$('#as-buscar').onclick = () => cargarAsistencia().catch(mostrarError);

// ---------- MARCACIONES ----------
async function cargarMarcaciones() {
  const d = await api(`/api/marcaciones?desde=${$('#ma-desde').value}&hasta=${$('#ma-hasta').value}`);
  tabla($('#tabla-marcaciones'), d, [['fechaHora','Fecha/Hora'],['codigoBiometrico','Código'],['empleado','Empleado'],
    ['tipo','Tipo'],['metodo','Método'],['origen','Origen'],['dispositivo','Dispositivo']]);
}
$('#ma-buscar').onclick = () => cargarMarcaciones().catch(mostrarError);

// ---------- EMPLEADOS ----------
let empleados = [];
async function cargarEmpleados() {
  empleados = await api('/api/empleados');
  tabla($('#tabla-empleados'), empleados, [['codigoBiometrico','Código'],['documento','Documento'],
    [e => e.apellidos + ', ' + e.nombres,'Nombre'],['area','Área'],['horario','Horario'],['email','Email'],
    [e => `<button onclick="editar(${e.id})">Editar</button> <button class="secundario" onclick="eliminar(${e.id})">Eliminar</button>`,'Acciones']]);
}
window.editar = id => {
  const e = empleados.find(x => x.id === id), f = $('#form-empleado');
  for (const k of ['id','codigoBiometrico','documento','nombres','apellidos','email','areaId','horarioId']) f.elements[k].value = e[k] ?? '';
};
window.eliminar = async id => {
  if (!confirm('¿Dar de baja a este empleado?')) return;
  try { await api('/api/empleados/' + id, { method: 'DELETE' }); await cargarEmpleados(); } catch (err) { mostrarError(err); }
};
$('#form-empleado').onsubmit = async e => {
  e.preventDefault();
  const f = Object.fromEntries(new FormData(e.target));
  const id = f.id; delete f.id;
  f.areaId = f.areaId ? Number(f.areaId) : null; f.horarioId = f.horarioId ? Number(f.horarioId) : null;
  try {
    await api(id ? '/api/empleados/' + id : '/api/empleados', { method: id ? 'PUT' : 'POST', body: JSON.stringify(f) });
    e.target.reset(); cargarEmpleados();
  } catch (err) { alert(err.message); }
};

// ---------- IMPORTAR ----------
async function cargarImportaciones() {
  const d = await api('/api/importaciones');
  tabla($('#tabla-importaciones'), d, [['fecha','Fecha'],['archivo','Archivo'],['formato','Formato'],
    ['registros','Registros'],['importadas','Nuevas'],['duplicadas','Duplicadas'],['errores','Errores']]);
}
$('#btn-importar').onclick = async () => {
  const file = $('#archivo').files[0];
  const res = $('#importar-res');
  if (!file) { res.innerHTML = '<p class="error">Primero elige el archivo del pendrive.</p>'; return; }
  const btn = $('#btn-importar');
  btn.disabled = true; btn.textContent = 'Importando...';
  try {
    try { localStorage.setItem('ajusteHoras', $('#ajuste').value); } catch (x) { }
    const r = await api('/api/importar?serie=' + encodeURIComponent($('#serie').value) + '&archivo=' + encodeURIComponent(file.name)
                        + '&ajusteHoras=' + encodeURIComponent($('#ajuste').value || '0'),
                        { method: 'POST', body: await file.text(), headers: { 'Content-Type': 'text/plain' } });
    res.innerHTML = (r.tipoArchivo ? `<p>Archivo detectado: <b>${r.tipoArchivo}</b></p>` : '') + `<div class="kpis">
        <div class="kpi"><b>${r.registrosEnArchivo ?? (r.importadas + r.duplicadas + r.errores)}</b>en el archivo</div>
        <div class="kpi"><b>${r.importadas}</b>marcaciones nuevas</div>
        <div class="kpi"><b>${r.duplicadas}</b>ya existían</div>
        <div class="kpi"><b>${r.empleadosNuevos ?? 0}</b>empleados nuevos</div>
        ${r.intentosFallidos != null ? `<div class="kpi"><b>${r.intentosFallidos}</b>intentos fallidos (ignorados)</div>` : ''}
        ${r.horariosGuardados != null ? `<div class="kpi"><b>${r.horariosGuardados}</b>horarios guardados</div>` : ''}
        <div class="kpi"><b>${r.errores}</b>errores</div></div>` +
      (r.detalleErrores && r.detalleErrores.length ? '<pre>' + r.detalleErrores.join('\n') + '</pre>' : '');
    cargarImportaciones().catch(() => {});
  } catch (err) {
    res.innerHTML = '<p class="error">' + err.message + '</p>';
  } finally {
    btn.disabled = false; btn.textContent = 'Importar';
  }
};

try { const a = localStorage.getItem('ajusteHoras'); if (a !== null) $('#ajuste').value = a; } catch (x) { }
if (token) mostrarApp();
console.log('Sistema Biométrico Inteligente: interfaz cargada');
