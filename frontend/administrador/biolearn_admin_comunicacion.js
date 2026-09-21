/* ================================================================
   BIOLearn — biolearn_admin_comunicacion.js
   Mensajería, Mi perfil
================================================================ */
let MSG_ACTIVO_ADM = null;

function renderMensajeria(cont) {
  if (MSG_ACTIVO_ADM === null && DATA.mensajes.length>0) MSG_ACTIVO_ADM = DATA.mensajes[0].id;
  const msg = DATA.mensajes.find(m=>m.id===MSG_ACTIVO_ADM);
  let html = '<div class="section-header"><div><h2>Mensajería</h2><p class="subtitle">Comunícate individualmente o en grupo con docentes, contadores, supervisores y estudiantes</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalMensajeAdmin()">+ Nuevo mensaje</button></div>';
  html += '<div class="flex gap-14" style="min-height:420px;align-items:flex-start">';
  html += '<div class="card" style="width:300px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.mensajes.forEach(m => {
    html += '<div onclick="seleccionarMensajeAdmin(\''+m.id+'\')" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:'+(m.id===MSG_ACTIVO_ADM?'var(--blue-soft)':(m.leido?'transparent':'var(--bone)'))+'">' +
      '<div class="flex justify-between mb-4"><span style="font-size:13.5px;font-weight:'+(m.leido?'500':'700')+'">' + esc(m.de) + '</span><span class="text-sm muted" style="font-size:11px">' + m.fecha + '</span></div>' +
      '<div style="font-size:12.5px;font-weight:'+(m.leido?'400':'600')+'">' + esc(m.asunto) + '</div>' +
      '<div class="text-sm muted" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + esc(m.cuerpo) + '</div></div>';
  });
  if (DATA.mensajes.length === 0) html += '<div class="empty-state">Sin mensajes</div>';
  html += '</div><div class="card" style="flex:1;min-width:0;display:flex;flex-direction:column">';
  if (msg) {
    html += '<div style="border-bottom:1px solid var(--line);padding-bottom:14px;margin-bottom:14px">' +
      '<h3 style="font-family:\'Fraunces\',serif;font-size:18px;margin:0 0 6px">' + esc(msg.asunto) + '</h3>' +
      '<div class="flex items-center gap-10">' + rolePill(msg.rol) + '<span class="text-sm">' + esc(msg.de) + '</span><span class="text-sm muted">· ' + msg.fecha + '</span></div></div>' +
      '<p style="font-size:14.5px;line-height:1.7;flex:1">' + esc(msg.cuerpo) + '</p>';
    (msg.respuestas||[]).forEach(r => {
      html += '<div style="background:var(--blue-soft);border-radius:10px;padding:12px 16px;margin-bottom:10px"><div class="flex justify-between"><span style="font-size:12.5px;font-weight:700;color:var(--blue-deep)">' + esc(r.de) + '</span><span class="text-sm muted" style="font-size:11px">' + esc(r.fecha) + '</span></div><p style="font-size:13.5px;margin:4px 0 0">' + esc(r.cuerpo) + '</p></div>';
    });
    html += '<div class="flex gap-10" style="border-top:1px solid var(--line);padding-top:16px"><input id="msg-reply-adm" placeholder="Escribe una respuesta..." style="flex:1"><button class="btn btn-primary" onclick="responderMensajeAdmin(\''+msg.id+'\')">✉ Enviar</button></div>';
  } else html += '<div class="empty-state">Selecciona un mensaje para leerlo</div>';
  html += '</div></div>';
  cont.innerHTML = html;
  renderModalMensajeAdmin();
}
function seleccionarMensajeAdmin(id) { MSG_ACTIVO_ADM = id; const m = DATA.mensajes.find(x=>x.id===id); m.leido = true; saveData(); render(); }
function responderMensajeAdmin(id) {
  const input = document.getElementById('msg-reply-adm');
  const texto = input.value.trim();
  if (!texto) { toast('Escribe una respuesta antes de enviar', true); return; }
  const m = DATA.mensajes.find(x=>x.id===id);
  if (!m.respuestas) m.respuestas = [];
  m.respuestas.push({ de: ADMIN.nombre, cuerpo: texto, fecha: new Date().toISOString().slice(0,10) });
  saveData(); render(); toast('Respuesta enviada');
}
function abrirModalMensajeAdmin() {
  document.getElementById('msgn-adm-tipo').value = 'individual'; ssReset('msgn-adm-para');
  document.getElementById('msgn-adm-curso').value=''; document.getElementById('msgn-adm-grupo').value='';
  document.getElementById('msgn-adm-asunto').value=''; document.getElementById('msgn-adm-cuerpo').value='';
  ssSetOptions('msgn-adm-para', DATA.usuarios.map(u=>({ value:u.nombre, label:u.nombre+' · '+ROLE_LABEL[u.rol] })));
  actualizarTipoEnvioAdmin();
  abrirModal('modal-mensaje-admin');
}
function actualizarTipoEnvioAdmin() {
  const tipo = document.getElementById('msgn-adm-tipo').value;
  document.getElementById('msgn-adm-wrap-individual').style.display = tipo==='individual' ? 'block' : 'none';
  document.getElementById('msgn-adm-wrap-grupo').style.display = tipo==='grupo' ? 'block' : 'none';
  document.getElementById('msgn-adm-wrap-curso').style.display = tipo==='curso' ? 'block' : 'none';
}
function renderModalMensajeAdmin() {
  if (document.getElementById('modal-mensaje-admin')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-mensaje-admin"><div class="modal"><div class="modal-header"><h3>Nuevo mensaje</h3><button class="modal-close" onclick="cerrarModal(\'modal-mensaje-admin\')">✕</button></div>' +
    '<div class="modal-body"><div class="campo"><label>Tipo de envío</label><select id="msgn-adm-tipo" onchange="actualizarTipoEnvioAdmin()">' +
    '<option value="individual">Individual</option><option value="grupo">Grupal (por rol)</option><option value="curso">Estudiantes de un curso específico</option></select></div>' +
    '<div id="msgn-adm-wrap-individual" class="campo">' + '<label>Destinatario</label>' + searchableSelectHTML('msgn-adm-para','Busca por nombre...') + '</div>' +
    '<div id="msgn-adm-wrap-grupo" class="campo" style="display:none"><label>Grupo destinatario</label><select id="msgn-adm-grupo"><option value="">Selecciona un rol</option>' +
    '<option value="Docentes">Todos los docentes</option><option value="Contadores">Todos los contadores</option><option value="Representantes">Todos los supervisores</option><option value="Estudiantes">Todos los estudiantes</option></select></div>' +
    '<div id="msgn-adm-wrap-curso" class="campo" style="display:none"><label>Curso</label><select id="msgn-adm-curso"><option value="">Selecciona un curso</option>' + DATA.cursos.map(c=>'<option value="'+c.id+'">'+esc(c.nombre)+' · '+c.inscritos+' inscritos</option>').join('') + '</select></div>' +
    '<div class="campo"><label>Asunto</label><input id="msgn-adm-asunto"></div><div class="campo"><label>Mensaje</label><textarea id="msgn-adm-cuerpo" rows="4"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-mensaje-admin\')">Cancelar</button><button class="btn btn-primary" onclick="enviarMensajeAdmin()">✉ Enviar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function enviarMensajeAdmin() {
  const tipo = document.getElementById('msgn-adm-tipo').value;
  const asunto = document.getElementById('msgn-adm-asunto').value.trim(), cuerpo = document.getElementById('msgn-adm-cuerpo').value.trim();
  if (!asunto || !cuerpo) { toast('Escribe el asunto y el mensaje', true); return; }
  cerrarModal('modal-mensaje-admin');
  if (tipo === 'curso') {
    const cursoId = document.getElementById('msgn-adm-curso').value;
    const curso = DATA.cursos.find(c=>c.id===cursoId);
    toast(curso ? 'Mensaje enviado a los ' + curso.inscritos + ' estudiantes de "' + curso.nombre + '"' : 'Mensaje enviado al curso');
  } else if (tipo === 'grupo') {
    toast('Mensaje enviado a todos los destinatarios del grupo');
  } else {
    const para = document.getElementById('msgn-adm-para').value;
    toast('Mensaje enviado a ' + (para || 'destinatario'));
  }
}

/* ---------------------------- Mi perfil ---------------------------- */
function renderPerfil(cont) {
  let html = '<div class="section-header"><div><h2>Mi perfil</h2><p class="subtitle">Edita tu información y revisa tu historial de acceso</p></div></div>';
  html += '<div class="flex gap-18 flex-wrap">';
  html += '<div class="card" style="flex:1;min-width:320px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 16px">👤 Información personal</h3>' +
    '<div class="campo"><label>Nombre completo</label><input id="perfil-adm-nombre" value="' + esc(ADMIN.nombre) + '"></div>' +
    '<div class="campo"><label>Correo electrónico</label><input id="perfil-adm-email" value="' + esc(ADMIN.email) + '"></div>' +
    '<button class="btn btn-primary" onclick="guardarPerfilAdmin()">Guardar cambios</button>' +
    '<h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:26px 0 16px">🔑 Seguridad</h3>' +
    '<div class="campo"><label>Nueva contraseña</label><input type="password" id="perfil-adm-pass" placeholder="••••••••"></div>' +
    '<button class="btn btn-ghost" onclick="actualizarPasswordAdmin()">Actualizar contraseña</button></div>';
  html += '<div class="card" style="flex:1;min-width:320px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 16px">🛡️ Historial de acceso</h3>';
  DATA.historial.forEach(h => html += '<div class="flex justify-between text-sm" style="padding:10px 0;border-top:1px solid var(--line)"><span>' + esc(h.evento) + '</span><span class="muted">' + esc(h.fecha) + '</span></div>');
  html += '<div style="margin-top:24px;padding-top:18px;border-top:1px dashed var(--line)"><button class="btn btn-danger" onclick="confirmarAccion(\'¿Restablecer todos los datos de prueba? Esta acción no se puede deshacer.\', resetData, \'Restablecer\')">🔄 Restablecer datos del prototipo</button></div></div>';
  html += '</div>';

  const df = DATA.datosFacturacion;
  html += '<div class="card" style="margin-top:18px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 4px">🧾 Datos de facturación</h3>' +
    '<p class="text-sm muted" style="margin:0 0 18px">Estos datos aparecen en el encabezado de todas las facturas que emite BioLearn — tanto las que genera el contador como las que descargan los estudiantes.</p>' +
    '<div class="flex gap-24 flex-wrap">' +
    '<div style="flex:1;min-width:280px">' +
    '<div class="campo"><label>Razón social</label><input id="df-razon" value="' + esc(df.razonSocial) + '" oninput="actualizarPreviewFacturacion()"></div>' +
    '<div class="campo"><label>NIT</label><input id="df-nit" value="' + esc(df.nit) + '" oninput="actualizarPreviewFacturacion()"></div>' +
    '<div class="campo"><label>Dirección</label><input id="df-direccion" value="' + esc(df.direccion) + '"></div>' +
    '<div class="campo"><label>Logotipo</label><div class="flex items-center gap-10"><img id="df-logo-preview" src="' + df.logo + '" style="height:32px;border:1px solid var(--line);border-radius:6px;padding:4px;background:#fff">' +
    '<button type="button" class="btn btn-ghost btn-sm" onclick="subirLogoFacturacion()">⬆ Subir</button>' +
    '<button type="button" class="btn btn-ghost btn-sm" onclick="restablecerLogoFacturacion()">↺ Restablecer</button></div></div>' +
    '<button class="btn btn-primary" onclick="guardarDatosFacturacion()">Guardar datos de facturación</button></div>' +
    '<div style="flex:1;min-width:280px"><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:10px">Vista previa</div><div id="df-preview"></div></div>' +
    '</div></div>';
  cont.innerHTML = html;
  actualizarPreviewFacturacion();
}
function actualizarPreviewFacturacion() {
  const razon = document.getElementById('df-razon').value, nit = document.getElementById('df-nit').value;
  const logo = document.getElementById('df-logo-preview').src;
  document.getElementById('df-preview').innerHTML = facturaHTML({ numero:'BIO-FAC-2026-0001', estudiante:'Nombre del estudiante', curso:'Nombre del curso', monto:180000, pagado:180000, fecha:new Date().toISOString().slice(0,10), estadoPago:'Pagada' }, { razonSocial:razon, nit, logo });
}
function subirLogoFacturacion() {
  seleccionarArchivos('image/*', function (archivos) {
    if (archivos.length === 0) return;
    document.getElementById('df-logo-preview').src = archivos[0].dataUrl;
    actualizarPreviewFacturacion();
    toast('Logo actualizado en la vista previa — recuerda guardar');
  });
}
function restablecerLogoFacturacion() {
  document.getElementById('df-logo-preview').src = LOGO_BIOLEARN_FULL;
  actualizarPreviewFacturacion();
}
function guardarDatosFacturacion() {
  DATA.datosFacturacion = {
    razonSocial: document.getElementById('df-razon').value.trim(),
    nit: document.getElementById('df-nit').value.trim(),
    direccion: document.getElementById('df-direccion').value.trim(),
    logo: document.getElementById('df-logo-preview').src,
  };
  saveData(); toast('Datos de facturación actualizados');
}
function guardarPerfilAdmin() { ADMIN.nombre = document.getElementById('perfil-adm-nombre').value.trim() || ADMIN.nombre; render(); toast('Perfil actualizado'); }
function actualizarPasswordAdmin() { document.getElementById('perfil-adm-pass').value=''; toast('Contraseña actualizada'); }
