/* ================================================================
   BIOLearn — biolearn_docente_comunicacion.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: MENSAJERÍA
================================================================ */
let MENSAJE_ACTIVO = null;
const MSG_AVATAR_COLORES = ['#5C7A63','#C08A3E','#7A5AA8','#33526E','#B25943'];
function msgAvatarColor(nombre) {
  let h = 0; for (let i=0;i<nombre.length;i++) h = (h + nombre.charCodeAt(i)) % MSG_AVATAR_COLORES.length;
  return MSG_AVATAR_COLORES[h];
}
function renderMensajeria(cont) {
  if (MENSAJE_ACTIVO === null && DATA.mensajes.length > 0) MENSAJE_ACTIVO = DATA.mensajes[0].id;
  const msg = DATA.mensajes.find(m=>m.id===MENSAJE_ACTIVO);
  let html = '<div class="section-header"><div><h2>Mensajería</h2><p class="subtitle">Comunícate con tus estudiantes, otros docentes o administración.</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalMensajeNuevo()">+ Nuevo mensaje</button></div>';
  html += '<div class="flex gap-14" style="min-height:380px;align-items:flex-start">';
  html += '<div class="card" style="width:300px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.mensajes.forEach(m => {
    const color = msgAvatarColor(m.de);
    const iniciales = m.de.split(' ').map(p=>p[0]).slice(0,2).join('');
    html += '<div onclick="seleccionarMensaje(\''+m.id+'\')" class="flex items-center gap-10" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:' + (m.id===MENSAJE_ACTIVO?'var(--blue-soft)':(m.leido?'transparent':'var(--bone)')) + '">' +
      '<div style="width:36px;height:36px;border-radius:50%;background:'+color+';color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px;flex-shrink:0">'+esc(iniciales)+'</div>' +
      '<div style="flex:1;min-width:0"><div class="flex justify-between"><span style="font-size:13.5px;font-weight:'+(m.leido?'500':'700')+'">' + esc(m.de) + '</span><span class="text-sm muted" style="font-size:11px">' + m.fecha + '</span></div>' +
      '<div style="font-size:12.5px;font-weight:'+(m.leido?'400':'600')+';white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + esc(m.asunto) + '</div></div>' +
      (m.leido ? '' : '<span style="width:8px;height:8px;border-radius:50%;background:var(--clay);flex-shrink:0"></span>') + '</div>';
  });
  if (DATA.mensajes.length === 0) html += '<div class="empty-state">No tienes mensajes.</div>';
  html += '</div>';
  html += '<div class="card" style="flex:1;min-width:0">';
  if (msg) {
    const color = msgAvatarColor(msg.de);
    const iniciales = msg.de.split(' ').map(p=>p[0]).slice(0,2).join('');
    html += '<div class="flex items-center gap-10 mb-14"><div style="width:38px;height:38px;border-radius:50%;background:'+color+';color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700">'+esc(iniciales)+'</div>' +
      '<div><h3 style="font-size:17px;margin:0">' + esc(msg.de) + '</h3><p class="text-sm muted" style="margin:0">' + esc(msg.asunto) + ' · ' + msg.fecha + '</p></div></div>' +
      '<p style="font-size:14.5px;line-height:1.7;margin-bottom:16px;background:var(--bone);border-radius:10px;padding:14px 16px">' + esc(msg.cuerpo) + '</p>';
    (msg.respuestas||[]).forEach(r => {
      html += '<div style="background:var(--blue-soft);border-radius:10px;padding:12px 16px;margin-bottom:10px;margin-left:40px">' +
        '<div class="flex justify-between"><span style="font-size:12.5px;font-weight:700;color:var(--blue)">' + esc(r.de) + '</span><span class="text-sm muted" style="font-size:11px">' + esc(r.fecha) + '</span></div>' +
        '<p style="font-size:13.5px;margin:4px 0 0">' + esc(r.cuerpo) + '</p></div>';
    });
    html += '<div style="border-top:1px solid var(--line);padding-top:14px;margin-top:6px">' +
      '<div class="campo"><label>Responder</label><textarea id="msg-reply-'+msg.id+'" rows="3" placeholder="Escribe tu respuesta..."></textarea></div>' +
      '<button class="btn btn-primary btn-sm" onclick="enviarRespuestaMensaje(\''+msg.id+'\')">✉ Enviar respuesta</button></div>';
  } else html += '<div class="empty-state">Selecciona un mensaje.</div>';
  html += '</div></div>';
  cont.innerHTML = html;
  renderModalMensajeNuevo();
}
function enviarRespuestaMensaje(id) {
  const textarea = document.getElementById('msg-reply-'+id);
  const texto = textarea.value.trim();
  if (!texto) { toast('Escribe una respuesta antes de enviar', true); return; }
  const m = DATA.mensajes.find(x=>x.id===id);
  if (!m.respuestas) m.respuestas = [];
  m.respuestas.push({ de: DOCENTE.nombre, cuerpo: texto, fecha: new Date().toISOString().slice(0,10) });
  saveData(); render();
  toast('Respuesta enviada a ' + m.de);
}
function seleccionarMensaje(id) {
  MENSAJE_ACTIVO = id;
  const m = DATA.mensajes.find(x=>x.id===id);
  m.leido = true; saveData(); render();
}
function msgnFiltrarEstudiantes() {
  const q = document.getElementById('msgn-para-search').value.trim().toLowerCase();
  const lista = DATA.estudiantes.filter(e => e.nombre.toLowerCase().includes(q));
  const cont = document.getElementById('msgn-para-sugerencias');
  document.getElementById('msgn-para-input').value = '';
  if (lista.length === 0) { cont.innerHTML = '<div class="rte-menu-item muted">Sin resultados</div>'; cont.classList.add('abierto'); return; }
  cont.innerHTML = lista.map(e => '<div class="rte-menu-item" onmousedown="event.preventDefault()" onclick="msgnSeleccionarEstudiante(\''+esc(e.nombre).replace(/'/g,"\\'")+'\')">'+esc(e.nombre)+' <span class="text-sm muted">· '+esc(e.curso)+'</span></div>').join('');
  cont.classList.add('abierto');
}
function msgnSeleccionarEstudiante(nombre) {
  document.getElementById('msgn-para-search').value = nombre;
  document.getElementById('msgn-para-input').value = nombre;
  document.getElementById('msgn-para-sugerencias').classList.remove('abierto');
}
function abrirModalMensajeNuevo() {
  document.getElementById('msgn-tipo').value = ''; document.getElementById('msgn-para-input').value = '';
  document.getElementById('msgn-para-search').value = '';
  document.getElementById('msgn-curso').value = '';
  document.getElementById('msgn-asunto').value = ''; document.getElementById('msgn-cuerpo').value = '';
  actualizarCampoDestinatarioMensaje();
  abrirModal('modal-mensaje-nuevo');
}
function actualizarCampoDestinatarioMensaje() {
  const tipo = document.getElementById('msgn-tipo').value;
  document.getElementById('msgn-curso-wrap').style.display = tipo === 'curso' ? 'block' : 'none';
  document.getElementById('msgn-para-wrap').style.display = tipo === 'individual' ? 'block' : 'none';
}
function renderModalMensajeNuevo() {
  if (document.getElementById('modal-mensaje-nuevo')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-mensaje-nuevo"><div class="modal"><div class="modal-header"><h3>✉ Nuevo mensaje</h3><button class="modal-close" onclick="cerrarModal(\'modal-mensaje-nuevo\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div class="campo"><label>Enviar a *</label><select id="msgn-tipo" onchange="actualizarCampoDestinatarioMensaje()">' +
    '<option value="">-- Selecciona el tipo de envío --</option>' +
    '<option value="todos">🎓 Todos mis estudiantes</option>' +
    '<option value="curso">🎓 Todos los estudiantes de un curso</option>' +
    '<option value="individual">👤 Un estudiante específico</option>' +
    '<option value="admin">🏫 Administrador</option>' +
    '<option value="supervisor">👁 Supervisora</option>' +
    '</select></div>' +
    '<div id="msgn-curso-wrap" class="campo" style="display:none"><label>Curso</label><select id="msgn-curso">' + DATA.cursos.map(c=>'<option value="'+esc(c.nombre)+'">'+esc(c.nombre)+' ('+c.inscritos+' estudiantes)</option>').join('') + '</select></div>' +
    '<div id="msgn-para-wrap" class="campo rte-dropdown" style="display:none"><label>Estudiante</label>' +
    '<input type="text" id="msgn-para-search" placeholder="Escribe el nombre del estudiante..." autocomplete="off" oninput="msgnFiltrarEstudiantes()" onfocus="msgnFiltrarEstudiantes()">' +
    '<input type="hidden" id="msgn-para-input">' +
    '<div class="rte-menu" id="msgn-para-sugerencias" style="width:100%;max-width:none;max-height:220px;overflow-y:auto"></div></div>' +
    '<div class="campo"><label>Asunto</label><input type="text" id="msgn-asunto"></div>' +
    '<div class="campo"><label>Mensaje</label><textarea id="msgn-cuerpo" rows="4"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-mensaje-nuevo\')">Cancelar</button><button class="btn btn-primary" onclick="enviarMensajeNuevo()">✉ Enviar mensaje</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function enviarMensajeNuevo() {
  const tipo = document.getElementById('msgn-tipo').value;
  const asunto = document.getElementById('msgn-asunto').value.trim(), cuerpo = document.getElementById('msgn-cuerpo').value.trim();
  if (!tipo) { toast('Selecciona a quién enviar el mensaje', true); return; }
  if (!asunto || !cuerpo) { toast('Escribe el asunto y el mensaje', true); return; }
  if (tipo === 'individual' && !document.getElementById('msgn-para-input').value) { toast('Busca y selecciona un estudiante de la lista', true); return; }
  cerrarModal('modal-mensaje-nuevo');
  if (tipo === 'todos') toast('Mensaje enviado a todos tus estudiantes');
  else if (tipo === 'curso') toast('Mensaje enviado a "' + document.getElementById('msgn-curso').value + '"');
  else if (tipo === 'individual') toast('Mensaje enviado a ' + (document.getElementById('msgn-para-input').value || 'el estudiante seleccionado'));
  else toast('Mensaje enviado a ' + (tipo === 'admin' ? 'Administración' : 'Supervisora'));
}

/* ================================================================
   VISTA: PERFIL
================================================================ */
function renderPerfil(cont) {
  let html = '<div class="section-header"><div><h2>Mi perfil</h2><p class="subtitle">Edita tu información, revisa tus estadísticas y tu historial de acceso.</p></div></div>';
  html += '<div class="flex gap-14 flex-wrap">';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-size:17px;margin:0 0 16px">👤 Información personal</h3>' +
    '<div class="campo"><label>Nombre completo</label><input type="text" id="perfil-nombre" value="' + esc(DOCENTE.nombre) + '"></div>' +
    '<div class="campo"><label>Correo electrónico</label><input type="text" id="perfil-email" value="' + esc(DOCENTE.email) + '"></div>' +
    '<button class="btn btn-primary" onclick="guardarPerfilDocente()">Guardar cambios</button>' +
    '<h3 style="font-size:17px;margin:26px 0 16px">🔑 Seguridad</h3>' +
    '<div class="campo"><label>Nueva contraseña</label><input type="password" id="perfil-pass" placeholder="••••••••"></div>' +
    '<button class="btn btn-ghost" onclick="actualizarPasswordDocente()">Actualizar contraseña</button></div>';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-size:17px;margin:0 0 16px">🏅 Mis estadísticas</h3>';
  [['Cursos publicados', DATA.estadisticas.cursosPublicados], ['Estudiantes totales', DATA.estadisticas.estudiantesTotal],
   ['Calificación promedio', DATA.estadisticas.calificacionPromedio], ['Tasa de finalización', DATA.estadisticas.tasaFinalizacion+'%']].forEach(([l,v]) => {
    html += '<div class="flex justify-between text-sm" style="padding:8px 0;border-top:1px solid var(--line)"><span class="muted">' + l + '</span><span style="font-weight:700">' + v + '</span></div>';
  });
  html += '<h3 style="font-size:16px;margin:20px 0 12px">🛡️ Historial de acceso</h3>';
  DATA.historial.forEach(h => { html += '<div class="flex justify-between text-sm" style="padding:8px 0;border-top:1px solid var(--line)"><span>' + esc(h.evento) + '</span><span class="muted">' + esc(h.fecha) + '</span></div>'; });
  html += '<div style="margin-top:20px;padding-top:16px;border-top:1px dashed var(--line)"><button class="btn btn-danger" onclick="confirmarAccion(\'¿Restablecer todos los datos de prueba? Esta acción no se puede deshacer.\', resetData, \'Restablecer\')">🔄 Restablecer datos del prototipo</button></div></div>';
  html += '</div>';
  cont.innerHTML = html;
}
function guardarPerfilDocente() { toast('Perfil actualizado'); }
function actualizarPasswordDocente() { document.getElementById('perfil-pass').value = ''; toast('Contraseña actualizada'); }
