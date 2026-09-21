/* ================================================================
   BIOLearn — biolearn_docente_sesiones_eventos.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: SESIONES EN LÍNEA
================================================================ */
let SESION_ACTIVA = null;
const PARTICIPACION_LABEL = { alta:'Alta participación', media:'Media participación', baja:'Baja participación' };

function renderSesiones(cont) {
  const leccionId = window.ctx_ses_leccion || '';
  let html = '<div class="section-header"><div><h2>Sesiones en línea</h2><p class="subtitle">Clases en vivo por lección: control de asistencia y participación.</p></div></div>';
  html += contextPickerHTML('ses');
  if (!leccionId) { html += '<div class="empty-state">Selecciona un curso y una lección para ver o crear sus sesiones en línea.</div>'; cont.innerHTML = html; renderModalSesion(); renderModalSesionPreview(); return; }

  const items = DATA.sesiones.filter(s => s.leccionId === leccionId);
  if (items.length > 0 && !items.some(s=>s.id===SESION_ACTIVA)) SESION_ACTIVA = items[0].id;
  if (items.length === 0) SESION_ACTIVA = null;
  const sesion = items.find(s => s.id === SESION_ACTIVA);

  html += '<div class="flex gap-14" style="align-items:flex-start">';
  html += '<div class="card" style="width:260px;flex-shrink:0;padding:0;overflow:hidden">' +
    '<div style="padding:12px 14px;border-bottom:1px solid var(--line)"><button class="btn btn-primary btn-sm w-full" onclick="abrirModalSesion()">+ Nueva sesión</button></div>';
  items.forEach(s => {
    html += '<div onclick="seleccionarSesion(\''+s.id+'\')" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer' + (s.id===SESION_ACTIVA?';background:var(--blue-soft)':'') + '">' +
      '<div style="font-size:13.5px;font-weight:700">' + esc(s.nombre) + '</div><div class="text-sm muted">' + fmtRel(s.fechaInicio) + '</div></div>';
  });
  if (items.length === 0) html += '<div class="empty-state">Esta lección no tiene sesiones.</div>';
  html += '</div>';

  html += '<div style="flex:1;min-width:0">';
  if (!sesion) {
    html += '<div class="empty-state">Crea la primera sesión de esta lección.</div>';
  } else {
    const asistieron = sesion.asistentes.filter(a=>a.asistio).length, total = sesion.asistentes.length;
    const pct = total > 0 ? Math.round((asistieron/total)*100) : 0;
    html += '<div class="flex justify-between items-start"><span class="pill pill-azul">💻 Sesión en línea</span><button class="btn btn-ghost btn-xs" onclick="abrirModalSesionPreview(\''+sesion.id+'\')">👁 Ver como estudiante</button></div>';
    html += '<h2 style="font-size:24px;margin:8px 0">' + esc(sesion.nombre) + '</h2>';
    html += '<div class="flex gap-14 flex-wrap text-sm muted mb-14"><span>📅 ' + fmtRel(sesion.fechaInicio) + '</span><span>💻 ' + PLATAFORMA_LABEL[sesion.plataforma] + '</span>' + pill(sesion.estado) + '</div>';
    html += '<div class="flex gap-8 flex-wrap items-center" style="background:var(--bone);border-radius:12px;padding:14px;margin-bottom:18px">' +
      '<span class="text-sm" style="font-weight:700;text-transform:uppercase">Gestión:</span>' +
      '<button class="btn btn-amber btn-sm" onclick="abrirEnlaceExterno(\''+esc(sesion.enlace||'')+'\',\''+sesion.plataforma+'\')">▶ Unirse a la sesión</button>' +
      '<button class="btn btn-primary btn-sm" onclick="abrirModalSesion()">+ Nueva sesión</button>' +
      (!sesion.codigoAsistencia ? '<button class="btn btn-ghost btn-sm" onclick="generarCodigoSesion(\''+sesion.id+'\')"># Generar código asistencia</button>' : '') +
      '<button class="btn btn-ghost btn-sm" onclick="abrirModalSesion(\''+sesion.id+'\')">✏️ Editar</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="agregarGrabacionSesion(\''+sesion.id+'\')">' + (sesion.grabacionUrl?'🔗 Cambiar enlace de grabación':'🔗 Agregar enlace de grabación') + '</button>' +
      (sesion.grabacionUrl ? '<button class="btn btn-ghost btn-sm" onclick="quitarGrabacionSesion(\''+sesion.id+'\')">Quitar grabación</button>' : '') +
      '<button class="btn btn-ghost btn-sm" onclick="confirmarEliminarSesion(\''+sesion.id+'\')" style="color:var(--clay)">🗑 Eliminar</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="toast(\'Exportando lista de asistencia...\')">⬇ Exportar lista</button></div>';
    html += '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:20px">' +
      '<div class="card" style="text-align:center"><h2 style="font-size:26px;color:var(--sage)">'+asistieron+'</h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Asistieron</div></div>' +
      '<div class="card" style="text-align:center"><h2 style="font-size:26px;color:var(--clay)">'+(total-asistieron)+'</h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Ausentes</div></div>' +
      '<div class="card" style="text-align:center"><h2 style="font-size:26px;color:var(--blue)">'+pct+'%</h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Asistencia</div></div></div>';
    html += '<div class="card"><div class="flex justify-between items-center mb-14 flex-wrap gap-8"><span style="font-weight:700;font-size:14px">🔑 Lista de asistencia</span><span class="text-sm muted">Haz clic en ✓/✗ para cambiar el estado</span></div>';
    sesion.asistentes.forEach((a,i) => {
      const iniciales = a.estudiante.split(' ').map(p=>p[0]).slice(0,2).join('');
      html += '<div class="flex items-center gap-14 flex-wrap" style="padding:12px 0;' + (i>0?'border-top:1px solid var(--line)':'') + '">' +
        '<button onclick="toggleAsistioSesion(\''+sesion.id+'\','+i+')" style="width:26px;height:26px;border-radius:6px;border:none;cursor:pointer;font-weight:800;background:' + (a.asistio?'#4CAF6D':'#E15C5C') + ';color:#fff">' + (a.asistio?'✓':'✗') + '</button>' +
        '<div style="width:30px;height:30px;border-radius:50%;background:var(--sage-soft);color:var(--sage);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12px">'+esc(iniciales)+'</div>' +
        '<span style="font-size:13.5px;font-weight:700;min-width:120px">' + esc(a.estudiante) + '</span>';
      if (a.asistio) {
        html += '<span class="text-sm muted">Entrada: ' + (a.entrada||'—') + ' · ' + (a.metodo==='automatico'?'Automático':'Código') + ' · ' + a.minutos + ' min</span>' +
          '<select style="width:170px" onchange="actualizarAsistenteSesion(\''+sesion.id+'\','+i+',\'participacion\',this.value)">' +
          Object.entries(PARTICIPACION_LABEL).map(([v,l])=>'<option value="'+v+'"'+(a.participacion===v?' selected':'')+'>'+l+'</option>').join('') + '</select>' +
          '<input value="'+esc(a.nota||'')+'" placeholder="Nota" style="width:70px" onchange="actualizarAsistenteSesion(\''+sesion.id+'\','+i+',\'nota\',this.value)">' +
          '<input value="'+esc(a.observacion||'')+'" placeholder="Observación (opcional)" style="flex:1;min-width:140px" onchange="actualizarAsistenteSesion(\''+sesion.id+'\','+i+',\'observacion\',this.value)">';
      } else {
        html += '<span class="text-sm" style="color:var(--clay);font-weight:700">No asistió</span>' +
          '<button class="btn btn-ghost btn-xs" style="margin-left:auto" onclick="toast(\'Mensaje enviado a '+esc(a.estudiante)+'\')">✉ Contactar</button>';
      }
      html += '</div>';
    });
    if (sesion.asistentes.length === 0) html += '<div class="empty-state">Sin convocados registrados en esta sesión.</div>';
    if (sesion.asistentes.length > 0) html += '<div class="flex justify-end mt-14"><button class="btn btn-primary" onclick="toast(\'Asistencia guardada\')">✓ Guardar asistencia</button></div>';
    html += '</div>';
  }
  html += '</div></div>';
  cont.innerHTML = html;
  renderModalSesion(); renderModalSesionPreview();
}
function seleccionarSesion(id) { SESION_ACTIVA = id; render(); }
function generarCodigoSesion(id) {
  const s = DATA.sesiones.find(x=>x.id===id);
  const codigo = Math.random().toString(36).slice(2,8).toUpperCase();
  s.codigoAsistencia = codigo; s.estado = 'en_curso';
  saveData(); render(); toast('Código de asistencia generado: ' + codigo);
}
function agregarGrabacionSesion(id) {
  const s = DATA.sesiones.find(x=>x.id===id);
  const url = prompt('Pega el enlace de la grabación (YouTube, Drive, Zoom cloud, etc.):', s.grabacionUrl||'');
  if (!url) return;
  s.grabacionUrl = url; s.grabacionHabilitada = true;
  saveData(); render(); toast('Grabación agregada y disponible para los estudiantes');
}
function quitarGrabacionSesion(id) {
  const s = DATA.sesiones.find(x=>x.id===id);
  s.grabacionUrl = ''; s.grabacionHabilitada = false;
  saveData(); render(); toast('Grabación quitada');
}
function confirmarEliminarSesion(id) {
  const s = DATA.sesiones.find(x=>x.id===id);
  confirmarAccion('¿Eliminar la sesión <strong>' + esc(s.nombre) + '</strong>? Se perderá el registro de asistencia.', function () {
    DATA.sesiones = DATA.sesiones.filter(x=>x.id!==id);
    SESION_ACTIVA = null;
    saveData(); render(); toast('Sesión eliminada');
  });
}
function toggleAsistioSesion(id, idx) {
  const s = DATA.sesiones.find(x=>x.id===id);
  s.asistentes[idx].asistio = !s.asistentes[idx].asistio;
  saveData(); render();
}
function actualizarAsistenteSesion(id, idx, campo, valor) {
  DATA.sesiones.find(x=>x.id===id).asistentes[idx][campo] = valor;
  saveData();
}
function abrirModalSesion(id) {
  const s = id ? DATA.sesiones.find(x=>x.id===id) : null;
  document.getElementById('ses-modal-titulo').textContent = s ? 'Editar sesión en línea' : 'Nueva sesión en línea';
  document.getElementById('ses-modal-btn').textContent = s ? 'Guardar cambios' : 'Crear sesión';
  document.getElementById('ses-id').value = s ? s.id : '';
  document.getElementById('ses-nombre').value = s ? s.nombre : '';
  document.getElementById('ses-tematica').value = s ? (s.tematica||'') : '';
  document.getElementById('ses-plataforma').value = s ? s.plataforma : 'meet';
  document.getElementById('ses-enlace').value = s ? (s.enlace||'') : '';
  document.getElementById('ses-fecha').value = s ? s.fechaInicio : '';
  document.getElementById('ses-duracion').value = s ? s.duracionMin : 60;
  abrirModal('modal-sesion');
}
function renderModalSesion() {
  if (document.getElementById('modal-sesion')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-sesion"><div class="modal"><div class="modal-header"><h3 id="ses-modal-titulo">Nueva sesión en línea</h3><button class="modal-close" onclick="cerrarModal(\'modal-sesion\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="ses-id">' +
    '<div class="campo"><label>Nombre</label><input type="text" id="ses-nombre" placeholder="Ej: Repaso lección 3"></div>' +
    '<div class="campo"><label>Temática</label><input type="text" id="ses-tematica"></div>' +
    '<div class="grid-2"><div class="campo"><label>Fecha y hora</label><input type="datetime-local" id="ses-fecha"></div>' +
    '<div class="campo"><label>Duración (min)</label><input type="number" id="ses-duracion"></div>' +
    '<div class="campo"><label>Plataforma</label><select id="ses-plataforma"><option value="meet">Google Meet</option><option value="zoom">Zoom</option><option value="teams">Microsoft Teams</option><option value="youtube">YouTube</option><option value="otro">Otro</option></select></div></div>' +
    '<div class="campo"><label>Enlace de la sesión</label><input type="url" id="ses-enlace" placeholder="https://..."><p class="hint">YouTube se reproduce dentro de BioLearn; Zoom, Teams y otras plataformas se abren en una pestaña nueva al unirse.</p></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-sesion\')">Cancelar</button><button class="btn btn-primary" id="ses-modal-btn" onclick="guardarSesion()">Crear sesión</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarSesion() {
  const id = document.getElementById('ses-id').value;
  const nombre = document.getElementById('ses-nombre').value.trim();
  const leccionId = window.ctx_ses_leccion;
  if (!nombre) { toast('Escribe el nombre de la sesión', true); return; }
  if (!leccionId) { toast('Selecciona primero una lección', true); return; }
  const campos = { nombre, tematica: document.getElementById('ses-tematica').value.trim(), plataforma: document.getElementById('ses-plataforma').value,
    enlace: document.getElementById('ses-enlace').value.trim(), fechaInicio: document.getElementById('ses-fecha').value,
    duracionMin: Number(document.getElementById('ses-duracion').value)||60 };
  if (id) {
    Object.assign(DATA.sesiones.find(x=>x.id===id), campos);
    saveData(); cerrarModal('modal-sesion'); render(); toast('Sesión "' + nombre + '" actualizada');
    return;
  }
  const nueva = { id: uid('ses'), leccionId, ...campos, codigoAsistencia:null, estado:'programada',
    grabacionHabilitada:false, grabacionUrl:'', convocados:0, temas:[], asistentes:[] };
  DATA.sesiones.push(nueva);
  SESION_ACTIVA = nueva.id;
  saveData(); cerrarModal('modal-sesion'); render(); toast('Sesión "' + nombre + '" creada · los estudiantes fueron notificados');
}
function abrirModalSesionPreview(id) {
  const s = DATA.sesiones.find(x=>x.id===id);
  const leccion = DATA.leccionesById[s.leccionId];
  const enCurso = s.estado === 'en_curso';
  document.getElementById('sp-nombre').textContent = s.nombre;
  document.getElementById('sp-meta').innerHTML = '<span>📅 ' + fmtRel(s.fechaInicio) + (s.duracionMin?' — '+s.duracionMin+' min':'') + '</span><span>💻 ' + (PLATAFORMA_LABEL[s.plataforma]||s.plataforma) + '</span>' +
    '<span style="background:' + (enCurso?'#FDE2E2':'var(--sage-soft)') + ';color:' + (enCurso?'#B23A3A':'var(--sage)') + ';font-weight:700;padding:3px 10px;border-radius:20px;font-size:12px">' + (enCurso?'● En curso':s.estado==='finalizada'?'Finalizada':'Programada') + '</span>';
  document.getElementById('sp-banner-titulo').textContent = '💻 ' + (leccion ? 'Lección ' + leccion.orden + ' — ' + leccion.nombre : s.nombre);
  document.getElementById('sp-banner-sub').textContent = 'Doc. ' + DOCENTE.nombre + ' · ' + (PLATAFORMA_LABEL[s.plataforma]||s.plataforma) + ' · ' + (s.convocados||0) + ' estudiantes convocados';
  document.getElementById('sp-en-curso-badge').style.display = enCurso ? 'inline-block' : 'none';
  document.getElementById('sp-hora-inicio').textContent = new Date(s.fechaInicio).toLocaleTimeString('es-CO',{hour:'numeric',minute:'2-digit'});
  document.getElementById('sp-duracion').textContent = (s.duracionMin || '—') + ' min';
  document.getElementById('sp-conectados').textContent = (s.conectados||0) + ' / ' + (s.convocados||0);
  document.getElementById('sp-connect-texto').textContent = (PLATAFORMA_LABEL[s.plataforma]||s.plataforma) + ' — Sesión ' + (enCurso?'activa':'programada');
  document.getElementById('sp-connect-sub').textContent = enCurso ? 'Haz clic para unirte a la clase en vivo' : 'El enlace se habilitará cuando comience la sesión';
  document.getElementById('sp-btn-unirse').style.display = enCurso ? 'block' : 'none';
  const temasWrap = document.getElementById('sp-temas-wrap');
  if (s.temas && s.temas.length > 0) { temasWrap.style.display = 'block'; document.getElementById('sp-temas').innerHTML = s.temas.map(t=>'<li>'+esc(t)+'</li>').join(''); }
  else temasWrap.style.display = 'none';
  document.getElementById('sp-codigo').placeholder = s.codigoAsistencia || 'ABC-123';
  document.getElementById('sp-grabacion').innerHTML = s.grabacionUrl ? '<a href="'+esc(s.grabacionUrl)+'" target="_blank">▶ Ver grabación</a>' : 'Aún no disponible';
  abrirModal('modal-sesion-preview');
}
function renderModalSesionPreview() {
  if (document.getElementById('modal-sesion-preview')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-sesion-preview"><div class="modal grande"><div class="modal-header"><h3>Vista previa del estudiante</h3><button class="modal-close" onclick="cerrarModal(\'modal-sesion-preview\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div style="background:#F7F3EC;border-radius:14px;padding:24px;border:1px solid #EFE6DA">' +
    '<span style="display:inline-flex;align-items:center;gap:6px;background:var(--blue-soft);color:var(--blue);font-size:12.5px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:14px">💻 Sesión en línea</span>' +
    '<h2 style="font-family:\'Fraunces\',serif;font-size:23px;color:#2C4A42;margin:0 0 8px" id="sp-nombre"></h2>' +
    '<div class="flex items-center gap-14 flex-wrap" style="font-size:13px;color:#6B5B45;margin-bottom:18px" id="sp-meta"></div>' +
    '<div class="flex justify-between items-center flex-wrap gap-10" style="background:linear-gradient(135deg,#1E3345,#2E5588);border-radius:14px;padding:20px 24px;margin-bottom:18px">' +
    '<div><div style="color:#fff;font-weight:700;font-size:17px;margin-bottom:4px" id="sp-banner-titulo"></div><div style="color:rgba(255,255,255,.85);font-size:13px" id="sp-banner-sub"></div></div>' +
    '<span id="sp-en-curso-badge" style="display:none;background:#4CAF6D;color:#fff;font-weight:700;font-size:12px;padding:6px 14px;border-radius:20px">● En curso ahora</span></div>' +
    '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:18px">' +
    '<div style="background:#fff;border-radius:10px;padding:14px 8px;border:1px solid #EFE6DA;text-align:center"><div style="font-family:\'Fraunces\',serif;font-size:20px" id="sp-hora-inicio"></div><div style="font-size:11px;color:#8A6A4E;font-weight:700">Hora de inicio</div></div>' +
    '<div style="background:#fff;border-radius:10px;padding:14px 8px;border:1px solid #EFE6DA;text-align:center"><div style="font-family:\'Fraunces\',serif;font-size:20px" id="sp-duracion"></div><div style="font-size:11px;color:#8A6A4E;font-weight:700">Duración</div></div>' +
    '<div style="background:#fff;border-radius:10px;padding:14px 8px;border:1px solid #EFE6DA;text-align:center"><div style="font-family:\'Fraunces\',serif;font-size:20px" id="sp-conectados"></div><div style="font-size:11px;color:#8A6A4E;font-weight:700">Conectados</div></div>' +
    '</div>' +
    '<div style="background:linear-gradient(180deg,#1E3345,#132330);border-radius:14px;padding:30px;text-align:center;margin-bottom:14px">' +
    '<div style="font-size:34px;margin-bottom:8px">💻</div><p style="color:#fff;font-weight:700;font-size:15px;margin:0 0 4px" id="sp-connect-texto"></p>' +
    '<p style="color:rgba(255,255,255,.65);font-size:12.5px" id="sp-connect-sub"></p></div>' +
    '<button id="sp-btn-unirse" disabled style="display:none;width:100%;background:linear-gradient(90deg,#5FC97A,#3FAE5F);border:none;border-radius:12px;padding:14px;font-weight:700;color:#0E2E1A;font-size:14.5px;margin-bottom:18px;opacity:.92">🌐 Unirse a la sesión ahora</button>' +
    '<div id="sp-temas-wrap" style="display:none;background:#fff;border-radius:12px;padding:18px;border:1px solid #EFE6DA;margin-bottom:14px">' +
    '<div style="font-size:12.5px;font-weight:700;color:var(--clay);text-transform:uppercase;margin-bottom:8px">📋 Temática de la sesión</div>' +
    '<ul id="sp-temas" style="margin:0;padding-left:20px;font-size:13.5px;line-height:1.8;color:var(--ink)"></ul></div>' +
    '<div style="background:#fff;border-radius:12px;padding:18px;border:1px solid #EFE6DA;margin-bottom:14px">' +
    '<div style="font-size:12.5px;font-weight:700;color:var(--sage);text-transform:uppercase;margin-bottom:8px">🔑 Código de asistencia</div>' +
    '<p style="font-size:13px;color:var(--ink-soft);margin-bottom:12px">El docente compartirá un código durante la sesión. Ingrésalo aquí para que tu asistencia quede registrada en el sistema.</p>' +
    '<div class="flex gap-10"><input disabled id="sp-codigo" style="flex:1;background:var(--bone);font-weight:700;letter-spacing:1px"><button disabled style="background:var(--blue);opacity:.85;border:none;border-radius:9px;padding:10px 18px;font-weight:700;color:#fff;font-size:13.5px">Registrar asistencia</button></div></div>' +
    '<div class="flex justify-between items-center" style="background:#fff;border-radius:12px;padding:16px;border:1px solid #EFE6DA">' +
    '<div class="flex items-center gap-10">▶️<div><div style="font-size:13.5px;font-weight:700">Grabación disponible después de la sesión</div><div style="font-size:12px;color:var(--ink-soft)">La grabación se publicará automáticamente cuando el docente la habilite</div></div></div>' +
    '<span style="font-size:12px;color:var(--ink-soft);background:var(--bone);padding:6px 12px;border-radius:8px" id="sp-grabacion"></span></div>' +
    '</div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se ve para el estudiante. Los botones están desactivados en esta vista previa.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-sesion-preview\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ================================================================
   VISTA: EVENTOS VIRTUALES (no ligados a una lección)
================================================================ */
let EVENTO_ACTIVO = null;
function renderEventos(cont) {
  let html = '<div class="section-header"><div><h2>Eventos virtuales</h2><p class="subtitle">Conferencias y actividades abiertas del curso, con control de asistencia y certificados.</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalEvento()">+ Nuevo evento</button></div>';

  if (DATA.eventos.length > 0 && !DATA.eventos.some(e=>e.id===EVENTO_ACTIVO)) EVENTO_ACTIVO = DATA.eventos[0].id;
  const evento = DATA.eventos.find(e => e.id === EVENTO_ACTIVO);
  const certificadosDelEvento = evento ? DATA.certificados.filter(c=>c.origen === 'Evento: '+evento.nombre) : [];

  html += '<div class="flex gap-14" style="align-items:flex-start">';
  html += '<div class="card" style="width:260px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.eventos.forEach(e => {
    html += '<div onclick="seleccionarEvento(\''+e.id+'\')" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer' + (e.id===EVENTO_ACTIVO?';background:var(--blue-soft)':'') + '">' +
      '<div style="font-size:13.5px;font-weight:700">' + esc(e.nombre) + '</div><div class="text-sm muted">' + fmtRel(e.fechaInicio) + '</div></div>';
  });
  if (DATA.eventos.length === 0) html += '<div class="empty-state">No hay eventos creados.</div>';
  html += '</div>';

  html += '<div style="flex:1;min-width:0">';
  if (!evento) {
    html += '<div class="empty-state">Crea tu primer evento virtual.</div>';
  } else {
    html += '<div class="flex justify-between items-start"><span class="pill pill-clay">🎥 Evento virtual</span><button class="btn btn-ghost btn-xs" onclick="abrirModalEventoPreview(\''+evento.id+'\')">👁 Ver como estudiante</button></div>';
    html += '<h2 style="font-size:24px;margin:8px 0">' + esc(evento.nombre) + '</h2>';
    html += '<div class="flex gap-14 flex-wrap text-sm muted mb-14"><span>📅 ' + fmtRel(evento.fechaInicio) + '</span><span>🎥 ' + PLATAFORMA_LABEL[evento.plataforma] + '</span>' + (evento.emiteCertificado?'<span>🏅 Certificado habilitado</span>':'') + pill(evento.estado) + '</div>';
    html += '<div class="flex gap-8 flex-wrap items-center" style="background:var(--bone);border-radius:12px;padding:14px;margin-bottom:18px">' +
      '<span class="text-sm" style="font-weight:700;text-transform:uppercase">Gestión:</span>' +
      '<button class="btn btn-amber btn-sm" onclick="abrirEnlaceExterno(\''+esc(evento.enlaceAcceso||'')+'\',\''+evento.plataforma+'\')">▶ Unirse / Ver en vivo</button>' +
      '<button class="btn btn-primary btn-sm" onclick="abrirModalEvento()">+ Nuevo evento</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="abrirModalEvento(\''+evento.id+'\')">✏️ Editar</button>' +
      (evento.emiteCertificado ? '<button class="btn btn-amber btn-sm" onclick="emitirTodosPendientes(\''+evento.id+'\')">🏅 Emitir certificados</button>' : '') +
      '<button class="btn btn-ghost btn-sm" onclick="toast(\'Recordatorio enviado a los convocados de \\\''+esc(evento.nombre)+'\\\'\')">🔔 Enviar recordatorio</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="agregarGrabacionEvento(\''+evento.id+'\')">' + (evento.grabacionUrl?'🔗 Cambiar grabación':'🔗 Agregar grabación') + '</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="toast(\'Exportando datos del evento...\')">⬇ Exportar</button>' +
      (evento.estado==='programado' ? '<button class="btn btn-ghost btn-sm" onclick="finalizarEvento(\''+evento.id+'\')">Marcar finalizado</button>' : '') +
      '<button class="btn btn-ghost btn-sm" onclick="confirmarEliminarEvento(\''+evento.id+'\')" style="color:var(--clay)">🗑 Eliminar</button></div>';
    html += '<div class="grid-2" style="margin-bottom:20px"><div class="card"><div class="text-sm" style="font-weight:700;text-transform:uppercase;margin-bottom:12px">Detalles del evento</div>' +
      '<div class="flex-col gap-8 text-sm"><div>📅 <strong>Fecha:</strong> ' + fmtDate(evento.fechaInicio) + '</div>' +
      '<div>🎥 <strong>Plataforma:</strong> ' + PLATAFORMA_LABEL[evento.plataforma] + '</div>' +
      '<div>🔗 <strong>Enlace:</strong> ' + (evento.enlaceAcceso||'—') + '</div>' +
      '<div>▶ <strong>Grabación:</strong> ' + (evento.grabacionUrl ? '<a href="'+esc(evento.grabacionUrl)+'" target="_blank">Ver grabación</a>' : 'No disponible') + '</div>' +
      '<div>🏅 <strong>Certificado:</strong> ' + (evento.emiteCertificado?'Sí — se emite al confirmar asistencia':'No') + '</div>' +
      '<div>👥 <strong>Convocados:</strong> ' + (evento.convocados||evento.asistentes.length) + ' estudiantes</div></div></div>' +
      '<div class="card"><div class="text-sm" style="font-weight:700;text-transform:uppercase;margin-bottom:12px">Estado actual</div>' +
      '<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">' +
      '<div style="background:var(--bone);border-radius:10px;padding:12px 8px;text-align:center"><h2 style="font-size:22px;color:var(--sage)">'+evento.asistentes.filter(a=>a.confirmoRecordatorio).length+'</h2><div class="text-sm muted">Confirmaron</div></div>' +
      '<div style="background:var(--bone);border-radius:10px;padding:12px 8px;text-align:center"><h2 style="font-size:22px;color:var(--clay)">'+evento.asistentes.filter(a=>!a.confirmoRecordatorio).length+'</h2><div class="text-sm muted">Sin confirmar</div></div>' +
      '<div style="background:var(--bone);border-radius:10px;padding:12px 8px;text-align:center"><h2 style="font-size:22px;color:var(--blue)">'+certificadosDelEvento.length+'</h2><div class="text-sm muted">Certificados</div></div>' +
      '<div style="background:var(--bone);border-radius:10px;padding:12px 8px;text-align:center"><h2 style="font-size:16px">'+(evento.estado==='finalizado'?'Finalizado':evento.estado==='en_curso'?'En curso':'Programado')+'</h2><div class="text-sm muted">Estado</div></div>' +
      '</div></div></div>';
    if (evento.emiteCertificado) {
      html += '<div class="card"><div class="flex justify-between items-center mb-14"><span style="font-weight:700;font-size:15px">🏅 Gestión de certificados</span></div>';
      evento.asistentes.forEach((a,i) => {
        const iniciales = a.estudiante.split(' ').map(p=>p[0]).slice(0,2).join('');
        const cert = certificadosDelEvento.find(c=>c.receptor===a.estudiante);
        html += '<div class="flex justify-between items-center" style="padding:12px 0;' + (i>0?'border-top:1px solid var(--line)':'') + '">' +
          '<div class="flex items-center gap-10"><div style="width:32px;height:32px;border-radius:50%;background:var(--sage-soft);color:var(--sage);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12.5px">'+esc(iniciales)+'</div>' +
          '<div><div style="font-size:14px;font-weight:700">' + esc(a.estudiante) + '</div><div class="text-sm muted">' + (cert ? cert.codigo+' · Emitido' : (a.asistio?'Pendiente — asistencia confirmada':'Sin asistencia confirmada')) + '</div></div></div>';
        if (cert) html += '<div class="flex items-center gap-8"><span class="pill pill-verde">✓ Emitido</span><button class="btn btn-ghost btn-xs" onclick="toast(\'Descargando certificado de '+esc(a.estudiante)+'\')">⬇ Ver</button></div>';
        else if (a.asistio) html += '<div class="flex items-center gap-8"><span class="pill pill-ambar">Pendiente</span><button class="btn btn-amber btn-xs" onclick="emitirCertificadoEvento(\''+evento.id+'\',\''+esc(a.estudiante).replace(/'/g,"\\'")+'\')">🏅 Emitir</button></div>';
        else html += '<div class="flex items-center gap-8"><span class="pill pill-clay">No aplica</span></div>';
        html += '</div>';
      });
      if (evento.asistentes.length === 0) html += '<div class="empty-state">Aún no hay convocados registrados.</div>';
      html += '</div>';
    }
  }
  html += '</div></div>';
  cont.innerHTML = html;
  renderModalEvento(); renderModalEventoPreview();
}
function seleccionarEvento(id) { EVENTO_ACTIVO = id; render(); }
function finalizarEvento(id) {
  const e = DATA.eventos.find(x=>x.id===id);
  e.estado = 'finalizado'; saveData(); render(); toast('Evento "' + e.nombre + '" marcado como finalizado');
}
function emitirCertificadoEvento(evId, estudiante) {
  const e = DATA.eventos.find(x=>x.id===evId);
  DATA.certificados.unshift({ id: uid('cert'), receptor: estudiante, origen: 'Evento: ' + e.nombre, codigo: 'BL-EVT-' + Math.floor(10000+Math.random()*89999), estado:'emitido', emitidoEn: new Date().toISOString().slice(0,10) });
  saveData(); render(); toast('Certificado emitido para ' + estudiante);
}
function emitirTodosPendientes(evId) {
  const e = DATA.eventos.find(x=>x.id===evId);
  const yaEmitidos = new Set(DATA.certificados.filter(c=>c.origen==='Evento: '+e.nombre).map(c=>c.receptor));
  const pendientes = e.asistentes.filter(a=>a.asistio && !yaEmitidos.has(a.estudiante));
  if (pendientes.length === 0) { toast('No hay certificados pendientes por emitir', true); return; }
  pendientes.forEach(a => DATA.certificados.unshift({ id: uid('cert'), receptor: a.estudiante, origen: 'Evento: ' + e.nombre, codigo: 'BL-EVT-' + Math.floor(10000+Math.random()*89999), estado:'emitido', emitidoEn: new Date().toISOString().slice(0,10) }));
  saveData(); render(); toast(pendientes.length + ' certificado(s) emitido(s)');
}
function abrirModalEvento(id) {
  const e = id ? DATA.eventos.find(x=>x.id===id) : null;
  document.getElementById('evt-modal-titulo').textContent = e ? 'Editar evento virtual' : 'Nuevo evento virtual';
  document.getElementById('evt-modal-btn').textContent = e ? 'Guardar cambios' : 'Crear evento';
  document.getElementById('evt-id').value = e ? e.id : '';
  document.getElementById('evt-nombre').value = e ? e.nombre : '';
  document.getElementById('evt-descripcion').value = e ? e.descripcion : '';
  document.getElementById('evt-plataforma').value = e ? e.plataforma : 'youtube';
  document.getElementById('evt-enlace').value = e ? (e.enlaceAcceso||'') : '';
  document.getElementById('evt-fecha').value = e ? e.fechaInicio : '';
  document.getElementById('evt-cert').checked = e ? e.emiteCertificado : false;
  abrirModal('modal-evento');
}
function renderModalEvento() {
  if (document.getElementById('modal-evento')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-evento"><div class="modal"><div class="modal-header"><h3 id="evt-modal-titulo">Nuevo evento virtual</h3><button class="modal-close" onclick="cerrarModal(\'modal-evento\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="evt-id">' +
    '<div class="campo"><label>Nombre</label><input type="text" id="evt-nombre"></div>' +
    '<div class="campo"><label>Descripción</label><textarea id="evt-descripcion" rows="2"></textarea></div>' +
    '<div class="grid-2"><div class="campo"><label>Plataforma</label><select id="evt-plataforma"><option value="youtube">YouTube Live</option><option value="meet">Google Meet</option><option value="zoom">Zoom</option><option value="teams">Microsoft Teams</option><option value="otro">Otro</option></select></div>' +
    '<div class="campo"><label>Fecha y hora</label><input type="datetime-local" id="evt-fecha"></div></div>' +
    '<div class="campo"><label>Enlace de acceso</label><input type="url" id="evt-enlace" placeholder="https://..."><p class="hint">YouTube se reproduce dentro de BioLearn; las demás plataformas se abren en pestaña nueva al unirse.</p></div>' +
    '<label class="flex items-center gap-8 text-sm mb-14" style="cursor:pointer"><input type="checkbox" id="evt-cert" style="width:auto"> Emitir certificado de asistencia</label></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-evento\')">Cancelar</button><button class="btn btn-primary" id="evt-modal-btn" onclick="guardarEvento()">Crear evento</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarEvento() {
  const id = document.getElementById('evt-id').value;
  const nombre = document.getElementById('evt-nombre').value.trim();
  if (!nombre) { toast('Escribe el nombre del evento', true); return; }
  const campos = { nombre, descripcion: document.getElementById('evt-descripcion').value.trim(),
    plataforma: document.getElementById('evt-plataforma').value, enlaceAcceso: document.getElementById('evt-enlace').value.trim(),
    fechaInicio: document.getElementById('evt-fecha').value, emiteCertificado: document.getElementById('evt-cert').checked };
  if (id) {
    Object.assign(DATA.eventos.find(x=>x.id===id), campos);
    saveData(); cerrarModal('modal-evento'); render(); toast('Evento "' + nombre + '" actualizado');
    return;
  }
  const nuevo = { id: uid('evt'), ...campos, fechaFin:'', estado:'programado', ponentes:'', organizadoPor:'', contenido:[], convocados:0, minutosAntes:10, grabacionUrl:'', asistentes:[] };
  DATA.eventos.unshift(nuevo);
  EVENTO_ACTIVO = nuevo.id;
  saveData(); cerrarModal('modal-evento'); render(); toast('Evento "' + nombre + '" creado · los estudiantes fueron notificados');
}
function agregarGrabacionEvento(id) {
  const e = DATA.eventos.find(x=>x.id===id);
  const url = prompt('Pega el enlace de la grabación (YouTube, Drive, Zoom cloud, etc.):', e.grabacionUrl||'');
  if (!url) return;
  e.grabacionUrl = url;
  saveData(); render(); toast('Grabación agregada y disponible para los estudiantes');
}
function confirmarEliminarEvento(id) {
  const e = DATA.eventos.find(x=>x.id===id);
  confirmarAccion('¿Eliminar el evento <strong>' + esc(e.nombre) + '</strong>? Se perderá el registro de asistencia y certificados asociados.', function () {
    DATA.eventos = DATA.eventos.filter(x=>x.id!==id);
    EVENTO_ACTIVO = null;
    saveData(); render(); toast('Evento eliminado');
  });
}
function abrirModalEventoPreview(id) {
  const e = DATA.eventos.find(x=>x.id===id);
  const rel = fmtRel(e.fechaInicio);
  const esProximo = e.estado === 'programado';
  document.getElementById('evtp-nombre').textContent = e.nombre;
  document.getElementById('evtp-nombre2').textContent = e.nombre;
  document.getElementById('evtp-nombre3').textContent = e.nombre;
  document.getElementById('evtp-meta').innerHTML = '<span>📅 ' + rel + '</span><span>🎥 ' + (PLATAFORMA_LABEL[e.plataforma]||e.plataforma) + '</span>' +
    (e.emiteCertificado ? '<span>🏅 Con certificado de participación</span>' : '') +
    '<span style="background:var(--amber-soft);color:#8A611E;font-weight:700;padding:3px 10px;border-radius:20px;font-size:12px">' + (esProximo?'Próximo':e.estado==='finalizado'?'Finalizado':'En curso') + '</span>';
  document.getElementById('evtp-banner-sub').textContent = (PLATAFORMA_LABEL[e.plataforma]||e.plataforma) + (e.organizadoPor ? ' · Organizado por ' + e.organizadoPor : '');
  document.getElementById('evtp-fecha').textContent = rel;
  document.getElementById('evtp-plataforma').textContent = PLATAFORMA_LABEL[e.plataforma] || e.plataforma;
  document.getElementById('evtp-ponentes-wrap').style.display = e.ponentes ? 'block' : 'none';
  document.getElementById('evtp-ponentes').textContent = e.ponentes || '';
  document.getElementById('evtp-cert-detalle').textContent = e.emiteCertificado ? 'Sí · se emite automáticamente al registrar asistencia' : 'No aplica';
  document.getElementById('evtp-live-badge').textContent = '● LIVE ' + rel.split(' ')[0].toLowerCase();
  document.getElementById('evtp-minutos').textContent = e.minutosAntes || 10;
  const contWrap = document.getElementById('evtp-contenido-wrap');
  if (e.contenido && e.contenido.length > 0) { contWrap.style.display = 'block'; document.getElementById('evtp-contenido').innerHTML = e.contenido.map(c=>'<li>'+esc(c)+'</li>').join(''); }
  else contWrap.style.display = 'none';
  document.getElementById('evtp-cert-wrap').style.display = e.emiteCertificado ? 'block' : 'none';
  document.getElementById('evtp-cert-nombre').textContent = e.nombre;
  document.getElementById('evtp-cert-texto').textContent = DATA.plantillaCertificado.textoReconocimiento;
  document.getElementById('evtp-cert-marca').innerHTML = '<img src="'+DATA.plantillaCertificado.logoImagen+'" alt="'+esc(DATA.plantillaCertificado.marcaInstitucional)+'" style="height:26px">';
  document.getElementById('evtp-cert-firma').innerHTML = (DATA.plantillaCertificado.firmaImagen ? '<img src="'+DATA.plantillaCertificado.firmaImagen+'" style="height:30px;display:block;margin:0 auto 2px">' : '') + esc(DATA.plantillaCertificado.firmaDocente);
  const color = DATA.plantillaCertificado.color || '#C08A3E';
  document.getElementById('evtp-cert-marca').style.color = '#2C4A42';
  document.getElementById('evtp-cert-certifica').style.color = color;
  document.getElementById('evtp-cert-firma').style.borderTopColor = color;
  document.getElementById('evtp-cert-firma').style.color = color;
  let duracionTxt = '';
  if (e.fechaInicio && e.fechaFin) {
    const horas = (new Date(e.fechaFin) - new Date(e.fechaInicio)) / 3600000;
    if (horas > 0) duracionTxt = 'Duración: ' + (horas % 1 === 0 ? horas : horas.toFixed(1)) + ' hora' + (horas===1?'':'s') + ' · ';
  }
  document.getElementById('evtp-cert-detalle-inf').textContent = duracionTxt + 'Emitido el ' + new Date().toISOString().slice(0,10);
  document.getElementById('evtp-cert-organizador').style.display = e.organizadoPor ? 'block' : 'none';
  document.getElementById('evtp-cert-organizador').textContent = e.organizadoPor ? 'organizada por ' + e.organizadoPor : '';
  document.getElementById('evtp-cert-codigo').textContent = 'BIO-' + new Date().getFullYear() + '-XXXX (se genera al asistir)';
  abrirModal('modal-evento-preview');
}
function renderModalEventoPreview() {
  if (document.getElementById('modal-evento-preview')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-evento-preview"><div class="modal grande"><div class="modal-header"><h3>Vista previa del estudiante</h3><button class="modal-close" onclick="cerrarModal(\'modal-evento-preview\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div style="background:#F7F3EC;border-radius:14px;padding:24px;border:1px solid #EFE6DA">' +
    '<span style="display:inline-flex;align-items:center;gap:6px;background:var(--clay-soft);color:var(--clay);font-size:12.5px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:14px">🎥 Evento virtual</span>' +
    '<h2 style="font-family:\'Fraunces\',serif;font-size:23px;color:#2C4A42;margin:0 0 8px" id="evtp-nombre"></h2>' +
    '<div class="flex items-center gap-14 flex-wrap" style="font-size:13px;color:#6B5B45;margin-bottom:18px" id="evtp-meta"></div>' +
    '<div style="background:linear-gradient(135deg,#8A1F3B,#B25943);border-radius:14px;padding:20px 24px;margin-bottom:18px">' +
    '<div style="color:#fff;font-weight:700;font-size:18px;margin-bottom:4px">🎥 <span id="evtp-nombre2"></span></div><div style="color:rgba(255,255,255,.85);font-size:13px" id="evtp-banner-sub"></div></div>' +
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:18px">' +
    '<div style="background:#fff;border-radius:10px;padding:14px;border:1px solid #EFE6DA"><div style="font-size:11px;font-weight:700;color:#8A6A4E;text-transform:uppercase;margin-bottom:4px">📅 Fecha y hora</div><div style="font-size:13.5px;font-weight:700" id="evtp-fecha"></div></div>' +
    '<div style="background:#fff;border-radius:10px;padding:14px;border:1px solid #EFE6DA"><div style="font-size:11px;font-weight:700;color:#8A6A4E;text-transform:uppercase;margin-bottom:4px">🎥 Plataforma</div><div style="font-size:13.5px;font-weight:700" id="evtp-plataforma"></div></div>' +
    '<div id="evtp-ponentes-wrap" style="display:none;background:#fff;border-radius:10px;padding:14px;border:1px solid #EFE6DA"><div style="font-size:11px;font-weight:700;color:#8A6A4E;text-transform:uppercase;margin-bottom:4px">🎤 Ponentes</div><div style="font-size:13.5px;font-weight:700" id="evtp-ponentes"></div></div>' +
    '<div style="background:#fff;border-radius:10px;padding:14px;border:1px solid #EFE6DA"><div style="font-size:11px;font-weight:700;color:#8A6A4E;text-transform:uppercase;margin-bottom:4px">🏅 Certificado</div><div style="font-size:13.5px;font-weight:700" id="evtp-cert-detalle"></div></div>' +
    '</div>' +
    '<div style="background:#241318;border-radius:14px;padding:24px;text-align:center;margin-bottom:18px">' +
    '<div style="width:62px;height:44px;border-radius:10px;background:#E12626;margin:0 auto 14px;display:flex;align-items:center;justify-content:center"><div style="width:0;height:0;border-top:9px solid transparent;border-bottom:9px solid transparent;border-left:14px solid #fff;margin-left:3px"></div></div>' +
    '<span style="background:#E12626;color:#fff;font-weight:700;font-size:12px;padding:4px 12px;border-radius:20px" id="evtp-live-badge"></span>' +
    '<p style="color:#fff;font-weight:700;font-size:15.5px;margin:14px 0 4px" id="evtp-nombre3"></p>' +
    '<p style="color:rgba(255,255,255,.6);font-size:12.5px;margin:0 0 16px">El enlace se activará <span id="evtp-minutos"></span> minutos antes del evento</p>' +
    '<button disabled style="background:#fff;border:none;border-radius:30px;padding:10px 20px;font-weight:700;font-size:13px;color:var(--ink);opacity:.9">🔔 Activar recordatorio</button></div>' +
    '<div id="evtp-contenido-wrap" style="display:none;background:#fff;border-radius:12px;padding:18px;border:1px solid #EFE6DA;margin-bottom:18px">' +
    '<div style="font-size:12.5px;font-weight:700;color:#8A611E;text-transform:uppercase;margin-bottom:8px">📋 Contenido del evento</div>' +
    '<ul id="evtp-contenido" style="margin:0;padding-left:20px;font-size:13.5px;line-height:1.8;color:var(--ink)"></ul></div>' +
    '<div id="evtp-cert-wrap" style="display:none;background:linear-gradient(135deg,#FBEBB5,#F3D98A);border:1.5px solid #E0B94A;border-radius:14px;padding:26px;text-align:center;margin-bottom:18px">' +
    '<div style="font-family:\'Fraunces\',serif;font-size:17px;color:#2C4A42;margin-bottom:4px" id="evtp-cert-marca"></div><div style="font-size:11.5px;letter-spacing:1px;margin-bottom:10px" id="evtp-cert-certifica">CERTIFICA QUE</div>' +
    '<div style="font-size:17px;font-weight:800;margin-bottom:8px">[Tu nombre]</div><div style="font-size:13px;color:#5A4A2A" id="evtp-cert-texto"></div><div style="font-size:13px;color:#5A4A2A;margin-bottom:6px">"<strong id="evtp-cert-nombre"></strong>"</div>' +
    '<div style="font-size:11px;color:#8A6A4E;margin-bottom:18px" id="evtp-cert-detalle-inf"></div>' +
    '<div id="evtp-cert-organizador" style="display:none;font-size:12.5px;color:#5A4A2A"></div>' +
    '<div style="font-size:11.5px;color:#7A6A3E;margin-top:10px;border-top:1px solid #E0B94A;padding-top:8px" id="evtp-cert-firma"></div>' +
    '<span style="display:inline-block;margin-top:10px;background:#F3E3B5;color:#8A611E;font-family:monospace;font-size:12px;padding:4px 12px;border-radius:8px" id="evtp-cert-codigo"></span></div>' +
    '<div style="background:#fff;border-radius:12px;padding:18px;border:1px solid #EFE6DA">' +
    '<div style="font-size:12.5px;font-weight:700;color:#2C4A42;text-transform:uppercase;margin-bottom:8px">✍️ Registrar mi asistencia al evento</div>' +
    '<p style="font-size:13px;color:var(--ink-soft);margin-bottom:12px">Ingresa el código que compartirá el docente durante el evento. Si cumples el tiempo mínimo, recibirás tu certificado automáticamente.</p>' +
    '<div class="flex gap-10"><input disabled placeholder="Código del evento..." style="flex:1;background:var(--bone)"><button disabled style="background:var(--sage);opacity:.85;border:none;border-radius:9px;padding:10px 18px;font-weight:700;color:#fff;font-size:13.5px">✅ Registrar asistencia</button></div></div>' +
    '</div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se ve para el estudiante. Los botones están desactivados en esta vista previa.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-evento-preview\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
