/* ================================================================
   BIOLearn — biolearn_docente_cursos.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: CURSOS
================================================================ */
function renderCursos(cont) {
  let html = '<div class="section-header"><div><h2>Mis cursos</h2><p class="subtitle">Crea y administra tus cursos propios.</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalCurso()">+ Nuevo curso</button></div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px;align-items:stretch">';
  DATA.cursos.forEach(c => {
    const numLecciones = leccionesDeCurso(c.id).length;
    const color = c.color || '#33526E';
    const banner = c.imagenPortada
      ? '<div style="height:100px;border-radius:12px 12px 0 0;margin:-20px -20px 14px;overflow:hidden"><img src="'+esc(c.imagenPortada)+'" style="width:100%;height:100%;object-fit:cover" alt=""></div>'
      : '<div style="height:8px;background:'+color+';border-radius:12px 12px 0 0;margin:-20px -20px 14px"></div>';
    html += '<div class="card" style="display:flex;flex-direction:column">' + banner +
      '<div class="flex justify-between mb-8"><h3 style="font-size:17px;margin:0"><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:'+color+';margin-right:6px"></span>' + esc(c.nombre) + '</h3>' + pill(c.estado) + '</div>' +
      (c.codigo ? '<div class="text-sm mb-4" style="font-family:monospace;font-weight:700;color:'+color+'">' + esc(c.codigo) + '</div>' : '') +
      '<p class="text-sm muted mb-0">' + esc(c.area) + ' · Grado ' + esc(c.grado) + '</p>' +
      '<p class="text-sm muted mb-14">' + c.creditos + ' créditos · ' + c.horas + ' horas</p>' +
      '<div class="flex justify-between text-sm mb-14"><span>' + c.inscritos + ' inscritos</span><span>' + numLecciones + ' lecciones</span></div>' +
      '<div class="flex gap-8 flex-wrap" style="margin-top:auto">' +
      '<button class="btn btn-ghost btn-sm" onclick="setCtx(\'lec\',\'curso\',\''+c.id+'\');goTo(\'lecciones\')">Ver lecciones</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="abrirModalCursoPreview(\''+c.id+'\')">👁 Ver como estudiante</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="abrirModalCurso(\''+c.id+'\')">✏️ Editar</button>';
    if (c.estado === 'borrador' && numLecciones > 0) html += '<button class="btn btn-amber btn-sm" onclick="cambiarEstadoCurso(\''+c.id+'\',\'activo\')">Publicar</button>';
    if (c.estado === 'activo') html += '<button class="btn btn-ghost btn-sm" onclick="cambiarEstadoCurso(\''+c.id+'\',\'archivado\')">Archivar</button>';
    if (c.estado === 'archivado') html += '<button class="btn btn-ghost btn-sm" onclick="cambiarEstadoCurso(\''+c.id+'\',\'activo\')">Reactivar</button>';
    html += '<button class="btn btn-danger btn-sm" onclick="confirmarEliminarCurso(\''+c.id+'\')">🗑 Eliminar</button>' +
      '</div></div>';
  });
  html += '</div>';
  cont.innerHTML = html;
  renderModalCurso();
  renderModalCursoPreview();
}
function cambiarEstadoCurso(id, estado) {
  const c = DATA.cursos.find(x=>x.id===id);
  c.estado = estado; saveData(); render();
  toast(estado === 'activo' ? '"' + c.nombre + '" publicado' : '"' + c.nombre + '" ahora está ' + estado);
}
function confirmarEliminarCurso(id) {
  const c = DATA.cursos.find(x=>x.id===id);
  confirmarAccion('¿Eliminar <strong>' + esc(c.nombre) + '</strong>? Se perderán sus lecciones y contenidos asociados.', function () {
    DATA.cursos = DATA.cursos.filter(x=>x.id!==id);
    saveData(); render(); toast('Curso "' + c.nombre + '" eliminado');
  });
}
function abrirModalCurso(id) {
  const c = id ? DATA.cursos.find(x=>x.id===id) : null;
  document.getElementById('curso-modal-titulo').textContent = c ? 'Editar curso' : 'Crear curso';
  document.getElementById('curso-modal-btn').textContent = c ? 'Guardar cambios' : 'Crear curso';
  document.getElementById('curso-id').value = c ? c.id : '';
  document.getElementById('curso-nombre').value = c ? c.nombre : '';
  document.getElementById('curso-descripcion').value = c ? c.descripcion : '';
  document.getElementById('curso-objetivos').value = c ? c.objetivos : '';
  document.getElementById('curso-color').value = c && c.color ? c.color : '#33526E';
  setTimeout(function(){ coverImgSetValue('curso-imagen', c ? (c.imagenPortada||'') : ''); }, 0);
  document.getElementById('curso-grado').value = c ? c.grado : '';
  const areaActual = c ? c.area : '';
  const areaEnCatalogo = AREAS_ACADEMICAS_ADMIN.some(a=>a.nombre===areaActual);
  document.getElementById('curso-area').innerHTML = AREAS_ACADEMICAS_ADMIN.map(a=>'<option value="'+esc(a.nombre)+'"'+(a.nombre===areaActual?' selected':'')+'>'+esc(a.codigo)+' — '+esc(a.nombre)+'</option>').join('') +
    (areaActual && !areaEnCatalogo ? '<option value="'+esc(areaActual)+'" selected>'+esc(areaActual)+' (no está en el catálogo actual)</option>' : '');
  document.getElementById('curso-codigo').value = c ? (c.codigo||'') : '';
  document.getElementById('curso-precio').value = c ? c.precio : 0;
  document.getElementById('curso-creditos').value = c ? c.creditos : 1;
  document.getElementById('curso-horas').value = c ? c.horas : 10;
  document.getElementById('curso-nota').style.display = c ? 'none' : 'block';
  setTimeout(function(){ rteSetValue('curso-info-estudiante', c ? (c.infoEstudiante||'') : ''); }, 0);
  abrirModal('modal-curso');
}
function renderModalCurso() {
  if (document.getElementById('modal-curso')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-curso"><div class="modal grande"><div class="modal-header"><h3 id="curso-modal-titulo">Crear curso</h3><button class="modal-close" onclick="cerrarModal(\'modal-curso\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="curso-id">' +
    '<div class="grid-2"><div class="campo"><label>Nombre del curso</label><input type="text" id="curso-nombre"></div>' +
    '<div class="campo"><label>Color representativo</label><input type="color" id="curso-color" style="height:42px"></div></div>' +
    '<div class="grid-2"><div class="campo"><label>Grado</label><input type="text" id="curso-grado" placeholder="Ej: Décimo"></div>' +
    '<div class="campo"><label>Área académica</label><select id="curso-area" onchange="sugerirCodigoCursoDocente()"></select><p class="hint">Estas áreas las define el administrador de la plataforma en Catálogos académicos.</p></div></div>' +
    '<div class="campo"><label>Código del curso</label><div class="flex gap-8"><input id="curso-codigo" style="flex:1;font-family:monospace" placeholder="Se sugiere según el área"><button type="button" class="btn btn-ghost btn-sm" onclick="sugerirCodigoCursoDocente()">🔄 Sugerir</button></div></div>' +
    '<div class="campo"><label>Descripción</label><textarea id="curso-descripcion" rows="2"></textarea></div>' +
    '<div class="campo"><label>Objetivos de aprendizaje</label><textarea id="curso-objetivos" rows="2"></textarea></div>' +
    coverImageFieldHTML('curso-imagen', 'Imagen de portada', 'Se muestra como banner en la tarjeta del curso. Si se deja vacía, se usa el color representativo.') +
    '<div class="grid-2"><div class="campo"><label>Precio (COP)</label><input type="number" id="curso-precio"></div>' +
    '<div class="campo"><label>Créditos</label><input type="number" id="curso-creditos"></div>' +
    '<div class="campo"><label>Horas estimadas</label><input type="number" id="curso-horas"></div></div>' +
    '<div class="campo"><label>Información para el estudiante antes de inscribirse</label><p class="hint" style="margin:0 0 6px">Requisitos, temario, metodología o cualquier dato que el estudiante deba conocer antes de suscribirse.</p>' + richTextEditorHTML('curso-info-estudiante','Escribe aquí lo que el estudiante debe saber antes de inscribirse...') + '</div>' +
    '<p class="text-sm muted" id="curso-nota">El curso se crea en borrador. Podrás publicarlo cuando tenga al menos una lección.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-curso\')">Cancelar</button><button class="btn btn-primary" id="curso-modal-btn" onclick="guardarCurso()">Crear curso</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function abrirModalCursoPreview(id) {
  const c = DATA.cursos.find(x=>x.id===id);
  const color = c.color || '#33526E';
  document.getElementById('cp-banner').style.background = c.imagenPortada ? 'transparent' : color;
  document.getElementById('cp-banner').innerHTML = c.imagenPortada ? '<img src="'+esc(c.imagenPortada)+'" style="width:100%;height:100%;object-fit:cover">' : '';
  document.getElementById('cp-nombre').textContent = c.nombre;
  document.getElementById('cp-nombre').style.color = color;
  document.getElementById('cp-meta').textContent = (c.area||'—') + ' · Grado ' + (c.grado||'—') + ' · ' + c.horas + ' horas · ' + c.creditos + ' créditos';
  document.getElementById('cp-precio').textContent = '$' + (c.precio||0).toLocaleString('es-CO');
  document.getElementById('cp-descripcion').textContent = c.descripcion || '';
  document.getElementById('cp-objetivos').textContent = c.objetivos || '';
  document.getElementById('cp-info').innerHTML = c.infoEstudiante && c.infoEstudiante.trim() ? c.infoEstudiante : '<p class="muted">El docente todavía no agregó información adicional para el estudiante.</p>';
  document.getElementById('cp-btn-inscribir').style.background = color;
  abrirModal('modal-curso-preview');
}
function renderModalCursoPreview() {
  if (document.getElementById('modal-curso-preview')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-curso-preview"><div class="modal grande"><div class="modal-header"><h3>Vista previa del curso</h3><button class="modal-close" onclick="cerrarModal(\'modal-curso-preview\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div id="cp-banner" style="height:160px;border-radius:12px;margin-bottom:18px"></div>' +
    '<h2 id="cp-nombre" style="font-size:24px;margin:0 0 6px"></h2>' +
    '<p class="text-sm muted mb-14" id="cp-meta"></p>' +
    '<div class="flex justify-between items-center card mb-14"><span style="font-size:20px;font-weight:800" id="cp-precio"></span><button id="cp-btn-inscribir" disabled style="border:none;border-radius:9px;padding:10px 20px;color:#fff;font-weight:700;opacity:.85">Inscribirme</button></div>' +
    '<div class="campo"><label>Descripción</label><p class="text-sm" id="cp-descripcion"></p></div>' +
    '<div class="campo"><label>Objetivos de aprendizaje</label><p class="text-sm" id="cp-objetivos"></p></div>' +
    '<div class="campo"><label>Antes de inscribirte</label><div class="card" id="cp-info" style="font-size:13.5px;line-height:1.7"></div></div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se ve para el estudiante. El botón "Inscribirme" está desactivado en esta vista previa.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-curso-preview\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function sugerirCodigoCurso(nombreArea) {
  const area = AREAS_ACADEMICAS_ADMIN.find(a=>a.nombre===nombreArea);
  const prefijo = area ? area.codigo : (nombreArea||'CURSO').slice(0,6).toUpperCase();
  const existentes = DATA.cursos.filter(c=>(c.codigo||'').startsWith(prefijo+'-')).length;
  return prefijo + '-' + (101 + existentes);
}
function sugerirCodigoCursoDocente() {
  document.getElementById('curso-codigo').value = sugerirCodigoCurso(document.getElementById('curso-area').value);
}
function guardarCurso() {
  const id = document.getElementById('curso-id').value;
  const nombre = document.getElementById('curso-nombre').value.trim();
  if (!nombre) { toast('Escribe el nombre del curso', true); return; }
  const campos = {
    nombre, codigo: document.getElementById('curso-codigo').value.trim().toUpperCase() || sugerirCodigoCurso(document.getElementById('curso-area').value),
    descripcion: document.getElementById('curso-descripcion').value.trim(), objetivos: document.getElementById('curso-objetivos').value.trim(),
    imagenPortada: document.getElementById('curso-imagen').value.trim(), color: document.getElementById('curso-color').value,
    infoEstudiante: rteGetValue('curso-info-estudiante'),
    grado: document.getElementById('curso-grado').value.trim(), area: document.getElementById('curso-area').value.trim(),
    precio: Number(document.getElementById('curso-precio').value) || 0, creditos: Number(document.getElementById('curso-creditos').value) || 0,
    horas: Number(document.getElementById('curso-horas').value) || 0,
  };
  if (id) {
    Object.assign(DATA.cursos.find(x=>x.id===id), campos);
    toast('Curso "' + nombre + '" actualizado');
  } else {
    DATA.cursos.unshift({ id: uid('curso'), ...campos, estado:'borrador', inscritos:0, lecciones:[] });
    toast('Curso "' + nombre + '" creado como borrador');
  }
  saveData(); cerrarModal('modal-curso'); render();
}

/* ================================================================
   VISTA: LECCIONES
================================================================ */
function renderLecciones(cont) {
  const cursoId = window.ctx_lec_curso || '';
  let html = '<div class="section-header"><div><h2>Lecciones</h2><p class="subtitle">Cada lección agrupa materiales, actividades, evaluaciones, foros y sesiones.</p></div>' +
    (cursoId ? '<button class="btn btn-primary" onclick="abrirModalLeccion()">+ Nueva lección</button>' : '') + '</div>';
  html += '<div class="context-picker"><select onchange="setCtx(\'lec\',\'curso\',this.value)"><option value="">Selecciona un curso</option>';
  DATA.cursos.forEach(c => html += '<option value="'+c.id+'"'+(c.id===cursoId?' selected':'')+'>'+esc(c.nombre)+'</option>');
  html += '</select></div>';

  if (!cursoId) { html += '<div class="empty-state">Selecciona un curso para ver o crear sus lecciones.</div>'; cont.innerHTML = html; return; }

  const lecciones = leccionesDeCurso(cursoId);
  if (lecciones.length === 0) html += '<div class="empty-state">Este curso todavía no tiene lecciones.</div>';
  lecciones.forEach(l => {
    const nMat = DATA.materiales.filter(m=>m.leccionId===l.id).length;
    const nAct = DATA.actividades.filter(a=>a.leccionId===l.id).length;
    const nEval = DATA.evaluaciones.filter(e=>e.leccionId===l.id).length;
    const nForo = DATA.foros.filter(f=>f.leccionId===l.id).length;
    html += '<div class="card mb-14">' +
      '<div class="flex items-center gap-10 mb-8">⠿' +
      (l.imagenPortada ? '<img src="'+esc(l.imagenPortada)+'" style="width:52px;height:52px;object-fit:cover;border-radius:8px;flex-shrink:0" alt="">' : '') +
      '<div style="flex:1"><div style="font-size:14.5px;font-weight:600">' + l.orden + '. ' + esc(l.nombre) + '</div>' +
      '<div class="text-sm muted">Tipo: <span style="text-transform:capitalize">' + LECCION_TIPO_LABEL[l.tipo] + '</span>' + (l.prerequisito ? ' · Requiere: ' + esc(leccionNombre(l.prerequisito)) : '') + '</div></div>' +
      pill(l.estado) +
      '<button class="btn btn-ghost btn-xs" onclick="abrirModalLeccion(\''+l.id+'\')">✏️</button>' +
      '<button class="btn btn-ghost btn-xs" onclick="confirmarEliminarLeccion(\''+l.id+'\')" style="color:var(--clay)">🗑</button></div>' +
      '<p class="text-sm muted mb-14">' + esc(l.descripcion) + '</p>' +
      '<div class="flex gap-8 flex-wrap">' +
      '<button class="btn btn-ghost btn-sm" onclick="irA(\'materiales\',\''+cursoId+'\',\''+l.id+'\')">📁 Materiales (' + nMat + ')</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="irA(\'actividades\',\''+cursoId+'\',\''+l.id+'\')">✅ Actividades (' + nAct + ')</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="irA(\'evaluaciones\',\''+cursoId+'\',\''+l.id+'\')">📋 Evaluaciones (' + nEval + ')</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="irA(\'foros\',\''+cursoId+'\',\''+l.id+'\')">💬 Foros (' + nForo + ')</button>';
    if (l.estado === 'borrador') html += '<button class="btn btn-amber btn-sm" onclick="cambiarEstadoLeccion(\''+l.id+'\',\'activa\')">Activar</button>';
    if (l.estado === 'activa') html += '<button class="btn btn-ghost btn-sm" onclick="cambiarEstadoLeccion(\''+l.id+'\',\'inactiva\')">Desactivar</button>';
    html += '</div></div>';
  });
  cont.innerHTML = html;
  renderModalLeccion(cursoId);
}
function irA(vista, cursoId, leccionId) {
  const prefix = { materiales:'mat', actividades:'act', evaluaciones:'eva', foros:'for', sesiones:'ses' }[vista];
  window['ctx_'+prefix+'_curso'] = cursoId; window['ctx_'+prefix+'_leccion'] = leccionId;
  goTo(vista);
}
function irAMaterialesDeLeccion(cursoId, leccionId) { irA('materiales', cursoId, leccionId); }
function cambiarEstadoLeccion(id, estado) {
  DATA.leccionesById[id].estado = estado; saveData(); render();
  toast('Lección "' + DATA.leccionesById[id].nombre + '" ahora está ' + estado);
}
function confirmarEliminarLeccion(id) {
  const l = DATA.leccionesById[id];
  confirmarAccion('¿Eliminar <strong>' + esc(l.nombre) + '</strong>? Se perderán sus materiales, actividades, evaluaciones y foros asociados.', function () {
    delete DATA.leccionesById[id];
    DATA.cursos.forEach(c => { c.lecciones = c.lecciones.filter(lid=>lid!==id); });
    saveData(); render(); toast('Lección "' + l.nombre + '" eliminada');
  });
}
function abrirModalLeccion(id) {
  const l = id ? DATA.leccionesById[id] : null;
  document.getElementById('leccion-modal-titulo').textContent = l ? 'Editar lección' : 'Nueva lección';
  document.getElementById('leccion-id').value = l ? l.id : '';
  document.getElementById('leccion-nombre').value = l ? l.nombre : '';
  document.getElementById('leccion-descripcion').value = l ? l.descripcion : '';
  document.getElementById('leccion-tipo').value = l ? l.tipo : 'teoria';
  document.getElementById('leccion-orden').value = l ? l.orden : (leccionesDeCurso(window.ctx_lec_curso).length + 1);
  setTimeout(function(){ coverImgSetValue('leccion-imagen', l ? (l.imagenPortada||'') : ''); }, 0);
  const cursoId = window.ctx_lec_curso;
  const sel = document.getElementById('leccion-prereq');
  sel.innerHTML = '<option value="">Sin prerequisito</option>' + leccionesDeCurso(cursoId).filter(x=>x.id!==id).map(x=>'<option value="'+x.id+'"'+(l&&l.prerequisito===x.id?' selected':'')+'>'+esc(x.nombre)+'</option>').join('');
  abrirModal('modal-leccion');
}
function renderModalLeccion(cursoId) {
  if (document.getElementById('modal-leccion')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-leccion"><div class="modal"><div class="modal-header"><h3 id="leccion-modal-titulo">Nueva lección</h3><button class="modal-close" onclick="cerrarModal(\'modal-leccion\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="leccion-id">' +
    '<div class="campo"><label>Nombre</label><input type="text" id="leccion-nombre" placeholder="Ej: Mitosis y Meiosis"></div>' +
    '<div class="campo"><label>Descripción</label><textarea id="leccion-descripcion" rows="2"></textarea></div>' +
    '<div class="grid-2"><div class="campo"><label>Tipo</label><select id="leccion-tipo"><option value="teoria">Teoría</option><option value="practica">Práctica</option><option value="evaluacion">Evaluación</option><option value="mixta">Mixta</option></select></div>' +
    '<div class="campo"><label>Orden</label><input type="number" id="leccion-orden"></div></div>' +
    '<div class="campo"><label>Prerequisito</label><select id="leccion-prereq"></select><p class="hint">El estudiante debe completar esta lección antes de acceder.</p></div>' +
    coverImageFieldHTML('leccion-imagen', 'Imagen de portada', 'Aparece como miniatura junto al nombre de la lección en la lista.') + '</div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-leccion\')">Cancelar</button><button class="btn btn-primary" onclick="guardarLeccion()">Guardar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarLeccion() {
  const id = document.getElementById('leccion-id').value;
  const nombre = document.getElementById('leccion-nombre').value.trim();
  const cursoId = window.ctx_lec_curso;
  if (!nombre) { toast('Escribe el nombre de la lección', true); return; }
  if (!cursoId) { toast('Selecciona primero un curso', true); return; }
  const campos = { nombre, descripcion: document.getElementById('leccion-descripcion').value.trim(), tipo: document.getElementById('leccion-tipo').value,
    orden: Number(document.getElementById('leccion-orden').value) || 1, imagenPortada: document.getElementById('leccion-imagen').value.trim(),
    prerequisito: document.getElementById('leccion-prereq').value || null };
  if (id) {
    Object.assign(DATA.leccionesById[id], campos);
    toast('Lección "' + nombre + '" actualizada');
  } else {
    const nueva = { id: uid('lec'), cursoId, estado:'borrador', ...campos };
    DATA.leccionesById[nueva.id] = nueva;
    DATA.cursos.find(c=>c.id===cursoId).lecciones.push(nueva.id);
    toast('Lección "' + nombre + '" creada');
  }
  saveData(); cerrarModal('modal-leccion'); render();
}
