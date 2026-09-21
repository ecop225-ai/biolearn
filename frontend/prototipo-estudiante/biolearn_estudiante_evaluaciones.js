/* ================================================================
   BIOLearn — biolearn_estudiante_evaluaciones.js
================================================================ */
let EVAL_RESPUESTAS = {};
let EVAL_ENVIADO = false;

function iniciarEvaluacion(evId) {
  EVAL_EJECUTANDO = evId; EVAL_RESPUESTAS = {}; EVAL_ENVIADO = false;
  renderModalEvaluacion();
  pintarModalEvaluacion();
  abrirModal('modal-evaluacion-runner');
}
function responderPregunta(pregId, val) { EVAL_RESPUESTAS[pregId] = val; }
function calcularResultado(ev) {
  let puntaje = 0, max = 0;
  ev.preguntas.forEach(p => {
    max += p.puntaje;
    if (p.tipo !== 'abierta') {
      const correcta = p.opciones.find(o=>o.esCorrecta);
      if (EVAL_RESPUESTAS[p.id] === correcta.id) puntaje += p.puntaje;
    }
  });
  return { puntaje, max };
}
function enviarEvaluacion() {
  const ev = DATA.evaluaciones.find(x=>x.id===EVAL_EJECUTANDO);
  const restantes = ev.intentos - ev.misIntentos.length;
  const esUltimo = restantes <= 1;
  const { puntaje, max } = calcularResultado(ev);
  ev.misIntentos.push({ respuestas: {...EVAL_RESPUESTAS}, puntaje, max, fecha: new Date().toISOString(), esUltimo });
  saveData();
  EVAL_ENVIADO = true;
  toast('Evaluación enviada');
  pintarModalEvaluacion();
}
function reintentarEvaluacion() { cerrarModal('modal-evaluacion-runner'); EVAL_EJECUTANDO = null; render(); }
function cerrarEvaluacionRunner() { cerrarModal('modal-evaluacion-runner'); EVAL_EJECUTANDO = null; render(); }

function pintarModalEvaluacion() {
  const ev = DATA.evaluaciones.find(x=>x.id===EVAL_EJECUTANDO);
  document.getElementById('evr-titulo').textContent = ev.nombre;
  const restantes = ev.intentos - ev.misIntentos.length;
  const esUltimo = restantes <= 1;
  const body = document.getElementById('evr-body');

  if (EVAL_ENVIADO) {
    const ultimo = ev.misIntentos[ev.misIntentos.length-1];
    let html = '<div style="text-align:center;margin-bottom:20px"><div style="font-family:\'Fraunces\',serif;font-size:40px;color:var(--blue-deep)">' + ultimo.puntaje + ' / ' + ultimo.max + '</div>' +
      '<p class="muted text-sm">' + (esUltimo ? 'Último intento · retroalimentación disponible' : 'Te queda(n) ' + (restantes-1) + ' intento(s)') + '</p></div>';
    ev.preguntas.forEach((p,i) => {
      if (p.tipo === 'abierta') {
        html += '<div style="background:var(--bone);border-radius:10px;padding:14px;margin-bottom:10px">' +
          '<p class="text-sm" style="font-weight:600;margin:0 0 8px">' + (i+1) + '. ' + esc(p.enunciado) + '</p>' +
          '<p class="text-sm muted" style="font-style:italic">' + esc(EVAL_RESPUESTAS[p.id]||'Sin respuesta') + '</p>' +
          (esUltimo && p.retro ? '<div style="background:var(--blue-soft);border-radius:8px;padding:10px;margin-top:8px;font-size:12.5px"><strong>Explicación del docente:</strong> ' + esc(p.retro) + '</div>' : '') + '</div>';
        return;
      }
      const correcta = p.opciones.find(o=>o.esCorrecta);
      const acerto = EVAL_RESPUESTAS[p.id] === correcta.id;
      html += '<div style="background:'+(esUltimo?(acerto?'var(--blue-soft)':'var(--clay-soft)'):'var(--bone)')+';border-radius:10px;padding:14px;margin-bottom:10px">' +
        '<div class="flex justify-between"><p class="text-sm" style="font-weight:600;margin:0 0 8px">' + (i+1) + '. ' + esc(p.enunciado) + '</p>' + (esUltimo ? (acerto?'<span style="color:var(--sage)">✓</span>':'<span style="color:var(--clay)">✗</span>') : '') + '</div>';
      p.opciones.forEach(o => { html += '<div class="text-sm" style="padding:4px 0;font-weight:'+(o.id===EVAL_RESPUESTAS[p.id]?'700':'400')+'">' + (o.id===EVAL_RESPUESTAS[p.id]?'● ':'○ ') + esc(o.texto) + '</div>'; });
      if (esUltimo) html += '<div style="background:#fff;border-radius:8px;padding:10px;margin-top:8px;font-size:12.5px"><strong>Explicación:</strong> ' + esc(correcta.retro) + '</div>';
      html += '</div>';
    });
    document.getElementById('evr-footer').innerHTML = (!esUltimo ? '<button class="btn btn-ghost" onclick="reintentarEvaluacion()" style="margin-right:8px">🔄 Intentar de nuevo (' + (restantes-1) + ' intento restante)</button>' : '') + '<button class="btn btn-primary" onclick="cerrarEvaluacionRunner()">Cerrar</button>';
    body.innerHTML = html;
    return;
  }

  let html = '<div class="flex gap-14 text-sm muted mb-14"><span>⏱ ' + (ev.tiempoLimite?ev.tiempoLimite+' min':'Sin límite') + '</span><span>' + restantes + ' intento(s) disponible(s)</span></div>';
  (ev.fragmentos||[]).forEach(f => {
    html += '<div style="background:var(--blue-soft);border-radius:10px;padding:14px;margin-bottom:14px;border-left:4px solid var(--sage)">' +
      '<div class="text-sm" style="font-weight:700;color:var(--blue-deep);text-transform:uppercase;margin-bottom:6px">' + esc(f.titulo) + '</div>' +
      '<div class="biolearn-richtext-body">' + f.contenido + '</div></div>';
  });
  ev.preguntas.forEach((p,i) => {
    html += '<div style="background:var(--bone);border-radius:10px;padding:14px;margin-bottom:10px">' +
      '<p class="text-sm" style="font-weight:600;margin:0 0 10px">' + (i+1) + '. ' + esc(p.enunciado) + '</p>';
    if (p.tipo === 'abierta') {
      html += '<textarea onchange="responderPregunta(\''+p.id+'\',this.value)" rows="3" style="background:#fff"></textarea>';
    } else {
      html += '<div class="flex-col gap-8">';
      p.opciones.forEach(o => {
        html += '<label style="display:flex;align-items:center;gap:10px;background:#fff;border-radius:8px;padding:10px 12px;font-size:13.5px;cursor:pointer;margin-bottom:8px">' +
          '<input type="radio" name="'+p.id+'" onchange="responderPregunta(\''+p.id+'\',\''+o.id+'\')"> ' + esc(o.texto) + '</label>';
      });
      html += '</div>';
    }
    html += '</div>';
  });
  document.getElementById('evr-footer').innerHTML = '<button class="btn btn-primary" onclick="enviarEvaluacion()">Enviar evaluación</button>';
  body.innerHTML = html;
}

function renderModalEvaluacion() {
  if (document.getElementById('modal-evaluacion-runner')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-evaluacion-runner"><div class="modal grande"><div class="modal-header"><h3 id="evr-titulo"></h3><button class="modal-close" onclick="cerrarEvaluacionRunner()">✕</button></div>' +
    '<div class="modal-body" id="evr-body"></div>' +
    '<div class="modal-btns" id="evr-footer"></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

function renderEvaluaciones(cont) {
  let html = '<div class="section-header"><div><h2>Evaluaciones</h2><p class="subtitle">Quizzes de tus lecciones — la retroalimentación se habilita al agotar los intentos</p></div></div>';
  DATA.evaluaciones.forEach(ev => {
    const restantes = ev.intentos - ev.misIntentos.length;
    html += '<div class="card mb-14"><div class="flex justify-between items-center">' +
      '<div><h3 style="font-size:18px;margin:0 0 6px">' + esc(ev.nombre) + '</h3>' +
      '<p class="text-sm muted mb-0">' + ev.tiempoLimite + ' min · ' + ev.preguntas.length + ' preguntas · ' + restantes + ' de ' + ev.intentos + ' intento(s) disponible(s)</p></div>' +
      '<button class="btn btn-primary" onclick="iniciarEvaluacion(\''+ev.id+'\')" '+(restantes<=0?'disabled':'')+'>' + (restantes<=0?'🔒 Sin más intentos':'Iniciar') + '</button></div></div>';
  });
  if (DATA.evaluaciones.length === 0) html += '<div class="empty-state">No tienes evaluaciones disponibles todavía.</div>';
  cont.innerHTML = html;
  renderModalEvaluacion();
}
