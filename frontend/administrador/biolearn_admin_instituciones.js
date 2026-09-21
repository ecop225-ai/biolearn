/* ================================================================
   BIOLearn — biolearn_admin_instituciones.js
================================================================ */
function renderInstituciones(cont) {
  let html = '<div class="section-header"><div><h2>Instituciones</h2><p class="subtitle">Administra las organizaciones que contratan cupos grupales y sus representantes</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalInstitucion()">+ Registrar institución</button></div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:16px">';
  DATA.instituciones.forEach(inst => {
    const sup = DATA.usuarios.find(u=>u.id===inst.supervisorId);
    const pct = Math.round((inst.usados/inst.cupos)*100);
    html += '<div class="card"><div class="flex justify-between items-start"><h3 style="font-size:17px;margin:0 0 4px">' + esc(inst.nombre) + '</h3>' + pill(inst.estado) + '</div>' +
      '<p class="text-sm muted mb-8">NIT ' + esc(inst.nit||'—') + '</p>' +
      '<div class="flex items-center gap-8 mb-10" style="background:var(--blue-soft);border-radius:8px;padding:8px 12px">' +
      '<span class="text-sm" style="font-weight:700;color:var(--blue-deep)">🔑 Código de afiliación:</span>' +
      '<span style="font-family:monospace;font-weight:700;font-size:13px">' + esc(inst.codigo||'—') + '</span>' +
      '<button onclick="copiarCodigoInstitucion(\''+esc(inst.codigo||'')+'\')" style="border:none;background:none;cursor:pointer;color:var(--blue);font-size:12px;margin-left:auto">📋 Copiar</button></div>' +
      '<div class="text-sm mb-14" style="line-height:1.8">' +
      '<div>👤 <strong>Rector:</strong> ' + esc(inst.rector||inst.contacto||'—') + '</div>' +
      '<div>✉️ ' + esc(inst.email||'—') + '</div>' +
      '<div>📞 ' + esc(inst.telefono||'—') + '</div>' +
      (inst.cursosContratados && inst.cursosContratados.length ? '<div>📚 <strong>Cursos:</strong> ' + esc(inst.cursosContratados.join(', ')) + '</div>' : '') + '</div>' +
      '<div class="mb-14"><div class="flex justify-between text-sm muted mb-4"><span>Cupos usados</span><span>' + inst.usados + '/' + inst.cupos + '</span></div>' +
      '<div class="progress-bar"><div class="progress-fill" style="width:'+Math.min(pct,100)+'%;background:'+(pct>=95?'var(--clay)':'var(--blue)')+'"></div></div></div>' +
      '<p class="text-sm" style="margin-bottom:14px"><strong>Supervisor:</strong> ' + (sup?esc(sup.nombre):'Sin asignar') + '</p>' +
      '<div class="flex gap-8 flex-wrap">' + (inst.estado==='Pendiente' ? '<button class="btn btn-amber btn-sm" onclick="aprobarInstitucion(\''+inst.id+'\')">Aprobar</button>' : '') +
      '<button class="btn btn-ghost btn-sm" onclick="abrirModalInstitucion(\''+inst.id+'\')">✏️ Editar</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="abrirModalAsignarSupervisor(\''+inst.id+'\')">Asignar supervisor</button></div></div>';
  });
  html += '</div>';
  cont.innerHTML = html;
  renderModalInstitucion(); renderModalAsignarSupervisor();
}
function aprobarInstitucion(id) {
  const inst = DATA.instituciones.find(x=>x.id===id);
  inst.estado = 'Activo'; saveData(); render(); toast('Institución ' + inst.nombre + ' aprobada');
}
function copiarCodigoInstitucion(codigo) { toast('Código ' + codigo + ' copiado al portapapeles'); }
function generarCodigoSugerido(nombre) {
  const base = nombre.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-zA-Z0-9 ]/g,'').split(' ').filter(w=>w.length>3 && !['colegio','instituto','institucion','corporacion','de','del','la','las','los'].includes(w.toLowerCase()))[0] || nombre.replace(/[^a-zA-Z0-9]/g,'');
  return base.toUpperCase() + new Date().getFullYear();
}
function regenerarCodigoInstitucion() {
  const nombre = document.getElementById('inst-nombre').value.trim();
  if (!nombre) { toast('Escribe primero el nombre de la institución', true); return; }
  document.getElementById('inst-codigo').value = generarCodigoSugerido(nombre);
}
function abrirModalInstitucion(id) {
  const inst = id ? DATA.instituciones.find(x=>x.id===id) : null;
  document.getElementById('inst-modal-titulo').textContent = inst ? 'Editar institución' : 'Registrar institución';
  document.getElementById('inst-modal-btn').textContent = inst ? 'Guardar cambios' : 'Registrar';
  document.getElementById('inst-id').value = inst ? inst.id : '';
  document.getElementById('inst-nombre').value = inst ? inst.nombre : '';
  document.getElementById('inst-nit').value = inst ? (inst.nit||'') : '';
  document.getElementById('inst-rector').value = inst ? (inst.rector||inst.contacto||'') : '';
  document.getElementById('inst-email').value = inst ? (inst.email||'') : '';
  document.getElementById('inst-telefono').value = inst ? (inst.telefono||'') : '';
  document.getElementById('inst-codigo').value = inst ? inst.codigo : '';
  document.getElementById('inst-cupos').value = inst ? inst.cupos : 20;
  document.getElementById('inst-cursos').value = inst && inst.cursosContratados ? inst.cursosContratados.join(', ') : '';
  abrirModal('modal-institucion');
}
function renderModalInstitucion() {
  if (document.getElementById('modal-institucion')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-institucion"><div class="modal" style="max-width:520px"><div class="modal-header"><h3 id="inst-modal-titulo">Registrar institución</h3><button class="modal-close" onclick="cerrarModal(\'modal-institucion\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="inst-id">' +
    '<div class="campo"><label>Nombre de la institución</label><input type="text" id="inst-nombre"></div>' +
    '<div class="campo"><label>Código de afiliación</label><div class="flex gap-8"><input type="text" id="inst-codigo" style="flex:1;font-family:monospace" placeholder="Ej: SANRAFAEL2026"><button type="button" class="btn btn-ghost btn-sm" onclick="regenerarCodigoInstitucion()">🔄 Generar</button></div><p class="hint">Este código lo usan los estudiantes de esta institución al registrarse, para afiliar su cuenta automáticamente.</p></div>' +
    '<div class="grid-2"><div class="campo"><label>NIT</label><input type="text" id="inst-nit"></div>' +
    '<div class="campo"><label>Cupos contratados</label><input type="number" id="inst-cupos"></div></div>' +
    '<div class="grid-2"><div class="campo"><label>Nombre del rector</label><input type="text" id="inst-rector"></div>' +
    '<div class="campo"><label>Teléfono</label><input type="text" id="inst-telefono"></div></div>' +
    '<div class="campo"><label>Correo electrónico</label><input type="email" id="inst-email"></div>' +
    '<div class="campo"><label>Cursos contratados (separados por coma)</label><input type="text" id="inst-cursos" placeholder="Ej: Fundamentos de Genética, Ecosistemas Tropicales"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-institucion\')">Cancelar</button><button class="btn btn-primary" id="inst-modal-btn" onclick="guardarInstitucion()">Registrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarInstitucion() {
  const id = document.getElementById('inst-id').value;
  const nombre = document.getElementById('inst-nombre').value.trim();
  if (!nombre) { toast('Escribe el nombre de la institución', true); return; }
  let codigo = document.getElementById('inst-codigo').value.trim().toUpperCase();
  if (!codigo) codigo = generarCodigoSugerido(nombre);
  const enUso = DATA.instituciones.find(i => i.codigo === codigo && i.id !== id);
  if (enUso) { toast('Ese código ya lo usa "' + enUso.nombre + '" — elige otro', true); return; }
  const campos = {
    nombre, codigo, nit: document.getElementById('inst-nit').value.trim(), rector: document.getElementById('inst-rector').value.trim(),
    contacto: document.getElementById('inst-rector').value.trim(), email: document.getElementById('inst-email').value.trim(),
    telefono: document.getElementById('inst-telefono').value.trim(), cupos: Number(document.getElementById('inst-cupos').value)||0,
    cursosContratados: document.getElementById('inst-cursos').value.split(',').map(c=>c.trim()).filter(Boolean),
  };
  if (id) {
    Object.assign(DATA.instituciones.find(x=>x.id===id), campos);
    toast('Institución ' + nombre + ' actualizada');
  } else {
    DATA.instituciones.unshift({ id:uid('inst'), ...campos, usados:0, estado:'Pendiente', supervisorId:null });
    toast('Institución ' + nombre + ' registrada, pendiente de aprobación');
  }
  saveData(); cerrarModal('modal-institucion'); render();
}
function abrirModalAsignarSupervisor(instId) {
  const inst = DATA.instituciones.find(x=>x.id===instId);
  document.getElementById('asig-inst-id').value = instId;
  document.getElementById('asig-inst-titulo').textContent = 'Asignar supervisor · ' + inst.nombre;
  const supervisores = DATA.usuarios.filter(u=>u.rol==='supervisor');
  document.getElementById('asig-select').innerHTML = '<option value="">Sin asignar</option>' + supervisores.map(s=>'<option value="'+s.id+'"'+(inst.supervisorId===s.id?' selected':'')+'>'+esc(s.nombre)+'</option>').join('');
  abrirModal('modal-asignar-supervisor');
}
function renderModalAsignarSupervisor() {
  if (document.getElementById('modal-asignar-supervisor')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-asignar-supervisor"><div class="modal" style="max-width:420px"><div class="modal-header"><h3 id="asig-inst-titulo">Asignar supervisor</h3><button class="modal-close" onclick="cerrarModal(\'modal-asignar-supervisor\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="asig-inst-id"><div class="campo"><label>Supervisor</label><select id="asig-select" onchange="guardarSupervisorAsignado()"></select></div>' +
    '<p class="text-sm muted">El supervisor (cuenta con rol "supervisor") queda asociado a esta institución: podrá ver los estudiantes, certificados y reportes de aquí en adelante.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-asignar-supervisor\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarSupervisorAsignado() {
  const instId = document.getElementById('asig-inst-id').value, supId = document.getElementById('asig-select').value;
  const inst = DATA.instituciones.find(x=>x.id===instId);
  inst.supervisorId = supId || null;
  saveData(); render(); toast('Supervisor asignado');
}
