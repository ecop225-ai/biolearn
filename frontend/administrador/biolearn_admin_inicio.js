/* ================================================================
   BIOLearn — biolearn_admin_inicio.js
================================================================ */
function renderInicio(cont) {
  const roleCounts = {};
  DATA.usuarios.forEach(u => roleCounts[u.rol] = (roleCounts[u.rol]||0)+1);
  const totalUsuarios = DATA.usuarios.length;
  const cursosActivos = DATA.cursos.filter(c=>c.estado==='Publicado').length;
  const ingresosMes = DATA.ingresosMensuales[DATA.ingresosMensuales.length-1].ingresos;
  const areasUnicas = new Set(DATA.cursos.map(c=>c.area)).size;
  const estudiantesVinculados = DATA.instituciones.reduce((s,i)=>s+(i.usados||0),0);
  const trend = DATA.inscripcionesTrend;
  const maxTrend = Math.max(...trend.map(t=>t.estudiantes), 1);
  const horaActual = new Date().toLocaleTimeString('es-CO', { hour:'numeric', minute:'2-digit' });

  let html = '<div style="background:linear-gradient(120deg,#1C332C,#16241F);border-radius:16px;padding:24px 30px;margin-bottom:24px;color:#fff" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><div style="font-family:\'Fraunces\',serif;font-size:21px">Panel de administración — BioLearn</div>' +
    '<div style="font-size:13px;opacity:.8">Sistema activo · Último acceso: hoy ' + horaActual + ' · v1.0.0</div></div>' +
    '<div class="flex gap-18" style="font-size:12.5px">';
  [['Base de datos',true],['Servidor',true],['Correo',true],['WebSocket',false]].forEach(([label,ok]) => {
    html += '<div class="flex items-center gap-6" style="opacity:.9"><span style="width:8px;height:8px;border-radius:50%;background:'+(ok?'#5FC97A':'var(--amber)')+';display:inline-block"></span>' + label + '</div>';
  });
  html += '</div></div>';

  html += '<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:26px">' +
    '<div class="card"><div style="font-size:24px;margin-bottom:10px">👥</div><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:6px">Usuarios totales</div><div style="font-family:\'Fraunces\',serif;font-size:30px">' + totalUsuarios + '</div><div class="text-sm" style="color:var(--blue);font-weight:700;margin-top:2px">↑ 12 este mes</div></div>' +
    '<div class="card"><div style="font-size:24px;margin-bottom:10px">📚</div><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:6px">Cursos activos</div><div style="font-family:\'Fraunces\',serif;font-size:30px">' + cursosActivos + '</div><div class="text-sm muted" style="margin-top:2px">' + areasUnicas + ' áreas</div></div>' +
    '<div class="card"><div style="font-size:24px;margin-bottom:10px">💰</div><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:6px">Ingresos del mes</div><div style="font-family:\'Fraunces\',serif;font-size:30px">$' + ingresosMes + 'M</div><div class="text-sm" style="color:var(--blue);font-weight:700;margin-top:2px">↑ 18% vs mes anterior</div></div>' +
    '<div class="card"><div style="font-size:24px;margin-bottom:10px">🏫</div><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:6px">Instituciones</div><div style="font-family:\'Fraunces\',serif;font-size:30px">' + DATA.instituciones.length + '</div><div class="text-sm muted" style="margin-top:2px">' + estudiantesVinculados + ' estudiantes vinculados</div></div>' +
  '</div>';

  html += '<div class="flex gap-18 flex-wrap mb-20">';
  html += '<div class="card" style="flex:1.6;min-width:360px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 16px">📈 Inscripciones por mes</h3>' +
    '<div class="flex items-end gap-12" style="height:170px">';
  trend.forEach(t => {
    html += '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;height:100%">' +
      '<span class="text-sm" style="font-weight:700;margin-bottom:6px">' + t.estudiantes + '</span>' +
      '<div style="width:100%;border-radius:6px 6px 0 0;background:linear-gradient(180deg,var(--blue),#1C332C);height:'+((t.estudiantes/maxTrend)*100)+'%;min-height:6px"></div></div>';
  });
  html += '</div><div class="flex justify-between text-sm muted" style="margin-top:8px">' + trend.map(t=>'<span style="flex:1;text-align:center">'+t.mes+'</span>').join('') + '</div></div>';

  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">🕐 Actividad reciente</h3>';
  DATA.actividadReciente.forEach((a,i) => {
    html += '<div style="padding:10px 0;' + (i>0?'border-top:1px solid var(--line)':'') + ';font-size:13.5px"><span>' + esc(a.texto) + '</span><div class="text-sm muted" style="margin-top:2px">' + esc(a.fecha) + '</div></div>';
  });
  html += '</div></div>';

  html += '<div class="flex gap-18 flex-wrap">';
  html += '<div class="card" style="flex:2;min-width:340px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 4px">Ingresos vs. egresos</h3><p class="text-sm muted" style="margin:0 0 14px">Millones de pesos, últimos 6 meses</p>';
  const maxMonto = Math.max(...DATA.ingresosMensuales.map(m=>m.ingresos), 1);
  html += '<div class="flex items-end gap-14" style="height:180px">';
  DATA.ingresosMensuales.forEach(m => {
    html += '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;height:100%">' +
      '<div class="flex items-end gap-4" style="height:100%;width:100%;justify-content:center">' +
      '<div style="width:38%;border-radius:5px 5px 0 0;background:var(--blue);height:'+((m.ingresos/maxMonto)*100)+'%" title="Ingresos: $'+m.ingresos+'M"></div>' +
      '<div style="width:38%;border-radius:5px 5px 0 0;background:var(--amber);height:'+((m.egresos/maxMonto)*100)+'%" title="Egresos: $'+m.egresos+'M"></div></div>' +
      '<span class="text-sm muted" style="margin-top:6px">' + m.mes + '</span></div>';
  });
  html += '</div><div class="flex gap-14 text-sm muted" style="margin-top:10px"><span><span style="display:inline-block;width:9px;height:9px;background:var(--blue);border-radius:2px;margin-right:5px"></span>Ingresos</span><span><span style="display:inline-block;width:9px;height:9px;background:var(--amber);border-radius:2px;margin-right:5px"></span>Egresos</span></div></div>';

  html += '<div class="card" style="flex:1;min-width:260px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 4px">Usuarios por rol</h3><p class="text-sm muted" style="margin:0 0 14px">Distribución actual</p>';
  Object.entries(roleCounts).forEach(([rol,count]) => {
    const pct = Math.round((count/totalUsuarios)*100);
    html += '<div style="margin-bottom:12px"><div class="flex justify-between mb-4">' + rolePill(rol) + '<span style="font-weight:700">' + count + '</span></div>' +
      '<div class="progress-bar"><div class="progress-fill" style="width:'+pct+'%;background:'+ROLE_COLOR[rol]+'"></div></div></div>';
  });
  html += '</div></div>';

  cont.innerHTML = html;
}
