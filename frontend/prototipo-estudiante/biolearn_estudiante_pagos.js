/* ================================================================
   BIOLearn — biolearn_estudiante_pagos.js
   Módulo de Pagos, Fase 1, conectado al backend real. Reemplaza «Mis pagos» del prototipo:
   - «Por pagar»: los cursos pendientes de pago (GET /api/estudiante/carrito); el estudiante elige
     los cursos y el medio (tarjeta, PSE, Nequi o transferencia) y crea el pago.
   - En línea: se abre la pasarela de pruebas (mismos datos del sandbox de Wompi). Aprobado → los
     cursos quedan activos y se emite la factura; rechazado → los cursos siguen por pagar.
   - Transferencia: se adjunta el comprobante (PDF, PNG o JPG) y un contador lo confirma.
   - Historial con el estado de cada pago, el motivo de un rechazo y la factura imprimible.
   Se carga después de biolearn_estudiante_inscripciones.js (usa pedirApiEst, API_BASE y urlEst).
================================================================ */
let PAG_CARRITO = null;
let PAG_SELECCION = {};
let PAG_MEDIO = 'tarjeta';
let PAG_LISTA = [];
let PAG_ACTUAL = null;           // pago abierto en la pasarela o en el comprobante

const MEDIOS_EST = [
  { id: 'tarjeta', nombre: 'Tarjeta', icono: '💳', nota: 'Crédito o débito, aprobación inmediata' },
  { id: 'pse', nombre: 'PSE', icono: '🏦', nota: 'Débito desde tu cuenta bancaria' },
  { id: 'nequi', nombre: 'Nequi', icono: '📱', nota: 'Pago desde la app Nequi' },
  { id: 'transferencia', nombre: 'Transferencia', icono: '🧾', nota: 'Adjuntas el comprobante y un contador lo confirma' },
];
const ESTADO_PAGO_LABEL = { pendiente: 'Pendiente', aprobado: 'Aprobado', rechazado: 'Rechazado', cancelado: 'Cancelado', reembolsado: 'Reembolsado' };
function pillPagoEst(e) {
  const clase = { pendiente: 'pill-ambar', aprobado: 'pill-verde', rechazado: 'pill-clay', cancelado: 'pill-gris', reembolsado: 'pill-azul' }[e] || 'pill-gris';
  return '<span class="pill ' + clase + '">' + esc(ESTADO_PAGO_LABEL[e] || e) + '</span>';
}
function pesosEst(v) { return '$' + Number(v || 0).toLocaleString('es-CO'); }
function fechaEst(iso) { return iso ? new Date(iso).toLocaleString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' }) : '—'; }

/* Tras «Confirmar inscripción» en el carrito del catálogo, los cursos con precio se pagan aquí. */
async function confirmarPagoEst() {
  const ids = DATA.carrito.slice();
  let activas = 0, pendientes = 0; const errores = [];
  for (const id of ids) {
    try { const r = await pedirApiEst('POST', '/api/estudiante/inscripciones', { idCurso: id }); r.estado === 'activa' ? activas++ : pendientes++; DATA.carrito = DATA.carrito.filter(x => x !== id); }
    catch (err) { errores.push(err.message); }
  }
  saveData();
  if (errores.length) toast(errores[0], true);
  if (activas) toast(activas + ' inscripción(es) activa(s)');
  CHECKOUT_ABIERTO = false; CATALOGO_DETALLE = null;
  if (pendientes) { PAG_SELECCION = {}; toast('Paga ahora tus ' + pendientes + ' curso(s) con precio'); goTo('pagos'); }
  else goTo('cursos');
}

/* ================================================================ MIS PAGOS */
function renderPagos(cont) {
  cont.innerHTML = '<div class="section-header"><div><h2>Mis pagos</h2><p class="subtitle">Paga tus cursos y consulta tus facturas</p></div></div>' +
    '<div id="pag-por-pagar"><div class="empty-state">Cargando...</div></div>' +
    '<h3 style="font-size:17px;margin:26px 0 12px">Historial de pagos</h3><div id="pag-historial"><div class="empty-state">Cargando...</div></div>';
  renderModalesPagoEst();
  cargarPagosEst();
}

async function cargarPagosEst() {
  try {
    const [carrito, pagos] = await Promise.all([pedirApiEst('GET', '/api/estudiante/carrito'), pedirApiEst('GET', '/api/estudiante/pagos')]);
    PAG_CARRITO = carrito; PAG_LISTA = pagos;
    carrito.items.forEach(i => { if (PAG_SELECCION[i.idInscripcion] === undefined) PAG_SELECCION[i.idInscripcion] = true; });
    pintarPorPagarEst(); pintarHistorialEst();
  } catch (err) {
    const el = document.getElementById('pag-por-pagar'); if (el) el.innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>';
  }
}

function pintarPorPagarEst() {
  const el = document.getElementById('pag-por-pagar'); if (!el || !PAG_CARRITO) return;
  const items = PAG_CARRITO.items;
  if (!items.length) {
    el.innerHTML = '<div class="card" id="pag-sin-pendientes" style="display:flex;gap:14px;align-items:center"><span style="font-size:26px">✅</span><div><strong>No tienes cursos por pagar.</strong>' +
      (PAG_CARRITO.enProceso.length ? '<div class="text-sm muted">' + PAG_CARRITO.enProceso.length + ' curso(s) con un pago en proceso; revisa el historial.</div>' : '<div class="text-sm muted">Los cursos con precio que agregues desde el catálogo aparecerán aquí.</div>') + '</div></div>';
    return;
  }
  const total = items.filter(i => PAG_SELECCION[i.idInscripcion]).reduce((s, i) => s + Number(i.monto), 0);
  let html = '<div class="flex gap-14 flex-wrap" style="align-items:start"><div style="flex:1.4;min-width:300px"><div class="card" id="pag-items">' +
    '<div class="text-sm mb-10" style="font-weight:700;text-transform:uppercase;color:var(--amber)">Cursos por pagar</div>';
  items.forEach((i, k) => {
    html += '<label class="flex justify-between items-center pag-item" data-id="' + i.idInscripcion + '" data-curso="' + esc(i.curso) + '" style="padding:12px 0;cursor:pointer;' + (k ? 'border-top:1px solid var(--line)' : '') + '">' +
      '<span class="flex items-center gap-10"><input type="checkbox" ' + (PAG_SELECCION[i.idInscripcion] ? 'checked' : '') + ' onchange="PAG_SELECCION[' + i.idInscripcion + ']=this.checked;pintarPorPagarEst()">' +
      '<span><span style="font-weight:700">' + esc(i.curso) + '</span><br><span class="text-sm muted" style="font-family:monospace">' + esc(i.codigoCurso || '') + '</span></span></span>' +
      '<span style="font-weight:700">' + pesosEst(i.monto) + '</span></label>';
  });
  html += '</div></div><div style="flex:1;min-width:280px"><div class="card"><div class="text-sm mb-10" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft)">Medio de pago</div>';
  MEDIOS_EST.forEach(m => {
    const sel = PAG_MEDIO === m.id;
    html += '<div class="pag-medio" data-medio="' + m.id + '" onclick="PAG_MEDIO=\'' + m.id + '\';pintarPorPagarEst()" style="display:flex;gap:10px;align-items:center;padding:10px 12px;margin-bottom:8px;border-radius:10px;cursor:pointer;border:2px solid ' + (sel ? 'var(--blue)' : 'var(--line)') + ';background:' + (sel ? 'var(--blue-soft)' : '#fff') + '">' +
      '<span style="font-size:20px">' + m.icono + '</span><span><strong>' + m.nombre + '</strong><br><span class="text-sm muted">' + m.nota + '</span></span></div>';
  });
  html += '<div class="flex justify-between" style="font-size:16px;font-weight:800;margin:14px 0 10px"><span>Total</span><span id="pag-total">' + pesosEst(total) + '</span></div>' +
    '<button class="btn btn-primary w-full" id="pag-btn-pagar" style="justify-content:center" ' + (total > 0 ? '' : 'disabled') + ' onclick="crearPagoEst()">' +
    (PAG_MEDIO === 'transferencia' ? 'Registrar transferencia' : 'Pagar ' + pesosEst(total)) + '</button></div></div></div>';
  el.innerHTML = html;
}

async function crearPagoEst() {
  const ids = PAG_CARRITO.items.filter(i => PAG_SELECCION[i.idInscripcion]).map(i => i.idInscripcion);
  if (!ids.length) { toast('Selecciona al menos un curso', true); return; }
  const btn = document.getElementById('pag-btn-pagar'); if (btn) btn.disabled = true;
  try {
    const pago = await pedirApiEst('POST', '/api/estudiante/pagos', { idsInscripcion: ids, medio: PAG_MEDIO });
    PAG_ACTUAL = pago;
    if (pago.enLinea) abrirPasarelaEst(pago); else abrirComprobanteEst(pago);
    cargarPagosEst();
  } catch (err) { toast(err.message, true); if (btn) btn.disabled = false; }
}

/* ================================================================ PASARELA DE PRUEBAS */
function abrirPasarelaEst(pago) {
  PAG_ACTUAL = pago;
  const medio = pago.medio;
  let campos;
  if (medio === 'wompi_tarjeta') campos = '<div class="campo"><label>Número de la tarjeta</label><input id="pas-tarjeta" inputmode="numeric" placeholder="4242 4242 4242 4242" maxlength="19"></div>' +
    '<div class="flex gap-10"><div class="campo" style="flex:1"><label>Vence</label><input value="12/30" disabled></div><div class="campo" style="flex:1"><label>CVC</label><input value="123" disabled></div></div>' +
    '<p class="text-sm muted">Datos de prueba: <code>4242 4242 4242 4242</code> aprueba · <code>4111 1111 1111 1111</code> rechaza.</p>';
  else if (medio === 'wompi_nequi') campos = '<div class="campo"><label>Celular Nequi</label><input id="pas-telefono" inputmode="numeric" placeholder="3991111111" maxlength="10"></div>' +
    '<p class="text-sm muted">Datos de prueba: <code>3991111111</code> aprueba · <code>3992222222</code> rechaza.</p>';
  else campos = '<div class="campo"><label>Banco</label><select id="pas-banco"><option value="">Selecciona tu banco</option><option value="1">Banco que aprueba (prueba)</option><option value="2">Banco que rechaza (prueba)</option></select></div>' +
    '<p class="text-sm muted">PSE de prueba: el banco 1 aprueba y el 2 rechaza.</p>';
  document.getElementById('pas-titulo').textContent = 'Pagar ' + pesosEst(pago.monto) + ' con ' + pago.medioNombre;
  document.getElementById('pas-cursos').innerHTML = pago.items.map(i => esc(i.curso)).join(' · ');
  document.getElementById('pas-campos').innerHTML = campos;
  document.getElementById('pas-resultado').innerHTML = '';
  document.getElementById('pas-btn').disabled = false; document.getElementById('pas-btn').style.display = '';
  abrirModal('modal-pasarela');
}

async function pagarPasarelaEst() {
  const p = PAG_ACTUAL; if (!p) return;
  const datos = {};
  if (p.medio === 'wompi_tarjeta') datos.numeroTarjeta = (document.getElementById('pas-tarjeta').value || '').replace(/\s/g, '');
  else if (p.medio === 'wompi_nequi') datos.telefono = document.getElementById('pas-telefono').value || '';
  else datos.banco = document.getElementById('pas-banco').value || '';
  const btn = document.getElementById('pas-btn'); btn.disabled = true;
  try {
    const r = await pedirApiEst('POST', '/api/estudiante/pagos/' + p.id + '/pasarela', datos);
    PAG_ACTUAL = r;
    const ok = r.estado === 'aprobado';
    document.getElementById('pas-resultado').innerHTML = '<div id="pas-estado" data-estado="' + r.estado + '" style="border-radius:10px;padding:14px;margin-top:6px;background:' + (ok ? 'var(--sage-soft,#E4EEE9)' : 'var(--clay-soft)') + '">' +
      (ok ? '<strong>✓ Pago aprobado</strong><div class="text-sm">Referencia ' + esc(r.referencia) + ' · Factura ' + esc(r.numeroFactura || '') + '. Tus cursos ya están activos.</div>'
          : '<strong>✕ Pago rechazado</strong><div class="text-sm">' + esc(r.motivoRechazo || '') + '. Los cursos siguen en «Por pagar»; puedes intentarlo de nuevo.</div>') + '</div>';
    btn.style.display = 'none';
    toast(ok ? 'Pago aprobado · factura ' + r.numeroFactura : 'Pago rechazado: ' + r.motivoRechazo, !ok);
    cargarPagosEst();
  } catch (err) { toast(err.message, true); btn.disabled = false; }
}

/* ================================================================ TRANSFERENCIA: COMPROBANTE */
function abrirComprobanteEst(pago) {
  PAG_ACTUAL = pago;
  document.getElementById('comp-resumen').innerHTML = 'Transfiere <strong>' + pesosEst(pago.monto) + '</strong> a la cuenta de ahorros <strong>BioLearn S.A.S. · 000-123456-78</strong> y adjunta el comprobante.<br>' +
    '<span class="text-sm muted">' + pago.items.map(i => esc(i.curso)).join(' · ') + '</span>';
  document.getElementById('comp-archivo').value = '';
  abrirModal('modal-comprobante');
}

async function subirComprobanteEst() {
  const p = PAG_ACTUAL; const input = document.getElementById('comp-archivo');
  const f = input.files && input.files[0];
  if (!f) { toast('Elige el archivo del comprobante', true); return; }
  if (f.size > 10 * 1024 * 1024) { toast('El archivo supera el tamaño máximo de 10 MB', true); return; }
  const btn = document.getElementById('comp-btn'); btn.disabled = true;
  try {
    const fd = new FormData(); fd.append('archivo', f);
    const h = authHeadersEst(); delete h['Content-Type'];
    const r = await fetch(API_BASE + '/api/archivos', { method: 'POST', headers: h, body: fd });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) throw new Error((data.mensaje || 'Error ' + r.status) + (data.detalles && data.detalles.length ? ' (' + data.detalles.join('; ') + ')' : ''));
    await pedirApiEst('PUT', '/api/estudiante/pagos/' + p.id + '/comprobante', { url: data.url });
    cerrarModal('modal-comprobante');
    toast('Comprobante enviado. Un contador confirmará tu pago.');
    cargarPagosEst();
  } catch (err) { toast(err.message, true); }
  btn.disabled = false;
}

function cancelarPagoEst(id) {
  confirmarAccion('¿Cancelar este pago? Los cursos volverán a «Por pagar».', async function () {
    try { await pedirApiEst('PATCH', '/api/estudiante/pagos/' + id + '/cancelacion'); toast('Pago cancelado'); cargarPagosEst(); }
    catch (err) { toast(err.message, true); }
  }, 'Cancelar pago');
}

/* ================================================================ HISTORIAL */
function pagoListaEst(id) { return PAG_LISTA.find(x => x.id === id); }
function pintarHistorialEst() {
  const el = document.getElementById('pag-historial'); if (!el) return;
  if (!PAG_LISTA.length) { el.innerHTML = '<div class="empty-state">Aún no tienes pagos.</div>'; return; }
  let html = '<div class="tabla-wrap"><table id="pag-tabla"><thead><tr><th>Fecha</th><th>Cursos</th><th>Medio</th><th>Monto</th><th>Estado</th><th></th></tr></thead><tbody>';
  PAG_LISTA.forEach(p => {
    let acciones = '';
    if (p.estado === 'aprobado' && p.idFactura) acciones += '<button class="btn btn-ghost btn-xs" onclick="verFacturaEst(' + p.idFactura + ')">⬇ ' + esc(p.numeroFactura) + '</button>';
    if (p.estado === 'pendiente' && p.enLinea) acciones += '<button class="btn btn-primary btn-xs" onclick="abrirPasarelaEst(pagoListaEst(' + p.id + '))">Pagar</button>';
    if (p.estado === 'pendiente' && !p.enLinea && !p.comprobanteUrl) acciones += '<button class="btn btn-primary btn-xs" onclick="abrirComprobanteEst(pagoListaEst(' + p.id + '))">Adjuntar comprobante</button>';
    if (p.estado === 'pendiente' && !p.enLinea && p.comprobanteUrl) acciones += '<span class="text-sm muted">Esperando confirmación</span>';
    if (p.estado === 'pendiente') acciones += '<button class="btn btn-ghost btn-xs" onclick="cancelarPagoEst(' + p.id + ')">Cancelar</button>';
    html += '<tr data-id="' + p.id + '" data-estado="' + p.estado + '"><td class="text-sm muted">' + fechaEst(p.fechaPago || p.creadoEn) + '</td>' +
      '<td style="font-weight:600">' + p.items.map(i => esc(i.curso)).join('<br>') + (p.motivoRechazo ? '<div class="text-sm" style="color:var(--clay);font-weight:400">Motivo: ' + esc(p.motivoRechazo) + '</div>' : '') + '</td>' +
      '<td class="text-sm">' + esc(p.medioNombre) + (p.referencia ? '<br><span class="muted" style="font-family:monospace">' + esc(p.referencia) + '</span>' : '') + '</td>' +
      '<td style="font-weight:700">' + pesosEst(p.monto) + '</td><td>' + pillPagoEst(p.estado) + '</td><td><div class="flex gap-6 flex-wrap">' + acciones + '</div></td></tr>';
  });
  el.innerHTML = html + '</tbody></table></div>';
}

/* ================================================================ FACTURA */
async function verFacturaEst(id) {
  try { const f = await pedirApiEst('GET', '/api/estudiante/facturas/' + id); abrirFacturaVentana(f); }
  catch (err) { toast(err.message, true); }
}

function facturaApiHTML(f) {
  const filas = f.items.map(i => '<tr><td style="padding:12px;border-bottom:1px solid #E0E2E8">Inscripción — ' + esc(i.curso) + (i.codigoCurso ? ' (' + esc(i.codigoCurso) + ')' : '') + '</td>' +
    '<td style="padding:12px;border-bottom:1px solid #E0E2E8;text-align:right;font-weight:700">' + pesosEst(i.monto) + '</td></tr>').join('');
  return '<div style="background:#fff;border:1px solid #E0E2E8;border-radius:14px;padding:32px;font-family:Inter,Arial,sans-serif;color:#2A2118;max-width:600px">' +
    '<div style="display:flex;justify-content:space-between;align-items:flex-start;border-bottom:3px solid #2C4A42;padding-bottom:18px;margin-bottom:22px">' +
    '<div><div style="font-size:22px;font-weight:800;color:#2C4A42">🌿 BioLearn</div><div style="font-size:11px;color:#8A6A4E">' + esc(f.razonSocial || '') + ' · NIT ' + esc(f.nit || '') + '<br>' + esc(f.direccionEmisor || '') + '</div></div>' +
    '<div style="text-align:right"><div style="font-size:11px;letter-spacing:1.5px;color:#8A6A4E;font-weight:700">FACTURA DE VENTA</div>' +
    '<div style="font-family:monospace;font-weight:800;font-size:17px;color:#2C4A42">' + esc(f.numero) + '</div></div></div>' +
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:22px;font-size:13px">' +
    '<div><div style="color:#8A6A4E;font-weight:700;text-transform:uppercase;font-size:10.5px;margin-bottom:3px">Facturar a</div><div style="font-weight:700">' + esc(f.estudiante) + '</div><div>' + esc(f.correo) + (f.documento ? '<br>Doc. ' + esc(f.documento) : '') + '</div></div>' +
    '<div style="text-align:right"><div style="color:#8A6A4E;font-weight:700;text-transform:uppercase;font-size:10.5px;margin-bottom:3px">Fecha de emisión</div><div style="font-weight:700">' + fechaEst(f.fechaEmision) + '</div></div></div>' +
    '<table style="width:100%;border-collapse:collapse;margin-bottom:18px;font-size:13.5px"><thead><tr style="background:#E4EEE9"><th style="text-align:left;padding:9px 12px;font-size:10.5px;color:#1C332C;text-transform:uppercase">Concepto</th>' +
    '<th style="text-align:right;padding:9px 12px;font-size:10.5px;color:#1C332C;text-transform:uppercase">Valor</th></tr></thead><tbody>' + filas + '</tbody>' +
    '<tfoot><tr><td style="padding:12px;font-weight:800;font-size:15px">Total</td><td style="padding:12px;text-align:right;font-weight:800;font-size:15px">' + pesosEst(f.montoTotal) + '</td></tr></tfoot></table>' +
    '<div style="display:flex;justify-content:space-between;align-items:center;background:#F7F3EC;border-radius:10px;padding:14px 16px;margin-bottom:18px;font-size:12.5px">' +
    '<div>Medio de pago: <strong>' + esc(f.medioNombre) + '</strong>' + (f.referencia ? ' · Ref. ' + esc(f.referencia) : '') + '</div>' +
    '<span style="background:#E5EDE6;color:#5C7A63;font-weight:700;padding:4px 12px;border-radius:20px">' + (f.estado === 'emitida' ? 'Pagada' : 'Anulada') + '</span></div>' +
    '<p style="font-size:10.5px;color:#8A6A4E;text-align:center;border-top:1px solid #E0E2E8;padding-top:14px;margin:0">Factura generada electrónicamente por BioLearn · Proyecto académico sin validez fiscal.</p></div>';
}

function abrirFacturaVentana(f) {
  const w = window.open('');
  if (!w) { toast('Habilita las ventanas emergentes para ver la factura', true); return; }
  w.document.write('<html><head><title>' + esc(f.numero) + '</title></head><body style="margin:0;background:#F4F5F7;padding:32px;display:flex;flex-direction:column;align-items:center;gap:14px">' +
    facturaApiHTML(f) + '<button onclick="window.print()" style="padding:9px 18px;border-radius:8px;border:none;background:#2C4A42;color:#fff;font-weight:700;cursor:pointer">Imprimir o guardar PDF</button></body></html>');
  w.document.close();
}

/* ================================================================ MODALES */
function renderModalesPagoEst() {
  if (!document.getElementById('modal-pasarela')) {
    const d = document.createElement('div');
    d.innerHTML = '<div class="modal-overlay" id="modal-pasarela"><div class="modal" style="max-width:440px">' +
      '<div class="modal-header"><h3>🔒 Pasarela de pruebas</h3><button class="modal-close" onclick="cerrarModal(\'modal-pasarela\')">✕</button></div>' +
      '<div class="modal-body"><div style="font-weight:800;font-size:16px" id="pas-titulo"></div><div class="text-sm muted mb-14" id="pas-cursos"></div>' +
      '<div id="pas-campos"></div><div id="pas-resultado"></div></div>' +
      '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-pasarela\')">Cerrar</button>' +
      '<button class="btn btn-primary" id="pas-btn" onclick="pagarPasarelaEst()">Pagar</button></div></div></div>';
    document.body.appendChild(d.firstElementChild);
  }
  if (!document.getElementById('modal-comprobante')) {
    const d = document.createElement('div');
    d.innerHTML = '<div class="modal-overlay" id="modal-comprobante"><div class="modal" style="max-width:440px">' +
      '<div class="modal-header"><h3>Comprobante de transferencia</h3><button class="modal-close" onclick="cerrarModal(\'modal-comprobante\')">✕</button></div>' +
      '<div class="modal-body"><p id="comp-resumen" style="font-size:14px;line-height:1.7"></p>' +
      '<div class="campo"><label>Comprobante (PDF, PNG o JPG, máximo 10 MB)</label><input type="file" id="comp-archivo" accept=".pdf,.png,.jpg,.jpeg"></div>' +
      '<p class="text-sm muted">Puedes adjuntarlo después desde el historial de pagos.</p></div>' +
      '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-comprobante\')">Después</button>' +
      '<button class="btn btn-primary" id="comp-btn" onclick="subirComprobanteEst()">Enviar comprobante</button></div></div></div>';
    document.body.appendChild(d.firstElementChild);
  }
}
