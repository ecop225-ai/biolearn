/* ================================================================
   BIOLearn — biolearn_docente_evaluaciones.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: EVALUACIONES
================================================================ */
function evalVideoEmbedHTML(url, inicio) {
  if (!url) return '';
  let seg = 0;
  if (inicio) {
    const partes = inicio.split(':').map(Number);
    seg = partes.length === 2 ? partes[0]*60 + partes[1] : Number(inicio) || 0;
  }
  const yt = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/);
  const vimeo = url.match(/vimeo\.com\/(\d+)/);
  if (yt) return '<div class="mb-8"><iframe src="https://www.youtube.com/embed/'+yt[1]+(seg?'?start='+seg:'')+'" style="width:100%;max-width:480px;height:270px;border:0;border-radius:8px" allowfullscreen></iframe></div>';
  if (vimeo) return '<div class="mb-8"><iframe src="https://player.vimeo.com/video/'+vimeo[1]+(seg?'#t='+seg+'s':'')+'" style="width:100%;max-width:480px;height:270px;border:0;border-radius:8px" allowfullscreen></iframe></div>';
  return '<p class="text-sm muted mb-8">🎬 <a href="'+esc(url)+'" target="_blank">'+esc(url)+'</a></p>';
}

let EVAL_ABIERTA = null;
function renderEvaluaciones(cont) {
  const leccionId = window.ctx_eva_leccion || '';
  let html = '<div class="section-header"><div><h2>Evaluaciones</h2><p class="subtitle">Crea quizzes con banco de preguntas y explicación de la respuesta correcta.</p></div>' +
    (leccionId ? '<button class="btn btn-primary" onclick="abrirModalEvaluacion()">+ Nueva evaluación</button>' : '') + '</div>';
  html += contextPickerHTML('eva', ['evaluacion','mixta']);
  if (!leccionId) { html += '<div class="empty-state">Selecciona un curso y una lección para ver o crear evaluaciones.</div>'; cont.innerHTML = html; renderModalEvaluacion(); renderModalPregunta(); renderModalFragmento(); renderModalEvalPreview(); return; }

  const items = DATA.evaluaciones.filter(e => e.leccionId === leccionId);
  if (items.length === 0) html += '<div class="empty-state">Esta lección aún no tiene evaluaciones.</div>';
  items.forEach(ev => {
    html += '<div class="card mb-14"><div class="flex justify-between mb-8">' +
      '<div><h3 style="font-size:17px;margin:0">' + esc(ev.nombre) + '</h3>' +
      '<p class="text-sm muted" style="margin:4px 0 0">' + ev.tipo + ' · ' + ev.intentos + ' intento(s) · ' + (ev.tiempoLimite?ev.tiempoLimite+' min':'sin límite de tiempo') + ' · pondera ' + ev.ponderacion + '% · ' + ev.preguntas.length + ' pregunta(s)</p></div>' +
      '<div class="flex gap-8" style="align-items:flex-start">' + pill(ev.estado) + '<button class="btn btn-ghost btn-xs" onclick="confirmarEliminarEvaluacion(\''+ev.id+'\')" style="color:var(--clay)">🗑</button></div></div>';
    html += '<div class="flex gap-8 mb-14"><button class="btn btn-ghost btn-sm" onclick="toggleEvalAbierta(\''+ev.id+'\')">' + (EVAL_ABIERTA===ev.id?'Ocultar preguntas':'Ver preguntas') + '</button>' +
      '<button class="btn btn-ghost btn-sm" onclick="abrirModalEvalPreview(\''+ev.id+'\')">👁 Ver como estudiante</button>' +
      (ev.estado==='borrador' ? '<button class="btn btn-amber btn-sm" onclick="publicarEvaluacion(\''+ev.id+'\')">Publicar</button>' : '') + '</div>';
    if (EVAL_ABIERTA === ev.id) {
      html += '<div style="border-top:1px solid var(--line);padding-top:14px">';
      html += '<div class="flex justify-between items-center mb-10"><span class="text-sm" style="font-weight:700;text-transform:uppercase">Lecturas base (opcional)</span>' +
        '<button class="btn btn-ghost btn-xs" onclick="abrirModalFragmento(\''+ev.id+'\')">+ Agregar lectura base</button></div>';
      (ev.fragmentos||[]).forEach(f => {
        html += '<div class="flex items-center gap-10" style="background:var(--sage-soft);border-radius:10px;padding:10px 14px;margin-bottom:8px">📖' +
          '<div style="flex:1"><div class="text-sm" style="font-weight:700;color:var(--sage)">' + esc(f.titulo) + '</div>' +
          '<div class="text-sm muted" style="font-size:11.5px">' + ev.preguntas.filter(p=>p.fragmentoId===f.id).length + ' pregunta(s) basada(s) en esta lectura</div></div>' +
          '<button class="btn btn-ghost btn-xs" onclick="abrirModalFragmento(\''+ev.id+'\',\''+f.id+'\')">✏️</button>' +
          '<button class="btn btn-ghost btn-xs" onclick="eliminarFragmento(\''+ev.id+'\',\''+f.id+'\')" style="color:var(--clay)">🗑</button></div>';
      });
      html += '<div class="text-sm" style="font-weight:700;text-transform:uppercase;margin-top:10px">Preguntas</div><div style="margin-top:10px">';
      ev.preguntas.forEach((p,i) => {
        const frag = (ev.fragmentos||[]).find(f=>f.id===p.fragmentoId);
        html += '<div class="card mb-8" style="background:var(--bone)">' +
          (frag ? '<div class="text-sm" style="font-weight:700;color:var(--sage);margin-bottom:6px">📖 Basada en: ' + esc(frag.titulo) + '</div>' : '') +
          '<div class="flex justify-between mb-4"><span class="text-sm" style="font-weight:600">' + (i+1) + '. ' + esc(p.enunciado) + '</span>' +
          '<div class="flex gap-6"><button class="btn btn-ghost btn-xs" onclick="abrirModalPregunta(\''+ev.id+'\',\''+p.id+'\')">✏️</button>' +
          '<button class="btn btn-ghost btn-xs" onclick="eliminarPregunta(\''+ev.id+'\',\''+p.id+'\')" style="color:var(--clay)">🗑</button></div></div>' +
          (p.imagen ? '<div style="margin-bottom:8px"><img src="'+p.imagen+'" style="max-width:220px;max-height:160px;border-radius:8px;border:1px solid var(--line)"></div>' : '') +
          (p.videoUrl ? evalVideoEmbedHTML(p.videoUrl, p.videoInicio) : '') +
          '<div class="text-sm muted" style="text-transform:capitalize;margin-bottom:6px">' + p.tipo.replace('_',' ') + ' · ' + p.puntaje + ' pto(s)</div>';
        (p.opciones||[]).forEach(o => {
          html += '<div class="text-sm" style="color:' + (o.esCorrecta?'var(--sage)':'var(--ink-soft)') + '">' + (o.esCorrecta?'✓':'○') + ' ' + esc(o.texto) + '</div>';
        });
        html += '</div>';
      });
      html += '</div><button class="btn btn-ghost btn-sm" onclick="abrirModalPregunta(\''+ev.id+'\')">+ Agregar pregunta</button></div>';
    }
    html += '</div>';
  });
  cont.innerHTML = html;
  renderModalEvaluacion(); renderModalPregunta(); renderModalFragmento(); renderModalEvalPreview();
}
function toggleEvalAbierta(id) { EVAL_ABIERTA = EVAL_ABIERTA === id ? null : id; render(); }
function publicarEvaluacion(id) {
  const ev = DATA.evaluaciones.find(x=>x.id===id);
  if (ev.preguntas.length === 0) { toast('Agrega al menos una pregunta antes de publicar', true); return; }
  ev.estado = 'activa'; saveData(); render(); toast('Evaluación "' + ev.nombre + '" publicada para los estudiantes');
}
function confirmarEliminarEvaluacion(id) {
  const ev = DATA.evaluaciones.find(x=>x.id===id);
  confirmarAccion('¿Eliminar <strong>' + esc(ev.nombre) + '</strong> y todas sus preguntas?', function () {
    DATA.evaluaciones = DATA.evaluaciones.filter(x=>x.id!==id);
    saveData(); render(); toast('Evaluación "' + ev.nombre + '" eliminada');
  });
}
function abrirModalEvaluacion() {
  document.getElementById('eval-nombre').value = ''; document.getElementById('eval-descripcion').value = '';
  document.getElementById('eval-tipo').value = 'formativa'; document.getElementById('eval-tiempo').value = '';
  document.getElementById('eval-intentos').value = 2; document.getElementById('eval-puntaje').value = 5;
  document.getElementById('eval-ponderacion').value = 100; document.getElementById('eval-apertura').value = ''; document.getElementById('eval-cierre').value = '';
  abrirModal('modal-evaluacion');
}
function renderModalEvaluacion() {
  if (document.getElementById('modal-evaluacion')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-evaluacion"><div class="modal"><div class="modal-header"><h3>Nueva evaluación</h3><button class="modal-close" onclick="cerrarModal(\'modal-evaluacion\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div class="campo"><label>Nombre</label><input type="text" id="eval-nombre" placeholder="Ej: Quiz: leyes de Mendel"></div>' +
    '<div class="campo"><label>Instrucciones</label><textarea id="eval-descripcion" rows="2"></textarea></div>' +
    '<div class="grid-2"><div class="campo"><label>Tipo</label><select id="eval-tipo"><option value="diagnostica">Diagnóstica</option><option value="formativa">Formativa</option><option value="sumativa">Sumativa</option></select></div>' +
    '<div class="campo"><label>Tiempo límite (min)</label><input type="number" id="eval-tiempo"><p class="hint">Vacío = sin límite</p></div>' +
    '<div class="campo"><label>Intentos permitidos</label><input type="number" id="eval-intentos"></div>' +
    '<div class="campo"><label>Puntaje máximo</label><input type="number" id="eval-puntaje"></div>' +
    '<div class="campo"><label>Ponderación (%)</label><input type="number" id="eval-ponderacion"></div>' +
    '<div class="campo"><label>Apertura</label><input type="datetime-local" id="eval-apertura"></div>' +
    '<div class="campo"><label>Cierre</label><input type="datetime-local" id="eval-cierre"></div></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-evaluacion\')">Cancelar</button><button class="btn btn-primary" onclick="guardarEvaluacion()">Crear evaluación</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarEvaluacion() {
  const nombre = document.getElementById('eval-nombre').value.trim();
  const leccionId = window.ctx_eva_leccion;
  if (!nombre) { toast('Escribe el nombre de la evaluación', true); return; }
  if (!leccionId) { toast('Selecciona primero una lección', true); return; }
  const nueva = { id: uid('eval'), leccionId, nombre, descripcion: document.getElementById('eval-descripcion').value.trim(),
    tipo: document.getElementById('eval-tipo').value, tiempoLimite: document.getElementById('eval-tiempo').value ? Number(document.getElementById('eval-tiempo').value) : '',
    intentos: Number(document.getElementById('eval-intentos').value)||1, puntajeMax: Number(document.getElementById('eval-puntaje').value)||5,
    ponderacion: Number(document.getElementById('eval-ponderacion').value)||100, fechaApertura: document.getElementById('eval-apertura').value,
    fechaCierre: document.getElementById('eval-cierre').value, estado:'borrador', preguntas:[], fragmentos:[] };
  DATA.evaluaciones.push(nueva);
  EVAL_ABIERTA = nueva.id;
  saveData(); cerrarModal('modal-evaluacion'); render(); toast('Evaluación "' + nombre + '" creada · agrega preguntas antes de publicarla');
}
/* ---- lecturas base (fragmentos) ---- */
function abrirModalFragmento(evId, fragId) {
  const ev = DATA.evaluaciones.find(x=>x.id===evId);
  const f = fragId ? (ev.fragmentos||[]).find(x=>x.id===fragId) : null;
  document.getElementById('frag-ev-id').value = evId; document.getElementById('frag-id').value = fragId||'';
  document.getElementById('frag-modal-titulo').textContent = f ? 'Editar lectura base' : 'Nueva lectura base';
  document.getElementById('frag-titulo').value = f ? f.titulo : '';
  setTimeout(()=>rteSetValue('frag-editor', f ? f.contenido : ''), 0);
  abrirModal('modal-fragmento');
}
function renderModalFragmento() {
  if (document.getElementById('modal-fragmento')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-fragmento"><div class="modal grande"><div class="modal-header"><h3 id="frag-modal-titulo">Nueva lectura base</h3><button class="modal-close" onclick="cerrarModal(\'modal-fragmento\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="frag-ev-id"><input type="hidden" id="frag-id">' +
    '<div class="campo"><label>Título</label><input type="text" id="frag-titulo"><p class="hint">Identifica el caso o texto que leerán los estudiantes antes de responder.</p></div>' +
    '<div class="campo"><label>Contenido de la lectura</label>' + richTextEditorHTML('frag-editor','Escribe el texto, caso o dato que el estudiante debe leer antes de responder...') + '</div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-fragmento\')">Cancelar</button><button class="btn btn-primary" onclick="guardarFragmento()">Guardar lectura</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarFragmento() {
  const evId = document.getElementById('frag-ev-id').value, fragId = document.getElementById('frag-id').value;
  const titulo = document.getElementById('frag-titulo').value.trim();
  if (!titulo) { toast('Escribe un título para la lectura', true); return; }
  const ev = DATA.evaluaciones.find(x=>x.id===evId);
  if (!ev.fragmentos) ev.fragmentos = [];
  const contenido = rteGetValue('frag-editor');
  if (fragId) { const f = ev.fragmentos.find(x=>x.id===fragId); f.titulo = titulo; f.contenido = contenido; }
  else ev.fragmentos.push({ id: uid('frag'), titulo, contenido, imagen:'' });
  saveData(); cerrarModal('modal-fragmento'); render(); toast('Lectura base guardada');
}
function eliminarFragmento(evId, fragId) {
  const ev = DATA.evaluaciones.find(x=>x.id===evId);
  ev.fragmentos = (ev.fragmentos||[]).filter(f=>f.id!==fragId);
  ev.preguntas.forEach(p => { if (p.fragmentoId === fragId) p.fragmentoId = null; });
  saveData(); render(); toast('Lectura base eliminada');
}
/* ---- preguntas (una por una, con opciones e imagen) ---- */
let PREG_OPCIONES_TMP = [];
function abrirModalPregunta(evId, pregId) {
  const ev = DATA.evaluaciones.find(x=>x.id===evId);
  const p = pregId ? ev.preguntas.find(x=>x.id===pregId) : null;
  document.getElementById('preg-ev-id').value = evId; document.getElementById('preg-id').value = pregId||'';
  document.getElementById('preg-modal-titulo').textContent = p ? 'Editar pregunta' : 'Nueva pregunta';
  const selFrag = document.getElementById('preg-fragmento');
  selFrag.innerHTML = '<option value="">Ninguna — pregunta independiente</option>' + (ev.fragmentos||[]).map(f=>'<option value="'+f.id+'"'+(p&&p.fragmentoId===f.id?' selected':'')+'>'+esc(f.titulo)+'</option>').join('');
  document.getElementById('preg-fragmento-wrap').style.display = (ev.fragmentos||[]).length > 0 ? 'block' : 'none';
  document.getElementById('preg-enunciado').value = p ? p.enunciado : '';
  document.getElementById('preg-imagen-label').innerHTML = p && p.imagen ? '<img src="'+p.imagen+'" style="max-width:120px;max-height:80px;border-radius:6px;vertical-align:middle;margin-left:8px">' : '';
  window._pregImagen = p ? (p.imagen||'') : '';
  document.getElementById('preg-video-url').value = p ? (p.videoUrl||'') : '';
  document.getElementById('preg-video-inicio').value = p ? (p.videoInicio||'') : '';
  document.getElementById('preg-tipo').value = p ? p.tipo : 'seleccion_multiple';
  document.getElementById('preg-puntaje').value = p ? p.puntaje : 1;
  document.getElementById('preg-retro-abierta').value = p ? (p.retro||'') : '';
  PREG_OPCIONES_TMP = p && p.opciones ? JSON.parse(JSON.stringify(p.opciones)) : [{id:uid('op'),texto:'',esCorrecta:true,retro:''},{id:uid('op'),texto:'',esCorrecta:false,retro:''}];
  actualizarCamposPregunta();
  renderOpcionesPregunta();
  abrirModal('modal-pregunta');
}
function actualizarCamposPregunta() {
  const tipo = document.getElementById('preg-tipo').value;
  document.getElementById('preg-campo-abierta').style.display = tipo === 'abierta' ? 'block' : 'none';
  document.getElementById('preg-campo-opciones').style.display = tipo === 'abierta' ? 'none' : 'block';
  document.getElementById('preg-btn-agregar-opcion').style.display = tipo === 'seleccion_multiple' ? 'inline-flex' : 'none';
  if (tipo === 'verdadero_falso' && PREG_OPCIONES_TMP.length !== 2) {
    PREG_OPCIONES_TMP = [{id:uid('op'),texto:'Verdadero',esCorrecta:true,retro:''},{id:uid('op'),texto:'Falso',esCorrecta:false,retro:''}];
    renderOpcionesPregunta();
  }
}
function renderOpcionesPregunta() {
  const cont = document.getElementById('preg-opciones-lista');
  cont.innerHTML = PREG_OPCIONES_TMP.map((o,i) =>
    '<div class="card mb-8" style="background:var(--bone);padding:10px">' +
    '<div class="flex gap-8 items-center mb-6"><input type="radio" name="preg-correcta" ' + (o.esCorrecta?'checked':'') + ' onchange="marcarOpcionCorrecta('+i+')">' +
    '<input type="text" placeholder="Texto de la opción" value="'+esc(o.texto)+'" oninput="PREG_OPCIONES_TMP['+i+'].texto=this.value" style="flex:1">' +
    (PREG_OPCIONES_TMP.length>2 ? '<button onclick="quitarOpcionPregunta('+i+')" style="border:none;background:none;color:var(--clay);cursor:pointer">🗑</button>' : '') + '</div>' +
    '<input type="text" placeholder="Explicación de por qué es correcta o incorrecta" value="'+esc(o.retro)+'" oninput="PREG_OPCIONES_TMP['+i+'].retro=this.value" style="font-size:12.5px">' +
    '</div>'
  ).join('');
}
function marcarOpcionCorrecta(i) { PREG_OPCIONES_TMP.forEach((o,idx)=>o.esCorrecta = idx===i); }
function agregarOpcionPregunta() { PREG_OPCIONES_TMP.push({id:uid('op'),texto:'',esCorrecta:false,retro:''}); renderOpcionesPregunta(); }
function quitarOpcionPregunta(i) { PREG_OPCIONES_TMP.splice(i,1); renderOpcionesPregunta(); }
function subirImagenPregunta() {
  const input = document.createElement('input'); input.type='file'; input.accept='image/*';
  input.onchange = function(){
    const f=input.files[0]; if(!f) return;
    const reader = new FileReader();
    reader.onload = function(e) {
      window._pregImagen = e.target.result;
      document.getElementById('preg-imagen-label').innerHTML = '<img src="'+e.target.result+'" style="max-width:120px;max-height:80px;border-radius:6px;vertical-align:middle;margin-left:8px">';
    };
    reader.readAsDataURL(f);
  };
  input.click();
}
function renderModalPregunta() {
  if (document.getElementById('modal-pregunta')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-pregunta"><div class="modal"><div class="modal-header"><h3 id="preg-modal-titulo">Nueva pregunta</h3><button class="modal-close" onclick="cerrarModal(\'modal-pregunta\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="preg-ev-id"><input type="hidden" id="preg-id">' +
    '<div id="preg-fragmento-wrap" class="campo" style="display:none"><label>Basar en una lectura (opcional)</label><select id="preg-fragmento"></select><p class="hint">El estudiante verá el texto de la lectura antes de responder.</p></div>' +
    '<div class="grid-2"><div class="campo"><label>Video de referencia (opcional)</label><input type="url" id="preg-video-url" placeholder="Enlace de YouTube/Vimeo"></div>' +
    '<div class="campo"><label>Minuto de inicio (opcional)</label><input type="text" id="preg-video-inicio" placeholder="Ej: 2:15"><p class="hint">El video se abrirá desde ese punto exacto.</p></div></div>' +
    '<div class="campo"><label>Enunciado</label><textarea id="preg-enunciado" rows="2"></textarea></div>' +
    '<div class="campo"><label>Imagen de la pregunta (opcional)</label><button type="button" class="btn btn-ghost btn-sm" onclick="subirImagenPregunta()">🖼 Agregar imagen</button><span id="preg-imagen-label"></span></div>' +
    '<div class="grid-2"><div class="campo"><label>Tipo de pregunta</label><select id="preg-tipo" onchange="actualizarCamposPregunta()"><option value="seleccion_multiple">Selección múltiple</option><option value="verdadero_falso">Verdadero / Falso</option><option value="abierta">Pregunta abierta</option></select></div>' +
    '<div class="campo"><label>Puntaje</label><input type="number" id="preg-puntaje"></div></div>' +
    '<div id="preg-campo-abierta" class="campo" style="display:none"><label>Explicación de la respuesta esperada</label><textarea id="preg-retro-abierta" rows="2"></textarea><p class="hint">Visible al estudiante solo al agotar todos sus intentos.</p></div>' +
    '<div id="preg-campo-opciones" class="campo"><label>Opciones de respuesta</label><div id="preg-opciones-lista"></div>' +
    '<button type="button" id="preg-btn-agregar-opcion" class="btn btn-ghost btn-sm" style="margin-top:8px" onclick="agregarOpcionPregunta()">+ Agregar opción</button></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-pregunta\')">Cancelar</button><button class="btn btn-primary" onclick="guardarPregunta()">Guardar pregunta</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarPregunta() {
  const evId = document.getElementById('preg-ev-id').value, pregId = document.getElementById('preg-id').value;
  const enunciado = document.getElementById('preg-enunciado').value.trim();
  if (!enunciado) { toast('Escribe el enunciado de la pregunta', true); return; }
  const ev = DATA.evaluaciones.find(x=>x.id===evId);
  const tipo = document.getElementById('preg-tipo').value;
  const datos = { enunciado, tipo, puntaje: Number(document.getElementById('preg-puntaje').value)||1,
    imagen: window._pregImagen||'', fragmentoId: document.getElementById('preg-fragmento').value || null,
    videoUrl: document.getElementById('preg-video-url').value.trim(), videoInicio: document.getElementById('preg-video-inicio').value.trim(),
    retro: tipo === 'abierta' ? document.getElementById('preg-retro-abierta').value.trim() : '',
    opciones: tipo === 'abierta' ? [] : PREG_OPCIONES_TMP };
  if (pregId) { Object.assign(ev.preguntas.find(x=>x.id===pregId), datos); }
  else ev.preguntas.push({ id: uid('preg'), ...datos });
  saveData(); cerrarModal('modal-pregunta'); render(); toast('Pregunta guardada');
}
function eliminarPregunta(evId, pregId) {
  const ev = DATA.evaluaciones.find(x=>x.id===evId);
  ev.preguntas = ev.preguntas.filter(p=>p.id!==pregId);
  saveData(); render();
}
/* ---- vista previa "como estudiante" de la evaluación ---- */
function abrirModalEvalPreview(evId) {
  const ev = DATA.evaluaciones.find(x=>x.id===evId);
  document.getElementById('evp-nombre').textContent = ev.nombre;
  document.getElementById('evp-meta').innerHTML = '<span>🕒 ' + (ev.tiempoLimite?ev.tiempoLimite+' min':'Sin límite de tiempo') + '</span><span>🔁 ' + ev.intentos + ' intento(s)</span><span>⭐ ' + ev.puntajeMax + ' pts · pondera ' + ev.ponderacion + '%</span>';
  document.getElementById('evp-descripcion').style.display = ev.descripcion ? 'block' : 'none';
  document.getElementById('evp-descripcion').textContent = ev.descripcion || '';
  let contador = 0;
  function renderPregunta(p) {
    contador++;
    let html = '<div class="card mb-8"><div class="text-sm" style="font-weight:700;color:#8A611E;margin-bottom:6px">Pregunta ' + contador + ' · ' + p.puntaje + ' pto(s)</div>' +
      '<p style="font-size:14px;font-weight:600;margin:0 0 12px">' + esc(p.enunciado) + '</p>';
    if (p.imagen) html += '<div class="mb-8"><img src="'+p.imagen+'" style="max-width:100%;max-height:260px;border-radius:8px"></div>';
    if (p.videoUrl) html += evalVideoEmbedHTML(p.videoUrl, p.videoInicio);
    if (p.tipo === 'abierta') html += '<textarea disabled placeholder="Escribe tu respuesta..." rows="2" style="background:var(--bone)"></textarea>';
    else { (p.opciones||[]).forEach(o => { html += '<label class="flex items-center gap-10" style="background:var(--bone);border-radius:8px;padding:10px 12px;font-size:13.5px;margin-bottom:6px"><input type="radio" disabled> ' + esc(o.texto) + '</label>'; }); }
    html += '</div>';
    return html;
  }
  let body = '';
  const fragmentosMostrados = new Set();
  ev.preguntas.forEach(p => {
    if (p.fragmentoId && !fragmentosMostrados.has(p.fragmentoId)) {
      const f = (ev.fragmentos||[]).find(x=>x.id===p.fragmentoId);
      if (f) {
        body += '<div class="card mb-8" style="border-left:4px solid var(--sage)"><div class="text-sm" style="font-weight:700;color:var(--sage);margin-bottom:8px">📖 ' + esc(f.titulo) + '</div><div>' + (f.contenido||'') + '</div></div>';
        fragmentosMostrados.add(p.fragmentoId);
      }
    }
    body += renderPregunta(p);
  });
  if (ev.preguntas.length === 0) body = '<div class="empty-state">Esta evaluación todavía no tiene preguntas.</div>';
  document.getElementById('evp-preguntas').innerHTML = body;
  abrirModal('modal-eval-preview');
}
function renderModalEvalPreview() {
  if (document.getElementById('modal-eval-preview')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-eval-preview"><div class="modal grande"><div class="modal-header"><h3>Vista previa del estudiante</h3><button class="modal-close" onclick="cerrarModal(\'modal-eval-preview\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div style="background:#F7F3EC;border-radius:14px;padding:24px;border:1px solid #EFE6DA">' +
    '<span style="display:inline-flex;align-items:center;gap:6px;background:var(--blue-soft);color:var(--blue);font-size:12.5px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:14px">📋 Evaluación</span>' +
    '<h2 style="font-family:\'Fraunces\',serif;font-size:23px;color:#2C4A42;margin:0 0 8px" id="evp-nombre"></h2>' +
    '<div class="flex items-center gap-14 flex-wrap" style="font-size:13px;color:#6B5B45;margin-bottom:18px" id="evp-meta"></div>' +
    '<p style="font-size:13.5px;color:var(--ink);background:#fff;border-radius:10px;padding:14px;border:1px solid #EFE6DA;margin-bottom:18px;display:none" id="evp-descripcion"></p>' +
    '<div id="evp-preguntas"></div>' +
    '<button disabled style="background:var(--blue-deep);opacity:.85;border:none;border-radius:9px;padding:10px 20px;font-weight:700;color:#fff;font-size:13.5px;margin-top:6px">Enviar evaluación</button>' +
    '</div>' +
    '<p class="text-sm muted" style="margin-top:14px">Así se ve para el estudiante. Las respuestas y el botón de envío están desactivados en esta vista previa.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-eval-preview\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
