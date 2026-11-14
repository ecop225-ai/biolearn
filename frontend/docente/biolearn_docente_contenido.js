/* ================================================================
   BIOLearn — biolearn_docente_contenido.js
   Fase 2 del módulo de Cursos: conecta al backend real las secciones «Mis cursos», «Lecciones» y
   «Materiales» del portal del docente CONSERVANDO su interfaz original (tarjetas, formularios,
   editor enriquecido con plantillas, vistas previas «como estudiante»).
   Reglas del backend que se reflejan aquí:
   - El titular edita los datos del curso; titular y colaboradores editan el contenido; los invitados
     solo consultan. El precio y la publicación son del Administrador.
   - Nada se borra: lecciones y materiales se archivan (se pueden restaurar).
   - Las imágenes (portadas y editor) se suben al servidor (PDF/PNG/JPG, máx. 10 MB, RNF-005).
   - Videos solo de YouTube, embebidos (RNF-015).
================================================================ */
let DOC_CURSOS = [];
let DOC_AREAS = [];
let DOC_GRADOS = [];
let DOC_LECCIONES = [];
let DOC_LECCION_DET = null;

const DOC_ESTADO_LECCION_BOTON = {
  borrador: [['activa', 'Activar', 'btn-amber']],
  activa: [['inactiva', 'Desactivar', 'btn-ghost']],
  inactiva: [['activa', 'Activar', 'btn-amber']],
  archivada: [['borrador', '↩ Restaurar', 'btn-ghost']]
};

/* ---------------------------------------------------------------- comunicación con el backend */
async function pedirApi(metodo, ruta, cuerpo) {
  const headers = authHeaders();
  if (!headers) throw new Error('Sesión no válida');
  const opciones = { method: metodo, headers };
  if (cuerpo !== undefined) opciones.body = JSON.stringify(cuerpo);
  const r = await fetch(API_BASE + ruta, opciones);
  if (manejarSesionExpirada(r)) throw new Error('Sesión expirada');
  const data = await r.json().catch(() => ({}));
  if (!r.ok) {
    const detalle = data.detalles && data.detalles.length ? ' (' + data.detalles.join('; ') + ')' : '';
    throw new Error((data.mensaje || 'Error ' + r.status) + detalle);
  }
  return data;
}

/** Sube un archivo (PDF, PNG o JPG de máximo 10 MB) y devuelve su URL relativa (/api/archivos/…). */
async function subirArchivoDocente(archivo) {
  const token = sessionStorage.getItem('biolearn_token');
  const datos = new FormData();
  datos.append('archivo', archivo);
  const r = await fetch(API_BASE + '/api/archivos', { method: 'POST', headers: { 'Authorization': 'Bearer ' + token }, body: datos });
  if (manejarSesionExpirada(r)) throw new Error('Sesión expirada');
  const data = await r.json().catch(() => ({}));
  if (!r.ok) {
    const detalle = data.detalles && data.detalles.length ? ' (' + data.detalles.join('; ') + ')' : '';
    throw new Error((data.mensaje || 'No se pudo subir el archivo') + detalle);
  }
  return data.url;
}

function urlCompleta(u) { return u && u.startsWith('/api/') ? API_BASE + u : u; }
function cursoActualDoc(id) { return DOC_CURSOS.find(c => c.id === Number(id)) || null; }

async function cargarCatalogosDocente() {
  if (DOC_AREAS.length && DOC_GRADOS.length) return;
  const [ra, rg] = await Promise.all([fetch(API_BASE + '/api/catalogos/areas'), fetch(API_BASE + '/api/catalogos/grados')]);
  DOC_AREAS = ra.ok ? await ra.json() : [];
  DOC_GRADOS = rg.ok ? await rg.json() : [];
}

async function cargarCursosDocente() {
  DOC_CURSOS = await pedirApi('GET', '/api/docente/cursos');
  await cargarCatalogosDocente();
}

/* ---------------------------------------------------------------- imágenes: se suben al servidor
   Sustituyen las versiones del núcleo que guardaban la imagen incrustada (data:base64), que no cabe
   en la base de datos (imagen_portada es VARCHAR(500)). */
function coverImgSubir(id) {
  const input = document.createElement('input');
  input.type = 'file'; input.accept = '.png,.jpg,.jpeg';
  input.onchange = async function () {
    if (!input.files.length) return;
    try {
      const url = await subirArchivoDocente(input.files[0]);
      document.getElementById(id + '-url').value = '';
      coverImgSetValue(id, url, false);
      toast('Imagen subida');
    } catch (err) { toast(err.message, true); }
  };
  input.click();
}
function coverImgSetValue(id, value, esUrl) {
  document.getElementById(id).value = value || '';
  if (!esUrl) document.getElementById(id + '-url').value = (value && !value.startsWith('/api/')) ? value : '';
  const preview = document.getElementById(id + '-preview');
  preview.innerHTML = value
    ? '<img src="' + esc(urlCompleta(value)) + '" style="max-width:180px;max-height:100px;border-radius:8px;border:1px solid var(--line);vertical-align:middle">' +
      ' <button type="button" onclick="coverImgSetValue(\'' + id + '\',\'\',true)" style="border:none;background:none;color:var(--clay);cursor:pointer;font-size:12.5px">Quitar</button>'
    : '';
}
function rteImage(id) {
  const input = document.createElement('input');
  input.type = 'file'; input.accept = '.png,.jpg,.jpeg';
  input.onchange = async function () {
    if (!input.files.length) return;
    try {
      const url = await subirArchivoDocente(input.files[0]);
      document.getElementById(id).focus();
      document.execCommand('insertHTML', false, '<span class="rte-img-wrap rte-media" style="width:320px"><img src="' + esc(API_BASE + url) + '" alt=""></span><span> </span>');
      rteWordCount(id);
    } catch (err) { toast(err.message, true); }
  };
  input.click();
}

/* ================================================================
   VISTA: MIS CURSOS (tarjetas del prototipo original)
================================================================ */
let DOC_FILTRO_BUSQUEDA = '';
let DOC_FILTRO_ESTADO = 'todos';
let DOC_FILTRO_ROL = 'todos';

function renderCursos(cont) {
  cont.innerHTML = '<div class="section-header"><div><h2>Mis cursos</h2><p class="subtitle">Cursos en los que participas como titular, colaborador o invitado. Puedes proponer un curso: quedará en borrador hasta que el Administrador lo publique.</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalCurso()">+ Proponer curso</button></div>' +
    '<div class="flex gap-10 flex-wrap mb-10">' +
    '<input id="doc-cur-buscar" placeholder="Buscar por nombre, código o área..." oninput="DOC_FILTRO_BUSQUEDA=this.value;pintarCursosDocente()" style="width:300px">' +
    '<select id="doc-cur-estado" onchange="DOC_FILTRO_ESTADO=this.value;pintarCursosDocente()" style="width:180px">' +
    '<option value="todos">Todos los estados</option><option value="borrador">Borrador</option><option value="activo">Activos</option>' +
    '<option value="cerrado">Cerrados</option><option value="archivado">Archivados</option></select>' +
    '<select id="doc-cur-rol" onchange="DOC_FILTRO_ROL=this.value;pintarCursosDocente()" style="width:180px">' +
    '<option value="todos">Todos mis roles</option><option value="titular">Titular</option><option value="colaborador">Colaborador</option><option value="invitado">Invitado</option></select>' +
    '</div>' +
    '<div id="doc-cursos-resumen" class="flex items-center gap-10 text-sm muted" style="margin-bottom:10px"></div>' +
    '<div id="doc-cursos-lista"><div class="empty-state">Cargando tus cursos...</div></div>';
  document.getElementById('doc-cur-buscar').value = DOC_FILTRO_BUSQUEDA;
  document.getElementById('doc-cur-estado').value = DOC_FILTRO_ESTADO;
  document.getElementById('doc-cur-rol').value = DOC_FILTRO_ROL;
  renderModalCurso();
  renderModalCursoPreview();
  cargarCursosDocente().then(pintarCursosDocente).catch(err => {
    document.getElementById('doc-cursos-lista').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>';
  });
}

function normalizarDoc(t) { return String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, ''); }

function limpiarFiltrosCursosDocente() {
  DOC_FILTRO_BUSQUEDA = ''; DOC_FILTRO_ESTADO = 'todos'; DOC_FILTRO_ROL = 'todos';
  document.getElementById('doc-cur-buscar').value = '';
  document.getElementById('doc-cur-estado').value = 'todos';
  document.getElementById('doc-cur-rol').value = 'todos';
  pintarCursosDocente();
}

function pintarCursosDocente() {
  const contenedor = document.getElementById('doc-cursos-lista');
  if (!contenedor) return;
  const q = normalizarDoc(DOC_FILTRO_BUSQUEDA.trim());
  const lista = DOC_CURSOS.filter(c =>
    (!q || normalizarDoc([c.nombre, c.codigo, c.area, c.grado].join(' ')).includes(q)) &&
    (DOC_FILTRO_ESTADO === 'todos' || c.estado === DOC_FILTRO_ESTADO) &&
    (DOC_FILTRO_ROL === 'todos' || c.miRol === DOC_FILTRO_ROL));

  const activos = [];
  if (q) activos.push('Búsqueda: "' + DOC_FILTRO_BUSQUEDA.trim() + '"');
  if (DOC_FILTRO_ESTADO !== 'todos') activos.push('Estado: ' + DOC_FILTRO_ESTADO);
  if (DOC_FILTRO_ROL !== 'todos') activos.push('Rol: ' + DOC_FILTRO_ROL);
  document.getElementById('doc-cursos-resumen').innerHTML = '<span>Mostrando <strong>' + lista.length + '</strong> ' + (lista.length === 1 ? 'curso' : 'cursos') +
    (activos.length ? ' · Filtros activos: ' + esc(activos.join(' · ')) : '') + '</span>' +
    (activos.length ? '<button class="btn btn-ghost btn-xs" onclick="limpiarFiltrosCursosDocente()">✕ Limpiar filtros</button>' : '');

  if (!DOC_CURSOS.length) { contenedor.innerHTML = '<div class="empty-state">Aún no participas en ningún curso. Propón uno con «+ Proponer curso».</div>'; return; }
  let html = '<div class="tabla-wrap"><table><thead><tr>' + ['Curso', 'Mi rol', 'Estado', 'Lecciones', 'Para publicar', ''].map(h => '<th>' + h + '</th>').join('') + '</tr></thead><tbody>';
  lista.forEach(c => {
    const color = c.color || '#33526E';
    const pendientes = c.estado === 'borrador'
      ? (c.pendientesParaPublicar && c.pendientesParaPublicar.length
          ? '<span class="text-sm" style="color:var(--clay)">Falta: ' + esc(c.pendientesParaPublicar.join('; ')) + '</span>'
          : '<span class="text-sm" style="color:var(--sage)">Listo; el Administrador lo publica</span>')
      : '<span class="muted text-sm">—</span>';
    const rolClase = c.miRol === 'titular' ? 'pill-verde' : (c.miRol === 'colaborador' ? 'pill-azul' : 'pill-gris');
    html += '<tr class="doc-curso" data-nombre="' + esc(c.nombre) + '"' + (c.estado === 'archivado' ? ' style="opacity:.6"' : '') + '>' +
      '<td><div style="font-weight:600"><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:' + esc(color) + ';margin-right:6px"></span>' + esc(c.nombre) + '</div>' +
      '<div class="text-sm muted">' + (c.codigo ? '<span style="font-family:monospace;font-weight:700;color:' + esc(color) + '">' + esc(c.codigo) + '</span> · ' : '') + esc(c.area) + ' · ' + esc(c.grado) + '</div></td>' +
      '<td><span class="pill ' + rolClase + '">' + esc(c.miRol) + '</span></td>' +
      '<td>' + pill(c.estado) + '</td>' +
      '<td class="text-sm">' + c.leccionesActivas + ' activas de ' + c.totalLecciones + '</td>' +
      '<td>' + pendientes + '</td>' +
      '<td style="white-space:nowrap;text-align:right">' +
      '<button class="btn btn-ghost btn-xs" onclick="setCtx(\'lec\',\'curso\',\'' + c.id + '\');goTo(\'lecciones\')">📚 Contenido</button> ' +
      '<button class="btn btn-ghost btn-xs" onclick="abrirModalCursoPreview(' + c.id + ')" title="Ver como estudiante">👁 Vista previa</button> ' +
      '<button class="btn btn-ghost btn-xs" onclick="abrirInscritosDocente(' + c.id + ')" title="Estudiantes inscritos y su avance">👥 Inscritos</button> ' +
      (c.miRol === 'titular' && c.estado !== 'archivado' ? '<button class="btn btn-ghost btn-xs" onclick="abrirModalCurso(' + c.id + ')">✏️ Editar</button>' : '') +
      '</td></tr>';
  });
  if (!lista.length) html += '<tr><td colspan="6" class="empty-state">No hay cursos que coincidan con los filtros.</td></tr>';
  contenedor.innerHTML = html + '</tbody></table></div>';
}

function sugerirCodigoCurso(nombreArea) {
  const conocida = (typeof AREAS_ACADEMICAS_ADMIN !== 'undefined' ? AREAS_ACADEMICAS_ADMIN : []).find(a => a.nombre === nombreArea);
  const base = (nombreArea || 'CURSO').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().replace(/[^A-Z ]/g, '');
  const prefijo = conocida ? conocida.codigo : (base.split(/\s+/).filter(p => p.length > 2).map(p => p.slice(0, 3)).join('').slice(0, 6) || 'CURSO');
  const existentes = DOC_CURSOS.filter(c => (c.codigo || '').startsWith(prefijo + '-')).length;
  return prefijo + '-' + (101 + existentes);
}
function sugerirCodigoCursoDocente() {
  const sel = document.getElementById('curso-area');
  const nombre = sel.selectedIndex > 0 ? sel.options[sel.selectedIndex].text : '';
  document.getElementById('curso-codigo').value = sugerirCodigoCurso(nombre);
}

async function abrirModalCurso(id) {
  try { await cargarCatalogosDocente(); } catch (e) { toast('No se pudieron cargar las áreas y los grados', true); }
  const c = id ? cursoActualDoc(id) : null;
  document.getElementById('curso-modal-titulo').textContent = c ? 'Editar curso' : 'Crear curso';
  document.getElementById('curso-modal-btn').textContent = c ? 'Guardar cambios' : 'Crear curso';
  document.getElementById('curso-id').value = c ? c.id : '';
  document.getElementById('curso-nombre').value = c ? c.nombre : '';
  document.getElementById('curso-descripcion').value = c && c.descripcion ? c.descripcion : '';
  document.getElementById('curso-objetivos').value = c && c.objetivos ? c.objetivos : '';
  document.getElementById('curso-color').value = c && c.color ? c.color : '#33526E';
  document.getElementById('curso-area').innerHTML = '<option value="">Selecciona el área</option>' +
    DOC_AREAS.map(a => '<option value="' + a.id + '"' + (c && a.id === c.idArea ? ' selected' : '') + '>' + esc(a.nombre) + '</option>').join('');
  document.getElementById('curso-grado').innerHTML = '<option value="">Selecciona el grado</option>' +
    DOC_GRADOS.map(g => '<option value="' + g.id + '"' + (c && g.id === c.idGrado ? ' selected' : '') + '>' + esc(g.nombre) + '</option>').join('');
  document.getElementById('curso-codigo').value = c && c.codigo ? c.codigo : '';
  document.getElementById('curso-precio').value = c ? Number(c.precio || 0) : 0;
  document.getElementById('curso-creditos').value = c && c.creditos ? c.creditos : 1;
  document.getElementById('curso-horas').value = c && c.horasEstimadas ? c.horasEstimadas : 10;
  document.getElementById('curso-nota').style.display = c ? 'none' : 'block';
  setTimeout(function () {
    coverImgSetValue('curso-imagen', c ? (c.imagenPortada || '') : '');
    rteSetValue('curso-info-estudiante', c ? (c.infoEstudiante || '') : '');
  }, 0);
  abrirModal('modal-curso');
}

function renderModalCurso() {
  if (document.getElementById('modal-curso')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-curso"><div class="modal grande"><div class="modal-header"><h3 id="curso-modal-titulo">Crear curso</h3><button class="modal-close" onclick="cerrarModal(\'modal-curso\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="curso-id">' +
    '<div class="grid-2"><div class="campo"><label for="curso-nombre">Nombre del curso</label><input type="text" id="curso-nombre" maxlength="150"></div>' +
    '<div class="campo"><label for="curso-color">Color representativo</label><input type="color" id="curso-color" style="height:42px"></div></div>' +
    '<div class="grid-2"><div class="campo"><label for="curso-grado">Grado</label><select id="curso-grado"></select></div>' +
    '<div class="campo"><label for="curso-area">Área académica</label><select id="curso-area" onchange="if(!document.getElementById(\'curso-id\').value)sugerirCodigoCursoDocente()"></select><p class="hint">Estas áreas las define el administrador de la plataforma en Catálogos académicos.</p></div></div>' +
    '<div class="campo"><label for="curso-codigo">Código del curso</label><div class="flex gap-8"><input id="curso-codigo" maxlength="20" style="flex:1;font-family:monospace" placeholder="Se sugiere según el área"><button type="button" class="btn btn-ghost btn-sm" onclick="sugerirCodigoCursoDocente()">🔄 Sugerir</button></div></div>' +
    '<div class="campo"><label for="curso-descripcion">Descripción</label><textarea id="curso-descripcion" rows="2" placeholder="Requerida para publicar"></textarea></div>' +
    '<div class="campo"><label for="curso-objetivos">Objetivos de aprendizaje</label><textarea id="curso-objetivos" rows="2"></textarea></div>' +
    coverImageFieldHTML('curso-imagen', 'Imagen de portada', 'Se muestra como banner en la tarjeta del curso. Si se deja vacía, se usa el color representativo. PNG o JPG de máximo 10 MB.') +
    '<div class="grid-2"><div class="campo"><label for="curso-precio">Precio (COP)</label><input type="number" id="curso-precio" disabled><p class="hint">Lo define el Administrador de la plataforma.</p></div>' +
    '<div class="campo"><label for="curso-creditos">Créditos</label><input type="number" id="curso-creditos" min="1" max="100"></div>' +
    '<div class="campo"><label for="curso-horas">Horas estimadas</label><input type="number" id="curso-horas" min="1" max="10000"></div></div>' +
    '<div class="campo"><label>Información para el estudiante antes de inscribirse</label><p class="hint" style="margin:0 0 6px">Requisitos, temario, metodología o cualquier dato que el estudiante deba conocer antes de suscribirse.</p>' + richTextEditorHTML('curso-info-estudiante', 'Escribe aquí lo que el estudiante debe saber antes de inscribirse...') + '</div>' +
    '<p class="text-sm muted" id="curso-nota">El curso se crea en borrador, contigo como docente titular. El Administrador lo publica cuando tenga descripción y al menos una lección activa.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-curso\')">Cancelar</button><button class="btn btn-primary" id="curso-modal-btn" onclick="guardarCurso()">Crear curso</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function guardarCurso() {
  const id = document.getElementById('curso-id').value;
  const val = k => document.getElementById(k).value.trim();
  const num = k => val(k) === '' ? null : Number(val(k));
  const cuerpo = {
    nombre: val('curso-nombre'), idArea: num('curso-area'), idGrado: num('curso-grado'),
    codigo: val('curso-codigo').toUpperCase(), color: val('curso-color'),
    descripcion: val('curso-descripcion'), objetivos: val('curso-objetivos'),
    imagenPortada: val('curso-imagen'), infoEstudiante: rteGetValue('curso-info-estudiante'),
    creditos: num('curso-creditos'), horasEstimadas: num('curso-horas')
  };
  if (!cuerpo.nombre) { toast('Escribe el nombre del curso', true); return; }
  if (!cuerpo.idArea || !cuerpo.idGrado) { toast('Selecciona el área académica y el grado', true); return; }
  if (!cuerpo.codigo) cuerpo.codigo = sugerirCodigoCurso(document.getElementById('curso-area').selectedOptions[0].text);
  try {
    const data = await pedirApi(id ? 'PATCH' : 'POST', '/api/docente/cursos' + (id ? '/' + id : ''), cuerpo);
    cerrarModal('modal-curso');
    toast(id ? 'Curso "' + data.nombre + '" actualizado' : 'Curso "' + data.nombre + '" creado como borrador');
    await cargarCursosDocente();
    pintarCursosDocente();
  } catch (err) { toast(err.message, true); }
}

function abrirModalCursoPreview(id) {
  const c = cursoActualDoc(id);
  if (!c) return;
  const color = c.color || '#33526E';
  document.getElementById('cp-banner').style.background = c.imagenPortada ? 'transparent' : color;
  document.getElementById('cp-banner').innerHTML = c.imagenPortada ? '<img src="' + esc(urlCompleta(c.imagenPortada)) + '" style="width:100%;height:100%;object-fit:cover;border-radius:12px">' : '';
  document.getElementById('cp-nombre').textContent = c.nombre;
  document.getElementById('cp-nombre').style.color = color;
  document.getElementById('cp-meta').textContent = (c.area || '—') + ' · Grado ' + (c.grado || '—') + ' · ' + (c.horasEstimadas || 0) + ' horas · ' + (c.creditos || 0) + ' créditos';
  const precio = Number(c.precio || 0);
  document.getElementById('cp-precio').textContent = precio ? '$' + precio.toLocaleString('es-CO') : 'Gratuito';
  document.getElementById('cp-descripcion').textContent = c.descripcion || '';
  document.getElementById('cp-objetivos').textContent = c.objetivos || '';
  document.getElementById('cp-info').innerHTML = c.infoEstudiante && c.infoEstudiante.trim() ? c.infoEstudiante : '<p class="muted">El docente todavía no agregó información adicional para el estudiante.</p>';
  document.getElementById('cp-btn-inscribir').style.background = color;
  abrirModal('modal-curso-preview');
}

function renderModalCursoPreview() {
  if (document.getElementById('modal-curso-preview')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-curso-preview"><div class="modal grande"><div class="modal-header"><h3>Vista previa del curso</h3><button class="modal-close" onclick="cerrarModal(\'modal-curso-preview\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div id="cp-banner" style="height:160px;border-radius:12px;margin-bottom:18px"></div>' +
    '<h2 id="cp-nombre" style="font-size:24px;margin:0 0 6px"></h2>' +
    '<p class="text-sm muted mb-14" id="cp-meta"></p>' +
    '<div class="flex justify-between items-center card mb-14"><span style="font-size:20px;font-weight:800" id="cp-precio"></span><button id="cp-btn-inscribir" disabled style="border:none;border-radius:9px;padding:10px 20px;color:#fff;font-weight:700;opacity:.85">Inscribirme</button></div>' +
    '<div class="campo"><label>Descripción</label><p class="text-sm" id="cp-descripcion"></p></div>' +
    '<div class="campo"><label>Objetivos de aprendizaje</label><p class="text-sm" id="cp-objetivos"></p></div>' +
    '<div class="campo"><label>Antes de inscribirte</label><div class="card" id="cp-info" style="font-size:13.5px;line-height:1.7"></div></div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se ve para el estudiante. El botón "Inscribirme" está desactivado en esta vista previa.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-curso-preview\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ================================================================
   VISTA: LECCIONES (tarjetas del prototipo original + orden, prerrequisitos y estados del backend)
================================================================ */
function renderLecciones(cont) {
  const cursoId = window.ctx_lec_curso || '';
  cont.innerHTML = '<div class="section-header"><div><h2>Lecciones</h2><p class="subtitle">Cada lección agrupa materiales, actividades, evaluaciones, foros y sesiones.</p></div>' +
    '<button class="btn btn-primary" id="doc-lec-nueva" onclick="abrirModalLeccion()" style="display:none">+ Nueva lección</button></div>' +
    '<div class="context-picker"><select id="doc-lec-curso" onchange="setCtx(\'lec\',\'curso\',this.value)"><option value="">Selecciona un curso</option></select></div>' +
    '<p class="context-hint" id="doc-lec-info"></p><div id="doc-lec-lista"></div>';
  renderModalLeccion();
  cargarCursosDocente().then(async () => {
    document.getElementById('doc-lec-curso').innerHTML = '<option value="">Selecciona un curso</option>' +
      DOC_CURSOS.map(c => '<option value="' + c.id + '"' + (String(c.id) === String(cursoId) ? ' selected' : '') + '>' + esc(c.nombre) + '</option>').join('');
    if (!cursoId || !cursoActualDoc(cursoId)) { document.getElementById('doc-lec-lista').innerHTML = '<div class="empty-state">Selecciona un curso para ver o crear sus lecciones.</div>'; return; }
    await cargarLeccionesDocente();
  }).catch(err => { document.getElementById('doc-lec-lista').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; });
}

async function cargarLeccionesDocente() {
  const c = cursoActualDoc(window.ctx_lec_curso);
  if (!c) return;
  document.getElementById('doc-lec-nueva').style.display = c.puedeEditarContenido ? '' : 'none';
  document.getElementById('doc-lec-info').innerHTML = 'Curso ' + esc(c.estado) + ' · Tu rol: <strong>' + esc(c.miRol) + '</strong>' +
    (c.puedeEditarContenido ? ' · Ordena las lecciones con las flechas ↑ ↓' : ' · <span style="color:var(--clay)">Solo lectura</span>');
  try {
    DOC_LECCIONES = await pedirApi('GET', '/api/cursos/' + c.id + '/lecciones');
    pintarLeccionesDocente(c);
  } catch (err) { document.getElementById('doc-lec-lista').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; }
}

function pintarLeccionesDocente(c) {
  const editable = c.puedeEditarContenido;
  const lista = document.getElementById('doc-lec-lista');
  if (!DOC_LECCIONES.length) { lista.innerHTML = '<div class="empty-state">Este curso todavía no tiene lecciones.' + (editable ? ' Crea la primera con «+ Nueva lección».' : '') + '</div>'; return; }
  const vigentes = DOC_LECCIONES.filter(l => l.estado !== 'archivada');
  lista.innerHTML = DOC_LECCIONES.map(l => {
    const i = vigentes.findIndex(x => x.id === l.id);
    const pos = i >= 0 ? i + 1 : '—';
    const flechas = editable && i >= 0
      ? '<button class="btn btn-ghost btn-xs" title="Subir" ' + (i === 0 ? 'disabled' : '') + ' onclick="moverLeccion(' + l.id + ',-1)">↑</button>' +
        '<button class="btn btn-ghost btn-xs" title="Bajar" ' + (i === vigentes.length - 1 ? 'disabled' : '') + ' onclick="moverLeccion(' + l.id + ',1)">↓</button>' : '';
    const acciones = editable
      ? (l.estado !== 'archivada' ? '<button class="btn btn-ghost btn-xs" onclick="abrirModalLeccion(' + l.id + ')" title="Editar">✏️</button>' +
          '<button class="btn btn-ghost btn-xs" onclick="archivarLeccion(' + l.id + ')" title="Archivar (no se borra)" style="color:var(--clay)">🗄</button>' : '')
      : '';
    const botonesEstado = editable ? (DOC_ESTADO_LECCION_BOTON[l.estado] || []).map(([e, t, cls]) =>
      '<button class="btn ' + cls + ' btn-sm" onclick="cambiarEstadoLeccion(' + l.id + ',\'' + e + '\')">' + t + '</button>').join('') : '';
    return '<div class="card mb-14 doc-leccion" data-nombre="' + esc(l.nombre) + '"' + (l.estado === 'archivada' ? ' style="opacity:.6"' : '') + '>' +
      '<div class="flex items-center gap-10 mb-8">' + flechas +
      (l.imagenPortada ? '<img src="' + esc(urlCompleta(l.imagenPortada)) + '" style="width:52px;height:52px;object-fit:cover;border-radius:8px;flex-shrink:0" alt="">' : '') +
      '<div style="flex:1"><div class="doc-leccion-nombre" style="font-size:14.5px;font-weight:600">' + pos + '. ' + esc(l.nombre) + '</div>' +
      '<div class="text-sm muted">Tipo: ' + esc(LECCION_TIPO_LABEL[l.tipo] || l.tipo) + (l.prerequisito ? ' · Requiere: ' + esc(l.prerequisito) : '') + '</div></div>' +
      pill(l.estado) + acciones + '</div>' +
      (l.descripcion ? '<p class="text-sm muted mb-14">' + esc(l.descripcion) + '</p>' : '') +
      '<div class="flex gap-8 flex-wrap">' +
      '<button class="btn btn-ghost btn-sm" onclick="irA(\'materiales\',\'' + c.id + '\',\'' + l.id + '\')">📁 Materiales (' + l.materialesActivos + ' de ' + l.totalMateriales + ')</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="irSeccionSimulada(\'actividades\')">✅ Actividades</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="irSeccionSimulada(\'evaluaciones\')">📋 Evaluaciones</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="irSeccionSimulada(\'foros\')">💬 Foros</button>' +
      botonesEstado + '</div></div>';
  }).join('');
}

/* Actividades, evaluaciones y foros siguen con datos de demostración hasta sus módulos. */
function irSeccionSimulada(vista) {
  toast('Esta sección aún funciona con datos de demostración; se conectará en su propio módulo');
  goTo(vista);
}

async function moverLeccion(id, delta) {
  const ids = DOC_LECCIONES.filter(l => l.estado !== 'archivada').map(l => l.id);
  const i = ids.indexOf(id), j = i + delta;
  if (i < 0 || j < 0 || j >= ids.length) return;
  [ids[i], ids[j]] = [ids[j], ids[i]];
  try {
    await pedirApi('PUT', '/api/cursos/' + window.ctx_lec_curso + '/lecciones/orden', { lecciones: ids });
    toast('Orden actualizado');
  } catch (err) { toast(err.message, true); }
  await cargarLeccionesDocente();
}

async function cambiarEstadoLeccion(id, estado) {
  const l = DOC_LECCIONES.find(x => x.id === id);
  try {
    await pedirApi('PATCH', '/api/lecciones/' + id + '/estado', { estado });
    toast('Lección "' + l.nombre + '" ' + { activa: 'quedó activa', inactiva: 'quedó inactiva', archivada: 'quedó archivada', borrador: 'volvió a borrador' }[estado]);
  } catch (err) { toast(err.message, true); }
  await cargarLeccionesDocente();
}

function archivarLeccion(id) {
  const l = DOC_LECCIONES.find(x => x.id === id);
  confirmarAccion('¿Archivar <strong>' + esc(l.nombre) + '</strong>? Dejará de mostrarse a los estudiantes, pero no se borra: podrás restaurarla.', () => cambiarEstadoLeccion(id, 'archivada'), 'Archivar');
}

function abrirModalLeccion(id) {
  const l = id ? DOC_LECCIONES.find(x => x.id === id) : null;
  const vigentes = DOC_LECCIONES.filter(x => x.estado !== 'archivada');
  document.getElementById('leccion-modal-titulo').textContent = l ? 'Editar lección' : 'Nueva lección';
  document.getElementById('leccion-id').value = l ? l.id : '';
  document.getElementById('leccion-nombre').value = l ? l.nombre : '';
  document.getElementById('leccion-descripcion').value = l && l.descripcion ? l.descripcion : '';
  document.getElementById('leccion-tipo').value = l ? l.tipo : 'teoria';
  document.getElementById('leccion-orden').value = l ? (vigentes.findIndex(x => x.id === l.id) + 1) : vigentes.length + 1;
  setTimeout(function () { coverImgSetValue('leccion-imagen', l ? (l.imagenPortada || '') : ''); }, 0);
  const anteriores = vigentes.filter(x => !l || x.orden < l.orden);
  document.getElementById('leccion-prereq').innerHTML = '<option value="0">Sin prerequisito</option>' +
    anteriores.map(x => '<option value="' + x.id + '"' + (l && l.idPrerequisito === x.id ? ' selected' : '') + '>' + esc(x.nombre) + '</option>').join('');
  abrirModal('modal-leccion');
}

function renderModalLeccion() {
  if (document.getElementById('modal-leccion')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-leccion"><div class="modal"><div class="modal-header"><h3 id="leccion-modal-titulo">Nueva lección</h3><button class="modal-close" onclick="cerrarModal(\'modal-leccion\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="leccion-id">' +
    '<div class="campo"><label for="leccion-nombre">Nombre</label><input type="text" id="leccion-nombre" maxlength="150" placeholder="Ej: Mitosis y Meiosis"></div>' +
    '<div class="campo"><label for="leccion-descripcion">Descripción</label><textarea id="leccion-descripcion" rows="2"></textarea></div>' +
    '<div class="grid-2"><div class="campo"><label for="leccion-tipo">Tipo</label><select id="leccion-tipo"><option value="teoria">Teoría</option><option value="practica">Práctica</option><option value="evaluacion">Evaluación</option><option value="mixta">Mixta</option></select></div>' +
    '<div class="campo"><label for="leccion-orden">Orden</label><input type="number" id="leccion-orden" disabled><p class="hint">Cámbialo con las flechas ↑ ↓ de la lista.</p></div></div>' +
    '<div class="campo"><label for="leccion-prereq">Prerequisito</label><select id="leccion-prereq"></select><p class="hint">El estudiante debe completar esta lección antes de acceder. Solo se ofrecen lecciones anteriores.</p></div>' +
    coverImageFieldHTML('leccion-imagen', 'Imagen de portada', 'Aparece como miniatura junto al nombre de la lección en la lista.') +
    '<p class="text-sm muted">La lección se crea en borrador; actívala cuando tenga al menos un material activo.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-leccion\')">Cancelar</button><button class="btn btn-primary" id="leccion-modal-btn" onclick="guardarLeccion()">Guardar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function guardarLeccion() {
  const id = document.getElementById('leccion-id').value;
  const nombre = document.getElementById('leccion-nombre').value.trim();
  const cursoId = window.ctx_lec_curso;
  if (!nombre) { toast('Escribe el nombre de la lección', true); return; }
  if (!cursoId) { toast('Selecciona primero un curso', true); return; }
  const cuerpo = { nombre, descripcion: document.getElementById('leccion-descripcion').value.trim(), tipo: document.getElementById('leccion-tipo').value,
    imagenPortada: document.getElementById('leccion-imagen').value.trim(), idPrerequisito: Number(document.getElementById('leccion-prereq').value) };
  try {
    await pedirApi(id ? 'PATCH' : 'POST', id ? '/api/lecciones/' + id : '/api/cursos/' + cursoId + '/lecciones', cuerpo);
    cerrarModal('modal-leccion');
    toast(id ? 'Lección "' + nombre + '" actualizada' : 'Lección "' + nombre + '" creada en borrador');
    await cargarLeccionesDocente();
  } catch (err) { toast(err.message, true); }
}

/* ================================================================
   VISTA: MATERIALES (lista, formulario por tipo y vistas previas del prototipo original)
================================================================ */
function pickerMaterialesHTML(cursoId, leccionId, lecciones) {
  return '<div class="context-picker">' +
    '<select id="doc-mat-curso" onchange="setCtx(\'mat\',\'curso\',this.value)"><option value="">Selecciona un curso</option>' +
    DOC_CURSOS.map(c => '<option value="' + c.id + '"' + (String(c.id) === String(cursoId) ? ' selected' : '') + '>' + esc(c.nombre) + '</option>').join('') + '</select>' +
    '<select id="doc-mat-leccion" onchange="setCtx(\'mat\',\'leccion\',this.value)"' + (!cursoId ? ' disabled' : '') + '>' +
    '<option value="">' + (cursoId ? 'Selecciona una lección' : 'Primero elige un curso') + '</option>' +
    lecciones.map((l, i) => '<option value="' + l.id + '"' + (String(l.id) === String(leccionId) ? ' selected' : '') + '>' + (i + 1) + '. ' + esc(l.nombre) + '</option>').join('') +
    '</select></div>';
}

function renderMateriales(cont) {
  const cursoId = window.ctx_mat_curso || '', leccionId = window.ctx_mat_leccion || '';
  cont.innerHTML = '<div class="section-header"><div><h2>Materiales de contenido</h2><p class="subtitle">Lecturas, videos, laboratorios y juegos externos, documentos y artículos científicos.</p></div>' +
    '<button class="btn btn-primary" id="doc-mat-nuevo" onclick="abrirModalMaterial()" style="display:none">+ Nuevo material</button></div>' +
    '<div id="doc-mat-picker"></div><p class="context-hint" id="doc-mat-info"></p><div id="doc-mat-lista"></div>';
  renderModalMaterial();
  renderModalPreviewLectura();
  renderModalPreviewInteractivo();
  cargarCursosDocente().then(async () => {
    let lecciones = [];
    if (cursoId && cursoActualDoc(cursoId)) {
      DOC_LECCIONES = await pedirApi('GET', '/api/cursos/' + cursoId + '/lecciones');
      lecciones = DOC_LECCIONES.filter(l => l.estado !== 'archivada');
    }
    document.getElementById('doc-mat-picker').innerHTML = pickerMaterialesHTML(cursoId, leccionId, lecciones);
    if (!leccionId) { document.getElementById('doc-mat-lista').innerHTML = '<div class="empty-state">Elige un curso y una lección para ver sus materiales.</div>'; return; }
    await cargarMaterialesDocente();
  }).catch(err => { document.getElementById('doc-mat-lista').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; });
}

async function cargarMaterialesDocente() {
  const c = cursoActualDoc(window.ctx_mat_curso);
  try {
    DOC_LECCION_DET = await pedirApi('GET', '/api/lecciones/' + window.ctx_mat_leccion);
    const editable = c && c.puedeEditarContenido && DOC_LECCION_DET.estado !== 'archivada';
    document.getElementById('doc-mat-nuevo').style.display = editable ? '' : 'none';
    document.getElementById('doc-mat-info').innerHTML = 'Lección ' + esc(DOC_LECCION_DET.estado) + ' · ' + DOC_LECCION_DET.materialesActivos + ' materiales activos' +
      (DOC_LECCION_DET.estado === 'borrador' && DOC_LECCION_DET.materialesActivos > 0 && editable ? ' · Lista para activarse en la sección Lecciones' : '') +
      (c && !c.puedeEditarContenido ? ' · <span style="color:var(--clay)">Solo lectura</span>' : '');
    pintarMaterialesDocente(editable);
  } catch (err) { document.getElementById('doc-mat-lista').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; }
}

function pintarMaterialesDocente(editable) {
  const items = DOC_LECCION_DET.materiales;
  const lista = document.getElementById('doc-mat-lista');
  if (!items.length) { lista.innerHTML = '<div class="empty-state">Esta lección aún no tiene materiales.</div>'; return; }
  lista.innerHTML = items.map((m, i) => {
    const t = MATERIAL_TYPES[m.tipo];
    const esInteractivo = m.tipo === 'laboratorio' || m.tipo === 'juego' || m.tipo === 'video';
    let meta = t.label;
    if (m.proveedor) meta += ' · ' + esc(m.proveedor) + ' (externo)';
    if (m.autores) meta += ' · ' + esc(m.autores);
    if (m.duracionMin) meta += ' · ' + m.duracionMin + ' min estimados';
    if (recursosApoyo(m).length) meta += ' · ' + recursosApoyo(m).length + ' recurso(s) de apoyo';
    meta += ' · Creado por ' + esc(m.creadoPor);
    const estado = editable && m.estado !== 'archivado'
      ? '<button class="pill-toggle" onclick="toggleEstadoMaterial(' + m.id + ')" title="Cambiar estado">' + pill(m.estado) + '</button>'
      : pill(m.estado);
    return '<div class="material-row doc-material" data-nombre="' + esc(m.nombre) + '"' + (m.estado === 'archivado' ? ' style="opacity:.6"' : '') + '>' +
      '<div class="m-icon" style="color:' + t.color + '">' + t.icon + '</div>' +
      '<div class="m-info"><div class="m-nombre">' + esc(m.nombre) + '</div><div class="m-meta">' + meta + '</div></div>' +
      estado +
      '<button class="btn btn-ghost btn-xs" onclick="abrirVistaEstudiante(' + m.id + ',' + (i + 1) + ',' + items.length + ')">👁 Ver como estudiante</button>' +
      '<button class="btn btn-ghost btn-xs" onclick="abrirRecursosApoyo(' + m.id + ')" title="Recursos de apoyo (complementarios)">📎 ' + recursosApoyo(m).length + '</button>' +
      (m.url && !esInteractivo ? '<a href="' + esc(urlCompleta(m.url)) + '" target="_blank" rel="noopener" title="Abrir" style="color:var(--ink-soft)">↗</a>' : '') +
      (editable && m.estado !== 'archivado' ? '<button class="btn btn-ghost btn-xs" onclick="abrirModalMaterial(' + m.id + ')" title="Editar">✏️</button>' +
        '<button class="btn btn-ghost btn-xs" onclick="archivarMaterial(' + m.id + ')" title="Archivar (no se borra)" style="color:var(--clay)">🗄</button>' : '') +
      (editable && m.estado === 'archivado' ? '<button class="btn btn-ghost btn-xs" onclick="cambiarEstadoMaterial(' + m.id + ',\'activo\')">↩ Restaurar</button>' : '') +
      '</div>';
  }).join('');
}

function materialDoc(id) { return DOC_LECCION_DET.materiales.find(x => x.id === Number(id)); }

async function cambiarEstadoMaterial(id, estado) {
  try { await pedirApi('PATCH', '/api/materiales/' + id + '/estado', { estado }); toast('Material ' + estado); }
  catch (err) { toast(err.message, true); }
  await cargarMaterialesDocente();
}
function toggleEstadoMaterial(id) {
  const m = materialDoc(id);
  cambiarEstadoMaterial(id, m.estado === 'activo' ? 'inactivo' : 'activo');
}
function archivarMaterial(id) {
  const m = materialDoc(id);
  confirmarAccion('¿Archivar <strong>' + esc(m.nombre) + '</strong>? Dejará de mostrarse a los estudiantes, pero no se borra: podrás restaurarlo.', () => cambiarEstadoMaterial(id, 'archivado'), 'Archivar');
}

/* ---- vista previa: lectura (contenido del editor enriquecido) ---- */
function abrirLecturaPreview(id) {
  const m = materialDoc(id);
  document.getElementById('preview-lectura-titulo').textContent = m.nombre;
  document.getElementById('preview-lectura-body').innerHTML = m.contenido && m.contenido.trim() ? m.contenido : '<p class="muted">Este material todavía no tiene contenido de lectura.</p>';
  document.getElementById('preview-lectura-body').style.background = m.colorFondo || '#ffffff';
  document.getElementById('preview-lectura-apoyo').innerHTML = htmlApoyoEstudiante(m);
  abrirModal('modal-preview-lectura');
}
function renderModalPreviewLectura() {
  if (document.getElementById('modal-preview-lectura')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-preview-lectura"><div class="modal grande"><div class="modal-header"><h3>Vista previa del material</h3><button class="modal-close" onclick="cerrarModal(\'modal-preview-lectura\')">✕</button></div>' +
    '<div class="modal-body"><span class="pill pill-ambar">📖 Lectura</span>' +
    '<h2 style="margin:12px 0 16px" id="preview-lectura-titulo"></h2>' +
    '<div class="card" id="preview-lectura-body" style="font-size:14.5px;line-height:1.8"></div><div id="preview-lectura-apoyo"></div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se verá para el estudiante — usa este panel para confirmar que la información queda bien presentada antes de publicar.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-preview-lectura\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ---- vista previa: laboratorio / juego / video (incrustado) ---- */
function abrirInteractivePreview(id, index, total) {
  const m = materialDoc(id);
  const t = MATERIAL_TYPES[m.tipo];
  const etiqueta = m.tipo === 'laboratorio' ? 'Simulación interactiva' : m.tipo === 'juego' ? 'Juego interactivo' : 'Video incrustado';
  document.getElementById('preview-inter-titulo').textContent = m.nombre;
  document.getElementById('preview-inter-badge').innerHTML = t.icon + ' ' + t.label;
  document.getElementById('preview-inter-meta').innerHTML =
    (m.duracionMin ? '<span>⏱ ' + m.duracionMin + ' min estimados</span>' : '') +
    '<span>' + t.icon + ' Material ' + index + ' de ' + total + ' — Lección ' + esc(DOC_LECCION_DET.nombre) + '</span>' +
    '<span class="pill pill-azul">' + etiqueta + '</span>';
  document.getElementById('preview-inter-frame-wrap').style.display = 'none';
  document.getElementById('preview-inter-launch').style.display = 'block';
  document.getElementById('preview-inter-launch-msg').textContent = m.proveedor ? 'Contenido alojado en ' + m.proveedor : (m.tipo === 'video' ? 'Video de YouTube' : 'Contenido interactivo externo');
  const btnLaunch = document.getElementById('preview-inter-launch-btn');
  btnLaunch.disabled = !m.url;
  btnLaunch.textContent = '▶ ' + (m.tipo === 'video' ? 'Reproducir video' : 'Iniciar simulación');
  btnLaunch.onclick = function () {
    if (!m.url) return;
    document.getElementById('preview-inter-frame-wrap').style.display = 'block';
    document.getElementById('preview-inter-launch').style.display = 'none';
    document.getElementById('preview-inter-frame-url').textContent = m.url;
    document.getElementById('preview-inter-iframe').src = m.url;
  };
  document.getElementById('preview-inter-sin-url').style.display = m.url ? 'none' : 'block';
  document.getElementById('preview-inter-guia-wrap').style.display = (m.guia && m.guia.trim()) ? 'block' : 'none';
  document.getElementById('preview-inter-guia').innerHTML = m.guia || '';
  document.getElementById('preview-inter-nueva').href = m.url || '#';
  document.getElementById('preview-inter-nueva').style.display = m.url ? '' : 'none';
  document.getElementById('preview-inter-apoyo').innerHTML = htmlApoyoEstudiante(m);
  abrirModal('modal-preview-interactivo');
}
function renderModalPreviewInteractivo() {
  if (document.getElementById('modal-preview-interactivo')) return;
  const cerrar = 'cerrarModal(\'modal-preview-interactivo\');document.getElementById(\'preview-inter-iframe\').src=\'\'';
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-preview-interactivo"><div class="modal grande"><div class="modal-header"><h3>Vista previa del material</h3><button class="modal-close" onclick="' + cerrar + '">✕</button></div>' +
    '<div class="modal-body"><span class="pill pill-azul" id="preview-inter-badge"></span>' +
    '<h2 style="margin:10px 0 6px" id="preview-inter-titulo"></h2>' +
    '<div class="flex gap-14 flex-wrap text-sm muted mb-14" id="preview-inter-meta"></div>' +
    '<div id="preview-inter-launch" style="background:linear-gradient(135deg,#1E3345,#2E5588);border-radius:14px;padding:48px 24px;text-align:center">' +
      '<div style="width:90px;height:90px;border-radius:50%;background:radial-gradient(circle,#5B8FD9,#2E5588);margin:0 auto 18px"></div>' +
      '<p style="color:#fff;font-size:14.5px;margin-bottom:18px" id="preview-inter-launch-msg"></p>' +
      '<button class="btn" id="preview-inter-launch-btn" style="background:#fff;color:#1E3345;border-radius:30px;padding:12px 26px">▶ Iniciar</button>' +
      '<p id="preview-inter-sin-url" style="display:none;color:#F4E1DA;font-size:12px;margin-top:12px">Todavía no se ha configurado el enlace de este material.</p>' +
    '</div>' +
    '<div id="preview-inter-frame-wrap" style="display:none;border-radius:14px;overflow:hidden;border:1px solid var(--line)">' +
      '<div style="background:var(--blue-deep);color:#fff;font-size:12px;padding:8px 14px;display:flex;justify-content:space-between;gap:12px"><span id="preview-inter-frame-url" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap"></span>' +
      '<a id="preview-inter-nueva" target="_blank" rel="noopener" style="color:#fff;white-space:nowrap">↗ Abrir en una pestaña nueva</a>' +
      '<button onclick="document.getElementById(\'preview-inter-frame-wrap\').style.display=\'none\';document.getElementById(\'preview-inter-launch\').style.display=\'block\';document.getElementById(\'preview-inter-iframe\').src=\'\'" style="background:none;border:none;color:#fff;cursor:pointer;font-size:12px">Cerrar ventana</button></div>' +
      '<iframe id="preview-inter-iframe" style="width:100%;height:420px;border:none;background:#fff" allow="accelerometer; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe>' +
      '<p class="text-sm muted" style="padding:8px 14px;margin:0;background:var(--panel)">Si el contenido no aparece, el proveedor no permite mostrarlo dentro de otras páginas con ese enlace: edita el material y pega el <strong>código de inserción</strong> del proveedor (en Wordwall: Compartir → Insertar), o ábrelo en una pestaña nueva.</p></div>' +
    '<div id="preview-inter-guia-wrap" style="display:none;margin-top:22px">' +
      '<div style="font-size:12.5px;font-weight:700;color:var(--sage);text-transform:uppercase;letter-spacing:.4px;margin-bottom:10px">Guía de observación</div>' +
      '<div class="card" id="preview-inter-guia"></div></div><div id="preview-inter-apoyo"></div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se verá para el estudiante — el contenido se incrusta dentro de la plataforma cuando el estudiante presiona iniciar.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="' + cerrar + '">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ---- formulario crear/editar material (diseño original, un conjunto de campos por tipo) ---- */
function abrirModalMaterial(id) {
  const m = id ? materialDoc(id) : null;
  const val = (k, v) => { document.getElementById(k).value = v == null ? '' : v; };
  document.getElementById('mat-modal-titulo').textContent = m ? 'Editar material' : 'Nuevo material';
  val('mat-id', m ? m.id : '');
  val('mat-nombre', m ? m.nombre : '');
  val('mat-descripcion', m ? m.descripcion : '');
  val('mat-duracion', m ? m.duracionMin : '');
  val('mat-proveedor', m ? m.proveedor : '');
  val('mat-url', m && (m.tipo === 'laboratorio' || m.tipo === 'juego') ? m.url : '');
  val('mat-url-video', m && m.tipo === 'video' ? m.url : '');
  val('mat-url-articulo', m && m.tipo === 'articulo' ? m.url : '');
  val('mat-autores', m ? m.autores : '');
  val('mat-revista', m ? m.revista : '');
  val('mat-doi', m ? m.doi : '');
  window._matArchivoUrl = m && m.tipo === 'documento' ? (m.url || '') : '';
  document.getElementById('mat-archivo-input').value = '';
  document.getElementById('mat-archivo-label').innerHTML = window._matArchivoUrl ? '<a href="' + esc(urlCompleta(window._matArchivoUrl)) + '" target="_blank" rel="noopener">Documento actual ↗</a>' : '';
  document.getElementById('mat-fondo').value = m && m.colorFondo ? m.colorFondo : '#ffffff';
  // El tipo no cambia al editar: sus datos viven en tablas distintas (recurso, articulo_cientifico)
  document.querySelectorAll('#modal-material .tipo-btn').forEach(b => { b.disabled = !!m && b.dataset.tipo !== m.tipo; });
  seleccionarTipoMaterial(m ? m.tipo : 'lectura');
  setTimeout(function () {
    rteSetValue('mat-editor', m ? (m.contenido || '') : '');
    rteSetValue('mat-guia', m ? (m.guia || '') : '');
    document.getElementById('mat-editor').style.background = m && m.colorFondo ? m.colorFondo : '#ffffff';
  }, 0);
  abrirModal('modal-material');
}

function seleccionarTipoMaterial(tipo) {
  document.getElementById('mat-tipo').value = tipo;
  document.querySelectorAll('#modal-material .tipo-btn').forEach(b => {
    const activo = b.dataset.tipo === tipo;
    b.classList.toggle('activo', activo);
    b.style.borderColor = activo ? MATERIAL_TYPES[b.dataset.tipo].color : '';
    b.style.color = activo ? MATERIAL_TYPES[b.dataset.tipo].color : '';
  });
  const esLabJuegoVideo = tipo === 'laboratorio' || tipo === 'juego' || tipo === 'video';
  const esLabJuego = tipo === 'laboratorio' || tipo === 'juego';
  document.getElementById('mat-campo-duracion').style.display = esLabJuegoVideo ? 'block' : 'none';
  document.getElementById('mat-campo-externo').style.display = esLabJuego ? 'block' : 'none';
  document.getElementById('mat-campo-video-url').style.display = tipo === 'video' ? 'block' : 'none';
  document.getElementById('mat-campo-guia').style.display = esLabJuegoVideo ? 'block' : 'none';
  document.getElementById('mat-campo-articulo').style.display = tipo === 'articulo' ? 'block' : 'none';
  document.getElementById('mat-campo-lectura').style.display = tipo === 'lectura' ? 'block' : 'none';
  document.getElementById('mat-campo-documento').style.display = tipo === 'documento' ? 'block' : 'none';
}

async function subirArchivoMaterial(input) {
  if (!input.files.length) return;
  const archivo = input.files[0];
  try {
    window._matArchivoUrl = await subirArchivoDocente(archivo);
    document.getElementById('mat-archivo-label').textContent = archivo.name + ' (subido)';
    if (!document.getElementById('mat-nombre').value.trim()) document.getElementById('mat-nombre').value = archivo.name.replace(/\.pdf$/i, '');
    toast('Archivo subido');
  } catch (err) { toast(err.message, true); input.value = ''; }
}

function renderModalMaterial() {
  if (document.getElementById('modal-material')) return;
  const tipoBtns = Object.entries(MATERIAL_TYPES).map(([k, v]) =>
    '<button type="button" class="tipo-btn" data-tipo="' + k + '" onclick="seleccionarTipoMaterial(\'' + k + '\')"><span class="m-icon-lg">' + v.icon + '</span>' + v.label + '</button>').join('');
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-material"><div class="modal grande"><div class="modal-header"><h3 id="mat-modal-titulo">Nuevo material</h3><button class="modal-close" onclick="cerrarModal(\'modal-material\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="mat-id"><input type="hidden" id="mat-tipo" value="lectura">' +
    '<div class="campo"><label>Tipo de material</label><div class="tipo-selector">' + tipoBtns + '</div></div>' +
    '<div class="campo"><label for="mat-nombre">Nombre</label><input type="text" id="mat-nombre" maxlength="250" placeholder="Ej: Las leyes de Mendel"></div>' +
    '<div class="campo"><label for="mat-descripcion">Descripción</label><textarea id="mat-descripcion" rows="2"></textarea></div>' +
    '<div id="mat-campo-duracion" class="campo" style="display:none"><label for="mat-duracion">Duración estimada (minutos)</label><input type="number" id="mat-duracion" min="1" max="1000" placeholder="15"></div>' +
    '<div id="mat-campo-externo" style="display:none">' +
      '<div class="campo"><label for="mat-proveedor">Proveedor externo</label><input type="text" id="mat-proveedor" maxlength="200" placeholder="Ej: Labster, PhET, Kahoot..."></div>' +
      '<div class="campo"><label for="mat-url">Enlace o código de inserción</label><input type="text" id="mat-url" placeholder="https://... o &lt;iframe src=&quot;...&quot;&gt;"><p class="hint">El estudiante lo verá incrustado dentro de la plataforma. Para Wordwall, Genially, Kahoot o Educaplay pega el <strong>código de inserción</strong> (en Wordwall: Compartir → Insertar): el enlace normal de esas páginas no se deja mostrar dentro de otra página.</p></div>' +
    '</div>' +
    '<div id="mat-campo-video-url" class="campo" style="display:none"><label for="mat-url-video">Enlace del video de YouTube</label><input type="url" id="mat-url-video" placeholder="https://www.youtube.com/watch?v=..."><p class="hint">Se incrusta dentro de la plataforma. Acepta enlaces youtube.com/watch, youtu.be y shorts.</p></div>' +
    '<div id="mat-campo-guia" class="campo" style="display:none"><label>Guía de observación (opcional)</label>' + richTextEditorHTML('mat-guia', '1. Observa... 2. Anota... 3. Responde...') + '</div>' +
    '<div id="mat-campo-articulo" style="display:none">' +
      '<div class="campo"><label for="mat-autores">Autores</label><input type="text" id="mat-autores" maxlength="500"></div>' +
      '<div class="grid-2"><div class="campo"><label for="mat-revista">Revista</label><input type="text" id="mat-revista" maxlength="250"></div><div class="campo"><label for="mat-doi">DOI</label><input type="text" id="mat-doi" maxlength="100"></div></div>' +
      '<div class="campo"><label for="mat-url-articulo">Enlace</label><input type="url" id="mat-url-articulo" placeholder="https://..."></div>' +
    '</div>' +
    '<div id="mat-campo-lectura" style="display:none">' +
      '<div class="campo"><label for="mat-fondo">Color de fondo de la lectura (opcional)</label>' +
      '<div class="flex items-center gap-10"><input type="color" id="mat-fondo" value="#ffffff" onchange="document.getElementById(\'mat-editor\').style.background=this.value">' +
      '<button type="button" class="btn btn-ghost btn-sm" onclick="document.getElementById(\'mat-fondo\').value=\'#ffffff\';document.getElementById(\'mat-editor\').style.background=\'#ffffff\'">Restablecer a blanco</button></div></div>' +
      '<div class="campo"><label>Contenido de la lectura</label><p class="hint" style="margin:0 0 6px">El estudiante lee este contenido directamente dentro de la plataforma. Usa "Vista previa" (ícono del ojo) para ver cómo quedará.</p>' + richTextEditorHTML('mat-editor', 'Escribe aquí el texto de la lectura...') + '</div>' +
    '</div>' +
    '<div id="mat-campo-documento" class="campo" style="display:none"><label for="mat-archivo-input">⬆ Documento (PDF)</label><input type="file" id="mat-archivo-input" accept=".pdf" onchange="subirArchivoMaterial(this)"> <span class="text-sm muted" id="mat-archivo-label"></span><p class="hint">PDF de máximo 10 MB; se valida el contenido real del archivo.</p></div>' +
    '</div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-material\')">Cancelar</button><button class="btn btn-primary" id="mat-modal-btn" onclick="guardarMaterial()">Guardar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

async function guardarMaterial() {
  const id = document.getElementById('mat-id').value;
  const val = k => document.getElementById(k).value.trim();
  const nombre = val('mat-nombre');
  const leccionId = window.ctx_mat_leccion;
  if (!nombre) { toast('Escribe el nombre del material', true); return; }
  if (!leccionId) { toast('Selecciona primero una lección', true); return; }
  const tipo = val('mat-tipo');
  const cuerpo = { nombre, tipo, descripcion: val('mat-descripcion') };
  if (tipo === 'lectura') { cuerpo.contenido = rteGetValue('mat-editor'); cuerpo.colorFondo = val('mat-fondo'); }
  if (tipo === 'video' || tipo === 'laboratorio' || tipo === 'juego') {
    cuerpo.guia = rteGetValue('mat-guia');
    cuerpo.duracionMin = val('mat-duracion') ? Number(val('mat-duracion')) : null;
    cuerpo.url = tipo === 'video' ? val('mat-url-video') : extraerUrlInsercion(val('mat-url'));
  }
  if (tipo === 'laboratorio' || tipo === 'juego') cuerpo.proveedor = val('mat-proveedor');
  if (tipo === 'documento') cuerpo.url = window._matArchivoUrl || '';
  if (tipo === 'articulo') { cuerpo.autores = val('mat-autores'); cuerpo.revista = val('mat-revista'); cuerpo.doi = val('mat-doi'); cuerpo.url = val('mat-url-articulo'); }
  if (!id && tipo === 'articulo' && !cuerpo.autores) { toast('Escribe los autores del artículo', true); return; }
  if (!id && tipo === 'documento' && !cuerpo.url) { toast('Sube el documento PDF', true); return; }
  try {
    await pedirApi(id ? 'PATCH' : 'POST', id ? '/api/materiales/' + id : '/api/lecciones/' + leccionId + '/materiales', cuerpo);
    cerrarModal('modal-material');
    toast(id ? 'Material "' + nombre + '" actualizado' : 'Material "' + nombre + '" agregado');
    await cargarMaterialesDocente();
  } catch (err) { toast(err.message, true); }
}

/* ================================================================
   VISTA DEL ESTUDIANTE PARA TODOS LOS TIPOS + RECURSOS DE APOYO
================================================================ */
/** Si se pega un código de inserción (<iframe src="...">), toma solo la dirección. */
function extraerUrlInsercion(valor) {
  const m = String(valor || '').match(/<iframe[^>]*\ssrc=["']([^"']+)["']/i);
  return (m ? m[1] : String(valor || '')).trim().replace(/&amp;/g, '&');
}

/** Recursos de apoyo del material: todos menos el recurso principal. */
function recursosApoyo(m) { return (m.recursos || []).filter(r => r.tipoContenido !== 'primario'); }

const ICONO_RECURSO = { video: '🎬', documento: '📄', imagen: '🖼', audio: '🎧', enlace: '🔗', interactivo: '🧩' };

/** Bloque «Recursos de apoyo» tal como lo verá el estudiante (solo los activos). */
function htmlApoyoEstudiante(m) {
  const activos = recursosApoyo(m).filter(r => r.estado === 'activo');
  if (!activos.length) return '';
  return '<div class="apoyo-estudiante" style="margin-top:22px"><div style="font-size:12.5px;font-weight:700;color:var(--sage);text-transform:uppercase;letter-spacing:.4px;margin-bottom:10px">Recursos de apoyo</div>' +
    activos.map(r => '<div class="card mb-8" style="display:flex;align-items:center;gap:12px;padding:12px 14px">' +
      '<span style="font-size:20px">' + (ICONO_RECURSO[r.tipoRecurso] || '📎') + '</span>' +
      '<div style="flex:1"><div style="font-weight:600">' + esc(r.nombre) + '</div>' +
      '<div class="text-sm muted">' + esc(r.tipoRecurso) + (r.duracionMin ? ' · ' + r.duracionMin + ' min' : '') + (r.nivelAcademico ? ' · nivel ' + esc(r.nivelAcademico) : '') + (r.descripcion ? ' · ' + esc(r.descripcion) : '') + '</div></div>' +
      (r.tipoRecurso === 'imagen' ? '<img src="' + esc(urlCompleta(r.url)) + '" style="height:46px;border-radius:6px" alt="">' : '') +
      '<a class="btn btn-ghost btn-xs" href="' + esc(urlCompleta(r.url)) + '" target="_blank" rel="noopener">' + (r.tipoRecurso === 'video' ? '▶ Ver' : '↗ Abrir') + '</a></div>').join('') + '</div>';
}

/** «Ver como estudiante» para cualquier tipo de material. */
function abrirVistaEstudiante(id, index, total) {
  const m = materialDoc(id);
  if (m.tipo === 'lectura') return abrirLecturaPreview(id);
  if (m.tipo === 'video' || m.tipo === 'laboratorio' || m.tipo === 'juego') return abrirInteractivePreview(id, index, total);
  if (m.tipo === 'articulo') return abrirArticuloPreview(id);
  return abrirDocumentoPreview(id);
}

function abrirArticuloPreview(id) {
  const m = materialDoc(id);
  renderModalPreviewSimple();
  // Una sección de color por parte del artículo, con la misma paleta de los bloques del editor
  const seccion = (icono, titulo, contenido, bg, borde) =>
    '<div style="background:' + bg + ';border-left:4px solid ' + borde + ';border-radius:10px;padding:14px 18px">' +
    '<div style="color:' + borde + ';font-weight:800;font-size:11px;letter-spacing:.5px;text-transform:uppercase;margin-bottom:4px">' + icono + ' ' + titulo + '</div>' +
    '<div style="font-size:14px;line-height:1.6;color:#1F2A33">' + contenido + '</div></div>';
  document.getElementById('preview-simple-badge').style.display = 'none'; // el encabezado de color ya lo indica
  document.getElementById('preview-simple-titulo').textContent = '';
  document.getElementById('preview-simple-body').innerHTML =
    '<div class="articulo-estudiante" style="border-radius:14px;overflow:hidden;border:1px solid var(--line)">' +
      '<div style="background:linear-gradient(135deg,#1E3345,#33526E);padding:24px 26px">' +
        '<div style="color:#C08A3E;font-weight:800;font-size:11px;letter-spacing:.6px;text-transform:uppercase;margin-bottom:8px">🔬 Artículo científico</div>' +
        '<div style="color:#fff;font-family:Fraunces,Georgia,serif;font-size:24px;font-weight:700;line-height:1.25">' + esc(m.nombre) + '</div>' +
        (m.autores ? '<div style="color:#D6E0E8;font-size:13.5px;margin-top:8px">' + esc(m.autores) + '</div>' : '') +
      '</div>' +
      '<div style="padding:18px;display:grid;gap:12px;background:#fff">' +
        '<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px">' +
          seccion('👥', 'Autores', esc(m.autores || '—'), '#E4EBF0', '#33526E') +
          seccion('📰', 'Revista', m.revista ? '<em>' + esc(m.revista) + '</em>' : '<span style="color:#5B6875">No indicada</span>', '#E5EDE6', '#5C7A63') +
          seccion('🔗', 'DOI', m.doi ? '<a href="https://doi.org/' + esc(m.doi) + '" target="_blank" rel="noopener" style="color:#8A5F22;font-weight:600">' + esc(m.doi) + '</a>' : '<span style="color:#5B6875">Sin DOI</span>', '#F3E7D2', '#C08A3E') +
        '</div>' +
        (m.descripcion ? seccion('📝', 'Resumen', esc(m.descripcion), '#EDE5F5', '#7A5AA8') : '') +
        '<div>' + (m.url ? '<a class="btn btn-primary" href="' + esc(m.url) + '" target="_blank" rel="noopener">↗ Leer el artículo completo</a>' : '<span class="muted text-sm">El artículo no tiene enlace.</span>') + '</div>' +
      '</div>' +
    '</div>' + htmlApoyoEstudiante(m);
  abrirModal('modal-preview-simple');
}

function abrirDocumentoPreview(id) {
  const m = materialDoc(id);
  renderModalPreviewSimple();
  document.getElementById('preview-simple-badge').style.display = '';
  document.getElementById('preview-simple-badge').innerHTML = '📄 Documento';
  document.getElementById('preview-simple-titulo').textContent = m.nombre;
  document.getElementById('preview-simple-body').innerHTML =
    (m.descripcion ? '<p class="text-sm mb-14">' + esc(m.descripcion) + '</p>' : '') +
    (m.url
      ? '<div style="border:1px solid var(--line);border-radius:12px;overflow:hidden"><iframe id="preview-simple-pdf" src="' + esc(urlCompleta(m.url)) + '" style="width:100%;height:460px;border:none"></iframe></div>' +
        '<a class="btn btn-ghost btn-sm" style="margin-top:10px" href="' + esc(urlCompleta(m.url)) + '" target="_blank" rel="noopener">↗ Abrir en una pestaña nueva</a>'
      : '<p class="muted">Este documento todavía no tiene archivo.</p>') + htmlApoyoEstudiante(m);
  abrirModal('modal-preview-simple');
}

function renderModalPreviewSimple() {
  if (document.getElementById('modal-preview-simple')) return;
  const cerrar = 'cerrarModal(\'modal-preview-simple\');document.getElementById(\'preview-simple-body\').innerHTML=\'\'';
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-preview-simple"><div class="modal grande"><div class="modal-header"><h3>Vista previa del material</h3><button class="modal-close" onclick="' + cerrar + '">✕</button></div>' +
    '<div class="modal-body"><span class="pill pill-azul" id="preview-simple-badge"></span><h2 style="margin:12px 0 16px" id="preview-simple-titulo"></h2><div id="preview-simple-body"></div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se verá para el estudiante.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="' + cerrar + '">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ---- gestión de los recursos de apoyo de un material ---- */
let APOYO_MATERIAL_ID = null;

function abrirRecursosApoyo(id) {
  APOYO_MATERIAL_ID = id;
  renderModalApoyo();
  pintarRecursosApoyo();
  limpiarFormularioApoyo();
  abrirModal('modal-doc-apoyo');
}

function pintarRecursosApoyo() {
  const m = materialDoc(APOYO_MATERIAL_ID);
  const c = cursoActualDoc(window.ctx_mat_curso);
  const editable = c && c.puedeEditarContenido && m.estado !== 'archivado';
  document.getElementById('apoyo-titulo').textContent = 'Recursos de apoyo — ' + m.nombre;
  document.getElementById('apoyo-form').style.display = editable ? 'block' : 'none';
  const lista = recursosApoyo(m);
  document.getElementById('apoyo-lista').innerHTML = lista.length ? lista.map(r =>
    '<div class="flex items-center gap-10 apoyo-item" data-nombre="' + esc(r.nombre) + '" style="padding:10px 0;border-bottom:1px solid var(--line)' + (r.estado !== 'activo' ? ';opacity:.55' : '') + '">' +
    '<span style="font-size:18px">' + (ICONO_RECURSO[r.tipoRecurso] || '📎') + '</span>' +
    '<div style="flex:1;min-width:0"><div style="font-weight:600">' + esc(r.nombre) + '</div><div class="text-sm muted" style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + esc(r.tipoRecurso) +
    (r.duracionMin ? ' · ' + r.duracionMin + ' min' : '') + (r.nivelAcademico ? ' · ' + esc(r.nivelAcademico) : '') + ' · ' + esc(r.url) + '</div></div>' +
    pill(r.estado) +
    (editable ? '<button class="btn btn-ghost btn-xs" onclick="editarRecursoApoyo(' + r.id + ')" title="Editar">✏️</button>' +
      '<button class="btn btn-ghost btn-xs" onclick="cambiarEstadoRecursoApoyo(' + r.id + ',\'' + (r.estado === 'activo' ? 'inactivo' : 'activo') + '\')">' + (r.estado === 'activo' ? 'Inactivar' : 'Activar') + '</button>' : '') +
    '</div>').join('') : '<p class="muted text-sm">Este material todavía no tiene recursos de apoyo.</p>';
}

function renderModalApoyo() {
  if (document.getElementById('modal-doc-apoyo')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-doc-apoyo"><div class="modal grande"><div class="modal-header"><h3 id="apoyo-titulo">Recursos de apoyo</h3><button class="modal-close" onclick="cerrarModal(\'modal-doc-apoyo\')">✕</button></div>' +
    '<div class="modal-body"><p class="text-sm muted">Recursos complementarios que el estudiante verá junto al material: videos, documentos, imágenes, enlaces o actividades interactivas.</p>' +
    '<div id="apoyo-lista" class="mb-14"></div>' +
    '<div id="apoyo-form" class="card"><h4 id="apoyo-form-titulo" style="margin:0 0 10px;font-size:14px">Agregar recurso de apoyo</h4><input type="hidden" id="apoyo-id">' +
    '<div class="grid-2"><div class="campo"><label for="apoyo-nombre">Nombre</label><input id="apoyo-nombre" maxlength="200"></div>' +
    '<div class="campo"><label for="apoyo-tipo">Tipo</label><select id="apoyo-tipo" onchange="ayudaApoyo()"><option value="enlace">Enlace</option><option value="video">Video de YouTube</option><option value="documento">Documento (PDF)</option><option value="imagen">Imagen</option><option value="audio">Audio</option><option value="interactivo">Interactivo</option></select></div></div>' +
    '<div class="campo"><label for="apoyo-url">URL</label><input id="apoyo-url" maxlength="1000" placeholder="https://...">' +
    '<div id="apoyo-archivo-wrap" style="display:none;margin-top:6px"><input type="file" id="apoyo-archivo" accept=".pdf,.png,.jpg,.jpeg" onchange="subirArchivoApoyo(this)"></div><p class="hint" id="apoyo-ayuda"></p></div>' +
    '<div class="grid-2"><div class="campo"><label for="apoyo-duracion">Duración (min)</label><input type="number" id="apoyo-duracion" min="1" max="1000"></div>' +
    '<div class="campo"><label for="apoyo-nivel">Nivel</label><select id="apoyo-nivel"><option value="">Sin nivel</option><option value="basico">Básico</option><option value="intermedio">Intermedio</option><option value="avanzado">Avanzado</option></select></div></div>' +
    '<div class="campo"><label for="apoyo-descripcion">Descripción (opcional)</label><input id="apoyo-descripcion"></div>' +
    '<div class="flex gap-8"><button class="btn btn-primary btn-sm" id="apoyo-guardar" onclick="guardarRecursoApoyo()">Agregar recurso</button>' +
    '<button class="btn btn-ghost btn-sm" id="apoyo-cancelar" onclick="limpiarFormularioApoyo()" style="display:none">Cancelar edición</button></div></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-doc-apoyo\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

function ayudaApoyo() {
  const tipo = document.getElementById('apoyo-tipo').value;
  document.getElementById('apoyo-archivo-wrap').style.display = (tipo === 'documento' || tipo === 'imagen') ? 'block' : 'none';
  document.getElementById('apoyo-ayuda').textContent = tipo === 'video' ? 'Solo videos de YouTube; se mostrarán embebidos.'
    : (tipo === 'documento' || tipo === 'imagen') ? 'Pega una URL o sube un archivo (PDF, PNG o JPG de máximo 10 MB).'
    : tipo === 'interactivo' ? 'Enlace o código de inserción del proveedor (Wordwall, Genially, PhET...).' : 'URL que empiece por http:// o https://';
}

function limpiarFormularioApoyo() {
  ['apoyo-id', 'apoyo-nombre', 'apoyo-url', 'apoyo-duracion', 'apoyo-descripcion'].forEach(k => document.getElementById(k).value = '');
  document.getElementById('apoyo-tipo').value = 'enlace';
  document.getElementById('apoyo-nivel').value = '';
  document.getElementById('apoyo-archivo').value = '';
  document.getElementById('apoyo-form-titulo').textContent = 'Agregar recurso de apoyo';
  document.getElementById('apoyo-guardar').textContent = 'Agregar recurso';
  document.getElementById('apoyo-cancelar').style.display = 'none';
  ayudaApoyo();
}

function editarRecursoApoyo(idRecurso) {
  const r = recursosApoyo(materialDoc(APOYO_MATERIAL_ID)).find(x => x.id === idRecurso);
  document.getElementById('apoyo-id').value = r.id;
  document.getElementById('apoyo-nombre').value = r.nombre;
  document.getElementById('apoyo-tipo').value = r.tipoRecurso;
  document.getElementById('apoyo-url').value = r.url;
  document.getElementById('apoyo-duracion').value = r.duracionMin || '';
  document.getElementById('apoyo-nivel').value = r.nivelAcademico || '';
  document.getElementById('apoyo-descripcion').value = r.descripcion || '';
  document.getElementById('apoyo-form-titulo').textContent = 'Editar recurso de apoyo';
  document.getElementById('apoyo-guardar').textContent = 'Guardar cambios';
  document.getElementById('apoyo-cancelar').style.display = '';
  ayudaApoyo();
}

async function subirArchivoApoyo(input) {
  if (!input.files.length) return;
  try {
    document.getElementById('apoyo-url').value = await subirArchivoDocente(input.files[0]);
    if (!document.getElementById('apoyo-nombre').value.trim()) document.getElementById('apoyo-nombre').value = input.files[0].name;
    toast('Archivo subido');
  } catch (err) { toast(err.message, true); input.value = ''; }
}

async function guardarRecursoApoyo() {
  const val = k => document.getElementById(k).value.trim();
  const id = val('apoyo-id');
  const cuerpo = { nombre: val('apoyo-nombre'), tipoRecurso: val('apoyo-tipo'), url: extraerUrlInsercion(val('apoyo-url')),
    duracionMin: val('apoyo-duracion') ? Number(val('apoyo-duracion')) : null, nivelAcademico: val('apoyo-nivel'), descripcion: val('apoyo-descripcion') };
  if (!cuerpo.nombre || !cuerpo.url) { toast('Nombre y URL son obligatorios', true); return; }
  try {
    await pedirApi(id ? 'PATCH' : 'POST', id ? '/api/recursos/' + id : '/api/materiales/' + APOYO_MATERIAL_ID + '/recursos', cuerpo);
    toast(id ? 'Recurso "' + cuerpo.nombre + '" actualizado' : 'Recurso "' + cuerpo.nombre + '" agregado');
    await cargarMaterialesDocente();
    pintarRecursosApoyo();
    limpiarFormularioApoyo();
  } catch (err) { toast(err.message, true); }
}

async function cambiarEstadoRecursoApoyo(idRecurso, estado) {
  try { await pedirApi('PATCH', '/api/recursos/' + idRecurso + '/estado', { estado }); toast('Recurso ' + estado); }
  catch (err) { toast(err.message, true); }
  await cargarMaterialesDocente();
  pintarRecursosApoyo();
}

/* ================================================================
   EDITOR: AGREGAR Y QUITAR PUNTOS EN EL BLOQUE «PUNTOS CLAVE»
   El bloque es una tabla de filas numeradas. Al hacer clic dentro aparece una barra flotante
   (fuera del contenido, así no se guarda con la lectura) para agregar o quitar puntos.
================================================================ */
(function () {
  const original = RTE_BLOQUES.puntosclave;
  RTE_BLOQUES.puntosclave = (bg, borde) => original(bg, borde).replace('<table style=', '<table class="rte-puntos" style=');
})();

let PUNTOS_CTX = null;

function esTablaPuntos(t) {
  if (!t || t.tagName !== 'TABLE') return false;
  if (t.classList.contains('rte-puntos')) return true;
  const previo = t.previousElementSibling; // bloques creados antes de esta versión
  return !!(previo && /puntos clave/i.test(previo.textContent) && t.rows.length && t.rows[0].cells.length === 2);
}

function barraPuntos() {
  let barra = document.getElementById('rte-puntos-bar');
  if (!barra) {
    barra = document.createElement('div');
    barra.id = 'rte-puntos-bar';
    barra.style.cssText = 'position:fixed;z-index:3000;display:none;gap:6px;background:#fff;border:1px solid #DFE2E1;border-radius:10px;padding:6px;box-shadow:0 6px 18px rgba(27,50,44,.18)';
    barra.innerHTML =
      '<button type="button" class="btn btn-ghost btn-xs" id="rte-punto-agregar" onmousedown="event.preventDefault()" onclick="agregarPuntoClave()">＋ Agregar punto</button>' +
      '<button type="button" class="btn btn-ghost btn-xs" id="rte-punto-quitar" onmousedown="event.preventDefault()" onclick="quitarPuntoClave()" style="color:var(--clay)">－ Quitar este punto</button>';
    document.body.appendChild(barra);
  }
  return barra;
}

function ubicarBarraPuntos() {
  const barra = barraPuntos();
  if (!PUNTOS_CTX || !document.body.contains(PUNTOS_CTX.fila)) { barra.style.display = 'none'; return; }
  const r = PUNTOS_CTX.fila.getBoundingClientRect();
  barra.style.display = 'flex';
  barra.style.top = Math.max(8, r.top - 40) + 'px';
  barra.style.left = Math.max(8, r.right - barra.offsetWidth) + 'px';
}

function revisarPuntosClave(e) {
  const editor = e.target.closest && e.target.closest('.rte-editor');
  if (!editor) { if (!e.target.closest || !e.target.closest('#rte-puntos-bar')) { PUNTOS_CTX = null; barraPuntos().style.display = 'none'; } return; }
  const sel = window.getSelection();
  const nodo = sel && sel.anchorNode ? (sel.anchorNode.nodeType === 1 ? sel.anchorNode : sel.anchorNode.parentElement) : e.target;
  const fila = nodo && nodo.closest ? nodo.closest('tr') : null;
  const tabla = fila ? fila.closest('table') : null;
  if (tabla && esTablaPuntos(tabla) && editor.contains(tabla)) {
    PUNTOS_CTX = { tabla, fila, editorId: editor.id };
    ubicarBarraPuntos();
  } else {
    PUNTOS_CTX = null;
    barraPuntos().style.display = 'none';
  }
}
document.addEventListener('click', revisarPuntosClave);
document.addEventListener('keyup', revisarPuntosClave);
document.addEventListener('scroll', () => { if (PUNTOS_CTX) ubicarBarraPuntos(); }, true);

/** Renumera las filas y ajusta las esquinas redondeadas de la primera y la última. */
function renumerarPuntos(tabla) {
  const filas = Array.from(tabla.rows), n = filas.length;
  filas.forEach((tr, i) => {
    const num = tr.cells[0], texto = tr.cells[1];
    num.textContent = String(i + 1);
    const primero = i === 0, ultimo = i === n - 1;
    num.style.borderRadius = (primero ? '8px' : '0') + ' 0 0 ' + (ultimo ? '8px' : '0');
    if (texto) texto.style.borderRadius = '0 ' + (primero ? '8px' : '0') + ' ' + (ultimo ? '8px' : '0') + ' 0';
  });
}

function agregarPuntoClave() {
  if (!PUNTOS_CTX) return;
  const nueva = PUNTOS_CTX.fila.cloneNode(true);
  nueva.cells[1].innerHTML = '<strong>Nuevo punto clave</strong><br><span style="font-size:13.5px">Explica brevemente esta idea.</span>';
  PUNTOS_CTX.fila.after(nueva);
  renumerarPuntos(PUNTOS_CTX.tabla);
  PUNTOS_CTX.fila = nueva;
  rteWordCount(PUNTOS_CTX.editorId);
  ubicarBarraPuntos();
}

function quitarPuntoClave() {
  if (!PUNTOS_CTX) return;
  const { tabla, fila, editorId } = PUNTOS_CTX;
  if (tabla.rows.length <= 1) { toast('El bloque necesita al menos un punto. Para quitar el bloque completo, selecciónalo y bórralo.', true); return; }
  const siguiente = fila.nextElementSibling || fila.previousElementSibling;
  fila.remove();
  renumerarPuntos(tabla);
  PUNTOS_CTX.fila = siguiente;
  rteWordCount(editorId);
  ubicarBarraPuntos();
}

/* ================================================================
   FASE 3: INSCRITOS DEL CURSO Y SU AVANCE (solo lectura)
================================================================ */
async function abrirInscritosDocente(idCurso) {
  const c = cursoActualDoc(idCurso);
  if (!document.getElementById('modal-doc-inscritos')) {
    const div = document.createElement('div');
    div.innerHTML = '<div class="modal-overlay" id="modal-doc-inscritos"><div class="modal grande"><div class="modal-header"><h3 id="dins-titulo">Inscritos</h3>' +
      '<button class="modal-close" onclick="cerrarModal(\'modal-doc-inscritos\')">✕</button></div><div class="modal-body" id="dins-body"></div>' +
      '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-doc-inscritos\')">Cerrar</button></div></div></div>';
    document.body.appendChild(div.firstElementChild);
  }
  document.getElementById('dins-titulo').textContent = 'Estudiantes inscritos — ' + (c ? c.nombre : '');
  document.getElementById('dins-body').innerHTML = '<p class="muted">Cargando...</p>';
  abrirModal('modal-doc-inscritos');
  try {
    const lista = await pedirApi('GET', '/api/cursos/' + idCurso + '/inscripciones');
    const etiqueta = { activa: 'Activa', completada: 'Completada', pendiente_pago: 'Pendiente de pago', retirada: 'Retirada', suspendida: 'Suspendida' };
    document.getElementById('dins-body').innerHTML = !lista.length ? '<div class="empty-state">Aún no hay estudiantes inscritos en este curso.</div>' :
      '<p class="text-sm muted">' + lista.length + ' inscripción(es) · el avance es el promedio de las lecciones activas.</p>' +
      '<div class="tabla-wrap"><table><thead><tr><th>Estudiante</th><th>Estado</th><th>Avance</th><th>Colegio</th></tr></thead><tbody>' +
      lista.map(i => {
        const pct = Math.round(Number(i.porcentaje || 0));
        return '<tr class="dins-fila" data-correo="' + esc(i.correo) + '"><td><div style="font-weight:600">' + esc(i.estudiante) + '</div><div class="text-sm muted">' + esc(i.correo) + '</div></td>' +
          '<td>' + esc(etiqueta[i.estado] || i.estado) + '</td>' +
          '<td style="min-width:180px"><div class="progress-bar" style="height:8px;background:#E7ECEA;border-radius:6px;overflow:hidden"><div style="width:' + pct + '%;height:100%;background:var(--sage)"></div></div>' +
          '<div class="text-sm">' + pct + ' % · ' + i.leccionesCompletadas + ' de ' + i.totalLecciones + ' lecciones</div></td>' +
          '<td class="text-sm">' + esc(i.institucion || '—') + '</td></tr>';
      }).join('') + '</tbody></table></div>';
  } catch (err) { document.getElementById('dins-body').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; }
}

