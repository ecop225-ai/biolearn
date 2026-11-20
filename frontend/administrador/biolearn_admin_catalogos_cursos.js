/* ================================================================
   BIOLearn — biolearn_admin_catalogos_cursos.js
   CATÁLOGOS: conectado al backend real (/api/catalogos/admin/**). Listado en tabla con pestañas
   (Grados / Áreas académicas), filtros, Ver y Editar, igual que Gestión de usuarios.
   No se borran: se inactivan, porque los usan los perfiles de estudiante y, más adelante, los cursos.
   CURSOS: está en biolearn_admin_cursos.js, conectado al backend real.
================================================================ */
let CAT_GRADOS = [];
let CAT_AREAS = [];
let CAT_TAB = 'grados';          // grados | areas
let CAT_BUSQUEDA = '';
let CAT_FILTRO_ESTADO = 'todos'; // todos | activo | inactivo
let CAT_FILTRO_NIVEL = 'todos';  // solo grados: todos | primaria | secundaria | media
let CAT_FILTRO_USO = 'todos';    // todos | con | sin (estudiantes asociados)

function renderCatalogos(cont) {
  cont.innerHTML = '<div class="section-header"><div><h2>Catálogos académicos</h2>' +
    '<p class="subtitle">Grados y áreas académicas que se ofrecen en el registro de estudiantes y para clasificar cursos</p></div>' +
    '<button class="btn btn-primary" id="cat-btn-agregar" onclick="abrirModalCatalogo(CAT_TAB === \'grados\' ? \'grado\' : \'area\')"></button></div>' +
    '<div class="flex gap-8 mb-10">' +
    '<button id="cat-tab-grados" class="btn btn-sm" onclick="cambiarTabCatalogo(\'grados\')">🎓 Grados</button>' +
    '<button id="cat-tab-areas" class="btn btn-sm" onclick="cambiarTabCatalogo(\'areas\')">🧬 Áreas académicas</button></div>' +
    '<div class="flex gap-10 flex-wrap mb-10">' +
    '<input id="cat-buscar" placeholder="Buscar por nombre o descripción..." oninput="CAT_BUSQUEDA=this.value;pintarCatalogos()" style="width:300px">' +
    '<select id="cat-f-estado" onchange="CAT_FILTRO_ESTADO=this.value;pintarCatalogos()" style="width:180px">' +
    '<option value="todos">Todos los estados</option><option value="activo">Activos</option><option value="inactivo">Inactivos</option></select>' +
    '<select id="cat-f-nivel" onchange="CAT_FILTRO_NIVEL=this.value;pintarCatalogos()" style="width:180px">' +
    '<option value="todos">Todos los niveles</option><option value="primaria">Primaria</option><option value="secundaria">Secundaria</option><option value="media">Media</option></select>' +
    '<select id="cat-f-uso" onchange="CAT_FILTRO_USO=this.value;pintarCatalogos()" style="width:220px">' +
    '<option value="todos">Con o sin estudiantes</option><option value="con">Con estudiantes asociados</option><option value="sin">Sin estudiantes asociados</option></select>' +
    '</div>' +
    '<div id="cat-resumen" class="flex items-center gap-10 text-sm muted" style="margin-bottom:10px"></div>' +
    '<div id="cat-lista">Cargando...</div>' +
    '<p class="text-sm muted" style="margin-top:12px">Un grado o área inactivo deja de ofrecerse en el registro, pero los estudiantes que ya lo tienen lo conservan.</p>';
  renderModalCatalogo();
  renderModalDetalleCatalogo();
  cambiarTabCatalogo(CAT_TAB, true);
  cargarCatalogos();
}

function cambiarTabCatalogo(tab, sinPintar) {
  CAT_TAB = tab;
  document.getElementById('cat-tab-grados').className = 'btn btn-sm ' + (tab === 'grados' ? 'btn-primary' : 'btn-ghost');
  document.getElementById('cat-tab-areas').className = 'btn btn-sm ' + (tab === 'areas' ? 'btn-primary' : 'btn-ghost');
  document.getElementById('cat-btn-agregar').textContent = tab === 'grados' ? '+ Agregar grado' : '+ Agregar área';
  document.getElementById('cat-f-nivel').style.display = tab === 'grados' ? '' : 'none';
  if (tab !== 'grados') { CAT_FILTRO_NIVEL = 'todos'; document.getElementById('cat-f-nivel').value = 'todos'; }
  if (!sinPintar) pintarCatalogos();
}

async function cargarCatalogos() {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const [rg, ra] = await Promise.all([
      fetch(API_BASE + '/api/catalogos/admin/grados', { headers }),
      fetch(API_BASE + '/api/catalogos/admin/areas', { headers })
    ]);
    if (manejarSesionExpirada(rg) || manejarSesionExpirada(ra)) return;
    if (rg.status === 403 || ra.status === 403) { toast('Tu usuario no tiene permisos de Administrador', true); return; }
    if (!rg.ok || !ra.ok) throw new Error('El servidor respondió con error ' + (rg.ok ? ra.status : rg.status));
    CAT_GRADOS = await rg.json();
    CAT_AREAS = await ra.json();
    pintarCatalogos();
  } catch (err) {
    document.getElementById('cat-lista').innerHTML = '<div class="empty-state">No se pudo cargar. ¿Está corriendo el backend? (' + esc(err.message) + ')</div>';
  }
}

function normalizarTextoCat(t) {
  return String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

function limpiarFiltrosCatalogos() {
  CAT_BUSQUEDA = ''; CAT_FILTRO_ESTADO = 'todos'; CAT_FILTRO_NIVEL = 'todos'; CAT_FILTRO_USO = 'todos';
  ['cat-buscar', 'cat-f-estado', 'cat-f-nivel', 'cat-f-uso'].forEach(id => {
    const el = document.getElementById(id); el.value = id === 'cat-buscar' ? '' : 'todos';
  });
  pintarCatalogos();
}

function pintarCatalogos() {
  const esGrado = CAT_TAB === 'grados';
  const fuente = esGrado ? CAT_GRADOS : CAT_AREAS;
  const q = normalizarTextoCat(CAT_BUSQUEDA.trim());
  const lista = fuente.filter(x =>
    (!q || normalizarTextoCat(x.nombre + ' ' + (x.descripcion || '')).includes(q)) &&
    (CAT_FILTRO_ESTADO === 'todos' || x.estado === CAT_FILTRO_ESTADO) &&
    (!esGrado || CAT_FILTRO_NIVEL === 'todos' || x.nivel === CAT_FILTRO_NIVEL) &&
    (CAT_FILTRO_USO === 'todos' || (CAT_FILTRO_USO === 'con' ? x.estudiantesAsociados > 0 : !x.estudiantesAsociados)));

  const activos = [];
  if (CAT_FILTRO_ESTADO !== 'todos') activos.push('Estado: ' + CAT_FILTRO_ESTADO);
  if (esGrado && CAT_FILTRO_NIVEL !== 'todos') activos.push('Nivel: ' + CAT_FILTRO_NIVEL);
  if (CAT_FILTRO_USO !== 'todos') activos.push(CAT_FILTRO_USO === 'con' ? 'Con estudiantes' : 'Sin estudiantes');
  if (q) activos.push('Búsqueda: "' + CAT_BUSQUEDA.trim() + '"');
  const nombreTipo = esGrado ? (lista.length === 1 ? 'grado' : 'grados') : (lista.length === 1 ? 'área académica' : 'áreas académicas');
  document.getElementById('cat-resumen').innerHTML = '<span>Mostrando <strong>' + lista.length + '</strong> ' + nombreTipo +
    (activos.length ? ' · Filtros activos: ' + esc(activos.join(' · ')) : '') + '</span>' +
    (activos.length ? '<button class="btn btn-ghost btn-xs" onclick="limpiarFiltrosCatalogos()">✕ Limpiar filtros</button>' : '');

  const cols = esGrado ? ['Orden', 'Grado', 'Nivel', 'Estudiantes', 'Estado', ''] : ['Área académica', 'Descripción', 'Estudiantes', 'Estado', ''];
  const tipo = esGrado ? 'grado' : 'area';
  let html = '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
    cols.map(h => '<th style="color:var(--blue)">' + h + '</th>').join('') + '</tr></thead><tbody>';
  lista.forEach(x => {
    const estado = '<select onchange="cambiarEstadoCatalogo(\'' + tipo + '\',' + x.id + ',this.value)" style="width:auto;padding:4px 8px;font-size:12.5px">' +
      ['activo', 'inactivo'].map(e => '<option value="' + e + '"' + (x.estado === e ? ' selected' : '') + '>' + e + '</option>').join('') + '</select>';
    const acciones = '<td style="white-space:nowrap;text-align:right"><button class="btn btn-ghost btn-xs" onclick="verDetalleCatalogo(\'' + tipo + '\',' + x.id + ')">👁 Ver</button> ' +
      '<button class="btn btn-ghost btn-xs" onclick="abrirModalCatalogo(\'' + tipo + '\',' + x.id + ')">✏️ Editar</button></td>';
    const estilo = x.estado === 'inactivo' ? ' style="opacity:.6"' : '';
    html += esGrado
      ? '<tr' + estilo + '><td>' + x.orden + '</td><td style="font-weight:600">' + esc(x.nombre) + '</td><td>' + esc(x.nivel) + '</td><td>' + (x.estudiantesAsociados || 0) + '</td><td>' + estado + '</td>' + acciones + '</tr>'
      : '<tr' + estilo + '><td style="font-weight:600">' + esc(x.nombre) + '</td><td class="text-sm muted">' + esc(x.descripcion || '—') + '</td><td>' + (x.estudiantesAsociados || 0) + '</td><td>' + estado + '</td>' + acciones + '</tr>';
  });
  if (!lista.length) html += '<tr><td colspan="' + cols.length + '" class="empty-state">No hay registros que coincidan.</td></tr>';
  document.getElementById('cat-lista').innerHTML = html + '</tbody></table></div>';
}

/* ---------- Ver detalle: GET /api/catalogos/admin/grados/{id} o /areas/{id} ---------- */
function renderModalDetalleCatalogo() {
  if (document.getElementById('modal-detalle-catalogo')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-detalle-catalogo"><div class="modal" style="max-width:480px"><div class="modal-header"><h3 id="cat-detalle-titulo">Detalle</h3>' +
    '<button class="modal-close" onclick="cerrarModal(\'modal-detalle-catalogo\')">✕</button></div>' +
    '<div class="modal-body" id="cat-detalle-body">Cargando...</div><div class="modal-btns" id="cat-detalle-btns"></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function verDetalleCatalogo(tipo, id) {
  const headers = authHeaders();
  if (!headers) return;
  const esGrado = tipo === 'grado';
  document.getElementById('cat-detalle-titulo').textContent = esGrado ? 'Detalle del grado' : 'Detalle del área académica';
  document.getElementById('cat-detalle-body').innerHTML = 'Cargando...';
  document.getElementById('cat-detalle-btns').innerHTML = '';
  abrirModal('modal-detalle-catalogo');
  try {
    const response = await fetch(API_BASE + '/api/catalogos/admin/' + (esGrado ? 'grados/' : 'areas/') + id, { headers });
    if (manejarSesionExpirada(response)) return;
    const x = await response.json();
    if (!response.ok) throw new Error(x.mensaje || 'No se pudo cargar el registro.');
    const fila = (k, v) => '<div class="flex justify-between" style="padding:8px 0;border-bottom:1px solid var(--line, #eee)"><span class="muted">' + k + '</span><span style="font-weight:600;text-align:right">' + v + '</span></div>';
    document.getElementById('cat-detalle-body').innerHTML =
      fila('ID', x.id) + fila('Nombre', esc(x.nombre)) +
      (esGrado ? fila('Nivel', esc(x.nivel)) + fila('Orden en la lista', x.orden) : '') +
      fila('Descripción', esc(x.descripcion || '—')) + fila('Estado', esc(x.estado)) +
      fila('Estudiantes asociados', x.estudiantesAsociados) +
      (x.estado === 'inactivo' ? '<p class="text-sm muted" style="margin-top:10px">Inactivo: no se ofrece en el registro de estudiantes.</p>' : '');
    document.getElementById('cat-detalle-btns').innerHTML =
      '<button class="btn btn-ghost" onclick="cerrarModal(\'modal-detalle-catalogo\')">Cerrar</button>' +
      '<button class="btn btn-primary" onclick="cerrarModal(\'modal-detalle-catalogo\');abrirModalCatalogo(\'' + tipo + '\',' + x.id + ')">✏️ Editar</button>';
  } catch (err) {
    document.getElementById('cat-detalle-body').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>';
  }
}

function abrirModalCatalogo(tipo, id) {
  const lista = tipo === 'grado' ? CAT_GRADOS : CAT_AREAS;
  const item = id ? lista.find(x => x.id === id) : null;
  const nombreTipo = tipo === 'grado' ? 'grado' : 'área académica';
  document.getElementById('cat-modal-titulo').textContent = (item ? 'Editar ' : 'Agregar ') + nombreTipo;
  document.getElementById('cat-tipo').value = tipo;
  document.getElementById('cat-id').value = item ? item.id : '';
  document.getElementById('cat-nombre').value = item ? item.nombre : '';
  document.getElementById('cat-descripcion').value = item ? (item.descripcion || '') : '';
  document.getElementById('cat-campos-grado').style.display = tipo === 'grado' ? '' : 'none';
  if (tipo === 'grado') {
    document.getElementById('cat-nivel').value = item ? item.nivel : 'secundaria';
    document.getElementById('cat-orden').value = item ? item.orden : (CAT_GRADOS.reduce((m, x) => Math.max(m, x.orden), 0) + 1);
  }
  abrirModal('modal-catalogo');
}

function renderModalCatalogo() {
  if (document.getElementById('modal-catalogo')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-catalogo"><div class="modal" style="max-width:460px"><div class="modal-header"><h3 id="cat-modal-titulo">Agregar</h3><button class="modal-close" onclick="cerrarModal(\'modal-catalogo\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="cat-tipo"><input type="hidden" id="cat-id">' +
    '<div class="campo"><label for="cat-nombre">Nombre *</label><input id="cat-nombre" maxlength="100"></div>' +
    '<div id="cat-campos-grado" class="grid-2"><div class="campo"><label for="cat-nivel">Nivel *</label><select id="cat-nivel">' +
    '<option value="primaria">Primaria</option><option value="secundaria">Secundaria</option><option value="media">Media</option></select></div>' +
    '<div class="campo"><label for="cat-orden">Orden en la lista *</label><input id="cat-orden" type="number" min="1" max="255"></div></div>' +
    '<div class="campo"><label for="cat-descripcion">Descripción</label><input id="cat-descripcion"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-catalogo\')">Cancelar</button><button class="btn btn-primary" id="cat-modal-btn" onclick="guardarCatalogo()">Guardar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function guardarCatalogo() {
  const headers = authHeaders();
  if (!headers) return;
  const tipo = document.getElementById('cat-tipo').value, id = document.getElementById('cat-id').value;
  const cuerpo = { nombre: document.getElementById('cat-nombre').value.trim(), descripcion: document.getElementById('cat-descripcion').value.trim() };
  if (tipo === 'grado') { cuerpo.nivel = document.getElementById('cat-nivel').value; cuerpo.orden = Number(document.getElementById('cat-orden').value) || null; }
  if (!cuerpo.nombre) { toast('Escribe el nombre', true); return; }
  const ruta = '/api/catalogos/admin/' + (tipo === 'grado' ? 'grados' : 'areas') + (id ? '/' + id : '');
  try {
    const response = await fetch(API_BASE + ruta, { method: id ? 'PATCH' : 'POST', headers, body: JSON.stringify(cuerpo) });
    if (manejarSesionExpirada(response)) return;
    const data = await response.json();
    if (!response.ok) throw new Error((data.detalles && data.detalles.length ? data.detalles.join(' · ') : data.mensaje) || 'No se pudo guardar.');
    cerrarModal('modal-catalogo');
    // Concordancia de género: "Grado ... creado/actualizado", "Área ... creada/actualizada"
    const esGrado = tipo === 'grado';
    toast((esGrado ? 'Grado' : 'Área') + ' "' + data.nombre + '" ' + (id ? (esGrado ? 'actualizado' : 'actualizada') : (esGrado ? 'creado' : 'creada')));
    cargarCatalogos();
  } catch (err) { toast(err.message, true); }
}

async function cambiarEstadoCatalogo(tipo, id, estado) {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const response = await fetch(API_BASE + '/api/catalogos/admin/' + (tipo === 'grado' ? 'grados' : 'areas') + '/' + id + '/estado', {
      method: 'PATCH', headers, body: JSON.stringify({ estado })
    });
    if (manejarSesionExpirada(response)) return;
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo cambiar el estado.');
    // El estado del backend es activo/inactivo; para las áreas se muestra en femenino (activa/inactiva)
    const estadoTexto = tipo === 'grado' ? data.estado : (data.estado === 'activo' ? 'activa' : 'inactiva');
    toast('"' + data.nombre + '" quedó ' + estadoTexto);
    cargarCatalogos();
  } catch (err) { toast(err.message, true); cargarCatalogos(); }
}

/* CURSOS: se movió a biolearn_admin_cursos.js (conectado al backend real, Fase 1 del módulo de Cursos). */
