/* ================================================================
   BIOLearn — biolearn_supervisor_comunicacion.js
   Mensajería, Mi perfil
================================================================ */
let MSG_ACTIVO_SUP = null;
function renderMensajeria(cont) {
  if (MSG_ACTIVO_SUP === null && DATA.mensajes.length>0) MSG_ACTIVO_SUP = DATA.mensajes[0].id;
  const msg = DATA.mensajes.find(m=>m.id===MSG_ACTIVO_SUP);
  let html = '<div class="section-header"><div><h2>Mensajería</h2><p class="subtitle">Comunícate con los docentes de tus estudiantes o con administración</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalMensajeSup()">+ Nuevo mensaje</button></div>';
  html += '<div class="flex gap-14" style="min-height:380px;align-items:flex-start">';
  html += '<div class="card" style="width:300px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.mensajes.forEach(m => {
    html += '<div onclick="seleccionarMensajeSup(\''+m.id+'\')" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:'+(m.id===MSG_ACTIVO_SUP?'var(--blue-soft)':(m.leido?'transparent':'var(--bone)'))+'">' +
      '<div class="flex justify-between"><span style="font-size:13.5px;font-weight:'+(m.leido?'500':'700')+'">' + esc(m.de) + '</span><span class="text-sm muted" style="font-size:11px">' + m.fecha + '</span></div>' +
      '<div style="font-size:12.5px;font-weight:'+(m.leido?'400':'600')+'">' + esc(m.asunto) + '</div></div>';
  });
  html += '</div><div class="card" style="flex:1;min-width:0">';
  if (msg) {
    html += '<h3 style="font-family:\'Fraunces\',serif;font-size:18px;margin:0 0 6px">' + esc(msg.asunto) + '</h3><p class="text-sm muted mb-14">' + esc(msg.de) + ' · ' + msg.fecha + '</p>' +
      '<p style="font-size:14.5px;line-height:1.7;margin-bottom:16px">' + esc(msg.cuerpo) + '</p>';
    (msg.respuestas||[]).forEach(r => {
      html += '<div style="background:var(--blue-soft);border-radius:10px;padding:12px 16px;margin-bottom:10px"><div class="flex justify-between"><span style="font-size:12.5px;font-weight:700;color:var(--blue-deep)">' + esc(r.de) + '</span><span class="text-sm muted" style="font-size:11px">' + esc(r.fecha) + '</span></div><p style="font-size:13.5px;margin:4px 0 0">' + esc(r.cuerpo) + '</p></div>';
    });
    html += '<div style="border-top:1px solid var(--line);padding-top:14px"><div class="campo"><label>Responder</label><textarea id="msg-reply-sup" rows="3" placeholder="Escribe tu respuesta..."></textarea></div>' +
      '<button class="btn btn-primary btn-sm" onclick="responderMensajeSup(\''+msg.id+'\')">✉ Enviar respuesta</button></div>';
  } else html += '<div class="empty-state">Selecciona un mensaje</div>';
  html += '</div></div>';
  cont.innerHTML = html;
  renderModalMensajeSup();
}
function seleccionarMensajeSup(id) { MSG_ACTIVO_SUP = id; const m = DATA.mensajes.find(x=>x.id===id); m.leido = true; saveData(); render(); }
function responderMensajeSup(id) {
  const texto = document.getElementById('msg-reply-sup').value.trim();
  if (!texto) { toast('Escribe una respuesta antes de enviar', true); return; }
  const m = DATA.mensajes.find(x=>x.id===id);
  if (!m.respuestas) m.respuestas = [];
  m.respuestas.push({ de:SUPERVISOR.nombre, cuerpo:texto, fecha:new Date().toISOString().slice(0,10) });
  saveData(); render(); toast('Respuesta enviada a ' + m.de);
}
function abrirModalMensajeSup() {
  const docentes = [...new Set(DATA.estudiantes.map(e=>e.docente))];
  const cursos = [...new Set(DATA.estudiantes.map(e=>e.curso))];
  document.getElementById('msgn-sup-tipo').value = 'docente';
  document.getElementById('msgn-sup-para').innerHTML = docentes.map(d=>'<option value="'+esc(d)+'">'+esc(d)+'</option>').join('');
  document.getElementById('msgn-sup-curso').innerHTML = cursos.map(c=>'<option value="'+esc(c)+'">'+esc(c)+' — '+DATA.estudiantes.filter(e=>e.curso===c).length+' estudiante(s)</option>').join('');
  document.getElementById('msgn-sup-estudiante').innerHTML = DATA.estudiantes.map(e=>'<option value="'+esc(e.nombre)+'">'+esc(e.nombre)+' — '+esc(e.curso)+'</option>').join('');
  document.getElementById('msgn-sup-asunto').value = ''; document.getElementById('msgn-sup-cuerpo').value = '';
  actualizarTipoMensajeSup();
  abrirModal('modal-mensaje-sup');
}
function actualizarTipoMensajeSup() {
  const tipo = document.getElementById('msgn-sup-tipo').value;
  document.getElementById('msgn-sup-wrap-docente').style.display = tipo==='docente' ? 'block' : 'none';
  document.getElementById('msgn-sup-wrap-curso').style.display = tipo==='curso' ? 'block' : 'none';
  document.getElementById('msgn-sup-wrap-estudiante').style.display = tipo==='estudiante' ? 'block' : 'none';
  document.getElementById('msgn-sup-nota-admin').style.display = tipo==='admin' ? 'block' : 'none';
}
function renderModalMensajeSup() {
  if (document.getElementById('modal-mensaje-sup')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-mensaje-sup"><div class="modal"><div class="modal-header"><h3>Nuevo mensaje</h3><button class="modal-close" onclick="cerrarModal(\'modal-mensaje-rep\')">✕</button></div>' +
    '<div class="modal-body"><div class="campo"><label>Enviar a *</label><select id="msgn-sup-tipo" onchange="actualizarTipoMensajeSup()">' +
    '<option value="docente">👩‍🏫 Docente de un estudiante</option>' +
    '<option value="curso">🎓 Todos los estudiantes de un curso</option>' +
    '<option value="estudiante">👤 Un estudiante específico</option>' +
    '<option value="admin">🏫 Administración</option></select></div>' +
    '<div id="msgn-sup-wrap-docente" class="campo"><label>Docente</label><select id="msgn-sup-para"></select></div>' +
    '<div id="msgn-sup-wrap-curso" class="campo" style="display:none"><label>Curso</label><select id="msgn-sup-curso"></select></div>' +
    '<div id="msgn-sup-wrap-estudiante" class="campo" style="display:none"><label>Estudiante</label><select id="msgn-sup-estudiante"></select></div>' +
    '<p id="msgn-sup-nota-admin" class="text-sm muted" style="display:none;margin:-6px 0 14px">Tu mensaje será atendido por el equipo administrativo de BioLearn.</p>' +
    '<div class="campo"><label>Asunto</label><input id="msgn-sup-asunto"></div><div class="campo"><label>Mensaje</label><textarea id="msgn-sup-cuerpo" rows="4"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-mensaje-rep\')">Cancelar</button><button class="btn btn-primary" onclick="enviarMensajeSup()">✉ Enviar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function enviarMensajeSup() {
  const tipo = document.getElementById('msgn-sup-tipo').value, asunto = document.getElementById('msgn-sup-asunto').value.trim(), cuerpo = document.getElementById('msgn-sup-cuerpo').value.trim();
  if (!asunto || !cuerpo) { toast('Escribe el asunto y el mensaje', true); return; }
  cerrarModal('modal-mensaje-sup');
  if (tipo === 'curso') {
    const curso = document.getElementById('msgn-sup-curso').value;
    const n = DATA.estudiantes.filter(e=>e.curso===curso).length;
    toast('Mensaje enviado a los ' + n + ' estudiantes de "' + curso + '"');
  } else if (tipo === 'estudiante') {
    toast('Mensaje enviado a ' + document.getElementById('msgn-sup-estudiante').value);
  } else if (tipo === 'admin') {
    toast('Mensaje enviado a administración');
  } else {
    toast('Mensaje enviado a ' + document.getElementById('msgn-sup-para').value);
  }
}

/* ---------------------------- Mi perfil ---------------------------- */
function renderPerfil(cont) {
  let html = '<div class="section-header"><div><h2>Mi perfil</h2><p class="subtitle">Edita tu información y revisa tu historial de acceso</p></div></div>';
  html += '<div class="flex gap-18 flex-wrap">';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 16px">👤 Información personal</h3>' +
    '<div class="campo"><label>Nombre completo</label><input id="perfil-sup-nombre" value="' + esc(SUPERVISOR.nombre) + '"></div>' +
    '<div class="campo"><label>Correo electrónico</label><input id="perfil-sup-email" value="' + esc(SUPERVISOR.email) + '"></div>' +
    '<div class="campo"><label>Cargo</label><input value="' + esc(SUPERVISOR.cargo) + '" disabled></div>' +
    '<button class="btn btn-primary" onclick="guardarPerfilSup()">Guardar cambios</button>' +
    '<h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:26px 0 16px">🔑 Seguridad</h3>' +
    '<div class="campo"><label>Nueva contraseña</label><input type="password" id="perfil-sup-pass" placeholder="••••••••"></div>' +
    '<button class="btn btn-ghost" onclick="actualizarPasswordSup()">Actualizar contraseña</button></div>';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-family:\'Fraunces\',serif;font-size:16px;margin:0 0 12px">🛡️ Historial de acceso</h3>';
  DATA.historial.forEach(h => html += '<div class="flex justify-between text-sm" style="padding:8px 0;border-top:1px solid var(--line)"><span>' + esc(h.evento) + '</span><span class="muted">' + esc(h.fecha) + '</span></div>');
  html += '<div style="margin-top:20px;padding-top:16px;border-top:1px dashed var(--line)"><button class="btn btn-danger" onclick="confirmarAccion(\'¿Restablecer todos los datos de prueba? Esta acción no se puede deshacer.\', resetData, \'Restablecer\')">🔄 Restablecer datos del prototipo</button></div></div>';
  html += '</div>';
  cont.innerHTML = html;
}
function guardarPerfilSup() { SUPERVISOR.nombre = document.getElementById('perfil-sup-nombre').value.trim() || SUPERVISOR.nombre; render(); toast('Perfil actualizado'); }
function actualizarPasswordSup() { document.getElementById('perfil-sup-pass').value=''; toast('Contraseña actualizada'); }
