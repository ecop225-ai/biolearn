/* ================================================================
   BIOLearn — biolearn_admin_usuarios.js
================================================================ */
let USR_FILTRO_ROL = 'todos';
let USR_FILTRO_INVITACION = 'todos';
let USR_BUSQUEDA = '';

function renderUsuarios(cont) {
  const filtrados = DATA.usuarios.filter(u => {
    const matchRol = USR_FILTRO_ROL === 'todos' || u.rol === USR_FILTRO_ROL;
    const q = USR_BUSQUEDA.toLowerCase();
    const matchBusqueda = u.nombre.toLowerCase().includes(q) || u.email.toLowerCase().includes(q);
    const matchInvitacion = USR_FILTRO_INVITACION === 'todos' || (u.invitacion && u.invitacion.estado === USR_FILTRO_INVITACION);
    return matchRol && matchBusqueda && matchInvitacion;
  });

  let html = '<div class="section-header"><div><h2>Gestión de usuarios</h2><p class="subtitle">Crea, edita y administra las cuentas de docentes, contadores, supervisores y estudiantes</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalUsuario()">+ Crear usuario</button></div>';
  html += '<div class="flex gap-10 flex-wrap mb-10">' +
    '<input id="usr-buscar" placeholder="Buscar por nombre o correo..." value="'+esc(USR_BUSQUEDA)+'" oninput="USR_BUSQUEDA=this.value;renderUsuarios(document.getElementById(\'app-content\'))" style="width:250px">' +
    '<select onchange="USR_FILTRO_ROL=this.value;render()" style="width:230px">' +
    '<option value="todos"'+(USR_FILTRO_ROL==='todos'?' selected':'')+'>Todos los roles</option>' +
    '<option value="docente"'+(USR_FILTRO_ROL==='docente'?' selected':'')+'>Docente</option>' +
    '<option value="contador"'+(USR_FILTRO_ROL==='contador'?' selected':'')+'>Contador</option>' +
    '<option value="supervisor"'+(USR_FILTRO_ROL==='supervisor'?' selected':'')+'>Supervisor</option>' +
    '<option value="estudiante"'+(USR_FILTRO_ROL==='estudiante'?' selected':'')+'>Estudiante</option></select></div>' +
    '<div class="flex gap-8 mb-14">';
  [['todos','Todas'],['pendiente','Invitación pendiente'],['aceptada','Invitación aceptada']].forEach(([key,label]) => {
    const activo = USR_FILTRO_INVITACION === key;
    html += '<button onclick="USR_FILTRO_INVITACION=\''+key+'\';render()" style="padding:7px 14px;border-radius:20px;font-size:13px;font-weight:600;cursor:pointer;border:1px solid '+(activo?'var(--blue)':'var(--line)')+';background:'+(activo?'var(--blue-soft)':'transparent')+';color:'+(activo?'var(--blue)':'var(--ink-soft)')+'">' + label + '</button>';
  });
  html += '</div>';

  html += '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
    ['Nombre','Correo','Rol','Institución','Estado','Invitación','Registrado',''].map(h=>'<th style="color:var(--blue)">'+h+'</th>').join('') + '</tr></thead><tbody>';
  filtrados.forEach(u => {
    const inst = DATA.instituciones.find(i=>i.id===u.institucionId);
    html += '<tr><td style="font-weight:600">' + esc(u.nombre) + '</td><td class="text-sm muted">' + esc(u.email) + '</td>' +
      '<td>' + rolePill(u.rol) + '</td><td class="text-sm muted">' + (inst?esc(inst.nombre):'—') + '</td>' +
      '<td><button onclick="toggleEstadoUsuario(\''+u.id+'\')" style="border:none;background:none;cursor:pointer;padding:0">' + pill(u.estado) + '</button></td>' +
      '<td>' + renderInvitacionCelda(u) + '</td>' +
      '<td class="text-sm muted">' + u.registrado + '</td>' +
      '<td><div class="flex gap-8" style="justify-content:flex-end"><button class="btn btn-ghost btn-xs" onclick="abrirModalUsuario(\''+u.id+'\')">✏️</button><button class="btn btn-ghost btn-xs" onclick="confirmarEliminarUsuario(\''+u.id+'\')" style="color:var(--clay)">🗑</button></div></td></tr>';
  });
  if (filtrados.length === 0) html += '<tr><td colspan="8" class="empty-state">No hay usuarios que coincidan con la búsqueda.</td></tr>';
  html += '</tbody></table></div>';
  cont.innerHTML = html;
  renderModalUsuario();
}
function renderInvitacionCelda(u) {
  if (!u.invitacion) return '<span class="text-sm muted">No aplica</span>';
  if (u.invitacion.estado === 'aceptada') return '<span class="pill pill-verde">✓ Aceptada</span>';
  return '<div class="flex items-center gap-6">' + pill('Pendiente') +
    '<button class="btn btn-ghost btn-xs" title="Reenviar invitación" onclick="reenviarInvitacion(\''+u.id+'\')">🔄</button>' +
    '<button class="btn btn-ghost btn-xs" title="Copiar enlace de invitación" onclick="copiarEnlaceInvitacion(\''+u.id+'\')">🔗</button>' +
    '<button class="btn btn-ghost btn-xs" title="Marcar como aceptada (simular)" onclick="marcarInvitacionAceptada(\''+u.id+'\')">✓</button></div>';
}
function reenviarInvitacion(id) {
  const u = DATA.usuarios.find(x=>x.id===id);
  u.invitacion.enviada = new Date().toISOString().slice(0,10);
  saveData(); render(); toast('Invitación reenviada a ' + u.email);
}
function copiarEnlaceInvitacion(id) {
  const u = DATA.usuarios.find(x=>x.id===id);
  toast('Enlace copiado: app.biolearn.co/invitacion/' + u.invitacion.token);
}
function marcarInvitacionAceptada(id) {
  const u = DATA.usuarios.find(x=>x.id===id);
  u.invitacion.estado = 'aceptada';
  saveData(); render(); toast(u.nombre + ' aceptó la invitación (simulado)');
}
function toggleEstadoUsuario(id) {
  const u = DATA.usuarios.find(x=>x.id===id);
  u.estado = u.estado==='Activo'?'Inactivo':'Activo';
  saveData(); render(); toast('Estado de ' + u.nombre + ' actualizado');
}
function confirmarEliminarUsuario(id) {
  const u = DATA.usuarios.find(x=>x.id===id);
  confirmarAccion('¿Eliminar a <strong>' + esc(u.nombre) + '</strong>? Esta acción no se puede deshacer.', function () {
    DATA.usuarios = DATA.usuarios.filter(x=>x.id!==id);
    saveData(); render(); toast('Usuario ' + u.nombre + ' eliminado');
  }, 'Eliminar');
}
function abrirModalUsuario(id) {
  const u = id ? DATA.usuarios.find(x=>x.id===id) : null;
  document.getElementById('usr-modal-titulo').textContent = u ? 'Editar usuario' : 'Crear usuario';
  document.getElementById('usr-modal-btn').textContent = u ? 'Guardar cambios' : 'Crear y notificar';
  document.getElementById('usr-id').value = u ? u.id : '';
  document.getElementById('usr-nombre').value = u ? u.nombre : '';
  document.getElementById('usr-email').value = u ? u.email : '';
  document.getElementById('usr-rol').value = u ? u.rol : 'estudiante';
  actualizarCampoInstitucionUsuario();
  document.getElementById('usr-institucion').value = u ? (u.institucionId||'') : '';
  abrirModal('modal-usuario');
}
function actualizarCampoInstitucionUsuario() {
  const rol = document.getElementById('usr-rol').value;
  document.getElementById('usr-campo-institucion').style.display = (rol==='supervisor'||rol==='estudiante') ? 'block' : 'none';
}
function renderModalUsuario() {
  if (document.getElementById('modal-usuario')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-usuario"><div class="modal"><div class="modal-header"><h3 id="usr-modal-titulo">Crear usuario</h3><button class="modal-close" onclick="cerrarModal(\'modal-usuario\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="usr-id">' +
    '<div class="campo"><label>Nombre completo</label><input id="usr-nombre"></div>' +
    '<div class="campo"><label>Correo electrónico</label><input id="usr-email"></div>' +
    '<div class="campo"><label>Rol</label><select id="usr-rol" onchange="actualizarCampoInstitucionUsuario()">' +
    '<option value="docente">Docente</option><option value="contador">Contador</option><option value="supervisor">Supervisor</option><option value="estudiante">Estudiante</option></select></div>' +
    '<div id="usr-campo-institucion" class="campo" style="display:none"><label>Institución</label><select id="usr-institucion"><option value="">Sin institución</option>' + DATA.instituciones.map(i=>'<option value="'+i.id+'">'+esc(i.nombre)+'</option>').join('') + '</select></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-usuario\')">Cancelar</button><button class="btn btn-primary" id="usr-modal-btn" onclick="guardarUsuario()">Crear y notificar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarUsuario() {
  const id = document.getElementById('usr-id').value;
  const nombre = document.getElementById('usr-nombre').value.trim(), email = document.getElementById('usr-email').value.trim();
  if (!nombre || !email) { toast('Escribe el nombre y el correo', true); return; }
  const campos = { nombre, email, rol: document.getElementById('usr-rol').value, institucionId: document.getElementById('usr-institucion').value || null };
  if (id) {
    Object.assign(DATA.usuarios.find(x=>x.id===id), campos);
    toast('Usuario ' + nombre + ' actualizado');
  } else {
    DATA.usuarios.unshift({ id: uid('usr'), ...campos, estado:'Activo', registrado: new Date().toISOString().slice(0,10),
      invitacion: { estado:'pendiente', enviada: new Date().toISOString().slice(0,10), token: uid('inv') } });
    toast('Usuario ' + nombre + ' creado y notificado por correo');
  }
  saveData(); cerrarModal('modal-usuario'); render();
}
