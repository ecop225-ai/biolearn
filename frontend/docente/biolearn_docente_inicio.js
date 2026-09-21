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
function renderInicio(cont) {
  const pendientes = DATA.actividades.flatMap(a => a.entregas.filter(e=>e.estado==='entregada').map(e => ({...e, actividad:a.nombre, leccionId:a.leccionId})));
  const proximos = [
    ...DATA.sesiones.map(s => ({...s, tipoLbl:'Sesión en línea'})),
    ...DATA.eventos.filter(e=>e.estado==='programado').map(e => ({...e, tipoLbl:'Evento virtual'})),
  ].sort((a,b)=> new Date(a.fechaInicio) - new Date(b.fechaInicio));
  const e = DATA.estadisticas;
  const mensajesNuevos = DATA.mensajes.filter(m=>!m.leido).length;

  let html = '<div style="background:var(--blue-deep);border-radius:14px;padding:24px 28px;margin-bottom:22px" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><h2 style="color:#fff;font-size:22px;margin:0 0 6px">' + saludoSegunHora() + ', ' + esc(DOCENTE.nombre.split(' ')[0]) + '! 👩\u200d🏫</h2>' +
    '<p style="color:rgba(255,255,255,.8);font-size:14px;margin:0">Tienes ' + pendientes.length + ' entrega' + (pendientes.length===1?'':'s') + ' pendiente' + (pendientes.length===1?'':'s') + ' de revisar y ' + mensajesNuevos + ' mensaje' + (mensajesNuevos===1?'':'s') + ' nuevo' + (mensajesNuevos===1?'':'s') + '</p></div>' +
    '<button style="background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.3);color:#fff;border-radius:9px;padding:11px 20px;font-weight:700;font-size:13.5px;cursor:pointer" onclick="goTo(\'actividades\')">Ver entregas</button></div>';

  html += '<div class="kpi-row">' +
    '<div class="kpi-card" style="border-left-color:var(--sage)"><div class="kpi-top"><span>Estudiantes</span><span class="kpi-icon" style="color:var(--sage)">' + iconSvg('users') + '</span></div><div class="kpi-value">' + e.estudiantesTotal + '</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--clay)"><div class="kpi-top"><span>Entregas pendientes</span><span class="kpi-icon" style="color:var(--clay)">' + iconSvg('check') + '</span></div><div class="kpi-value">' + pendientes.length + '</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--sage)"><div class="kpi-top"><span>Progreso del curso</span><span class="kpi-icon" style="color:var(--sage)">' + iconSvg('chart') + '</span></div><div class="kpi-value">' + e.tasaFinalizacion + '%</div>' +
      '<div class="progress-bar" style="margin-top:8px"><div class="progress-fill" style="width:' + e.tasaFinalizacion + '%"></div></div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--amber)"><div class="kpi-top"><span>Aportes en foros</span><span class="kpi-icon" style="color:var(--amber)">' + iconSvg('chat') + '</span></div><div class="kpi-value">' + DATA.foros.reduce((n,f)=>n+f.comentarios.length,0) + '</div></div>' +
  '</div>';

  html += '<div class="flex gap-14 flex-wrap mb-20" style="align-items:start">';
  html += '<div class="card" style="flex:1.4;min-width:320px"><div class="flex justify-between items-center mb-14"><h3 style="font-size:17px;margin:0">Entregas por calificar</h3>' +
    '<button class="btn btn-ghost btn-sm" onclick="goTo(\'actividades\')">Ver todas</button></div>';
  if (pendientes.length === 0) html += '<div class="empty-state">No tienes entregas pendientes 🎉</div>';
  pendientes.forEach(p => {
    html += '<div onclick="irAEntregaInicio(\'' + p.leccionId + '\')" class="flex justify-between items-center" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
      '<div><div style="font-size:14px;font-weight:600">' + esc(p.actividad) + '</div><div class="text-sm muted">' + esc(p.estudiante) + (p.esTardia?' · entrega tardía':'') + '</div></div>' +
      pill(p.estado) + '</div>';
  });
  html += '</div>';

  html += '<div class="card" style="flex:1;min-width:280px"><h3 style="font-size:17px;margin:0 0 14px">Próximas sesiones y eventos</h3>';
  if (proximos.length === 0) html += '<div class="empty-state">Nada programado por ahora.</div>';
  proximos.forEach(p => {
    html += '<div onclick="irAProximoInicio(\'' + p.tipoLbl + '\',\'' + (p.leccionId||'') + '\')" class="flex gap-10" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
      '<div style="width:30px;height:30px;border-radius:8px;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;flex-shrink:0">' + iconSvg('calendar') + '</div>' +
      '<div><div style="font-size:13.5px;font-weight:600">' + esc(p.nombre) + '</div><div class="text-sm muted">' + p.tipoLbl + ' · ' + fmtDate(p.fechaInicio) + '</div></div></div>';
  });
  html += '</div></div>';

  html += '<div class="text-sm" style="font-weight:800;letter-spacing:.5px;text-transform:uppercase;color:var(--ink-soft);margin-bottom:12px">Acceso rápido</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px">';
  ACCESO_RAPIDO.forEach(a => {
    html += '<div onclick="goTo(\'' + a.key + '\')" class="card" style="text-align:center;cursor:pointer;padding:22px 12px">' +
      '<div style="font-size:30px;margin-bottom:10px">' + a.icon + '</div><div style="font-weight:700;font-size:13.5px">' + a.label + '</div></div>';
  });
  html += '</div>';

  cont.innerHTML = html;
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
