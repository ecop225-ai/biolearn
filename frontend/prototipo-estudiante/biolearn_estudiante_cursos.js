/* ================================================================
   BIOLearn — biolearn_estudiante_cursos.js
   Mis cursos → curso → lección (materiales, actividad, evaluación, sesión, foro)
================================================================ */
let CURSO_ACTIVO = null;
let LECCION_ACTIVA = null;
let EVAL_EJECUTANDO = null;

function renderCursos(cont) {
  if (LECCION_ACTIVA) { renderLeccionDetalle(cont); return; }
  if (CURSO_ACTIVO) { renderCursoDetalle(cont); return; }

  let html = '<div class="section-header"><div><h2>Mis cursos</h2><p class="subtitle">Cursos en los que estás inscrito</p></div></div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px">';
  DATA.cursos.forEach(c => {
    const lecciones = c.lecciones.map(id=>DATA.leccionesById[id]).filter(l=>l.estado==='activa');
    const completadas = lecciones.filter(l=>DATA.progresoLeccion[l.id] && DATA.progresoLeccion[l.id].estado==='completada').length;
    const enProgreso = lecciones.some(l=>DATA.progresoLeccion[l.id] && DATA.progresoLeccion[l.id].estado==='en_progreso');
    const pct = lecciones.length ? Math.round((completadas/lecciones.length)*100) : 0;
    const completado = completadas === lecciones.length && lecciones.length > 0;
    const color = completado ? 'var(--amber)' : enProgreso ? 'var(--sage)' : 'var(--blue)';
    html += '<div onclick="abrirCurso(\''+c.id+'\')" class="card" style="cursor:pointer;border-top:4px solid '+color+'">' +
      '<div class="flex justify-between items-start mb-14"><span style="font-size:30px">🧬</span>' + pill('Inscrito') + '</div>' +
      '<div class="text-sm" style="font-weight:700;color:'+(enProgreso?'var(--amber)':'var(--ink-soft)')+';text-transform:uppercase;margin-bottom:4px">' + (completado?'Completado':enProgreso?'En progreso':'Por comenzar') + '</div>' +
      '<h3 style="font-size:17px;margin:0 0 4px">' + esc(c.nombre) + '</h3>' +
      (c.codigo ? '<div class="text-sm mb-4" style="font-family:monospace;font-weight:700;color:'+color+'">' + esc(c.codigo) + '</div>' : '') +
      '<p class="text-sm muted mb-14">Doc. ' + esc(c.docente) + ' · ' + lecciones.length + ' lecciones</p>' +
      '<div class="progress-bar"><div class="progress-fill" style="width:'+pct+'%;background:'+color+'"></div></div>' +
      '<p class="text-sm" style="color:'+color+';font-weight:700;margin:6px 0 0">' + pct + '% completado</p></div>';
  });
  html += '<div onclick="goTo(\'catalogo\')" class="card" style="border:2px dashed var(--line);min-height:220px;display:flex;flex-direction:column;align-items:center;justify-content:center;cursor:pointer;gap:10px">' +
    '<div style="width:44px;height:44px;border-radius:50%;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;font-size:20px;color:var(--blue)">+</div>' +
    '<span style="font-weight:700;color:var(--blue)">Inscribirse a un curso</span></div>';
  html += '</div>';
  cont.innerHTML = html;
}
function abrirCurso(id) { CURSO_ACTIVO = id; LECCION_ACTIVA = null; render(); }
function volverAMisCursos() { CURSO_ACTIVO = null; LECCION_ACTIVA = null; render(); }
function abrirLeccion(id) { LECCION_ACTIVA = id; render(); }
function volverALecciones() { LECCION_ACTIVA = null; render(); }

function renderCursoDetalle(cont) {
  const curso = DATA.cursos.find(c=>c.id===CURSO_ACTIVO);
  const lecciones = curso.lecciones.map(id=>DATA.leccionesById[id]).filter(l=>l.estado==='activa').sort((a,b)=>a.orden-b.orden);
  const completadas = lecciones.filter(l=>DATA.progresoLeccion[l.id] && DATA.progresoLeccion[l.id].estado==='completada').length;

  let html = '<button class="btn btn-ghost" onclick="volverAMisCursos()" style="margin-bottom:16px">← Volver a mis cursos</button>';
  html += '<div class="section-header"><div><h2>' + esc(curso.nombre) + '</h2><p class="subtitle">' + esc(curso.area) + ' · ' + completadas + '/' + lecciones.length + ' lecciones completadas</p></div></div>';
  html += '<div class="progress-bar" style="margin-bottom:20px"><div class="progress-fill" style="width:'+(lecciones.length?(completadas/lecciones.length)*100:0)+'%"></div></div>';

  html += '<div class="card flex justify-between items-center" style="margin-bottom:20px">' +
    '<div class="flex items-center gap-10"><div style="width:38px;height:38px;border-radius:50%;background:var(--amber-soft);color:var(--amber);display:flex;align-items:center;justify-content:center;font-weight:700">' + esc(curso.docente.split(' ').map(p=>p[0]).slice(0,2).join('')) + '</div>' +
    '<div><div style="font-size:13.5px;font-weight:700">' + esc(curso.docente) + '</div><div class="text-sm muted">Docente del curso · ' + esc(curso.docenteEmail) + '</div></div></div>' +
    '<button class="btn btn-ghost" onclick="goTo(\'mensajeria\')">✉ Enviar mensaje</button></div>';

  html += '<div class="text-sm" style="font-weight:700;color:var(--ink-soft);text-transform:uppercase;letter-spacing:.3px;margin-bottom:10px">📖 Lecciones — ' + esc(curso.nombre) + '</div>';
  lecciones.forEach(l => {
    const info = estadoLeccion(curso, l);
    const numMateriales = DATA.materiales.filter(m=>m.leccionId===l.id).length;
    const numActividades = DATA.actividades.filter(a=>a.leccionId===l.id).length;
    const borderColor = info.estado==='completada' ? 'var(--sage)' : info.estado==='en_progreso' ? 'var(--amber)' : 'var(--line)';
    const circleBg = info.estado==='completada' ? 'var(--blue-soft)' : info.estado==='en_progreso' ? 'var(--amber-soft)' : 'var(--bone)';
    const circleFg = info.estado==='completada' ? 'var(--blue)' : info.estado==='en_progreso' ? '#8A611E' : 'var(--ink-soft)';
    let detalle = '';
    if (info.estado==='completada') detalle = 'Completada el ' + info.fecha + ' · ' + numMateriales + ' materiales · ' + numActividades + ' actividad(es)';
    else if (info.estado==='en_progreso') detalle = 'En progreso · ' + info.avance + '% · ' + numActividades + ' actividad(es) pendiente(s)';
    else if (info.estado==='pendiente') detalle = 'Disponible para comenzar';
    else detalle = 'Bloqueada · requiere completar "' + esc((DATA.leccionesById[l.prerequisito]||{}).nombre || 'la lección anterior') + '"';

    html += '<div onclick="abrirLeccion(\''+l.id+'\')" class="flex items-center gap-14" style="background:var(--panel);border-radius:12px;border:1px solid var(--line);border-left:4px solid '+borderColor+';padding:14px 16px;cursor:pointer;margin-bottom:10px;opacity:'+(info.estado==='bloqueada'?'.65':'1')+'">' +
      '<div style="width:34px;height:34px;border-radius:50%;background:'+circleBg+';color:'+circleFg+';display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px;flex-shrink:0">' +
      (info.estado==='completada' ? '✓' : info.estado==='bloqueada' ? iconSvg('lock') : l.orden) + '</div>' +
      '<div style="flex:1;min-width:0"><div style="font-size:14px;font-weight:600">' + l.orden + '. ' + esc(l.nombre) + '</div>' +
      '<div class="text-sm muted">' + detalle + '</div>' +
      (info.estado==='en_progreso' ? '<div style="margin-top:6px;width:160px" class="progress-bar"><div class="progress-fill" style="width:'+info.avance+'%;background:var(--amber)"></div></div>' : '') + '</div>' +
      pill(info.estado) + '</div>';
  });
  cont.innerHTML = html;
}

/* ---------------------------- Lección: materiales, actividad, evaluación, sesión, foro ---------------------------- */
let LECCION_TAB = 'materiales';
function renderLeccionDetalle(cont) {
  const leccion = DATA.leccionesById[LECCION_ACTIVA];
  const curso = DATA.cursos.find(c=>c.id===leccion.cursoId);
  const info = estadoLeccion(curso, leccion);

  if (info.estado === 'bloqueada') {
    const prereq = leccion.prerequisito ? DATA.leccionesById[leccion.prerequisito] : null;
    cont.innerHTML = '<button class="btn btn-ghost" onclick="volverALecciones()" style="margin-bottom:16px">← Volver a lecciones</button>' +
      '<div class="card" style="padding:40px;text-align:center">' + iconSvg('lock') +
      '<h3 style="font-size:19px;margin:12px 0 8px">Lección bloqueada</h3>' +
      '<p class="muted">Debes completar primero: <strong>' + esc(prereq ? prereq.nombre : 'la lección anterior') + '</strong></p></div>';
    return;
  }

  const materiales = DATA.materiales.filter(m=>m.leccionId===leccion.id).sort((a,b)=>a.orden-b.orden);
  const actividades = DATA.actividades.filter(a=>a.leccionId===leccion.id);
  const evaluaciones = DATA.evaluaciones.filter(e=>e.leccionId===leccion.id);
  const foros = DATA.foros.filter(f=>f.leccionId===leccion.id);
  const sesiones = DATA.sesiones.filter(s=>s.leccionId===leccion.id);

  const tabs = [
    { key:'materiales', label:'Materiales', n:materiales.length },
    { key:'actividad', label:'Actividad', n:actividades.length },
    { key:'evaluacion', label:'Evaluación', n:evaluaciones.length },
    { key:'sesion', label:'Sesión en línea', n:sesiones.length },
    { key:'foro', label:'Foro', n:foros.length },
  ].filter(t => t.n > 0);
  if (!tabs.some(t=>t.key===LECCION_TAB) && tabs.length > 0) LECCION_TAB = tabs[0].key;

  let html = '<button class="btn btn-ghost" onclick="volverALecciones()" style="margin-bottom:16px">← Volver a lecciones</button>';
  html += '<div class="section-header"><div><h2>' + leccion.orden + '. ' + esc(leccion.nombre) + '</h2><p class="subtitle">' + esc(curso.nombre) + '</p></div>' +
    (info.estado !== 'completada' ? '<button class="btn btn-primary" onclick="marcarLeccionCompletada(\''+leccion.id+'\')">✓ Marcar como completada</button>' : '') + '</div>';
  if (info.estado === 'completada') html += '<div style="display:inline-flex;align-items:center;gap:8px;background:var(--blue-soft);color:var(--blue);border-radius:10px;padding:10px 16px;font-weight:700;font-size:13px;margin-bottom:18px">✓ Lección completada</div>';

  if (tabs.length === 0) { html += '<div class="empty-state">Esta lección todavía no tiene contenido publicado.</div>'; cont.innerHTML = html; return; }

  html += '<div class="flex gap-8 flex-wrap" style="margin-bottom:20px">';
  tabs.forEach(t => {
    const activo = LECCION_TAB === t.key;
    html += '<button onclick="cambiarTabLeccion(\''+t.key+'\')" style="padding:9px 18px;border-radius:20px;font-weight:700;font-size:13.5px;cursor:pointer;border:1.5px solid '+(activo?'var(--blue)':'var(--line)')+';background:'+(activo?'var(--blue-soft)':'var(--panel)')+';color:'+(activo?'var(--blue-deep)':'var(--ink-soft)')+'">' + t.label + ' (' + t.n + ')</button>';
  });
  html += '</div>';

  if (LECCION_TAB === 'materiales') materiales.forEach(m => html += materialCardHTML(m));
  if (LECCION_TAB === 'actividad') actividades.forEach(a => html += actividadCardHTML(a));
  if (LECCION_TAB === 'evaluacion') evaluaciones.forEach(ev => {
    const restantes = ev.intentos - ev.misIntentos.length;
    html += '<div class="card mb-14"><div class="flex justify-between items-center">' +
      '<div><h4 style="font-size:17px;margin:0 0 6px">' + esc(ev.nombre) + '</h4>' +
      '<p class="text-sm muted mb-0">' + ev.tiempoLimite + ' min · ' + restantes + ' de ' + ev.intentos + ' intento(s) disponible(s)</p></div>' +
      '<button class="btn btn-primary" onclick="iniciarEvaluacion(\''+ev.id+'\')" '+(restantes<=0?'disabled':'')+'>' + (restantes<=0?'🔒 Sin más intentos':'Iniciar evaluación') + '</button></div>';
    if (ev.misIntentos.length > 0) { const u = ev.misIntentos[ev.misIntentos.length-1]; html += '<div class="text-sm muted" style="margin-top:12px">Último resultado: ' + u.puntaje + ' / ' + u.max + '</div>'; }
    html += '</div>';
  });
  if (LECCION_TAB === 'sesion') sesiones.forEach(s => html += sesionCardHTML(s));
  if (LECCION_TAB === 'foro') foros.forEach(f => html += foroViewHTML(f, leccion, curso));

  cont.innerHTML = html;
  renderModalEvaluacion();
}
function cambiarTabLeccion(key) { LECCION_TAB = key; render(); }

/* ---------------------------- Tarjeta de material ---------------------------- */
let MATERIAL_EXPANDIDO = {};
let MATERIAL_INICIADO = {};
function materialCardHTML(m) {
  const t = MATERIAL_TYPES[m.tipo];
  const esInteractivo = m.tipo==='laboratorio' || m.tipo==='juego' || m.tipo==='video';
  const expandido = !!MATERIAL_EXPANDIDO[m.id];
  let html = '<div class="card mb-14" style="padding:0;overflow:hidden">' +
    '<div onclick="toggleMaterial(\''+m.id+'\')" class="flex items-center gap-12" style="padding:14px 16px;cursor:pointer">' +
    '<div style="width:32px;height:32px;border-radius:8px;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;flex-shrink:0;color:'+t.color+'">' + t.icon + '</div>' +
    '<div style="flex:1"><div style="font-size:14px;font-weight:600">' + esc(m.nombre) + '</div>' +
    '<div class="text-sm muted">' + t.label + (m.proveedor?' · '+esc(m.proveedor):'') + (m.duracion?' · '+esc(m.duracion):'') + '</div></div>' +
    (m.vista ? '<span style="color:var(--sage);font-size:17px">✓</span>' : '') +
    '<span style="transform:rotate('+(expandido?'90deg':'0')+');transition:transform .15s;color:var(--ink-soft)">›</span></div>';
  if (expandido) {
    html += '<div style="padding:0 16px 18px">';
    if (m.tipo === 'lectura') html += '<div class="biolearn-richtext-body" style="background:var(--bone);border-radius:10px;padding:16px">' + (m.contenido || '<p>Sin contenido.</p>') + '</div>';
    if (m.tipo === 'articulo') html += '<div style="background:var(--bone);border-radius:10px;padding:16px;font-size:13.5px"><p style="margin:0 0 6px">' + esc(m.autores||'') + '</p><p style="margin:0 0 6px;color:var(--ink-soft)">' + esc(m.revista||'') + (m.doi?' · DOI: '+esc(m.doi):'') + '</p>' + (m.url?'<a href="'+esc(m.url)+'" target="_blank" style="color:var(--blue);font-weight:700">Ver artículo completo →</a>':'') + '</div>';
    if (m.tipo === 'documento') html += '<div class="flex justify-between items-center" style="background:var(--bone);border-radius:10px;padding:12px 14px"><span style="font-size:13.5px">📄 ' + esc(m.nombre) + '</span><span style="color:var(--blue);font-weight:700;font-size:12.5px">Descargar →</span></div>';
    if (esInteractivo) {
      if (!MATERIAL_INICIADO[m.id]) {
        html += '<div style="background:linear-gradient(135deg,#1E3345,#2E5588);border-radius:12px;padding:30px 20px;text-align:center">' +
          '<p style="color:#fff;font-size:13.5px;margin-bottom:14px">' + (m.proveedor?'Contenido alojado en '+esc(m.proveedor):'Contenido interactivo externo') + '</p>' +
          '<button onclick="event.stopPropagation();iniciarMaterialInteractivo(\''+m.id+'\')" style="background:#fff;color:var(--blue-deep);border:none;border-radius:30px;padding:10px 22px;font-weight:700;font-size:13.5px;cursor:pointer">▶ ' + (m.tipo==='video'?'Reproducir video':'Iniciar simulación') + '</button></div>';
      } else {
        html += '<div style="border-radius:12px;overflow:hidden;border:1px solid var(--line)"><iframe title="'+esc(m.nombre)+'" src="'+esc(m.url)+'" style="width:100%;height:340px;border:none"></iframe></div>';
      }
    }
    if (m.guia) html += '<div style="margin-top:14px;background:var(--blue-soft);border-radius:10px;padding:14px"><div class="text-sm" style="font-weight:700;color:var(--blue-deep);text-transform:uppercase;margin-bottom:6px">Guía de observación</div><div class="biolearn-richtext-body">' + m.guia + '</div></div>';
    html += '</div>';
  }
  html += '</div>';
  return html;
}
function toggleMaterial(id) {
  MATERIAL_EXPANDIDO[id] = !MATERIAL_EXPANDIDO[id];
  if (MATERIAL_EXPANDIDO[id]) {
    const m = DATA.materiales.find(x=>x.id===id);
    if (m && !m.vista) { m.vista = true; saveData(); }
  }
  render();
}
function iniciarMaterialInteractivo(id) { MATERIAL_INICIADO[id] = true; render(); }

function marcarLeccionCompletada(leccionId) {
  const leccion = DATA.leccionesById[leccionId];
  const curso = DATA.cursos.find(c=>c.id===leccion.cursoId);
  const activas = curso.lecciones.map(id=>DATA.leccionesById[id]).filter(l=>l.estado==='activa').sort((a,b)=>a.orden-b.orden);
  const siguiente = activas.find(l=>l.orden === leccion.orden+1);
  DATA.progresoLeccion[leccionId] = { estado:'completada', fecha:new Date().toISOString().slice(0,10) };
  if (siguiente && !DATA.progresoLeccion[siguiente.id]) DATA.progresoLeccion[siguiente.id] = { estado:'en_progreso', avance:0 };
  saveData(); render();
  toast('Lección "' + leccion.nombre + '" marcada como completada');
}
