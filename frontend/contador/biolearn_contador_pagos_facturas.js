/* ================================================================
   BIOLearn — biolearn_contador_pagos_facturas.js
================================================================ */
let PAG_FILTRO = 'todos';
function renderPagos(cont) {
  const filtrados = DATA.pagos.filter(p => PAG_FILTRO==='todos' || p.estado===PAG_FILTRO);
  const totales = { pendiente:0, aprobado:0, reembolsado:0, fallido:0 };
  DATA.pagos.forEach(p => totales[p.estado] = (totales[p.estado]||0)+1);

  let html = '<div class="section-header"><div><h2>Pagos</h2><p class="subtitle">Confirma los pagos de los estudiantes para emitir su factura automáticamente</p></div></div>';
  html += '<div class="flex gap-8 flex-wrap mb-18">';
  ['todos','pendiente','aprobado','fallido','reembolsado'].forEach(f => {
    const activo = PAG_FILTRO === f;
    html += '<button onclick="PAG_FILTRO=\''+f+'\';render()" style="padding:7px 14px;border-radius:20px;font-size:13px;font-weight:700;cursor:pointer;border:1px solid '+(activo?'var(--blue)':'var(--line)')+';background:'+(activo?'var(--blue-soft)':'transparent')+';color:'+(activo?'var(--blue-deep)':'var(--ink-soft)')+';text-transform:capitalize">' + f + (f!=='todos'?' ('+(totales[f]||0)+')':'') + '</button>';
  });
  html += '</div>';
  html += '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
    ['Estudiante','Curso','Fecha','Monto','Método','Referencia','Estado',''].map(h=>'<th style="color:var(--blue-deep)">'+h+'</th>').join('') + '</tr></thead><tbody>';
  filtrados.forEach(p => {
    html += '<tr' + (p.estado==='fallido'?' style="background:var(--clay-soft)"':'') + '><td style="font-weight:700">' + esc(p.estudiante) + '</td>' +
      '<td class="text-sm muted">' + esc(p.curso) + '</td><td class="text-sm muted">' + fmtDate(p.fecha) + '</td>' +
      '<td style="font-weight:700">' + fmtMoney(p.monto) + '</td><td class="text-sm">' + esc(p.metodo) + '</td>' +
      '<td class="text-sm muted" style="font-family:monospace">' + esc(p.referencia||'—') + '</td><td>' + pill(p.estado) + '</td>' +
      '<td><div class="flex gap-6 flex-wrap">' +
      (p.estado==='pendiente' ? '<button class="btn btn-sm" style="background:var(--sage);color:#fff" onclick="abrirModalConfirmarPago(\''+p.id+'\')">✓ Confirmar</button>' : '') +
      (p.estado==='pendiente' && p.metodo!=='Tarjeta de crédito' ? '<button class="btn btn-ghost btn-xs" onclick="toast(\'Mostrando comprobante adjunto por el estudiante\')">Comprobante</button>' : '') +
      (p.estado==='fallido' ? '<button class="btn btn-ghost btn-xs" onclick="abrirModalVerFallo(\''+p.id+'\')">⚠ Ver detalle</button>' : '') +
      ((p.estado==='aprobado'||p.estado==='reembolsado') ? '<span class="text-sm muted">—</span>' : '') + '</div></td></tr>';
  });
  if (filtrados.length === 0) html += '<tr><td colspan="8" class="empty-state">No hay pagos en este filtro.</td></tr>';
  html += '</tbody></table></div>';
  cont.innerHTML = html;
  renderModalConfirmarPago(); renderModalVerFallo();
}
function abrirModalConfirmarPago(id) {
  const p = DATA.pagos.find(x=>x.id===id);
  document.getElementById('confp-id').value = id;
  document.getElementById('confp-resumen').innerHTML = 'Estudiante: <strong>' + esc(p.estudiante) + '</strong><br>Curso: ' + esc(p.curso) + '<br>Monto: <strong>' + fmtMoney(p.monto) + '</strong> · ' + esc(p.metodo) + (p.referencia?' · '+esc(p.referencia):'');
  abrirModal('modal-confirmar-pago');
}
function renderModalConfirmarPago() {
  if (document.getElementById('modal-confirmar-pago')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-confirmar-pago"><div class="modal" style="max-width:440px"><div class="modal-header"><h3>Confirmar pago</h3><button class="modal-close" onclick="cerrarModal(\'modal-confirmar-pago\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="confp-id"><p style="font-size:14px;line-height:1.8" id="confp-resumen"></p>' +
    '<p class="text-sm muted">Al confirmar, el pago pasará a estado <strong>aprobado</strong> y se emitirá automáticamente la factura correspondiente al estudiante.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-confirmar-pago\')">Cancelar</button><button class="btn btn-primary" onclick="confirmarPago()">✓ Confirmar y emitir factura</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function confirmarPago() {
  const id = document.getElementById('confp-id').value;
  const pago = DATA.pagos.find(x=>x.id===id);
  const facturaId = uid('fact');
  const numero = 'BIO-FAC-' + new Date().getFullYear() + '-' + Math.floor(1000+Math.random()*8999);
  DATA.facturas.unshift({ id:facturaId, numero, pagoId:pago.id, estudiante:pago.estudiante, curso:pago.curso, monto:pago.monto, pagado:pago.monto, fecha:new Date().toISOString().slice(0,10), estadoPago:'Pagada', correoEnviado:false });
  pago.estado = 'aprobado'; pago.facturaId = facturaId;
  saveData(); cerrarModal('modal-confirmar-pago'); render();
  toast('Pago de ' + pago.estudiante + ' confirmado · factura ' + numero + ' emitida automáticamente');
}
function abrirModalVerFallo(id) {
  const p = DATA.pagos.find(x=>x.id===id);
  document.getElementById('fallo-resumen').innerHTML = 'Estudiante: <strong>' + esc(p.estudiante) + '</strong><br>Curso: ' + esc(p.curso) + '<br>Monto: <strong>' + fmtMoney(p.monto) + '</strong> · ' + esc(p.metodo);
  document.getElementById('fallo-motivo').textContent = p.motivoFallo;
  abrirModal('modal-ver-fallo');
}
function renderModalVerFallo() {
  if (document.getElementById('modal-ver-fallo')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-ver-fallo"><div class="modal" style="max-width:420px"><div class="modal-header"><h3>Pago fallido</h3><button class="modal-close" onclick="cerrarModal(\'modal-ver-fallo\')">✕</button></div>' +
    '<div class="modal-body"><p style="font-size:14px;line-height:1.8" id="fallo-resumen"></p>' +
    '<div class="flex gap-8" style="background:var(--clay-soft);border-radius:10px;padding:14px">' + iconSvg('warn') +
    '<div><div class="text-sm" style="font-weight:700;color:var(--clay);text-transform:uppercase;margin-bottom:4px">Motivo del fallo</div><p class="text-sm" id="fallo-motivo" style="margin:0"></p></div></div>' +
    '<p class="text-sm muted" style="margin-top:14px">El estudiante deberá intentar el pago nuevamente desde el catálogo de cursos.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-ver-fallo\')">Cerrar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

/* ---------------------------- Facturas ---------------------------- */
let FACT_BUSQUEDA = '', FACT_FILTRO = 'Todos los estados';
function renderFacturas(cont) {
  const pendientesEnviar = DATA.facturas.filter(f => !f.correoEnviado && f.estadoPago!=='Anulada');
  const q = FACT_BUSQUEDA.toLowerCase();
  const filtradas = DATA.facturas.filter(f => (FACT_FILTRO==='Todos los estados'||f.estadoPago===FACT_FILTRO) && (f.numero.toLowerCase().includes(q)||f.estudiante.toLowerCase().includes(q)));

  let html = '<div class="section-header"><div><h2>Gestión de facturas</h2><p class="subtitle">Emite, descarga o reenvía las facturas de los estudiantes</p></div>' +
    '<div class="flex gap-8"><button class="btn btn-ghost" onclick="toast(\'Exportando listado de facturas...\')">⬇ Exportar</button><button class="btn btn-primary" onclick="abrirModalEmitirFactura()">+ Emitir factura</button></div></div>';

  if (pendientesEnviar.length > 0) {
    html += '<div class="text-sm mb-10" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft)">Pendientes de enviar</div><div class="mb-26">';
    pendientesEnviar.forEach(f => {
      const esPendPago = f.estadoPago==='Pendiente'||f.estadoPago==='Parcial';
      html += '<div class="flex items-center gap-14" style="background:var(--panel);border-radius:12px;border:1px solid var(--line);border-left:4px solid '+(esPendPago?'var(--amber)':'var(--clay)')+';padding:14px 18px;margin-bottom:10px">' +
        '<div style="flex:1"><div style="font-size:14.5px;font-weight:700">' + esc(f.numero) + ' · ' + esc(f.estudiante) + '</div>' +
        '<div class="text-sm muted">' + fmtMoney(f.monto) + ' · ' + esc(f.curso) + ' · Emitida ' + fmtDate(f.fecha) + ' · ' + (esPendPago?'Pago pendiente de confirmación':'No enviada al correo') + '</div></div>' +
        pill(esPendPago?'Pend. pago':'No enviada') +
        '<button class="btn btn-primary btn-sm" onclick="enviarCorreoFactura(\''+f.id+'\')">✉ Enviar</button></div>';
    });
    html += '</div>';
  }

  html += '<div class="text-sm mb-10" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft)">Todas las facturas</div>';
  html += '<div class="flex gap-10 flex-wrap mb-16">' +
    '<input id="fact-buscar" placeholder="Buscar por número o estudiante..." value="'+esc(FACT_BUSQUEDA)+'" oninput="FACT_BUSQUEDA=this.value;renderFacturas(document.getElementById(\'app-content\'))" style="flex:1;min-width:220px">' +
    '<select onchange="FACT_FILTRO=this.value;render()" style="width:180px">' + ['Todos los estados','Pagada','Pendiente','Parcial','Anulada'].map(e=>'<option'+(FACT_FILTRO===e?' selected':'')+'>'+e+'</option>').join('') + '</select>' +
    '<button class="btn btn-ghost" onclick="toast(\'Descargando listado en PDF...\')">⬇ PDF</button></div>';

  html += '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
    ['N° Factura','Estudiante','Emisión','Total','Pagado','Estado','Correo','Acciones'].map(h=>'<th style="color:var(--blue-deep)">'+h+'</th>').join('') + '</tr></thead><tbody>';
  filtradas.forEach(f => {
    html += '<tr><td style="font-family:monospace;font-weight:700">' + esc(f.numero) + '</td><td style="font-weight:700">' + esc(f.estudiante) + '</td>' +
      '<td class="text-sm muted">' + fmtDate(f.fecha) + '</td><td style="font-weight:700">' + fmtMoney(f.monto) + '</td>' +
      '<td style="font-weight:700;color:' + (f.pagado===0?'var(--clay)':f.pagado<f.monto?'#2C4F9E':'var(--sage)') + '">' + fmtMoney(f.pagado) + '</td>' +
      '<td>' + pill(f.estadoPago) + '</td>' +
      '<td>' + (f.estadoPago==='Anulada' ? '<span class="text-sm muted">N/A</span>' : (f.correoEnviado ? pill('✓ Enviada') : pill('No enviada'))) + '</td>' +
      '<td><div class="flex gap-4"><button class="btn btn-ghost btn-xs" onclick="abrirModalVerFactura(\''+f.id+'\')">Ver</button>' +
      '<button class="btn btn-ghost btn-xs" onclick="abrirFacturaImprimible(\''+f.id+'\')">⬇</button>' +
      (f.estadoPago!=='Anulada' ? '<button class="btn btn-ghost btn-xs" onclick="enviarCorreoFactura(\''+f.id+'\')">✉</button>' : '') + '</div></td></tr>';
  });
  if (filtradas.length === 0) html += '<tr><td colspan="8" class="empty-state">No hay facturas que coincidan.</td></tr>';
  html += '</tbody></table></div>';
  cont.innerHTML = html;
  renderModalVerFactura(); renderModalEmitirFactura();
}
function enviarCorreoFactura(id) {
  const f = DATA.facturas.find(x=>x.id===id);
  f.correoEnviado = true; saveData(); render();
  toast('Factura ' + f.numero + ' enviada al correo de ' + f.estudiante);
}
function abrirModalVerFactura(id) {
  document.getElementById('vf-id').value = id;
  document.getElementById('vf-cuerpo').innerHTML = facturaHTML(DATA.facturas.find(x=>x.id===id));
  abrirModal('modal-ver-factura');
}
function renderModalVerFactura() {
  if (document.getElementById('modal-ver-factura')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-ver-factura"><div class="modal" style="max-width:620px"><div class="modal-header"><h3>Factura</h3><button class="modal-close" onclick="cerrarModal(\'modal-ver-factura\')">✕</button></div>' +
    '<div class="modal-body" style="background:var(--bone)"><input type="hidden" id="vf-id"><div id="vf-cuerpo"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-ver-factura\')">Cerrar</button><button class="btn btn-primary" onclick="abrirFacturaImprimible(document.getElementById(\'vf-id\').value)">⬇ Descargar / Imprimir</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function abrirModalEmitirFactura() {
  document.getElementById('ef-estudiante').value=''; document.getElementById('ef-curso').value=''; document.getElementById('ef-monto').value='';
  abrirModal('modal-emitir-factura');
}
function renderModalEmitirFactura() {
  if (document.getElementById('modal-emitir-factura')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-emitir-factura"><div class="modal" style="max-width:440px"><div class="modal-header"><h3>Emitir factura manual</h3><button class="modal-close" onclick="cerrarModal(\'modal-emitir-factura\')">✕</button></div>' +
    '<div class="modal-body"><div class="campo"><label>Estudiante</label><input id="ef-estudiante"></div><div class="campo"><label>Curso</label><input id="ef-curso"></div>' +
    '<div class="campo"><label>Monto (COP)</label><input type="number" id="ef-monto"></div>' +
    '<p class="text-sm muted">La factura se emitirá en estado "Pendiente" hasta que se confirme el pago correspondiente.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-emitir-factura\')">Cancelar</button><button class="btn btn-primary" onclick="emitirFacturaManual()">Emitir factura</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function emitirFacturaManual() {
  const estudiante = document.getElementById('ef-estudiante').value.trim(), curso = document.getElementById('ef-curso').value.trim(), monto = document.getElementById('ef-monto').value;
  if (!estudiante || !curso || !monto) { toast('Completa estudiante, curso y monto', true); return; }
  const numero = 'BIO-FAC-' + new Date().getFullYear() + '-' + Math.floor(1000+Math.random()*8999);
  DATA.facturas.unshift({ id:uid('fact'), numero, pagoId:null, estudiante, curso, monto:Number(monto), pagado:0, fecha:new Date().toISOString().slice(0,10), estadoPago:'Pendiente', correoEnviado:false });
  saveData(); cerrarModal('modal-emitir-factura'); render();
  toast('Factura ' + numero + ' emitida manualmente para ' + estudiante);
}
