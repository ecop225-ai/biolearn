/* ================================================================
   BIOLearn — biolearn_docente_inicio.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: INICIO
================================================================ */
const ACCESO_RAPIDO = [
  { key:'cursos', label:'Mis cursos', icon:'📚' },
  { key:'lecciones', label:'Lecciones', icon:'📖' },
  { key:'actividades', label:'Actividades', icon:'✏️' },
  { key:'evaluaciones', label:'Evaluaciones', icon:'📝' },
  { key:'foros', label:'Foros', icon:'💬' },
  { key:'sesiones', label:'Sesiones', icon:'📡' },
  { key:'estudiantes', label:'Estudiantes', icon:'👥' },
  { key:'materiales', label:'Materiales', icon:'📄' },
  { key:'certificados', label:'Certificados', icon:'🏅' },
  { key:'reportes', label:'Reportes', icon:'📊' },
  { key:'mensajeria', label:'Mensajes', icon:'✉️' },
];
function saludoSegunHora() {
  const h = new Date().getHours();
  if (h < 12) return '¡Buenos días';
  if (h < 19) return '¡Buenas tardes';
  return '¡Buenas noches';
}
/* INICIO del docente conectado al backend (DEF-039). Antes mostraba cifras del prototipo (86 estudiantes, 78 %,
   entregas y sesiones de ejemplo). Ahora usa GET /api/docente/cursos y, por cada curso activo,
   GET /api/cursos/{id}/inscripciones. Entregas, foros y sesiones todavía no tienen datos en el backend. */
function renderInicio(cont) {
  cont.innerHTML = bannerInicioDoc(null) + '<div id="ini-doc-cuerpo"><div class="card muted" style="padding:30px;text-align:center">Cargando tu resumen…</div></div>';
  cargarInicioDoc();
}

function bannerInicioDoc(r) {
  const texto = !r ? 'Cargando…'
    : !r.cursos.length ? 'Todavía no tienes cursos asignados. Puedes proponer uno desde «Mis cursos».'
    : 'Tienes ' + r.activos.length + ' curso' + (r.activos.length === 1 ? '' : 's') + ' activo' + (r.activos.length === 1 ? '' : 's') +
      ' con ' + r.estudiantes + ' estudiante' + (r.estudiantes === 1 ? '' : 's') + ' inscrito' + (r.estudiantes === 1 ? '' : 's') +
      (r.borradores.length ? ' y ' + r.borradores.length + ' en borrador' : '') + '.';
  return '<div id="ini-doc-banner" style="background:var(--blue-deep);border-radius:14px;padding:24px 28px;margin-bottom:22px" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><h2 style="color:#fff;font-size:22px;margin:0 0 6px">' + saludoSegunHora() + ', ' + esc(DOCENTE.nombre.split(' ')[0]) + '! 👩‍🏫</h2>' +
    '<p style="color:rgba(255,255,255,.8);font-size:14px;margin:0">' + texto + '</p></div>' +
    '<button style="background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.3);color:#fff;border-radius:9px;padding:11px 20px;font-weight:700;font-size:13.5px;cursor:pointer" onclick="goTo(\'cursos\')">Ver mis cursos</button></div>';
}

async function cargarInicioDoc() {
  const cuerpo = () => document.getElementById('ini-doc-cuerpo');
  let cursos;
  try { cursos = await pedirApi('GET', '/api/docente/cursos'); }
  catch (err) { if (cuerpo()) cuerpo().innerHTML = '<div class="card" style="padding:30px;text-align:center">No se pudo cargar tu resumen: ' + esc(err.message) + '</div>'; return; }
  cursos = cursos || [];
  const activos = cursos.filter(c => c.estado === 'activo');
  const borradores = cursos.filter(c => c.estado === 'borrador');
  const porCurso = {};
  await Promise.all(activos.map(c => pedirApi('GET', '/api/cursos/' + c.id + '/inscripciones')
    .then(l => { porCurso[c.id] = (l || []).filter(i => i.estado === 'activa' || i.estado === 'completada'); })
    .catch(() => { porCurso[c.id] = []; })));
  const todas = Object.values(porCurso).flat();
  const estudiantes = new Set(todas.map(i => i.idEstudiante)).size;
  const avance = todas.length ? Math.round(todas.reduce((s, i) => s + Number(i.porcentaje || 0), 0) / todas.length) : 0;
  const leccionesActivas = activos.reduce((s, c) => s + (c.leccionesActivas || 0), 0);
  const r = { cursos, activos, borradores, estudiantes };

  const banner = document.getElementById('ini-doc-banner'); if (banner) banner.outerHTML = bannerInicioDoc(r);
  if (!cuerpo()) return;

  const kpi = (color, titulo, icono, valor, detalle) =>
    '<div class="kpi-card" style="border-left-color:' + color + '"><div class="kpi-top"><span>' + titulo + '</span><span class="kpi-icon" style="color:' + color + '">' + iconSvg(icono) + '</span></div>' +
    '<div class="kpi-value">' + valor + '</div>' + (detalle ? '<div class="text-sm muted" style="margin-top:2px">' + detalle + '</div>' : '') + '</div>';
  let html = '<div class="kpi-row">' +
    kpi('var(--sage)', 'Cursos activos', 'book', activos.length, cursos.length + ' en total') +
    kpi('var(--blue)', 'Estudiantes', 'users', estudiantes, todas.length + ' inscripción(es)') +
    kpi('var(--sage)', 'Avance promedio', 'chart', avance + '%', 'de tus estudiantes') +
    kpi('var(--amber)', 'Lecciones activas', 'check', leccionesActivas, 'en cursos activos') +
  '</div>';

  html += '<div class="flex gap-14 flex-wrap mb-20" style="align-items:start">';
  html += '<div class="card" style="flex:1.4;min-width:320px"><div class="flex justify-between items-center mb-14"><h3 style="font-size:17px;margin:0">Mis cursos activos</h3>' +
    '<button class="btn btn-ghost btn-sm" onclick="goTo(\'cursos\')">Ver todos</button></div>';
  if (!activos.length) html += '<div class="empty-state">No tienes cursos activos.</div>';
  activos.slice().sort((a, b) => (porCurso[b.id] || []).length - (porCurso[a.id] || []).length).slice(0, 6).forEach(c => {
    const l = porCurso[c.id] || [];
    const pct = l.length ? Math.round(l.reduce((s, i) => s + Number(i.porcentaje || 0), 0) / l.length) : 0;
    html += '<div onclick="goTo(\'cursos\')" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
      '<div class="flex justify-between items-center gap-12"><div style="min-width:0"><div style="font-size:14px;font-weight:600">' + esc(c.nombre) + '</div>' +
      '<div class="text-sm muted">' + esc(c.miRol || '') + ' · ' + l.length + ' estudiante' + (l.length === 1 ? '' : 's') + ' · ' + (c.leccionesActivas || 0) + ' lecciones activas</div></div>' +
      '<strong style="white-space:nowrap">' + pct + '%</strong></div>' +
      '<div class="progress-bar" style="margin-top:8px"><div class="progress-fill" style="width:' + pct + '%"></div></div></div>';
  });
  html += '</div>';

  html += '<div class="card" style="flex:1;min-width:280px"><h3 style="font-size:17px;margin:0 0 14px">Cursos en borrador</h3>';
  if (!borradores.length) html += '<div class="empty-state">No tienes cursos pendientes de publicar.</div>';
  borradores.forEach(c => {
    const faltan = (c.pendientesParaPublicar || []);
    html += '<div onclick="goTo(\'cursos\')" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
      '<div style="font-size:13.5px;font-weight:600">' + esc(c.nombre) + '</div>' +
      '<div class="text-sm muted">' + (faltan.length ? 'Falta: ' + esc(faltan.join(', ')) : 'Listo para que el Administrador lo publique') + '</div></div>';
  });
  html += '</div></div>';

  html += '<div class="text-sm" style="font-weight:800;letter-spacing:.5px;text-transform:uppercase;color:var(--ink-soft);margin-bottom:12px">Acceso rápido</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px">';
  ACCESO_RAPIDO.forEach(a => {
    html += '<div onclick="goTo(\'' + a.key + '\')" class="card" style="text-align:center;cursor:pointer;padding:22px 12px">' +
      '<div style="font-size:30px;margin-bottom:10px">' + a.icon + '</div><div style="font-weight:700;font-size:13.5px">' + a.label + '</div></div>';
  });
  html += '</div>';
  cuerpo().innerHTML = html;
}
function irAEntregaInicio(leccionId) {
  const leccion = DATA.leccionesById[leccionId];
  if (leccion) { window.ctx_act_curso = leccion.cursoId; window.ctx_act_leccion = leccion.id; }
  goTo('actividades');
}
function irAProximoInicio(tipoLbl, leccionId) {
  if (tipoLbl === 'Sesión en línea') {
    const leccion = DATA.leccionesById[leccionId];
    if (leccion) { window.ctx_ses_curso = leccion.cursoId; window.ctx_ses_leccion = leccion.id; }
    goTo('sesiones');
  } else {
    goTo('eventos');
  }
}
