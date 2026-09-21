/* ================================================================
   BIOLearn — biolearn_estudiante_foros.js
================================================================ */
let FORO_RESPONDER_ABIERTO = {};
let FORO_ACTIVO_GLOBAL = null;

let FORO_APORTE_ADJ = {}; // { foroId: [{tipo,label,url}] }
let FORO_RESPUESTA_ADJ_EST = {}; // { comentarioId: [...] }
let MIS_LIKES = {}; // { comentarioId: true }

function renderAdjuntoEst(a) {
  if (a.tipo === 'imagen' && a.url) return '<div style="margin:6px 0"><img src="'+a.url+'" style="max-width:220px;max-height:180px;border-radius:10px;cursor:zoom-in;border:1px solid var(--line)" onclick="verImagenGrande(this.src)"></div>';
  if (a.tipo === 'documento' && a.url) return '<a href="'+a.url+'" download="'+esc(a.label)+'" style="display:inline-flex;align-items:center;gap:6px;background:var(--bone);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px;text-decoration:none;color:var(--ink)">📄 '+esc(a.label)+'</a>';
  if (a.tipo === 'enlace' && a.url) return '<a href="'+esc(a.url)+'" target="_blank" rel="noreferrer" style="display:inline-flex;align-items:center;gap:5px;background:var(--bone);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px;text-decoration:none;color:var(--ink)">🔗 '+esc(a.label)+'</a>';
  return '<span style="display:inline-flex;align-items:center;gap:5px;background:var(--bone);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px">' + (a.tipo==='imagen'?'🖼':a.tipo==='documento'?'📄':'🔗') + ' ' + esc(a.label) + '</span>';
}
function adjuntarImagenEst(store, key) {
  seleccionarArchivos('image/*', function (archivos) {
    if (archivos.length === 0) return;
    if (!store[key]) store[key] = [];
    store[key].push({ tipo:'imagen', label: archivos[0].nombre, url: archivos[0].dataUrl });
    render();
  });
}
function adjuntarDocumentoEst(store, key) {
  seleccionarArchivos('.pdf,.doc,.docx,.ppt,.pptx', function (archivos) {
    if (archivos.length === 0) return;
    if (!store[key]) store[key] = [];
    store[key].push({ tipo:'documento', label: archivos[0].nombre, url: archivos[0].dataUrl });
    render();
  });
}
function adjuntarEnlaceEst(store, key) {
  const url = prompt('Pega la URL del enlace:');
  if (!url) return;
  if (!store[key]) store[key] = [];
  store[key].push({ tipo:'enlace', label:url, url });
  render();
}
function quitarAdjuntoEst(store, key, i) { store[key].splice(i,1); render(); }
function chipsAdjuntoTmpHTML(store, key) {
  const lista = store[key] || [];
  if (lista.length === 0) return '';
  return '<div class="flex gap-8 flex-wrap" style="margin:8px 0">' + lista.map((a,i) =>
    '<span style="display:inline-flex;align-items:center;gap:5px;background:#fff;border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px">' + (a.tipo==='imagen'?'🖼':a.tipo==='documento'?'📄':'🔗') + ' ' + esc(a.label) +
    ' <span onclick="quitarAdjuntoEst(window.'+(store===FORO_APORTE_ADJ?'FORO_APORTE_ADJ':'FORO_RESPUESTA_ADJ_EST')+',\''+key+'\','+i+')" style="cursor:pointer;color:var(--clay)">✕</span></span>'
  ).join('') + '</div>';
}
function toggleLikeEst(comId, foroId) {
  const foro = DATA.foros.find(f=>f.id===foroId);
  const c = foro.comentarios.find(x=>x.id===comId);
  if (MIS_LIKES[comId]) { c.likes = Math.max(0,(c.likes||0)-1); MIS_LIKES[comId] = false; }
  else { c.likes = (c.likes||0)+1; MIS_LIKES[comId] = true; }
  saveData(); render();
}

function foroViewHTML(foro, leccion, curso) {
  const aportes = foro.comentarios.filter(c=>!c.padreId);
  let html = '<div class="card">' +
    '<span style="display:inline-flex;align-items:center;gap:6px;background:var(--blue-soft);color:var(--blue-deep);font-size:12px;font-weight:700;padding:4px 10px;border-radius:20px;margin-bottom:10px">💬 Foro de discusión</span>' +
    '<h3 style="font-size:19px;margin:0 0 10px">' + esc(foro.nombre) + '</h3>';
  if (leccion) html += '<div style="background:linear-gradient(135deg,#1E3345,#2E5588);border-radius:12px;padding:16px 18px;margin-bottom:14px">' +
    '<div style="color:#fff;font-weight:700;font-size:14.5px">💬 Foro: Lección ' + leccion.orden + ' — ' + esc(leccion.nombre) + '</div>' +
    '<div style="color:rgba(255,255,255,.8);font-size:12px">Doc. ' + esc(curso?curso.docente:'') + ' · ' + esc(curso?curso.nombre:'') + '</div></div>';
  html += '<div style="background:var(--blue-soft);border-radius:10px;padding:14px;margin-bottom:12px;border-left:4px solid var(--blue)">' +
    '<div class="text-sm" style="font-weight:700;color:var(--blue-deep);text-transform:uppercase;margin-bottom:6px">📌 Tema del foro</div>' +
    '<p class="text-sm" style="font-weight:700;margin:0 0 4px">' + esc(foro.tema) + '</p>' + (foro.instrucciones?'<p class="text-sm muted" style="margin:0">'+esc(foro.instrucciones)+'</p>':'') + '</div>';
  if (foro.reglasParticipacion) html += '<div style="background:var(--amber-soft);border-radius:10px;padding:14px;margin-bottom:16px">' +
    '<div class="text-sm" style="font-weight:700;color:#8A611E;text-transform:uppercase;margin-bottom:6px">📋 Reglas de participación</div>' +
    '<ul style="margin:0;padding-left:18px;font-size:12.5px;line-height:1.7">' + foro.reglasParticipacion.map(r=>'<li>'+esc(r)+'</li>').join('') + '</ul></div>';

  html += '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:8px">Tu aporte</div>' +
    '<div style="background:var(--bone);border-radius:10px;padding:14px;margin-bottom:18px">' +
    '<input id="foro-titulo-'+foro.id+'" placeholder="Título de tu aporte (opcional)..." style="background:#fff;margin-bottom:8px">' +
    '<textarea id="foro-texto-'+foro.id+'" oninput="actualizarContadorPalabras(\''+foro.id+'\')" placeholder="Escribe tu aporte aquí... (mínimo '+(foro.minimoPalabras||0)+' palabras)" rows="3" style="background:#fff;margin-bottom:8px"></textarea>' +
    chipsAdjuntoTmpHTML(FORO_APORTE_ADJ, foro.id) +
    '<div class="flex justify-between items-center flex-wrap gap-10"><div class="flex gap-8">' +
    '<button class="btn btn-ghost btn-sm" onclick="adjuntarImagenEst(FORO_APORTE_ADJ,\''+foro.id+'\')">🖼 Imagen</button>' +
    '<button class="btn btn-ghost btn-sm" onclick="adjuntarEnlaceEst(FORO_APORTE_ADJ,\''+foro.id+'\')">🔗 Enlace</button>' +
    '<button class="btn btn-ghost btn-sm" onclick="adjuntarDocumentoEst(FORO_APORTE_ADJ,\''+foro.id+'\')">📄 Documento</button></div>' +
    '<span class="text-sm muted" id="foro-contador-'+foro.id+'">Palabras: ~0</span></div>' +
    '<div class="flex justify-end" style="margin-top:10px"><button class="btn btn-primary" onclick="publicarAporteEst(\''+foro.id+'\')">Publicar aporte</button></div></div>';

  html += '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:10px">Aportes del foro (' + aportes.length + ')</div>';
  aportes.forEach(c => {
    const iniciales = c.autor.split(' ').map(p=>p[0]).slice(0,2).join('');
    const esYo = c.autor === ESTUDIANTE.nombre;
    html += '<div style="background:var(--panel);border-radius:10px;padding:14px;border:1px solid var(--line);border-left:3px solid var(--blue);margin-bottom:12px">' +
      '<div class="flex items-center gap-10 mb-8"><div style="width:30px;height:30px;border-radius:50%;background:var(--sage-soft);color:var(--sage);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12px">' + esc(iniciales) + '</div>' +
      '<div><div style="font-size:13px;font-weight:700;display:flex;align-items:center;gap:6px">' + esc(c.autor) + (esYo?' <span style="font-size:10.5px;background:var(--blue-soft);color:var(--blue-deep);padding:1px 8px;border-radius:12px">Tú</span>':'') + '</div>' +
      '<div class="text-sm muted" style="font-size:11px">' + esc(c.horaTexto) + ' · ' + esc(c.fecha) + '</div></div></div>';
    if (c.titulo) html += '<div style="font-weight:700;color:var(--sage);font-size:13.5px;margin-bottom:4px">' + esc(c.titulo) + '</div>';
    html += '<p class="text-sm" style="margin:0 0 8px">' + esc(c.contenido) + '</p>';
    if (c.adjuntos && c.adjuntos.length > 0) { html += '<div class="flex gap-8 flex-wrap mb-8">'; c.adjuntos.forEach(a=>html+=renderAdjuntoEst(a)); html += '</div>'; }
    html += '<div class="flex items-center gap-14 text-sm muted mb-8">' +
      '<span onclick="toggleLikeEst(\''+c.id+'\',\''+foro.id+'\')" style="cursor:pointer;font-weight:'+(MIS_LIKES[c.id]?'700':'400')+';color:'+(MIS_LIKES[c.id]?'var(--amber)':'var(--ink-soft)')+'">' + (MIS_LIKES[c.id]?'👍':'👍🏻') + ' ' + (c.likes||0) + '</span>' +
      '<span style="font-weight:700;color:var(--blue);cursor:pointer" onclick="toggleResponderEst(\''+c.id+'\')">Responder</span></div>';
    foro.comentarios.filter(r=>r.padreId===c.id).forEach(r => {
      html += '<div style="margin-left:20px;margin-top:8px;background:'+(r.rol==='docente'?'var(--amber-soft)':'var(--bone)')+';border-radius:8px;padding:8px 12px">' +
        '<div class="text-sm" style="font-weight:700;margin-bottom:2px;color:'+(r.rol==='docente'?'#8A611E':'var(--ink)')+'">' + esc(r.autor) + (r.rol==='docente'?' · Docente':'') + '</div>' +
        '<p class="text-sm" style="margin:0 0 6px">' + esc(r.contenido) + '</p>';
      if (r.adjuntos && r.adjuntos.length > 0) { html += '<div class="flex gap-8 flex-wrap">'; r.adjuntos.forEach(a=>html+=renderAdjuntoEst(a)); html += '</div>'; }
      html += '</div>';
    });
    if (FORO_RESPONDER_ABIERTO[c.id]) {
      html += '<div style="margin-top:8px;margin-left:20px">' +
        '<input id="foro-resp-'+c.id+'" placeholder="Responder a '+esc(c.autor)+'..." style="width:100%;font-size:12.5px;margin-bottom:6px">' +
        chipsAdjuntoTmpHTML(FORO_RESPUESTA_ADJ_EST, c.id) +
        '<div class="flex justify-between items-center"><div class="flex gap-6">' +
        '<button class="btn btn-ghost btn-sm" onclick="adjuntarImagenEst(FORO_RESPUESTA_ADJ_EST,\''+c.id+'\')">🖼</button>' +
        '<button class="btn btn-ghost btn-sm" onclick="adjuntarEnlaceEst(FORO_RESPUESTA_ADJ_EST,\''+c.id+'\')">🔗</button>' +
        '<button class="btn btn-ghost btn-sm" onclick="adjuntarDocumentoEst(FORO_RESPUESTA_ADJ_EST,\''+c.id+'\')">📄</button></div>' +
        '<button class="btn btn-primary btn-sm" onclick="enviarRespuestaAporteEst(\''+foro.id+'\',\''+c.id+'\')">Enviar</button></div></div>';
    }
    html += '</div>';
  });
  html += '</div>';
  return html;
}
function actualizarContadorPalabras(foroId) {
  const texto = document.getElementById('foro-texto-'+foroId).value.trim();
  document.getElementById('foro-contador-'+foroId).textContent = 'Palabras: ~' + (texto ? texto.split(/\s+/).length : 0);
}
function toggleResponderEst(comId) { FORO_RESPONDER_ABIERTO[comId] = !FORO_RESPONDER_ABIERTO[comId]; render(); }
function publicarAporteEst(foroId) {
  const tituloEl = document.getElementById('foro-titulo-'+foroId), textoEl = document.getElementById('foro-texto-'+foroId);
  const texto = textoEl.value.trim();
  if (!texto) return;
  const palabras = texto.split(/\s+/).length;
  const foro = DATA.foros.find(f=>f.id===foroId);
  foro.comentarios.push({ id:uid('com'), autor:ESTUDIANTE.nombre, rol:'estudiante', titulo: tituloEl.value.trim()||null, contenido:texto, padreId:null, fecha:new Date().toISOString().slice(0,10), horaTexto:'ahora', adjuntos: FORO_APORTE_ADJ[foroId]||[], likes:0, palabras });
  delete FORO_APORTE_ADJ[foroId];
  saveData(); render();
  toast('Aporte publicado en el foro');
}
function enviarRespuestaAporteEst(foroId, padreId) {
  const input = document.getElementById('foro-resp-'+padreId);
  const contenido = input.value.trim();
  if (!contenido) return;
  const foro = DATA.foros.find(f=>f.id===foroId);
  foro.comentarios.push({ id:uid('com'), autor:ESTUDIANTE.nombre, rol:'estudiante', titulo:null, contenido, padreId, fecha:new Date().toISOString().slice(0,10), horaTexto:'ahora', adjuntos: FORO_RESPUESTA_ADJ_EST[padreId]||[], likes:0, palabras:contenido.split(/\s+/).length });
  delete FORO_RESPUESTA_ADJ_EST[padreId];
  FORO_RESPONDER_ABIERTO[padreId] = false;
  saveData(); render();
  toast('Respuesta publicada');
}

function renderForos(cont) {
  if (FORO_ACTIVO_GLOBAL === null && DATA.foros.length > 0) FORO_ACTIVO_GLOBAL = DATA.foros[0].id;
  const foro = DATA.foros.find(f=>f.id===FORO_ACTIVO_GLOBAL);
  let html = '<div class="section-header"><div><h2>Foros</h2><p class="subtitle">Participa en las discusiones de tus cursos</p></div></div>';
  html += '<div class="flex gap-14" style="align-items:flex-start">';
  html += '<div class="card" style="width:260px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.foros.forEach(f => {
    html += '<div onclick="FORO_ACTIVO_GLOBAL=\''+f.id+'\';render()" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:'+(FORO_ACTIVO_GLOBAL===f.id?'var(--blue-soft)':'transparent')+'">' +
      '<div style="font-size:13.5px;font-weight:700">' + esc(f.nombre) + '</div><div class="text-sm muted">' + f.comentarios.filter(c=>!c.padreId).length + ' aportes</div></div>';
  });
  html += '</div><div style="flex:1;min-width:0">';
  if (foro) {
    const leccion = DATA.leccionesById[foro.leccionId];
    const curso = leccion ? DATA.cursos.find(c=>c.id===leccion.cursoId) : null;
    html += foroViewHTML(foro, leccion, curso);
  } else html += '<div class="empty-state">Selecciona un foro</div>';
  html += '</div></div>';
  cont.innerHTML = html;
}
