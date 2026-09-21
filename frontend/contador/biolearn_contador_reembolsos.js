/* ================================================================
   BIOLearn — biolearn_contador_reembolsos.js
================================================================ */
let REEM_FILTRO = 'Todos';
const REEM_FILTRO_ESTADO = { Todos:null, Solicitados:'solicitado', Aprobados:'aprobado', Rechazados:'rechazado' };

function renderReembolsos(cont) {
  const mesActual = new Date().getMonth();
  const solicitados = DATA.reembolsos.filter(r=>r.estado==='solicitado');
  const aprobadosMes = DATA.reembolsos.filter(r=>r.estado==='aprobado' && r.fechaResolucion && new Date(r.fechaResolucion).getMonth()===mesActual);
  const rechazadosMes = DATA.reembolsos.filter(r=>r.estado==='rechazado' && r.fechaResolucion && new Date(r.fechaResolucion).getMonth()===mesActual);
  const montoDevueltoMes = aprobadosMes.reduce((s,r)=>s+(r.montoDevuelto||0),0);
  const filtrados = DATA.reembolsos.filter(r => !REEM_FILTRO_ESTADO[REEM_FILTRO] || r.estado===REEM_FILTRO_ESTADO[REEM_FILTRO]);
  const pendientesF = filtrados.filter(r=>r.estado==='solicitado'), resueltosF = filtrados.filter(r=>r.estado!=='solicitado');

  let html = '<div class="section-header"><div><h2>Gestión de reembolsos</h2><p class="subtitle">Aprueba o rechaza las solicitudes de reembolso de los estudiantes</p></div>' +
    '<div class="flex gap-8"><select onchange="REEM_FILTRO=this.value;render()" style="width:150px">' + Object.keys(REEM_FILTRO_ESTADO).map(f=>'<option'+(REEM_FILTRO===f?' selected':'')+'>'+f+'</option>').join('') + '</select>' +
    '<button class="btn btn-ghost" onclick="toast(\'Exportando reembolsos...\')">⬇ Exportar</button></div></div>';

  html += '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:26px">' +
    '<div class="card"><div style="font-size:22px;margin-bottom:8px">⏳</div><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">Solicitados</div><div style="font-family:\'Fraunces\',serif;font-size:26px;color:var(--amber)">' + solicitados.length + '</div><div class="text-sm muted">Pendientes de revisión</div></div>' +
    '<div class="card"><div style="width:32px;height:32px;border-radius:8px;background:var(--sage-soft);display:flex;align-items:center;justify-content:center;margin-bottom:8px;color:var(--sage)">' + iconSvg('check') + '</div><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">Aprobados este mes</div><div style="font-family:\'Fraunces\',serif;font-size:26px">' + aprobadosMes.length + '</div><div class="text-sm muted">' + fmtMoney(montoDevueltoMes) + ' devueltos</div></div>' +
    '<div class="card"><div style="width:32px;height:32px;border-radius:8px;background:var(--clay-soft);display:flex;align-items:center;justify-content:center;margin-bottom:8px;color:var(--clay)">✕</div><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">Rechazados</div><div style="font-family:\'Fraunces\',serif;font-size:26px">' + rechazadosMes.length + '</div><div class="text-sm muted">Este mes</div></div></div>';

  if (pendientesF.length > 0) {
    html += '<div class="text-sm mb-10" style="font-weight:700;text-transform:uppercase;color:var(--amber)">Solicitudes pendientes de revisión</div>';
    pendientesF.forEach(r => {
      const dias = Math.max(1, Math.round((Date.now()-new Date(r.fechaSolicitud))/86400000));
      html += '<div class="card mb-14" style="border-left:4px solid var(--amber)"><div class="flex justify-between items-start mb-14">' +
        '<div><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 4px">' + esc(r.estudiante) + ' — ' + esc(r.referencia) + '</h3><p class="text-sm muted" style="margin:0">Solicitado el ' + fmtDate(r.fechaSolicitud) + '</p></div>' +
        '<div style="text-align:right"><div style="font-weight:800;font-size:18px">' + fmtMoney(r.monto) + '</div>' + pill('solicitado') + '<div class="text-sm muted" style="margin-top:2px">hace ' + dias + ' día(s)</div></div></div>' +
        '<div style="display:grid;grid-template-columns:140px 1fr;row-gap:8px;font-size:13.5px;margin-bottom:16px">' +
        '<div style="font-weight:700;color:var(--ink-soft)">Pago original</div><div>' + esc(r.referencia) + ' · ' + fmtMoney(r.monto) + ' · ' + fmtDate((DATA.pagos.find(p=>p.id===r.pagoId)||{}).fecha) + '</div>' +
        '<div style="font-weight:700;color:var(--ink-soft)">Curso</div><div>' + esc(r.curso) + '</div>' +
        '<div style="font-weight:700;color:var(--ink-soft)">Método pago</div><div>' + esc(r.metodoPago) + '</div>' +
        '<div style="font-weight:700;color:var(--ink-soft)">Motivo</div><div>"' + esc(r.motivo) + '"</div></div>' +
        '<div class="flex gap-10" style="justify-content:flex-end"><button class="btn btn-ghost" onclick="abrirModalVerPagoReem(\''+r.pagoId+'\')">Ver pago</button>' +
        '<button class="btn btn-danger" onclick="abrirResolucionReembolso(\''+r.id+'\',\'rechazar\')">✕ Rechazar</button>' +
        '<button class="btn" style="background:var(--sage);color:#fff" onclick="abrirResolucionReembolso(\''+r.id+'\',\'aprobar\')">✓ Aprobar reembolso</button></div></div>';
    });
  }
  if (resueltosF.length > 0) {
    html += '<div class="text-sm mb-10" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft)">Reembolsos resueltos</div>';
    resueltosF.forEach(r => {
      html += '<div class="card mb-14" style="border-left:4px solid ' + (r.estado==='aprobado'?'var(--sage)':'var(--clay)') + '"><div class="flex justify-between items-start mb-12">' +
        '<div><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 4px">' + esc(r.estudiante) + ' — ' + esc(r.referencia) + '</h3><p class="text-sm muted" style="margin:0">' + (r.estado==='aprobado'?'Aprobado':'Rechazado') + ' el ' + fmtDate(r.fechaResolucion) + ' · Por: ' + esc(r.aprobadoPor) + '</p></div>' +
        '<div style="text-align:right"><div style="font-weight:800;font-size:18px">' + fmtMoney(r.monto) + '</div>' + pill(r.estado) + '</div></div>' +
        '<div style="display:grid;grid-template-columns:140px 1fr;row-gap:8px;font-size:13.5px">' +
        '<div style="font-weight:700;color:var(--ink-soft)">Justificación</div><div>"' + esc(r.justificacion) + '"</div>' +
        (r.estado==='aprobado' ? '<div style="font-weight:700;color:var(--ink-soft)">Monto devuelto</div><div style="color:var(--sage);font-weight:700">' + fmtMoney(r.montoDevuelto) + ' (reembolso total)</div>' : '') +
        '<div style="font-weight:700;color:var(--ink-soft)">Resuelto en</div><div>' + new Date(r.fechaResolucion).toLocaleString('es-CO',{day:'2-digit',month:'short',year:'numeric',hour:'numeric',minute:'2-digit'}) + '</div></div></div>';
    });
  }
  if (pendientesF.length===0 && resueltosF.length===0) html += '<div class="empty-state">No hay reembolsos en este filtro.</div>';
  cont.innerHTML = html;
  renderModalVerPagoReem(); renderModalResolverReembolso();
}
function abrirModalVerPagoReem(pagoId) {
  const p = DATA.pagos.find(x=>x.id===pagoId);
  if (!p) return;
  document.getElementById('vpr-cuerpo').innerHTML = 'Estudiante: <strong>' + esc(p.estudiante) + '</strong><br>Curso: ' + esc(p.curso) + '<br>Referencia: ' + esc(p.referencia) + '<br>Monto: <strong>' + fmtMoney(p.monto) + '</strong> · ' + esc(p.metodo) + '<br>Fecha: ' + fmtDate(p.fecha);
  abrirModal('modal-ver-pago-reem');
}
function renderModalVerPagoReem() {
  if (document.getElementById('modal-ver-pago-reem')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-ver-pago-reem"><div class="modal" style="max-width:400px"><div class="modal-header"><h3>Detalle del pago</h3><button class="modal-close" onclick="cerrarModal(\'modal-ver-pago-reem\')">✕</button></div>' +
    '<div class="modal-body"><p style="font-size:14px;line-height:1.8" id="vpr-cuerpo"></p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-ver-pago-reem\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function abrirResolucionReembolso(id, accion) {
  document.getElementById('resr-id').value = id;
  document.getElementById('resr-accion').value = accion;
  const r = DATA.reembolsos.find(x=>x.id===id);
  document.getElementById('resr-titulo').textContent = accion==='aprobar' ? 'Aprobar reembolso' : 'Rechazar reembolso';
  document.getElementById('resr-resumen').textContent = r.estudiante + ' · ' + fmtMoney(r.monto);
  document.getElementById('resr-aviso').style.display = accion==='rechazar' ? 'flex' : 'none';
  document.getElementById('resr-justificacion').value = accion==='aprobar' ? 'Retiro voluntario verificado dentro del plazo permitido. Reembolso total aprobado.' : '';
  document.getElementById('resr-label-req').style.display = accion==='rechazar' ? 'inline' : 'none';
  document.getElementById('resr-btn').textContent = accion==='aprobar' ? 'Confirmar aprobación' : 'Confirmar rechazo';
  document.getElementById('resr-btn').className = accion==='aprobar' ? 'btn' : 'btn btn-danger';
  if (accion==='aprobar') document.getElementById('resr-btn').style.cssText = 'background:var(--sage);color:#fff';
  else document.getElementById('resr-btn').style.cssText = '';
  abrirModal('modal-resolver-reembolso');
}
function renderModalResolverReembolso() {
  if (document.getElementById('modal-resolver-reembolso')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-resolver-reembolso"><div class="modal" style="max-width:460px"><div class="modal-header"><h3 id="resr-titulo"></h3><button class="modal-close" onclick="cerrarModal(\'modal-resolver-reembolso\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="resr-id"><input type="hidden" id="resr-accion">' +
    '<p style="font-size:14px;margin-bottom:14px" id="resr-resumen"></p>' +
    '<div id="resr-aviso" class="flex gap-8" style="display:none;background:var(--clay-soft);border-radius:10px;padding:12px;margin-bottom:14px">' + iconSvg('warn') + '<p class="text-sm" style="color:var(--clay);margin:0">El estudiante verá esta justificación, así que sé claro y respetuoso.</p></div>' +
    '<div class="campo"><label>Justificación <span id="resr-label-req" style="display:none">*</span></label><textarea id="resr-justificacion" rows="4"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-resolver-reembolso\')">Cancelar</button><button class="btn" id="resr-btn" onclick="resolverReembolso()">Confirmar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function resolverReembolso() {
  const id = document.getElementById('resr-id').value, accion = document.getElementById('resr-accion').value;
  const justificacion = document.getElementById('resr-justificacion').value.trim();
  if (accion==='rechazar' && !justificacion) { toast('Escribe una justificación para rechazar la solicitud', true); return; }
  const r = DATA.reembolsos.find(x=>x.id===id);
  const ahora = new Date().toISOString().slice(0,16);
  r.estado = accion==='aprobar' ? 'aprobado' : 'rechazado';
  r.justificacion = justificacion; r.montoDevuelto = accion==='aprobar' ? r.monto : null; r.fechaResolucion = ahora; r.aprobadoPor = CONTADOR.nombre;
  if (accion==='aprobar') {
    const pago = DATA.pagos.find(p=>p.id===r.pagoId); if (pago) pago.estado = 'reembolsado';
    const factura = DATA.facturas.find(f=>f.pagoId===r.pagoId); if (factura) { factura.estadoPago='Anulada'; factura.pagado=0; }
  }
  saveData(); cerrarModal('modal-resolver-reembolso'); render();
  toast(accion==='aprobar' ? 'Reembolso aprobado · inscripción de ' + r.estudiante + ' marcada como retirada' : 'Solicitud de ' + r.estudiante + ' rechazada');
}
