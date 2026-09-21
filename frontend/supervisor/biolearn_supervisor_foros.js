/* ================================================================
   BIOLearn — biolearn_supervisor_foros.js
================================================================ */
let FORO_ACTIVO_SUP = null;
let FORO_RESPUESTA_ADJ_SUP = {}; // { comentarioId: [{tipo,label,url}] }

function renderAdjuntoSup(a) {
  if (a.tipo === 'imagen' && a.url) return '<div style="margin:6px 0"><img src="'+a.url+'" style="max-width:220px;max-height:180px;border-radius:10px;cursor:zoom-in;border:1px solid var(--line)" onclick="verImagenGrande(this.src)"></div>';
  if (a.tipo === 'documento' && a.url) return '<a href="'+a.url+'" download="'+esc(a.label)+'" style="display:inline-flex;align-items:center;gap:6px;background:var(--panel);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px;text-decoration:none;color:var(--ink)">📄 '+esc(a.label)+'</a>';
  if (a.tipo === 'enlace' && a.url) return '<a href="'+esc(a.url)+'" target="_blank" rel="noreferrer" style="display:inline-flex;align-items:center;gap:5px;background:var(--panel);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px;text-decoration:none;color:var(--ink)">🔗 '+esc(a.label)+'</a>';
  return '<span style="display:inline-flex;align-items:center;gap:5px;background:var(--panel);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px">' + (a.tipo==='imagen'?'🖼':a.tipo==='documento'?'📄':'🔗') + ' ' + esc(a.label) + '</span>';
}
function adjuntarImagenSup(comId) { seleccionarArchivos('image/*', function(archivos){ if(!archivos.length) return; if(!FORO_RESPUESTA_ADJ_SUP[comId]) FORO_RESPUESTA_ADJ_SUP[comId]=[]; FORO_RESPUESTA_ADJ_SUP[comId].push({tipo:'imagen',label:archivos[0].nombre,url:archivos[0].dataUrl}); render(); }); }
function adjuntarDocumentoSup(comId) { seleccionarArchivos('.pdf,.doc,.docx,.ppt,.pptx', function(archivos){ if(!archivos.length) return; if(!FORO_RESPUESTA_ADJ_SUP[comId]) FORO_RESPUESTA_ADJ_SUP[comId]=[]; FORO_RESPUESTA_ADJ_SUP[comId].push({tipo:'documento',label:archivos[0].nombre,url:archivos[0].dataUrl}); render(); }); }
function adjuntarEnlaceSup(comId) { const url = prompt('Pega la URL del enlace:'); if(!url) return; if(!FORO_RESPUESTA_ADJ_SUP[comId]) FORO_RESPUESTA_ADJ_SUP[comId]=[]; FORO_RESPUESTA_ADJ_SUP[comId].push({tipo:'enlace',label:url,url}); render(); }
function quitarAdjuntoSup(comId, i) { FORO_RESPUESTA_ADJ_SUP[comId].splice(i,1); render(); }
function chipsAdjuntoRepHTML(comId) {
  const lista = FORO_RESPUESTA_ADJ_SUP[comId] || [];
  if (lista.length === 0) return '';
  return '<div class="flex gap-8 flex-wrap" style="margin:8px 0 0 20px">' + lista.map((a,i) =>
    '<span style="display:inline-flex;align-items:center;gap:5px;background:var(--panel);border:1px solid var(--line);border-radius:20px;padding:4px 10px;font-size:11.5px">' + (a.tipo==='imagen'?'🖼':a.tipo==='documento'?'📄':'🔗') + ' ' + esc(a.label) +
    ' <span onclick="quitarAdjuntoSup(\''+comId+'\','+i+')" style="cursor:pointer;color:var(--clay)">✕</span></span>'
  ).join('') + '</div>';
}

function renderForos(cont) {
  if (FORO_ACTIVO_SUP === null && DATA.foros.length>0) FORO_ACTIVO_SUP = DATA.foros[0].id;
  const foro = DATA.foros.find(f=>f.id===FORO_ACTIVO_SUP);
  let html = '<div class="section-header"><div><h2>Foros</h2><p class="subtitle">Acompaña la discusión de los estudiantes de tu institución</p></div></div>';
  html += '<div class="flex gap-18" style="align-items:flex-start">';
  html += '<div class="card" style="width:260px;flex-shrink:0;padding:0;overflow:hidden">';
  DATA.foros.forEach(f => {
    html += '<div onclick="FORO_ACTIVO_SUP=\''+f.id+'\';render()" style="padding:14px 16px;border-bottom:1px solid var(--line);cursor:pointer;background:'+(FORO_ACTIVO_SUP===f.id?'var(--blue-soft)':'transparent')+'">' +
      '<div style="font-size:13.5px;font-weight:700">' + esc(f.nombre) + '</div><div class="text-sm muted">' + esc(f.curso) + '</div></div>';
  });
  html += '</div><div class="card" style="flex:1;min-width:0">';
  if (foro) {
    const aportes = foro.comentarios.filter(c=>!c.padreId);
    html += '<div style="border-bottom:1px solid var(--line);padding-bottom:14px;margin-bottom:14px"><h3 style="font-family:\'Fraunces\',serif;font-size:18px;margin:0 0 4px">' + esc(foro.nombre) + '</h3><span class="text-sm muted">' + esc(foro.tema) + '</span></div>';
    aportes.forEach(c => {
      html += '<div style="margin-bottom:14px"><div style="background:var(--bone);border-radius:10px;padding:12px 14px">' +
        '<div class="flex justify-between mb-4"><span style="font-size:13px;font-weight:700">' + esc(c.autor) + '</span><span class="text-sm muted" style="font-size:11.5px">' + esc(c.horaTexto) + '</span></div>' +
        (c.titulo ? '<div style="font-weight:700;color:var(--sage);font-size:13.5px;margin-bottom:4px">' + esc(c.titulo) + '</div>' : '') +
        '<p style="font-size:13.5px;margin:0 0 6px">' + esc(c.contenido) + '</p>' +
        (c.adjuntos && c.adjuntos.length ? '<div class="flex gap-8 flex-wrap">' + c.adjuntos.map(renderAdjuntoSup).join('') + '</div>' : '') + '</div>';
      foro.comentarios.filter(r=>r.padreId===c.id).forEach(r => {
        const bg = r.rol==='docente' ? 'var(--amber-soft)' : r.rol==='supervisor' ? 'var(--blue-soft)' : 'var(--bone)';
        const fg = r.rol==='docente' ? '#8A611E' : r.rol==='supervisor' ? 'var(--blue-deep)' : 'var(--ink)';
        html += '<div style="margin-left:20px;margin-top:8px;background:'+bg+';border-radius:8px;padding:10px 14px">' +
          '<div class="text-sm" style="font-weight:700;margin-bottom:2px;color:'+fg+'">' + esc(r.autor) + (r.rol==='docente'?' · Docente':'') + (r.rol==='supervisor'?' · Supervisor':'') + '</div>' +
          '<p class="text-sm" style="margin:0 0 6px">' + esc(r.contenido) + '</p>' +
          (r.adjuntos && r.adjuntos.length ? '<div class="flex gap-8 flex-wrap">' + r.adjuntos.map(renderAdjuntoSup).join('') + '</div>' : '') + '</div>';
      });
      html += '<div style="margin-top:8px;margin-left:20px">' +
        '<input id="foro-resp-sup-'+c.id+'" placeholder="Responder a '+esc(c.autor)+' como supervisor..." style="width:100%;font-size:13px;margin-bottom:6px">' +
        chipsAdjuntoRepHTML(c.id) +
        '<div class="flex justify-between items-center"><div class="flex gap-6">' +
        '<button class="btn btn-ghost btn-xs" onclick="adjuntarImagenSup(\''+c.id+'\')">🖼 Imagen</button>' +
        '<button class="btn btn-ghost btn-xs" onclick="adjuntarDocumentoSup(\''+c.id+'\')">📄 Documento</button>' +
        '<button class="btn btn-ghost btn-xs" onclick="adjuntarEnlaceSup(\''+c.id+'\')">🔗 Enlace</button></div>' +
        '<button class="btn btn-ghost btn-sm" onclick="responderForoSup(\''+c.id+'\')">↩ Responder</button></div></div></div>';
    });
    if (aportes.length === 0) html += '<div class="empty-state">Sin aportes en este foro todavía.</div>';
  } else html += '<div class="empty-state">Selecciona un foro</div>';
  html += '</div></div>';
  cont.innerHTML = html;
}
function responderForoSup(comentarioId) {
  const input = document.getElementById('foro-resp-sup-'+comentarioId);
  const texto = input.value.trim();
  if (!texto) return;
  const foro = DATA.foros.find(f=>f.id===FORO_ACTIVO_SUP);
  const comentario = foro.comentarios.find(c=>c.id===comentarioId);
  foro.comentarios.push({ id:uid('com'), autor:SUPERVISOR.nombre, rol:'supervisor', titulo:null, contenido:texto, padreId:comentarioId, fecha:new Date().toISOString().slice(0,10), horaTexto:'ahora', likes:0, adjuntos: FORO_RESPUESTA_ADJ_SUP[comentarioId]||[] });
  delete FORO_RESPUESTA_ADJ_SUP[comentarioId];
  saveData(); render();
  toast('Respondiste a ' + comentario.autor + ' · marcado como Supervisor · el estudiante fue notificado');
}
