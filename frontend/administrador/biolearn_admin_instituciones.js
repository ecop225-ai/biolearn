/* ================================================================
   BIOLearn — biolearn_admin_instituciones.js
   Conectado al backend real: /api/instituciones (exclusivo del Administrador)
   Listado en tabla (como Gestión de usuarios) con filtros, Ver y Editar.
   - Estado: se filtra en el backend (?estado=). Búsqueda, cupos y representante: se filtran localmente.
   - El código de afiliación lo genera el backend (8 caracteres); aquí se muestra, copia o regenera.
   - El representante se crea desde Usuarios (rol Supervisor); aquí se muestra y se ofrece el atajo.
================================================================ */
let INST_CACHE = [];
let INST_FILTRO_ESTADO = 'todas';
let INST_FILTRO_CUPOS = 'todos';
let INST_FILTRO_REP = 'todos';
let INST_BUSQUEDA = '';

function renderInstituciones(cont) {
  // Igual que en Usuarios: la barra de filtros se dibuja una vez y al escribir solo se redibuja la tabla
  cont.innerHTML = '<div class="section-header"><div><h2>Instituciones</h2>' +
    '<p class="subtitle">Organizaciones que contratan cupos para sus estudiantes — conectado a tu backend real</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalInstitucion()">+ Registrar institución</button></div>' +
    '<div class="flex gap-10 flex-wrap mb-10">' +
    '<input id="inst-buscar" placeholder="Buscar por nombre, NIT, código, rector o representante..." value="' + esc(INST_BUSQUEDA) + '" oninput="INST_BUSQUEDA=this.value;pintarInstituciones()" style="width:320px">' +
    selectFiltroInst('inst-f-estado', 'INST_FILTRO_ESTADO', INST_FILTRO_ESTADO, 'cargarInstituciones()',
      [['todas', 'Todos los estados'], ['activa', 'Activas'], ['inactiva', 'Inactivas'], ['suspendida', 'Suspendidas']]) +
    selectFiltroInst('inst-f-cupos', 'INST_FILTRO_CUPOS', INST_FILTRO_CUPOS, 'pintarInstituciones()',
      [['todos', 'Todos los cupos'], ['disponibles', 'Con cupos disponibles'], ['casi', 'Casi llenas (90 % o más)'], ['llenas', 'Sin cupos disponibles'], ['sinlimite', 'Sin límite de cupos']]) +
    selectFiltroInst('inst-f-rep', 'INST_FILTRO_REP', INST_FILTRO_REP, 'pintarInstituciones()',
      [['todos', 'Con o sin representante'], ['con', 'Con representante'], ['pendiente', 'Representante con invitación pendiente'], ['sin', 'Sin representante']]) +
    '</div>' +
    '<div id="inst-resumen" class="flex items-center gap-10 text-sm muted" style="margin-bottom:10px"></div>' +
    '<div id="inst-lista">Cargando instituciones...</div>';
  renderModalInstitucion();
  renderModalDetalleInstitucion();
  cargarInstituciones();
}

function selectFiltroInst(id, variable, actual, accion, opciones) {
  return '<select id="' + id + '" onchange="' + variable + '=this.value;' + accion + '" style="width:210px">' +
    opciones.map(([v, t]) => '<option value="' + v + '"' + (actual === v ? ' selected' : '') + '>' + t + '</option>').join('') + '</select>';
}

async function cargarInstituciones() {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const params = new URLSearchParams();
    if (INST_FILTRO_ESTADO !== 'todas') params.set('estado', INST_FILTRO_ESTADO);
    const response = await fetch(API_BASE + '/api/instituciones?' + params.toString(), { headers });
    if (manejarSesionExpirada(response)) return;
    if (response.status === 403) { toast('Tu usuario no tiene permisos de Administrador', true); return; }
    if (!response.ok) throw new Error('El servidor respondió con error ' + response.status);
    INST_CACHE = await response.json();
    pintarInstituciones();
  } catch (err) {
    document.getElementById('inst-lista').innerHTML = '<div class="empty-state">No se pudo conectar con el servidor (' + esc(API_BASE) + '). ¿Está encendido? (' + esc(err.message) + ')</div>';
  }
}

function porcentajeCupos(i) {
  return i.cuposContratados ? Math.round((i.cuposUsados / i.cuposContratados) * 100) : 0;
}

function cumpleFiltroCupos(i) {
  switch (INST_FILTRO_CUPOS) {
    case 'disponibles': return i.cuposContratados == null || i.cuposDisponibles > 0;
    case 'casi': return i.cuposContratados != null && porcentajeCupos(i) >= 90 && i.cuposDisponibles > 0;
    case 'llenas': return i.cuposContratados != null && i.cuposDisponibles === 0;
    case 'sinlimite': return i.cuposContratados == null;
    default: return true;
  }
}

function cumpleFiltroRep(i) {
  switch (INST_FILTRO_REP) {
    case 'con': return !!i.representante;
    case 'pendiente': return !!i.representante && i.representante.estadoCuenta === 'pendiente';
    case 'sin': return !i.representante;
    default: return true;
  }
}

const TEXTO_FILTRO_CUPOS = { disponibles: 'con cupos disponibles', casi: 'casi llenas', llenas: 'sin cupos', sinlimite: 'sin límite' };
const TEXTO_FILTRO_REP = { con: 'con representante', pendiente: 'invitación pendiente', sin: 'sin representante' };

function limpiarFiltrosInstituciones() {
  INST_FILTRO_ESTADO = 'todas'; INST_FILTRO_CUPOS = 'todos'; INST_FILTRO_REP = 'todos'; INST_BUSQUEDA = '';
  document.getElementById('inst-buscar').value = '';
  document.getElementById('inst-f-estado').value = 'todas';
  document.getElementById('inst-f-cupos').value = 'todos';
  document.getElementById('inst-f-rep').value = 'todos';
  cargarInstituciones();
}

function pintarInstituciones() {
  const q = normalizarTextoInst(INST_BUSQUEDA.trim());
  const lista = INST_CACHE.filter(i =>
    (!q || normalizarTextoInst([i.nombre, i.nit, i.codigoAfiliacion, i.rector, i.correo, i.representante && i.representante.nombreCompleto].join(' ')).includes(q)) &&
    cumpleFiltroCupos(i) && cumpleFiltroRep(i));

  const activos = [];
  if (INST_FILTRO_ESTADO !== 'todas') activos.push('Estado: ' + INST_FILTRO_ESTADO);
  if (INST_FILTRO_CUPOS !== 'todos') activos.push('Cupos: ' + TEXTO_FILTRO_CUPOS[INST_FILTRO_CUPOS]);
  if (INST_FILTRO_REP !== 'todos') activos.push('Representante: ' + TEXTO_FILTRO_REP[INST_FILTRO_REP]);
  if (q) activos.push('Búsqueda: "' + INST_BUSQUEDA.trim() + '"');
  document.getElementById('inst-resumen').innerHTML = '<span>Mostrando <strong>' + lista.length + '</strong> ' +
    (lista.length === 1 ? 'institución' : 'instituciones') + (activos.length ? ' · Filtros activos: ' + esc(activos.join(' · ')) : '') + '</span>' +
    (activos.length ? '<button class="btn btn-ghost btn-xs" onclick="limpiarFiltrosInstituciones()">✕ Limpiar filtros</button>' : '');

  let html = '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
    ['Institución', 'Código', 'Cupos', 'Representante', 'Estado', 'Registrada', ''].map(h => '<th style="color:var(--blue)">' + h + '</th>').join('') +
    '</tr></thead><tbody>';
  lista.forEach(i => {
    const pct = porcentajeCupos(i);
    const rep = i.representante;
    html += '<tr><td><div style="font-weight:600">' + esc(i.nombre) + '</div><div class="text-sm muted">NIT ' + esc(i.nit) + ' · #' + i.id + '</div></td>' +
      '<td><span style="font-family:monospace;font-weight:700;letter-spacing:1px">' + esc(i.codigoAfiliacion) + '</span></td>' +
      '<td style="min-width:120px"><div class="text-sm">' + (i.cuposContratados ? i.cuposUsados + ' / ' + i.cuposContratados : i.cuposUsados + ' · sin límite') + '</div>' +
      (i.cuposContratados ? '<div class="progress-bar" style="margin-top:4px"><div class="progress-fill" style="width:' + Math.min(pct, 100) + '%;background:' + (pct >= 90 ? 'var(--clay)' : 'var(--blue)') + '"></div></div>' : '') + '</td>' +
      '<td class="text-sm">' + (rep ? esc(rep.nombreCompleto) + (rep.estadoCuenta === 'pendiente' ? '<div class="muted">invitación pendiente</div>' : '') : '<span class="muted">Sin asignar</span>') + '</td>' +
      '<td><select onchange="cambiarEstadoInstitucion(' + i.id + ', this.value)" style="width:auto;padding:4px 8px;font-size:12.5px">' +
      ['activa', 'inactiva', 'suspendida'].map(e => '<option value="' + e + '"' + (i.estado === e ? ' selected' : '') + '>' + e + '</option>').join('') + '</select></td>' +
      '<td class="text-sm muted">' + esc(i.fechaRegistro || '—') + '</td>' +
      '<td style="white-space:nowrap;text-align:right"><button class="btn btn-ghost btn-xs" onclick="verDetalleInstitucion(' + i.id + ')">👁 Ver</button> ' +
      '<button class="btn btn-ghost btn-xs" onclick="abrirModalInstitucion(' + i.id + ')">✏️ Editar</button></td></tr>';
  });
  if (!lista.length) html += '<tr><td colspan="7" class="empty-state">No hay instituciones que coincidan.</td></tr>';
  document.getElementById('inst-lista').innerHTML = html + '</tbody></table></div>';
}

/* ---------- Ver detalle: consulta GET /api/instituciones/{id} para mostrar datos frescos ---------- */
function renderModalDetalleInstitucion() {
  if (document.getElementById('modal-detalle-institucion')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-detalle-institucion"><div class="modal" style="max-width:560px"><div class="modal-header"><h3>Detalle de la institución</h3>' +
    '<button class="modal-close" onclick="cerrarModal(\'modal-detalle-institucion\')">✕</button></div>' +
    '<div class="modal-body" id="inst-detalle-body">Cargando...</div>' +
    '<div class="modal-btns" id="inst-detalle-btns"></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function verDetalleInstitucion(id) {
  const headers = authHeaders();
  if (!headers) return;
  document.getElementById('inst-detalle-body').innerHTML = 'Cargando...';
  document.getElementById('inst-detalle-btns').innerHTML = '';
  abrirModal('modal-detalle-institucion');
  try {
    const response = await fetch(API_BASE + '/api/instituciones/' + id, { headers });
    if (manejarSesionExpirada(response)) return;
    const i = await response.json();
    if (!response.ok) throw new Error(i.mensaje || 'No se pudo cargar la institución.');
    const fila = (k, v) => '<div class="flex justify-between" style="padding:8px 0;border-bottom:1px solid var(--line, #eee)"><span class="muted">' + k + '</span><span style="font-weight:600;text-align:right">' + v + '</span></div>';
    const rep = i.representante;
    document.getElementById('inst-detalle-body').innerHTML =
      fila('ID', i.id) + fila('Nombre', esc(i.nombre)) + fila('NIT', esc(i.nit)) +
      fila('Código de afiliación', '<span style="font-family:monospace;letter-spacing:1px">' + esc(i.codigoAfiliacion) + '</span> <button class="btn btn-ghost btn-xs" onclick="copiarCodigoInstitucion(\'' + esc(i.codigoAfiliacion) + '\')">📋 Copiar</button>') +
      fila('Estado', esc(i.estado)) +
      fila('Cupos contratados', i.cuposContratados == null ? 'Sin límite' : i.cuposContratados) +
      fila('Estudiantes afiliados (activos)', i.cuposUsados) +
      fila('Cupos disponibles', i.cuposDisponibles == null ? 'Sin límite' : i.cuposDisponibles) +
      fila('Representante', rep ? esc(rep.nombreCompleto) + '<div class="text-sm muted">' + esc(rep.correo) + (rep.cargo ? ' · ' + esc(rep.cargo) : '') + ' · cuenta ' + esc(rep.estadoCuenta) + '</div>' : 'Sin asignar') +
      fila('Rector', esc(i.rector || '—')) + fila('Correo', esc(i.correo || '—')) + fila('Teléfono', esc(i.telefono || '—')) +
      fila('Dirección', esc(i.direccion || '—')) + fila('Registrada', esc(i.fechaRegistro || '—'));
    document.getElementById('inst-detalle-btns').innerHTML =
      '<button class="btn btn-ghost" onclick="cerrarModal(\'modal-detalle-institucion\');regenerarCodigoInstitucion(' + i.id + ')">🔄 Nuevo código</button>' +
      (!rep && i.estado === 'activa' ? '<button class="btn btn-ghost" onclick="cerrarModal(\'modal-detalle-institucion\');asignarRepresentante(' + i.id + ')">+ Representante</button>' : '') +
      '<button class="btn btn-primary" onclick="cerrarModal(\'modal-detalle-institucion\');abrirModalInstitucion(' + i.id + ')">✏️ Editar</button>';
  } catch (err) {
    document.getElementById('inst-detalle-body').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>';
  }
}

function normalizarTextoInst(t) {
  return String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

function copiarCodigoInstitucion(codigo) {
  if (navigator.clipboard) navigator.clipboard.writeText(codigo).catch(() => {});
  toast('Código ' + codigo + ' copiado al portapapeles');
}

/* Atajo: abre Usuarios con el modal de creación listo para un Supervisor de esta institución */
/* "+ Representante": permite ASIGNAR un representante existente que no tiene institución (por ejemplo, porque
   cambió de colegio o su institución se dio de baja) o CREAR uno nuevo, que era la única opción antes. */
async function asignarRepresentante(idInstitucion) {
  renderModalAsignarRepresentante();
  const inst = INST_CACHE.find(i => i.id === idInstitucion);
  const modal = document.getElementById('modal-asignar-rep');
  modal.dataset.inst = idInstitucion;
  document.getElementById('rep-titulo').textContent = 'Representante de ' + (inst ? inst.nombre : 'la institución');
  document.getElementById('rep-cargo').value = '';
  const sel = document.getElementById('rep-sel');
  sel.innerHTML = '<option value="">Cargando…</option>';
  abrirModal('modal-asignar-rep');
  try {
    const response = await fetch(API_BASE + '/api/usuarios?rol=supervisor', { headers: authHeaders() });
    if (manejarSesionExpirada(response)) return;
    const libres = (response.ok ? await response.json() : []).filter(u => !u.idInstitucion && u.estado !== 'inactivo');
    sel.innerHTML = libres.length
      ? '<option value="">-- Selecciona un representante sin institución --</option>' +
        libres.map(u => '<option value="' + u.id + '">' + esc(u.nombre + ' ' + u.apellido) + ' · ' + esc(u.correo) + (u.estado === 'pendiente' ? ' (pendiente)' : '') + '</option>').join('')
      : '<option value="">No hay representantes sin institución</option>';
    sel.disabled = !libres.length;
    document.getElementById('rep-btn').disabled = !libres.length;
    document.getElementById('rep-hint').textContent = libres.length
      ? 'Representantes con la cuenta activa o pendiente que hoy no tienen institución.'
      : 'Todos los representantes tienen institución. Crea uno nuevo para esta institución.';
  } catch (err) {
    sel.innerHTML = '<option value="">No se pudieron cargar los representantes</option>';
  }
}

function renderModalAsignarRepresentante() {
  if (document.getElementById('modal-asignar-rep')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-asignar-rep"><div class="modal" style="max-width:520px"><div class="modal-header"><h3 id="rep-titulo">Representante</h3>' +
    '<button class="modal-close" onclick="cerrarModal(\'modal-asignar-rep\')">✕</button></div><div class="modal-body">' +
    '<div class="campo"><label for="rep-sel">Asignar un representante existente</label><select id="rep-sel"></select><p class="hint" id="rep-hint"></p></div>' +
    '<div class="campo"><label for="rep-cargo">Cargo (opcional)</label><input id="rep-cargo" maxlength="150" placeholder="Ej.: Coordinador académico"></div>' +
    '</div><div class="modal-btns"><button class="btn btn-ghost" id="rep-crear-btn" onclick="crearRepresentanteNuevo()">+ Crear un representante nuevo</button>' +
    '<button class="btn btn-primary" id="rep-btn" onclick="guardarAsignacionRepresentante()">Asignar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function guardarAsignacionRepresentante() {
  const idInstitucion = Number(document.getElementById('modal-asignar-rep').dataset.inst);
  const idUsuario = Number(document.getElementById('rep-sel').value);
  if (!idUsuario) { toast('Selecciona un representante', true); return; }
  const cargo = document.getElementById('rep-cargo').value.trim();
  try {
    const response = await fetch(API_BASE + '/api/usuarios/' + idUsuario + '/institucion', {
      method: 'PUT', headers: authHeaders(), body: JSON.stringify({ idInstitucion, cargo: cargo || null }) });
    if (manejarSesionExpirada(response)) return;
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo asignar el representante.');
    toast(data.nombre + ' ' + data.apellido + ' ahora representa a ' + data.institucion);
    cerrarModal('modal-asignar-rep');
    cargarInstituciones();
  } catch (err) {
    toast(err.message, true);
  }
}

/* Flujo anterior: crear la cuenta del representante con la institución preseleccionada. */
function crearRepresentanteNuevo() {
  const idInstitucion = Number(document.getElementById('modal-asignar-rep').dataset.inst);
  cerrarModal('modal-asignar-rep');
  goTo('usuarios');
  setTimeout(() => {
    abrirModalUsuario();
    document.getElementById('usr-rol').value = 'supervisor';
    actualizarCamposPorRolUsuario();
    cargarOpcionesInstitucionUsuario(idInstitucion);
  }, 150);
}

function regenerarCodigoInstitucion(id) {
  const inst = INST_CACHE.find(i => i.id === id);
  confirmarAccion('¿Generar un nuevo código para <strong>' + esc(inst.nombre) + '</strong>? El código actual (<code>' + esc(inst.codigoAfiliacion) +
    '</code>) dejará de funcionar para nuevos registros. Los estudiantes ya afiliados no se ven afectados.', async function () {
    const headers = authHeaders();
    if (!headers) return;
    try {
      const response = await fetch(API_BASE + '/api/instituciones/' + id + '/regenerar-codigo', { method: 'POST', headers });
      if (manejarSesionExpirada(response)) return;
      const data = await response.json();
      if (!response.ok) throw new Error(data.mensaje || 'No se pudo generar el código.');
      toast('Nuevo código de ' + data.nombre + ': ' + data.codigoAfiliacion);
      cargarInstituciones();
    } catch (err) { toast(err.message, true); }
  }, 'Generar nuevo código');
}

async function cambiarEstadoInstitucion(id, estado) {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const response = await fetch(API_BASE + '/api/instituciones/' + id + '/estado', {
      method: 'PATCH', headers, body: JSON.stringify({ estado })
    });
    if (manejarSesionExpirada(response)) return;
    const data = await response.json();
    if (!response.ok) throw new Error((data.detalles && data.detalles.length ? data.detalles.join(' · ') : data.mensaje) || 'No se pudo cambiar el estado.');
    toast(data.nombre + ' quedó ' + data.estado + (estado !== 'activa' ? ': no admite nuevas afiliaciones' : ''));
    cargarInstituciones();
  } catch (err) { toast(err.message, true); cargarInstituciones(); }
}

function abrirModalInstitucion(id) {
  const inst = id ? INST_CACHE.find(x => x.id === id) : null;
  document.getElementById('inst-modal-titulo').textContent = inst ? 'Editar institución' : 'Registrar institución';
  document.getElementById('inst-modal-btn').textContent = inst ? 'Guardar cambios' : 'Registrar';
  document.getElementById('inst-id').value = inst ? inst.id : '';
  document.getElementById('inst-nombre').value = inst ? inst.nombre : '';
  document.getElementById('inst-nit').value = inst ? inst.nit : '';
  document.getElementById('inst-rector').value = inst ? (inst.rector || '') : '';
  document.getElementById('inst-email').value = inst ? (inst.correo || '') : '';
  document.getElementById('inst-telefono').value = inst ? (inst.telefono || '') : '';
  document.getElementById('inst-direccion').value = inst ? (inst.direccion || '') : '';
  document.getElementById('inst-cupos').value = inst ? (inst.cuposContratados || '') : 20;
  document.getElementById('inst-codigo-info').innerHTML = inst
    ? '🔑 Código de afiliación: <strong style="font-family:monospace">' + esc(inst.codigoAfiliacion) + '</strong> · ' + inst.cuposUsados + ' estudiantes afiliados'
    : '🔑 El código de afiliación de 8 caracteres lo generará el sistema al registrar.';
  abrirModal('modal-institucion');
}

function renderModalInstitucion() {
  if (document.getElementById('modal-institucion')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-institucion"><div class="modal" style="max-width:540px"><div class="modal-header"><h3 id="inst-modal-titulo">Registrar institución</h3><button class="modal-close" onclick="cerrarModal(\'modal-institucion\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="inst-id">' +
    '<p id="inst-codigo-info" class="text-sm" style="background:var(--blue-soft);border-radius:8px;padding:8px 12px;margin:0 0 14px"></p>' +
    '<div class="campo"><label for="inst-nombre">Nombre de la institución *</label><input type="text" id="inst-nombre" maxlength="200"></div>' +
    '<div class="grid-2"><div class="campo"><label for="inst-nit">NIT *</label><input type="text" id="inst-nit" placeholder="900.223.114-2" maxlength="20"></div>' +
    '<div class="campo"><label for="inst-cupos">Cupos contratados *</label><input type="number" id="inst-cupos" min="1" max="65535"></div></div>' +
    '<div class="grid-2"><div class="campo"><label for="inst-rector">Nombre del rector</label><input type="text" id="inst-rector"></div>' +
    '<div class="campo"><label for="inst-telefono">Teléfono</label><input type="text" id="inst-telefono" maxlength="20"></div></div>' +
    '<div class="campo"><label for="inst-email">Correo electrónico</label><input type="email" id="inst-email"></div>' +
    '<div class="campo"><label for="inst-direccion">Dirección</label><input type="text" id="inst-direccion" maxlength="300"></div>' +
    '<p class="text-sm muted">Los cursos contratados por la institución se asignarán en el módulo de Cursos.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-institucion\')">Cancelar</button><button class="btn btn-primary" id="inst-modal-btn" onclick="guardarInstitucion()">Registrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function guardarInstitucion() {
  const headers = authHeaders();
  if (!headers) return;
  const id = document.getElementById('inst-id').value;
  const val = k => document.getElementById(k).value.trim();
  const cuerpo = {
    nombre: val('inst-nombre'), nit: val('inst-nit'), rector: val('inst-rector'), telefono: val('inst-telefono'),
    correo: val('inst-email'), direccion: val('inst-direccion'), cuposContratados: Number(val('inst-cupos')) || null
  };
  if (!cuerpo.nombre || !cuerpo.nit || !cuerpo.cuposContratados) { toast('Nombre, NIT y cupos son obligatorios', true); return; }
  const btn = document.getElementById('inst-modal-btn');
  btn.disabled = true;
  try {
    const response = await fetch(API_BASE + '/api/instituciones' + (id ? '/' + id : ''), {
      method: id ? 'PATCH' : 'POST', headers, body: JSON.stringify(cuerpo)
    });
    if (manejarSesionExpirada(response)) return;
    const data = await response.json();
    if (!response.ok) throw new Error((data.detalles && data.detalles.length ? data.detalles.join(' · ') : data.mensaje) || 'No se pudo guardar.');
    cerrarModal('modal-institucion');
    toast(id ? 'Institución ' + data.nombre + ' actualizada' : 'Institución registrada. Código de afiliación: ' + data.codigoAfiliacion);
    cargarInstituciones();
  } catch (err) {
    toast(err.message, true);
  } finally {
    btn.disabled = false;
  }
}
