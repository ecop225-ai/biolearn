/* ================================================================
   BIOLearn — biolearn_estudiante_reembolsos.js
   Módulo de Pagos, Fase 2 (reembolsos), conectado al backend real. Se suma a «Mis pagos»:
   - «↩ Solicitar reembolso» en los pagos aprobados de los últimos 8 días. En la ventana el estudiante
     marca los cursos que quiere reembolsar (los que no cumplen salen deshabilitados con el motivo:
     avance del 20 % o más, curso completado, ya reembolsado…), escribe el motivo y, si pagó por PSE
     o transferencia, indica dónde recibir el dinero (cuenta bancaria o Nequi). Con tarjeta o Nequi
     se devuelve al mismo medio.
   - «Mis reembolsos»: estado de cada solicitud, motivo del rechazo, referencia de la devolución,
     nota crédito imprimible y «Cancelar solicitud» mientras no se haya resuelto.
   Se carga después de biolearn_estudiante_pagos.js (usa pedirApiEst, pesosEst y fechaEst).
================================================================ */
let REE_LISTA = [];
let REE_OPCIONES = null;
let REE_SELECCION = {};
let REE_DESTINO = 'cuenta_bancaria';

const ESTADO_REE_LABEL = { solicitado: 'En revisión', aprobado: 'Aprobado · por devolver', devuelto: 'Devuelto', rechazado: 'Rechazado', cancelado: 'Cancelado' };
function pillReembolsoEst(e) {
  const clase = { solicitado: 'pill-ambar', aprobado: 'pill-azul', devuelto: 'pill-verde', rechazado: 'pill-clay', cancelado: 'pill-gris' }[e] || 'pill-gris';
  return '<span class="pill ' + clase + '">' + esc(ESTADO_REE_LABEL[e] || e) + '</span>';
}
const BANCOS_EST = ['Bancolombia', 'Banco de Bogotá', 'Davivienda', 'BBVA Colombia', 'Banco de Occidente', 'Banco Popular', 'Banco AV Villas',
  'Banco Caja Social', 'Scotiabank Colpatria', 'Banco Agrario', 'Bancoomeva', 'Banco Falabella', 'Nu Colombia', 'Otro'];

/* El servidor valida el plazo; aquí solo se decide si mostrar el botón (8 días desde la aprobación del pago). */
function reembolsoEnPlazoEst(p) {
  if (!p.fechaPago) return false;
  return Date.now() <= new Date(p.fechaPago).getTime() + 8 * 86400000;
}

/* ================================================================ MIS REEMBOLSOS */
async function cargarReembolsosEst() {
  const el = document.getElementById('ree-lista'); if (!el) return;
  try { REE_LISTA = await pedirApiEst('GET', '/api/estudiante/reembolsos'); pintarReembolsosEst(); }
  catch (err) { el.innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; }
}

function pintarReembolsosEst() {
  const el = document.getElementById('ree-lista'); if (!el) return;
  if (!REE_LISTA.length) { el.innerHTML = '<div class="empty-state" id="ree-vacio">No tienes solicitudes de reembolso. Puedes pedirlo dentro de los 8 días siguientes a un pago aprobado, si llevas menos del 20 % del curso.</div>'; return; }
  let html = '<div class="tabla-wrap"><table id="ree-tabla"><thead><tr><th>Solicitado</th><th>Cursos</th><th>Devolución</th><th>Valor</th><th>Estado</th><th></th></tr></thead><tbody>';
  REE_LISTA.forEach(r => {
    let detalle = '';
    if (r.estado === 'rechazado' && r.justificacion) detalle += '<div class="text-sm" style="color:var(--clay)">Motivo: ' + esc(r.justificacion) + '</div>';
    if (r.aprobacionAutomatica) detalle += '<div class="text-sm ree-auto" style="color:var(--sage);font-weight:600">✓ Aprobado automáticamente (sin avance)</div>';
    if (r.estado === 'aprobado') detalle += '<div class="text-sm muted">' + (r.aprobacionAutomatica ? 'El' : 'Aprobado; el') + ' contador hará la devolución.</div>';
    if (r.referenciaDevolucion) detalle += '<div class="text-sm muted">Ref. <span style="font-family:monospace">' + esc(r.referenciaDevolucion) + '</span> · ' + fechaEst(r.devueltoEn) + '</div>';
    let acciones = '';
    if (r.idNotaCredito) acciones += '<button class="btn btn-ghost btn-xs ree-nota" onclick="verNotaCreditoEst(' + r.idNotaCredito + ')">⬇ ' + esc(r.numeroNotaCredito) + '</button>';
    if (r.comprobanteDevolucionUrl) acciones += '<button class="btn btn-ghost btn-xs" onclick="window.open(\'' + esc(urlEst(r.comprobanteDevolucionUrl)) + '\',\'_blank\')">📎 Comprobante</button>';
    if (r.estado === 'solicitado') acciones += '<button class="btn btn-ghost btn-xs ree-cancelar" onclick="cancelarReembolsoEst(' + r.id + ')">Cancelar solicitud</button>';
    html += '<tr class="ree-fila" data-id="' + r.id + '" data-estado="' + r.estado + '"><td class="text-sm muted">' + fechaEst(r.solicitadoEn) + '</td>' +
      '<td style="font-weight:600">' + r.items.map(i => esc(i.curso)).join('<br>') + '<div class="text-sm muted" style="font-weight:400">«' + esc(r.motivo) + '»</div>' + detalle + '</td>' +
      '<td class="text-sm">' + esc(destinoReembolsoEst(r)) + '</td>' +
      '<td style="font-weight:700">' + pesosEst(r.monto) + '</td><td>' + pillReembolsoEst(r.estado) + '</td><td><div class="flex gap-6 flex-wrap">' + acciones + '</div></td></tr>';
  });
  el.innerHTML = html + '</tbody></table></div>';
}

function destinoReembolsoEst(r) {
  if (r.medioDevolucion === 'cuenta_bancaria') return r.banco + ' · ' + (r.tipoCuenta === 'corriente' ? 'Corriente' : 'Ahorros') + ' ' + r.numeroCuenta;
  if (r.medioDevolucion === 'nequi') return 'Nequi ' + r.telefonoNequi;
  return r.medioDevolucionNombre || 'Mismo medio de pago';
}

function cancelarReembolsoEst(id) {
  confirmarAccion('¿Cancelar esta solicitud de reembolso? Tus cursos vuelven a estar disponibles.', async function () {
    try { await pedirApiEst('PATCH', '/api/estudiante/reembolsos/' + id + '/cancelacion'); toast('Solicitud cancelada; tus cursos vuelven a estar disponibles'); cargarPagosEst(); }
    catch (err) { toast(err.message, true); }
  }, 'Cancelar solicitud');
}

/* ================================================================ SOLICITUD */
async function abrirReembolsoEst(idPago) {
  renderModalReembolsoEst();
  try { REE_OPCIONES = await pedirApiEst('GET', '/api/estudiante/pagos/' + idPago + '/reembolso'); }
  catch (err) { toast(err.message, true); return; }
  REE_SELECCION = {};
  REE_DESTINO = 'cuenta_bancaria';
  const o = REE_OPCIONES;
  document.getElementById('ree-plazo').innerHTML = 'Pago #' + o.idPago + ' con ' + esc(o.medioNombre) + ' · ' + (o.enPlazo
    ? 'puedes solicitarlo hasta el <strong>' + fechaEst(o.plazoHasta) + '</strong>'
    : '<span style="color:var(--clay)">el plazo de ' + o.diasPlazo + ' días venció el ' + fechaEst(o.plazoHasta) + '</span>');
  let html = '';
  o.cursos.forEach((c, k) => {
    const dis = !c.reembolsable;
    html += '<label class="flex justify-between items-center ree-item" data-id="' + c.idInscripcion + '" data-curso="' + esc(c.curso) + '" style="padding:10px 0;gap:10px;' + (k ? 'border-top:1px solid var(--line);' : '') + (dis ? 'opacity:.6;cursor:not-allowed' : 'cursor:pointer') + '">' +
      '<span class="flex gap-10 items-center"><input type="checkbox" ' + (dis ? 'disabled' : '') + ' onchange="REE_SELECCION[' + c.idInscripcion + ']=this.checked;pintarTotalReembolsoEst()">' +
      '<span><strong>' + esc(c.curso) + '</strong><br><span class="text-sm muted">Avance ' + Math.round(Number(c.avance || 0)) + ' %' + (dis ? ' · <span style="color:var(--clay)" class="ree-motivo-no">' + esc(c.motivo) + '</span>' : '') + '</span></span></span>' +
      '<strong>' + pesosEst(c.monto) + '</strong></label>';
  });
  document.getElementById('ree-cursos').innerHTML = html;
  document.getElementById('ree-motivo').value = '';
  pintarDestinoReembolsoEst();
  pintarTotalReembolsoEst();
  abrirModal('modal-reembolso');
}

function pintarDestinoReembolsoEst() {
  const el = document.getElementById('ree-destino'); const o = REE_OPCIONES;
  if (o.devolucionAlMismoMedio) {
    el.innerHTML = '<div class="card" id="ree-mismo-medio" style="background:var(--sage-soft);padding:12px 14px;box-shadow:none">↩ El dinero se devuelve automáticamente a tu <strong>' + esc(o.medioNombre) + '</strong> cuando se apruebe la solicitud.</div>';
    return;
  }
  const opcion = (v, t) => '<label style="display:flex;gap:6px;align-items:center;cursor:pointer;font-weight:600"><input type="radio" name="ree-destino" value="' + v + '"' + (REE_DESTINO === v ? ' checked' : '') + ' onchange="REE_DESTINO=this.value;pintarDestinoReembolsoEst()">' + t + '</label>';
  let html = '<p class="text-sm" style="margin:0 0 8px">Pagaste por <strong>' + esc(o.medioNombre) + '</strong>: dinos dónde recibir el dinero.</p>' +
    '<div class="flex gap-14 mb-10">' + opcion('cuenta_bancaria', '🏦 Cuenta bancaria') + opcion('nequi', '📱 Nequi') + '</div>';
  if (REE_DESTINO === 'cuenta_bancaria') {
    html += '<div style="display:grid;grid-template-columns:1fr 1fr;gap:0 12px">' +
      '<div class="campo"><label>Banco</label><select id="ree-banco"><option value="">Elige el banco</option>' + BANCOS_EST.map(b => '<option>' + b + '</option>').join('') + '</select></div>' +
      '<div class="campo"><label>Tipo de cuenta</label><select id="ree-tipo"><option value="ahorros">Ahorros</option><option value="corriente">Corriente</option></select></div>' +
      '<div class="campo"><label>Número de cuenta</label><input id="ree-numero" inputmode="numeric" maxlength="20" placeholder="Solo números"></div>' +
      '<div class="campo"><label>Documento del titular</label><input id="ree-documento" inputmode="numeric" maxlength="15"></div></div>' +
      '<div class="campo"><label>Nombre del titular</label><input id="ree-titular" maxlength="150"></div>';
  } else {
    html += '<div class="campo"><label>Número Nequi</label><input id="ree-nequi" inputmode="numeric" maxlength="10" placeholder="3001234567"></div>';
  }
  el.innerHTML = html;
}

function pintarTotalReembolsoEst() {
  const total = REE_OPCIONES.cursos.filter(c => REE_SELECCION[c.idInscripcion]).reduce((s, c) => s + Number(c.monto), 0);
  document.getElementById('ree-total').textContent = pesosEst(total);
  document.getElementById('ree-btn-enviar').disabled = !total;
  // aprobación automática: si todos los cursos elegidos llevan 0 % de avance no pasa por el contador
  const elegidos = REE_OPCIONES.cursos.filter(c => REE_SELECCION[c.idInscripcion]);
  const auto = document.getElementById('ree-auto');
  if (!elegidos.length) { auto.style.display = 'none'; return; }
  const sinAvance = elegidos.every(c => !Number(c.avance));
  auto.style.display = '';
  auto.innerHTML = sinAvance
    ? '<span style="color:var(--sage);font-weight:700">✓ No llevas avance: el reembolso se aprueba automáticamente al enviarlo.</span>'
    : '<span class="muted">Llevas algo de avance en ' + (elegidos.length === 1 ? 'el curso' : 'algún curso') + ': un contador revisará la solicitud y, mientras tanto, quedará en pausa.</span>';
}

async function enviarReembolsoEst() {
  const o = REE_OPCIONES;
  const ids = o.cursos.filter(c => REE_SELECCION[c.idInscripcion]).map(c => c.idInscripcion);
  if (!ids.length) { toast('Selecciona al menos un curso', true); return; }
  const motivo = document.getElementById('ree-motivo').value.trim();
  if (motivo.length < 10) { toast('Cuéntanos el motivo (mínimo 10 caracteres)', true); return; }
  const cuerpo = { idPago: o.idPago, idsInscripcion: ids, motivo };
  if (!o.devolucionAlMismoMedio) {
    cuerpo.medioDevolucion = REE_DESTINO;
    if (REE_DESTINO === 'cuenta_bancaria') {
      Object.assign(cuerpo, { banco: document.getElementById('ree-banco').value, tipoCuenta: document.getElementById('ree-tipo').value,
        numeroCuenta: document.getElementById('ree-numero').value, titular: document.getElementById('ree-titular').value,
        documentoTitular: document.getElementById('ree-documento').value });
    } else cuerpo.telefonoNequi = document.getElementById('ree-nequi').value;
  }
  const btn = document.getElementById('ree-btn-enviar'); btn.disabled = true;
  try {
    const r = await pedirApiEst('POST', '/api/estudiante/reembolsos', cuerpo);
    cerrarModal('modal-reembolso');
    if (r.aprobacionAutomatica)
      toast(r.estado === 'devuelto' ? 'Reembolso aprobado automáticamente: devolvimos ' + pesosEst(r.monto) + ' a tu ' + o.medioNombre + '.'
        : 'Reembolso aprobado automáticamente; te avisaremos cuando hagamos la devolución.');
    else toast('Solicitud enviada. Mientras la revisamos, ' + (ids.length === 1 ? 'el curso queda en pausa.' : 'los cursos quedan en pausa.'));
    cargarPagosEst();
  } catch (err) { toast(err.message, true); btn.disabled = false; }
}

/* ================================================================ NOTA CRÉDITO */
async function verNotaCreditoEst(id) {
  try { const n = await pedirApiEst('GET', '/api/estudiante/notas-credito/' + id); abrirDocumentoVentanaEst(n.numero, notaCreditoHTML(n, pesosEst, fechaEst)); }
  catch (err) { toast(err.message, true); }
}

/* También la usa el portal del contador (misma presentación del documento). */
function notaCreditoHTML(n, pesos, fecha) {
  const filas = n.items.map(i => '<tr><td style="padding:12px;border-bottom:1px solid #E0E2E8">Reembolso — ' + esc(i.curso) + (i.codigoCurso ? ' (' + esc(i.codigoCurso) + ')' : '') + '</td>' +
    '<td style="padding:12px;border-bottom:1px solid #E0E2E8;text-align:right;font-weight:700">' + pesos(i.monto) + '</td></tr>').join('');
  return '<div class="nc-documento" style="background:#fff;border:1px solid #E0E2E8;border-radius:14px;padding:32px;font-family:Inter,Arial,sans-serif;color:#2A2118;max-width:600px">' +
    '<div style="display:flex;justify-content:space-between;align-items:flex-start;border-bottom:3px solid #B5553C;padding-bottom:18px;margin-bottom:22px">' +
    '<div><div style="font-size:22px;font-weight:800;color:#2C4A42">🌿 BioLearn</div><div style="font-size:11px;color:#8A6A4E">' + esc(n.razonSocial || '') + ' · NIT ' + esc(n.nit || '') + '<br>' + esc(n.direccionEmisor || '') + '</div></div>' +
    '<div style="text-align:right"><div style="font-size:11px;letter-spacing:1.5px;color:#B5553C;font-weight:700">NOTA CRÉDITO</div>' +
    '<div style="font-family:monospace;font-weight:800;font-size:17px;color:#2C4A42">' + esc(n.numero) + '</div>' +
    '<div style="font-size:11.5px;color:#8A6A4E">Corrige la factura <strong>' + esc(n.numeroFactura) + '</strong></div></div></div>' +
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:22px;font-size:13px">' +
    '<div><div style="color:#8A6A4E;font-weight:700;text-transform:uppercase;font-size:10.5px;margin-bottom:3px">Cliente</div><div style="font-weight:700">' + esc(n.estudiante) + '</div><div>' + esc(n.correo) + (n.documento ? '<br>Doc. ' + esc(n.documento) : '') + '</div></div>' +
    '<div style="text-align:right"><div style="color:#8A6A4E;font-weight:700;text-transform:uppercase;font-size:10.5px;margin-bottom:3px">Fecha de emisión</div><div style="font-weight:700">' + fecha(n.fechaEmision) + '</div>' +
    '<div style="font-size:11.5px;color:#8A6A4E">Factura del ' + fecha(n.fechaFactura) + '</div></div></div>' +
    '<table style="width:100%;border-collapse:collapse;margin-bottom:18px;font-size:13.5px"><thead><tr style="background:#F6E7E1"><th style="text-align:left;padding:9px 12px;font-size:10.5px;text-transform:uppercase">Concepto</th>' +
    '<th style="text-align:right;padding:9px 12px;font-size:10.5px;text-transform:uppercase">Valor</th></tr></thead><tbody>' + filas + '</tbody>' +
    '<tfoot><tr><td style="padding:12px;font-weight:800;font-size:15px">Total a devolver</td><td style="padding:12px;text-align:right;font-weight:800;font-size:15px">' + pesos(n.montoTotal) + '</td></tr></tfoot></table>' +
    '<div style="background:#F7F3EC;border-radius:10px;padding:14px 16px;margin-bottom:18px;font-size:12.5px"><div>Motivo: ' + esc(n.motivo) + '</div>' +
    '<div style="margin-top:4px">Devolución: <strong>' + esc(n.medioDevolucionNombre || '') + '</strong></div></div>' +
    '<p style="font-size:10.5px;color:#8A6A4E;text-align:center;border-top:1px solid #E0E2E8;padding-top:14px;margin:0">Nota crédito generada electrónicamente por BioLearn · Proyecto académico sin validez fiscal.</p></div>';
}

function abrirDocumentoVentanaEst(titulo, html) {
  const w = window.open('');
  if (!w) { toast('Habilita las ventanas emergentes para ver el documento', true); return; }
  w.document.write('<html><head><title>' + esc(titulo) + '</title></head><body style="margin:0;background:#F4F5F7;padding:32px;display:flex;flex-direction:column;align-items:center;gap:14px">' +
    html + '<button onclick="window.print()" style="padding:9px 18px;border-radius:8px;border:none;background:#2C4A42;color:#fff;font-weight:700;cursor:pointer">Imprimir o guardar PDF</button></body></html>');
  w.document.close();
}

/* ================================================================ MODAL */
function renderModalReembolsoEst() {
  if (document.getElementById('modal-reembolso')) return;
  const d = document.createElement('div');
  d.innerHTML = '<div class="modal-overlay" id="modal-reembolso"><div class="modal" style="max-width:560px">' +
    '<div class="modal-header"><h3>↩ Solicitar reembolso</h3><button class="modal-close" onclick="cerrarModal(\'modal-reembolso\')">✕</button></div>' +
    '<div class="modal-body"><p class="text-sm" id="ree-plazo" style="margin:0 0 6px"></p>' +
    '<p class="text-sm muted" style="margin:0 0 10px">Elige los cursos: se reembolsan los que llevan menos del 20 % de avance. Mientras revisamos la solicitud, esos cursos quedan en pausa.</p>' +
    '<div id="ree-cursos" class="mb-14"></div>' +
    '<div class="flex justify-between" style="font-weight:800;border-top:2px solid var(--line);padding:10px 0 14px"><span>Total a devolver</span><span id="ree-total">$0</span></div>' +
    '<div id="ree-auto" class="text-sm" style="display:none;margin:-6px 0 12px"></div>' +
    '<div class="campo"><label>Motivo</label><textarea id="ree-motivo" rows="3" maxlength="500" placeholder="Ej.: me inscribí en el curso equivocado"></textarea></div>' +
    '<div id="ree-destino"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-reembolso\')">Cancelar</button>' +
    '<button class="btn btn-primary" id="ree-btn-enviar" onclick="enviarReembolsoEst()">Enviar solicitud</button></div></div></div>';
  document.body.appendChild(d.firstElementChild);
}
