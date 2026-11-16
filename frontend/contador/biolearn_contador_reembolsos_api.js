/* ================================================================
   BIOLearn — biolearn_contador_reembolsos_api.js
   Módulo de Pagos, Fase 2 (reembolsos), conectado al backend real. Reemplaza la sección «Reembolsos»
   del prototipo del contador:
   - Solicitudes con filtros por estado, búsqueda y fechas. Cada una muestra el pago, la factura, los
     cursos con su avance al solicitar, el motivo y dónde devolver el dinero.
   - Aprobar: los cursos quedan retirados y se emite la nota crédito; con tarjeta o Nequi la pasarela
     devuelve el dinero al mismo medio. Con PSE o transferencia queda «por devolver» hasta que el
     contador registra la referencia (y, si quiere, el comprobante) de la devolución.
   - Rechazar con un motivo: el estudiante lo ve y recupera el acceso a sus cursos.
   - Nota crédito imprimible y reenvío por correo.
   El auditor solo consulta (no ve Aprobar, Rechazar, Registrar devolución ni Reenviar) y ve los números
   de cuenta enmascarados. Se carga después de biolearn_contador_pagos_api.js (usa pedirApiCont, qs,
   urlCont, fechaHoraCont, cargarPermisosCont y CONT_PUEDE_RESOLVER).
================================================================ */
let CR_FILTRO = { estado: 'solicitado', busqueda: '', desde: '', hasta: '' };
let CR_LISTA = [];
let CR_NOTA = null;

const ESTADO_REE_CONT = { solicitado: 'Por revisar', aprobado: 'Por devolver', devuelto: 'Devuelto', rechazado: 'Rechazado', cancelado: 'Cancelado' };
function pillReembolsoCont(e) {
  const clase = { solicitado: 'pill-ambar', aprobado: 'pill-azul', devuelto: 'pill-verde', rechazado: 'pill-clay', cancelado: 'pill-gris' }[e] || 'pill-gris';
  return '<span class="pill ' + clase + '">' + esc(ESTADO_REE_CONT[e] || e) + '</span>';
}

function renderReembolsos(cont) {
  let html = '<div class="section-header"><div><h2>Reembolsos</h2><p class="subtitle">Solicitudes de los estudiantes: hasta 8 días después del pago y con menos del 20 % de avance. Las que no tienen avance se aprueban solas.</p></div></div>';
  html += '<div id="cr-resumen" style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:20px"></div>';
  html += '<div class="flex gap-8 flex-wrap mb-14">';
  [['', 'Todos'], ['solicitado', 'Por revisar'], ['aprobado', 'Por devolver'], ['devuelto', 'Devueltos'], ['rechazado', 'Rechazados'], ['cancelado', 'Cancelados']].forEach(([v, t]) => {
    const a = CR_FILTRO.estado === v;
    html += '<button class="cr-chip" data-estado="' + v + '" onclick="CR_FILTRO.estado=\'' + v + '\';render()" style="padding:7px 14px;border-radius:20px;font-size:13px;font-weight:700;cursor:pointer;border:1px solid ' + (a ? 'var(--blue)' : 'var(--line)') + ';background:' + (a ? 'var(--blue-soft)' : '#fff') + ';color:' + (a ? 'var(--blue-deep)' : 'var(--ink-soft)') + '">' + t + '</button>';
  });
  html += '</div><div class="flex gap-10 flex-wrap mb-16">' +
    '<input id="cr-buscar" placeholder="Buscar estudiante, correo o curso..." value="' + esc(CR_FILTRO.busqueda) + '" onkeydown="if(event.key===\'Enter\'){CR_FILTRO.busqueda=this.value;cargarReembolsosCont()}" style="flex:1;min-width:240px">' +
    '<input type="date" id="cr-desde" value="' + CR_FILTRO.desde + '" onchange="CR_FILTRO.desde=this.value;cargarReembolsosCont()" title="Desde" style="width:170px">' +
    '<input type="date" id="cr-hasta" value="' + CR_FILTRO.hasta + '" onchange="CR_FILTRO.hasta=this.value;cargarReembolsosCont()" title="Hasta" style="width:170px">' +
    '<button class="btn btn-ghost" onclick="CR_FILTRO.busqueda=document.getElementById(\'cr-buscar\').value;cargarReembolsosCont()">Buscar</button></div>';
  html += '<div id="cr-lista"><div class="empty-state">Cargando solicitudes...</div></div>';
  cont.innerHTML = html;
  renderModalesReembolsoCont();
  cargarReembolsosCont();
}

async function cargarReembolsosCont() {
  try {
    await cargarPermisosCont();
    const [lista, revisar, devolver] = await Promise.all([
      pedirApiCont('GET', '/api/reembolsos' + qs(CR_FILTRO)),
      pedirApiCont('GET', '/api/reembolsos?estado=solicitado'),
      pedirApiCont('GET', '/api/reembolsos?estado=aprobado'),
    ]);
    CR_LISTA = lista;
    pintarResumenReembolsosCont(revisar, devolver);
    pintarReembolsosCont();
  } catch (err) { const el = document.getElementById('cr-lista'); if (el) el.innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; }
}

function pintarResumenReembolsosCont(revisar, devolver) {
  const el = document.getElementById('cr-resumen'); if (!el) return;
  const suma = l => l.reduce((s, r) => s + Number(r.monto), 0);
  const devueltos = CR_LISTA.filter(r => r.estado === 'devuelto');
  const tarjeta = (t, v, sub, color) => '<div class="card"><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">' + t + '</div><div style="font-size:24px;font-weight:800;color:' + color + '">' + v + '</div><div class="text-sm muted">' + sub + '</div></div>';
  el.innerHTML = tarjeta('Por revisar', revisar.length, fmtMoney(suma(revisar)) + ' solicitados', 'var(--amber)') +
    tarjeta('Por devolver', devolver.length, fmtMoney(suma(devolver)) + ' a cuentas bancarias o Nequi', 'var(--blue)') +
    tarjeta('Devuelto en la lista', fmtMoney(suma(devueltos)), devueltos.length + ' reembolso(s) con los filtros actuales', 'var(--sage)');
}

function destinoReembolsoCont(r) {
  if (r.medioDevolucion === 'cuenta_bancaria')
    return '<strong>' + esc(r.banco) + '</strong> · ' + (r.tipoCuenta === 'corriente' ? 'Corriente' : 'Ahorros') + ' <span class="cr-cuenta" style="font-family:monospace">' + esc(r.numeroCuenta) + '</span>' +
      '<br><span class="text-sm muted">Titular: ' + esc(r.titular) + ' · Doc. ' + esc(r.documentoTitular) + '</span>';
  if (r.medioDevolucion === 'nequi') return '<strong>Nequi</strong> <span class="cr-cuenta" style="font-family:monospace">' + esc(r.telefonoNequi) + '</span>';
  return esc(r.medioDevolucionNombre) + ' · automático por la pasarela';
}

function pintarReembolsosCont() {
  const el = document.getElementById('cr-lista'); if (!el) return;
  const aviso = CONT_PUEDE_RESOLVER === false ? '<div id="cont-aviso-auditor" class="card mb-14" style="border-left:4px solid var(--blue);padding:12px 16px"><strong>Rol auditor:</strong> puedes consultar los reembolsos y las notas crédito, pero no aprobarlos, rechazarlos ni registrar devoluciones. Los números de cuenta se muestran enmascarados.</div>' : '';
  if (!CR_LISTA.length) { el.innerHTML = aviso + '<div class="empty-state" id="cr-vacio">No hay solicitudes de reembolso con estos filtros.</div>'; return; }
  let html = aviso;
  CR_LISTA.forEach(r => {
    const borde = { solicitado: 'var(--amber)', aprobado: 'var(--blue)', devuelto: 'var(--sage)', rechazado: 'var(--clay)' }[r.estado] || 'var(--line)';
    let acc = '';
    if (r.idNotaCredito) acc += '<button class="btn btn-ghost btn-sm cr-ver-nota" onclick="verNotaCreditoCont(' + r.idNotaCredito + ')">📄 ' + esc(r.numeroNotaCredito) + '</button>';
    if (r.comprobanteDevolucionUrl) acc += '<button class="btn btn-ghost btn-sm" onclick="window.open(\'' + esc(urlCont(r.comprobanteDevolucionUrl)) + '\',\'_blank\')">📎 Comprobante</button>';
    if (CONT_PUEDE_RESOLVER && r.estado === 'solicitado')
      acc += '<button class="btn btn-danger btn-sm cr-rechazar" onclick="abrirRechazoReembolsoCont(' + r.id + ')">✕ Rechazar</button>' +
        '<button class="btn btn-sm cr-aprobar" style="background:var(--sage);color:#fff" onclick="abrirAprobarReembolsoCont(' + r.id + ')">✓ Aprobar reembolso</button>';
    if (CONT_PUEDE_RESOLVER && r.estado === 'aprobado' && r.medioDevolucion !== 'mismo_medio')
      acc += '<button class="btn btn-primary btn-sm cr-devolucion" onclick="abrirDevolucionCont(' + r.id + ')">💸 Registrar devolución</button>';
    const filas = [
      ['Pago', esc(r.medioPagoNombre) + ' · #' + r.idPago + (r.referenciaPago ? ' · <span style="font-family:monospace">' + esc(r.referenciaPago) + '</span>' : '') + ' · ' + fechaHoraCont(r.fechaPago) + (r.numeroFactura ? ' · factura ' + esc(r.numeroFactura) : '')],
      ['Cursos', r.items.map(i => esc(i.curso) + ' — ' + fmtMoney(i.monto) + ' <span class="text-sm muted">(avance ' + Math.round(Number(i.avance || 0)) + ' %)</span>').join('<br>')],
      ['Motivo', '«' + esc(r.motivo) + '»'],
      ['Devolución', destinoReembolsoCont(r)],
    ];
    if (r.justificacion) filas.push(['Motivo del rechazo', '<span style="color:var(--clay)">' + esc(r.justificacion) + '</span>']);
    if (r.referenciaDevolucion) filas.push(['Referencia', '<span style="font-family:monospace">' + esc(r.referenciaDevolucion) + '</span> · ' + fechaHoraCont(r.devueltoEn)]);
    if (r.aprobacionAutomatica) filas.push(['Aprobación', '<span class="cr-auto" style="color:var(--sage);font-weight:700">⚡ Automática</span> <span class="muted">· todos los cursos con 0 % de avance</span>']);
    const devolvioContador = r.aprobacionAutomatica && r.resueltoPor && r.resueltoPor !== 'Aprobación automática';
    if (r.resueltoPor) filas.push([devolvioContador ? 'Devolución registrada por' : 'Resuelto por', esc(r.resueltoPor) + ' · ' + fechaHoraCont(devolvioContador ? r.devueltoEn : r.resueltoEn)]);
    html += '<div class="card mb-14 cr-tarjeta" data-id="' + r.id + '" data-estado="' + r.estado + '" data-correo="' + esc(r.correo) + '" style="border-left:4px solid ' + borde + '">' +
      '<div class="flex justify-between items-start mb-14"><div><h3 style="font-size:17px;margin:0 0 4px">' + esc(r.estudiante) + '</h3><p class="text-sm muted" style="margin:0">' + esc(r.correo) + ' · solicitado el ' + fechaHoraCont(r.solicitadoEn) + '</p></div>' +
      '<div style="text-align:right"><div style="font-weight:800;font-size:18px">' + fmtMoney(r.monto) + '</div>' + pillReembolsoCont(r.estado) + '</div></div>' +
      '<div style="display:grid;grid-template-columns:150px 1fr;row-gap:8px;font-size:13.5px;margin-bottom:14px">' +
      filas.map(([k, v]) => '<div style="font-weight:700;color:var(--ink-soft)">' + k + '</div><div>' + v + '</div>').join('') + '</div>' +
      (acc ? '<div class="flex gap-10 flex-wrap" style="justify-content:flex-end">' + acc + '</div>' : '') + '</div>';
  });
  el.innerHTML = html;
}

function reembolsoCont(id) { return CR_LISTA.find(x => x.id === id); }

/* ---------------------------------------------------------------- aprobar */
function abrirAprobarReembolsoCont(id) {
  const r = reembolsoCont(id);
  document.getElementById('cra-id').value = id;
  const auto = r.medioDevolucion === 'mismo_medio';
  document.getElementById('cra-resumen').innerHTML = 'Estudiante: <strong>' + esc(r.estudiante) + '</strong><br>Cursos: ' + r.items.map(i => esc(i.curso)).join(', ') +
    '<br>Valor: <strong>' + fmtMoney(r.monto) + '</strong>';
  document.getElementById('cra-nota').innerHTML = (auto
    ? 'La pasarela devolverá el dinero a la <strong>' + esc(r.medioPagoNombre) + '</strong> del estudiante de inmediato.'
    : 'Después de aprobar, transfiere el dinero a ' + destinoReembolsoCont(r) + ' y registra la devolución con su referencia.') +
    '<br>Los cursos quedan retirados y se emite y envía la nota crédito sobre la factura ' + esc(r.numeroFactura || '') + '.';
  abrirModal('modal-cr-aprobar');
}
async function aprobarReembolsoCont() {
  const id = Number(document.getElementById('cra-id').value);
  const btn = document.getElementById('cra-btn'); btn.disabled = true;
  try {
    const r = await pedirApiCont('PATCH', '/api/reembolsos/' + id + '/aprobacion');
    cerrarModal('modal-cr-aprobar');
    toast('Reembolso aprobado · nota crédito ' + r.numeroNotaCredito + (r.estado === 'devuelto' ? ' · dinero devuelto (' + r.referenciaDevolucion + ')' : ' · falta registrar la devolución'));
    cargarReembolsosCont();
  } catch (err) { toast(err.message, true); }
  btn.disabled = false;
}

/* ---------------------------------------------------------------- rechazar */
function abrirRechazoReembolsoCont(id) {
  const r = reembolsoCont(id);
  document.getElementById('crr-id').value = id;
  document.getElementById('crr-resumen').innerHTML = 'Estudiante: <strong>' + esc(r.estudiante) + '</strong> · ' + fmtMoney(r.monto);
  document.getElementById('crr-motivo').value = '';
  abrirModal('modal-cr-rechazo');
}
async function rechazarReembolsoCont() {
  const id = Number(document.getElementById('crr-id').value);
  const motivo = document.getElementById('crr-motivo').value.trim();
  const btn = document.getElementById('crr-btn'); btn.disabled = true;
  try {
    await pedirApiCont('PATCH', '/api/reembolsos/' + id + '/rechazo', { motivo });
    cerrarModal('modal-cr-rechazo'); toast('Solicitud rechazada; el estudiante verá el motivo y recupera sus cursos');
    cargarReembolsosCont();
  } catch (err) { toast(err.message, true); }
  btn.disabled = false;
}

/* ---------------------------------------------------------------- registrar devolución */
function abrirDevolucionCont(id) {
  const r = reembolsoCont(id);
  document.getElementById('crd-id').value = id;
  document.getElementById('crd-resumen').innerHTML = 'Devuelve <strong>' + fmtMoney(r.monto) + '</strong> a:<br>' + destinoReembolsoCont(r);
  document.getElementById('crd-referencia').value = '';
  document.getElementById('crd-archivo').value = '';
  abrirModal('modal-cr-devolucion');
}
async function registrarDevolucionCont() {
  const id = Number(document.getElementById('crd-id').value);
  const referencia = document.getElementById('crd-referencia').value.trim();
  const archivo = document.getElementById('crd-archivo').files[0];
  if (archivo && archivo.size > 10 * 1024 * 1024) { toast('El archivo supera el tamaño máximo de 10 MB', true); return; }
  const btn = document.getElementById('crd-btn'); btn.disabled = true;
  try {
    let comprobanteUrl = null;
    if (archivo) {
      const fd = new FormData(); fd.append('archivo', archivo);
      const r = await fetch(API_BASE_CONT + '/api/archivos', { method: 'POST', headers: { 'Authorization': 'Bearer ' + sessionStorage.getItem('biolearn_token') }, body: fd });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error((data.mensaje || 'Error ' + r.status) + (data.detalles && data.detalles.length ? ' (' + data.detalles.join('; ') + ')' : ''));
      comprobanteUrl = data.url;
    }
    await pedirApiCont('PATCH', '/api/reembolsos/' + id + '/devolucion', { referencia, comprobanteUrl });
    cerrarModal('modal-cr-devolucion'); toast('Devolución registrada; se avisó al estudiante');
    cargarReembolsosCont();
  } catch (err) { toast(err.message, true); }
  btn.disabled = false;
}

/* ---------------------------------------------------------------- nota crédito */
async function verNotaCreditoCont(id) {
  try {
    CR_NOTA = await pedirApiCont('GET', '/api/notas-credito/' + id);
    renderModalesReembolsoCont();
    document.getElementById('crn-cuerpo').innerHTML = notaCreditoHTMLCont(CR_NOTA);
    document.getElementById('crn-reenviar').style.display = CONT_PUEDE_RESOLVER ? '' : 'none';
    abrirModal('modal-cr-nota');
  } catch (err) { toast(err.message, true); }
}
async function reenviarNotaCont() {
  if (!CR_NOTA) return;
  try { const n = await pedirApiCont('POST', '/api/notas-credito/' + CR_NOTA.id + '/correo'); toast('Nota crédito ' + n.numero + ' reenviada a ' + n.correo); }
  catch (err) { toast(err.message, true); }
}
function imprimirNotaCont() {
  if (!CR_NOTA) return;
  const w = window.open('');
  if (!w) { toast('Habilita las ventanas emergentes para imprimir', true); return; }
  w.document.write('<html><head><title>' + esc(CR_NOTA.numero) + '</title></head><body style="margin:0;background:#F4F5F7;padding:32px;display:flex;justify-content:center"><div style="max-width:620px">' + notaCreditoHTMLCont(CR_NOTA) + '</div></body></html>');
  w.document.close(); w.focus(); w.print();
}

function notaCreditoHTMLCont(n) {
  const filas = n.items.map(i => '<tr><td style="padding:12px;border-bottom:1px solid #E0E2E8">Reembolso — ' + esc(i.curso) + '</td><td style="padding:12px;border-bottom:1px solid #E0E2E8;text-align:right;font-weight:700">' + fmtMoney(i.monto) + '</td></tr>').join('');
  return '<div id="crn-documento" style="background:#fff;border:1px solid #E0E2E8;border-radius:14px;padding:32px;font-family:Inter,Arial,sans-serif;color:#2A2118">' +
    '<div style="display:flex;justify-content:space-between;align-items:flex-start;border-bottom:3px solid #B5553C;padding-bottom:18px;margin-bottom:22px">' +
    '<div><div style="font-size:22px;font-weight:800;color:#2C4A42">🌿 BioLearn</div><div style="font-size:11px;color:#8A6A4E">' + esc(n.razonSocial || '') + ' · NIT ' + esc(n.nit || '') + '<br>' + esc(n.direccionEmisor || '') + '</div></div>' +
    '<div style="text-align:right"><div style="font-size:11px;letter-spacing:1.5px;color:#B5553C;font-weight:700">NOTA CRÉDITO</div><div style="font-family:monospace;font-weight:800;font-size:17px;color:#2C4A42">' + esc(n.numero) + '</div>' +
    '<div style="font-size:11.5px;color:#8A6A4E">Corrige la factura <strong>' + esc(n.numeroFactura) + '</strong></div></div></div>' +
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:22px;font-size:13px"><div><div style="color:#8A6A4E;font-weight:700;text-transform:uppercase;font-size:10.5px">Cliente</div><div style="font-weight:700">' + esc(n.estudiante) + '</div><div>' + esc(n.correo) + (n.documento ? '<br>Doc. ' + esc(n.documento) : '') + '</div></div>' +
    '<div style="text-align:right"><div style="color:#8A6A4E;font-weight:700;text-transform:uppercase;font-size:10.5px">Fecha de emisión</div><div style="font-weight:700">' + fechaHoraCont(n.fechaEmision) + '</div><div style="font-size:11.5px;color:#8A6A4E">Factura del ' + fechaHoraCont(n.fechaFactura) + '</div></div></div>' +
    '<table style="width:100%;border-collapse:collapse;margin-bottom:18px;font-size:13.5px"><thead><tr style="background:#F6E7E1"><th style="text-align:left;padding:9px 12px;font-size:10.5px;text-transform:uppercase">Concepto</th><th style="text-align:right;padding:9px 12px;font-size:10.5px;text-transform:uppercase">Valor</th></tr></thead><tbody>' + filas + '</tbody>' +
    '<tfoot><tr><td style="padding:12px;font-weight:800;font-size:15px">Total a devolver</td><td style="padding:12px;text-align:right;font-weight:800;font-size:15px">' + fmtMoney(n.montoTotal) + '</td></tr></tfoot></table>' +
    '<div style="background:#F7F3EC;border-radius:10px;padding:14px 16px;font-size:12.5px"><div>Motivo: ' + esc(n.motivo) + '</div><div style="margin-top:4px">Devolución: <strong>' + esc(n.medioDevolucionNombre || '') + '</strong>' +
    (n.enviadaPorCorreo ? ' · enviada por correo el ' + fechaHoraCont(n.enviadaEn) : '') + '</div></div>' +
    '<p style="font-size:10.5px;color:#8A6A4E;text-align:center;margin:14px 0 0">Nota crédito generada electrónicamente por BioLearn · Proyecto académico sin validez fiscal.</p></div>';
}

/* ================================================================ MODALES */
function renderModalesReembolsoCont() {
  const crear = (html) => { const d = document.createElement('div'); d.innerHTML = html; document.body.appendChild(d.firstElementChild); };
  if (!document.getElementById('modal-cr-aprobar')) crear('<div class="modal-overlay" id="modal-cr-aprobar"><div class="modal" style="max-width:460px"><div class="modal-header"><h3>Aprobar reembolso</h3><button class="modal-close" onclick="cerrarModal(\'modal-cr-aprobar\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="cra-id"><p style="font-size:14px;line-height:1.8" id="cra-resumen"></p><p class="text-sm muted" id="cra-nota"></p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-cr-aprobar\')">Cancelar</button><button class="btn" style="background:var(--sage);color:#fff" id="cra-btn" onclick="aprobarReembolsoCont()">✓ Aprobar y emitir nota crédito</button></div></div></div>');
  if (!document.getElementById('modal-cr-rechazo')) crear('<div class="modal-overlay" id="modal-cr-rechazo"><div class="modal" style="max-width:460px"><div class="modal-header"><h3>Rechazar reembolso</h3><button class="modal-close" onclick="cerrarModal(\'modal-cr-rechazo\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="crr-id"><p style="font-size:14px" id="crr-resumen"></p><div class="campo"><label>Motivo (el estudiante lo verá)</label><textarea id="crr-motivo" rows="3" maxlength="500" placeholder="Ej.: el curso ya se revisó en su mayor parte"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-cr-rechazo\')">Cancelar</button><button class="btn btn-danger" id="crr-btn" onclick="rechazarReembolsoCont()">✕ Rechazar solicitud</button></div></div></div>');
  if (!document.getElementById('modal-cr-devolucion')) crear('<div class="modal-overlay" id="modal-cr-devolucion"><div class="modal" style="max-width:460px"><div class="modal-header"><h3>Registrar devolución</h3><button class="modal-close" onclick="cerrarModal(\'modal-cr-devolucion\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="crd-id"><p style="font-size:14px;line-height:1.7" id="crd-resumen"></p>' +
    '<div class="campo"><label>Referencia o número de la transacción</label><input id="crd-referencia" maxlength="200" placeholder="Ej.: TRF-20261115-0042"></div>' +
    '<div class="campo"><label>Comprobante (opcional: PDF, PNG o JPG, máximo 10 MB)</label><input type="file" id="crd-archivo" accept=".pdf,.png,.jpg,.jpeg"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-cr-devolucion\')">Cancelar</button><button class="btn btn-primary" id="crd-btn" onclick="registrarDevolucionCont()">💸 Registrar devolución</button></div></div></div>');
  if (!document.getElementById('modal-cr-nota')) crear('<div class="modal-overlay" id="modal-cr-nota"><div class="modal" style="max-width:660px"><div class="modal-header"><h3>Nota crédito</h3><button class="modal-close" onclick="cerrarModal(\'modal-cr-nota\')">✕</button></div>' +
    '<div class="modal-body" style="background:var(--bone)"><div id="crn-cuerpo"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-cr-nota\')">Cerrar</button><button class="btn btn-ghost" id="crn-reenviar" onclick="reenviarNotaCont()">✉ Reenviar</button><button class="btn btn-primary" onclick="imprimirNotaCont()">⬇ Imprimir o guardar PDF</button></div></div></div>');
}
