/* ================================================================
   BIOLearn — biolearn_docente_actividades.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: ACTIVIDADES
================================================================ */
let ACT_FILTRO = {}; // { actividadId: 'todas'|'pendientes'|'calificadas' }
let ACT_ENUNCIADO_ABIERTO = {};

function renderActividades(cont) {
  const leccionId = window.ctx_act_leccion || '';
  let html = '<div class="section-header"><div><h2>Actividades</h2><p class="subtitle">Crea tareas, revisa las entregas de tus estudiantes y califica con retroalimentación.</p></div>' +
    (leccionId ? '<button class="btn btn-primary" onclick="abrirModalActividad()">+ Nueva actividad</button>' : '') + '</div>';
  html += contextPickerHTML('act', ['practica','mixta']);
  if (!leccionId) { html += '<div class="empty-state">Selecciona un curso y una lección para ver o crear actividades.</div>'; cont.innerHTML = html; renderModalActividad(); renderModalActividadPreview(); return; }

  const items = DATA.actividades.filter(a => a.leccionId === leccionId);
  if (items.length === 0) html += '<div class="empty-state">Esta lección aún no tiene actividades.</div>';

  items.forEach(a => {
    const filtro = ACT_FILTRO[a.id] || 'todas';
    const pendientes = a.entregas.filter(e=>e.estado==='entregada');
    const calificadas = a.entregas.filter(e=>e.estado==='revisada');
    const entregasFiltradas = filtro==='pendientes' ? pendientes : filtro==='calificadas' ? calificadas : a.entregas;
    const enunciadoVisible = ACT_ENUNCIADO_ABIERTO[a.id] !== false;

    html += '<div class="card mb-14" style="background:var(--bone)">';
    html += '<div class="flex justify-between items-start mb-4"><span class="pill pill-ambar">✏️ Actividad</span>' +
      '<div class="flex gap-8"><button class="btn btn-ghost btn-xs" onclick="abrirModalActividadPreview(\''+a.id+'\')">👁 Ver como estudiante</button>' +
      '<button class="btn btn-ghost btn-xs" onclick="confirmarEliminarActividad(\''+a.id+'\')" style="color:var(--clay)">🗑</button></div></div>';
    html += '<h3 style="font-size:20px;margin:8px 0 6px">' + esc(a.nombre) + '</h3>';
    html += '<div class="flex gap-14 flex-wrap text-sm muted mb-14"><span>📅 Vence ' + fmtDate(a.fechaLimite) + '</span><span>⭐ Valor: ' + a.puntajeMax.toFixed(1) + ' pts</span>' +
      (pendientes.length>0 ? '<span class="pill pill-ambar">' + pendientes.length + ' sin calificar</span>' : '') + '</div>';

    html += '<div class="card mb-14"><div class="flex justify-between items-center" style="cursor:pointer" onclick="toggleEnunciadoActividad(\''+a.id+'\')">' +
      '<span style="font-weight:700;font-size:14px">📋 Enunciado de la actividad</span><span style="font-size:12.5px;font-weight:700;color:var(--blue)">' + (enunciadoVisible?'Ocultar ▲':'Mostrar ▼') + '</span></div>';
    if (enunciadoVisible) {
      html += '<div class="text-sm" style="margin-top:12px;line-height:1.7">' + (a.instrucciones || '<span class="muted">Sin instrucciones todavía.</span>') + '</div>' +
        (a.archivosAdjuntos && a.archivosAdjuntos.length ? renderChipsArchivos(a.archivosAdjuntos) : '') +
        '<button class="btn btn-ghost btn-sm" style="margin-top:10px" onclick="abrirEditarEnunciado(\''+a.id+'\')">✏️ Editar enunciado</button>';
    }
    html += '</div>';

    html += '<div class="text-sm muted" style="font-weight:700;text-transform:uppercase;letter-spacing:.4px;margin-bottom:10px">📥 Entregas de estudiantes</div>';
    html += '<div class="flex gap-8" style="background:var(--panel);border:1px solid var(--line);border-radius:12px;padding:10px;margin-bottom:14px;width:fit-content">';
    [['todas','Todas ('+a.entregas.length+')'],['pendientes','Pendientes ('+pendientes.length+')'],['calificadas','Calificadas ('+calificadas.length+')']].forEach(([key,label]) => {
      const activo = filtro === key;
      html += '<button onclick="setFiltroActividad(\''+a.id+'\',\''+key+'\')" style="padding:7px 14px;border-radius:8px;font-size:12.5px;font-weight:700;cursor:pointer;border:none;background:' + (activo?'var(--blue-soft)':'transparent') + ';color:' + (activo?'var(--blue)':'var(--ink-soft)') + '">' + label + '</button>';
    });
    html += '</div>';

    if (entregasFiltradas.length === 0) html += '<div class="empty-state">No hay entregas en este filtro.</div>';
    entregasFiltradas.forEach(e => {
      const iniciales = e.estudiante.split(' ').map(p=>p[0]).slice(0,2).join('');
      html += '<div class="card mb-14" style="padding:0;overflow:hidden">' +
        '<div class="flex justify-between items-center" style="padding:14px 18px;background:' + (e.estado==='entregada'?'var(--blue-soft)':'transparent') + '">' +
        '<div class="flex items-center gap-10"><div style="width:34px;height:34px;border-radius:50%;background:var(--sage-soft);color:var(--sage);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px">'+esc(iniciales)+'</div>' +
        '<span style="font-weight:700;font-size:14.5px">' + esc(e.estudiante) + '</span></div>' +
        '<div class="flex items-center gap-10">' + pill(e.estado==='entregada'?'Pendiente de calificar':'Calificada') +
        '<span class="text-sm muted">Entregada: ' + esc(e.fechaEntrega) + (e.esTardia?' (tardía)':'') + '</span></div></div>';
      html += '<div style="padding:18px">' +
        '<div class="text-sm muted" style="font-weight:700;text-transform:uppercase;margin-bottom:6px">📝 Respuesta del estudiante</div>' +
        '<p class="text-sm mb-14">' + esc(e.respuestaTexto || 'El estudiante no incluyó texto adicional, solo el archivo adjunto.') + '</p>' +
        '<div class="flex justify-between items-center" style="background:var(--bone);border-radius:10px;padding:10px 14px;margin-bottom:16px">' +
        '<span class="text-sm">📄 ' + esc(e.archivo||'Sin archivo') + '</span><span class="text-sm" style="color:var(--blue);font-weight:700;cursor:pointer">Ver archivo →</span></div>';
      html += '<div class="flex gap-14 flex-wrap"><div style="flex:2;min-width:220px" class="campo"><label>Retroalimentación al estudiante *</label>' +
        '<textarea id="grade-retro-'+e.id+'" rows="2" placeholder="Escribe una retroalimentación constructiva...">'+esc(e.comentarioRevisor||'')+'</textarea></div>' +
        '<div style="width:110px" class="campo"><label>Nota /' + a.puntajeMax.toFixed(1) + ' *</label><input type="number" id="grade-nota-'+e.id+'" min="0" max="'+a.puntajeMax+'" step="0.1" value="'+(e.calificacion!=null?e.calificacion:'')+'"></div></div>';
      html += '<div class="campo"><label>Sugerencias de mejora (opcional)</label><textarea id="grade-sug-'+e.id+'" rows="2" placeholder="Ej: Revisar el artículo científico de la lección...">'+esc(e.sugerencias||'')+'</textarea></div>';
      html += '<div class="flex justify-between items-center flex-wrap gap-10">' +
        '<select id="grade-estado-'+e.id+'" style="width:170px"><option value="aprobado"'+(e.aprobacion==='aprobado'||!e.aprobacion?' selected':'')+'>✅ Aprobado</option><option value="revision"'+(e.aprobacion==='revision'?' selected':'')+'>🔄 Requiere revisión</option><option value="rechazado"'+(e.aprobacion==='rechazado'?' selected':'')+'>❌ Rechazado</option></select>' +
        '<div class="flex gap-10"><button class="btn btn-ghost" onclick="guardarBorradorEntrega(\''+a.id+'\',\''+e.id+'\')">Guardar borrador</button>' +
        '<button class="btn btn-primary" onclick="calificarYNotificar(\''+a.id+'\',\''+e.id+'\')">📤 Calificar y notificar</button></div></div>';
      if (e.estado === 'revisada') html += '<div style="margin-top:14px;background:var(--sage-soft);border-radius:10px;padding:10px 14px;font-size:12.5px;color:var(--sage)">✓ Calificada con ' + (e.calificacion!=null?e.calificacion.toFixed(1):'—') + ' / ' + a.puntajeMax.toFixed(1) + ' y retroalimentación enviada.</div>';
      html += '</div></div>';
    });
    html += '</div>';
  });
  cont.innerHTML = html;
  renderModalActividad(); renderModalActividadPreview();
}
function toggleEnunciadoActividad(id) { ACT_ENUNCIADO_ABIERTO[id] = ACT_ENUNCIADO_ABIERTO[id] === false; render(); }
function setFiltroActividad(id, key) { ACT_FILTRO[id] = key; render(); }
function guardarBorradorEntrega(actId, entId) {
  const e = DATA.actividades.find(x=>x.id===actId).entregas.find(x=>x.id===entId);
  e.comentarioRevisor = document.getElementById('grade-retro-'+entId).value.trim();
  e.sugerencias = document.getElementById('grade-sug-'+entId).value.trim();
  e.aprobacion = document.getElementById('grade-estado-'+entId).value;
  saveData(); toast('Borrador de calificación guardado');
}
function calificarYNotificar(actId, entId) {
  const a = DATA.actividades.find(x=>x.id===actId), e = a.entregas.find(x=>x.id===entId);
  const retro = document.getElementById('grade-retro-'+entId).value.trim();
  const nota = document.getElementById('grade-nota-'+entId).value;
  if (!retro || nota === '') { toast('Escribe una retroalimentación y una nota antes de calificar', true); return; }
  e.calificacion = Number(nota); e.comentarioRevisor = retro;
  e.sugerencias = document.getElementById('grade-sug-'+entId).value.trim();
  e.aprobacion = document.getElementById('grade-estado-'+entId).value; e.estado = 'revisada';
  saveData(); render(); toast('Calificación enviada a ' + e.estudiante + ' y notificada');
}
function confirmarEliminarActividad(id) {
  const a = DATA.actividades.find(x=>x.id===id);
  confirmarAccion('¿Eliminar <strong>' + esc(a.nombre) + '</strong>? Se perderán las entregas registradas.', function () {
    DATA.actividades = DATA.actividades.filter(x=>x.id!==id);
    saveData(); render(); toast('Actividad "' + a.nombre + '" eliminada');
  });
}
function abrirModalActividad(id) {
  const a = id ? DATA.actividades.find(x=>x.id===id) : null;
  document.getElementById('act-modal-titulo').textContent = a ? 'Editar enunciado de la actividad' : 'Nueva actividad';
  document.getElementById('act-id').value = a ? a.id : '';
  document.getElementById('act-nombre').value = a ? a.nombre : '';
  document.getElementById('act-descripcion').value = a ? a.descripcion : '';
  setTimeout(function(){ rteSetValue('act-instrucciones', a ? (a.instrucciones||'') : ''); }, 0);
  document.getElementById('act-tipo').value = a ? a.tipo : 'tarea';
  document.getElementById('act-fecha-disp').value = a ? a.fechaDisponible : '';
  document.getElementById('act-fecha-limite').value = a ? a.fechaLimite : '';
  document.getElementById('act-puntaje').value = a ? a.puntajeMax : 5;
  document.getElementById('act-tardia').checked = a ? a.permiteTardia : false;
  window._actArchivos = a && a.archivosAdjuntos ? a.archivosAdjuntos : [];
  document.getElementById('act-archivo-label').innerHTML = renderChipsArchivos(window._actArchivos);
  abrirModal('modal-actividad');
}
function abrirEditarEnunciado(id) { abrirModalActividad(id); }
function subirArchivoActividad() {
  seleccionarArchivos(null, function (archivos) {
    window._actArchivos = (window._actArchivos || []).concat(archivos);
    document.getElementById('act-archivo-label').innerHTML = renderChipsArchivos(window._actArchivos);
  });
}
function renderModalActividad() {
  if (document.getElementById('modal-actividad')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-actividad"><div class="modal"><div class="modal-header"><h3 id="act-modal-titulo">Nueva actividad</h3><button class="modal-close" onclick="cerrarModal(\'modal-actividad\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="act-id">' +
    '<div class="campo"><label>Nombre</label><input type="text" id="act-nombre" placeholder="Ej: Taller de cruces de Punnett"></div>' +
    '<div class="campo"><label>Descripción</label><textarea id="act-descripcion" rows="2"></textarea></div>' +
    '<div class="campo"><label>Instrucciones para el estudiante</label>' + richTextEditorHTML('act-instrucciones','Escribe las instrucciones de la actividad...') + '</div>' +
    '<div class="grid-2"><div class="campo"><label>Tipo</label><select id="act-tipo"><option value="tarea">Tarea</option><option value="proyecto">Proyecto</option><option value="investigacion">Investigación</option><option value="ejercicio">Ejercicio</option><option value="otro">Otro</option></select></div>' +
    '<div class="campo"><label>Puntaje máximo</label><input type="number" id="act-puntaje" step="0.1"></div>' +
    '<div class="campo"><label>Disponible desde</label><input type="datetime-local" id="act-fecha-disp"></div>' +
    '<div class="campo"><label>Fecha límite</label><input type="datetime-local" id="act-fecha-limite"></div></div>' +
    '<label class="flex items-center gap-8 text-sm mb-14" style="cursor:pointer"><input type="checkbox" id="act-tardia" style="width:auto"> Permitir entregas tardías</label>' +
    '<div class="campo"><label>Adjuntar imagen o archivo de referencia (opcional)</label><p class="hint" style="margin:0 0 6px">Ej: una plantilla, un esquema o un documento de apoyo.</p>' +
    '<button type="button" class="btn btn-ghost btn-sm" onclick="subirArchivoActividad()">⬆ Subir imagen o archivo</button> <span class="text-sm muted">Puedes adjuntar varios · máx. 10 MB c/u</span><div id="act-archivo-label"></div></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-actividad\')">Cancelar</button><button class="btn btn-primary" onclick="guardarActividad()">Guardar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarActividad() {
  const id = document.getElementById('act-id').value;
  const nombre = document.getElementById('act-nombre').value.trim();
  const leccionId = window.ctx_act_leccion;
  if (!nombre) { toast('Escribe el nombre de la actividad', true); return; }
  if (!leccionId) { toast('Selecciona primero una lección', true); return; }
  const campos = { nombre, descripcion: document.getElementById('act-descripcion').value.trim(), instrucciones: rteGetValue('act-instrucciones'),
    tipo: document.getElementById('act-tipo').value,
    fechaDisponible: document.getElementById('act-fecha-disp').value, fechaLimite: document.getElementById('act-fecha-limite').value,
    puntajeMax: Number(document.getElementById('act-puntaje').value) || 5, permiteTardia: document.getElementById('act-tardia').checked,
    archivosAdjuntos: window._actArchivos || [] };
  if (id) {
    Object.assign(DATA.actividades.find(x=>x.id===id), campos);
    toast('Enunciado de "' + nombre + '" actualizado');
  } else {
    DATA.actividades.push({ id: uid('act'), leccionId, estado:'activa', entregas:[], ...campos });
    toast('Actividad "' + nombre + '" creada');
  }
  saveData(); cerrarModal('modal-actividad'); render();
}
/* ---- vista previa "como estudiante" de una actividad ---- */
function abrirModalActividadPreview(id) {
  const a = DATA.actividades.find(x=>x.id===id);
  document.getElementById('ap-nombre').textContent = a.nombre;
  document.getElementById('ap-nombre2').textContent = a.nombre;
  document.getElementById('ap-vence').textContent = fmtDate(a.fechaLimite);
  document.getElementById('ap-vence2').textContent = fmtDate(a.fechaLimite);
  document.getElementById('ap-puntaje').textContent = a.puntajeMax.toFixed(1);
  document.getElementById('ap-puntaje2').textContent = a.puntajeMax.toFixed(1) + ' pts';
  document.getElementById('ap-disponible').textContent = fmtDate(a.fechaDisponible);
  document.getElementById('ap-tipo').textContent = a.tipo;
  document.getElementById('ap-tardia').textContent = a.permiteTardia ? 'Permitida' : 'No permitida';
  document.getElementById('ap-instrucciones').innerHTML = a.instrucciones || '<span class="muted">El docente todavía no ha escrito instrucciones detalladas.</span>';
  document.getElementById('ap-archivo-wrap').style.display = (a.archivosAdjuntos && a.archivosAdjuntos.length) ? 'block' : 'none';
  document.getElementById('ap-archivo').innerHTML = a.archivosAdjuntos ? renderChipsArchivos(a.archivosAdjuntos) : '';
  abrirModal('modal-actividad-preview');
}
function renderModalActividadPreview() {
  if (document.getElementById('modal-actividad-preview')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-actividad-preview"><div class="modal grande"><div class="modal-header"><h3>Vista previa del estudiante</h3><button class="modal-close" onclick="cerrarModal(\'modal-actividad-preview\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div style="background:#F7F3EC;border-radius:14px;padding:24px;border:1px solid #EFE6DA">' +
    '<span style="display:inline-flex;align-items:center;gap:6px;background:var(--amber-soft);color:#8A611E;font-size:12.5px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:14px">✏️ Tarea</span>' +
    '<h2 style="font-family:\'Fraunces\',serif;font-size:23px;color:#2C4A42;margin:0 0 8px" id="ap-nombre"></h2>' +
    '<div class="flex items-center gap-14 flex-wrap" style="font-size:13px;color:#6B5B45;margin-bottom:18px">' +
    '<span>📅 Vence: <strong id="ap-vence"></strong></span><span>⭐ Valor: <strong id="ap-puntaje"></strong> puntos</span>' +
    '<span style="background:var(--clay-soft);color:var(--clay);font-weight:700;padding:3px 10px;border-radius:20px;font-size:12px">Pendiente de entrega</span></div>' +
    '<div style="background:linear-gradient(135deg,#B25943,#C08A3E);border-radius:14px;padding:20px 24px;margin-bottom:18px">' +
    '<div style="color:#fff;font-weight:700;font-size:17px;margin-bottom:4px">✏️ <span id="ap-nombre2"></span></div><div style="color:rgba(255,255,255,.85);font-size:13px" id="ap-tipo"></div></div>' +
    '<div class="flex gap-14 flex-wrap" style="font-size:13px;color:#6B5B45;margin-bottom:18px">' +
    '<span>📅 Disponible desde: <strong id="ap-disponible"></strong></span><span>⏰ Vence: <strong style="color:var(--clay)" id="ap-vence2"></strong></span><span>⭐ Puntaje: <strong id="ap-puntaje2"></strong></span></div>' +
    '<p style="font-size:13px;color:#6B5B45;margin-bottom:18px">📎 Entrega tardía: <strong id="ap-tardia"></strong></p>' +
    '<div style="background:#fff;border-radius:12px;padding:18px;border:1px solid #EFE6DA;margin-bottom:18px;border-left:4px solid var(--amber)">' +
    '<div style="font-size:12.5px;font-weight:700;color:#8A611E;text-transform:uppercase;letter-spacing:.3px;margin-bottom:8px">📋 Instrucciones</div>' +
    '<p style="font-size:13.5px;color:var(--ink);margin:0;white-space:pre-wrap" id="ap-instrucciones"></p>' +
    '<div id="ap-archivo-wrap" class="flex justify-between items-center" style="display:none;background:#F7F3EC;border-radius:10px;padding:10px 14px;margin-top:12px"><span style="font-size:13px" id="ap-archivo"></span><span style="font-size:12.5px;color:var(--blue);font-weight:700">Descargar →</span></div></div>' +
    '<div style="font-size:13px;font-weight:700;color:#2C4A42;margin-bottom:8px">📎 Adjuntar archivo de entrega</div>' +
    '<div style="border:2px dashed #E0D3BE;border-radius:12px;padding:34px 20px;text-align:center;margin-bottom:18px">' +
    '<div style="font-size:34px;margin-bottom:8px">📁</div><div style="font-weight:700;font-size:14px">Haz clic para seleccionar tu archivo</div>' +
    '<div style="font-size:12.5px;color:#8A6A4E;margin-top:4px">O arrastra el archivo sobre esta área</div>' +
    '<div style="font-size:11.5px;color:#8A6A4E;margin-top:4px">PDF, DOCX, PPTX, JPG, PNG · Máx. 10 MB</div></div>' +
    '<div style="font-size:13px;font-weight:700;color:#2C4A42;margin-bottom:8px">💬 Comentario para el docente (opcional)</div>' +
    '<textarea disabled placeholder="Agrega un mensaje o aclaración sobre tu entrega..." rows="2" style="margin-bottom:16px;background:#fff"></textarea>' +
    '<div class="flex justify-end gap-10">' +
    '<button disabled style="background:#fff;border:1px solid #E0D3BE;border-radius:9px;padding:9px 16px;font-weight:700;color:#8A6A4E;font-size:13.5px">🗑 Limpiar</button>' +
    '<button disabled style="background:var(--sage);border:none;border-radius:9px;padding:9px 16px;font-weight:700;color:#fff;font-size:13.5px;opacity:.85">📤 Entregar tarea</button></div>' +
    '</div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se verá para el estudiante. Los botones están desactivados porque estás viendo la vista previa como docente.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-actividad-preview\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
