/* ================================================================
   BIOLearn — biolearn_admin_cursos.js
   CURSOS (Fase 1): conectado al backend real (/api/cursos/**, exclusivo del Administrador).
   Tabla con filtros, Ver (detalle y gestión de docentes, horarios e instituciones) y Editar.
   - Un curso nace en borrador; solo el Administrador lo publica.
   - Estados: borrador → activo ⇄ cerrado; cualquiera → archivado; archivado → borrador.
   - "Eliminar" = archivar (se conserva el historial).
   Los ids de docente son ids de USUARIO (los de la Gestión de usuarios).
================================================================ */
let CUR_CACHE = [];
let CUR_AREAS = [];
let CUR_GRADOS = [];
let CUR_DOCENTES = [];
let CUR_INSTITUCIONES = [];
let CUR_BUSQUEDA = '';
let CUR_FILTRO_ESTADO = 'todos';
let CUR_FILTRO_AREA = 'todas';
let CUR_FILTRO_GRADO = 'todos';
let CUR_DETALLE_ID = null;

const CUR_TRANSICIONES = {
  borrador: ['activo', 'archivado'],
  activo: ['cerrado', 'archivado'],
  cerrado: ['activo', 'archivado'],
  archivado: ['borrador']
};
const CUR_ACCION = { activo: 'Publicar', cerrado: 'Cerrar', archivado: 'Archivar', borrador: 'Restaurar a borrador' };
const CUR_DIAS = [['lunes', 'Lunes'], ['martes', 'Martes'], ['miercoles', 'Miércoles'], ['jueves', 'Jueves'], ['viernes', 'Viernes'], ['sabado', 'Sábado'], ['domingo', 'Domingo']];

function renderCursos(cont) {
  cont.innerHTML = '<div class="section-header"><div><h2>Cursos</h2>' +
    '<p class="subtitle">Catálogo de cursos de la plataforma: datos, estado, docentes, horarios e instituciones — conectado a tu backend real</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalCurso()">+ Crear curso</button></div>' +
    '<div class="flex gap-10 flex-wrap mb-10">' +
    '<input id="cur-buscar" placeholder="Buscar por nombre, descripción o docente..." oninput="CUR_BUSQUEDA=this.value;pintarCursos()" style="width:300px">' +
    '<select id="cur-f-estado" onchange="CUR_FILTRO_ESTADO=this.value;pintarCursos()" style="width:170px">' +
    '<option value="todos">Todos los estados</option><option value="borrador">Borrador</option><option value="activo">Activos</option>' +
    '<option value="cerrado">Cerrados</option><option value="archivado">Archivados</option></select>' +
    '<select id="cur-f-area" onchange="CUR_FILTRO_AREA=this.value;pintarCursos()" style="width:200px"><option value="todas">Todas las áreas</option></select>' +
    '<select id="cur-f-grado" onchange="CUR_FILTRO_GRADO=this.value;pintarCursos()" style="width:170px"><option value="todos">Todos los grados</option></select>' +
    '</div>' +
    '<div id="cur-resumen" class="flex items-center gap-10 text-sm muted" style="margin-bottom:10px"></div>' +
    '<div id="cur-lista">Cargando cursos...</div>';
  renderModalCurso();
  renderModalDetalleCurso();
  cargarCursos();
}

async function cargarCursos() {
  const headers = authHeaders();
  if (!headers) return;
  try {
    const [rc, ra, rg, rd, ri] = await Promise.all([
      fetch(API_BASE + '/api/cursos', { headers }),
      fetch(API_BASE + '/api/catalogos/admin/areas', { headers }),
      fetch(API_BASE + '/api/catalogos/admin/grados', { headers }),
      fetch(API_BASE + '/api/usuarios?rol=docente&estado=activo', { headers }),
      fetch(API_BASE + '/api/instituciones?estado=activa', { headers })
    ]);
    if ([rc, ra, rg, rd, ri].some(r => manejarSesionExpirada(r))) return;
    if (rc.status === 403) { toast('Tu usuario no tiene permisos de Administrador', true); return; }
    if (!rc.ok) throw new Error('El servidor respondió con error ' + rc.status);
    CUR_CACHE = await rc.json();
    CUR_AREAS = ra.ok ? await ra.json() : [];
    CUR_GRADOS = rg.ok ? await rg.json() : [];
    CUR_DOCENTES = rd.ok ? await rd.json() : [];
    CUR_INSTITUCIONES = ri.ok ? await ri.json() : [];
    llenarFiltrosCursos();
    pintarCursos();
  } catch (err) {
    document.getElementById('cur-lista').innerHTML = '<div class="empty-state">No se pudo conectar con el servidor (' + esc(API_BASE) + '). ¿Está encendido? (' + esc(err.message) + ')</div>';
  }
}

function llenarFiltrosCursos() {
  const fa = document.getElementById('cur-f-area'), fg = document.getElementById('cur-f-grado');
  fa.innerHTML = '<option value="todas">Todas las áreas</option>' + CUR_AREAS.map(a => '<option value="' + a.id + '"' + (String(a.id) === CUR_FILTRO_AREA ? ' selected' : '') + '>' + esc(a.nombre) + '</option>').join('');
  fg.innerHTML = '<option value="todos">Todos los grados</option>' + CUR_GRADOS.map(g => '<option value="' + g.id + '"' + (String(g.id) === CUR_FILTRO_GRADO ? ' selected' : '') + '>' + esc(g.nombre) + '</option>').join('');
}

function normalizarTextoCur(t) {
  return String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
}

function precioCOP(v) {
  const n = Number(v || 0);
  return n === 0 ? 'Gratuito' : '$' + n.toLocaleString('es-CO', { maximumFractionDigits: 0 });
}

function pillEstadoCurso(e) {
  const clase = { borrador: 'pill-ambar', activo: 'pill-verde', cerrado: 'pill-azul', archivado: 'pill-gris' }[e] || 'pill-gris';
  return '<span class="pill ' + clase + '">' + esc(e) + '</span>';
}

function limpiarFiltrosCursos() {
  CUR_BUSQUEDA = ''; CUR_FILTRO_ESTADO = 'todos'; CUR_FILTRO_AREA = 'todas'; CUR_FILTRO_GRADO = 'todos';
  document.getElementById('cur-buscar').value = '';
  document.getElementById('cur-f-estado').value = 'todos';
  document.getElementById('cur-f-area').value = 'todas';
  document.getElementById('cur-f-grado').value = 'todos';
  pintarCursos();
}

function pintarCursos() {
  const q = normalizarTextoCur(CUR_BUSQUEDA.trim());
  const lista = CUR_CACHE.filter(c =>
    (!q || normalizarTextoCur([c.nombre, c.descripcion, c.docenteTitular].join(' ')).includes(q)) &&
    (CUR_FILTRO_ESTADO === 'todos' || c.estado === CUR_FILTRO_ESTADO) &&
    (CUR_FILTRO_AREA === 'todas' || String(c.idArea) === CUR_FILTRO_AREA) &&
    (CUR_FILTRO_GRADO === 'todos' || String(c.idGrado) === CUR_FILTRO_GRADO));

  const activos = [];
  if (CUR_FILTRO_ESTADO !== 'todos') activos.push('Estado: ' + CUR_FILTRO_ESTADO);
  if (CUR_FILTRO_AREA !== 'todas') { const a = CUR_AREAS.find(x => String(x.id) === CUR_FILTRO_AREA); activos.push('Área: ' + (a ? a.nombre : CUR_FILTRO_AREA)); }
  if (CUR_FILTRO_GRADO !== 'todos') { const g = CUR_GRADOS.find(x => String(x.id) === CUR_FILTRO_GRADO); activos.push('Grado: ' + (g ? g.nombre : CUR_FILTRO_GRADO)); }
  if (q) activos.push('Búsqueda: "' + CUR_BUSQUEDA.trim() + '"');
  document.getElementById('cur-resumen').innerHTML = '<span>Mostrando <strong>' + lista.length + '</strong> ' + (lista.length === 1 ? 'curso' : 'cursos') +
    (activos.length ? ' · Filtros activos: ' + esc(activos.join(' · ')) : '') + '</span>' +
    (activos.length ? '<button class="btn btn-ghost btn-xs" onclick="limpiarFiltrosCursos()">✕ Limpiar filtros</button>' : '');

  let html = '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
    ['Curso', 'Docente titular', 'Instituciones', 'Horario', 'Precio', 'Estado', ''].map(h => '<th style="color:var(--blue)">' + h + '</th>').join('') +
    '</tr></thead><tbody>';
  lista.forEach(c => {
    const opciones = [c.estado].concat(CUR_TRANSICIONES[c.estado] || []);
    const selector = '<select onchange="cambiarEstadoCurso(' + c.id + ', this.value, this)" style="width:auto;padding:4px 8px;font-size:12.5px" title="Cambiar estado">' +
      opciones.map(e => '<option value="' + e + '"' + (e === c.estado ? ' selected' : '') + '>' + (e === c.estado ? e : '→ ' + CUR_ACCION[e]) + '</option>').join('') + '</select>';
    const estilo = c.estado === 'archivado' ? ' style="opacity:.6"' : '';
    html += '<tr' + estilo + '><td><div style="font-weight:600">' + esc(c.nombre) + '</div><div class="text-sm muted">' + esc(c.area) + ' · ' + esc(c.grado) + '</div></td>' +
      '<td class="text-sm">' + (c.docenteTitular ? esc(c.docenteTitular) : '<span class="muted">Sin asignar</span>') + '</td>' +
      '<td class="text-sm">' + c.instituciones.length + '</td>' +
      '<td class="text-sm">' + (c.horarios.length ? c.horarios.length + (c.horarios.length === 1 ? ' sesión' : ' sesiones') + ' por semana' : '<span class="muted">Sin horario</span>') + '</td>' +
      '<td class="text-sm">' + precioCOP(c.precio) + (c.soloInstitucional ? '<div class="text-sm" style="color:var(--blue);font-weight:600" title="Solo para colegios">🏫 Solo colegios</div>' : '') + '</td>' +
      '<td>' + selector + '</td>' +
      '<td style="white-space:nowrap;text-align:right"><button class="btn btn-ghost btn-xs" onclick="verDetalleCurso(' + c.id + ')">👁 Ver</button> ' +
      (c.estado !== 'archivado' ? '<button class="btn btn-ghost btn-xs" onclick="abrirModalCurso(' + c.id + ')">✏️ Editar</button>' : '') + '</td></tr>';
  });
  if (!lista.length) html += '<tr><td colspan="7" class="empty-state">No hay cursos que coincidan.</td></tr>';
  document.getElementById('cur-lista').innerHTML = html + '</tbody></table></div>';
}

/* ---------- Petición común: devuelve el JSON o lanza el mensaje del backend ---------- */
async function pedirCurso(metodo, ruta, cuerpo) {
  const headers = authHeaders();
  if (!headers) throw new Error('Sesión no válida');
  const opciones = { method: metodo, headers: Object.assign({ 'Content-Type': 'application/json' }, headers) };
  if (cuerpo !== undefined) opciones.body = JSON.stringify(cuerpo);
  const response = await fetch(API_BASE + ruta, opciones);
  if (manejarSesionExpirada(response)) throw new Error('Sesión expirada');
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const detalle = data.detalles && data.detalles.length ? ' (' + data.detalles.join('; ') + ')' : '';
    throw new Error((data.mensaje || 'Error ' + response.status) + detalle);
  }
  return data;
}

function actualizarEnCache(curso) {
  const i = CUR_CACHE.findIndex(c => c.id === curso.id);
  if (i >= 0) CUR_CACHE[i] = curso; else CUR_CACHE.push(curso);
  CUR_CACHE.sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
}

/* ---------- Cambio de estado ---------- */
async function cambiarEstadoCurso(id, estado, selector) {
  const curso = CUR_CACHE.find(c => c.id === id);
  if (!curso || estado === curso.estado) return;
  const ejecutar = async () => {
    try {
      const data = await pedirCurso('PATCH', '/api/cursos/' + id + '/estado', { estado });
      actualizarEnCache(data);
      toast('"' + data.nombre + '" ' + { activo: 'quedó publicado', cerrado: 'quedó cerrado', archivado: 'quedó archivado', borrador: 'volvió a borrador' }[estado]);
      pintarCursos();
      if (CUR_DETALLE_ID === id) pintarDetalleCurso(data);
    } catch (err) {
      toast(err.message, true);
      pintarCursos();
    }
  };
  if (estado === 'archivado') {
    confirmarAccion('¿Archivar el curso <strong>' + esc(curso.nombre) + '</strong>? Dejará de ofrecerse en el catálogo y no admitirá contrataciones. Podrás restaurarlo a borrador.', ejecutar, 'Archivar');
    if (selector) selector.value = curso.estado;
  } else {
    ejecutar();
  }
}

/* ---------- Crear y editar ---------- */
function renderModalCurso() {
  if (document.getElementById('modal-curso')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-curso"><div class="modal" style="max-width:640px"><div class="modal-header"><h3 id="cur-modal-titulo">Crear curso</h3>' +
    '<button class="modal-close" onclick="cerrarModal(\'modal-curso\')">✕</button></div><div class="modal-body">' +
    '<div class="campo"><label for="cur-nombre">Nombre del curso *</label><input type="text" id="cur-nombre" maxlength="150"></div>' +
    '<div class="grid-2"><div class="campo"><label for="cur-area">Área académica *</label><select id="cur-area"></select></div>' +
    '<div class="campo"><label for="cur-grado">Grado *</label><select id="cur-grado"></select></div></div>' +
    '<div class="campo"><label for="cur-docente">Docente titular</label><select id="cur-docente"></select><p class="hint">Requerido para publicar. Solo aparecen docentes con la cuenta activa. Los colaboradores e invitados se agregan desde 👁 Ver, en la sección Docentes.</p></div>' +
    '<div class="grid-2"><div class="campo"><label for="cur-precio">Precio (COP)</label><input type="number" id="cur-precio" min="0" step="1000" placeholder="0 = gratuito"></div>' +
    '<div class="campo"><label for="cur-creditos">Créditos</label><input type="number" id="cur-creditos" min="1" max="100"></div></div>' +
    '<div class="grid-2"><div class="campo"><label for="cur-horas">Horas estimadas</label><input type="number" id="cur-horas" min="1" max="10000"></div>' +
    '<div class="campo"><label for="cur-portada">Imagen de portada (URL)</label><input type="url" id="cur-portada" maxlength="500" placeholder="https://..."></div></div>' +
    '<div class="grid-2"><div class="campo"><label for="cur-meses">Meses para terminar</label><input type="number" id="cur-meses" min="1" max="36" placeholder="12"></div>' +
    '<div class="campo"><label for="cur-repaso">Meses de repaso</label><input type="number" id="cur-repaso" min="0" max="24" placeholder="6"></div></div>' +
    '<p class="hint" style="margin:-6px 0 12px;font-size:11.5px;color:var(--ink-soft)">El plazo para terminar empieza cuando el curso se activa (pago aprobado o inscripción gratuita); al terminarlo queda en repaso, solo lectura. Un cambio aplica a los plazos que empiecen después.</p>' +
    '<label class="campo" style="display:flex;gap:10px;align-items:flex-start;cursor:pointer"><input type="checkbox" id="cur-solo-colegios" style="width:auto;margin-top:3px">' +
    '<span><strong>Solo para colegios</strong><br><span class="hint" style="margin:0">No aparece en el catálogo abierto: lo ven los estudiantes de los colegios que lo contraten y se inscriben con el código del colegio.</span></span></label>' +
    '<div class="campo"><label for="cur-descripcion">Descripción</label><textarea id="cur-descripcion" rows="3" placeholder="Requerida para publicar"></textarea></div>' +
    '<div class="campo"><label for="cur-objetivos">Objetivos</label><textarea id="cur-objetivos" rows="2"></textarea></div>' +
    '</div><div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-curso\')">Cancelar</button>' +
    '<button class="btn btn-primary" id="cur-modal-btn" onclick="guardarCurso()">Crear curso</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

function abrirModalCurso(id) {
  const c = id ? CUR_CACHE.find(x => x.id === id) : null;
  CUR_DETALLE_ID = null;
  document.getElementById('modal-curso').dataset.id = id || '';
  document.getElementById('cur-modal-titulo').textContent = c ? 'Editar curso' : 'Crear curso';
  document.getElementById('cur-modal-btn').textContent = c ? 'Guardar cambios' : 'Crear curso';
  const opcion = (v, t, sel, extra) => '<option value="' + v + '"' + (sel ? ' selected' : '') + '>' + esc(t) + (extra || '') + '</option>';
  document.getElementById('cur-area').innerHTML = '<option value="">Selecciona el área</option>' +
    CUR_AREAS.filter(a => a.estado === 'activo' || (c && a.id === c.idArea)).map(a => opcion(a.id, a.nombre, c && a.id === c.idArea, a.estado !== 'activo' ? ' (inactiva)' : '')).join('');
  document.getElementById('cur-grado').innerHTML = '<option value="">Selecciona el grado</option>' +
    CUR_GRADOS.filter(g => g.estado === 'activo' || (c && g.id === c.idGrado)).map(g => opcion(g.id, g.nombre, c && g.id === c.idGrado, g.estado !== 'activo' ? ' (inactivo)' : '')).join('');
  const docentes = CUR_DOCENTES.slice();
  if (c && c.idDocenteTitular && !docentes.some(d => d.id === c.idDocenteTitular)) docentes.push({ id: c.idDocenteTitular, nombre: c.docenteTitular, apellido: '(no activo)' });
  document.getElementById('cur-docente').innerHTML = '<option value="">Sin asignar por ahora</option>' +
    docentes.map(d => opcion(d.id, (d.nombre + ' ' + (d.apellido || '')).trim(), c && d.id === c.idDocenteTitular)).join('');
  document.getElementById('cur-nombre').value = c ? c.nombre : '';
  document.getElementById('cur-precio').value = c ? Number(c.precio || 0) : '';
  document.getElementById('cur-creditos').value = c && c.creditos ? c.creditos : '';
  document.getElementById('cur-horas').value = c && c.horasEstimadas ? c.horasEstimadas : '';
  document.getElementById('cur-meses').value = c && c.mesesParaTerminar ? c.mesesParaTerminar : 12;
  document.getElementById('cur-repaso').value = c && c.mesesRepaso != null ? c.mesesRepaso : 6;
  document.getElementById('cur-portada').value = c && c.imagenPortada ? c.imagenPortada : '';
  document.getElementById('cur-descripcion').value = c && c.descripcion ? c.descripcion : '';
  document.getElementById('cur-objetivos').value = c && c.objetivos ? c.objetivos : '';
  document.getElementById('cur-solo-colegios').checked = !!(c && c.soloInstitucional);
  abrirModal('modal-curso');
}

async function guardarCurso() {
  const id = document.getElementById('modal-curso').dataset.id;
  const val = k => document.getElementById(k).value.trim();
  const num = k => val(k) === '' ? null : Number(val(k));
  const cuerpo = {
    nombre: val('cur-nombre'), idArea: num('cur-area'), idGrado: num('cur-grado'),
    precio: num('cur-precio'), creditos: num('cur-creditos'), horasEstimadas: num('cur-horas'),
    mesesParaTerminar: num('cur-meses'), mesesRepaso: num('cur-repaso'),
    imagenPortada: val('cur-portada'), descripcion: val('cur-descripcion'), objetivos: val('cur-objetivos'),
    soloInstitucional: document.getElementById('cur-solo-colegios').checked
  };
  if (num('cur-docente') !== null) cuerpo.idDocenteTitular = num('cur-docente');
  if (!cuerpo.nombre || !cuerpo.idArea || !cuerpo.idGrado) { toast('Nombre, área y grado son obligatorios', true); return; }
  try {
    const data = await pedirCurso(id ? 'PATCH' : 'POST', '/api/cursos' + (id ? '/' + id : ''), cuerpo);
    actualizarEnCache(data);
    cerrarModal('modal-curso');
    toast(id ? 'Curso "' + data.nombre + '" actualizado' : 'Curso "' + data.nombre + '" creado en borrador');
    pintarCursos();
  } catch (err) {
    toast(err.message, true);
  }
}

/* ---------- Ver: detalle y gestión de docentes, horarios e instituciones ---------- */
function renderModalDetalleCurso() {
  if (document.getElementById('modal-detalle-curso')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-detalle-curso"><div class="modal" style="max-width:720px"><div class="modal-header"><h3 id="curd-titulo">Detalle del curso</h3>' +
    '<button class="modal-close" onclick="cerrarDetalleCurso()">✕</button></div>' +
    '<div class="modal-body" id="curd-body">Cargando...</div>' +
    '<div class="modal-btns" id="curd-btns"></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

function cerrarDetalleCurso() {
  CUR_DETALLE_ID = null;
  cerrarModal('modal-detalle-curso');
}

async function verDetalleCurso(id) {
  CUR_DETALLE_ID = id;
  document.getElementById('curd-body').innerHTML = 'Cargando...';
  document.getElementById('curd-btns').innerHTML = '';
  abrirModal('modal-detalle-curso');
  try {
    const data = await pedirCurso('GET', '/api/cursos/' + id);
    actualizarEnCache(data);
    pintarDetalleCurso(data);
  } catch (err) {
    document.getElementById('curd-body').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>';
  }
}

function pintarDetalleCurso(c) {
  const editable = c.estado !== 'archivado';
  const fila = (k, v) => '<div class="flex justify-between" style="padding:7px 0;border-bottom:1px solid var(--line, #eee)"><span class="muted">' + k + '</span><span style="font-weight:600;text-align:right">' + v + '</span></div>';
  const seccion = t => '<h4 style="margin:18px 0 8px;font-size:14px">' + t + '</h4>';
  document.getElementById('curd-titulo').textContent = c.nombre;

  let html = '';
  if (c.pendientesParaPublicar && c.pendientesParaPublicar.length) {
    html += '<div id="curd-pendientes" style="background:var(--amber-soft, #FBEFD9);color:#8A611E;padding:10px 12px;border-radius:10px;margin-bottom:12px;font-size:13px">' +
      '<strong>Para publicarlo falta:</strong> ' + esc(c.pendientesParaPublicar.join('; ')) + '.</div>';
  }
  html += fila('Estado', pillEstadoCurso(c.estado)) + fila('Área · Grado', esc(c.area) + ' · ' + esc(c.grado)) +
    fila('Docente titular', c.docenteTitular ? esc(c.docenteTitular) : 'Sin asignar') + fila('Precio', precioCOP(c.precio) + (c.soloInstitucional ? ' · <span class="pill pill-azul">🏫 Solo para colegios</span>' : '')) +
    fila('Créditos · Horas', (c.creditos || '—') + ' · ' + (c.horasEstimadas || '—')) +
    fila('Plazo · Repaso', (c.mesesParaTerminar || 12) + ' meses para terminar · ' + (c.mesesRepaso != null ? c.mesesRepaso : 6) + ' de repaso') + fila('Creado por', esc(c.creadoPor) + ' · ' + esc(c.fechaCreacion || '')) +
    fila('Descripción', esc(c.descripcion || '—'));

  // Docentes
  html += seccion('Docentes (' + c.docentes.length + ')') + '<div id="curd-docentes">' +
    (c.docentes.length ? c.docentes.map(d => '<div class="flex justify-between items-center" style="padding:6px 0"><span>' + esc(d.nombre) + ' <span class="muted text-sm">· ' + esc(d.rol) + '</span></span>' +
      (editable && d.rol !== 'titular' ? '<button class="btn btn-ghost btn-xs" onclick="retirarDocenteCurso(' + c.id + ',' + d.idDocente + ')">Retirar</button>' : '') + '</div>').join('')
      : '<p class="muted text-sm">Sin docentes asignados.</p>') + '</div>';
  if (editable) {
    const disponibles = CUR_DOCENTES.filter(d => !c.docentes.some(x => x.idDocente === d.id));
    html += '<div class="flex gap-8" style="margin-top:6px"><select id="curd-doc-sel" style="flex:1"' + (disponibles.length ? '' : ' disabled') + '>' +
      '<option value="">' + (disponibles.length ? 'Agregar colaborador o invitado…' : 'No hay otros docentes activos disponibles') + '</option>' +
      disponibles.map(d => '<option value="' + d.id + '">' + esc(d.nombre + ' ' + d.apellido) + '</option>').join('') + '</select>' +
      '<select id="curd-doc-rol" style="width:140px" title="Colaborador: participación regular en el curso. Invitado: participación puntual."><option value="colaborador">Colaborador</option><option value="invitado">Invitado</option></select>' +
      '<button class="btn btn-ghost btn-sm" id="curd-doc-btn"' + (disponibles.length ? '' : ' disabled') + ' onclick="agregarDocenteCurso(' + c.id + ')">+ Agregar</button></div>' +
      (disponibles.length ? '' : '<p class="text-sm muted" style="margin-top:4px">Para agregar un colaborador o invitado, primero crea o activa su cuenta en Usuarios (rol Docente).</p>');
  }

  // Horarios
  html += seccion('Horarios') + '<div id="curd-horarios">' + (c.horarios.length ? '' : '<p class="muted text-sm" id="curd-hor-vacio">Sin horarios definidos.</p>') +
    c.horarios.map(h => filaHorarioHtml(h, editable)).join('') + '</div>';
  if (editable) {
    html += '<div class="flex gap-8" style="margin-top:6px"><button class="btn btn-ghost btn-sm" id="curd-hor-add" onclick="agregarFilaHorario()">+ Agregar horario</button>' +
      '<button class="btn btn-ghost btn-sm" id="curd-hor-guardar" onclick="guardarHorariosCurso(' + c.id + ')">💾 Guardar horarios</button></div>';
  }

  // Instituciones
  html += seccion('Instituciones que lo contratan (' + c.instituciones.length + ')') + '<div id="curd-inst">' +
    (c.instituciones.length ? c.instituciones.map(i => '<div class="flex justify-between items-center curd-contratacion" data-nombre="' + esc(i.nombre) + '" style="padding:6px 0"><span>' + esc(i.nombre) +
      ' <span class="muted text-sm">· desde ' + esc(i.contratadoEn || '') + '</span>' +
      (i.codigoInscripcion ? '<br><span class="text-sm">Código de inscripción: <strong class="curd-codigo" style="font-family:monospace;letter-spacing:1px">' + esc(i.codigoInscripcion) + '</strong> · ' + (i.inscritos || 0) + ' inscrito(s) con el código</span>' : '') + '</span>' +
      '<span class="flex gap-8">' + (i.codigoInscripcion ? '<button class="btn btn-ghost btn-xs" onclick="regenerarCodigoCurso(' + c.id + ',' + i.idInstitucion + ')" title="Generar un código nuevo">🔄 Regenerar código</button>' : '') +
      '<button class="btn btn-ghost btn-xs" onclick="quitarInstitucionCurso(' + c.id + ',' + i.idInstitucion + ')">Quitar</button></span></div>').join('')
      : '<p class="muted text-sm">Ninguna institución lo ha contratado.</p>') + '</div>';
  if (editable) {
    const libres = CUR_INSTITUCIONES.filter(i => !c.instituciones.some(x => x.idInstitucion === i.id));
    html += '<div class="flex gap-8" style="margin-top:6px"><select id="curd-inst-sel" style="flex:1"><option value="">Agregar institución activa…</option>' +
      libres.map(i => '<option value="' + i.id + '">' + esc(i.nombre) + '</option>').join('') + '</select>' +
      '<button class="btn btn-ghost btn-sm" id="curd-inst-btn" onclick="contratarCurso(' + c.id + ')">+ Contratar</button></div>';
  }
  // Fase 3: inscritos del curso con su avance
  html += seccion('Estudiantes inscritos') + '<div id="curd-inscritos"><p class="muted text-sm">Cargando inscritos...</p></div>';
  document.getElementById('curd-body').innerHTML = html;
  cargarInscritosCurso(c.id);

  const acciones = (CUR_TRANSICIONES[c.estado] || []).map(e => '<button class="btn btn-ghost" onclick="cambiarEstadoCurso(' + c.id + ',\'' + e + '\')">' + CUR_ACCION[e] + '</button>').join('');
  document.getElementById('curd-btns').innerHTML = acciones +
    (editable ? '<button class="btn btn-primary" onclick="cerrarDetalleCurso();abrirModalCurso(' + c.id + ')">✏️ Editar</button>' : '');
}

function filaHorarioHtml(h, editable) {
  const dis = editable ? '' : ' disabled';
  return '<div class="flex gap-8 curd-hor-fila" style="margin-bottom:6px">' +
    '<select class="curd-hor-dia" style="width:150px"' + dis + '>' + CUR_DIAS.map(([v, t]) => '<option value="' + v + '"' + (h && h.dia === v ? ' selected' : '') + '>' + t + '</option>').join('') + '</select>' +
    '<input type="time" class="curd-hor-ini" value="' + (h ? String(h.horaInicio).slice(0, 5) : '08:00') + '"' + dis + '>' +
    '<input type="time" class="curd-hor-fin" value="' + (h ? String(h.horaFin).slice(0, 5) : '10:00') + '"' + dis + '>' +
    (editable ? '<button class="btn btn-ghost btn-xs" onclick="this.parentElement.remove()" title="Quitar">✕</button>' : '') + '</div>';
}

function agregarFilaHorario() {
  const vacio = document.getElementById('curd-hor-vacio');
  if (vacio) vacio.remove();
  document.getElementById('curd-horarios').insertAdjacentHTML('beforeend', filaHorarioHtml(null, true));
}

async function guardarHorariosCurso(id) {
  const horarios = Array.from(document.querySelectorAll('#curd-horarios .curd-hor-fila')).map(f => ({
    dia: f.querySelector('.curd-hor-dia').value,
    horaInicio: f.querySelector('.curd-hor-ini').value,
    horaFin: f.querySelector('.curd-hor-fin').value
  }));
  try {
    const data = await pedirCurso('PUT', '/api/cursos/' + id + '/horarios', { horarios });
    actualizarEnCache(data);
    toast('Horarios guardados (' + data.horarios.length + ')');
    pintarDetalleCurso(data);
    pintarCursos();
  } catch (err) {
    toast(err.message, true);
  }
}

async function agregarDocenteCurso(id) {
  const idDocente = Number(document.getElementById('curd-doc-sel').value);
  if (!idDocente) { toast('Selecciona un docente', true); return; }
  try {
    const data = await pedirCurso('POST', '/api/cursos/' + id + '/docentes', { idDocente, rol: document.getElementById('curd-doc-rol').value });
    actualizarEnCache(data);
    toast('Docente agregado al curso');
    pintarDetalleCurso(data);
  } catch (err) {
    toast(err.message, true);
  }
}

async function retirarDocenteCurso(id, idDocente) {
  try {
    const data = await pedirCurso('DELETE', '/api/cursos/' + id + '/docentes/' + idDocente);
    actualizarEnCache(data);
    toast('Docente retirado del curso');
    pintarDetalleCurso(data);
  } catch (err) {
    toast(err.message, true);
  }
}

async function contratarCurso(id) {
  const idInstitucion = Number(document.getElementById('curd-inst-sel').value);
  if (!idInstitucion) { toast('Selecciona una institución', true); return; }
  try {
    const data = await pedirCurso('POST', '/api/cursos/' + id + '/instituciones', { idInstitucion });
    actualizarEnCache(data);
    toast('Contratación registrada');
    pintarDetalleCurso(data);
    pintarCursos();
  } catch (err) {
    toast(err.message, true);
  }
}

async function quitarInstitucionCurso(id, idInstitucion) {
  const curso = CUR_CACHE.find(c => c.id === id);
  const inst = curso && curso.instituciones.find(i => i.idInstitucion === idInstitucion);
  confirmarAccion('¿Quitar la contratación de <strong>' + esc(inst ? inst.nombre : 'la institución') + '</strong> para este curso?', async () => {
    try {
      const data = await pedirCurso('DELETE', '/api/cursos/' + id + '/instituciones/' + idInstitucion);
      actualizarEnCache(data);
      toast('Contratación eliminada');
      pintarDetalleCurso(data);
      pintarCursos();
    } catch (err) {
      toast(err.message, true);
    }
  }, 'Quitar');
}

/* ---------- Fase 3: código de inscripción e inscritos ---------- */
const CUR_INSC_LABEL = { activa: 'activa', completada: 'completada · en repaso', pendiente_pago: 'pendiente de pago', retirada: 'retirada', suspendida: 'suspendida', en_reembolso: 'reembolso en trámite', vencida: 'plazo vencido', cerrada: 'cerrada' };
/* Acceso por tiempo: hasta cuándo puede terminar o repasar */
function plazoInscritoTexto(i) {
  const f = iso => { if (!iso) return '—'; const d = new Date(iso); return String(d.getDate()).padStart(2, '0') + '/' + String(d.getMonth() + 1).padStart(2, '0') + '/' + d.getFullYear(); };
  if (i.estado === 'activa' && i.accesoHasta) return 'Hasta el ' + f(i.accesoHasta);
  if (i.estado === 'completada' && i.repasoHasta) return 'Repaso hasta el ' + f(i.repasoHasta);
  if (i.estado === 'vencida') return 'Venció el ' + f(i.accesoHasta);
  if (i.estado === 'cerrada') return 'Repaso terminó el ' + f(i.repasoHasta);
  return '—';
}

async function regenerarCodigoCurso(id, idInstitucion) {
  confirmarAccion('¿Generar un código de inscripción nuevo? El código anterior dejará de funcionar; quienes ya se inscribieron siguen vinculados.', async () => {
    try {
      const data = await pedirCurso('POST', '/api/cursos/' + id + '/instituciones/' + idInstitucion + '/codigo');
      actualizarEnCache(data);
      toast('Código de inscripción regenerado');
      pintarDetalleCurso(data);
    } catch (err) { toast(err.message, true); }
  }, 'Regenerar');
}

async function cargarInscritosCurso(id) {
  const cont = document.getElementById('curd-inscritos');
  try {
    const lista = await pedirCurso('GET', '/api/cursos/' + id + '/inscripciones');
    if (!cont) return;
    if (!lista.length) { cont.innerHTML = '<p class="muted text-sm">Aún no hay estudiantes inscritos.</p>'; return; }
    cont.innerHTML = '<div class="tabla-wrap"><table><thead><tr><th>Estudiante</th><th>Estado</th><th>Avance</th><th>Plazo</th><th>Colegio (código)</th><th></th></tr></thead><tbody>' +
      lista.map(i => '<tr class="curd-inscrito" data-correo="' + esc(i.correo) + '"><td><div style="font-weight:600">' + esc(i.estudiante) + '</div><div class="text-sm muted">' + esc(i.correo) + '</div></td>' +
        '<td>' + pill(i.estado === 'pendiente_pago' ? 'pendiente' : i.estado) + ' <span class="text-sm muted">' + esc(CUR_INSC_LABEL[i.estado] || i.estado) + '</span></td>' +
        '<td class="text-sm">' + Math.round(Number(i.porcentaje || 0)) + ' % · ' + i.leccionesCompletadas + ' de ' + i.totalLecciones + ' lecciones</td>' +
        '<td class="text-sm curd-plazo">' + plazoInscritoTexto(i) + '</td>' +
        '<td class="text-sm">' + esc(i.institucion || '—') + '</td>' +
        '<td style="text-align:right;white-space:nowrap">' +
        (['activa', 'completada', 'pendiente_pago'].includes(i.estado) ? '<button class="btn btn-ghost btn-xs" onclick="estadoInscripcionAdmin(' + id + ',' + i.id + ',\'suspendida\')">Suspender</button>' : '') +
        (i.estado === 'suspendida' ? '<button class="btn btn-ghost btn-xs" onclick="estadoInscripcionAdmin(' + id + ',' + i.id + ',\'activa\')">Reactivar</button>' : '') +
        '</td></tr>').join('') + '</tbody></table></div>';
  } catch (err) { if (cont) cont.innerHTML = '<p class="muted text-sm">' + esc(err.message) + '</p>'; }
}

async function estadoInscripcionAdmin(idCurso, idInscripcion, estado) {
  try {
    await pedirCurso('PATCH', '/api/inscripciones/' + idInscripcion + '/estado', { estado });
    toast(estado === 'suspendida' ? 'Inscripción suspendida' : 'Inscripción reactivada');
    cargarInscritosCurso(idCurso);
  } catch (err) { toast(err.message, true); }
}

