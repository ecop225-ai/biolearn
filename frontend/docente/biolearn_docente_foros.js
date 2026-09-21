/* ================================================================
   BIOLearn — biolearn_docente_foros.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: FOROS
================================================================ */
let FORO_ACTIVO = null;
let FORO_RESPUESTA_ADJ = {}; // { comentarioId: [{tipo,label,url}] }

function renderForos(cont) {
  const leccionId = window.ctx_for_leccion || '';
  let html = '<div class="section-header"><div><h2>Foros</h2><p class="subtitle">Crea foros, responde individualmente a cada aporte, modera y califica la participación.</p></div>' +
    (leccionId ? '<button class="btn btn-primary" onclick="abrirModalForo()">+ Nuevo foro</button>' : '') + '</div>';
  html += contextPickerHTML('for');
  if (!leccionId) { html += '<div class="empty-state">Elige un curso y una lección para ver o crear foros.</div>'; cont.innerHTML = html; renderModalForo(); renderModalCalifForo(); return; }

  const items = DATA.foros.filter(f => f.leccionId === leccionId);
  if (items.length > 0 && !items.some(f=>f.id===FORO_ACTIVO)) FORO_ACTIVO = items[0].id;
  if (items.length === 0) FORO_ACTIVO = null;
  const foro = items.find(f => f.id === FORO_ACTIVO);

  html += '<div class="flex gap-14" style="align-items:flex-start">';
  html += '<div class="card" style="width:260px;flex-shrink:0;padding:0;overflow:hidden">';
  items.forEach(f => {
    const reportado = f.comentarios.some(c=>c.estado==='moderado');
    const activo = f.id === FORO_ACTIVO;
    html += '<div onclick="seleccionarForo(\''+f.id+'\')" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer' + (activo?';background:var(--blue-soft)':'') + '">' +
      '<div class="flex justify-between"><span style="font-size:13.5px;font-weight:700">' + esc(f.nombre) + '</span>' + (reportado?'<span style="color:var(--clay)">🚩</span>':'') + '</div>' +
      '<div class="text-sm muted">' + f.comentarios.filter(c=>!c.padreId).length + ' aportes</div></div>';
  });
  if (items.length === 0) html += '<div class="empty-state">Sin foros en esta lección.</div>';
  html += '</div>';

  html += '<div class="card" style="flex:1;min-width:0">';
  if (!foro) {
    html += '<div class="empty-state">Selecciona un foro de la lista.</div>';
  } else {
    const aportesRaiz = foro.comentarios.filter(c=>!c.padreId);
    const calificadosCount = foro.calificaciones.filter(c=>c.estado==='publicada').length;
    const participantes = [...new Set(aportesRaiz.map(c=>c.autor))];
    const sinRetro = Math.max(participantes.length - calificadosCount, 0);

    html += '<div class="flex justify-between items-center mb-8">' +
      '<span class="pill pill-azul">💬 Foro</span>' +
      '<button class="btn btn-ghost btn-xs" onclick="abrirForoStudentPreview(\''+foro.id+'\')">👁 Ver como estudiante</button></div>';
    html += '<h3 style="font-size:20px;margin-bottom:6px">' + esc(foro.nombre) + '</h3>';
    html += '<div class="flex gap-14 flex-wrap text-sm muted mb-14"><span>💬 ' + aportesRaiz.length + ' aportes</span><span>📅 Cierra: ' + (foro.fechaCierre||'—') + '</span>' + pill(foro.estado==='cerrado'?'cerrado':'activo') + '</div>';

    html += '<div class="flex gap-8 flex-wrap items-center" style="background:var(--bone);border-radius:12px;padding:14px;margin-bottom:16px">' +
      '<span style="font-size:11.5px;font-weight:700;color:var(--ink-soft);text-transform:uppercase">Gestión:</span>' +
      '<button class="btn btn-primary btn-sm" onclick="abrirModalForo()">+ Crear foro</button>' +
      '<button class="btn btn-amber btn-sm" onclick="toggleCerrarForo(\''+foro.id+'\')">' + (foro.estado==='cerrado'?'🔓 Abrir foro':'🔒 Cerrar foro') + '</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="abrirModalCalifGrupal(\''+foro.id+'\')">⭐ Calificar participación grupal</button></div>';

    html += '<div class="grid-3" style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:20px">' +
      '<div class="card" style="text-align:center;padding:16px 10px"><h2 style="font-size:26px">' + aportesRaiz.length + '</h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Aportes totales</div></div>' +
      '<div class="card" style="text-align:center;padding:16px 10px"><h2 style="font-size:26px;color:var(--sage)">' + calificadosCount + '</h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Calificados</div></div>' +
      '<div class="card" style="text-align:center;padding:16px 10px"><h2 style="font-size:26px;color:var(--clay)">' + sinRetro + '</h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Sin retroalimentar</div></div>' +
      '</div>';

    if (aportesRaiz.length === 0) html += '<div class="empty-state">Todavía no hay aportes en este foro.</div>';
    aportesRaiz.forEach(c => {
      const calif = foro.calificaciones.find(cf=>cf.estudiante===c.autor);
      const iniciales = c.autor.split(' ').map(p=>p[0]).slice(0,2).join('');
      const respuestas = foro.comentarios.filter(r=>r.padreId===c.id);
      const bajoMinimo = foro.minimoPalabras && c.palabras < foro.minimoPalabras;
      html += '<div style="background:' + (c.estado==='moderado'?'var(--clay-soft)':'var(--bone)') + ';border-radius:12px;padding:16px;margin-bottom:14px">';
      html += '<div class="flex justify-between items-start mb-8">' +
        '<div class="flex items-center gap-10"><div style="width:34px;height:34px;border-radius:50%;background:var(--sage-soft);color:var(--sage);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px">' + esc(iniciales) + '</div>' +
        '<div><div style="font-size:13.5px;font-weight:700">' + esc(c.autor) + ' <span style="font-weight:500;color:var(--ink-soft);font-size:12px">Estudiante</span></div>' +
        '<div class="text-sm muted" style="font-size:11.5px">' + esc(c.horaTexto) + ' · ' + esc(c.fecha) + ' · ' + respuestas.length + ' respuesta(s) · <strong>Aporte raíz</strong></div></div></div>' +
        (calif ? '<span class="pill pill-verde">✓ Calificada · ' + calif.calificacion.toFixed(1) + '</span>' : '<span class="pill pill-clay">Sin retroalimentar</span>') + '</div>';
      if (c.titulo) html += '<div style="font-weight:700;color:var(--sage);font-size:14.5px;margin-bottom:4px">' + esc(c.titulo) + '</div>';
      html += '<p class="text-sm" style="margin:0 0 10px">' + esc(c.contenido) + '</p>';
      if (c.adjuntos && c.adjuntos.length > 0) {
        html += '<div class="flex gap-8 flex-wrap mb-8">';
        c.adjuntos.forEach(a => { html += renderAdjuntoForo(a); });
        html += '</div>';
      }
      if (c.estado === 'moderado') html += '<div style="font-size:11.5px;color:var(--clay);font-weight:700;margin-bottom:10px">Reportado por la comunidad · pendiente de revisión</div>';

      html += '<div class="flex justify-between items-center flex-wrap gap-8">';
      html += '<div class="flex gap-8">';
      if (c.estado === 'moderado') {
        html += '<button class="btn btn-ghost btn-xs" onclick="moderarComentario(\''+foro.id+'\',\''+c.id+'\',\'aprobar\')">Aprobar</button>' +
                '<button class="btn btn-danger btn-xs" onclick="moderarComentario(\''+foro.id+'\',\''+c.id+'\',\'eliminar\')">🗑 Eliminar</button>';
      } else {
        html += '<button class="btn btn-ghost btn-xs" onclick="toggleResponderForo(\''+c.id+'\')">↩ Responder</button>' +
                '<button class="btn btn-primary btn-xs" onclick="abrirModalCalifForo(\''+foro.id+'\',\''+esc(c.autor).replace(/'/g,"\\'")+'\')">⭐ Calificar participación</button>';
      }
      html += '</div>';
      html += '<span class="text-sm muted">Palabras: ~' + (c.palabras||0) + (foro.minimoPalabras ? (bajoMinimo ? ' <span style="color:var(--clay);font-weight:700">⚠ Bajo el mínimo</span>' : ' <span style="color:var(--sage);font-weight:700">✓ Regla mínimo '+foro.minimoPalabras+'</span>') : '') + '</span>';
      html += '</div>';

      // caja de respuesta con adjuntos (imagen/enlace)
      html += '<div id="responder-box-'+c.id+'" style="display:none;margin-top:10px">' +
        '<textarea id="responder-txt-'+c.id+'" rows="2" placeholder="Responder a '+esc(c.autor)+'..." style="margin-bottom:6px"></textarea>' +
        '<div id="responder-adj-'+c.id+'" class="flex gap-8 flex-wrap mb-8"></div>' +
        '<div class="flex justify-between items-center">' +
        '<div class="flex gap-8"><button class="btn btn-ghost btn-xs" onclick="responderForoAdjImagen(\''+c.id+'\')">🖼 Imagen</button><button class="btn btn-ghost btn-xs" onclick="responderForoAdjEnlace(\''+c.id+'\')">🔗 Enlace</button><button class="btn btn-ghost btn-xs" onclick="responderForoAdjDocumento(\''+c.id+'\')">📄 Documento</button></div>' +
        '<button class="btn btn-primary btn-sm" onclick="enviarRespuestaForo(\''+foro.id+'\',\''+c.id+'\')">Enviar</button></div></div>';

      // respuestas anidadas (de compañeros y del docente)
      respuestas.forEach(r => {
        html += '<div style="margin-top:10px;background:' + (r.rol==='docente'?'var(--sage-soft)':'var(--panel)') + ';border-radius:10px;padding:10px 14px;border:1px solid var(--line)">' +
          '<div style="font-size:12px;font-weight:700;color:' + (r.rol==='docente'?'var(--sage)':'var(--ink)') + ';margin-bottom:2px">' +
          (r.rol==='docente' ? '✓ Retroalimentación enviada — ' : '') + esc(r.autor) + (r.rol==='docente'?'':' (estudiante)') + '</div>';
        html += '<p style="font-size:13px;margin:0 0 6px">' + esc(r.contenido) + '</p>';
        if (r.adjuntos && r.adjuntos.length > 0) {
          html += '<div class="flex gap-8 flex-wrap">';
          r.adjuntos.forEach(a => { html += renderAdjuntoForo(a); });
          html += '</div>';
        }
        html += '</div>';
      });

      if (calif) {
        html += '<div style="margin-top:10px;background:var(--sage-soft);border-radius:10px;padding:10px 14px">' +
          '<div style="font-size:12px;font-weight:700;color:var(--sage);margin-bottom:2px">Nota: ' + calif.calificacion.toFixed(1) + ' · Calidad: ' + esc(calif.calidadAportes||'—') + '</div>' +
          (calif.retro ? '<p style="font-size:12.5px;margin:0">' + esc(calif.retro) + '</p>' : '') + '</div>';
      }
      html += '</div>';
    });
  }
  html += '</div></div>';
  cont.innerHTML = html;
  renderModalForo(); renderModalCalifForo(); renderModalCalifGrupal(); renderModalForoPreview();
}
function seleccionarForo(id) { FORO_ACTIVO = id; render(); }
function toggleResponderForo(comId) {
  const box = document.getElementById('responder-box-'+comId);
  box.style.display = box.style.display === 'none' ? 'block' : 'none';
}
function responderForoAdjImagen(comId) {
  const input = document.createElement('input');
  input.type = 'file'; input.accept = 'image/*';
  input.onchange = function () {
    const file = input.files[0]; if (!file) return;
    const reader = new FileReader();
    reader.onload = function (e) {
      if (!FORO_RESPUESTA_ADJ[comId]) FORO_RESPUESTA_ADJ[comId] = [];
      FORO_RESPUESTA_ADJ[comId].push({ tipo:'imagen', label: file.name, url: e.target.result });
      renderRespuestaAdjChips(comId);
    };
    reader.readAsDataURL(file);
  };
  input.click();
}
function responderForoAdjEnlace(comId) {
  const url = prompt('Pega la URL del enlace:');
  if (!url) return;
  if (!FORO_RESPUESTA_ADJ[comId]) FORO_RESPUESTA_ADJ[comId] = [];
  FORO_RESPUESTA_ADJ[comId].push({ tipo:'enlace', label: url, url: url });
  renderRespuestaAdjChips(comId);
}
function responderForoAdjDocumento(comId) {
  seleccionarArchivos('.pdf,.doc,.docx,.ppt,.pptx', function (archivos) {
    if (!FORO_RESPUESTA_ADJ[comId]) FORO_RESPUESTA_ADJ[comId] = [];
    archivos.forEach(f => FORO_RESPUESTA_ADJ[comId].push({ tipo:'documento', label: f.nombre, url: f.dataUrl, tamañoMB: f.tamañoMB }));
    renderRespuestaAdjChips(comId);
  });
}
/* Muestra el adjunto: si es una imagen con datos reales, la incrusta y el
   estudiante/docente puede verla en grande con un clic; si no hay datos
   (por ejemplo, en los aportes de ejemplo), se muestra como chip. */
function renderAdjuntoForo(a) {
  if (a.tipo === 'imagen' && a.url) {
    return '<div style="margin:6px 0"><img src="'+a.url+'" alt="'+esc(a.label)+'" title="Clic para ver en grande" style="max-width:260px;max-height:220px;border-radius:10px;cursor:zoom-in;border:1px solid var(--line)" onclick="foroVerImagenGrande(this.src)"></div>';
  }
  if (a.tipo === 'enlace' && a.url) {
    const yt = a.url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/);
    const vimeo = a.url.match(/vimeo\.com\/(\d+)/);
    if (yt) return '<div style="margin:6px 0"><iframe src="https://www.youtube.com/embed/'+yt[1]+'" style="width:100%;max-width:360px;height:200px;border:0;border-radius:10px" allowfullscreen></iframe></div>';
    if (vimeo) return '<div style="margin:6px 0"><iframe src="https://player.vimeo.com/video/'+vimeo[1]+'" style="width:100%;max-width:360px;height:200px;border:0;border-radius:10px" allowfullscreen></iframe></div>';
    return '<a href="'+esc(a.url)+'" target="_blank" rel="noreferrer" style="display:inline-flex;align-items:center;gap:5px;background:var(--bone);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px;text-decoration:none;color:var(--ink)">🔗 '+esc(a.label)+'</a>';
  }
  if (a.tipo === 'documento' && a.url) {
    return '<a href="'+a.url+'" download="'+esc(a.label)+'" style="display:inline-flex;align-items:center;gap:8px;background:var(--bone);border:1px solid var(--line);border-radius:10px;padding:8px 12px;font-size:12.5px;text-decoration:none;color:var(--ink);margin:4px 0">📄 <span>'+esc(a.label)+(a.tamañoMB?' <span style="color:var(--ink-soft)">('+a.tamañoMB+' MB)</span>':'')+'</span></a>';
  }
  return '<span style="display:inline-flex;align-items:center;gap:5px;background:var(--bone);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px">' + (a.tipo==='imagen'?'🖼':a.tipo==='documento'?'📄':'🔗') + ' ' + esc(a.label) + '</span>';
}
function renderRespuestaAdjChips(comId) {
  const cont = document.getElementById('responder-adj-'+comId);
  if (!cont) return;
  cont.innerHTML = (FORO_RESPUESTA_ADJ[comId]||[]).map((a,i) =>
    '<span style="display:inline-flex;align-items:center;gap:5px;background:var(--panel);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px">' +
    (a.tipo==='imagen'?'🖼':'🔗') + ' ' + esc(a.label) + ' <button onclick="quitarRespuestaAdj(\''+comId+'\','+i+')" style="border:none;background:none;cursor:pointer;color:var(--clay)">✕</button></span>'
  ).join('');
}
function quitarRespuestaAdj(comId, i) { FORO_RESPUESTA_ADJ[comId].splice(i,1); renderRespuestaAdjChips(comId); }
function enviarRespuestaForo(foroId, comId) {
  const texto = document.getElementById('responder-txt-'+comId).value.trim();
  if (!texto) { toast('Escribe una respuesta antes de enviar', true); return; }
  const c = DATA.foros.find(f=>f.id===foroId).comentarios.find(x=>x.id===comId);
  DATA.foros.find(f=>f.id===foroId).comentarios.push({
    id: uid('com'), autor: DOCENTE.nombre, rol:'docente', titulo:null, contenido: texto, tipo:'retroalimentacion',
    padreId: comId, estado:'activo', fecha: new Date().toISOString().slice(0,10), horaTexto:'ahora',
    adjuntos: FORO_RESPUESTA_ADJ[comId] || [], likes:0, palabras:0 });
  delete FORO_RESPUESTA_ADJ[comId];
  saveData(); render(); toast('Respondiste a ' + c.autor + ' · recibió una notificación');
}
function moderarComentario(foroId, comId, accion) {
  const foro = DATA.foros.find(f=>f.id===foroId);
  if (accion === 'eliminar') { foro.comentarios = foro.comentarios.filter(x=>x.id!==comId); toast('Comentario eliminado por moderación · se notificó al autor'); }
  else { foro.comentarios.find(x=>x.id===comId).estado = 'activo'; toast('Comentario aprobado'); }
  saveData(); render();
}
function toggleCerrarForo(foroId) {
  const foro = DATA.foros.find(f=>f.id===foroId);
  foro.estado = foro.estado === 'cerrado' ? 'activo' : 'cerrado';
  saveData(); render(); toast(foro.estado==='cerrado' ? 'Foro "'+foro.nombre+'" cerrado · ya no se aceptan nuevos aportes' : 'Foro "'+foro.nombre+'" reabierto');
}
function abrirModalCalifForo(foroId, estudiante) {
  document.getElementById('cf-foro-id').value = foroId; document.getElementById('cf-estudiante').value = estudiante;
  document.getElementById('cf-estudiante-label').textContent = estudiante;
  const existente = DATA.foros.find(f=>f.id===foroId).calificaciones.find(c=>c.estudiante===estudiante);
  document.getElementById('cf-calidad').value = existente ? existente.calidadAportes : 'bueno';
  document.getElementById('cf-nota').value = existente ? existente.calificacion : '';
  document.getElementById('cf-retro').value = existente ? existente.retro : '';
  abrirModal('modal-calif-foro');
}
function renderModalCalifForo() {
  if (document.getElementById('modal-calif-foro')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-calif-foro"><div class="modal"><div class="modal-header"><h3>⭐ Calificar participación</h3><button class="modal-close" onclick="cerrarModal(\'modal-calif-foro\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="cf-foro-id"><input type="hidden" id="cf-estudiante">' +
    '<p style="font-weight:700" id="cf-estudiante-label"></p>' +
    '<div class="campo"><label>Calidad del aporte</label><select id="cf-calidad"><option value="excelente">Excelente</option><option value="bueno">Bueno</option><option value="regular">Regular</option><option value="insuficiente">Insuficiente</option></select></div>' +
    '<div class="campo"><label>Nota (0-5)</label><input type="number" id="cf-nota" min="0" max="5" step="0.1"></div>' +
    '<div class="campo"><label>Retroalimentación</label><textarea id="cf-retro" rows="2"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-calif-foro\')">Cancelar</button><button class="btn btn-primary" onclick="guardarCalifForo()">Publicar calificación</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarCalifForo() {
  const foroId = document.getElementById('cf-foro-id').value, estudiante = document.getElementById('cf-estudiante').value;
  const nota = document.getElementById('cf-nota').value;
  if (nota === '') { toast('Ingresa una nota', true); return; }
  const foro = DATA.foros.find(f=>f.id===foroId);
  const cantidadAportes = foro.comentarios.filter(c=>c.autor===estudiante).length;
  const nueva = { estudiante, cantidadAportes, calidadAportes: document.getElementById('cf-calidad').value, calificacion:Number(nota), retro: document.getElementById('cf-retro').value.trim(), estado:'publicada' };
  const existe = foro.calificaciones.some(c=>c.estudiante===estudiante);
  foro.calificaciones = existe ? foro.calificaciones.map(c=>c.estudiante===estudiante?nueva:c) : [...foro.calificaciones, nueva];
  saveData(); cerrarModal('modal-calif-foro'); render(); toast('Participación de ' + estudiante + ' calificada');
}
/* ---- calificación grupal ---- */
function abrirModalCalifGrupal(foroId) {
  const foro = DATA.foros.find(f=>f.id===foroId);
  const aportesRaiz = foro.comentarios.filter(c=>!c.padreId);
  const participantes = [...new Set(aportesRaiz.map(c=>c.autor))];
  document.getElementById('cg-foro-id').value = foroId;
  document.getElementById('cg-lista').innerHTML = participantes.map(est => {
    const existente = foro.calificaciones.find(c=>c.estudiante===est);
    return '<div class="flex justify-between items-center" style="padding:8px 0;border-bottom:1px solid var(--line)"><span style="font-size:13.5px;font-weight:600">'+esc(est)+'</span>' +
      '<input type="number" min="0" max="5" step="0.1" class="cg-nota" data-estudiante="'+esc(est)+'" value="'+(existente?existente.calificacion:'')+'" style="width:80px"></div>';
  }).join('');
  abrirModal('modal-calif-grupal');
}
function renderModalCalifGrupal() {
  if (document.getElementById('modal-calif-grupal')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-calif-grupal"><div class="modal"><div class="modal-header"><h3>⭐ Calificar participación grupal</h3><button class="modal-close" onclick="cerrarModal(\'modal-calif-grupal\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="cg-foro-id"><p class="text-sm muted">Asigna una nota rápida a cada estudiante que participó en este foro.</p><div id="cg-lista"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-calif-grupal\')">Cancelar</button><button class="btn btn-primary" onclick="guardarCalifGrupal()">Publicar todas</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarCalifGrupal() {
  const foroId = document.getElementById('cg-foro-id').value;
  const foro = DATA.foros.find(f=>f.id===foroId);
  document.querySelectorAll('.cg-nota').forEach(inp => {
    if (inp.value === '') return;
    const estudiante = inp.dataset.estudiante, nota = Number(inp.value);
    const cantidadAportes = foro.comentarios.filter(c=>c.autor===estudiante).length;
    const nueva = { estudiante, cantidadAportes, calidadAportes:'', calificacion: nota, retro:'', estado:'publicada' };
    const existe = foro.calificaciones.some(c=>c.estudiante===estudiante);
    foro.calificaciones = existe ? foro.calificaciones.map(c=>c.estudiante===estudiante?nueva:c) : [...foro.calificaciones, nueva];
  });
  saveData(); cerrarModal('modal-calif-grupal'); render(); toast('Calificaciones grupales publicadas');
}
/* ---- vista previa "como estudiante" del foro ---- */
const FORO_PARTICIPANTE_LABEL = { abierto:'Abierto a todos', solo_estudiantes:'Solo estudiantes', moderado:'Moderado' };
function abrirForoStudentPreview(foroId) {
  const foro = DATA.foros.find(f=>f.id===foroId);
  const leccion = DATA.leccionesById[foro.leccionId];
  const curso = leccion ? DATA.cursos.find(c=>c.id===leccion.cursoId) : null;
  const aportes = foro.comentarios.filter(c=>!c.padreId);

  document.getElementById('fp-nombre').textContent = foro.nombre;
  document.getElementById('fp-meta').innerHTML = '<span>💬 ' + aportes.length + ' aportes</span><span>📅 Cierra el ' + (foro.fechaCierre||'—') + '</span>' +
    '<span>👥 Participantes: ' + (FORO_PARTICIPANTE_LABEL[foro.tipoParticipante] || foro.tipoParticipante) + '</span>' +
    '<span style="background:var(--sage-soft);color:var(--sage);font-weight:700;padding:3px 10px;border-radius:20px;font-size:12px">' + (foro.estado==='cerrado'?'Cerrado':'Abierto') + '</span>';
  document.getElementById('fp-banner-titulo').textContent = '💬 Foro: Lección ' + (leccion?leccion.orden:'—') + ' — ' + (leccion?leccion.nombre:foro.nombre);
  document.getElementById('fp-banner-sub').textContent = 'Doc. ' + DOCENTE.nombre + ' · ' + (curso?curso.nombre:'');
  document.getElementById('fp-tema').textContent = foro.tema || '';
  document.getElementById('fp-instrucciones').style.display = foro.instrucciones ? 'block' : 'none';
  document.getElementById('fp-instrucciones').textContent = foro.instrucciones || '';
  document.getElementById('fp-archivo-wrap').style.display = (foro.archivosAdjuntos && foro.archivosAdjuntos.length) ? 'block' : 'none';
  document.getElementById('fp-archivo').innerHTML = foro.archivosAdjuntos ? renderChipsArchivos(foro.archivosAdjuntos) : '';
  document.getElementById('fp-composer-textarea').placeholder = 'Escribe tu aporte aquí... (mínimo ' + (foro.minimoPalabras||0) + ' palabras)';

  const reglasWrap = document.getElementById('fp-reglas-wrap');
  if (foro.reglasParticipacion && foro.reglasParticipacion.length > 0) {
    reglasWrap.style.display = 'block';
    document.getElementById('fp-reglas').innerHTML = foro.reglasParticipacion.map(r=>'<li>'+esc(r)+'</li>').join('');
  } else reglasWrap.style.display = 'none';

  document.getElementById('fp-aportes-titulo').textContent = 'Aportes del foro (' + aportes.length + ')';
  let aportesHtml = '';
  aportes.forEach(c => {
    const iniciales = c.autor.split(' ').map(p=>p[0]).slice(0,2).join('');
    const esYo = c.autor === DOCENTE.nombre;
    aportesHtml += '<div style="background:#fff;border-radius:10px;padding:16px;border-left:3px solid var(--blue);border:1px solid #EFE6DA;margin-bottom:14px">' +
      '<div class="flex items-center gap-10 mb-8"><div style="width:32px;height:32px;border-radius:50%;background:var(--sage-soft);color:var(--sage);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12.5px">'+esc(iniciales)+'</div>' +
      '<div><div style="font-size:13.5px;font-weight:700;display:flex;align-items:center;gap:6px">' + esc(c.autor) + (esYo?' <span style="font-size:11px;background:var(--blue-soft);color:var(--blue);padding:1px 8px;border-radius:12px">Tú</span>':'') + '</div>' +
      '<div style="font-size:11.5px;color:var(--ink-soft)">' + esc(c.horaTexto) + ' · ' + esc(c.fecha) + '</div></div></div>';
    if (c.titulo) aportesHtml += '<div style="font-weight:700;color:var(--sage);font-size:14px;margin-bottom:4px">' + esc(c.titulo) + '</div>';
    aportesHtml += '<p style="font-size:13.5px;color:var(--ink);margin:0 0 10px">' + esc(c.contenido) + '</p>';
    if (c.adjuntos && c.adjuntos.length > 0) {
      aportesHtml += '<div class="flex gap-8 flex-wrap mb-10">';
      c.adjuntos.forEach(a => { aportesHtml += renderAdjuntoForo(a); });
      aportesHtml += '</div>';
    }
    aportesHtml += '<div class="flex items-center gap-14" style="font-size:12.5px;color:var(--ink-soft)"><span class="flex items-center gap-4">👍 ' + (c.likes||0) + '</span><span style="font-weight:700;color:var(--blue)">Responder</span></div></div>';
  });
  if (aportes.length === 0) aportesHtml = '<div class="empty-state">Todavía no hay aportes en este foro.</div>';
  document.getElementById('fp-aportes').innerHTML = aportesHtml;

  abrirModal('modal-foro-preview');
}
function renderModalForoPreview() {
  if (document.getElementById('modal-foro-preview')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-foro-preview"><div class="modal grande"><div class="modal-header"><h3>Vista previa del estudiante</h3><button class="modal-close" onclick="cerrarModal(\'modal-foro-preview\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div style="background:#F7F3EC;border-radius:14px;padding:24px;border:1px solid #EFE6DA">' +
    '<span style="display:inline-flex;align-items:center;gap:6px;background:var(--blue-soft);color:var(--blue);font-size:12.5px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:14px">💬 Foro de discusión</span>' +
    '<h2 style="font-family:\'Fraunces\',serif;font-size:23px;color:#2C4A42;margin:0 0 8px" id="fp-nombre"></h2>' +
    '<div class="flex items-center gap-14 flex-wrap" style="font-size:13px;color:#6B5B45;margin-bottom:18px" id="fp-meta"></div>' +
    '<div style="background:linear-gradient(135deg,#1E3345,#2E5588);border-radius:14px;padding:20px 24px;margin-bottom:18px">' +
    '<div style="color:#fff;font-weight:700;font-size:17px;margin-bottom:4px" id="fp-banner-titulo"></div><div style="color:rgba(255,255,255,.85);font-size:13px" id="fp-banner-sub"></div></div>' +
    '<div style="background:var(--blue-soft);border-radius:12px;padding:18px;margin-bottom:14px;border-left:4px solid var(--blue)">' +
    '<div style="font-size:12.5px;font-weight:700;color:var(--blue);text-transform:uppercase;letter-spacing:.3px;margin-bottom:8px">📌 Tema del foro</div>' +
    '<p style="font-size:14px;font-weight:700;color:var(--ink);margin:0 0 6px" id="fp-tema"></p>' +
    '<p style="font-size:13px;color:var(--ink-soft);margin:0" id="fp-instrucciones"></p>' +
    '<div id="fp-archivo-wrap" style="display:none;align-items:center;gap:6px;background:#fff;border:1px solid var(--line);border-radius:20px;padding:5px 12px;font-size:12px;margin-top:10px">🖼 <span id="fp-archivo"></span></div></div>' +
    '<div id="fp-reglas-wrap" class="card" style="border-left:4px solid var(--amber);margin-bottom:20px">' +
    '<div style="font-size:12.5px;font-weight:700;color:#8A611E;text-transform:uppercase;letter-spacing:.3px;margin-bottom:8px">📋 Reglas de participación</div>' +
    '<ul id="fp-reglas" style="margin:0;padding-left:20px;font-size:13.5px;color:var(--ink);line-height:1.8"></ul></div>' +
    '<div style="font-size:12.5px;font-weight:700;color:#2C4A42;text-transform:uppercase;letter-spacing:.3px;margin-bottom:10px">Tu aporte</div>' +
    '<div style="background:#fff;border-radius:12px;padding:16px;border:1px solid #EFE6DA;margin-bottom:20px">' +
    '<input disabled placeholder="Título de tu aporte (opcional)..." style="background:var(--bone);margin-bottom:10px">' +
    '<textarea disabled id="fp-composer-textarea" rows="3" style="background:var(--bone);margin-bottom:10px"></textarea>' +
    '<div class="flex justify-between flex-wrap gap-10"><div class="flex gap-8">' +
    '<button disabled style="background:var(--bone);border:1px solid var(--line);border-radius:8px;padding:8px 14px;font-size:12.5px;font-weight:700;color:var(--ink-soft)">🖼 Imagen</button>' +
    '<button disabled style="background:var(--bone);border:1px solid var(--line);border-radius:8px;padding:8px 14px;font-size:12.5px;font-weight:700;color:var(--ink-soft)">🔗 Enlace</button></div>' +
    '<button disabled style="background:var(--blue-deep);opacity:.85;border:none;border-radius:9px;padding:10px 18px;font-weight:700;color:#fff;font-size:13.5px">📤 Publicar aporte</button></div></div>' +
    '<div style="font-size:12.5px;font-weight:700;color:#2C4A42;text-transform:uppercase;letter-spacing:.3px;margin-bottom:12px" id="fp-aportes-titulo"></div>' +
    '<div id="fp-aportes"></div>' +
    '</div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se ve para el estudiante. El compositor y los botones están desactivados en esta vista previa.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-foro-preview\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function abrirModalForo() {
  document.getElementById('foro-nombre').value = ''; document.getElementById('foro-tema').value = '';
  document.getElementById('foro-instrucciones').value = ''; document.getElementById('foro-min-palabras').value = 100;
  document.getElementById('foro-tipo-part').value = 'abierto'; document.getElementById('foro-cierre').value = '';
  document.getElementById('foro-archivo-label').innerHTML = ''; window._foroArchivos = [];
  abrirModal('modal-foro');
}
function subirArchivoForo() {
  seleccionarArchivos(null, function (archivos) {
    window._foroArchivos = (window._foroArchivos || []).concat(archivos);
    document.getElementById('foro-archivo-label').innerHTML = renderChipsArchivos(window._foroArchivos);
  });
}
function renderModalForo() {
  if (document.getElementById('modal-foro')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-foro"><div class="modal"><div class="modal-header"><h3>💬 Nuevo foro</h3><button class="modal-close" onclick="cerrarModal(\'modal-foro\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div class="campo"><label>Nombre del foro</label><input type="text" id="foro-nombre" placeholder="Ej: ¿Qué pasaría si...?"></div>' +
    '<div class="campo"><label>Tema</label><input type="text" id="foro-tema"></div>' +
    '<div class="campo"><label>Instrucciones</label><textarea id="foro-instrucciones" rows="2"></textarea></div>' +
    '<div class="grid-2"><div class="campo"><label>Tipo de participantes</label><select id="foro-tipo-part"><option value="abierto">Abierto</option><option value="solo_estudiantes">Solo estudiantes</option><option value="moderado">Moderado</option></select></div>' +
    '<div class="campo"><label>Mínimo de palabras por aporte</label><input type="number" id="foro-min-palabras"></div></div>' +
    '<div class="campo"><label>Fecha de cierre</label><input type="date" id="foro-cierre"></div>' +
    '<div class="campo"><label>Adjuntar imagen o archivo de apoyo (opcional)</label><p class="hint" style="margin:0 0 6px">Ej: un esquema, caso de estudio o documento que acompañe el tema del foro.</p>' +
    '<button type="button" class="btn btn-ghost btn-sm" onclick="subirArchivoForo()">🖼 Subir imagen o archivo</button> <span class="text-sm muted">Puedes adjuntar varios · máx. 10 MB c/u</span><div id="foro-archivo-label"></div></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-foro\')">Cancelar</button><button class="btn btn-primary" onclick="guardarForo()">Crear foro</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarForo() {
  const nombre = document.getElementById('foro-nombre').value.trim();
  const leccionId = window.ctx_for_leccion;
  if (!nombre) { toast('Escribe el nombre del foro', true); return; }
  if (!leccionId) { toast('Selecciona primero una lección', true); return; }
  const nuevo = { id: uid('foro'), leccionId, nombre, tema: document.getElementById('foro-tema').value.trim(),
    instrucciones: document.getElementById('foro-instrucciones').value.trim(), reglasParticipacion: [],
    minimoPalabras: Number(document.getElementById('foro-min-palabras').value)||100, tipoParticipante: document.getElementById('foro-tipo-part').value,
    estado:'activo', fechaCierre: document.getElementById('foro-cierre').value, archivosAdjuntos: window._foroArchivos||[], comentarios:[], calificaciones:[] };
  DATA.foros.push(nuevo);
  FORO_ACTIVO = nuevo.id;
  saveData(); cerrarModal('modal-foro'); render(); toast('Foro "' + nombre + '" creado · los estudiantes fueron notificados');
}
