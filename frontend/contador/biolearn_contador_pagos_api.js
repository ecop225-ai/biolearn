/* ================================================================
   BIOLearn — biolearn_contador_pagos_api.js
   Módulo de Pagos, Fase 1, conectado al backend real. Reemplaza las secciones «Pagos» y «Facturas»
   del prototipo del contador (las demás siguen con datos de demostración):
   - Pagos: filtros por estado, medio, búsqueda y fechas. Las transferencias con comprobante se
     confirman (activa los cursos y emite la factura) o se rechazan con un motivo. Los pagos en
     línea los resuelve la pasarela. El contador auditor solo consulta: no ve Confirmar, Rechazar ni
     Reenviar (GET /api/pagos/permisos) y, si llamara la API, el servidor responde 403.
   - Facturas: búsqueda y fechas, vista imprimible y reenvío al correo del estudiante. Ya no hay
     «Emitir factura manual»: cada factura nace de un pago aprobado.
   Se carga al final de biolearn_contador_prototipo.html.
================================================================ */
/* URL del backend: en el equipo de desarrollo (localhost, 127.0.0.1 o archivo abierto directamente) es el backend
   de IntelliJ en el puerto 8080; en el servidor, Apache publica el sitio y reenvía /api al backend, así que se usa
   la misma dirección del sitio (despliegue/README.md). */
const API_BASE_CONT = (['localhost', '127.0.0.1', ''].includes(location.hostname) ? 'http://localhost:8080' : location.origin);
let CP_FILTRO = { estado: 'pendiente', medio: '', busqueda: '', desde: '', hasta: '' };
let CP_PAGOS = [];
let CF_FILTRO = { busqueda: '', desde: '', hasta: '' };
let CF_FACTURAS = [];
let CONT_PUEDE_RESOLVER = null;   // false para el contador auditor: no ve Confirmar, Rechazar ni Reenviar

/* ---------------------------------------------------------------- sesión y comunicación */
/* Nombre real del contador que inició sesión (el prototipo traía «Camila Ortiz» fijo). CONTADOR es un
   objeto de biolearn_contador_core.js: se actualizan sus datos al cargar, antes del primer render. */
(function () {
  try {
    const u = JSON.parse(sessionStorage.getItem('biolearn_usuario') || 'null');
    if (u) {
      CONTADOR.nombre = ((u.nombre || '') + ' ' + (u.apellido || '')).trim() || 'Contador';
      CONTADOR.cargo = 'Contador';
      CONTADOR.email = u.correo || '';
    }
  } catch (e) { /* sin sesión: se conserva el dato de demostración */ }
})();

/* Cerrar sesión de verdad: borra el token y vuelve al inicio de sesión (reemplaza la versión del prototipo). */
function confirmarCerrarSesion() {
  confirmarAccion('¿Cerrar sesión? Volverás a la pantalla de inicio de sesión.', function () {
    sessionStorage.removeItem('biolearn_token');
    sessionStorage.removeItem('biolearn_usuario');
    toast('Sesión cerrada');
    setTimeout(function () { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; }, 500);
  }, 'Cerrar sesión');
}

document.addEventListener('DOMContentLoaded', function () {
  let u = null;
  try { u = JSON.parse(sessionStorage.getItem('biolearn_usuario') || 'null'); } catch (e) { u = null; }
  if (!sessionStorage.getItem('biolearn_token') || !u) { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; return; }
  if (u.rol !== 'contador') {
    document.body.innerHTML = '<div id="acceso-denegado" style="max-width:480px;margin:12vh auto;padding:32px;background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(27,50,44,.15);text-align:center">' +
      '<div style="font-size:40px">🔒</div><h2 style="margin:12px 0 8px">Acceso solo para contadores</h2><p style="color:#555">Tu cuenta (' + esc(u.nombre || '') + ', rol ' + esc(u.rol || '') + ') no tiene acceso a este portal.</p></div>';
    setTimeout(() => { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; }, 2500);
    return;
  }
  cargarPermisosCont();   // muestra en el menú el cargo real (Administrador financiero, Auxiliar contable o Auditor)
});

async function pedirApiCont(metodo, ruta, cuerpo) {
  const token = sessionStorage.getItem('biolearn_token');
  if (!token) { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; throw new Error('Sesión no válida'); }
  const op = { method: metodo, headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token } };
  if (cuerpo !== undefined) op.body = JSON.stringify(cuerpo);
  const r = await fetch(API_BASE_CONT + ruta, op);
  if (r.status === 401) {
    sessionStorage.removeItem('biolearn_token'); sessionStorage.removeItem('biolearn_usuario');
    toast('Tu sesión expiró. Inicia sesión nuevamente.', true);
    setTimeout(() => { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; }, 1200);
    throw new Error('Sesión expirada');
  }
  const data = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error((data.mensaje || 'Error ' + r.status) + (data.detalles && data.detalles.length ? ' (' + data.detalles.join('; ') + ')' : ''));
  return data;
}
function qs(o) { const p = Object.entries(o).filter(([, v]) => v !== '' && v != null).map(([k, v]) => k + '=' + encodeURIComponent(v)); return p.length ? '?' + p.join('&') : ''; }
function urlCont(u) { return u && u.startsWith('/api/') ? API_BASE_CONT + u : u; }
function fechaHoraCont(iso) { return iso ? new Date(iso).toLocaleString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' }) : '—'; }
const ESTADO_PAGO_CONT = { pendiente: 'Pendiente', aprobado: 'Aprobado', rechazado: 'Rechazado', cancelado: 'Cancelado', reembolsado: 'Reembolsado' };
function pillPagoCont(e) {
  const clase = { pendiente: 'pill-ambar', aprobado: 'pill-verde', rechazado: 'pill-clay', cancelado: 'pill-gris', reembolsado: 'pill-azul' }[e] || 'pill-gris';
  return '<span class="pill ' + clase + '">' + esc(ESTADO_PAGO_CONT[e] || e) + '</span>';
}

/* El auditor solo consulta: se pregunta una vez al servidor qué puede hacer este contador. */
const CARGO_CONTADOR = { administrador_financiero: 'Administrador financiero', auxiliar: 'Auxiliar contable', auditor: 'Auditor' };
async function cargarPermisosCont() {
  if (CONT_PUEDE_RESOLVER !== null) return;
  try {
    const p = await pedirApiCont('GET', '/api/pagos/permisos');
    // Si la consulta responde sin el campo, se muestran los botones: el servidor igual rechaza al auditor con 403.
    CONT_PUEDE_RESOLVER = p.puedeResolver !== false;
    if (CARGO_CONTADOR[p.rolContador]) { CONTADOR.cargo = CARGO_CONTADOR[p.rolContador]; renderSidebar(); }
    return;
  } catch (e) { CONT_PUEDE_RESOLVER = true; return; }
}
function avisoAuditorCont() {
  return CONT_PUEDE_RESOLVER === false ? '<div id="cont-aviso-auditor" class="card mb-14" style="border-left:4px solid var(--blue);padding:12px 16px"><strong>Rol auditor:</strong> puedes consultar pagos y facturas, pero no confirmarlos, rechazarlos ni reenviarlos.</div>' : '';
}

/* ================================================================ PAGOS */
function renderPagos(cont) {
  let html = '<div class="section-header"><div><h2>Pagos</h2><p class="subtitle">Confirma las transferencias de los estudiantes; los pagos en línea los resuelve la pasarela</p></div></div>';
  html += '<div id="cp-resumen" style="display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:20px"></div>';
  html += '<div class="flex gap-8 flex-wrap mb-14">';
  [['', 'Todos'], ['pendiente', 'Pendientes'], ['aprobado', 'Aprobados'], ['rechazado', 'Rechazados'], ['cancelado', 'Cancelados'], ['reembolsado', 'Reembolsados']].forEach(([v, t]) => {
    const a = CP_FILTRO.estado === v;
    html += '<button class="cp-chip" data-estado="' + v + '" onclick="CP_FILTRO.estado=\'' + v + '\';render()" style="padding:7px 14px;border-radius:20px;font-size:13px;font-weight:700;cursor:pointer;border:1px solid ' + (a ? 'var(--blue)' : 'var(--line)') + ';background:' + (a ? 'var(--blue-soft)' : '#fff') + ';color:' + (a ? 'var(--blue-deep)' : 'var(--ink-soft)') + '">' + t + '</button>';
  });
  html += '</div><div class="flex gap-10 flex-wrap mb-16">' +
    '<input id="cp-buscar" placeholder="Buscar estudiante, correo, curso o referencia..." value="' + esc(CP_FILTRO.busqueda) + '" onkeydown="if(event.key===\'Enter\'){CP_FILTRO.busqueda=this.value;cargarPagosCont()}" style="flex:1;min-width:240px">' +
    '<select id="cp-medio" onchange="CP_FILTRO.medio=this.value;cargarPagosCont()" style="width:160px">' + [['', 'Todos los medios'], ['tarjeta', 'Tarjeta'], ['pse', 'PSE'], ['nequi', 'Nequi'], ['transferencia', 'Transferencia']].map(([v, t]) => '<option value="' + v + '"' + (CP_FILTRO.medio === v ? ' selected' : '') + '>' + t + '</option>').join('') + '</select>' +
    '<input type="date" id="cp-desde" value="' + CP_FILTRO.desde + '" onchange="CP_FILTRO.desde=this.value;cargarPagosCont()" title="Desde" style="width:170px">' +
    '<input type="date" id="cp-hasta" value="' + CP_FILTRO.hasta + '" onchange="CP_FILTRO.hasta=this.value;cargarPagosCont()" title="Hasta" style="width:170px">' +
    '<button class="btn btn-ghost" onclick="CP_FILTRO.busqueda=document.getElementById(\'cp-buscar\').value;cargarPagosCont()">Buscar</button></div>';
  html += '<div id="cp-lista"><div class="empty-state">Cargando pagos...</div></div>';
  cont.innerHTML = html;
  renderModalesPagoCont();
  cargarPagosCont();
}

async function cargarPagosCont() {
  try {
    await cargarPermisosCont();
    const [lista, pendientes] = await Promise.all([
      pedirApiCont('GET', '/api/pagos' + qs(CP_FILTRO)),
      pedirApiCont('GET', '/api/pagos?estado=pendiente&medio=transferencia'),
    ]);
    CP_PAGOS = lista;
    pintarResumenCont(pendientes);
    pintarPagosCont();
  } catch (err) { const el = document.getElementById('cp-lista'); if (el) el.innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; }
}

function pintarResumenCont(pendTransf) {
  const el = document.getElementById('cp-resumen'); if (!el) return;
  const conComp = pendTransf.filter(p => p.comprobanteUrl).length;
  const aprob = CP_PAGOS.filter(p => p.estado === 'aprobado');
  const total = aprob.reduce((s, p) => s + Number(p.monto), 0);
  const tarjeta = (t, v, sub, color) => '<div class="card"><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">' + t + '</div><div style="font-size:24px;font-weight:800;color:' + color + '">' + v + '</div><div class="text-sm muted">' + sub + '</div></div>';
  el.innerHTML = tarjeta('Por confirmar', conComp, 'Transferencias con comprobante', 'var(--amber)') +
    tarjeta('Sin comprobante', pendTransf.length - conComp, 'Transferencias en espera del estudiante', 'var(--ink-soft)') +
    tarjeta('Aprobado en la lista', fmtMoney(total), aprob.length + ' pago(s) con los filtros actuales', 'var(--sage)');
}

function pintarPagosCont() {
  const el = document.getElementById('cp-lista'); if (!el) return;
  let html = '<div class="tabla-wrap"><table id="cp-tabla"><thead><tr style="background:var(--blue-soft)">' +
    ['Estudiante', 'Cursos', 'Creado', 'Monto', 'Medio', 'Estado', ''].map(h => '<th style="color:var(--blue-deep)">' + h + '</th>').join('') + '</tr></thead><tbody>';
  CP_PAGOS.forEach(p => {
    let acc = '';
    if (p.comprobanteUrl) acc += '<button class="btn btn-ghost btn-xs" onclick="window.open(\'' + esc(urlCont(p.comprobanteUrl)) + '\',\'_blank\')">📎 Comprobante</button>';
    if (p.estado === 'pendiente' && !p.enLinea && !CONT_PUEDE_RESOLVER) acc += '<span class="text-sm muted">' + (p.comprobanteUrl ? 'Por confirmar' : 'Sin comprobante') + '</span>';
    else if (p.estado === 'pendiente' && !p.enLinea && p.comprobanteUrl) acc += '<button class="btn btn-sm cp-confirmar" style="background:var(--sage);color:#fff" onclick="abrirConfirmarPagoCont(' + p.id + ')">✓ Confirmar</button><button class="btn btn-danger btn-xs cp-rechazar" onclick="abrirRechazoPagoCont(' + p.id + ')">✕ Rechazar</button>';
    else if (p.estado === 'pendiente' && !p.enLinea && !p.comprobanteUrl) acc += '<span class="text-sm muted">Sin comprobante</span><button class="btn btn-danger btn-xs cp-rechazar" onclick="abrirRechazoPagoCont(' + p.id + ')">✕ Rechazar</button>';
    if (p.estado === 'pendiente' && p.enLinea) acc += '<span class="text-sm muted">En la pasarela</span>';
    if (p.numeroFactura) acc += '<button class="btn btn-ghost btn-xs" onclick="verFacturaCont(' + p.idFactura + ')">' + esc(p.numeroFactura) + '</button>';
    html += '<tr class="cp-fila" data-id="' + p.id + '" data-estado="' + p.estado + '" data-correo="' + esc(p.correo) + '"' + (p.estado === 'rechazado' ? ' style="background:var(--clay-soft)"' : '') + '>' +
      '<td><div style="font-weight:700">' + esc(p.estudiante) + '</div><div class="text-sm muted">' + esc(p.correo) + '</div></td>' +
      '<td class="text-sm">' + p.items.map(i => esc(i.curso)).join('<br>') + (p.motivoRechazo ? '<div style="color:var(--clay)">Motivo: ' + esc(p.motivoRechazo) + '</div>' : '') + '</td>' +
      '<td class="text-sm muted">' + fechaHoraCont(p.creadoEn) + '</td><td style="font-weight:700">' + fmtMoney(p.monto) + '</td>' +
      '<td class="text-sm">' + esc(p.medioNombre) + (p.referencia ? '<br><span class="muted" style="font-family:monospace">' + esc(p.referencia) + '</span>' : '') + '</td>' +
      '<td>' + pillPagoCont(p.estado) + (p.resueltoPor ? '<div class="text-sm muted">por ' + esc(p.resueltoPor) + '</div>' : '') + '</td>' +
      '<td><div class="flex gap-6 flex-wrap">' + acc + '</div></td></tr>';
  });
  if (!CP_PAGOS.length) html += '<tr><td colspan="7" class="empty-state">No hay pagos con estos filtros.</td></tr>';
  el.innerHTML = avisoAuditorCont() + html + '</tbody></table></div>';
}

function pagoCont(id) { return CP_PAGOS.find(x => x.id === id); }

function abrirConfirmarPagoCont(id) {
  const p = pagoCont(id);
  document.getElementById('cpc-id').value = id;
  document.getElementById('cpc-resumen').innerHTML = 'Estudiante: <strong>' + esc(p.estudiante) + '</strong><br>Cursos: ' + p.items.map(i => esc(i.curso)).join(', ') +
    '<br>Monto: <strong>' + fmtMoney(p.monto) + '</strong> · Transferencia' + (p.comprobanteUrl ? ' · <a href="' + esc(urlCont(p.comprobanteUrl)) + '" target="_blank">ver comprobante</a>' : '');
  abrirModal('modal-cp-confirmar');
}
async function confirmarPagoCont() {
  const id = Number(document.getElementById('cpc-id').value);
  const btn = document.getElementById('cpc-btn'); btn.disabled = true;
  try {
    const r = await pedirApiCont('PATCH', '/api/pagos/' + id + '/confirmacion');
    cerrarModal('modal-cp-confirmar');
    toast('Pago de ' + r.estudiante + ' confirmado · factura ' + r.numeroFactura + ' emitida y enviada');
    cargarPagosCont();
  } catch (err) { toast(err.message, true); }
  btn.disabled = false;
}

function abrirRechazoPagoCont(id) {
  const p = pagoCont(id);
  document.getElementById('cpr-id').value = id;
  document.getElementById('cpr-resumen').innerHTML = 'Estudiante: <strong>' + esc(p.estudiante) + '</strong> · ' + fmtMoney(p.monto);
  document.getElementById('cpr-motivo').value = '';
  abrirModal('modal-cp-rechazo');
}
async function rechazarPagoCont() {
  const id = Number(document.getElementById('cpr-id').value);
  const motivo = document.getElementById('cpr-motivo').value.trim();
  const btn = document.getElementById('cpr-btn'); btn.disabled = true;
  try {
    await pedirApiCont('PATCH', '/api/pagos/' + id + '/rechazo', { motivo });
    cerrarModal('modal-cp-rechazo'); toast('Pago rechazado; el estudiante verá el motivo');
    cargarPagosCont();
  } catch (err) { toast(err.message, true); }
  btn.disabled = false;
}

/* ================================================================ FACTURAS */
function renderFacturas(cont) {
  cont.innerHTML = '<div class="section-header"><div><h2>Facturas</h2><p class="subtitle">Facturas emitidas automáticamente al aprobarse cada pago</p></div></div>' +
    '<div class="flex gap-10 flex-wrap mb-16">' +
    '<input id="cf-buscar" placeholder="Buscar por número, estudiante o curso..." value="' + esc(CF_FILTRO.busqueda) + '" onkeydown="if(event.key===\'Enter\'){CF_FILTRO.busqueda=this.value;cargarFacturasCont()}" style="flex:1;min-width:240px">' +
    '<input type="date" id="cf-desde" value="' + CF_FILTRO.desde + '" onchange="CF_FILTRO.desde=this.value;cargarFacturasCont()" title="Desde" style="width:170px">' +
    '<input type="date" id="cf-hasta" value="' + CF_FILTRO.hasta + '" onchange="CF_FILTRO.hasta=this.value;cargarFacturasCont()" title="Hasta" style="width:170px">' +
    '<button class="btn btn-ghost" onclick="CF_FILTRO.busqueda=document.getElementById(\'cf-buscar\').value;cargarFacturasCont()">Buscar</button></div>' +
    '<div id="cf-lista"><div class="empty-state">Cargando facturas...</div></div>';
  renderModalesPagoCont();
  cargarFacturasCont();
}

async function cargarFacturasCont() {
  const el = document.getElementById('cf-lista');
  try {
    await cargarPermisosCont();
    CF_FACTURAS = await pedirApiCont('GET', '/api/facturas' + qs(CF_FILTRO));
    let html = '<div class="tabla-wrap"><table id="cf-tabla"><thead><tr style="background:var(--blue-soft)">' +
      ['N.º factura', 'Estudiante', 'Emisión', 'Total', 'Medio', 'Correo', 'Acciones'].map(h => '<th style="color:var(--blue-deep)">' + h + '</th>').join('') + '</tr></thead><tbody>';
    CF_FACTURAS.forEach(f => {
      html += '<tr class="cf-fila" data-numero="' + esc(f.numero) + '"><td style="font-family:monospace;font-weight:700">' + esc(f.numero) + '</td>' +
        '<td><div style="font-weight:700">' + esc(f.estudiante) + '</div><div class="text-sm muted">' + esc(f.descripcion || '') + '</div></td>' +
        '<td class="text-sm muted">' + fechaHoraCont(f.fechaEmision) + '</td><td style="font-weight:700">' + fmtMoney(f.montoTotal) + '</td>' +
        '<td class="text-sm">' + esc(f.medioNombre) + '</td>' +
        '<td>' + (f.enviadaPorCorreo ? '<span class="pill pill-verde">✓ Enviada</span><div class="text-sm muted">' + fechaHoraCont(f.enviadaEn) + '</div>' : '<span class="pill pill-gris">No enviada</span>') + '</td>' +
        '<td><div class="flex gap-4"><button class="btn btn-ghost btn-xs" onclick="verFacturaCont(' + f.id + ')">Ver</button>' +
        (CONT_PUEDE_RESOLVER ? '<button class="btn btn-ghost btn-xs cf-reenviar" onclick="reenviarFacturaCont(' + f.id + ')">✉ Reenviar</button>' : '') + '</div></td></tr>';
    });
    if (!CF_FACTURAS.length) html += '<tr><td colspan="7" class="empty-state">No hay facturas con estos filtros.</td></tr>';
    el.innerHTML = avisoAuditorCont() + html + '</tbody></table></div>';
  } catch (err) { if (el) el.innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; }
}

async function reenviarFacturaCont(id) {
  try { const f = await pedirApiCont('POST', '/api/facturas/' + id + '/correo'); toast('Factura ' + f.numero + ' reenviada a ' + f.correo); cargarFacturasCont(); }
  catch (err) { toast(err.message, true); }
}

let CF_ABIERTA = null;
async function verFacturaCont(id) {
  try {
    CF_ABIERTA = await pedirApiCont('GET', '/api/facturas/' + id);
    renderModalesPagoCont();
    document.getElementById('cfv-cuerpo').innerHTML = facturaHTMLCont(CF_ABIERTA);
    abrirModal('modal-cf-ver');
  } catch (err) { toast(err.message, true); }
}

function facturaHTMLCont(f) {
  const filas = f.items.map(i => '<tr><td style="padding:12px;border-bottom:1px solid #E0E2E8">Inscripción — ' + esc(i.curso) + '</td><td style="padding:12px;border-bottom:1px solid #E0E2E8;text-align:right;font-weight:700">' + fmtMoney(i.monto) + '</td></tr>').join('');
  return '<div id="cf-documento" style="background:#fff;border:1px solid #E0E2E8;border-radius:14px;padding:32px;font-family:Inter,Arial,sans-serif;color:#2A2118">' +
    '<div style="display:flex;justify-content:space-between;align-items:flex-start;border-bottom:3px solid #2C4A42;padding-bottom:18px;margin-bottom:22px">' +
    '<div><div style="font-size:22px;font-weight:800;color:#2C4A42">🌿 BioLearn</div><div style="font-size:11px;color:#8A6A4E">' + esc(f.razonSocial || '') + ' · NIT ' + esc(f.nit || '') + '<br>' + esc(f.direccionEmisor || '') + '</div></div>' +
    '<div style="text-align:right"><div style="font-size:11px;letter-spacing:1.5px;color:#8A6A4E;font-weight:700">FACTURA DE VENTA</div><div style="font-family:monospace;font-weight:800;font-size:17px;color:#2C4A42">' + esc(f.numero) + '</div></div></div>' +
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:22px;font-size:13px"><div><div style="color:#8A6A4E;font-weight:700;text-transform:uppercase;font-size:10.5px">Facturar a</div><div style="font-weight:700">' + esc(f.estudiante) + '</div><div>' + esc(f.correo) + (f.documento ? '<br>Doc. ' + esc(f.documento) : '') + '</div></div>' +
    '<div style="text-align:right"><div style="color:#8A6A4E;font-weight:700;text-transform:uppercase;font-size:10.5px">Fecha de emisión</div><div style="font-weight:700">' + fechaHoraCont(f.fechaEmision) + '</div></div></div>' +
    '<table style="width:100%;border-collapse:collapse;margin-bottom:18px;font-size:13.5px"><thead><tr style="background:#E4EEE9"><th style="text-align:left;padding:9px 12px;font-size:10.5px;text-transform:uppercase">Concepto</th><th style="text-align:right;padding:9px 12px;font-size:10.5px;text-transform:uppercase">Valor</th></tr></thead><tbody>' + filas + '</tbody>' +
    '<tfoot><tr><td style="padding:12px;font-weight:800;font-size:15px">Total</td><td style="padding:12px;text-align:right;font-weight:800;font-size:15px">' + fmtMoney(f.montoTotal) + '</td></tr></tfoot></table>' +
    '<div style="background:#F7F3EC;border-radius:10px;padding:14px 16px;font-size:12.5px">Medio de pago: <strong>' + esc(f.medioNombre) + '</strong>' + (f.referencia ? ' · Ref. ' + esc(f.referencia) : '') + ' · Pago #' + f.idPago + '</div>' +
    '<p style="font-size:10.5px;color:#8A6A4E;text-align:center;margin:14px 0 0">Factura generada electrónicamente por BioLearn · Proyecto académico sin validez fiscal.</p></div>';
}

function imprimirFacturaCont() {
  if (!CF_ABIERTA) return;
  const w = window.open('');
  if (!w) { toast('Habilita las ventanas emergentes para imprimir', true); return; }
  w.document.write('<html><head><title>' + esc(CF_ABIERTA.numero) + '</title></head><body style="margin:0;background:#F4F5F7;padding:32px;display:flex;justify-content:center"><div style="max-width:620px">' + facturaHTMLCont(CF_ABIERTA) + '</div></body></html>');
  w.document.close(); w.focus(); w.print();
}

/* ================================================================ MODALES */
function renderModalesPagoCont() {
  const crear = (html) => { const d = document.createElement('div'); d.innerHTML = html; document.body.appendChild(d.firstElementChild); };
  if (!document.getElementById('modal-cp-confirmar')) crear('<div class="modal-overlay" id="modal-cp-confirmar"><div class="modal" style="max-width:440px"><div class="modal-header"><h3>Confirmar transferencia</h3><button class="modal-close" onclick="cerrarModal(\'modal-cp-confirmar\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="cpc-id"><p style="font-size:14px;line-height:1.8" id="cpc-resumen"></p><p class="text-sm muted">Verifica que el dinero haya llegado a la cuenta. Al confirmar, los cursos quedan activos y se emite y envía la factura.</p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-cp-confirmar\')">Cancelar</button><button class="btn btn-primary" id="cpc-btn" onclick="confirmarPagoCont()">✓ Confirmar y emitir factura</button></div></div></div>');
  if (!document.getElementById('modal-cp-rechazo')) crear('<div class="modal-overlay" id="modal-cp-rechazo"><div class="modal" style="max-width:440px"><div class="modal-header"><h3>Rechazar transferencia</h3><button class="modal-close" onclick="cerrarModal(\'modal-cp-rechazo\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="cpr-id"><p style="font-size:14px" id="cpr-resumen"></p><div class="campo"><label>Motivo (el estudiante lo verá)</label><textarea id="cpr-motivo" rows="3" maxlength="500" placeholder="Ej.: el comprobante no corresponde al valor del pago"></textarea></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-cp-rechazo\')">Cancelar</button><button class="btn btn-danger" id="cpr-btn" onclick="rechazarPagoCont()">✕ Rechazar pago</button></div></div></div>');
  if (!document.getElementById('modal-cf-ver')) crear('<div class="modal-overlay" id="modal-cf-ver"><div class="modal" style="max-width:660px"><div class="modal-header"><h3>Factura</h3><button class="modal-close" onclick="cerrarModal(\'modal-cf-ver\')">✕</button></div>' +
    '<div class="modal-body" style="background:var(--bone)"><div id="cfv-cuerpo"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-cf-ver\')">Cerrar</button><button class="btn btn-primary" onclick="imprimirFacturaCont()">⬇ Imprimir o guardar PDF</button></div></div></div>');
}
