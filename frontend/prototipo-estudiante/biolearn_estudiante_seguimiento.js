/* ================================================================
   BIOLearn — biolearn_estudiante_seguimiento.js
   Certificados, Mis notas, Mi reporte
================================================================ */
const TIPO_ICONO_CERT = { evento:'🎖', inscripcion:'📄', finalizacion:'🥈' };

function renderCertificados(cont) {
  let html = '<div class="section-header"><div><h2>Mis certificados</h2></div></div>';
  html += '<div style="background:var(--blue-soft);color:var(--blue-deep);border-radius:12px;padding:14px 18px;font-size:13px;margin-bottom:22px">' +
    'Cada certificado incluye un código único de validación. Puedes verificarlo con tu docente o en <strong>biolearn.co/verificar</strong>.</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px;margin-bottom:24px">';
  DATA.certificados.forEach(c => {
    const emitido = c.estado === 'emitido';
    let pct = 0;
    if (!emitido && c.cursoId) {
      const curso = DATA.cursos.find(x=>x.id===c.cursoId);
      const lecciones = curso ? curso.lecciones.map(id=>DATA.leccionesById[id]).filter(l=>l.estado==='activa') : [];
      const completadas = lecciones.filter(l=>DATA.progresoLeccion[l.id] && DATA.progresoLeccion[l.id].estado==='completada').length;
      pct = lecciones.length ? Math.round((completadas/lecciones.length)*100) : 0;
    }
    html += '<div class="card" style="opacity:'+(emitido?'1':'.75')+'"><div style="font-size:28px;margin-bottom:10px">' + (TIPO_ICONO_CERT[c.tipo]||'🎖') + '</div>' +
      (emitido ? '<span style="font-size:11.5px;font-weight:700;color:var(--sage)">✓ Emitido</span>' : '<span style="font-size:11.5px;font-weight:700;color:var(--ink-soft)">Pendiente</span>') +
      '<h3 style="font-family:\'Fraunces\',serif;font-size:16.5px;margin:6px 0 4px">' + esc(c.origen) + '</h3>';
    if (emitido) {
      html += '<p class="text-sm muted mb-14">Emitido el ' + c.emitidoEn + '</p>' +
        '<div style="background:'+(c.tipo==='evento'?'var(--blue-soft)':'var(--bone-deep)')+';border-radius:10px;padding:12px;margin-bottom:12px">' +
        '<div class="text-sm muted" style="font-weight:700;text-transform:uppercase;margin-bottom:4px">Código de verificación</div>' +
        '<div style="font-family:monospace;font-weight:700;font-size:14px">' + esc(c.codigo) + '</div></div>' +
        '<div class="flex gap-8 mb-8"><button class="btn btn-ghost" style="flex:1;justify-content:center" onclick="toast(\'Descargando certificado...\')">⬇ Descargar</button>' +
        '<button class="btn btn-ghost" style="flex:1;justify-content:center" onclick="copiarCodigoCert(\''+esc(c.codigo)+'\')">📋 Copiar código</button></div>' +
        '<button class="btn btn-ghost w-full" style="justify-content:center" onclick="document.getElementById(\'cert-verif-codigo\').value=\''+esc(c.codigo)+'\';verificarCertificadoEst()">🛡 Verificar autenticidad</button>';
    } else {
      html += '<p class="text-sm muted mb-14">Curso en progreso (' + pct + '%)</p>' +
        '<div style="background:var(--bone-deep);border-radius:10px;padding:12px;font-size:12px;color:var(--ink-soft);display:flex;align-items:center;gap:6px">' + iconSvg('lock') + ' Se habilitará al completar el 100% del curso</div>';
    }
    html += '</div>';
  });
  if (DATA.certificados.length === 0) html += '<div class="empty-state">Aún no tienes certificados.</div>';
  html += '</div>';
  html += '<div class="card" style="max-width:420px"><h3 style="font-family:\'Fraunces\',serif;font-size:16px;margin:0 0 12px">Verificar un certificado</h3>' +
    '<div class="campo"><label>Código de validación</label><input id="cert-verif-codigo" placeholder="Ej: BIO-CERT-77621"></div>' +
    '<button class="btn btn-primary w-full" onclick="verificarCertificadoEst()">🛡 Verificar</button>' +
    '<div id="cert-verif-resultado"></div></div>';
  cont.innerHTML = html;
}
function copiarCodigoCert(codigo) { toast('Código ' + codigo + ' copiado al portapapeles'); }
function verificarCertificadoEst() {
  const codigo = document.getElementById('cert-verif-codigo').value.trim().toLowerCase();
  const c = DATA.certificados.find(x=>x.codigo && x.codigo.toLowerCase()===codigo);
  const cont = document.getElementById('cert-verif-resultado');
  if (!c) cont.innerHTML = '<p class="text-sm" style="color:var(--clay);margin-top:14px">No se encontró ningún certificado con ese código.</p>';
  else cont.innerHTML = '<div style="margin-top:14px;background:var(--blue-soft);border-radius:10px;padding:14px"><div class="text-sm" style="font-weight:700;color:var(--blue-deep);margin-bottom:6px">✓ Certificado válido</div><div class="text-sm">' + esc(c.origen) + '</div><div class="text-sm muted">Emitido el ' + esc(c.emitidoEn) + '</div></div>';
}

/* ---------------------------- Mis notas ---------------------------- */
let NOTAS_TAB = {};
function notaFilaHTML(opts) {
  const { icon, iconBg, titulo, subtitulo, feedback, docente, valorTxt, maxTxt, pct, estadoTxt, colorEstado } = opts;
  return '<div class="flex justify-between items-start" style="padding:16px 0;border-top:1px solid var(--line)">' +
    '<div class="flex gap-14" style="flex:1;min-width:0">' +
    '<div style="width:40px;height:40px;border-radius:10px;background:'+iconBg+';display:flex;align-items:center;justify-content:center;font-size:17px;flex-shrink:0">' + icon + '</div>' +
    '<div style="min-width:0"><div style="font-size:15px;font-weight:700">' + esc(titulo) + '</div>' +
    '<div class="text-sm muted" style="margin-top:2px">' + subtitulo + '</div>' +
    (feedback ? '<div class="text-sm" style="margin-top:8px;font-style:italic;color:var(--ink-soft);border-left:2px solid var(--line);padding-left:10px">"' + esc(feedback) + '" — ' + esc(docente) + '</div>' : '') +
    '</div></div>' +
    '<div style="text-align:right;flex-shrink:0;margin-left:14px;min-width:74px">' +
    '<div style="font-size:20px;font-weight:800;color:'+colorEstado+'">' + valorTxt + '</div>' +
    (maxTxt ? '<div class="text-sm muted">' + maxTxt + '</div>' : '') +
    (pct != null ? '<div class="progress-bar" style="width:80px;margin:6px 0 4px"><div class="progress-fill" style="width:'+pct+'%;background:'+colorEstado+'"></div></div>' : '<div style="height:10px"></div>') +
    '<div class="text-sm" style="font-weight:700;color:'+colorEstado+'">' + estadoTxt + '</div></div></div>';
}
function cambiarTabNotas(cursoId, tab) { NOTAS_TAB[cursoId] = tab; render(); }

function renderNotas(cont) {
  const todasActividades = DATA.actividades;
  const todasEvaluaciones = DATA.evaluaciones;
  const todosForos = DATA.foros;

  const actCalificadas = todasActividades.filter(a=>a.miEntrega && a.miEntrega.estado==='revisada');
  const evalPcts = todasEvaluaciones.filter(e=>e.misIntentos.length>0).map(e=>{const u=e.misIntentos[e.misIntentos.length-1]; return u.puntaje/u.max;});
  const forosCalificados = todosForos.filter(f => (f.calificaciones||[]).some(c=>c.estudiante===ESTUDIANTE.nombre));
  const notasGenerales = [...actCalificadas.map(a=>a.miEntrega.calificacion/a.puntajeMax*5), ...evalPcts.map(p=>p*5),
    ...forosCalificados.map(f=>f.calificaciones.find(c=>c.estudiante===ESTUDIANTE.nombre).calificacion)];
  const promedioGeneral = notasGenerales.length ? (notasGenerales.reduce((a,b)=>a+b,0)/notasGenerales.length) : 0;
  const promedioEval = evalPcts.length ? Math.round((evalPcts.reduce((a,b)=>a+b,0)/evalPcts.length)*100) : 0;

  let html = '<div class="section-header"><div><h2>Mis notas</h2><p class="subtitle">Resultados de tus actividades, evaluaciones y foros</p></div></div>';
  html += '<div class="kpi-row">' +
    '<div class="kpi-card" style="border-left-color:var(--sage)"><div class="kpi-value" style="font-size:26px;color:var(--sage)">' + (notasGenerales.length?promedioGeneral.toFixed(1):'—') + '</div><div class="text-sm" style="font-weight:800;text-transform:uppercase;color:var(--sage);margin-top:4px">Promedio general</div><div class="text-sm muted">Todos los cursos</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--blue)"><div class="kpi-value" style="font-size:26px">' + actCalificadas.length + '/' + todasActividades.length + '</div><div class="text-sm" style="font-weight:800;text-transform:uppercase;color:var(--blue);margin-top:4px">Actividades</div><div class="text-sm muted">Entregadas y calificadas</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--ink)"><div class="kpi-value" style="font-size:26px">' + promedioEval + '%</div><div class="text-sm" style="font-weight:800;text-transform:uppercase;color:var(--ink);margin-top:4px">Evaluaciones</div><div class="text-sm muted">Promedio de evaluaciones</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--amber)"><div class="kpi-value" style="font-size:26px">' + forosCalificados.length + '/' + todosForos.length + '</div><div class="text-sm" style="font-weight:800;text-transform:uppercase;color:var(--amber);margin-top:4px">Foros</div><div class="text-sm muted">Con participación calificada</div></div>' +
  '</div>';

  DATA.cursos.forEach(curso => {
    const actividades = DATA.actividades.filter(a => curso.lecciones.includes(a.leccionId));
    const evaluaciones = DATA.evaluaciones.filter(e => curso.lecciones.includes(e.leccionId));
    const foros = DATA.foros.filter(f => curso.lecciones.includes(f.leccionId));
    const notasCurso = [...actividades.filter(a=>a.miEntrega && a.miEntrega.estado==='revisada').map(a=>a.miEntrega.calificacion/a.puntajeMax*5),
      ...evaluaciones.filter(e=>e.misIntentos.length>0).map(e=>{const u=e.misIntentos[e.misIntentos.length-1]; return u.puntaje/u.max*5;}),
      ...foros.filter(f=>(f.calificaciones||[]).some(c=>c.estudiante===ESTUDIANTE.nombre)).map(f=>f.calificaciones.find(c=>c.estudiante===ESTUDIANTE.nombre).calificacion)];
    const promedioCurso = notasCurso.length ? (notasCurso.reduce((a,b)=>a+b,0)/notasCurso.length) : 0;
    const tab = NOTAS_TAB[curso.id] || 'actividades';

    html += '<div class="mb-20"><div style="background:linear-gradient(120deg,var(--blue),var(--sage));border-radius:14px 14px 0 0;padding:20px 24px" class="flex justify-between items-center flex-wrap gap-10">' +
      '<div><div style="color:#fff;font-size:17px;font-weight:800">🧬 ' + esc(curso.nombre) + '</div>' +
      '<div style="color:rgba(255,255,255,.85);font-size:13px">Doc. ' + esc(curso.docente) + ' · ' + curso.lecciones.length + ' lecciones · ' + (curso.companeros?curso.companeros.length+1:1) + ' estudiantes</div></div>' +
      '<div style="text-align:right"><div style="color:rgba(255,255,255,.8);font-size:11.5px;font-weight:700;text-transform:uppercase">Promedio del curso</div><div style="color:#C6E85B;font-size:26px;font-weight:800">' + (notasCurso.length?promedioCurso.toFixed(1):'—') + '</div></div></div>';

    html += '<div style="background:var(--panel);border:1px solid var(--line);border-top:none;padding:0 20px">' +
      '<div class="flex gap-24" style="border-bottom:1px solid var(--line)">';
    [['actividades','✏️','Actividades'],['evaluaciones','📝','Evaluaciones'],['foros','💬','Foros']].forEach(([key,icon,label]) => {
      const activo = tab === key;
      html += '<button onclick="cambiarTabNotas(\''+curso.id+'\',\''+key+'\')" style="background:none;border:none;cursor:pointer;padding:14px 4px;font-weight:700;font-size:14px;color:'+(activo?'var(--blue-deep)':'var(--ink-soft)')+';border-bottom:2.5px solid '+(activo?'var(--blue-deep)':'transparent')+'">' + icon + ' ' + label + '</button>';
    });
    html += '</div>';

    if (tab === 'actividades') {
      if (actividades.length === 0) html += '<div class="empty-state">Sin actividades en este curso.</div>';
      actividades.forEach(a => {
        const leccion = DATA.leccionesById[a.leccionId];
        const revisada = a.miEntrega && a.miEntrega.estado==='revisada';
        const vencida = new Date(a.fechaLimite) < new Date();
        let subtitulo, valorTxt, estadoTxt, color, pct = null;
        if (revisada) {
          subtitulo = 'Lección ' + leccion.orden + ' · Entregada: ' + fmtDate(a.miEntrega.fecha).split(',')[0] + ' · Calificada: ' + fmtDate(a.miEntrega.fecha).split(',')[0];
          pct = (a.miEntrega.calificacion/a.puntajeMax)*100;
          color = pct>=60 ? 'var(--sage)' : 'var(--amber)';
          valorTxt = a.miEntrega.calificacion.toFixed(1); estadoTxt = pct>=60?'Aprobado':'Reprobado';
        } else if (a.miEntrega) {
          subtitulo = 'Lección ' + leccion.orden + ' · Entregada: ' + fmtDate(a.miEntrega.fecha).split(',')[0] + ' · ⏳ Pendiente de calificar';
          color = 'var(--ink-soft)'; valorTxt = '—'; estadoTxt = 'Pendiente';
        } else {
          subtitulo = 'Lección ' + leccion.orden + ' · Vence: ' + fmtDate(a.fechaLimite).split(',')[0] + (vencida?' · ⚠️ Sin entregar':'');
          color = vencida?'var(--clay)':'var(--ink-soft)'; valorTxt = '—'; estadoTxt = vencida?'Sin entregar':'Por entregar';
        }
        html += notaFilaHTML({ icon:'📋', iconBg:'var(--amber-soft)', titulo:a.nombre, subtitulo,
          feedback: revisada ? a.miEntrega.comentarioRevisor : null, docente:'Doc. '+curso.docente,
          valorTxt, maxTxt: '/ ' + a.puntajeMax.toFixed(1), pct, estadoTxt, colorEstado:color });
      });
    }
    if (tab === 'evaluaciones') {
      if (evaluaciones.length === 0) html += '<div class="empty-state">Sin evaluaciones en este curso.</div>';
      evaluaciones.forEach(e => {
        const leccion = DATA.leccionesById[e.leccionId];
        const u = e.misIntentos.length ? e.misIntentos[e.misIntentos.length-1] : null;
        let subtitulo, valorTxt, estadoTxt, color, pct = null, feedback = null;
        if (u) {
          pct = Math.round((u.puntaje/u.max)*100);
          subtitulo = 'Lección ' + leccion.orden + ' · Intento ' + e.misIntentos.length + ' de ' + e.intentos + ' · Realizado: ' + fmtDate(u.fecha).split(',')[0];
          color = pct>=80 ? 'var(--sage)' : 'var(--amber)';
          valorTxt = pct + '%'; estadoTxt = pct>=80?'Aprobado':'Parcial';
        } else {
          subtitulo = 'Lección ' + leccion.orden + ' · ' + e.intentos + ' intento(s) disponible(s)';
          color = 'var(--ink-soft)'; valorTxt = '—'; estadoTxt = 'Sin presentar';
        }
        html += notaFilaHTML({ icon:'📝', iconBg:'var(--sage-soft)', titulo:e.nombre, subtitulo, feedback, docente:'Doc. '+curso.docente,
          valorTxt, maxTxt: u ? (u.puntaje+' / '+u.max+' pts') : null, pct, estadoTxt, colorEstado:color });
      });
    }
    if (tab === 'foros') {
      if (foros.length === 0) html += '<div class="empty-state">Sin foros en este curso.</div>';
      foros.forEach(f => {
        const leccion = DATA.leccionesById[f.leccionId];
        const calif = (f.calificaciones||[]).find(c=>c.estudiante===ESTUDIANTE.nombre);
        const misAportes = f.comentarios.filter(c=>c.autor===ESTUDIANTE.nombre && !c.padreId).length;
        let subtitulo, valorTxt, estadoTxt, color, pct = null, feedback = null;
        if (calif) {
          subtitulo = 'Lección ' + leccion.orden + ' · ' + misAportes + ' aporte(s) · ' + (f.estado==='cerrado'?'Cerrado el '+f.fechaCierre:'Abierto hasta '+f.fechaCierre);
          pct = (calif.calificacion/5)*100; color = pct>=60?'var(--sage)':'var(--amber)';
          valorTxt = calif.calificacion.toFixed(1); estadoTxt = 'Calificado'; feedback = calif.retro;
        } else {
          subtitulo = 'Lección ' + leccion.orden + ' · ' + misAportes + ' aporte(s) · ' + (f.estado==='cerrado'?'Cerrado el '+f.fechaCierre:'Abierto hasta '+f.fechaCierre);
          color = 'var(--ink-soft)'; valorTxt = '—'; estadoTxt = f.estado==='cerrado'?'Sin calificar':'En curso';
        }
        html += notaFilaHTML({ icon:'💬', iconBg:'var(--blue-soft)', titulo:f.nombre, subtitulo, feedback, docente:'Doc. '+curso.docente,
          valorTxt, maxTxt: '/ 5.0', pct, estadoTxt, colorEstado:color });
      });
    }
    html += '</div></div>';
  });
  cont.innerHTML = html;
}

/* ---------------------------- Mi reporte ---------------------------- */
function renderReporte(cont) {
  let html = '<div class="section-header"><div><h2>Mi reporte</h2><p class="subtitle">Solicita un reporte de tu progreso académico</p></div></div>';
  html += '<div class="card mb-14"><div class="campo"><label>Curso</label><select id="rep-est-curso"><option value="todos">Todos mis cursos</option>' + DATA.cursos.map(c=>'<option value="'+c.id+'">'+esc(c.nombre)+'</option>').join('') + '</select></div>' +
    '<div class="campo"><label>Formato</label><div class="flex gap-14"><label class="flex items-center gap-6 text-sm"><input type="radio" name="rep-est-formato" value="pdf" checked style="width:auto"> 📄 PDF</label>' +
    '<label class="flex items-center gap-6 text-sm"><input type="radio" name="rep-est-formato" value="excel" style="width:auto"> 📊 Excel</label></div></div>' +
    '<button class="btn btn-primary" onclick="solicitarReporteEst()">📊 Generar reporte</button></div>';
  html += '<h3 style="font-size:17px;margin:0 0 12px">Reportes generados</h3><div class="card" style="padding:0">';
  if (DATA.reportesSolicitados.length === 0) html += '<div class="empty-state">Aún no has generado reportes.</div>';
  DATA.reportesSolicitados.forEach((r,i) => {
    html += '<div class="flex justify-between items-center" style="padding:14px 18px;' + (i>0?'border-top:1px solid var(--line)':'') + '"><div><div style="font-size:14px;font-weight:600">' + esc(r.curso) + '</div><div class="text-sm muted">' + r.fecha + ' · ' + r.formato + '</div></div>' +
      '<button class="btn btn-ghost btn-sm" onclick="toast(\'Descargando reporte...\')">Descargar</button></div>';
  });
  html += '</div>';
  cont.innerHTML = html;
}
function solicitarReporteEst() {
  const cursoVal = document.getElementById('rep-est-curso').value;
  const cursoNombre = cursoVal==='todos' ? 'Todos mis cursos' : DATA.cursos.find(c=>c.id===cursoVal).nombre;
  const formato = document.querySelector('input[name="rep-est-formato"]:checked').value.toUpperCase();
  DATA.reportesSolicitados.unshift({ id:uid('rep'), curso:cursoNombre, formato, fecha:new Date().toISOString().slice(0,10) });
  saveData(); render();
  toast('Reporte de "' + cursoNombre + '" generado en ' + formato);
}
