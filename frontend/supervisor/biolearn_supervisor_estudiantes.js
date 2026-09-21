/* ================================================================
   BIOLearn — biolearn_supervisor_estudiantes.js
   Estudiantes (con detalle) + Cursos inscritos (solo lectura)
================================================================ */
let EST_FILTRO_SUP = 'todos';

function renderEstudiantes(cont) {
  const filtrados = DATA.estudiantes.filter(e => EST_FILTRO_SUP==='todos' || e.estado===EST_FILTRO_SUP);
  let html = '<div class="section-header"><div><h2>Estudiantes</h2><p class="subtitle">Estudiantes de tu institución afiliados a BioLearn</p></div></div>';
  html += '<div class="flex gap-8 mb-18">';
  ['todos','Bien','Regular','Riesgo'].forEach(f => {
    const activo = EST_FILTRO_SUP === f;
    html += '<button onclick="EST_FILTRO_SUP=\''+f+'\';render()" style="padding:7px 14px;border-radius:20px;font-size:13px;font-weight:700;cursor:pointer;border:1px solid '+(activo?'var(--blue)':'var(--line)')+';background:'+(activo?'var(--blue-soft)':'transparent')+';color:'+(activo?'var(--blue-deep)':'var(--ink-soft)')+'">' + f.charAt(0).toUpperCase()+f.slice(1) + '</button>';
  });
  html += '</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px">';
  filtrados.forEach(e => {
    const cfg = ESTADO_ESTUDIANTE[e.estado];
    const iniciales = e.nombre.split(' ').map(p=>p[0]).slice(0,2).join('');
    html += '<div onclick="abrirModalEstudianteSup(\''+e.id+'\')" class="card" style="cursor:pointer">' +
      '<div class="flex justify-between items-start mb-10"><div class="flex items-center gap-10">' +
      '<div style="width:38px;height:38px;border-radius:50%;background:var(--blue-soft);color:var(--blue-deep);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px">' + esc(iniciales) + '</div>' +
      '<div><div style="font-size:14.5px;font-weight:700">' + esc(e.nombre) + '</div><div class="text-sm muted">' + esc(e.grado) + '</div></div></div>' +
      '<span class="pill ' + pillClass(e.estado) + '">' + cfg.icon + ' ' + esc(e.estado) + '</span></div>' +
      '<div class="mb-10"><div class="flex justify-between text-sm muted mb-4"><span>Avance</span><span style="font-weight:700">' + e.avance + '%</span></div>' +
      '<div class="progress-bar"><div class="progress-fill" style="width:'+e.avance+'%;background:'+cfg.fg+'"></div></div></div>' +
      '<div class="flex justify-between text-sm"><span>Promedio: <strong>' + e.promedio.toFixed(1) + '</strong></span><span>' + e.actividadesEntregadas + '/' + e.actividadesTotal + ' actividades</span></div>' +
      (e.certificados>0 ? '<div style="margin-top:8px;font-size:12px;color:var(--amber);font-weight:700">🏅 ' + e.certificados + ' certificado(s)</div>' : '') + '</div>';
  });
  if (filtrados.length === 0) html += '<div class="empty-state">No hay estudiantes en este filtro.</div>';
  html += '</div>';
  cont.innerHTML = html;
  renderModalEstudianteSup();
}
function abrirModalEstudianteSup(id) {
  const e = DATA.estudiantes.find(x=>x.id===id);
  const d = e.detalle;
  const pct = Math.round((d.leccionesCompletadas/d.leccionesTotal)*100);
  document.getElementById('estrep-titulo').textContent = e.nombre;
  document.getElementById('estrep-meta').innerHTML = '🎓 ' + esc(e.grado) + ' &nbsp; 📘 ' + esc(e.curso) + ' &nbsp; 🧑\u200d🏫 Doc. ' + esc(e.docente);
  document.getElementById('estrep-kpis').innerHTML =
    '<div class="kpi-card" style="border-left-color:var(--amber);flex:1"><div class="text-sm muted">Promedio</div><div class="kpi-value">' + e.promedio.toFixed(1) + '</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--sage);flex:1"><div class="text-sm muted">Avance</div><div class="kpi-value">' + e.avance + '%</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--blue);flex:1"><div class="text-sm muted">Certificados</div><div class="kpi-value">' + e.certificados + '</div></div>';
  document.getElementById('estrep-progreso-bar').innerHTML = '<div class="progress-fill" style="width:'+pct+'%;background:var(--sage)"></div>';
  document.getElementById('estrep-progreso-txt').textContent = d.leccionesCompletadas + ' de ' + d.leccionesTotal + ' lecciones (' + pct + '%)';
  document.getElementById('estrep-notas').innerHTML = d.notasEvaluaciones.length === 0 ? '<div class="empty-state">Sin evaluaciones presentadas.</div>' :
    d.notasEvaluaciones.map((n,i) => '<div class="flex justify-between" style="padding:10px 14px;'+(i>0?'border-top:1px solid var(--line)':'')+';font-size:13.5px"><span>' + esc(n.nombre) + '</span><span style="font-weight:700">' + n.nota.toFixed(1) + ' / ' + n.max.toFixed(1) + '</span></div>').join('');
  document.getElementById('estrep-foros').textContent = d.participacionForos + ' aporte(s)';
  document.getElementById('estrep-eventos').innerHTML = d.asistenciaEventos.length === 0 ? '<div class="empty-state">Sin eventos registrados.</div>' :
    d.asistenciaEventos.map((ev,i) => '<div class="flex justify-between" style="padding:10px 14px;'+(i>0?'border-top:1px solid var(--line)':'')+';font-size:13px"><span>' + esc(ev.evento) + '</span><span style="font-weight:700;color:'+(ev.asistio?'var(--sage)':'var(--clay)')+'">' + (ev.asistio?'Asistió':'No asistió') + '</span></div>').join('');
  abrirModal('modal-estudiante-sup');
}
function renderModalEstudianteSup() {
  if (document.getElementById('modal-estudiante-sup')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-estudiante-sup"><div class="modal" style="max-width:560px"><div class="modal-header"><h3 id="estrep-titulo"></h3><button class="modal-close" onclick="cerrarModal(\'modal-estudiante-rep\')">✕</button></div>' +
    '<div class="modal-body">' +
    '<div class="text-sm muted mb-18" id="estrep-meta"></div>' +
    '<div class="flex gap-14 flex-wrap mb-20" id="estrep-kpis"></div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:8px">Lecciones completadas</div>' +
    '<div class="progress-bar" id="estrep-progreso-bar"></div><p class="text-sm muted" id="estrep-progreso-txt" style="margin:6px 0 18px"></p>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:8px">Notas por evaluación</div>' +
    '<div class="card" style="padding:0;margin-bottom:18px" id="estrep-notas"></div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:6px">Participación en foros</div>' +
    '<div style="font-size:18px;font-weight:700;margin-bottom:18px" id="estrep-foros"></div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:8px">Asistencia a eventos</div>' +
    '<div class="card" style="padding:0" id="estrep-eventos"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-estudiante-rep\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ---------------------------- Cursos inscritos (solo lectura) ---------------------------- */
let CURSO_REP_SEL = null;
function renderCursosInscritos(cont) {
  if (CURSO_REP_SEL === null && DATA.cursosInscritos.length>0) CURSO_REP_SEL = DATA.cursosInscritos[0].id;
  const curso = DATA.cursosInscritos.find(c=>c.id===CURSO_REP_SEL);
  let html = '<div class="section-header"><div><h2>Cursos donde están mis estudiantes</h2><p class="subtitle">Visibilidad de solo lectura sobre el avance de tus estudiantes en cada curso</p></div></div>';
  html += '<span style="display:inline-flex;align-items:center;gap:6px;background:var(--blue-soft);color:var(--blue-deep);font-size:12.5px;font-weight:700;padding:5px 12px;border-radius:20px;margin-bottom:18px">' + iconSvg('eye') + ' Modo observador — solo lectura</span>';

  html += '<div class="tabla-wrap mb-20"><table><thead><tr style="background:var(--blue-soft)">' +
    ['Curso','Docente','Lecciones','Mis inscritos','Progreso prom.','Nota prom.','Estado'].map(h=>'<th style="color:var(--blue-deep)">'+h+'</th>').join('') + '</tr></thead><tbody>';
  DATA.cursosInscritos.forEach(c => {
    html += '<tr onclick="CURSO_REP_SEL=\''+c.id+'\';render()" style="cursor:pointer;background:'+(CURSO_REP_SEL===c.id?'var(--bone)':'transparent')+'">' +
      '<td style="font-weight:700">' + esc(c.nombre) + '</td><td class="text-sm muted">Doc. ' + esc(c.docente) + '</td><td class="text-sm">' + c.lecciones + ' lecciones</td>' +
      '<td style="font-weight:700">' + c.misInscritos + ' estudiantes</td>' +
      '<td><div class="flex items-center gap-8"><div style="width:80px" class="progress-bar"><div class="progress-fill" style="width:'+c.progresoProm+'%;background:'+(c.progresoProm>=60?'var(--sage)':'var(--amber)')+'"></div></div><span style="font-weight:700">' + c.progresoProm + '%</span></div></td>' +
      '<td style="font-weight:700;color:' + (c.notaProm>=4?'var(--sage)':c.notaProm>=3?'#8A611E':'var(--clay)') + '">' + c.notaProm.toFixed(1) + '</td>' +
      '<td>' + pill('activo') + '</td></tr>';
  });
  html += '</tbody></table></div>';

  if (curso) {
    html += '<h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 12px">📖 Avance por lección — ' + esc(curso.nombre) + '</h3>';
    html += '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
      ['Lección','Estudiantes completaron','Promedio notas','Estado'].map(h=>'<th style="color:var(--blue-deep)">'+h+'</th>').join('') + '</tr></thead><tbody>';
    curso.avancePorLeccion.forEach(l => {
      const estadoLbl = l.estado==='completada'?'Completada':l.estado==='en_progreso'?'En progreso':'Pendiente';
      html += '<tr><td style="font-weight:700">' + esc(l.nombre) + '</td><td>' + l.completaron + ' / ' + l.total + ' (' + Math.round((l.completaron/l.total)*100) + '%)</td>' +
        '<td style="font-weight:700;color:' + (l.promedio==null?'var(--ink-soft)':l.promedio>=4?'var(--sage)':l.promedio>=3?'#8A611E':'var(--clay)') + '">' + (l.promedio!=null?l.promedio.toFixed(1):'—') + '</td>' +
        '<td>' + pill(estadoLbl) + '</td></tr>';
    });
    html += '</tbody></table></div>';
  }
  cont.innerHTML = html;
}
