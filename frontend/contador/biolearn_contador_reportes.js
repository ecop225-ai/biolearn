/* ================================================================
   BIOLearn — biolearn_contador_reportes.js
================================================================ */
const TIPOS_REPORTE_CONT = ['💰 Ingresos del período', '🧾 Estado de facturas', '↩️ Reembolsos procesados', '📊 Reporte financiero general'];
function renderReportes(cont) {
  let html = '<div class="section-header"><div><h2>Reportes</h2><p class="subtitle">Genera resúmenes financieros de ingresos, facturas y reembolsos</p></div></div>';
  html += '<div class="card mb-24"><div class="flex items-center gap-8 mb-18">🔍<h3 style="font-family:\'Fraunces\',serif;font-size:16.5px;margin:0">Configurar reporte</h3></div>' +
    '<div class="campo"><label>Tipo *</label><select id="cont-rep-tipo"><option value="">-- Selecciona --</option>' + TIPOS_REPORTE_CONT.map(t=>'<option value="'+t+'">'+t+'</option>').join('') + '</select></div>' +
    '<div class="campo"><label>Período</label><div class="flex items-center gap-10"><input type="date" id="cont-rep-desde"><span class="text-sm muted">hasta</span><input type="date" id="cont-rep-hasta"></div></div>' +
    '<div class="grid-2"><div class="campo"><label>Fecha de corte</label><input type="date" id="cont-rep-corte"></div>' +
    '<div class="campo"><label>Formato</label><div class="flex gap-18" style="padding-top:10px"><label class="flex items-center gap-6 text-sm"><input type="radio" name="cont-rep-formato" value="pdf" checked style="width:auto">PDF</label><label class="flex items-center gap-6 text-sm"><input type="radio" name="cont-rep-formato" value="excel" style="width:auto">EXCEL</label></div></div></div>' +
    '<div class="campo"><label>Motivo</label><input id="cont-rep-motivo" placeholder="Ej: Cierre contable mensual"></div>' +
    '<button class="btn btn-primary" onclick="generarReporteCont()">📊 Generar reporte</button></div>';
  html += '<h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 12px">Reportes generados</h3><div class="card" style="padding:0">';
  if (DATA.reportesGenerados.length === 0) html += '<div class="empty-state">Aún no has generado reportes.</div>';
  DATA.reportesGenerados.forEach((r,i) => {
    html += '<div class="flex justify-between items-center" style="padding:14px 18px;'+(i>0?'border-top:1px solid var(--line)':'')+'"><div><div style="font-size:14px;font-weight:600">' + esc(r.tipo) + '</div><div class="text-sm muted">' + r.fecha + ' · ' + r.formato + (r.periodo?' · '+esc(r.periodo):'') + '</div></div>' +
      '<button class="btn btn-ghost btn-sm" onclick="toast(\'Descargando reporte...\')">⬇ Descargar</button></div>';
  });
  html += '</div>';
  cont.innerHTML = html;
}
function generarReporteCont() {
  const tipo = document.getElementById('cont-rep-tipo').value;
  if (!tipo) { toast('Selecciona el tipo de reporte', true); return; }
  const desde = document.getElementById('cont-rep-desde').value, hasta = document.getElementById('cont-rep-hasta').value;
  const formato = document.querySelector('input[name="cont-rep-formato"]:checked').value.toUpperCase();
  DATA.reportesGenerados.unshift({ id:uid('rep'), tipo, periodo: desde&&hasta ? desde+' a '+hasta : '', formato, fecha:new Date().toISOString().slice(0,10) });
  saveData(); render();
  toast('Reporte de "' + tipo + '" generado en ' + formato);
}
