/* ================================================================
   BIOLearn — biolearn_estudiante_sesiones_eventos.js
================================================================ */
let SESION_CODIGO_TMP = {};
let SESION_ACTIVA_GLOBAL = null;
let EVENTO_ACTIVO_GLOBAL = null;
let EVENTO_CODIGO_TMP = '';

function sesionCardHTML(s) {
  const enCurso = s.estado === 'en_curso';
  let html = '<div class="card mb-14"><div class="flex justify-between items-center mb-10"><h4 style="font-size:16px;margin:0">' + esc(s.nombre) + '</h4>' + pill(s.estado) + '</div>' +
    '<p class="text-sm muted mb-14">' + fmtRel(s.fechaInicio) + ' · ' + (PLATAFORMA_LABEL[s.plataforma]||s.plataforma) + '</p>';
  if (enCurso) html += '<a href="'+esc(s.enlace)+'" target="_blank"><button class="btn btn-primary" style="margin-bottom:12px">🌐 Unirse a la sesión ahora</button></a><br>';
  if (!s.miAsistencia.asistio) {
    html += '<div class="flex gap-8"><input id="ses-codigo-'+s.id+'" placeholder="Código de asistencia" style="flex:1" value="'+(SESION_CODIGO_TMP[s.id]||'')+'" oninput="SESION_CODIGO_TMP[\''+s.id+'\']=this.value">' +
      '<button class="btn btn-primary" onclick="registrarAsistenciaSesionEst(\''+s.id+'\')">Registrar asistencia</button></div>';
  } else html += '<div style="color:var(--sage);font-weight:700;font-size:13px">✓ Asistencia registrada</div>';
  if (s.grabacionHabilitada) html += '<div style="margin-top:10px"><a href="'+esc(s.grabacionUrl)+'" target="_blank" style="color:var(--blue);font-weight:700;font-size:12.5px">▶ Ver grabación</a></div>';
  html += '</div>';
  return html;
}
function registrarAsistenciaSesionEst(sesId) {
  const codigo = (SESION_CODIGO_TMP[sesId]||'').trim();
  if (!codigo) return;
  const s = DATA.sesiones.find(x=>x.id===sesId);
  if (codigo.toUpperCase() === (s.codigoAsistencia||'').toUpperCase()) {
    s.miAsistencia = { asistio:true, codigoIngresado:codigo };
    saveData(); render();
    toast('Asistencia registrada correctamente');
  } else toast('Código incorrecto, verifica con tu docente', true);
}

function renderSesiones(cont) {
  if (SESION_ACTIVA_GLOBAL === null && DATA.sesiones.length>0) SESION_ACTIVA_GLOBAL = DATA.sesiones[0].id;
  const s = DATA.sesiones.find(x=>x.id===SESION_ACTIVA_GLOBAL);
  const leccion = s ? DATA.leccionesById[s.leccionId] : null;
  const curso = leccion ? DATA.cursos.find(c=>c.id===leccion.cursoId) : null;
  const enCurso = s && s.estado === 'en_curso';

  let html = '<div class="section-header"><div><h2>Sesiones en línea</h2><p class="subtitle">Clases en vivo de tus lecciones</p></div></div>';
  html += '<div class="flex gap-14" style="align-items:flex-start">';
  html += '<div class="card" style="width:260px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.sesiones.forEach(x => {
    html += '<div onclick="SESION_ACTIVA_GLOBAL=\''+x.id+'\';render()" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:'+(SESION_ACTIVA_GLOBAL===x.id?'var(--blue-soft)':'transparent')+'">' +
      '<div style="font-size:13.5px;font-weight:700">' + esc(x.nombre) + '</div><div class="text-sm muted">' + fmtRel(x.fechaInicio) + '</div></div>';
  });
  html += '</div><div style="flex:1;min-width:0">';
  if (!s) html += '<div class="empty-state">No tienes sesiones programadas.</div>';
  else {
    html += '<div class="card"><span style="display:inline-flex;align-items:center;gap:6px;background:var(--blue-soft);color:var(--blue-deep);font-size:12.5px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:14px">💻 Sesión en línea</span>' +
      '<h2 style="font-size:22px;margin:0 0 8px">' + esc(s.nombre) + '</h2>' +
      '<div class="flex gap-14 flex-wrap text-sm muted mb-18"><span>📅 ' + fmtRel(s.fechaInicio) + (s.duracionMin?' — '+s.duracionMin+' min':'') + '</span><span>💻 ' + (PLATAFORMA_LABEL[s.plataforma]||s.plataforma) + '</span>' + pill(s.estado) + '</div>' +
      '<div class="flex justify-between items-center flex-wrap gap-10" style="background:linear-gradient(135deg,#1E3345,#2E5588);border-radius:14px;padding:20px 24px;margin-bottom:18px">' +
      '<div><div style="color:#fff;font-weight:700;font-size:16px">💻 ' + (leccion?'Lección '+leccion.orden+' — '+esc(leccion.nombre):esc(s.nombre)) + '</div><div style="color:rgba(255,255,255,.8);font-size:12.5px">Doc. ' + esc(curso?curso.docente:'') + ' · ' + (PLATAFORMA_LABEL[s.plataforma]||s.plataforma) + '</div></div>' +
      (enCurso?'<span style="background:#4CAF6D;color:#fff;font-weight:700;font-size:12px;padding:5px 12px;border-radius:20px">● En curso ahora</span>':'') + '</div>';
    if (enCurso) html += '<a href="'+esc(s.enlace)+'" target="_blank"><button class="btn btn-primary" style="width:100%;margin-bottom:18px;background:#3FAE5F">🌐 Unirse a la sesión ahora</button></a>';
    if (s.temas && s.temas.length>0) html += '<div style="background:var(--bone);border-radius:12px;padding:16px;margin-bottom:14px"><div class="text-sm" style="font-weight:700;text-transform:uppercase;margin-bottom:8px">📋 Temática de la sesión</div><ul style="margin:0;padding-left:20px;font-size:13.5px;line-height:1.8">' + s.temas.map(t=>'<li>'+esc(t)+'</li>').join('') + '</ul></div>';
    if (!s.miAsistencia.asistio) {
      html += '<div style="background:var(--blue-soft);border-radius:12px;padding:16px;margin-bottom:14px"><div class="text-sm" style="font-weight:700;color:var(--blue-deep);text-transform:uppercase;margin-bottom:8px">🔑 Código de asistencia</div>' +
        '<p class="text-sm muted mb-10">El docente compartirá un código durante la sesión.</p>' +
        '<div class="flex gap-8"><input id="ses-codigo-g-'+s.id+'" value="'+(SESION_CODIGO_TMP[s.id]||'')+'" oninput="SESION_CODIGO_TMP[\''+s.id+'\']=this.value" placeholder="Código de asistencia" style="flex:1;background:#fff"><button class="btn btn-primary" onclick="registrarAsistenciaSesionEst(\''+s.id+'\')">Registrar asistencia</button></div></div>';
    } else html += '<div style="color:var(--sage);font-weight:700;font-size:13px;margin-bottom:14px">✓ Asistencia registrada</div>';
    if (s.grabacionHabilitada) html += '<a href="'+esc(s.grabacionUrl)+'" target="_blank" style="display:inline-flex;align-items:center;gap:6px;color:var(--blue);font-weight:700;font-size:13px">▶ Ver grabación de la sesión</a>';
    html += '</div>';
  }
  html += '</div></div>';
  cont.innerHTML = html;
}

function renderEventos(cont) {
  if (EVENTO_ACTIVO_GLOBAL === null && DATA.eventos.length>0) EVENTO_ACTIVO_GLOBAL = DATA.eventos[0].id;
  const evt = DATA.eventos.find(e=>e.id===EVENTO_ACTIVO_GLOBAL);
  let html = '<div class="section-header"><div><h2>Eventos</h2><p class="subtitle">Conferencias y actividades abiertas de tus cursos</p></div></div>';
  html += '<div class="flex gap-14" style="align-items:flex-start">';
  html += '<div class="card" style="width:260px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.eventos.forEach(e => {
    html += '<div onclick="EVENTO_ACTIVO_GLOBAL=\''+e.id+'\';render()" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:'+(EVENTO_ACTIVO_GLOBAL===e.id?'var(--blue-soft)':'transparent')+'">' +
      '<div style="font-size:13.5px;font-weight:700">' + esc(e.nombre) + '</div><div class="text-sm muted">' + fmtRel(e.fechaInicio) + '</div></div>';
  });
  html += '</div><div style="flex:1;min-width:0">';
  if (!evt) html += '<div class="empty-state">No hay eventos disponibles.</div>';
  else {
    html += '<div class="card"><span style="display:inline-flex;align-items:center;gap:6px;background:var(--clay-soft);color:var(--clay);font-size:12.5px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:14px">🎥 Evento virtual</span>' +
      '<h2 style="font-size:22px;margin:0 0 8px">' + esc(evt.nombre) + '</h2>' +
      '<div class="flex gap-14 flex-wrap text-sm muted mb-18"><span>📅 ' + fmtRel(evt.fechaInicio) + '</span><span>🎥 ' + (PLATAFORMA_LABEL[evt.plataforma]||evt.plataforma) + '</span>' + (evt.emiteCertificado?'<span>🏅 Con certificado de participación</span>':'') + pill(evt.estado) + '</div>';
    html += '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:18px">' +
      '<div style="background:var(--bone);border-radius:10px;padding:14px"><div class="text-sm muted" style="font-weight:700;text-transform:uppercase;margin-bottom:4px">📅 Fecha y hora</div><div style="font-size:13.5px;font-weight:700">' + fmtRel(evt.fechaInicio) + '</div></div>' +
      (evt.ponentes?'<div style="background:var(--bone);border-radius:10px;padding:14px"><div class="text-sm muted" style="font-weight:700;text-transform:uppercase;margin-bottom:4px">🎤 Ponentes</div><div style="font-size:13.5px;font-weight:700">'+esc(evt.ponentes)+'</div></div>':'') +
      '<div style="background:var(--bone);border-radius:10px;padding:14px"><div class="text-sm muted" style="font-weight:700;text-transform:uppercase;margin-bottom:4px">👥 Inscritos</div><div style="font-size:13.5px;font-weight:700">' + evt.inscritos + ' estudiantes</div></div>' +
      '<div style="background:var(--bone);border-radius:10px;padding:14px"><div class="text-sm muted" style="font-weight:700;text-transform:uppercase;margin-bottom:4px">🏅 Certificado</div><div style="font-size:13.5px;font-weight:700">' + (evt.emiteCertificado?'Sí, al registrar asistencia':'No aplica') + '</div></div></div>';
    if (evt.estado === 'programado') {
      html += '<div style="background:#241318;border-radius:14px;padding:24px;text-align:center;margin-bottom:18px">' +
        '<div style="width:56px;height:40px;border-radius:9px;background:#E12626;margin:0 auto 12px;display:flex;align-items:center;justify-content:center"><div style="width:0;height:0;border-top:8px solid transparent;border-bottom:8px solid transparent;border-left:12px solid #fff;margin-left:2px"></div></div>' +
        '<span style="background:#E12626;color:#fff;font-weight:700;font-size:11.5px;padding:3px 10px;border-radius:20px">● PRÓXIMO EN VIVO</span>' +
        '<p style="color:#fff;font-weight:700;font-size:14.5px;margin:12px 0 4px">' + esc(evt.nombre) + '</p>' +
        '<p style="color:rgba(255,255,255,.6);font-size:12px;margin-bottom:14px">El enlace se activará ' + (evt.minutosAntes||10) + ' minutos antes del evento</p>' +
        '<div class="flex justify-center gap-10 flex-wrap"><button class="btn" style="background:#3FAE5F;color:#fff" onclick="abrirEnlaceExternoEst(\''+esc(evt.enlaceAcceso)+'\',\''+evt.plataforma+'\')">🌐 Unirse al evento</button>' +
        (!evt.miAsistencia.confirmoRecordatorio ? '<button class="btn" style="background:#fff;color:var(--ink)" onclick="confirmarRecordatorioEst(\''+evt.id+'\')">🔔 Activar recordatorio</button>' : '<span style="color:var(--sage);font-weight:700;font-size:12.5px;align-self:center">✓ Recordatorio activado</span>') + '</div></div>';
    } else {
      html += '<div style="background:'+(evt.miAsistencia.asistio?'var(--blue-soft)':'var(--clay-soft)')+';border-radius:12px;padding:16px;margin-bottom:18px;color:'+(evt.miAsistencia.asistio?'var(--blue-deep)':'var(--clay)')+';font-weight:700;font-size:13.5px">' +
        (evt.miAsistencia.asistio ? '✓ Asististe a este evento · certificado disponible en Certificados' : 'No se registró tu asistencia a este evento.') + '</div>';
    }
    if (evt.contenido && evt.contenido.length>0) html += '<div style="background:var(--bone);border-radius:12px;padding:16px;margin-bottom:18px"><div class="text-sm muted" style="font-weight:700;text-transform:uppercase;margin-bottom:8px">📋 Contenido del evento</div><ul style="margin:0;padding-left:20px;font-size:13.5px;line-height:1.8">' + evt.contenido.map(c=>'<li>'+esc(c)+'</li>').join('') + '</ul></div>';
    if (evt.estado === 'programado' && !evt.miAsistencia.asistio) {
      html += '<div style="background:var(--blue-soft);border-radius:12px;padding:16px"><div class="text-sm" style="font-weight:700;color:var(--blue-deep);text-transform:uppercase;margin-bottom:8px">✍️ Registrar mi asistencia</div>' +
        '<p class="text-sm muted mb-10">Ingresa el código que compartirá el docente durante la transmisión.</p>' +
        '<div class="flex gap-8"><input id="evt-codigo" value="'+EVENTO_CODIGO_TMP+'" oninput="EVENTO_CODIGO_TMP=this.value" placeholder="Código del evento" style="flex:1;background:#fff"><button class="btn btn-primary" onclick="registrarAsistenciaEventoEst(\''+evt.id+'\')">Registrar asistencia</button></div></div>';
    }
    html += '</div>';
  }
  html += '</div></div>';
  cont.innerHTML = html;
}
function confirmarRecordatorioEst(id) { DATA.eventos.find(e=>e.id===id).miAsistencia.confirmoRecordatorio = true; saveData(); render(); toast('Recordatorio activado'); }
function registrarAsistenciaEventoEst(id) {
  const codigo = EVENTO_CODIGO_TMP.trim(); if (!codigo) return;
  const e = DATA.eventos.find(x=>x.id===id);
  e.miAsistencia = {...e.miAsistencia, asistio:true, codigoIngresado:codigo};
  if (e.emiteCertificado) DATA.certificados.unshift({ id:uid('cert'), origen:'Participación — '+e.nombre, codigo:'BIO-CERT-'+Math.floor(10000+Math.random()*89999), emitidoEn:new Date().toISOString().slice(0,10), tipo:'evento', estado:'emitido' });
  EVENTO_CODIGO_TMP = '';
  saveData(); render();
  toast('Asistencia registrada' + (e.emiteCertificado?' · certificado disponible en Certificados':''));
}
