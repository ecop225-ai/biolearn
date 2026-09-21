/* ================================================================
   BIOLearn — biolearn_estudiante_comunicacion.js
   Mensajería, Mis pagos, Mi perfil
================================================================ */
let MENSAJE_ACTIVO_EST = null;
const MSG_COLORES_EST = ['#5C7A63','#C08A3E','#7A5AA8','#33526E','#B25943'];
function msgColorEst(nombre) { let h=0; for (let i=0;i<nombre.length;i++) h=(h+nombre.charCodeAt(i))%MSG_COLORES_EST.length; return MSG_COLORES_EST[h]; }

function renderMensajeria(cont) {
  if (MENSAJE_ACTIVO_EST === null && DATA.mensajes.length>0) MENSAJE_ACTIVO_EST = DATA.mensajes[0].id;
  const msg = DATA.mensajes.find(m=>m.id===MENSAJE_ACTIVO_EST);
  let html = '<div class="section-header"><div><h2>Mensajería</h2><p class="subtitle">Comunícate con tus docentes o administración</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalMensajeNuevoEst()">+ Nuevo mensaje</button></div>';
  html += '<div class="flex gap-14" style="min-height:380px;align-items:flex-start">';
  html += '<div class="card" style="width:300px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.mensajes.forEach(m => {
    const color = msgColorEst(m.de), iniciales = m.de.split(' ').map(p=>p[0]).slice(0,2).join('');
    html += '<div onclick="seleccionarMensajeEst(\''+m.id+'\')" class="flex items-center gap-10" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:'+(m.id===MENSAJE_ACTIVO_EST?'var(--blue-soft)':(m.leido?'transparent':'var(--bone)'))+'">' +
      '<div style="width:36px;height:36px;border-radius:50%;background:'+color+';color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px;flex-shrink:0">'+esc(iniciales)+'</div>' +
      '<div style="flex:1;min-width:0"><div class="flex justify-between"><span style="font-size:13.5px;font-weight:'+(m.leido?'500':'700')+'">'+esc(m.de)+'</span><span class="text-sm muted" style="font-size:11px">'+m.fecha+'</span></div>' +
      '<div style="font-size:12.5px;font-weight:'+(m.leido?'400':'600')+';white-space:nowrap;overflow:hidden;text-overflow:ellipsis">'+esc(m.asunto)+'</div></div>' +
      (m.leido?'':'<span style="width:8px;height:8px;border-radius:50%;background:var(--clay);flex-shrink:0"></span>') + '</div>';
  });
  if (DATA.mensajes.length === 0) html += '<div class="empty-state">No tienes mensajes.</div>';
  html += '</div><div class="card" style="flex:1;min-width:0">';
  if (msg) {
    const color = msgColorEst(msg.de), iniciales = msg.de.split(' ').map(p=>p[0]).slice(0,2).join('');
    html += '<div class="flex items-center gap-10 mb-14"><div style="width:38px;height:38px;border-radius:50%;background:'+color+';color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700">'+esc(iniciales)+'</div>' +
      '<div><h3 style="font-size:17px;margin:0">'+esc(msg.de)+'</h3><p class="text-sm muted" style="margin:0">'+esc(msg.asunto)+' · '+msg.fecha+'</p></div></div>' +
      '<p style="font-size:14.5px;line-height:1.7;margin-bottom:16px;background:var(--bone);border-radius:10px;padding:14px 16px">' + esc(msg.cuerpo) + '</p>';
    (msg.respuestas||[]).forEach(r => {
      html += '<div style="background:var(--blue-soft);border-radius:10px;padding:12px 16px;margin-bottom:10px;margin-left:40px"><div class="flex justify-between"><span style="font-size:12.5px;font-weight:700;color:var(--blue-deep)">'+esc(r.de)+'</span><span class="text-sm muted" style="font-size:11px">'+esc(r.fecha)+'</span></div><p style="font-size:13.5px;margin:4px 0 0">'+esc(r.cuerpo)+'</p></div>';
    });
    html += '<div style="border-top:1px solid var(--line);padding-top:14px;margin-top:6px"><div class="campo"><label>Responder</label><textarea id="msg-reply-'+msg.id+'" rows="3" placeholder="Escribe tu respuesta..."></textarea></div>' +
      '<button class="btn btn-primary btn-sm" onclick="enviarRespuestaMensajeEst(\''+msg.id+'\')">✉ Enviar respuesta</button></div>';
  } else html += '<div class="empty-state">Selecciona un mensaje.</div>';
  html += '</div></div>';
  cont.innerHTML = html;
  renderModalMensajeNuevoEst();
}
function seleccionarMensajeEst(id) { MENSAJE_ACTIVO_EST = id; const m = DATA.mensajes.find(x=>x.id===id); m.leido = true; saveData(); render(); }
function enviarRespuestaMensajeEst(id) {
  const texto = document.getElementById('msg-reply-'+id).value.trim();
  if (!texto) { toast('Escribe una respuesta antes de enviar', true); return; }
  const m = DATA.mensajes.find(x=>x.id===id);
  if (!m.respuestas) m.respuestas = [];
  m.respuestas.push({ de:ESTUDIANTE.nombre, cuerpo:texto, fecha:new Date().toISOString().slice(0,10) });
  saveData(); render();
  toast('Respuesta enviada a ' + m.de);
}
function abrirModalMensajeNuevoEst() {
  document.getElementById('msgn-est-tipo').value = ''; document.getElementById('msgn-est-asunto').value = ''; document.getElementById('msgn-est-cuerpo').value = '';
  abrirModal('modal-mensaje-nuevo-est');
}
function renderModalMensajeNuevoEst() {
  if (document.getElementById('modal-mensaje-nuevo-est')) return;
  const docentes = [...new Set(DATA.cursos.map(c=>c.docente))];
  const companeros = [...new Set(DATA.cursos.flatMap(c=>(c.companeros||[]).map(nombre => nombre+'|'+c.nombre)))];
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-mensaje-nuevo-est"><div class="modal"><div class="modal-header"><h3>✉ Nuevo mensaje</h3><button class="modal-close" onclick="cerrarModal(\'modal-mensaje-nuevo-est\')">✕</button></div>' +
    '<div class="modal-body"><div class="campo"><label>Enviar a *</label><select id="msgn-est-tipo"><option value="">-- Selecciona --</option>' +
    '<optgroup label="Docentes">' + docentes.map(d=>'<option value="'+esc(d)+'">👩‍🏫 '+esc(d)+'</option>').join('') + '</optgroup>' +
    '<optgroup label="Compañeros de curso">' + companeros.map(c => { const [nombre,curso] = c.split('|'); return '<option value="'+esc(nombre)+'">🎓 '+esc(nombre)+' — '+esc(curso)+'</option>'; }).join('') + '</optgroup>' +
    '<option value="admin">🏫 Administración</option></select></div>' +
    '<div class="campo"><label>Asunto</label><input id="msgn-est-asunto"></div><div class="campo"><label>Mensaje</label><textarea id="msgn-est-cuerpo" rows="4"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-mensaje-nuevo-est\')">Cancelar</button><button class="btn btn-primary" onclick="enviarMensajeNuevoEst()">✉ Enviar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function enviarMensajeNuevoEst() {
  const para = document.getElementById('msgn-est-tipo').value, asunto = document.getElementById('msgn-est-asunto').value.trim(), cuerpo = document.getElementById('msgn-est-cuerpo').value.trim();
  if (!para) { toast('Selecciona un destinatario', true); return; }
  if (!asunto || !cuerpo) { toast('Escribe el asunto y el mensaje', true); return; }
  cerrarModal('modal-mensaje-nuevo-est');
  toast('Mensaje enviado a ' + (para==='admin'?'Administración':para));
}

/* ---------------------------- Mis pagos ---------------------------- */
function renderPagos(cont) {
  let html = '<div class="section-header"><div><h2>Mis pagos</h2><p class="subtitle">Historial de compras y facturas</p></div></div>';
  html += '<div class="tabla-wrap"><table><thead><tr><th>Curso</th><th>Fecha</th><th>Monto</th><th>Método</th><th>Estado</th><th>Factura</th></tr></thead><tbody>';
  DATA.pagos.forEach(p => {
    html += '<tr><td style="font-weight:600">' + esc(p.curso) + '</td><td class="text-sm muted">' + p.fecha + '</td><td>$' + p.monto.toLocaleString('es-CO') + '</td><td class="text-sm">' + esc(p.metodo) + '</td><td>' + pill(p.estado) + '</td>' +
      '<td><button class="btn btn-ghost btn-xs" onclick="abrirFacturaImprimibleEst(\''+p.id+'\')">⬇ ' + esc(p.factura) + '</button></td></tr>';
  });
  if (DATA.pagos.length === 0) html += '<tr><td colspan="6" class="empty-state">No tienes pagos registrados.</td></tr>';
  html += '</tbody></table></div>';
  if (DATA.reembolsos.length > 0) {
    html += '<h3 style="font-size:17px;margin:22px 0 12px">Solicitudes de reembolso</h3><div class="card">';
    DATA.reembolsos.forEach(r => html += '<div class="flex justify-between" style="padding:8px 0"><span class="text-sm">' + esc(r.curso) + '</span>' + pill(r.estado) + '</div>');
    html += '</div>';
  }
  cont.innerHTML = html;
}

/* ---------------------------- Mi perfil ---------------------------- */
function renderPerfil(cont) {
  let html = '<div class="section-header"><div><h2>Mi perfil</h2><p class="subtitle">Edita tu información y revisa tu historial de acceso</p></div></div>';
  html += '<div class="flex gap-14 flex-wrap">';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-size:17px;margin:0 0 16px">👤 Información personal</h3>' +
    '<div class="campo"><label>Nombre completo</label><input id="perfil-est-nombre" value="' + esc(ESTUDIANTE.nombre) + '"></div>' +
    '<div class="campo"><label>Correo electrónico</label><input id="perfil-est-email" value="' + esc(ESTUDIANTE.email) + '"></div>' +
    '<div class="campo"><label>Documento de identidad</label><input value="' + esc(ESTUDIANTE.documento) + '" disabled></div>' +
    '<button class="btn btn-primary" onclick="guardarPerfilEst()">Guardar cambios</button>' +
    '<h3 style="font-size:17px;margin:26px 0 16px">🔑 Seguridad</h3>' +
    '<div class="campo"><label>Nueva contraseña</label><input type="password" id="perfil-est-pass" placeholder="••••••••"></div>' +
    '<button class="btn btn-ghost" onclick="actualizarPasswordEst()">Actualizar contraseña</button></div>';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-size:17px;margin:0 0 16px">🛡️ Historial de acceso</h3>';
  DATA.historial.forEach(h => html += '<div class="flex justify-between text-sm" style="padding:8px 0;border-top:1px solid var(--line)"><span>' + esc(h.evento) + '</span><span class="muted">' + esc(h.fecha) + '</span></div>');
  html += '<div style="margin-top:20px;padding-top:16px;border-top:1px dashed var(--line)"><button class="btn btn-danger" onclick="confirmarAccion(\'¿Restablecer todos los datos de prueba? Esta acción no se puede deshacer.\', resetData, \'Restablecer\')">🔄 Restablecer datos del prototipo</button></div></div>';
  html += '</div>';
  cont.innerHTML = html;
}
function guardarPerfilEst() { toast('Perfil actualizado'); }
function actualizarPasswordEst() { document.getElementById('perfil-est-pass').value=''; toast('Contraseña actualizada'); }
