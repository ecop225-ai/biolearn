/* ================================================================
   BIOLearn — biolearn_docente_materiales.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: MATERIALES
================================================================ */
function renderMateriales(cont) {
  const cursoId = window.ctx_mat_curso || '', leccionId = window.ctx_mat_leccion || '';
  let html = '<div class="section-header"><div><h2>Materiales de contenido</h2><p class="subtitle">Lecturas, videos, laboratorios y juegos externos, documentos y artículos científicos.</p></div>' +
    (leccionId ? '<button class="btn btn-primary" onclick="abrirModalMaterial()">+ Nuevo material</button>' : '') + '</div>';
  html += contextPickerHTML('mat');
  if (!leccionId) { html += '<div class="empty-state">Elige un curso y una lección para ver sus materiales.</div>'; cont.innerHTML = html; renderModalMaterial(); return; }

  const items = DATA.materiales.filter(m => m.leccionId === leccionId).sort((a,b)=>a.orden-b.orden);
  if (items.length === 0) html += '<div class="empty-state">Esta lección aún no tiene materiales.</div>';
  items.forEach((m, i) => {
    const t = MATERIAL_TYPES[m.tipo];
    const esInteractivo = m.tipo === 'laboratorio' || m.tipo === 'juego' || m.tipo === 'video';
    let meta = t.label;
    if (m.proveedor) meta += ' · ' + esc(m.proveedor) + ' (externo)';
    if (m.autores) meta += ' · ' + esc(m.autores);
    if (m.duracion) meta += ' · ' + esc(m.duracion);
    html += '<div class="material-row">' +
      '<div class="m-icon" style="color:' + t.color + '">' + t.icon + '</div>' +
      '<div class="m-info"><div class="m-nombre">' + esc(m.nombre) + '</div><div class="m-meta">' + meta + '</div></div>' +
      '<button class="pill-toggle" onclick="toggleEstadoMaterial(\''+m.id+'\')" title="Cambiar estado">' + pill(m.estado) + '</button>' +
      (m.tipo === 'lectura' ? '<button class="btn btn-ghost btn-xs" onclick="abrirLecturaPreview(\''+m.id+'\')">👁 Vista previa</button>' : '') +
      (esInteractivo ? '<button class="btn btn-ghost btn-xs" onclick="abrirInteractivePreview(\''+m.id+'\','+(i+1)+','+items.length+')">👁 Ver como estudiante</button>' : '') +
      (m.url ? '<a href="'+esc(m.url)+'" target="_blank" title="Abrir enlace externo" style="color:var(--ink-soft)">↗</a>' : '') +
      '<button class="btn btn-ghost btn-xs" onclick="abrirModalMaterial(\''+m.id+'\')">✏️</button>' +
      '<button class="btn btn-ghost btn-xs" onclick="confirmarEliminarMaterial(\''+m.id+'\')" style="color:var(--clay)">🗑</button>' +
      '</div>';
  });
  cont.innerHTML = html;
  renderModalMaterial();
  renderModalPreviewLectura();
  renderModalPreviewInteractivo();
}
function toggleEstadoMaterial(id) {
  const m = DATA.materiales.find(x=>x.id===id);
  m.estado = m.estado === 'activo' ? 'inactivo' : 'activo';
  saveData(); render();
}
function confirmarEliminarMaterial(id) {
  const m = DATA.materiales.find(x=>x.id===id);
  confirmarAccion('¿Eliminar <strong>' + esc(m.nombre) + '</strong>? Esta acción no se puede deshacer.', function () {
    DATA.materiales = DATA.materiales.filter(x=>x.id!==id);
    saveData(); render(); toast('Material "' + m.nombre + '" eliminado');
  });
}

/* ---- vista previa: lectura (contenido de texto enriquecido) ---- */
function abrirLecturaPreview(id) {
  const m = DATA.materiales.find(x=>x.id===id);
  document.getElementById('preview-lectura-titulo').textContent = m.nombre;
  document.getElementById('preview-lectura-body').innerHTML = m.contenido && m.contenido.trim() ? m.contenido : '<p class="muted">Este material todavía no tiene contenido de lectura.</p>';
  document.getElementById('preview-lectura-body').style.background = m.fondo || '#ffffff';
  abrirModal('modal-preview-lectura');
}
function renderModalPreviewLectura() {
  if (document.getElementById('modal-preview-lectura')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-preview-lectura"><div class="modal grande"><div class="modal-header"><h3>Vista previa del material</h3><button class="modal-close" onclick="cerrarModal(\'modal-preview-lectura\')">✕</button></div>' +
    '<div class="modal-body"><span class="pill pill-ambar">📖 Lectura</span>' +
    '<h2 style="margin:12px 0 16px" id="preview-lectura-titulo"></h2>' +
    '<div class="card" id="preview-lectura-body" style="font-size:14.5px;line-height:1.8"></div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se verá para el estudiante — usa este panel para confirmar que la información queda bien presentada antes de publicar.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-preview-lectura\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ---- vista previa: laboratorio / juego / video (simulación incrustada) ---- */
function abrirInteractivePreview(id, index, total) {
  const m = DATA.materiales.find(x=>x.id===id);
  const leccion = DATA.leccionesById[m.leccionId];
  const t = MATERIAL_TYPES[m.tipo];
  const etiqueta = m.tipo === 'laboratorio' ? 'Simulación interactiva' : m.tipo === 'juego' ? 'Juego interactivo' : 'Video incrustado';
  document.getElementById('preview-inter-titulo').textContent = m.nombre;
  document.getElementById('preview-inter-badge').innerHTML = t.icon + ' ' + t.label;
  document.getElementById('preview-inter-meta').innerHTML =
    (m.duracion ? '<span>⏱ ' + esc(m.duracion) + '</span>' : '') +
    '<span>' + t.icon + ' Material ' + index + ' de ' + total + ' — Lección ' + (leccion?leccion.orden:'—') + '</span>' +
    '<span class="pill pill-azul">' + etiqueta + '</span>';
  document.getElementById('preview-inter-frame-wrap').style.display = 'none';
  document.getElementById('preview-inter-launch').style.display = 'block';
  document.getElementById('preview-inter-launch-msg').textContent = m.proveedor ? 'Contenido alojado en ' + m.proveedor : 'Contenido interactivo externo';
  const btnLaunch = document.getElementById('preview-inter-launch-btn');
  btnLaunch.disabled = !m.url;
  btnLaunch.textContent = '▶ ' + (m.tipo === 'video' ? 'Reproducir video' : 'Iniciar simulación');
  btnLaunch.onclick = function () {
    if (!m.url) return;
    document.getElementById('preview-inter-frame-wrap').style.display = 'block';
    document.getElementById('preview-inter-launch').style.display = 'none';
    document.getElementById('preview-inter-frame-url').textContent = m.url;
    document.getElementById('preview-inter-iframe').src = m.url;
  };
  document.getElementById('preview-inter-sin-url').style.display = m.url ? 'none' : 'block';
  document.getElementById('preview-inter-guia-wrap').style.display = (m.guia && m.guia.trim()) ? 'block' : 'none';
  document.getElementById('preview-inter-guia').innerHTML = m.guia || '';
  abrirModal('modal-preview-interactivo');
}
function renderModalPreviewInteractivo() {
  if (document.getElementById('modal-preview-interactivo')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-preview-interactivo"><div class="modal grande"><div class="modal-header"><h3>Vista previa del material</h3><button class="modal-close" onclick="cerrarModal(\'modal-preview-interactivo\');document.getElementById(\'preview-inter-iframe\').src=\'\'">✕</button></div>' +
    '<div class="modal-body"><span class="pill pill-azul" id="preview-inter-badge"></span>' +
    '<h2 style="margin:10px 0 6px" id="preview-inter-titulo"></h2>' +
    '<div class="flex gap-14 flex-wrap text-sm muted mb-14" id="preview-inter-meta"></div>' +
    '<div id="preview-inter-launch" style="background:linear-gradient(135deg,#1E3345,#2E5588);border-radius:14px;padding:48px 24px;text-align:center">' +
      '<div style="width:90px;height:90px;border-radius:50%;background:radial-gradient(circle,#5B8FD9,#2E5588);margin:0 auto 18px"></div>' +
      '<p style="color:#fff;font-size:14.5px;margin-bottom:18px" id="preview-inter-launch-msg"></p>' +
      '<button class="btn" id="preview-inter-launch-btn" style="background:#fff;color:#1E3345;border-radius:30px;padding:12px 26px">▶ Iniciar</button>' +
      '<p id="preview-inter-sin-url" style="display:none;color:#F4E1DA;font-size:12px;margin-top:12px">Todavía no se ha configurado el enlace externo de este material.</p>' +
    '</div>' +
    '<div id="preview-inter-frame-wrap" style="display:none;border-radius:14px;overflow:hidden;border:1px solid var(--line)">' +
      '<div style="background:var(--blue-deep);color:#fff;font-size:12px;padding:8px 14px;display:flex;justify-content:space-between"><span id="preview-inter-frame-url"></span>' +
      '<button onclick="document.getElementById(\'preview-inter-frame-wrap\').style.display=\'none\';document.getElementById(\'preview-inter-launch\').style.display=\'block\';document.getElementById(\'preview-inter-iframe\').src=\'\'" style="background:none;border:none;color:#fff;cursor:pointer;font-size:12px">Cerrar ventana</button></div>' +
      '<iframe id="preview-inter-iframe" style="width:100%;height:380px;border:none;background:#fff"></iframe></div>' +
    '<div id="preview-inter-guia-wrap" style="display:none;margin-top:22px">' +
      '<div style="font-size:12.5px;font-weight:700;color:var(--sage);text-transform:uppercase;letter-spacing:.4px;margin-bottom:10px">Guía de observación</div>' +
      '<div class="card" id="preview-inter-guia"></div></div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se verá para el estudiante — la página externa se incrusta dentro de la plataforma cuando el estudiante presiona iniciar.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-preview-interactivo\');document.getElementById(\'preview-inter-iframe\').src=\'\'">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ---- modal crear/editar material ---- */
function abrirModalMaterial(id) {
  const m = id ? DATA.materiales.find(x=>x.id===id) : null;
  document.getElementById('mat-modal-titulo').textContent = m ? 'Editar material' : 'Nuevo material';
  document.getElementById('mat-id').value = m ? m.id : '';
  document.getElementById('mat-nombre').value = m ? m.nombre : '';
  document.getElementById('mat-descripcion').value = m ? m.descripcion : '';
  document.getElementById('mat-duracion').value = m ? (m.duracion||'') : '';
  document.getElementById('mat-proveedor').value = m ? (m.proveedor||'') : '';
  document.getElementById('mat-url').value = m ? (m.url||'') : '';
  document.getElementById('mat-url-video').value = m ? (m.url||'') : '';
  document.getElementById('mat-autores').value = m ? (m.autores||'') : '';
  document.getElementById('mat-revista').value = m ? (m.revista||'') : '';
  document.getElementById('mat-doi').value = m ? (m.doi||'') : '';
  document.getElementById('mat-archivo-label').textContent = m && m.archivo ? m.archivo : '';
  window._matArchivo = m ? (m.archivo||'') : '';
  document.getElementById('mat-fondo').value = m && m.fondo ? m.fondo : '#ffffff';
  seleccionarTipoMaterial(m ? m.tipo : 'lectura');
  setTimeout(function(){
    rteSetValue('mat-editor', m ? (m.contenido||'') : '');
    rteSetValue('mat-guia', m ? (m.guia||'') : '');
    document.getElementById('mat-editor').style.background = m && m.fondo ? m.fondo : '#ffffff';
  }, 0);
  abrirModal('modal-material');
}
function seleccionarTipoMaterial(tipo) {
  document.getElementById('mat-tipo').value = tipo;
  document.querySelectorAll('.tipo-btn').forEach(b => {
    const activo = b.dataset.tipo === tipo;
    b.classList.toggle('activo', activo);
    b.style.borderColor = activo ? MATERIAL_TYPES[b.dataset.tipo].color : '';
    b.style.color = activo ? MATERIAL_TYPES[b.dataset.tipo].color : '';
  });
  const esLabJuegoVideo = tipo === 'laboratorio' || tipo === 'juego' || tipo === 'video';
  const esLabJuego = tipo === 'laboratorio' || tipo === 'juego';
  document.getElementById('mat-campo-duracion').style.display = esLabJuegoVideo ? 'block' : 'none';
  document.getElementById('mat-campo-externo').style.display = esLabJuego ? 'block' : 'none';
  document.getElementById('mat-campo-video-url').style.display = tipo === 'video' ? 'block' : 'none';
  document.getElementById('mat-campo-guia').style.display = esLabJuegoVideo ? 'block' : 'none';
  document.getElementById('mat-campo-articulo').style.display = tipo === 'articulo' ? 'block' : 'none';
  document.getElementById('mat-campo-lectura').style.display = tipo === 'lectura' ? 'block' : 'none';
  document.getElementById('mat-campo-documento').style.display = tipo === 'documento' ? 'block' : 'none';
}
function subirArchivoMaterial() {
  const input = document.createElement('input');
  input.type = 'file'; input.accept = '.pdf,.doc,.docx';
  input.onchange = function () {
    const file = input.files[0]; if (!file) return;
    window._matArchivo = file.name;
    document.getElementById('mat-archivo-label').textContent = file.name;
  };
  input.click();
}
function renderModalMaterial() {
  if (document.getElementById('modal-material')) return;
  const tipoBtns = Object.entries(MATERIAL_TYPES).map(([k,v]) =>
    '<button type="button" class="tipo-btn" data-tipo="'+k+'" onclick="seleccionarTipoMaterial(\''+k+'\')"><span class="m-icon-lg">'+v.icon+'</span>'+v.label+'</button>').join('');
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-material"><div class="modal grande"><div class="modal-header"><h3 id="mat-modal-titulo">Nuevo material</h3><button class="modal-close" onclick="cerrarModal(\'modal-material\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="mat-id"><input type="hidden" id="mat-tipo" value="lectura">' +
    '<div class="campo"><label>Tipo de material</label><div class="tipo-selector">'+tipoBtns+'</div></div>' +
    '<div class="campo"><label>Nombre</label><input type="text" id="mat-nombre" placeholder="Ej: Las leyes de Mendel"></div>' +
    '<div class="campo"><label>Descripción</label><textarea id="mat-descripcion" rows="2"></textarea></div>' +
    '<div id="mat-campo-duracion" class="campo" style="display:none"><label>Duración estimada</label><input type="text" id="mat-duracion" placeholder="15 min estimados"></div>' +
    '<div id="mat-campo-externo" style="display:none">' +
      '<div class="campo"><label>Proveedor externo</label><input type="text" id="mat-proveedor" placeholder="Ej: Labster, PhET, Kahoot..."></div>' +
      '<div class="campo"><label>Enlace externo</label><input type="url" id="mat-url" placeholder="https://..."><p class="hint">El estudiante lo verá incrustado dentro de la plataforma al iniciar.</p></div>' +
    '</div>' +
    '<div id="mat-campo-video-url" class="campo" style="display:none"><label>Enlace del video</label><input type="url" id="mat-url-video" placeholder="https://..."><p class="hint">Se incrusta dentro de la plataforma (ej. enlace de inserción de YouTube).</p></div>' +
    '<div id="mat-campo-guia" class="campo" style="display:none"><label>Guía de observación (opcional)</label>' + richTextEditorHTML('mat-guia','1. Observa... 2. Anota... 3. Responde...') + '</div>' +
    '<div id="mat-campo-articulo" style="display:none">' +
      '<div class="campo"><label>Autores</label><input type="text" id="mat-autores"></div>' +
      '<div class="grid-2"><div class="campo"><label>Revista</label><input type="text" id="mat-revista"></div><div class="campo"><label>DOI</label><input type="text" id="mat-doi"></div></div>' +
      '<div class="campo"><label>Enlace</label><input type="url" id="mat-url-articulo" placeholder="https://..."></div>' +
    '</div>' +
    '<div id="mat-campo-lectura" style="display:none">' +
      '<div class="campo"><label>Color de fondo de la lectura (opcional)</label>' +
      '<div class="flex items-center gap-10"><input type="color" id="mat-fondo" value="#ffffff" onchange="document.getElementById(\'mat-editor\').style.background=this.value">' +
      '<button type="button" class="btn btn-ghost btn-sm" onclick="document.getElementById(\'mat-fondo\').value=\'#ffffff\';document.getElementById(\'mat-editor\').style.background=\'#ffffff\'">Restablecer a blanco</button></div></div>' +
      '<div class="campo"><label>Contenido de la lectura</label><p class="hint" style="margin:0 0 6px">El estudiante lee este contenido directamente dentro de la plataforma. Usa "Vista previa" (ícono del ojo) para ver cómo quedará.</p>' + richTextEditorHTML('mat-editor','Escribe aquí el texto de la lectura...') + '</div>' +
    '</div>' +
    '<div id="mat-campo-documento" style="display:none"><button type="button" class="btn btn-ghost btn-sm" onclick="subirArchivoMaterial()">⬆ Subir documento (PDF)</button> <span class="text-sm muted" id="mat-archivo-label"></span></div>' +
    '</div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-material\')">Cancelar</button><button class="btn btn-primary" onclick="guardarMaterial()">Guardar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarMaterial() {
  const id = document.getElementById('mat-id').value;
  const nombre = document.getElementById('mat-nombre').value.trim();
  const leccionId = window.ctx_mat_leccion;
  if (!nombre) { toast('Escribe el nombre del material', true); return; }
  if (!leccionId) { toast('Selecciona primero una lección', true); return; }
  const tipo = document.getElementById('mat-tipo').value;
  const campos = {
    nombre, descripcion: document.getElementById('mat-descripcion').value.trim(), tipo,
    duracion: document.getElementById('mat-duracion').value.trim(),
    proveedor: document.getElementById('mat-proveedor').value.trim(),
    url: tipo === 'articulo' ? document.getElementById('mat-url-articulo').value.trim() : tipo === 'video' ? document.getElementById('mat-url-video').value.trim() : document.getElementById('mat-url').value.trim(),
    guia: rteGetValue('mat-guia'),
    autores: document.getElementById('mat-autores').value.trim(),
    revista: document.getElementById('mat-revista').value.trim(),
    doi: document.getElementById('mat-doi').value.trim(),
    contenido: rteGetValue('mat-editor'),
    archivo: window._matArchivo || '',
    fondo: document.getElementById('mat-fondo').value,
  };
  if (id) {
    Object.assign(DATA.materiales.find(x=>x.id===id), campos);
    toast('Material "' + nombre + '" actualizado');
  } else {
    const orden = DATA.materiales.filter(m=>m.leccionId===leccionId).length + 1;
    DATA.materiales.push({ id: uid('mat'), leccionId, orden, estado:'activo', ...campos });
    toast('Material "' + nombre + '" agregado');
  }
  saveData(); cerrarModal('modal-material'); render();
}
