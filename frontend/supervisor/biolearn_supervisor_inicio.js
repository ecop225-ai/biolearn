/* ================================================================
   BIOLearn — biolearn_supervisor_inicio.js
================================================================ */
const ACCESO_RAPIDO_SUP = [
  ['cursos','Cursos inscritos','📚'], ['estudiantes','Estudiantes','👥'], ['foros','Foros','💬'],
  ['reportes','Reportes','📊'], ['institucion','Mi institución','🏫'], ['mensajeria','Mensajes','✉️'],
];
function renderInicio(cont) {
  const total = DATA.estudiantes.length;
  const enRiesgo = DATA.estudiantes.filter(e=>e.estado==='Riesgo');
  const promedioGeneral = (DATA.estudiantes.reduce((s,e)=>s+e.promedio,0)/total).toFixed(1);
  const avancePromedio = Math.round(DATA.estudiantes.reduce((s,e)=>s+e.avance,0)/total);
  const h = new Date().getHours();
  const saludo = h<12?'Buenos días':h<19?'Buenas tardes':'Buenas noches';
  const mensajesNuevos = DATA.mensajes.filter(m=>!m.leido).length;

  let html = '<div style="background:linear-gradient(120deg,var(--blue-deep),var(--blue));border-radius:16px;padding:22px 28px;margin-bottom:24px;color:#fff" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><div style="font-family:\'Fraunces\',serif;font-size:21px">¡' + saludo + ', ' + esc(SUPERVISOR.nombre.split(' ')[0]) + '! 🏫</div>' +
    '<div style="font-size:13.5px;opacity:.9">' + enRiesgo.length + ' estudiante(s) en riesgo académico y ' + mensajesNuevos + ' mensaje' + (mensajesNuevos===1?'':'s') + ' nuevo' + (mensajesNuevos===1?'':'s') + ' en ' + esc(DATA.institucion.nombre) + '</div></div>' +
    '<button style="background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.4);color:#fff;border-radius:9px;padding:11px 20px;font-weight:700;font-size:13.5px;cursor:pointer" onclick="goTo(\'estudiantes\')">Ver estudiantes</button></div>';

  html += '<div class="kpi-row">' +
    '<div class="kpi-card" style="border-left-color:var(--blue)"><div class="kpi-top"><span>Estudiantes afiliados</span><span class="kpi-icon" style="color:var(--blue)">' + iconSvg('users') + '</span></div><div class="kpi-value">' + total + '</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--amber)"><div class="kpi-top"><span>Promedio general</span><span class="kpi-icon" style="color:var(--amber)">' + iconSvg('chart') + '</span></div><div class="kpi-value">' + promedioGeneral + '</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--sage)"><div class="kpi-top"><span>Avance promedio</span><span class="kpi-icon" style="color:var(--sage)">↑</span></div><div class="kpi-value">' + avancePromedio + '%</div></div>' +
    '<div class="kpi-card" style="border-left-color:var(--clay)"><div class="kpi-top"><span>Estudiantes en riesgo</span><span class="kpi-icon" style="color:var(--clay)">↓</span></div><div class="kpi-value">' + enRiesgo.length + '</div></div>' +
  '</div>';

  html += '<div class="text-sm" style="font-weight:800;letter-spacing:.5px;text-transform:uppercase;color:var(--ink-soft);margin-bottom:12px">Acceso rápido</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px;margin-bottom:26px">';
  ACCESO_RAPIDO_SUP.forEach(([key,label,icon]) => {
    html += '<div onclick="goTo(\'' + key + '\')" class="card" style="text-align:center;cursor:pointer;padding:22px 12px"><div style="font-size:26px;margin-bottom:8px">' + icon + '</div><div style="font-weight:700;font-size:13px">' + label + '</div></div>';
  });
  html += '</div>';

  html += '<div class="flex gap-18 flex-wrap">';
  html += '<div class="card" style="flex:1.4;min-width:320px"><div class="flex justify-between items-center mb-14"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0">Estudiantes en riesgo académico</h3><button class="btn btn-ghost btn-sm" onclick="goTo(\'estudiantes\')">Ver todos</button></div>';
  if (enRiesgo.length === 0) html += '<div class="empty-state">Ningún estudiante en riesgo por ahora 🎉</div>';
  enRiesgo.forEach(e => {
    html += '<div onclick="goTo(\'estudiantes\')" class="flex justify-between items-center" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
      '<div><div style="font-size:14px;font-weight:600">' + esc(e.nombre) + '</div><div class="text-sm muted">' + esc(e.grado) + ' · ' + esc(e.curso) + ' · ' + e.avance + '% de avance</div></div>' + pill(e.estado) + '</div>';
  });
  html += '</div>';

  html += '<div class="card" style="flex:1;min-width:280px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">Mensajes recientes</h3>';
  DATA.mensajes.slice(0,3).forEach(m => {
    html += '<div onclick="goTo(\'mensajeria\')" class="flex gap-10" style="padding:12px 0;border-top:1px solid var(--line);cursor:pointer">' +
      '<div style="width:30px;height:30px;border-radius:8px;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;flex-shrink:0;color:var(--blue)">✉</div>' +
      '<div><div style="font-size:13.5px;font-weight:' + (m.leido?'500':'700') + '">' + esc(m.de) + '</div><div class="text-sm muted">' + esc(m.asunto) + '</div></div></div>';
  });
  html += '</div></div>';

  cont.innerHTML = html;
}
