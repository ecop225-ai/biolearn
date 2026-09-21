/* ================================================================
   BIOLearn — biolearn_estudiante_actividades.js
================================================================ */
let ACT_ARCHIVO_TMP = {};
let ACT_FILTRO_GLOBAL = 'todas';

function actividadCardHTML(a) {
  const vencida = new Date(a.fechaLimite) < new Date();
  const bloqueada = vencida && !a.permiteTardia && !a.miEntrega;
  let html = '<div class="card mb-14">' +
    '<div class="flex justify-between items-start mb-8"><div><span style="display:inline-flex;align-items:center;gap:6px;background:var(--amber-soft);color:#8A611E;font-size:12px;font-weight:700;padding:4px 10px;border-radius:20px;margin-bottom:8px">✏️ Tarea</span>' +
    '<h3 style="font-size:18px;margin:0">' + esc(a.nombre) + '</h3></div>' +
    (a.miEntrega ? pill(a.miEntrega.estado) : pill(vencida?'rechazado':'pendiente')) + '</div>' +
    '<div class="flex gap-14 flex-wrap text-sm muted mb-14"><span>📅 Vence ' + fmtDate(a.fechaLimite) + '</span><span>⭐ Valor: ' + a.puntajeMax.toFixed(1) + ' pts</span>' + (a.permiteTardia?'<span>Admite entrega tardía</span>':'') + '</div>' +
    '<div style="background:var(--bone);border-radius:10px;padding:14px;margin-bottom:14px;border-left:4px solid var(--amber)">' +
    '<div class="text-sm" style="font-weight:700;color:#8A611E;text-transform:uppercase;margin-bottom:6px">Instrucciones</div><p class="text-sm" style="margin:0">' + esc(a.instrucciones) + '</p>';
  if (a.archivoAdjunto) html += '<div class="flex justify-between items-center" style="background:#fff;border-radius:8px;padding:8px 12px;margin-top:10px"><span class="text-sm">📄 ' + esc(a.archivoAdjunto) + '</span><span class="text-sm" style="color:var(--blue);font-weight:700;cursor:pointer" onclick="descargarAdjuntoActividad(\''+esc(a.archivoAdjunto)+'\')">Descargar →</span></div>';
  html += '</div>';

  if (a.miEntrega) {
    html += '<div style="background:var(--blue-soft);border-radius:10px;padding:14px">' +
      '<div class="text-sm" style="font-weight:700;color:var(--blue-deep);margin-bottom:6px">Tu entrega</div>' +
      '<div class="text-sm flex items-center gap-6 mb-8">📄 ' + esc(a.miEntrega.archivo) + ' · ' + a.miEntrega.fecha + '</div>';
    if (a.miEntrega.estado === 'revisada') {
      html += '<div style="font-size:20px;font-weight:800;color:var(--blue-deep);margin-bottom:6px">' + a.miEntrega.calificacion.toFixed(1) + ' / ' + a.puntajeMax.toFixed(1) + '</div>' +
        '<div class="text-sm" style="font-weight:700;margin-bottom:4px">Retroalimentación del docente:</div><p class="text-sm" style="margin:0">' + esc(a.miEntrega.comentarioRevisor) + '</p>' +
        (a.miEntrega.sugerencias ? '<p class="text-sm muted" style="margin-top:6px"><strong>Sugerencias:</strong> ' + esc(a.miEntrega.sugerencias) + '</p>' : '');
    } else {
      html += '<p class="text-sm muted" style="margin:0">Tu docente aún no ha calificado esta entrega.</p>';
    }
    html += '</div>';
  } else if (bloqueada) {
    html += '<div class="empty-state">El plazo de entrega venció y esta actividad no admite entregas tardías.</div>';
  } else {
    const archivo = ACT_ARCHIVO_TMP[a.id];
    html += '<div class="text-sm" style="font-weight:700;color:var(--blue-deep);margin-bottom:8px">📎 Adjuntar archivo de entrega</div>' +
      '<div onclick="subirArchivoEntrega(\''+a.id+'\')" style="border:2px dashed var(--line);border-radius:12px;padding:24px 20px;text-align:center;margin-bottom:14px;cursor:pointer">' +
      (archivo ? '<div style="font-size:13.5px;font-weight:700">📄 ' + esc(archivo.nombre) + '</div>' :
        '<div style="font-size:28px;margin-bottom:6px">📁</div><div style="font-weight:700;font-size:13.5px">Haz clic para seleccionar tu archivo</div><div class="text-sm muted" style="margin-top:4px">PDF, DOCX, PPTX, JPG, PNG · Máx. 10 MB</div>') +
      '</div>' +
      '<div class="campo"><label>Comentario para el docente (opcional)</label><textarea id="act-comentario-'+a.id+'" rows="2" placeholder="Agrega un mensaje o aclaración sobre tu entrega..."></textarea></div>' +
      '<div class="flex justify-end"><button class="btn btn-primary" onclick="entregarActividad(\''+a.id+'\')" '+(!archivo?'disabled':'')+'>📤 Entregar tarea</button></div>';
  }
  html += '</div>';
  return html;
}
function descargarAdjuntoActividad(nombre) {
  toast('En esta demo, "' + nombre + '" es solo un nombre de ejemplo — no hay un archivo real que descargar todavía', true);
}
function subirArchivoEntrega(actId) {
  seleccionarArchivos(null, function (archivos) {
    if (archivos.length === 0) return;
    ACT_ARCHIVO_TMP[actId] = archivos[0];
    render();
  });
}
function entregarActividad(actId) {
  const archivo = ACT_ARCHIVO_TMP[actId];
  if (!archivo) return;
  const a = DATA.actividades.find(x=>x.id===actId);
  const comentarioEl = document.getElementById('act-comentario-'+actId);
  a.miEntrega = { archivo: archivo.nombre, comentario: comentarioEl?comentarioEl.value:'', fecha: new Date().toISOString().slice(0,10), estado:'entregada', calificacion:null, comentarioRevisor:'' };
  delete ACT_ARCHIVO_TMP[actId];
  saveData(); render();
  toast('Actividad entregada · tu docente la revisará pronto');
}

function renderActividades(cont) {
  let html = '<div class="section-header"><div><h2>Actividades</h2><p class="subtitle">Tareas asignadas por tus docentes</p></div></div>';
  html += '<div class="flex gap-8" style="margin-bottom:18px">';
  [['todas','Todas'],['pendientes','Pendientes'],['calificadas','Calificadas']].forEach(([key,label]) => {
    const activo = ACT_FILTRO_GLOBAL === key;
    html += '<button onclick="setFiltroActividadGlobal(\''+key+'\')" style="padding:7px 14px;border-radius:20px;font-size:13px;font-weight:700;cursor:pointer;border:1px solid '+(activo?'var(--blue)':'var(--line)')+';background:'+(activo?'var(--blue-soft)':'transparent')+';color:'+(activo?'var(--blue-deep)':'var(--ink-soft)')+'">' + label + '</button>';
  });
  html += '</div>';
  const actividades = DATA.actividades.filter(a => ACT_FILTRO_GLOBAL==='todas' || (ACT_FILTRO_GLOBAL==='pendientes' ? !a.miEntrega : (a.miEntrega && a.miEntrega.estado==='entregada')) || (ACT_FILTRO_GLOBAL==='calificadas' && a.miEntrega && a.miEntrega.estado==='revisada'));
  actividades.forEach(a => html += actividadCardHTML(a));
  if (actividades.length === 0) html += '<div class="empty-state">No hay actividades en este filtro.</div>';
  cont.innerHTML = html;
}
function setFiltroActividadGlobal(f) { ACT_FILTRO_GLOBAL = f; render(); }
