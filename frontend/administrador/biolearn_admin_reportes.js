/* ================================================================
   BIOLearn — biolearn_admin_reportes.js
================================================================ */
const REPORT_TYPES = [
  { value:'usuarios', label:'Usuarios registrados por período', icon:'👥' },
  { value:'ingresos', label:'Ingresos y pagos del sistema', icon:'💰' },
  { value:'actividad', label:'Actividad por curso e institución', icon:'📊' },
  { value:'docentes', label:'Rendimiento de docentes', icon:'🎓' },
  { value:'institucion', label:'Reporte por institución', icon:'🏫' },
  { value:'general', label:'Reporte general de la plataforma', icon:'📈' },
];
let REP_TAB = 'academico';

function renderReportBuilder() {
  return '<div class="card mb-20"><div class="flex items-center gap-8 mb-14">🔍<h3 style="font-family:\'Fraunces\',serif;font-size:16.5px;margin:0">Configurar reporte</h3></div>' +
    '<div class="campo"><label>Tipo *</label><select id="rep-tipo"><option value="">-- Selecciona --</option>' + REPORT_TYPES.map(t=>'<option value="'+t.value+'">'+t.icon+' '+t.label+'</option>').join('') + '</select></div>' +
    '<div class="campo"><label>Institución</label><select id="rep-institucion"><option value="">Todas las instituciones</option>' + DATA.instituciones.map(i=>'<option value="'+i.id+'">'+esc(i.nombre)+'</option>').join('') + '</select></div>' +
    '<div class="campo"><label>Período</label><div class="flex items-center gap-10"><input type="date" id="rep-desde"><span class="text-sm muted">hasta</span><input type="date" id="rep-hasta"></div></div>' +
    '<div class="grid-2"><div class="campo"><label>Fecha de corte</label><input type="date" id="rep-corte"></div>' +
    '<div class="campo"><label>Formato</label><div class="flex gap-18" style="padding-top:10px">' +
    '<label class="flex items-center gap-6 text-sm"><input type="radio" name="rep-formato" value="pdf" checked style="width:auto">📄 PDF</label>' +
    '<label class="flex items-center gap-6 text-sm"><input type="radio" name="rep-formato" value="excel" style="width:auto">📊 Excel</label>' +
    '<label class="flex items-center gap-6 text-sm"><input type="radio" name="rep-formato" value="csv" style="width:auto">📄 CSV</label></div></div></div>' +
    '<div class="campo"><label>Motivo</label><input id="rep-motivo" placeholder="Ej: Reporte trimestral para rectoría"></div>' +
    '<div class="flex gap-10"><button class="btn btn-primary" onclick="generarReporteAdmin()">📊 Generar y descargar</button><button class="btn btn-ghost" onclick="enviarReporteCorreoAdmin()">✉ Enviar por correo</button></div></div>';
}
function generarReporteAdmin() {
  const tipo = document.getElementById('rep-tipo').value;
  if (!tipo) { toast('Selecciona un tipo de reporte', true); return; }
  const tipoInfo = REPORT_TYPES.find(t=>t.value===tipo);
  const instId = document.getElementById('rep-institucion').value;
  const inst = instId ? DATA.instituciones.find(i=>i.id===instId).nombre : 'todas las instituciones';
  const formato = document.querySelector('input[name="rep-formato"]:checked').value.toUpperCase();
  DATA.reportesGenerados.unshift({ id:uid('rep'), nombre:tipoInfo.label, formato, fecha:new Date().toISOString().slice(0,10) });
  saveData();
  toast('Generando "' + tipoInfo.label + '" (' + inst + ') en ' + formato + '...');
}
function enviarReporteCorreoAdmin() {
  const tipo = document.getElementById('rep-tipo').value;
  if (!tipo) { toast('Selecciona un tipo de reporte', true); return; }
  toast('"' + REPORT_TYPES.find(t=>t.value===tipo).label + '" se enviará por correo en cuanto esté listo');
}
function cambiarTabReportes(tab) { REP_TAB = tab; render(); }
function descargarDocReporte(nombre) { toast('Generando "' + nombre + '"... listo para descargar'); }

function renderReportes(cont) {
  let html = '<div class="section-header"><div><h2>Reportes</h2><p class="subtitle">Configura y genera reportes académicos y financieros de toda la plataforma</p></div></div>';
  html += renderReportBuilder();
  html += '<div class="flex gap-8 mb-20">' +
    '<button onclick="cambiarTabReportes(\'academico\')" style="padding:8px 16px;border-radius:9px;border:1px solid '+(REP_TAB==='academico'?'var(--blue)':'var(--line)')+';background:'+(REP_TAB==='academico'?'var(--blue-soft)':'transparent')+';color:'+(REP_TAB==='academico'?'var(--blue)':'var(--ink-soft)')+';font-weight:600;font-size:13.5px;cursor:pointer">Académico</button>' +
    '<button onclick="cambiarTabReportes(\'financiero\')" style="padding:8px 16px;border-radius:9px;border:1px solid '+(REP_TAB==='financiero'?'var(--amber)':'var(--line)')+';background:'+(REP_TAB==='financiero'?'var(--amber-soft)':'transparent')+';color:'+(REP_TAB==='financiero'?'var(--amber)':'var(--ink-soft)')+';font-weight:600;font-size:13.5px;cursor:pointer">Financiero</button></div>';

  if (REP_TAB === 'academico') {
    html += '<div class="kpi-row">' +
      '<div class="kpi-card" style="border-left-color:var(--blue)"><div class="text-sm muted">Promedio de asistencia</div><div class="kpi-value">87%</div></div>' +
      '<div class="kpi-card" style="border-left-color:var(--sage)"><div class="text-sm muted">Tasa de aprobación</div><div class="kpi-value">91%</div></div>' +
      '<div class="kpi-card" style="border-left-color:var(--amber)"><div class="text-sm muted">Certificados emitidos</div><div class="kpi-value">214</div></div></div>';
    html += '<div class="card"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">Documentos disponibles</h3>';
    ['Reporte de calificaciones por curso — septiembre','Reporte de asistencia a eventos virtuales — septiembre','Reporte de progreso académico por institución'].forEach((r,i) => {
      html += '<div class="flex justify-between items-center" style="padding:12px 0;'+(i>0?'border-top:1px solid var(--line)':'')+'"><span style="font-size:14px">' + r + '</span><button class="btn btn-ghost btn-sm" onclick="descargarDocReporte(\''+r.replace(/'/g,"\\'")+'\')">⬇ Descargar</button></div>';
    });
    html += '</div>';
  } else {
    html += '<div class="kpi-row">' +
      '<div class="kpi-card" style="border-left-color:var(--amber)"><div class="text-sm muted">Ingresos septiembre</div><div class="kpi-value">$14.8M</div><div class="text-sm" style="color:var(--blue);font-weight:700">↑ 9.6%</div></div>' +
      '<div class="kpi-card" style="border-left-color:var(--clay)"><div class="text-sm muted">Egresos septiembre</div><div class="kpi-value">$4.9M</div></div>' +
      '<div class="kpi-card" style="border-left-color:var(--sage)"><div class="text-sm muted">Reembolsos procesados</div><div class="kpi-value">6</div></div></div>';
    html += '<div class="card mb-18">';
    const maxMonto = Math.max(...DATA.ingresosMensuales.map(m=>m.ingresos),1);
    html += '<div class="flex items-end gap-14" style="height:180px">';
    DATA.ingresosMensuales.forEach(m => {
      html += '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;height:100%"><div class="flex items-end gap-4" style="height:100%;width:100%;justify-content:center">' +
        '<div style="width:38%;border-radius:5px 5px 0 0;background:var(--amber);height:'+((m.ingresos/maxMonto)*100)+'%"></div>' +
        '<div style="width:38%;border-radius:5px 5px 0 0;background:var(--clay);height:'+((m.egresos/maxMonto)*100)+'%"></div></div>' +
        '<span class="text-sm muted" style="margin-top:6px">' + m.mes + '</span></div>';
    });
    html += '</div></div>';
    html += '<div class="card"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">Documentos disponibles</h3>';
    ['Estado de cuenta general — septiembre','Facturación por institución','Reembolsos aprobados y pendientes'].forEach((r,i) => {
      html += '<div class="flex justify-between items-center" style="padding:12px 0;'+(i>0?'border-top:1px solid var(--line)':'')+'"><span style="font-size:14px">' + r + '</span><button class="btn btn-ghost btn-sm" onclick="descargarDocReporte(\''+r.replace(/'/g,"\\'")+'\')">⬇ Descargar</button></div>';
    });
    html += '</div>';
  }
  cont.innerHTML = html;
}
