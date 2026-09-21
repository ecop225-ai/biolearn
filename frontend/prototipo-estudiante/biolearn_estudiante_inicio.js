/* ================================================================
   BIOLearn — biolearn_estudiante_inicio.js
================================================================ */
const ACCESO_RAPIDO_EST = [
  { key:'cursos', label:'Mis cursos', icon:'📚' }, { key:'actividades', label:'Actividades', icon:'✏️' },
  { key:'evaluaciones', label:'Evaluaciones', icon:'📝' }, { key:'foros', label:'Foros', icon:'💬' },
  { key:'sesiones', label:'Sesiones', icon:'📡' }, { key:'eventos', label:'Eventos', icon:'🎥' },
  { key:'certificados', label:'Certificados', icon:'🏅' }, { key:'notas', label:'Mis notas', icon:'📊' },
];
function saludoHora() {
  const h = new Date().getHours();
  return h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
}
function renderInicio(cont) {
  const curso = DATA.cursos[0];
  const lecciones = curso ? curso.lecciones.map(id=>DATA.leccionesById[id]).filter(l=>l.estado==='activa') : [];
  const completadas = lecciones.filter(l => DATA.progresoLeccion[l.id] && DATA.progresoLeccion[l.id].estado==='completada').length;
  const avance = lecciones.length ? Math.round((completadas/lecciones.length)*100) : 0;
  const continuarLeccion = curso ? (lecciones.find(l=>estadoLeccion(curso,l).estado==='en_progreso') || lecciones.find(l=>estadoLeccion(curso,l).estado==='pendiente')) : null;

  const pendientesActividad = DATA.actividades.filter(a=>!a.miEntrega);
  const pendientesEvaluacion = DATA.evaluaciones.filter(e=>e.misIntentos.length < e.intentos);
  const proximos = [
    ...DATA.sesiones.filter(s=>s.estado!=='finalizada').map(s=>({...s, tipoLbl:'Sesión en línea'})),
    ...DATA.eventos.filter(e=>e.estado==='programado').map(e=>({...e, tipoLbl:'Evento virtual'})),
  ].sort((a,b)=> new Date(a.fechaInicio) - new Date(b.fechaInicio));
  const mensajesNuevos = DATA.mensajes.filter(m=>!m.leido).length;

  let html = '<div style="background:linear-gradient(120deg,var(--blue),var(--sage));border-radius:16px;padding:22px 28px;margin-bottom:24px" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><div style="color:#fff;font-family:\'Fraunces\',serif;font-size:21px">¡' + saludoHora() + ', ' + esc(ESTUDIANTE.nombre.split(' ')[0]) + '! 🌱</div>' +
    '<div style="color:rgba(255,255,255,.9);font-size:13.5px">' +
    (curso && continuarLeccion ? 'Tienes pendiente "' + esc(continuarLeccion.nombre) + '" y ' + mensajesNuevos + ' mensaje' + (mensajesNuevos===1?'':'s') + ' nuevo' + (mensajesNuevos===1?'':'s') : 'Tienes ' + pendientesActividad.length + ' actividad' + (pendientesActividad.length===1?'':'es') + ' pendiente' + (pendientesActividad.length===1?'':'s') + ' y ' + mensajesNuevos + ' mensaje' + (mensajesNuevos===1?'':'s') + ' nuevo' + (mensajesNuevos===1?'':'s')) +
    '</div></div>' +
    '<button style="background:rgba(255,255,255,.18);border:1px solid rgba(255,255,255,.45);color:#fff;border-radius:9px;padding:11px 20px;font-weight:700;font-size:13.5px;cursor:pointer" onclick="goTo(\'cursos\')">Continuar aprendiendo →</button></div>';

  html += '<div class="kpi-row">' +
    '<div class="kpi-card" style="border-left-color:var(--blue)"><div class="kpi-top"><span>Cursos inscritos</span><span class="kpi-icon" style="color:var(--blue)">' + iconSvg('book') + '</span></div><div class="kpi-value">' + DATA.cursos.length + '</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--sage)"><div class="kpi-top"><span>Avance del curso</span><span class="kpi-icon" style="color:var(--sage)">' + iconSvg('chart') + '</span></div><div class="kpi-value">' + avance + '%</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--amber)"><div class="kpi-top"><span>Certificados obtenidos</span><span class="kpi-icon" style="color:var(--amber)">' + iconSvg('award') + '</span></div><div class="kpi-value">' + DATA.certificados.filter(c=>c.estado==='emitido').length + '</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--clay)"><div class="kpi-top"><span>Actividades pendientes</span><span class="kpi-icon" style="color:var(--clay)">' + iconSvg('check') + '</span></div><div class="kpi-value">' + pendientesActividad.length + '</div></div>' +
  '</div>';

  html += '<div class="text-sm" style="font-weight:800;letter-spacing:.5px;text-transform:uppercase;color:var(--ink-soft);margin-bottom:12px">Acceso rápido</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px;margin-bottom:26px">';
  ACCESO_RAPIDO_EST.forEach(a => {
    html += '<div onclick="goTo(\'' + a.key + '\')" class="card" style="text-align:center;cursor:pointer;padding:22px 12px"><div style="font-size:26px;margin-bottom:8px">' + a.icon + '</div><div style="font-weight:700;font-size:13px">' + a.label + '</div></div>';
  });
  html += '</div>';

  html += '<div class="flex gap-14 flex-wrap" style="align-items:start">';
  html += '<div class="card" style="flex:1.4;min-width:320px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">Por hacer</h3>';
  if (pendientesActividad.length === 0 && pendientesEvaluacion.length === 0) {
    html += '<div class="empty-state">¡Estás al día! 🎉</div>';
  } else {
    pendientesActividad.forEach(a => {
      html += '<div onclick="goTo(\'actividades\')" class="flex justify-between items-center" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
        '<div><div style="font-size:14px;font-weight:600">' + esc(a.nombre) + '</div><div class="text-sm muted">Actividad · vence ' + fmtDate(a.fechaLimite) + '</div></div>' + pill('pendiente') + '</div>';
    });
    pendientesEvaluacion.forEach(e => {
      html += '<div onclick="goTo(\'evaluaciones\')" class="flex justify-between items-center" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
        '<div><div style="font-size:14px;font-weight:600">' + esc(e.nombre) + '</div><div class="text-sm muted">Evaluación · ' + (e.intentos-e.misIntentos.length) + ' intento(s) disponible(s)</div></div>' + pill('pendiente') + '</div>';
    });
  }
  html += '</div>';

  html += '<div class="card" style="flex:1;min-width:280px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">Próximas sesiones y eventos</h3>';
  if (proximos.length === 0) html += '<div class="empty-state">Nada programado por ahora</div>';
  proximos.forEach(p => {
    html += '<div onclick="goTo(\'' + (p.tipoLbl==='Sesión en línea'?'sesiones':'eventos') + '\')" class="flex gap-10" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
      '<div style="width:30px;height:30px;border-radius:8px;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;flex-shrink:0">' + iconSvg('calendar') + '</div>' +
      '<div><div style="font-size:13.5px;font-weight:600">' + esc(p.nombre) + '</div><div class="text-sm muted">' + p.tipoLbl + ' · ' + fmtRel(p.fechaInicio) + '</div></div></div>';
  });
  html += '</div></div>';

  cont.innerHTML = html;
}
