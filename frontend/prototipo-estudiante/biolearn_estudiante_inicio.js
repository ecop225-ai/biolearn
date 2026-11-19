/* ================================================================
   BIOLearn — biolearn_estudiante_inicio.js
   INICIO del estudiante conectado al backend (DEF-039).
   Antes mostraba cifras del prototipo (2 cursos, 33 %, 2 certificados, actividades y eventos de ejemplo)
   aunque el estudiante fuera nuevo. Ahora usa GET /api/estudiante/inscripciones:
   cursos con acceso, avance promedio, cursos terminados y el plazo más próximo a vencer.
   Actividades, evaluaciones, sesiones, eventos y certificados todavía no tienen datos en el backend,
   así que el Inicio no muestra cifras de ellos.
================================================================ */
const ACCESO_RAPIDO_EST = [
  { key:'cursos', label:'Mis cursos', icon:'📚' }, { key:'catalogo', label:'Catálogo', icon:'🔎' },
  { key:'actividades', label:'Actividades', icon:'✏️' }, { key:'evaluaciones', label:'Evaluaciones', icon:'📝' },
  { key:'foros', label:'Foros', icon:'💬' }, { key:'sesiones', label:'Sesiones', icon:'📡' },
  { key:'eventos', label:'Eventos', icon:'🎥' }, { key:'certificados', label:'Certificados', icon:'🏅' },
];
function saludoHora() {
  const h = new Date().getHours();
  return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
}

function renderInicio(cont) {
  cont.innerHTML = bannerInicioEst(null) + '<div id="ini-est-cuerpo"><div class="card muted" style="padding:30px;text-align:center">Cargando tu resumen…</div></div>';
  pedirApiEst('GET', '/api/estudiante/inscripciones')
    .then(lista => pintarInicioEst(lista || []))
    .catch(err => {
      const el = document.getElementById('ini-est-cuerpo');
      if (el) el.innerHTML = '<div class="card" style="padding:30px;text-align:center">No se pudo cargar tu resumen: ' + esc(err.message) + '</div>';
    });
}

function bannerInicioEst(resumen) {
  const nombre = esc(ESTUDIANTE.nombre.split(' ')[0]);
  let texto = 'Cargando…', boton = '', accion = 'cursos';
  if (resumen) {
    if (resumen.enCurso.length) {
      const c = resumen.enCurso[0];
      texto = 'Sigue con «' + esc(c.curso) + '»: llevas ' + c.leccionesCompletadas + ' de ' + c.totalLecciones + ' lecciones.';
      boton = 'Continuar aprendiendo →';
    } else if (resumen.conAcceso.length) {
      texto = 'Terminaste todos tus cursos. Puedes repasarlos o inscribirte en uno nuevo.';
      boton = 'Ver catálogo →'; accion = 'catalogo';
    } else if (resumen.pendientesPago.length) {
      texto = 'Tienes ' + resumen.pendientesPago.length + ' curso(s) pendiente(s) de pago.';
      boton = 'Ir a mis pagos →'; accion = 'pagos';
    } else {
      texto = 'Aún no estás inscrito en ningún curso. Explora el catálogo para empezar.';
      boton = 'Ver catálogo →'; accion = 'catalogo';
    }
  }
  return '<div style="background:linear-gradient(120deg,var(--blue),var(--sage));border-radius:16px;padding:22px 28px;margin-bottom:24px" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><div style="color:#fff;font-family:\'Fraunces\',serif;font-size:21px">¡' + saludoHora() + ', ' + nombre + '! 🌱</div>' +
    '<div style="color:rgba(255,255,255,.9);font-size:13.5px">' + texto + '</div></div>' +
    (boton ? '<button style="background:rgba(255,255,255,.18);border:1px solid rgba(255,255,255,.45);color:#fff;border-radius:9px;padding:11px 20px;font-weight:700;font-size:13.5px;cursor:pointer" onclick="goTo(\'' + accion + '\')">' + boton + '</button>' : '') +
    '</div>';
}

function pintarInicioEst(lista) {
  const conAcceso = lista.filter(i => i.estado === 'activa' || i.estado === 'completada');
  const enCurso = lista.filter(i => i.estado === 'activa' && Number(i.porcentaje || 0) < 100)
    .sort((a, b) => Number(b.porcentaje || 0) - Number(a.porcentaje || 0));
  const terminados = lista.filter(i => i.estado === 'completada' || (i.estado === 'activa' && Number(i.porcentaje || 0) >= 100));
  const pendientesPago = lista.filter(i => i.estado === 'pendiente_pago');
  const avance = conAcceso.length ? Math.round(conAcceso.reduce((s, i) => s + Number(i.porcentaje || 0), 0) / conAcceso.length) : 0;
  const porVencer = enCurso.filter(i => i.diasRestantes != null && !i.modoRepaso).sort((a, b) => a.diasRestantes - b.diasRestantes);
  const resumen = { conAcceso, enCurso, pendientesPago };

  const cont = document.getElementById('ini-est-cuerpo'); if (!cont) return;
  const banner = cont.previousElementSibling;
  if (banner) banner.outerHTML = bannerInicioEst(resumen);

  const kpi = (color, titulo, icono, valor, detalle) =>
    '<div class="kpi-card" style="border-left-color:' + color + '"><div class="kpi-top"><span>' + titulo + '</span><span class="kpi-icon" style="color:' + color + '">' + iconSvg(icono) + '</span></div>' +
    '<div class="kpi-value">' + valor + '</div>' + (detalle ? '<div class="text-sm muted" style="margin-top:2px">' + detalle + '</div>' : '') + '</div>';

  let html = '<div class="kpi-row">' +
    kpi('var(--blue)', 'Cursos inscritos', 'book', conAcceso.length, pendientesPago.length ? pendientesPago.length + ' pendiente(s) de pago' : '') +
    kpi('var(--sage)', 'Avance promedio', 'chart', avance + '%', conAcceso.length ? 'en ' + conAcceso.length + ' curso(s)' : 'sin cursos todavía') +
    kpi('var(--amber)', 'Cursos terminados', 'award', terminados.length, '') +
    kpi('var(--clay)', 'Próximo plazo', 'calendar', porVencer.length ? porVencer[0].diasRestantes + ' días' : '—', porVencer.length ? esc(porVencer[0].curso) : 'sin plazos activos') +
  '</div>';

  html += '<div class="text-sm" style="font-weight:800;letter-spacing:.5px;text-transform:uppercase;color:var(--ink-soft);margin-bottom:12px">Acceso rápido</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px;margin-bottom:26px">';
  ACCESO_RAPIDO_EST.forEach(a => {
    html += '<div onclick="goTo(\'' + a.key + '\')" class="card" style="text-align:center;cursor:pointer;padding:22px 12px"><div style="font-size:26px;margin-bottom:8px">' + a.icon + '</div><div style="font-weight:700;font-size:13px">' + a.label + '</div></div>';
  });
  html += '</div>';

  html += '<div class="card"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">Cursos en progreso</h3>';
  if (!enCurso.length) {
    html += '<div class="empty-state">' + (conAcceso.length ? '¡Estás al día! No tienes cursos sin terminar 🎉' : 'Cuando te inscribas en un curso, aquí verás tu avance.') + '</div>';
  }
  enCurso.slice(0, 5).forEach(i => {
    const pct = Math.round(Number(i.porcentaje || 0));
    html += '<div onclick="goTo(\'cursos\')" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
      '<div class="flex justify-between items-center gap-12"><div style="min-width:0"><div style="font-size:14px;font-weight:600">' + esc(i.curso) + '</div>' +
      '<div class="text-sm muted">' + i.leccionesCompletadas + ' de ' + i.totalLecciones + ' lecciones' + (i.diasRestantes != null ? ' · quedan ' + i.diasRestantes + ' días' : '') + '</div></div>' +
      '<strong style="white-space:nowrap">' + pct + '%</strong></div>' +
      '<div class="progress-bar" style="margin-top:8px"><div class="progress-fill" style="width:' + pct + '%"></div></div></div>';
  });
  html += '</div>';

  cont.innerHTML = html;
}
