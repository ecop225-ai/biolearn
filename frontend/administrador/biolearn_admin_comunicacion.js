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
  let html = '<div class="section-header"><div><h2>Mi perfil</h2><p class="subtitle">Consulta tus datos y la seguridad de tu cuenta</p></div></div>';
  html += '<div class="flex gap-18 flex-wrap">';
  html += '<div class="card" style="flex:1;min-width:320px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 16px">👤 Información personal</h3>' +
    '<div class="campo"><label>Nombre completo</label><input id="perfil-adm-nombre" value="' + esc(ADMIN.nombre) + '" disabled></div>' +
    '<div class="campo"><label>Correo electrónico</label><input id="perfil-adm-email" value="' + esc(ADMIN.email) + '" disabled></div>' +
    '<p class="text-sm muted" style="margin:0">Para corregir tus datos, comunícate con el administrador de BioLearn.</p>' +
    '<h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:26px 0 16px">🔑 Seguridad</h3>' +
    '<p class="text-sm muted" style="margin:0 0 12px">Por seguridad, te enviamos a tu correo un enlace de un solo uso para definir la nueva contraseña.</p>' +
    '<button class="btn btn-ghost" onclick="solicitarCambioClave()">Enviar enlace para cambiar la contraseña</button></div>';
  html += '<div class="card" style="flex:1;min-width:320px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 16px">🛡️ Historial de acceso</h3>';
  html += '<p class="text-sm muted" style="margin:0">El historial de tus accesos estará disponible próximamente.</p>';
  html += '</div>';
  html += '</div>';

  // DEF-041: el bloque «Datos de facturación» del prototipo guardaba en el navegador datos distintos de los reales.
  // Los datos reales de las facturas se administran en Finanzas › Facturación y recaudo.
  html += '<div class="card" style="margin-top:18px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 6px">🧾 Datos de facturación</h3>' +
    '<p class="text-sm muted" style="margin:0 0 12px">Los datos del emisor que aparecen en las facturas y la cuenta para transferencias se administran en Finanzas.</p>' +
    '<button class="btn btn-ghost" onclick="goTo(\'finanzas\')">Ir a Finanzas › Facturación y recaudo</button></div>';
  cont.innerHTML = html;
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

/* DEF-042 · Mi perfil: los botones «Guardar cambios» y «Actualizar contraseña» del prototipo mostraban un mensaje de éxito sin guardar nada.
   Los datos personales quedan de solo lectura (los corrige el administrador) y el cambio de contraseña usa el enlace de un solo uso
   de la API (POST /api/auth/recuperar-password), el mismo de «Olvidé contraseña». */
async function solicitarCambioClave() {
  let correo = '';
  try { correo = (JSON.parse(sessionStorage.getItem('biolearn_usuario') || '{}').correo) || ''; } catch (e) { correo = ''; }
  if (!correo) { toast('No se encontró el correo de tu sesión; usa «Olvidé contraseña» en la pantalla de inicio.'); return; }
  const base = (typeof API_BASE !== 'undefined') ? API_BASE : (['localhost', '127.0.0.1', ''].includes(location.hostname) ? 'http://localhost:8080' : location.origin);
  try {
    const r = await fetch(base + '/api/auth/recuperar-password', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ correo: correo }) });
    if (!r.ok) throw new Error('HTTP ' + r.status);
    toast('Te enviamos a ' + correo + ' un enlace para cambiar la contraseña (vence en 30 minutos).');
  } catch (e) { toast('No se pudo solicitar el enlace. Intenta de nuevo en unos minutos.'); }
}
