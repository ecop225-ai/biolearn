/* ================================================================
   BIOLearn — biolearn_contador_comunicacion.js
================================================================ */
let MSG_ACTIVO_CONT = null;
function renderMensajeria(cont) {
  if (MSG_ACTIVO_CONT === null && DATA.mensajes.length>0) MSG_ACTIVO_CONT = DATA.mensajes[0].id;
  const msg = DATA.mensajes.find(m=>m.id===MSG_ACTIVO_CONT);
  let html = '<div class="section-header"><div><h2>Mensajería</h2><p class="subtitle">Comunícate con estudiantes sobre pagos o con administración</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalMensajeCont()">+ Nuevo mensaje</button></div>';
  html += '<div class="flex gap-14" style="min-height:380px;align-items:flex-start">';
  html += '<div class="card" style="width:300px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.mensajes.forEach(m => {
    html += '<div onclick="seleccionarMensajeCont(\''+m.id+'\')" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:'+(m.id===MSG_ACTIVO_CONT?'var(--blue-soft)':(m.leido?'transparent':'var(--bone)'))+'">' +
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
    html += '<div style="border-top:1px solid var(--line);padding-top:14px"><div class="campo"><label>Responder</label><textarea id="msg-reply-cont" rows="3" placeholder="Escribe tu respuesta..."></textarea></div>' +
      '<button class="btn btn-primary btn-sm" onclick="responderMensajeCont(\''+msg.id+'\')">✉ Enviar respuesta</button></div>';
  } else html += '<div class="empty-state">Selecciona un mensaje</div>';
  html += '</div></div>';
  cont.innerHTML = html;
  renderModalMensajeCont();
}
function seleccionarMensajeCont(id) { MSG_ACTIVO_CONT = id; const m = DATA.mensajes.find(x=>x.id===id); m.leido = true; saveData(); render(); }
function responderMensajeCont(id) {
  const texto = document.getElementById('msg-reply-cont').value.trim();
  if (!texto) { toast('Escribe una respuesta antes de enviar', true); return; }
  const m = DATA.mensajes.find(x=>x.id===id);
  if (!m.respuestas) m.respuestas = [];
  m.respuestas.push({ de:CONTADOR.nombre, cuerpo:texto, fecha:new Date().toISOString().slice(0,10) });
  saveData(); render(); toast('Respuesta enviada a ' + m.de);
}
function abrirModalMensajeCont() {
  const estudiantes = [...new Set(DATA.pagos.map(p=>p.estudiante))];
  document.getElementById('msgn-cont-tipo').value = 'estudiante';
  document.getElementById('msgn-cont-para').innerHTML = estudiantes.map(e=>'<option value="'+esc(e)+'">'+esc(e)+'</option>').join('');
  document.getElementById('msgn-cont-asunto').value = ''; document.getElementById('msgn-cont-cuerpo').value = '';
  actualizarTipoMensajeCont();
  abrirModal('modal-mensaje-cont');
}
function actualizarTipoMensajeCont() {
  const tipo = document.getElementById('msgn-cont-tipo').value;
  document.getElementById('msgn-cont-wrap-estudiante').style.display = tipo==='estudiante' ? 'block' : 'none';
  document.getElementById('msgn-cont-nota-admin').style.display = tipo==='admin' ? 'block' : 'none';
}
function renderModalMensajeCont() {
  if (document.getElementById('modal-mensaje-cont')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-mensaje-cont"><div class="modal"><div class="modal-header"><h3>Nuevo mensaje</h3><button class="modal-close" onclick="cerrarModal(\'modal-mensaje-cont\')">✕</button></div>' +
    '<div class="modal-body"><div class="campo"><label>Tipo de destinatario</label><select id="msgn-cont-tipo" onchange="actualizarTipoMensajeCont()"><option value="estudiante">Estudiante</option><option value="admin">Administración</option></select></div>' +
    '<div id="msgn-cont-wrap-estudiante" class="campo"><label>Estudiante</label><select id="msgn-cont-para"></select></div>' +
    '<p id="msgn-cont-nota-admin" class="text-sm muted" style="display:none;margin:-6px 0 14px">Tu mensaje será atendido por el equipo administrativo de BioLearn.</p>' +
    '<div class="campo"><label>Asunto</label><input id="msgn-cont-asunto"></div><div class="campo"><label>Mensaje</label><textarea id="msgn-cont-cuerpo" rows="4"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-mensaje-cont\')">Cancelar</button><button class="btn btn-primary" onclick="enviarMensajeCont()">✉ Enviar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function enviarMensajeCont() {
  const tipo = document.getElementById('msgn-cont-tipo').value, asunto = document.getElementById('msgn-cont-asunto').value.trim(), cuerpo = document.getElementById('msgn-cont-cuerpo').value.trim();
  if (!asunto || !cuerpo) { toast('Escribe el asunto y el mensaje', true); return; }
  const para = tipo==='estudiante' ? document.getElementById('msgn-cont-para').value : 'administración';
  cerrarModal('modal-mensaje-cont');
  toast('Mensaje enviado a ' + para);
}

/* ---------------------------- Mi perfil ---------------------------- */
function renderPerfil(cont) {
  let html = '<div class="section-header"><div><h2>Mi perfil</h2><p class="subtitle">Edita tu información y revisa tu historial de acceso</p></div></div>';
  html += '<div class="flex gap-18 flex-wrap">';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 16px">👤 Información personal</h3>' +
    '<div class="campo"><label>Nombre completo</label><input id="perfil-cont-nombre" value="' + esc(CONTADOR.nombre) + '"></div>' +
    '<div class="campo"><label>Correo electrónico</label><input id="perfil-cont-email" value="' + esc(CONTADOR.email) + '"></div>' +
    '<div class="campo"><label>Cargo</label><input value="' + esc(CONTADOR.cargo) + '" disabled></div>' +
    '<button class="btn btn-primary" onclick="guardarPerfilCont()">Guardar cambios</button>' +
    '<h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:26px 0 16px">🔑 Seguridad</h3>' +
    '<div class="campo"><label>Nueva contraseña</label><input type="password" id="perfil-cont-pass" placeholder="••••••••"></div>' +
    '<button class="btn btn-ghost" onclick="actualizarPasswordCont()">Actualizar contraseña</button></div>';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-family:\'Fraunces\',serif;font-size:16px;margin:0 0 12px">🛡️ Historial de acceso</h3>';
  DATA.historial.forEach(h => html += '<div class="flex justify-between text-sm" style="padding:8px 0;border-top:1px solid var(--line)"><span>' + esc(h.evento) + '</span><span class="muted">' + esc(h.fecha) + '</span></div>');
  html += '<div style="margin-top:20px;padding-top:16px;border-top:1px dashed var(--line)"><button class="btn btn-danger" onclick="confirmarAccion(\'¿Restablecer todos los datos de prueba? Esta acción no se puede deshacer.\', resetData, \'Restablecer\')">🔄 Restablecer datos del prototipo</button></div></div>';
  html += '</div>';
  cont.innerHTML = html;
}
function guardarPerfilCont() { CONTADOR.nombre = document.getElementById('perfil-cont-nombre').value.trim() || CONTADOR.nombre; render(); toast('Perfil actualizado'); }
function actualizarPasswordCont() { document.getElementById('perfil-cont-pass').value=''; toast('Contraseña actualizada'); }
