/* ================================================================
   BIOLearn — biolearn_admin_usuarios.js
   Conectado al backend real: GET/POST /api/usuarios, PATCH /api/usuarios/{id}/estado
   Filtros: rol, estado e institución se envían al backend; la búsqueda por texto se aplica localmente
================================================================ */
let USR_FILTRO_ROL = 'todos';
let USR_FILTRO_ESTADO = 'todos';
let USR_BUSQUEDA = '';
let USR_FILTRO_INST = 'todas';
/* Instituciones vistas en los listados (id → nombre). Solo crece, para que el selector no pierda opciones
   al filtrar. Cuando exista el módulo de Instituciones se llenará con GET /api/instituciones. */
let USR_INSTITUCIONES = {};
let USR_CACHE = []; // último listado traído del backend, para filtrar localmente por texto

function renderUsuarios(cont) {
  // La barra de filtros se dibuja UNA sola vez. Al escribir en el buscador solo se redibuja la tabla
  // (antes se redibujaba todo, incluida la casilla de búsqueda, y por eso se perdía el cursor en cada letra).
  cont.innerHTML = '<div class="section-header"><div><h2>Gestión de usuarios</h2>' +
    '<p class="subtitle">Crea y administra las cuentas de Docente, Contador y Representante institucional</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalUsuario()">+ Crear usuario</button></div>' +
    '<div class="flex gap-10 flex-wrap mb-10">' +
    '<input id="usr-buscar" placeholder="Buscar por nombre, usuario, correo o institución..." value="' + esc(USR_BUSQUEDA) + '" oninput="USR_BUSQUEDA=this.value;pintarTabla()" style="width:300px">' +
    '<select id="usr-f-rol" onchange="USR_FILTRO_ROL=this.value;cargarUsuarios()" style="width:230px">' +
    ['todos', 'admin', 'docente', 'contador', 'supervisor', 'estudiante'].map(r =>
      '<option value="' + r + '"' + (USR_FILTRO_ROL === r ? ' selected' : '') + '>' + etiquetaRol(r) + '</option>').join('') +
    '</select>' +
    '<select id="usr-f-estado" onchange="USR_FILTRO_ESTADO=this.value;cargarUsuarios()" style="width:180px">' +
    ['todos', 'pendiente', 'activo', 'inactivo', 'suspendido'].map(e =>
      '<option value="' + e + '"' + (USR_FILTRO_ESTADO === e ? ' selected' : '') + '>' + (e === 'todos' ? 'Todos los estados' : e[0].toUpperCase() + e.slice(1)) + '</option>').join('') +
    '</select>' +
    '<select id="usr-f-inst" onchange="USR_FILTRO_INST=this.value;cargarUsuarios()" style="width:260px" title="Muestra representantes y estudiantes afiliados a la institución"></select>' +
    '</div>' +
    '<div id="usr-resumen" class="text-sm muted" style="margin-bottom:10px;display:flex;align-items:center;gap:10px;flex-wrap:wrap"></div>' +
    '<div id="usr-lista">Cargando usuarios...</div>';
  pintarOpcionesInstitucion();
  renderModalUsuario();
  renderModalEditarUsuario();
  cargarInstitucionesParaFiltro();
  cargarUsuarios();
}

/* Llena el selector del filtro con TODAS las instituciones (no solo las que ya tienen usuarios) */
async function cargarInstitucionesParaFiltro() {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const response = await fetch(API_BASE + '/api/instituciones', { headers });
    if (!response.ok) return;
    (await response.json()).forEach(i => { USR_INSTITUCIONES[i.id] = i.nombre; });
    pintarOpcionesInstitucion();
  } catch (err) { /* el filtro sigue funcionando con las instituciones vistas en el listado */ }
}

function etiquetaRol(r) {
  return r === 'todos' ? 'Todos los roles' : r === 'supervisor' ? 'Representante institucional' : r[0].toUpperCase() + r.slice(1);
}

async function cargarUsuarios() {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const params = new URLSearchParams();
    if (USR_FILTRO_ROL !== 'todos') params.set('rol', USR_FILTRO_ROL);
    if (USR_FILTRO_ESTADO !== 'todos') params.set('estado', USR_FILTRO_ESTADO);
    if (USR_FILTRO_INST !== 'todas') params.set('idInstitucion', USR_FILTRO_INST);
    const response = await fetch(API_BASE + '/api/usuarios?' + params.toString(), { headers });
    if (manejarSesionExpirada(response)) return;
    if (response.status === 403) { toast('Tu usuario no tiene permisos de Administrador', true); USR_CACHE = []; pintarTabla(); return; }
    if (!response.ok) throw new Error('El servidor respondió con error ' + response.status);
    USR_CACHE = await response.json();
    USR_CACHE.forEach(u => { if (u.idInstitucion) USR_INSTITUCIONES[u.idInstitucion] = u.institucion; });
    pintarOpcionesInstitucion();
    pintarTabla();
  } catch (err) {
    document.getElementById('usr-lista').innerHTML = '<div class="empty-state">No se pudo conectar con el servidor (' + esc(API_BASE) + '). ¿Está encendido? (' + esc(err.message) + ')</div>';
  }
}

/* Solo actualiza las opciones del selector de institución (sin tocar el resto de la barra) */
function pintarOpcionesInstitucion() {
  const sel = document.getElementById('usr-f-inst');
  if (!sel) return;
  sel.innerHTML = '<option value="todas">Todas las instituciones</option>' +
    Object.keys(USR_INSTITUCIONES).sort((a, b) => USR_INSTITUCIONES[a].localeCompare(USR_INSTITUCIONES[b])).map(id =>
      '<option value="' + id + '"' + (String(USR_FILTRO_INST) === id ? ' selected' : '') + '>🏫 ' + esc(USR_INSTITUCIONES[id]) + ' (#' + id + ')</option>').join('');
}

/* Minúsculas y sin tildes, para que "andres" encuentre "Andrés" y "munoz" encuentre "Muñoz" */
function normalizarTexto(t) {
  return String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

function limpiarFiltrosUsuarios() {
  USR_FILTRO_ROL = 'todos'; USR_FILTRO_ESTADO = 'todos'; USR_FILTRO_INST = 'todas'; USR_BUSQUEDA = '';
  document.getElementById('usr-buscar').value = '';
  document.getElementById('usr-f-rol').value = 'todos';
  document.getElementById('usr-f-estado').value = 'todos';
  pintarOpcionesInstitucion();
  cargarUsuarios();
}

/* Compatibilidad: otras funciones del prototipo llaman pintarLista(...) después de crear/editar */
function pintarLista() { pintarTabla(); }

function pintarTabla() {
  const q = normalizarTexto(USR_BUSQUEDA.trim());
  const filtrados = USR_CACHE.filter(u => !q || normalizarTexto(
    u.nombre + ' ' + u.apellido + ' ' + (u.nombreUsuario || '') + ' ' + u.correo + ' ' + (u.institucion || '')
  ).includes(q));

  // Resumen: cuántos se muestran y qué filtros están limitando el resultado (para que no "falten" usuarios sin explicación)
  const activos = [];
  if (USR_FILTRO_ROL !== 'todos') activos.push('Rol: ' + etiquetaRol(USR_FILTRO_ROL));
  if (USR_FILTRO_ESTADO !== 'todos') activos.push('Estado: ' + USR_FILTRO_ESTADO);
  if (USR_FILTRO_INST !== 'todas') activos.push('Institución: ' + (USR_INSTITUCIONES[USR_FILTRO_INST] || '#' + USR_FILTRO_INST) + ' (#' + USR_FILTRO_INST + ')');
  if (q) activos.push('Búsqueda: "' + USR_BUSQUEDA.trim() + '"');
  const resumen = document.getElementById('usr-resumen');
  if (resumen) {
    resumen.innerHTML = '<span>Mostrando <strong>' + filtrados.length + '</strong> usuario' + (filtrados.length === 1 ? '' : 's') +
      (activos.length ? ' · Filtros activos: ' + esc(activos.join(' · ')) : '') + '</span>' +
      (activos.length ? '<button class="btn btn-ghost btn-xs" onclick="limpiarFiltrosUsuarios()">✕ Limpiar filtros</button>' : '');
  }

  let html = '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
    ['Nombre', 'Correo', 'Rol', 'Estado', 'Registrado', ''].map(h => '<th style="color:var(--blue)">' + h + '</th>').join('') +
    '</tr></thead><tbody>';
  filtrados.forEach(u => {
    html += '<tr><td style="font-weight:600">' + esc(u.nombre + ' ' + u.apellido) + '</td>' +
      '<td class="text-sm muted">' + esc(u.correo) + '</td>' +
      '<td>' + rolePill(u.rol) + (u.institucion ? '<div class="text-sm muted" style="margin-top:4px">🏫 ' + esc(u.institucion) + '</div>'
        : (u.rol === 'supervisor' ? '<div class="text-sm" style="margin-top:4px;color:var(--clay)">⚠️ Sin institución asignada</div>' : '')) + '</td>' +
      '<td><select onchange="cambiarEstadoUsuario(' + u.id + ', this.value)" style="width:auto;padding:4px 8px;font-size:12.5px">' +
      // 'pendiente' solo se muestra (deshabilitado) si es el estado actual: el admin no puede asignarlo
      (u.estado === 'pendiente' ? '<option value="pendiente" selected disabled>pendiente</option>' : '') +
      ['activo', 'inactivo', 'suspendido'].map(e => '<option value="' + e + '"' + (u.estado === e ? ' selected' : '') + '>' + e + '</option>').join('') +
      '</select></td>' +
      '<td class="text-sm muted">' + esc(u.fechaRegistro || '—') + '</td>' +
      '<td><div class="flex gap-6" style="justify-content:flex-end">' +
      '<button class="btn btn-ghost btn-xs" onclick="verDetalleUsuario(' + u.id + ')">👁 Ver</button>' +
      (u.rol !== 'admin' && u.rol !== 'estudiante' ? '<button class="btn btn-ghost btn-xs" onclick="abrirModalEditarUsuario(' + u.id + ')">✏️ Editar</button>' : '') +
      (u.rol === 'estudiante' ? '<button class="btn btn-ghost btn-xs usr-btn-inst-est" onclick="abrirInstitucionEstudiante(' + u.id + ')">🏫 Institución</button>' : '') +
      (u.estado === 'pendiente' && u.rol !== 'admin' && u.rol !== 'estudiante' ? '<button class="btn btn-ghost btn-xs" title="Genera un nuevo enlace de invitación (7 días)" onclick="reenviarInvitacion(' + u.id + ')">✉️ Reenviar invitación</button>' : '') +
      '</div></td></tr>';
  });
  if (filtrados.length === 0) html += '<tr><td colspan="6" class="empty-state">No hay usuarios que coincidan' + (activos.length > 1 || (activos.length === 1 && !q) ? ' con los filtros activos.' : '.') + '</td></tr>';
  html += '</tbody></table></div>';
  document.getElementById('usr-lista').innerHTML = html;
}

async function cambiarEstadoUsuario(id, nuevoEstado) {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const response = await fetch(API_BASE + '/api/usuarios/' + id + '/estado', {
      method: 'PATCH', headers, body: JSON.stringify({ estado: nuevoEstado })
    });
    if (manejarSesionExpirada(response)) return;
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo actualizar el estado.');
    toast('Estado actualizado a "' + nuevoEstado + '"');
    cargarUsuarios();
  } catch (err) {
    toast(err.message, true);
    cargarUsuarios(); // recarga para revertir visualmente el <select> si falló
  }
}

async function reenviarInvitacion(id) {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const response = await fetch(API_BASE + '/api/usuarios/' + id + '/reenviar-invitacion', { method: 'POST', headers });
    if (manejarSesionExpirada(response)) return;
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo reenviar la invitación.');
    toast(data.mensaje + ' (enviamos la invitación a su correo)');
  } catch (err) {
    toast(err.message, true);
  }
}

async function verDetalleUsuario(id) {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const response = await fetch(API_BASE + '/api/usuarios/' + id, { headers });
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo consultar el usuario.');
    mostrarDetalleUsuario(data);
  } catch (err) {
    toast(err.message, true);
  }
}

function fila(etiqueta, valor) {
  return '<div class="flex justify-between" style="padding:8px 0;border-bottom:1px solid var(--line)">' +
    '<span class="text-sm muted">' + etiqueta + '</span><span style="font-weight:600">' + esc(valor || '—') + '</span></div>';
}

function mostrarDetalleUsuario(u) {
  if (!document.getElementById('modal-detalle-usuario')) {
    const div = document.createElement('div');
    div.innerHTML = '<div class="modal-overlay" id="modal-detalle-usuario"><div class="modal">' +
      '<div class="modal-header"><h3>Detalle del usuario</h3><button class="modal-close" onclick="cerrarModal(\'modal-detalle-usuario\')">✕</button></div>' +
      '<div class="modal-body" id="detalle-usuario-body"></div>' +
      '<div class="modal-btns"><button class="btn btn-primary" onclick="cerrarModal(\'modal-detalle-usuario\')">Cerrar</button></div></div></div>';
    document.body.appendChild(div.firstElementChild);
  }
  const campoRol = u.rol === 'docente' ? fila('Especialidad', u.especialidad)
    : u.rol === 'contador' ? fila('Rol en contabilidad', u.rolContador)
    : u.rol === 'supervisor' ? fila('Institución que representa', u.institucion || 'Sin institución asignada (asígnala desde Editar)') + fila('Estado como representante', u.estadoRepresentacion) + fila('Cargo', u.cargo)
    : u.rol === 'estudiante' ? fila('Institución', u.institucion || 'Independiente (sin institución)') : '';
  document.getElementById('detalle-usuario-body').innerHTML =
    fila('ID', u.id) + fila('Nombre completo', u.nombre + ' ' + u.apellido) +
    fila('Nombre de usuario', u.nombreUsuario) + fila('Correo', u.correo) +
    fila('Rol', u.rol) + fila('Estado', u.estado) + campoRol +
    fila('Teléfono', u.telefono) + fila('Documento de identidad', u.docIdentidad) +
    fila('Dirección', u.direccion) + fila('Ciudad', u.ciudad) +
    fila('Registrado', u.fechaRegistro);
  abrirModal('modal-detalle-usuario');
}

async function abrirModalEditarUsuario(id) {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const response = await fetch(API_BASE + '/api/usuarios/' + id, { headers });
    const u = await response.json();
    if (!response.ok) throw new Error(u.mensaje || 'No se pudo cargar el usuario.');

    document.getElementById('edit-id').value = u.id;
    document.getElementById('edit-nombre').value = u.nombre || '';
    document.getElementById('edit-apellido').value = u.apellido || '';
    document.getElementById('edit-telefono').value = u.telefono || '';
    document.getElementById('edit-direccion').value = u.direccion || '';
    document.getElementById('edit-ciudad').value = u.ciudad || '';
    document.getElementById('edit-docidentidad').value = u.docIdentidad || '';
    document.getElementById('edit-campo-especialidad').style.display = u.rol === 'docente' ? 'block' : 'none';
    document.getElementById('edit-campo-rolcontador').style.display = u.rol === 'contador' ? 'block' : 'none';
    document.getElementById('edit-campo-cargo').style.display = u.rol === 'supervisor' ? 'block' : 'none';
    document.getElementById('edit-especialidad').value = u.especialidad || '';
    if (u.rolContador) document.getElementById('edit-rolcontador').value = u.rolContador;
    document.getElementById('edit-cargo').value = u.cargo || '';
    document.getElementById('edit-campo-institucion').style.display = u.rol === 'supervisor' ? 'block' : 'none';
    if (u.rol === 'supervisor') await cargarInstitucionesEdicion(u);
    abrirModal('modal-editar-usuario');
  } catch (err) {
    toast(err.message, true);
  }
}

function renderModalEditarUsuario() {
  if (document.getElementById('modal-editar-usuario')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-editar-usuario"><div class="modal">' +
    '<div class="modal-header"><h3>Editar usuario</h3><button class="modal-close" onclick="cerrarModal(\'modal-editar-usuario\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="edit-id">' +
    '<p class="text-sm muted" style="margin:-4px 0 14px">El correo, el nombre de usuario y el rol no se pueden cambiar desde aquí.</p>' +
    '<div class="grid-2"><div class="campo"><label for="edit-nombre">Nombre</label><input id="edit-nombre"></div>' +
    '<div class="campo"><label for="edit-apellido">Apellido</label><input id="edit-apellido"></div></div>' +
    '<div class="grid-2"><div class="campo"><label for="edit-telefono">Teléfono</label><input id="edit-telefono"></div>' +
    '<div class="campo"><label for="edit-docidentidad">Documento de identidad</label><input id="edit-docidentidad"></div></div>' +
    '<div class="grid-2"><div class="campo"><label for="edit-direccion">Dirección</label><input id="edit-direccion"></div>' +
    '<div class="campo"><label for="edit-ciudad">Ciudad</label><input id="edit-ciudad"></div></div>' +
    '<div id="edit-campo-especialidad" class="campo" style="display:none"><label for="edit-especialidad">Especialidad</label><input id="edit-especialidad"></div>' +
    '<div id="edit-campo-rolcontador" class="campo" style="display:none"><label for="edit-rolcontador">Rol dentro de Contabilidad</label><select id="edit-rolcontador">' +
    '<option value="auxiliar">Auxiliar</option><option value="administrador_financiero">Administrador financiero</option><option value="auditor">Auditor</option></select></div>' +
    '<div id="edit-campo-cargo" class="campo" style="display:none"><label for="edit-cargo">Cargo</label><input id="edit-cargo"></div>' +
    '<div id="edit-campo-institucion" class="campo" style="display:none"><label for="edit-institucion">Institución que representa</label>' +
    '<select id="edit-institucion"></select><p class="hint" id="edit-inst-hint">Solo se ofrecen instituciones activas sin otro representante activo.</p></div>' +
    '</div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-editar-usuario\')">Cancelar</button>' +
    '<button class="btn btn-primary" id="edit-modal-btn" onclick="guardarEdicionUsuario()">Guardar cambios</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function guardarEdicionUsuario() {
  const headers = authHeaders();
  if (!headers) return;
  const id = document.getElementById('edit-id').value;

  const cuerpo = {};
  const nombre = document.getElementById('edit-nombre').value.trim(); if (nombre) cuerpo.nombre = nombre;
  const apellido = document.getElementById('edit-apellido').value.trim(); if (apellido) cuerpo.apellido = apellido;
  const telefono = document.getElementById('edit-telefono').value.trim(); if (telefono) cuerpo.telefono = telefono;
  const direccion = document.getElementById('edit-direccion').value.trim(); if (direccion) cuerpo.direccion = direccion;
  const ciudad = document.getElementById('edit-ciudad').value.trim(); if (ciudad) cuerpo.ciudad = ciudad;
  const doc = document.getElementById('edit-docidentidad').value.trim(); if (doc) cuerpo.docIdentidad = doc;
  if (document.getElementById('edit-campo-especialidad').style.display !== 'none') {
    const esp = document.getElementById('edit-especialidad').value.trim(); if (esp) cuerpo.especialidad = esp;
  }
  if (document.getElementById('edit-campo-rolcontador').style.display !== 'none') {
    cuerpo.rolContador = document.getElementById('edit-rolcontador').value;
  }
  if (document.getElementById('edit-campo-cargo').style.display !== 'none') {
    const cargo = document.getElementById('edit-cargo').value.trim(); if (cargo) cuerpo.cargo = cargo;
  }

  const btn = document.getElementById('edit-modal-btn');
  const original = btn.textContent;
  btn.disabled = true; btn.textContent = 'Guardando...';
  try {
    const response = await fetch(API_BASE + '/api/usuarios/' + id, { method: 'PATCH', headers, body: JSON.stringify(cuerpo) });
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo guardar los cambios.');
    const selInst = document.getElementById('edit-institucion');
    if (document.getElementById('edit-campo-institucion').style.display !== 'none' && selInst.value && selInst.value !== selInst.dataset.original) {
      const r2 = await fetch(API_BASE + '/api/usuarios/' + id + '/institucion', { method: 'PUT', headers,
        body: JSON.stringify({ idInstitucion: Number(selInst.value), cargo: cuerpo.cargo || null }) });
      const d2 = await r2.json();
      if (!r2.ok) throw new Error('Los datos se guardaron, pero no se pudo asignar la institución: ' + (d2.mensaje || 'error ' + r2.status));
      toast('Institución asignada: ' + d2.institucion);
    } else {
      toast('Usuario actualizado correctamente');
    }
    cerrarModal('modal-editar-usuario');
    cargarUsuarios();
  } catch (err) {
    toast(err.message, true);
  } finally {
    btn.disabled = false; btn.textContent = original;
  }
}

function abrirModalUsuario() {
  document.getElementById('usr-nombre').value = '';
  document.getElementById('usr-apellido').value = '';
  document.getElementById('usr-usuario').value = '';
  document.getElementById('usr-email').value = '';
  document.getElementById('usr-telefono').value = '';
  document.getElementById('usr-docidentidad').value = '';
  document.getElementById('usr-direccion').value = '';
  document.getElementById('usr-ciudad').value = '';
  document.getElementById('usr-rol').value = 'docente';
  document.getElementById('usr-especialidad').value = '';
  document.getElementById('usr-rolcontador').value = 'auxiliar';
  document.getElementById('usr-institucion-id').innerHTML = '<option value="">Cargando instituciones...</option>';
  document.getElementById('usr-cargo').value = '';
  actualizarCamposPorRolUsuario();
  abrirModal('modal-usuario');
}

function actualizarCamposPorRolUsuario() {
  const rol = document.getElementById('usr-rol').value;
  document.getElementById('usr-campo-especialidad').style.display = rol === 'docente' ? 'block' : 'none';
  document.getElementById('usr-campo-rolcontador').style.display = rol === 'contador' ? 'block' : 'none';
  document.getElementById('usr-campo-institucion').style.display = rol === 'supervisor' ? 'block' : 'none';
  document.getElementById('usr-campo-cargo').style.display = rol === 'supervisor' ? 'block' : 'none';
  if (rol === 'supervisor') cargarOpcionesInstitucionUsuario();
}

/* Selector de institución para el Supervisor: solo instituciones activas; las que ya tienen
   representante activo aparecen deshabilitadas (el backend también lo valida). */
/* Instituciones para el representante que se edita: activas y sin OTRO representante activo, más la suya actual. */
async function cargarInstitucionesEdicion(u) {
  const sel = document.getElementById('edit-institucion');
  sel.dataset.original = u.idInstitucion ? String(u.idInstitucion) : '';
  sel.innerHTML = '<option value="">Cargando…</option>';
  try {
    const response = await fetch(API_BASE + '/api/instituciones?estado=activa', { headers: authHeaders() });
    if (manejarSesionExpirada(response)) return;
    const lista = response.ok ? await response.json() : [];
    let html = u.idInstitucion ? '' : '<option value="">-- Sin institución: selecciona una --</option>';
    if (u.idInstitucion && !lista.some(i => i.id === u.idInstitucion)) {
      html += '<option value="' + u.idInstitucion + '" selected>' + esc(u.institucion) + ' (actual, no activa)</option>';
    }
    html += lista.map(i => {
      const ocupada = i.representante && i.representante.idUsuario !== u.id;
      return '<option value="' + i.id + '"' + (ocupada ? ' disabled' : '') + (i.id === u.idInstitucion ? ' selected' : '') + '>' +
        esc(i.nombre) + ' (#' + i.id + ')' + (ocupada ? ' — ya tiene representante' : '') + '</option>';
    }).join('');
    sel.innerHTML = html;
    document.getElementById('edit-inst-hint').textContent = u.idInstitucion
      ? 'Cambiarla mueve al representante a la nueva institución. Solo se ofrecen instituciones activas sin otro representante activo.'
      : '⚠️ Este representante no tiene institución asignada. Solo se ofrecen instituciones activas sin otro representante activo.';
  } catch (err) {
    sel.innerHTML = '<option value="">No se pudieron cargar las instituciones</option>';
  }
}

async function cargarOpcionesInstitucionUsuario(idPreseleccionado) {
  const sel = document.getElementById('usr-institucion-id');
  const headers = authHeaders();
  if (!sel || !headers) return;
  try {
    const response = await fetch(API_BASE + '/api/instituciones?estado=activa', { headers });
    if (manejarSesionExpirada(response)) return;
    if (!response.ok) throw new Error('error ' + response.status);
    const lista = await response.json();
    sel.innerHTML = '<option value="">-- Selecciona la institución --</option>' + lista.map(i =>
      '<option value="' + i.id + '"' + (i.representante ? ' disabled' : '') + (String(idPreseleccionado) === String(i.id) ? ' selected' : '') + '>' +
      esc(i.nombre) + ' (#' + i.id + ')' + (i.representante ? ' — ya tiene representante' : '') + '</option>').join('');
  } catch (err) {
    sel.innerHTML = '<option value="">No se pudieron cargar las instituciones</option>';
  }
}

function renderModalUsuario() {
  if (document.getElementById('modal-usuario')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-usuario"><div class="modal">' +
    '<div class="modal-header"><h3>Crear usuario</h3><button class="modal-close" onclick="cerrarModal(\'modal-usuario\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div class="grid-2"><div class="campo"><label for="usr-nombre">Nombre</label><input id="usr-nombre"></div>' +
    '<div class="campo"><label for="usr-apellido">Apellido</label><input id="usr-apellido"></div></div>' +
    '<div class="campo"><label for="usr-usuario">Nombre de usuario</label><input id="usr-usuario" placeholder="sin espacios, ej. julianprada"></div>' +
    '<div class="campo"><label for="usr-email">Correo electrónico</label><input id="usr-email" type="email"></div>' +
    '<div class="grid-2"><div class="campo"><label for="usr-telefono">Teléfono</label><input id="usr-telefono"></div>' +
    '<div class="campo"><label for="usr-docidentidad">Documento de identidad</label><input id="usr-docidentidad"></div></div>' +
    '<div class="grid-2"><div class="campo"><label for="usr-direccion">Dirección</label><input id="usr-direccion"></div>' +
    '<div class="campo"><label for="usr-ciudad">Ciudad</label><input id="usr-ciudad"></div></div>' +
    '<div class="campo"><label for="usr-rol">Rol</label><select id="usr-rol" onchange="actualizarCamposPorRolUsuario()">' +
    '<option value="docente">Docente</option><option value="contador">Contador</option><option value="supervisor">Representante institucional</option></select></div>' +
    '<div id="usr-campo-especialidad" class="campo"><label for="usr-especialidad">Especialidad</label><input id="usr-especialidad" placeholder="Ej. Genética molecular"></div>' +
    '<div id="usr-campo-rolcontador" class="campo" style="display:none"><label for="usr-rolcontador">Rol dentro de Contabilidad</label><select id="usr-rolcontador">' +
    '<option value="auxiliar">Auxiliar</option><option value="administrador_financiero">Administrador financiero</option><option value="auditor">Auditor</option></select></div>' +
    '<div id="usr-campo-institucion" class="campo" style="display:none"><label for="usr-institucion-id">Institución que representará</label><select id="usr-institucion-id"></select></div>' +
    '<div id="usr-campo-cargo" class="campo" style="display:none"><label for="usr-cargo">Cargo</label><input id="usr-cargo" placeholder="Ej. Coordinador académico"></div>' +
    '</div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-usuario\')">Cancelar</button>' +
    '<button class="btn btn-primary" id="usr-modal-btn" onclick="guardarUsuario()">Crear y enviar invitación</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function guardarUsuario() {
  const headers = authHeaders();
  if (!headers) return;

  const nombre = document.getElementById('usr-nombre').value.trim();
  const apellido = document.getElementById('usr-apellido').value.trim();
  const nombreUsuario = document.getElementById('usr-usuario').value.trim();
  const correo = document.getElementById('usr-email').value.trim();
  const rol = document.getElementById('usr-rol').value;
  if (!nombre || !apellido || !nombreUsuario || !correo) { toast('Completa nombre, apellido, usuario y correo', true); return; }

  const cuerpo = { nombre, apellido, nombreUsuario, correo, rol: rol.toUpperCase(),
    telefono: document.getElementById('usr-telefono').value.trim() || null,
    docIdentidad: document.getElementById('usr-docidentidad').value.trim() || null,
    direccion: document.getElementById('usr-direccion').value.trim() || null,
    ciudad: document.getElementById('usr-ciudad').value.trim() || null };
  if (rol === 'docente') cuerpo.especialidad = document.getElementById('usr-especialidad').value.trim();
  if (rol === 'contador') cuerpo.rolContador = document.getElementById('usr-rolcontador').value;
  if (rol === 'supervisor') {
    const idInst = document.getElementById('usr-institucion-id').value;
    if (!idInst) { toast('Selecciona la institución que representará', true); return; }
    cuerpo.idInstitucion = Number(idInst);
    cuerpo.cargo = document.getElementById('usr-cargo').value.trim();
  }

  const btn = document.getElementById('usr-modal-btn');
  const original = btn.textContent;
  btn.disabled = true; btn.textContent = 'Creando...';
  try {
    const response = await fetch(API_BASE + '/api/usuarios', { method: 'POST', headers, body: JSON.stringify(cuerpo) });
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo crear el usuario.');
    toast('Usuario ' + nombre + ' creado — le enviamos la invitación a su correo');
    cerrarModal('modal-usuario');
    cargarUsuarios();
  } catch (err) {
    toast(err.message, true);
  } finally {
    btn.disabled = false; btn.textContent = original;
  }
}

/* ================================================================
   Institución de un estudiante (casos especiales: perdió el código, error de vinculación).
   PUT /api/usuarios/{id}/institucion-estudiante con idInstitucion o null (independiente).
   El estudiante conserva sus cursos, su progreso, sus pagos y sus certificados.
================================================================ */
async function abrirInstitucionEstudiante(id) {
  const headers = authHeaders();
  if (!headers) return;
  renderModalInstitucionEstudiante();
  try {
    const [r1, r2] = await Promise.all([fetch(API_BASE + '/api/usuarios/' + id, { headers }), fetch(API_BASE + '/api/instituciones?estado=activa', { headers })]);
    if (manejarSesionExpirada(r1) || manejarSesionExpirada(r2)) return;
    const u = await r1.json();
    if (!r1.ok) throw new Error(u.mensaje || 'No se pudo cargar el estudiante.');
    const lista = r2.ok ? await r2.json() : [];
    document.getElementById('ie-id').value = id;
    document.getElementById('ie-nombre').textContent = (u.nombre || '') + ' ' + (u.apellido || '') + ' · ' + (u.institucion ? 'actualmente en ' + u.institucion : 'actualmente independiente');
    const sel = document.getElementById('ie-institucion');
    sel.dataset.original = u.idInstitucion ? String(u.idInstitucion) : '';
    let html = '<option value=""' + (u.idInstitucion ? '' : ' selected') + '>Independiente (sin institución)</option>';
    if (u.idInstitucion && !lista.some(i => i.id === u.idInstitucion)) html += '<option value="' + u.idInstitucion + '" selected>' + esc(u.institucion) + ' (actual, no activa)</option>';
    html += lista.map(i => '<option value="' + i.id + '"' + (i.id === u.idInstitucion ? ' selected' : '') + '>' + esc(i.nombre) + ' (#' + i.id + ')</option>').join('');
    sel.innerHTML = html;
    abrirModal('modal-inst-estudiante');
  } catch (err) { toast(err.message, true); }
}

async function guardarInstitucionEstudiante() {
  const headers = authHeaders();
  if (!headers) return;
  const id = document.getElementById('ie-id').value;
  const sel = document.getElementById('ie-institucion');
  if (sel.value === sel.dataset.original) { toast('No hay cambios', true); return; }
  const btn = document.getElementById('ie-btn'); btn.disabled = true;
  try {
    const r = await fetch(API_BASE + '/api/usuarios/' + id + '/institucion-estudiante', { method: 'PUT', headers,
      body: JSON.stringify({ idInstitucion: sel.value ? Number(sel.value) : null }) });
    const d = await r.json();
    if (manejarSesionExpirada(r)) return;
    if (!r.ok) throw new Error(d.mensaje || 'No se pudo cambiar la institución.');
    cerrarModal('modal-inst-estudiante');
    toast(d.institucion ? 'El estudiante ahora pertenece a ' + d.institucion : 'El estudiante quedó como independiente');
    cargarUsuarios();
  } catch (err) { toast(err.message, true); }
  btn.disabled = false;
}

function renderModalInstitucionEstudiante() {
  if (document.getElementById('modal-inst-estudiante')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-inst-estudiante"><div class="modal" style="max-width:480px">' +
    '<div class="modal-header"><h3>Institución del estudiante</h3><button class="modal-close" onclick="cerrarModal(\'modal-inst-estudiante\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="ie-id"><p style="font-weight:700;margin:0 0 12px" id="ie-nombre"></p>' +
    '<div class="campo"><label for="ie-institucion">Institución</label><select id="ie-institucion"></select>' +
    '<p class="hint">Para casos especiales (perdió el código, error de vinculación). El estudiante también puede unirse, cambiar o salir desde su perfil con el código de afiliación. Conserva sus cursos, su progreso, sus pagos y sus certificados; la nueva institución debe tener cupos.</p></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-inst-estudiante\')">Cancelar</button>' +
    '<button class="btn btn-primary" id="ie-btn" onclick="guardarInstitucionEstudiante()">Guardar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

