/* ================================================================
   BIOLearn — biolearn_supervisor_reportes.js
   Reportes + verificación de certificados, Mi institución
================================================================ */
const TIPOS_REPORTE_SUP = [
  '🏫 Informe de supervisión institucional', '📈 Rendimiento académico por curso', '🧑\u200d🏫 Evaluación de docentes',
  '👥 Progreso de estudiantes', '📊 Comparativo entre períodos',
];
function renderReportes(cont) {
  const cursos = [...new Set(DATA.estudiantes.map(e=>e.curso))];
  let html = '<div class="section-header"><div><h2>Reportes</h2><p class="subtitle">Genera reportes de seguimiento y verifica certificados de tus estudiantes</p></div></div>';
  html += '<div class="flex gap-18 flex-wrap" style="align-items:flex-start">';
  html += '<div style="flex:1.3;min-width:320px">';
  html += '<div class="card mb-20"><div class="flex items-center gap-8 mb-14">🔍<h3 style="font-family:\'Fraunces\',serif;font-size:16.5px;margin:0">Configurar reporte</h3></div>' +
    '<div class="grid-2"><div class="campo"><label>Tipo *</label><select id="rep-tipo-sup"><option value="">-- Selecciona --</option>' + TIPOS_REPORTE_SUP.map(t=>'<option value="'+t+'">'+t+'</option>').join('') + '</select></div>' +
    '<div class="campo"><label>Curso</label><select id="rep-curso-sup"><option value="">-- Selecciona --</option>' + cursos.map(c=>'<option value="'+esc(c)+'">'+esc(c)+'</option>').join('') + '</select></div></div>' +
    '<div class="campo"><label>Período</label><div class="flex items-center gap-10"><input type="date" id="rep-desde-sup"><span class="text-sm muted">hasta</span><input type="date" id="rep-hasta-sup"></div></div>' +
    '<div class="grid-2"><div class="campo"><label>Fecha de corte</label><input type="date" id="rep-corte-sup"></div>' +
    '<div class="campo"><label>Formato</label><div class="flex gap-18" style="padding-top:10px"><label class="flex items-center gap-6 text-sm"><input type="radio" name="rep-formato-sup" value="pdf" checked style="width:auto">PDF</label><label class="flex items-center gap-6 text-sm"><input type="radio" name="rep-formato-sup" value="excel" style="width:auto">EXCEL</label></div></div></div>' +
    '<div class="campo"><label>Motivo</label><input id="rep-motivo-sup" placeholder="Ej: Seguimiento trimestral"></div>' +
    '<button class="btn btn-primary" onclick="generarReporteSup()">Descargar reporte</button></div>';
  html += '<h3 style="font-family:\'Fraunces\',serif;font-size:16px;margin:0 0 12px">Reportes generados</h3><div class="card" style="padding:0">';
  if (DATA.reportesGenerados.length === 0) html += '<div class="empty-state">Aún no has generado reportes.</div>';
  DATA.reportesGenerados.forEach((r,i) => {
    html += '<div class="flex justify-between items-center" style="padding:14px 18px;'+(i>0?'border-top:1px solid var(--line)':'')+'"><div><div style="font-size:14px;font-weight:600">' + esc(r.tipo) + ' · ' + esc(r.curso) + '</div><div class="text-sm muted">' + r.fecha + ' · ' + r.formato + (r.periodo?' · '+esc(r.periodo):'') + '</div></div>' +
      '<button class="btn btn-ghost btn-sm" onclick="toast(\'Descargando reporte...\')">⬇ Descargar</button></div>';
  });
  html += '</div></div>';

  html += '<div style="flex:1;min-width:280px"><div class="card"><h3 style="font-family:\'Fraunces\',serif;font-size:16px;margin:0 0 12px">Verificar certificado</h3>' +
    '<div class="campo"><label>Código de validación</label><input id="cert-verif-sup" placeholder="Ej: BIO-CERT-77621"></div>' +
    '<button class="btn btn-primary w-full" onclick="verificarCertificadoSup()">🛡 Verificar</button><div id="cert-verif-resultado-sup"></div></div></div>';
  html += '</div>';
  cont.innerHTML = html;
}
function generarReporteSup() {
  const tipo = document.getElementById('rep-tipo-sup').value, curso = document.getElementById('rep-curso-sup').value;
  if (!tipo || !curso) { toast('Selecciona el tipo de reporte y el curso', true); return; }
  const desde = document.getElementById('rep-desde-sup').value, hasta = document.getElementById('rep-hasta-sup').value;
  const formato = document.querySelector('input[name="rep-formato-sup"]:checked').value.toUpperCase();
  DATA.reportesGenerados.unshift({ id:uid('rep'), tipo, curso, periodo: desde&&hasta ? desde+' a '+hasta : '', formato, fecha:new Date().toISOString().slice(0,10) });
  saveData(); render();
  toast('Reporte de "' + tipo + '" generado en ' + formato);
}
function verificarCertificadoSup() {
  const codigo = document.getElementById('cert-verif-sup').value.trim().toLowerCase();
  const c = DATA.certificadosRegistro.find(x=>x.codigo.toLowerCase()===codigo);
  const cont = document.getElementById('cert-verif-resultado-sup');
  if (!c) cont.innerHTML = '<p class="text-sm" style="color:var(--clay);margin-top:14px">No se encontró ningún certificado con ese código.</p>';
  else cont.innerHTML = '<div style="margin-top:14px;background:var(--sage-soft);border-radius:10px;padding:14px"><div class="text-sm" style="font-weight:700;color:var(--sage);margin-bottom:6px">🛡 Certificado válido</div><div class="text-sm">' + esc(c.receptor) + '</div><div class="text-sm" style="margin:2px 0">' + esc(c.origen) + '</div><div class="text-sm muted">Emitido el ' + esc(c.emitidoEn) + '</div></div>';
}

/* ---------------------------- Mi institución ---------------------------- */
function renderInstitucion(cont) {
  const i = DATA.institucion;
  let html = '<div class="section-header"><div><h2>Mi institución</h2><p class="subtitle">Datos registrados por administración de BioLearn</p></div></div>';
  html += '<div class="flex gap-18 flex-wrap">';
  html += '<div class="card" style="flex:1.3;min-width:320px"><div class="flex items-center gap-14 mb-18">' +
    '<div style="width:52px;height:52px;border-radius:12px;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;font-size:24px">🏫</div>' +
    '<div><h3 style="font-family:\'Fraunces\',serif;font-size:19px;margin:0">' + esc(i.nombre) + '</h3><p class="text-sm muted" style="margin:2px 0 0">NIT ' + esc(i.nit) + '</p></div></div>' +
    '<div class="text-sm" style="display:flex;flex-direction:column;gap:12px">' +
    '<div>🎓 Rector(a): <strong>' + esc(i.rector) + '</strong></div>' +
    '<div>📞 ' + esc(i.telefono) + '</div>' +
    '<div>✉️ ' + esc(i.correo) + '</div>' +
    '<div>📍 ' + esc(i.direccion) + '</div></div></div>';
  html += '<div class="card" style="flex:1;min-width:260px">' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:8px">Código de afiliación</div>' +
    '<div style="background:var(--amber-soft);border-radius:10px;padding:16px;text-align:center;margin-bottom:18px">' +
    '<div style="font-family:monospace;font-weight:800;font-size:22px;color:var(--amber);letter-spacing:2px">' + esc(i.codigoAfiliacion) + '</div>' +
    '<p class="text-sm muted" style="margin:6px 0 0">Los estudiantes lo usan para afiliarse a esta institución al registrarse</p></div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:8px">Supervisores asignados</div>';
  i.supervisores.forEach((s,idx) => {
    html += '<div class="flex items-center gap-8 text-sm" style="padding:8px 0;'+(idx>0?'border-top:1px solid var(--line)':'')+'">✓ ' + esc(s) + (s===SUPERVISOR.nombre ? ' <span style="font-size:11px;background:var(--blue-soft);color:var(--blue-deep);padding:1px 8px;border-radius:12px">Tú</span>' : '') + '</div>';
  });
  html += '<p class="text-sm muted" style="margin-top:16px">' + i.estudiantesAfiliados + ' estudiante(s) afiliado(s) · registrada el ' + fmtDate(i.fechaRegistro) + '</p></div>';
  html += '</div>';
  cont.innerHTML = html;
}
