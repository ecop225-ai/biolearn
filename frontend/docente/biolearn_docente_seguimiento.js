/* ================================================================
   BIOLearn — biolearn_docente_seguimiento.js
   Módulo del prototipo docente unificado (biolearn_docente_prototipo)
================================================================ */

/* ================================================================
   VISTA: ESTUDIANTES
================================================================ */
function renderEstudiantes(cont) {
  let html = '<div class="section-header"><div><h2>Estudiantes</h2><p class="subtitle">Progreso individual de cada estudiante en tus cursos.</p></div>' +
    '<button class="btn btn-ghost" onclick="toast(\'Exportando progreso de estudiantes...\')">⬇ Exportar</button></div>';
  html += '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)"><th style="color:var(--blue)">Estudiante</th><th style="color:var(--blue)">Avance curso</th><th style="color:var(--blue)">Avance lección</th><th style="color:var(--blue)">Nota promedio</th><th style="color:var(--blue)">Foro</th><th style="color:var(--blue)">Actividad</th><th style="color:var(--blue)">Evaluación</th><th style="color:var(--blue)">Estado</th><th style="color:var(--blue)"></th></tr></thead><tbody>';
  DATA.estudiantes.forEach(e => {
    const barColor = e.avanceLeccion >= 70 ? 'var(--sage)' : e.avanceLeccion >= 40 ? 'var(--amber)' : 'var(--clay)';
    const barColorCurso = e.avanceCurso >= 70 ? 'var(--sage)' : e.avanceCurso >= 40 ? 'var(--amber)' : 'var(--clay)';
    const iniciales = e.nombre.split(' ').map(p=>p[0]).slice(0,2).join('');
    html += '<tr' + (e.riesgo ? ' style="background:var(--clay-soft)"' : '') + '>' +
      '<td><div class="flex items-center gap-10"><div style="width:32px;height:32px;border-radius:50%;background:var(--sage-soft);color:var(--sage);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:12.5px">'+esc(iniciales)+'</div><strong>' + esc(e.nombre) + '</strong></div></td>' +
      '<td><div class="flex items-center gap-8"><div style="width:70px;height:8px;border-radius:6px;background:var(--bone-deep);overflow:hidden;display:inline-block"><div style="width:'+(e.avanceCurso||0)+'%;height:100%;background:'+barColorCurso+'"></div></div><span style="font-weight:700;color:'+barColorCurso+'">' + (e.avanceCurso||0) + '%</span></div></td>' +
      '<td><div class="flex items-center gap-8"><div style="width:70px;height:8px;border-radius:6px;background:var(--bone-deep);overflow:hidden;display:inline-block"><div style="width:'+e.avanceLeccion+'%;height:100%;background:'+barColor+'"></div></div><span style="font-weight:700;color:'+barColor+'">' + e.avanceLeccion + '%</span></div></td>' +
      '<td style="font-weight:700;color:' + (e.notaPromedio>=4?'var(--sage)':e.notaPromedio>=3?'#8A611E':'var(--clay)') + '">' + e.notaPromedio.toFixed(1) + '</td>' +
      '<td>' + pill(e.foroEstado) + '</td><td>' + pill(e.actividadEstado) + '</td>' +
      '<td>' + (e.evaluacionPct != null ? pill(e.evaluacionPct+'%') : '<span class="muted">—</span>') + '</td>' +
      '<td>' + (e.riesgo ? '<span class="pill pill-clay">⚠️ Riesgo</span>' : '<span class="muted">—</span>') + '</td>' +
      '<td><button class="btn btn-ghost btn-xs" onclick="abrirModalEstudianteDetalle(\''+e.id+'\')">Ver detalle</button></td></tr>';
  });
  if (DATA.estudiantes.length === 0) html += '<tr><td colspan="9" class="empty-state">No hay estudiantes inscritos todavía.</td></tr>';
  html += '</tbody></table></div>';
  cont.innerHTML = html;
  renderModalEstudianteDetalle();
}
function abrirModalEstudianteDetalle(id) {
  const e = DATA.estudiantes.find(x=>x.id===id);
  document.getElementById('ed-id').value = e.id;
  document.getElementById('ed-nombre').textContent = e.nombre;
  document.getElementById('ed-avance').textContent = (e.avanceCurso||0) + '%';
  document.getElementById('ed-actividades').textContent = e.actividadesCompletadas + '/' + e.actividadesTotal;
  document.getElementById('ed-promedio').textContent = e.notaPromedio.toFixed(1);
  document.getElementById('ed-lecciones').innerHTML = e.progresoLecciones.map((l,i) =>
    '<div class="flex items-center gap-14" style="padding:12px 0;' + (i>0?'border-top:1px solid var(--line)':'') + '">' +
    '<div style="flex:1"><div style="font-size:13.5px;font-weight:700">Lección ' + (i+1) + ': ' + esc(l.nombre) + '</div>' +
    '<div style="width:100%;max-width:260px;height:8px;border-radius:6px;background:var(--bone-deep);overflow:hidden;margin-top:6px"><div style="width:'+l.pct+'%;height:100%;background:' + (l.pct>=100?'var(--sage)':l.pct>0?'var(--amber)':'var(--clay)') + '"></div></div></div>' +
    '<span style="font-weight:700;font-size:13px">' + l.pct + '%</span>' + pill(l.estado) + '</div>'
  ).join('');
  document.getElementById('ed-actividades-recientes').innerHTML = e.ultimasActividades.map(a =>
    '<div class="flex items-center gap-10" style="padding:10px 0;border-top:1px solid var(--line)">✅<div><div style="font-size:13.5px;font-weight:700">' + esc(a.titulo) + '</div>' + (a.detalle?'<div class="text-sm muted">'+esc(a.detalle)+'</div>':'') + '</div></div>'
  ).join('');
  abrirModal('modal-estudiante-detalle');
}
function renderModalEstudianteDetalle() {
  if (document.getElementById('modal-estudiante-detalle')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-estudiante-detalle"><div class="modal grande"><div class="modal-header"><h3>Detalle del estudiante — <span id="ed-nombre"></span></h3><button class="modal-close" onclick="cerrarModal(\'modal-estudiante-detalle\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="ed-id">' +
    '<div class="grid-2" style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:20px">' +
    '<div class="card" style="text-align:center"><h2 style="font-size:26px;color:var(--sage)" id="ed-avance"></h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Progreso del curso</div></div>' +
    '<div class="card" style="text-align:center"><h2 style="font-size:26px;color:var(--blue)" id="ed-actividades"></h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Actividades</div></div>' +
    '<div class="card" style="text-align:center"><h2 style="font-size:26px;color:var(--amber)" id="ed-promedio"></h2><div class="text-sm muted" style="text-transform:uppercase;font-weight:700;font-size:11.5px">Promedio</div></div></div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;letter-spacing:.4px;margin-bottom:6px">Progreso por lección</div>' +
    '<div class="card mb-14" id="ed-lecciones"></div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;letter-spacing:.4px;margin-bottom:6px">Últimas actividades</div>' +
    '<div class="card" id="ed-actividades-recientes"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-estudiante-detalle\')">Cerrar</button>' +
    '<button class="btn btn-primary" onclick="cerrarModal(\'modal-estudiante-detalle\');goTo(\'mensajeria\');abrirModalMensajeNuevo();document.getElementById(\'msgn-tipo\').value=\'individual\';actualizarCampoDestinatarioMensaje();msgnSeleccionarEstudiante(document.getElementById(\'ed-nombre\').textContent)">✉ Enviar mensaje</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ================================================================
   VISTA: CERTIFICADOS
================================================================ */
function renderCertificados(cont) {
  let html = '<div class="section-header"><div><h2>Certificados</h2><p class="subtitle">Certificados emitidos a partir de la asistencia a eventos virtuales.</p></div>' +
    '<button class="btn btn-ghost" onclick="abrirModalPlantillaCertificado()">🎨 Personalizar plantilla</button></div>';
  html += '<div class="flex gap-14 flex-wrap">';
  html += '<div style="flex:1.4;min-width:320px"><h3 style="font-size:17px;margin:0 0 12px">Certificados emitidos</h3><div class="tabla-wrap"><table><thead><tr><th>Estudiante</th><th>Origen</th><th>Código</th><th>Emitido</th></tr></thead><tbody>';
  if (DATA.certificados.length === 0) html += '<tr><td colspan="4" class="empty-state">Aún no se han emitido certificados. Se generan desde un evento finalizado en "Eventos".</td></tr>';
  DATA.certificados.forEach(c => {
    html += '<tr><td style="font-weight:600">' + esc(c.receptor) + '</td><td class="text-sm muted">' + esc(c.origen) + '</td>' +
      '<td style="font-family:monospace">' + esc(c.codigo) + '</td><td class="text-sm muted">' + esc(c.emitidoEn) + '</td></tr>';
  });
  html += '</tbody></table></div></div>';
  html += '<div style="flex:1;min-width:280px"><h3 style="font-size:17px;margin:0 0 12px">Verificar certificado</h3><div class="card">' +
    '<div class="campo"><label>Código de validación</label><input type="text" id="cert-verif-codigo" placeholder="Ej: BL-EVT-88441"></div>' +
    '<button class="btn btn-primary w-full" onclick="verificarCertificado()">🔎 Verificar</button>' +
    '<div id="cert-verif-resultado"></div></div></div>';
  html += '</div>';
  cont.innerHTML = html;
  renderModalPlantillaCertificado();
}
function abrirModalPlantillaCertificado() {
  const p = DATA.plantillaCertificado;
  document.getElementById('pc-texto').value = p.textoReconocimiento;
  document.getElementById('pc-firma').value = p.firmaDocente;
  document.getElementById('pc-color').value = p.color || '#C08A3E';
  window._pcFirmaImagen = p.firmaImagen || '';
  window._pcLogoImagen = p.logoImagen || LOGO_BIOLEARN_FULL;
  document.getElementById('pc-firma-img-label').innerHTML = p.firmaImagen ? '<img src="'+p.firmaImagen+'" style="height:34px;vertical-align:middle;margin-left:8px">' : '';
  actualizarPreviewPlantillaCertificado();
  abrirModal('modal-plantilla-certificado');
}
function subirLogoPlantillaCertificado() {
  seleccionarArchivos('image/*', function (archivos) {
    if (archivos.length === 0) return;
    window._pcLogoImagen = archivos[0].dataUrl;
    actualizarPreviewPlantillaCertificado();
    toast('Logo actualizado en la vista previa — recuerda guardar');
  });
}
function restablecerLogoPlantillaCertificado() {
  window._pcLogoImagen = LOGO_BIOLEARN_FULL;
  actualizarPreviewPlantillaCertificado();
}
function subirFirmaPlantillaCertificado() {
  const input = document.createElement('input'); input.type='file'; input.accept='image/*';
  input.onchange = function(){
    const f = input.files[0]; if (!f) return;
    const reader = new FileReader();
    reader.onload = function(e) {
      window._pcFirmaImagen = e.target.result;
      document.getElementById('pc-firma-img-label').innerHTML = '<img src="'+e.target.result+'" style="height:34px;vertical-align:middle;margin-left:8px">';
      actualizarPreviewPlantillaCertificado();
    };
    reader.readAsDataURL(f);
  };
  input.click();
}
function quitarFirmaPlantillaCertificado() {
  window._pcFirmaImagen = '';
  document.getElementById('pc-firma-img-label').innerHTML = '';
  actualizarPreviewPlantillaCertificado();
}
function actualizarPreviewPlantillaCertificado() {
  const p = DATA.plantillaCertificado;
  const color = document.getElementById('pc-color').value;
  const hoy = new Date().toISOString().slice(0,10);
  document.getElementById('pc-preview').style.borderColor = color;
  document.getElementById('pc-preview').style.background = 'linear-gradient(135deg,#fff,'+color+'22)';
  const firmaHtml = window._pcFirmaImagen
    ? '<img src="'+window._pcFirmaImagen+'" style="height:36px;display:block;margin:0 auto 4px">' + esc(document.getElementById('pc-firma').value || 'Firma del docente')
    : esc(document.getElementById('pc-firma').value || 'Firma del docente');
  document.getElementById('pc-preview').innerHTML =
    '<img src="' + (window._pcLogoImagen || p.logoImagen) + '" alt="'+esc(p.marcaInstitucional)+'" style="height:34px;margin-bottom:2px">' +
    '<div style="font-size:10.5px;letter-spacing:1.5px;color:'+color+';margin:14px 0 6px">CERTIFICA QUE</div>' +
    '<div style="font-size:19px;font-weight:800;margin-bottom:10px">[Nombre del estudiante]</div>' +
    '<div style="font-size:13px;color:#5A4A2A;margin-bottom:6px">' + esc(document.getElementById('pc-texto').value) + '</div>' +
    '<div style="font-size:11.5px;color:#8A6A4E;margin-bottom:22px">Duración: 2 horas · Emitido el ' + hoy + '</div>' +
    '<div style="display:flex;justify-content:space-around;font-size:11.5px;color:'+color+';border-top:1px solid '+color+';padding-top:10px">' +
    '<span>' + firmaHtml + '</span><span>' + esc(p.firmaRepresentante) + '</span></div>';
}
function guardarPlantillaCertificado() {
  DATA.plantillaCertificado.textoReconocimiento = document.getElementById('pc-texto').value.trim();
  DATA.plantillaCertificado.firmaDocente = document.getElementById('pc-firma').value.trim();
  DATA.plantillaCertificado.color = document.getElementById('pc-color').value;
  DATA.plantillaCertificado.firmaImagen = window._pcFirmaImagen || '';
  DATA.plantillaCertificado.logoImagen = window._pcLogoImagen || LOGO_BIOLEARN_FULL;
  saveData(); cerrarModal('modal-plantilla-certificado'); toast('Plantilla de certificado actualizada');
}
function renderModalPlantillaCertificado() {
  if (document.getElementById('modal-plantilla-certificado')) return;
  const div = document.createElement('div');
  div.innerHTML =
    '<div class="modal-overlay" id="modal-plantilla-certificado"><div class="modal grande"><div class="modal-header"><h3>🎨 Plantilla del certificado</h3><button class="modal-close" onclick="cerrarModal(\'modal-plantilla-certificado\')">✕</button></div>' +
    '<div class="modal-body"><div class="flex gap-14 flex-wrap">' +
    '<div style="flex:1;min-width:280px">' +
    '<p class="text-sm muted mb-14">El logo, los colores institucionales y la firma del representante de BioLearn los define el administrador para toda la plataforma. Tú puedes personalizar el color de acento, el texto de reconocimiento y tu firma como docente.</p>' +
    '<div class="campo"><label>Elementos definidos por administración</label><div class="card flex items-center gap-10" style="font-size:12.5px;color:var(--ink-soft)"><img src="'+DATA.plantillaCertificado.logoImagen+'" style="height:24px"> Firma institucional: ' + esc(DATA.plantillaCertificado.firmaRepresentante) + '</div></div>' +
    '<div class="campo"><label>Logo del certificado</label><p class="hint" style="margin:0 0 6px">Administración definió el logo institucional. Puedes reemplazarlo por uno propio si tu institución lo permite.</p><div class="flex items-center gap-10"><button type="button" class="btn btn-ghost btn-sm" onclick="subirLogoPlantillaCertificado()">⬆ Subir logo</button><button type="button" class="btn btn-ghost btn-sm" onclick="restablecerLogoPlantillaCertificado()">Usar el institucional</button></div></div>' +
    '<div class="campo"><label>Color de acento del certificado</label><input type="color" id="pc-color" style="height:42px" oninput="actualizarPreviewPlantillaCertificado()"></div>' +
    '<div class="campo"><label>Texto de reconocimiento</label><textarea id="pc-texto" rows="2" oninput="actualizarPreviewPlantillaCertificado()"></textarea></div>' +
    '<div class="campo"><label>Tu firma y cargo (texto)</label><input type="text" id="pc-firma" oninput="actualizarPreviewPlantillaCertificado()" placeholder="Ej: Prof. Laura Restrepo — Docente titular"></div>' +
    '<div class="campo"><label>Firma escaneada (opcional)</label><div class="flex items-center gap-10"><button type="button" class="btn btn-ghost btn-sm" onclick="subirFirmaPlantillaCertificado()">⬆ Subir imagen de firma</button><button type="button" class="btn btn-ghost btn-sm" onclick="quitarFirmaPlantillaCertificado()">Quitar</button><span id="pc-firma-img-label"></span></div></div>' +
    '<p class="hint">La fecha de emisión y la duración del evento se calculan automáticamente para cada certificado.</p>' +
    '</div>' +
    '<div style="flex:1;min-width:280px"><label class="text-sm" style="font-weight:700;display:block;margin-bottom:8px">Vista previa</label>' +
    '<div style="border:1.5px solid #E0B94A;border-radius:14px;padding:26px;text-align:center" id="pc-preview"></div></div>' +
    '</div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-plantilla-certificado\')">Cancelar</button><button class="btn btn-primary" onclick="guardarPlantillaCertificado()">Guardar plantilla</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function verificarCertificado() {
  const codigo = document.getElementById('cert-verif-codigo').value.trim().toLowerCase();
  const cert = DATA.certificados.find(c => c.codigo.toLowerCase() === codigo);
  const cont = document.getElementById('cert-verif-resultado');
  if (!cert) {
    cont.innerHTML = '<p class="text-sm" style="color:var(--clay);margin-top:14px">No se encontró ningún certificado con ese código.</p>';
  } else {
    cont.innerHTML = '<div style="margin-top:14px;background:var(--sage-soft);border-radius:10px;padding:14px">' +
      '<div class="text-sm" style="font-weight:700;color:var(--sage);margin-bottom:6px">✓ Certificado válido</div>' +
      '<div class="text-sm">' + esc(cert.receptor) + ' · ' + esc(cert.origen) + '</div>' +
      '<div class="text-sm muted">Emitido el ' + esc(cert.emitidoEn) + '</div></div>';
  }
}

/* ================================================================
   VISTA: REPORTES
================================================================ */
const REPORTE_TIPOS = [
  { value:'notas', label:'Notas y promedios por estudiante', icon:'📝' },
  { value:'asistencia', label:'Asistencia a sesiones en línea', icon:'✅' },
  { value:'progreso', label:'Progreso por lección', icon:'📈' },
  { value:'actividades', label:'Entregas y calificaciones de actividades', icon:'✏️' },
  { value:'evaluaciones', label:'Resultados de evaluaciones', icon:'📋' },
  { value:'foros', label:'Participación en foros', icon:'💬' },
  { value:'general', label:'Reporte general del curso', icon:'📊' },
];
function renderReportes(cont) {
  let html = '<div class="section-header"><div><h2>Reportes</h2><p class="subtitle">Genera reportes de notas, asistencia, progreso, actividades, evaluaciones o foros.</p></div></div>';
  html += '<div class="card mb-14"><div class="flex items-center gap-8 mb-14">🔎<h3 style="font-size:16.5px;margin:0">Configurar reporte</h3></div>';
  html += '<div class="grid-2"><div class="campo"><label>Tipo *</label><select id="rep-tipo"><option value="">-- Selecciona el tipo --</option>' +
    REPORTE_TIPOS.map(t=>'<option value="'+t.value+'">'+t.icon+' '+t.label+'</option>').join('') + '</select></div>' +
    '<div class="campo"><label>Curso</label><select id="rep-curso"><option value="">-- Selecciona --</option>' +
    DATA.cursos.map(c=>'<option value="'+c.id+'">'+esc(c.nombre)+' — '+esc(c.grado)+' ('+c.inscritos+' estudiantes)</option>').join('') +
    '<option value="todos">Todos mis cursos</option></select></div></div>';
  html += '<div class="campo"><label>Período</label><div class="flex items-center gap-10"><input type="date" id="rep-desde"><span class="text-sm muted">hasta</span><input type="date" id="rep-hasta"></div></div>';
  html += '<div class="grid-2"><div class="campo"><label>Fecha de corte</label><input type="date" id="rep-corte"></div>' +
    '<div class="campo"><label>Formato</label><div class="flex gap-14" style="padding-top:8px">' +
    '<label class="flex items-center gap-6 text-sm" style="cursor:pointer"><input type="radio" name="rep-formato" value="pdf" checked style="width:auto"> 📄 PDF</label>' +
    '<label class="flex items-center gap-6 text-sm" style="cursor:pointer"><input type="radio" name="rep-formato" value="excel" style="width:auto"> 📊 Excel</label></div></div></div>';
  html += '<div class="campo"><label>Motivo del reporte</label><input type="text" id="rep-motivo" placeholder="Ej: Seguimiento de fin de período"></div>';
  html += '<button class="btn btn-primary" onclick="generarReporte()">📊 Generar reporte</button></div>';
  html += '<h3 style="font-size:17px;margin:0 0 12px">Reportes generados</h3><div class="card" style="padding:0">';
  if (DATA.reportesGenerados.length === 0) html += '<div class="empty-state">Aún no has generado reportes.</div>';
  DATA.reportesGenerados.forEach((r,i) => {
    html += '<div class="flex justify-between items-center" style="padding:14px 18px;' + (i>0?'border-top:1px solid var(--line)':'') + '">' +
      '<div><div style="font-size:14px;font-weight:600">' + esc(r.tipo) + ' · ' + esc(r.curso) + '</div><div class="text-sm muted">' + r.fecha + ' · ' + r.formato + '</div></div>' +
      '<button class="btn btn-ghost btn-sm" onclick="toast(\'Descargando reporte...\')">Descargar</button></div>';
  });
  html += '</div>';
  cont.innerHTML = html;
}
function generarReporte() {
  const tipoVal = document.getElementById('rep-tipo').value, cursoVal = document.getElementById('rep-curso').value;
  if (!tipoVal || !cursoVal) { toast('Selecciona el tipo de reporte y el curso', true); return; }
  const tipoLabel = REPORTE_TIPOS.find(t=>t.value===tipoVal).label;
  const cursoNombre = cursoVal === 'todos' ? 'Todos mis cursos' : DATA.cursos.find(c=>c.id===cursoVal).nombre;
  const formato = document.querySelector('input[name="rep-formato"]:checked').value.toUpperCase();
  DATA.reportesGenerados.unshift({ id: uid('rep'), tipo: tipoLabel, curso: cursoNombre, formato, fecha: new Date().toISOString().slice(0,10) });
  saveData(); render(); toast('Reporte de "' + tipoLabel + '" generado en ' + formato + ' — listo para descargar');
}
