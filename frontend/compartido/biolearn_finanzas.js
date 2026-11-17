/* ================================================================
   BIOLearn — compartido/biolearn_finanzas.js
   Pagos, Fase 3. Módulo compartido por los portales del contador y del Administrador:
   - Reportes financieros (ingresos, facturas, reembolsos y general) por período, con motivo:
     vista imprimible (Imprimir o guardar PDF) y descarga en Excel. Historial de reportes generados.
   - Datos de facturación (emisor de facturas y notas crédito) y datos de recaudo (cuenta y Nequi
     donde transfiere el estudiante). Cada cambio guarda una versión nueva; editan el Administrador y
     el administrador financiero, el auxiliar y el auditor solo consultan.
   - Inicio del contador con cifras reales (finRenderInicio).
   Cada portal define FIN.pedir (su función para llamar la API) antes de usarlo.
================================================================ */
const FIN = {
  base: 'http://localhost:8080',
  pedir: null,              // function (metodo, ruta, cuerpo) → Promise<JSON>; la define cada portal
  aviso: null,              // function (texto, esError); por defecto usa toast() del portal
  reporte: null,            // último reporte generado (vista imprimible)
  config: null,
  pestana: 'reportes',      // Administrador: reportes | datos
};
const FIN_TIPOS = [
  { v: 'ingresos', t: '💰 Ingresos del período', d: 'Pagos aprobados, por medio de pago y por curso, con lo reembolsado de cada pago.' },
  { v: 'facturas', t: '🧾 Estado de facturas', d: 'Facturas emitidas con sus notas crédito y el saldo de cada una.' },
  { v: 'reembolsos', t: '↩️ Reembolsos', d: 'Solicitudes del período por estado, con la nota crédito y la devolución.' },
  { v: 'general', t: '📊 Reporte financiero general', d: 'Indicadores, mes a mes, medios de pago, estado de los pagos y cursos con más ingresos.' },
];

function finEsc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function finAviso(t, e) { (FIN.aviso || (typeof toast === 'function' ? toast : alert))(t, e); }
function finMoneda(v) { return '$' + Math.round(Number(v || 0)).toLocaleString('es-CO'); }
function finNumero(v) { return Number(v || 0).toLocaleString('es-CO'); }
function finPct(v) { return (Number(v || 0) * 100).toLocaleString('es-CO', { maximumFractionDigits: 1 }) + ' %'; }
function finFecha(iso, conHora) {
  if (!iso) return '—';
  const d = new Date(String(iso).length === 10 ? iso + 'T00:00:00' : iso);
  if (isNaN(d.getTime())) return finEsc(iso);
  return conHora === false ? d.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })
    : d.toLocaleString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' });
}
function finValor(v, tipo) {
  if (v === null || v === undefined || v === '') return '';
  switch (tipo) {
    case 'moneda': return finMoneda(v);
    case 'numero': return finNumero(v);
    case 'porcentaje': return finPct(v);
    case 'fecha': return finFechaCorta(v);
    default: return finEsc(v);
  }
}
function finHoy() { const d = new Date(); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); }
function finInicioMes() { return finHoy().slice(0, 8) + '01'; }

/* Dirección con la ciudad, sin repetirla si ya está escrita (igual que FinanzasServiceImpl.direccionCompleta) */
function finDireccion(d) {
  const dir = d.direccion || '', ciudad = d.ciudad || '';
  if (!ciudad || dir.toLowerCase().includes(ciudad.toLowerCase())) return dir;
  return dir ? dir + ', ' + ciudad : ciudad;
}

/* Fecha corta para las tablas del reporte: 15/11/2026 18:18 (las columnas no se parten en varias líneas) */
function finFechaCorta(iso) {
  const d = new Date(String(iso).length === 10 ? iso + 'T00:00:00' : iso);
  if (isNaN(d.getTime())) return finEsc(iso);
  const p = n => String(n).padStart(2, '0');
  const f = p(d.getDate()) + '/' + p(d.getMonth() + 1) + '/' + d.getFullYear();
  return String(iso).length === 10 ? f : f + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
}

/* Las tarjetas del Inicio abren la sección con el filtro que corresponde (pagos:pendiente, reembolsos:aprobado…) */
function finIrA(destino) {
  const [seccion, estado] = destino.split(':');
  if (estado && seccion === 'pagos' && typeof CP_FILTRO !== 'undefined') CP_FILTRO.estado = estado;
  if (estado && seccion === 'reembolsos' && typeof CR_FILTRO !== 'undefined') CR_FILTRO.estado = estado;
  goTo(seccion);
}

/* ================================================================ INICIO DEL CONTADOR */
async function finRenderInicio(cont, nombre, cargo) {
  cont.innerHTML = '<div class="empty-state" style="padding:60px">Cargando el panel financiero…</div>';
  let r;
  try { r = await FIN.pedir('GET', '/api/finanzas/resumen'); }
  catch (err) { cont.innerHTML = '<div class="empty-state">' + finEsc(err.message) + '</div>'; return; }
  const mesNombre = new Date().toLocaleDateString('es-CO', { month: 'long', year: 'numeric' });
  const tarjeta = (id, titulo, valor, detalle, color, destino) =>
    '<div class="card fin-tarjeta" id="' + id + '"' + (destino ? ' onclick="finIrA(\'' + destino + '\')" style="cursor:pointer"' : '') + '>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:6px">' + titulo + '</div>' +
    '<div class="fin-valor" style="font-family:\'Fraunces\',serif;font-size:26px;color:' + (color || 'var(--ink)') + '">' + valor + '</div>' +
    '<div class="text-sm muted" style="margin-top:2px">' + detalle + '</div></div>';
  let html = '<div style="background:linear-gradient(120deg,var(--blue-deep),#4A3A2C);border-radius:16px;padding:26px 30px;margin-bottom:24px;color:#fff" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><div style="font-family:\'Fraunces\',serif;font-size:21px">Panel financiero — BioLearn</div>' +
    '<div style="font-size:13px;opacity:.85;text-transform:capitalize">' + finEsc(nombre || '') + (cargo ? ' · ' + finEsc(cargo) : '') + ' · ' + mesNombre + '</div></div>' +
    '<div style="text-align:right"><div style="font-size:12px;opacity:.8">Ingresos netos del mes</div><div id="fin-neto-mes" style="font-family:\'Fraunces\',serif;font-size:30px;color:#8CE0A8">' + finMoneda(r.netoMes) + '</div></div></div>';
  html += '<div class="fin-grid-4 mb-26">' +
    tarjeta('fin-t-ingresos', 'Ingresos del mes', finMoneda(r.ingresosMes), r.pagosMes + ' pago' + (r.pagosMes === 1 ? '' : 's') + ' aprobado' + (r.pagosMes === 1 ? '' : 's') + ' · ' + finMoneda(r.reembolsadoMes) + ' devuelto', 'var(--sage)') +
    tarjeta('fin-t-transferencias', 'Transferencias por confirmar', r.transferenciasPorConfirmar, 'Con comprobante adjunto →', 'var(--amber)', 'pagos:pendiente') +
    tarjeta('fin-t-revisar', 'Reembolsos por revisar', r.reembolsosPorRevisar, 'Solicitudes con avance →', 'var(--clay)', 'reembolsos:solicitado') +
    tarjeta('fin-t-devolver', 'Reembolsos por devolver', r.reembolsosPorDevolver, finMoneda(r.montoPorDevolver) + ' a cuentas o Nequi →', 'var(--blue)', 'reembolsos:aprobado') + '</div>';

  const max = Math.max(1, ...r.ultimosMeses.map(m => Math.max(Number(m.ingresos), Number(m.reembolsos))));
  html += '<div class="flex gap-18 flex-wrap mb-26"><div class="card" style="flex:1.4;min-width:340px"><div class="flex justify-between items-center mb-18">' +
    '<h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0">Ingresos y devoluciones · últimos 6 meses</h3>' +
    '<button class="btn btn-ghost btn-sm" onclick="goTo(\'reportes\')">Reportes →</button></div>' +
    '<div class="fin-barras" id="fin-barras">' + r.ultimosMeses.map(m => {
      const etiqueta = new Date(m.mes + '-01T00:00:00').toLocaleDateString('es-CO', { month: 'short' });
      return '<div class="fin-barra-mes" title="' + finEsc(m.mes) + ': ingresos ' + finMoneda(m.ingresos) + ', devuelto ' + finMoneda(m.reembolsos) + '">' +
        '<div class="fin-barra-par"><div class="fin-barra fin-barra-ing" style="height:' + (Number(m.ingresos) / max * 100) + '%"></div>' +
        '<div class="fin-barra fin-barra-dev" style="height:' + (Number(m.reembolsos) / max * 100) + '%"></div></div>' +
        '<div class="text-sm muted" style="text-transform:capitalize">' + etiqueta + '</div></div>';
    }).join('') + '</div>' +
    '<div class="flex gap-18 text-sm" style="margin-top:10px"><span><span class="fin-punto" style="background:var(--blue-deep)"></span>Ingresos</span>' +
    '<span><span class="fin-punto" style="background:var(--clay)"></span>Dinero devuelto</span></div></div>';
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">Últimos movimientos</h3>';
  if (!r.movimientos.length) html += '<div class="empty-state">Todavía no hay movimientos.</div>';
  r.movimientos.forEach(m => {
    const esPago = m.tipo === 'pago';
    html += '<div class="flex items-center gap-12 fin-mov" data-tipo="' + m.tipo + '" style="padding:10px 0;border-bottom:1px solid var(--line)">' +
      '<div style="font-size:18px">' + (esPago ? '💳' : '↩️') + '</div><div style="flex:1;min-width:0"><div style="font-weight:600;font-size:13.5px">' + finEsc(m.descripcion) + '</div>' +
      '<div class="text-sm muted">' + finEsc(m.estudiante) + ' · ' + finFecha(m.fecha) + '</div></div>' +
      '<div style="font-weight:700;color:' + (esPago ? 'var(--sage)' : 'var(--clay)') + '">' + (esPago ? '+' : '−') + finMoneda(m.monto) + '</div></div>';
  });
  html += '</div></div>';
  cont.innerHTML = html;
}

/* ================================================================ REPORTES */
function finRenderReportes(cont, conEncabezado) {
  const hoy = finHoy();
  cont.innerHTML = (conEncabezado === false ? '' : '<div class="section-header"><div><h2>Reportes financieros</h2>' +
    '<p class="subtitle">Elige el tipo y el período; ábrelo para imprimir o guardar en PDF, o descárgalo en Excel</p></div></div>') +
    '<div class="flex gap-18 flex-wrap"><div class="card" style="flex:1;min-width:340px">' +
    '<h3 style="font-family:\'Fraunces\',serif;font-size:16.5px;margin:0 0 14px">Configurar reporte</h3>' +
    '<div class="campo"><label>Tipo de reporte</label><div id="fin-tipos" class="fin-tipos">' + FIN_TIPOS.map((t, i) =>
      '<label class="fin-tipo"><input type="radio" name="fin-tipo" value="' + t.v + '"' + (i === 0 ? ' checked' : '') + '><span><strong>' + t.t + '</strong><br><span class="text-sm muted">' + t.d + '</span></span></label>').join('') + '</div></div>' +
    '<div class="grid-2"><div class="campo"><label for="fin-desde">Desde</label><input type="date" id="fin-desde" value="' + finInicioMes() + '" max="' + hoy + '"></div>' +
    '<div class="campo"><label for="fin-hasta">Hasta</label><input type="date" id="fin-hasta" value="' + hoy + '"></div></div>' +
    '<div class="flex gap-6 mb-14" style="flex-wrap:wrap">' +
    '<button class="btn btn-ghost btn-xs" onclick="finPeriodo(\'mes\')">Este mes</button><button class="btn btn-ghost btn-xs" onclick="finPeriodo(\'anterior\')">Mes anterior</button>' +
    '<button class="btn btn-ghost btn-xs" onclick="finPeriodo(\'trimestre\')">Últimos 3 meses</button><button class="btn btn-ghost btn-xs" onclick="finPeriodo(\'anio\')">Este año</button></div>' +
    '<div class="campo"><label for="fin-motivo">Motivo</label><input id="fin-motivo" maxlength="300" placeholder="Ej.: cierre contable de noviembre"><p class="hint">Queda registrado con tu nombre en el historial.</p></div>' +
    '<div class="flex gap-10" style="flex-wrap:wrap"><button class="btn btn-primary" id="fin-btn-ver" onclick="finGenerar()">📄 Ver e imprimir (PDF)</button>' +
    '<button class="btn btn-ghost" id="fin-btn-excel" onclick="finExcel()">📊 Descargar Excel</button></div></div>' +
    '<div class="card" style="flex:1;min-width:300px"><h3 style="font-family:\'Fraunces\',serif;font-size:16.5px;margin:0 0 6px">Reportes generados</h3>' +
    '<p class="text-sm muted" style="margin:0 0 10px">Los últimos 50, con su motivo y quién los generó.</p><div id="fin-historial"><div class="empty-state">Cargando…</div></div></div></div>';
  finCargarHistorial();
}

function finPeriodo(cual) {
  const d = new Date(), y = d.getFullYear(), m = d.getMonth();
  const f = x => x.getFullYear() + '-' + String(x.getMonth() + 1).padStart(2, '0') + '-' + String(x.getDate()).padStart(2, '0');
  let desde, hasta = new Date();
  if (cual === 'mes') desde = new Date(y, m, 1);
  else if (cual === 'anterior') { desde = new Date(y, m - 1, 1); hasta = new Date(y, m, 0); }
  else if (cual === 'trimestre') desde = new Date(y, m - 2, 1);
  else desde = new Date(y, 0, 1);
  document.getElementById('fin-desde').value = f(desde);
  document.getElementById('fin-hasta').value = f(hasta);
}

function finParametros() {
  const tipo = (document.querySelector('input[name="fin-tipo"]:checked') || {}).value;
  const desde = document.getElementById('fin-desde').value, hasta = document.getElementById('fin-hasta').value;
  const motivo = document.getElementById('fin-motivo').value.trim();
  if (!tipo) { finAviso('Elige el tipo de reporte', true); return null; }
  if (!desde || !hasta) { finAviso('Elige las dos fechas del período', true); return null; }
  if (desde > hasta) { finAviso('La fecha inicial no puede ser posterior a la final', true); return null; }
  if (motivo.length < 5) { finAviso('Escribe el motivo del reporte (mínimo 5 caracteres)', true); document.getElementById('fin-motivo').focus(); return null; }
  return { tipo, desde, hasta, motivo };
}

async function finCargarHistorial() {
  const c = document.getElementById('fin-historial'); if (!c) return;
  try {
    const lista = await FIN.pedir('GET', '/api/finanzas/reportes/historial');
    if (!lista.length) { c.innerHTML = '<div class="empty-state">Aún no se han generado reportes.</div>'; return; }
    c.innerHTML = lista.map(h => '<div class="fin-hist" style="padding:10px 0;border-top:1px solid var(--line)">' +
      '<div class="flex justify-between gap-10"><strong style="font-size:13.5px">' + finEsc(h.tipoNombre) + '</strong><span class="pill ' + (h.formato === 'excel' ? 'pill-verde' : 'pill-azul') + '">' + (h.formato === 'excel' ? 'Excel' : 'PDF') + '</span></div>' +
      '<div class="text-sm muted">' + finFecha(h.desde, false) + ' a ' + finFecha(h.hasta, false) + ' · ' + finEsc(h.generadoPor) + ' · ' + finFecha(h.generadoEn) + '</div>' +
      '<div class="text-sm">«' + finEsc(h.motivo) + '»</div></div>').join('');
  } catch (err) { c.innerHTML = '<div class="empty-state">' + finEsc(err.message) + '</div>'; }
}

async function finGenerar() {
  const p = finParametros(); if (!p) return;
  const btn = document.getElementById('fin-btn-ver'); btn.disabled = true;
  try {
    FIN.reporte = await FIN.pedir('POST', '/api/finanzas/reportes', p);
    finAbrirVista();
    finCargarHistorial();
  } catch (err) { finAviso(err.message, true); }
  finally { btn.disabled = false; }
}

async function finExcel() {
  const p = finParametros(); if (!p) return;
  const btn = document.getElementById('fin-btn-excel'); btn.disabled = true;
  try {
    const r = await fetch(FIN.base + '/api/finanzas/reportes/excel', {
      method: 'POST', body: JSON.stringify(p),
      headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + sessionStorage.getItem('biolearn_token') } });
    if (!r.ok) {
      const data = await r.json().catch(() => ({}));
      throw new Error((data.mensaje || 'Error ' + r.status) + (data.detalles && data.detalles.length ? ' (' + data.detalles.join('; ') + ')' : ''));
    }
    const blob = await r.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'BioLearn_' + p.tipo + '_' + p.desde + '_' + p.hasta + '.xlsx';
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(a.href), 4000);
    finAviso('Reporte en Excel descargado');
    finCargarHistorial();
  } catch (err) { finAviso(err.message, true); }
  finally { btn.disabled = false; }
}

function finReporteHTML(r) {
  const fila = (vals, cols, total) => '<tr' + (total ? ' style="background:#E4EEE9;font-weight:700"' : '') + '>' + vals.map((v, i) => {
    const t = cols[i].tipo, num = t === 'moneda' || t === 'numero' || t === 'porcentaje';
    const texto = total && typeof v === 'string' && t !== 'texto' && !/^\d{4}-\d{2}-\d{2}/.test(v) && isNaN(Number(v));
    return '<td style="padding:6px 8px;border-bottom:1px solid #E0E2E8;' + (num ? 'text-align:right;white-space:nowrap' : t === 'fecha' ? 'white-space:nowrap' : '') + '">' + (texto ? finEsc(v) : finValor(v, t)) + '</td>';
  }).join('') + '</tr>';
  let h = '<div id="fin-documento" style="background:#fff;border:1px solid #E0E2E8;border-radius:14px;padding:28px;font-family:Inter,Arial,sans-serif;color:#2A2118;font-size:12.5px">' +
    '<div style="display:flex;justify-content:space-between;align-items:flex-start;border-bottom:3px solid #2C4A42;padding-bottom:14px;margin-bottom:18px;gap:16px">' +
    '<div><div style="font-size:20px;font-weight:800;color:#2C4A42">🌿 BioLearn</div><div style="font-size:11px;color:#8A6A4E">' + finEsc(r.razonSocial || '') + (r.nit ? ' · NIT ' + finEsc(r.nit) : '') + '</div></div>' +
    '<div style="text-align:right"><div style="font-size:11px;letter-spacing:1.5px;color:#8A6A4E;font-weight:700;text-transform:uppercase">Reporte financiero</div>' +
    '<div id="fin-titulo" style="font-weight:800;font-size:17px;color:#2C4A42">' + finEsc(r.titulo) + '</div>' +
    '<div style="font-size:11.5px">' + finFecha(r.desde, false) + ' a ' + finFecha(r.hasta, false) + '</div></div></div>' +
    '<div class="fin-indicadores" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:10px;margin-bottom:18px">' +
    r.indicadores.map(i => '<div style="background:#F7F3EC;border-radius:10px;padding:10px 12px"><div style="font-size:10.5px;color:#8A6A4E;font-weight:700;text-transform:uppercase">' + finEsc(i.etiqueta) + '</div>' +
      '<div style="font-size:17px;font-weight:800;color:#2C4A42">' + finValor(i.valor, i.formato) + '</div></div>').join('') + '</div>';
  r.tablas.forEach(t => {
    h += '<h4 style="font-size:13.5px;color:#2C4A42;margin:16px 0 6px">' + finEsc(t.titulo) + '</h4>' +
      '<table class="fin-tabla" style="width:100%;border-collapse:collapse;font-size:11.5px"><thead><tr style="background:#2F5D50;color:#fff">' +
      t.columnas.map(c => '<th style="text-align:' + (['moneda', 'numero', 'porcentaje'].includes(c.tipo) ? 'right' : 'left') + ';padding:6px 8px;font-size:10.5px;text-transform:uppercase">' + finEsc(c.nombre) + '</th>').join('') + '</tr></thead><tbody>' +
      (t.filas.length ? t.filas.map(f => fila(f, t.columnas)).join('') : '<tr><td colspan="' + t.columnas.length + '" style="padding:10px;color:#8A6A4E;text-align:center">Sin movimientos en el período.</td></tr>') +
      (t.total ? fila(t.total, t.columnas, true) : '') + '</tbody></table>' +
      (t.nota ? '<p style="font-size:10.5px;color:#8A6A4E;margin:4px 0 0">' + finEsc(t.nota) + '</p>' : '');
  });
  h += '<p style="font-size:10.5px;color:#8A6A4E;margin:18px 0 0;border-top:1px solid #E0E2E8;padding-top:10px">Generado por ' + finEsc(r.generadoPor) + ' el ' + finFecha(r.generadoEn) +
    ' · Motivo: ' + finEsc(r.motivo) + ' · Registro #' + finEsc(r.idRegistro) + '. Las cuentas bancarias y los Nequi se muestran enmascarados.</p></div>';
  return h;
}

function finAbrirVista() {
  if (!document.getElementById('modal-fin-reporte')) {
    const d = document.createElement('div');
    d.innerHTML = '<div class="modal-overlay" id="modal-fin-reporte"><div class="modal" style="max-width:980px;width:96vw">' +
      '<div class="modal-header"><h3>Reporte financiero</h3><button class="modal-close" onclick="cerrarModal(\'modal-fin-reporte\')">✕</button></div>' +
      '<div class="modal-body" style="background:var(--bone);max-height:70vh;overflow:auto"><div id="fin-vista"></div></div>' +
      '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-fin-reporte\')">Cerrar</button>' +
      '<button class="btn btn-primary" id="fin-btn-imprimir" onclick="finImprimir()">⬇ Imprimir o guardar PDF</button></div></div></div>';
    document.body.appendChild(d.firstElementChild);
  }
  document.getElementById('fin-vista').innerHTML = finReporteHTML(FIN.reporte);
  abrirModal('modal-fin-reporte');
}

function finImprimir() {
  const r = FIN.reporte; if (!r) return;
  const w = window.open('');
  if (!w) { finAviso('Habilita las ventanas emergentes para imprimir', true); return; }
  w.document.write('<html><head><title>' + finEsc(r.titulo + ' ' + r.desde + ' a ' + r.hasta) + '</title>' +
    '<style>@page{size:landscape;margin:0}body{margin:0;padding:12mm;background:#fff}tr{page-break-inside:avoid}</style></head><body>' + finReporteHTML(r) + '</body></html>');
  w.document.close(); w.focus(); w.print();
}

/* ================================================================ DATOS DE FACTURACIÓN Y RECAUDO */
async function finRenderDatos(cont, conEncabezado) {
  cont.innerHTML = (conEncabezado === false ? '' : '<div class="section-header"><div><h2>Facturación y recaudo</h2>' +
    '<p class="subtitle">Datos del emisor de las facturas y la cuenta donde transfieren los estudiantes</p></div></div>') +
    '<div id="fin-datos"><div class="empty-state">Cargando…</div></div>';
  try { FIN.config = await FIN.pedir('GET', '/api/finanzas/configuracion'); }
  catch (err) { document.getElementById('fin-datos').innerHTML = '<div class="empty-state">' + finEsc(err.message) + '</div>'; return; }
  finPintarDatos();
}

function finPintarDatos() {
  const c = FIN.config, f = c.facturacion || {}, r = c.recaudo || {}, ed = c.puedeEditar;
  const dis = ed ? '' : ' disabled';
  const campo = (id, label, valor, extra) => '<div class="campo"><label for="' + id + '">' + label + '</label><input id="' + id + '" value="' + finEsc(valor || '') + '"' + dis + (extra || '') + '></div>';
  const pie = (d) => d && d.actualizadoEn ? '<p class="text-sm muted" style="margin:0 0 12px">Vigente desde ' + finFecha(d.actualizadoEn) + (d.actualizadoPor ? ' · ' + finEsc(d.actualizadoPor) : '') + '</p>' : '';
  let html = ed ? '' : '<div class="card mb-14" id="fin-solo-consulta" style="background:var(--amber-soft);box-shadow:none">🔒 Solo consulta: los cambia el Administrador o el administrador financiero.</div>';
  html += '<div class="flex gap-18 flex-wrap">' +
    '<div class="card" style="flex:1;min-width:320px"><h3 style="font-family:\'Fraunces\',serif;font-size:16.5px;margin:0 0 4px">🧾 Datos de facturación</h3>' +
    '<p class="text-sm muted" style="margin:0 0 4px">Aparecen en las facturas y notas crédito nuevas. Las ya emitidas conservan los suyos.</p>' + pie(f) +
    campo('fin-f-razon', 'Razón social', f.razonSocial, ' maxlength="200"') +
    '<div class="grid-2">' + campo('fin-f-nit', 'NIT', f.nit, ' maxlength="15" placeholder="901000000-0"') + campo('fin-f-telefono', 'Teléfono', f.telefono, ' maxlength="20"') + '</div>' +
    campo('fin-f-direccion', 'Dirección', f.direccion, ' maxlength="255"') +
    '<div class="grid-2">' + campo('fin-f-ciudad', 'Ciudad', f.ciudad, ' maxlength="100"') + campo('fin-f-correo', 'Correo de facturación', f.correo, ' maxlength="150" type="email"') + '</div>' +
    (ed ? '<div class="flex gap-10"><button class="btn btn-primary" id="fin-f-guardar" onclick="finGuardarFacturacion()">Guardar datos de facturación</button>' : '<div class="flex gap-10">') +
    '<button class="btn btn-ghost" onclick="finHistorial(\'facturacion\')">Ver versiones anteriores</button></div></div>' +
    '<div class="card" style="flex:1;min-width:320px"><h3 style="font-family:\'Fraunces\',serif;font-size:16.5px;margin:0 0 4px">🏦 Cuenta para transferencias</h3>' +
    '<p class="text-sm muted" style="margin:0 0 4px">El estudiante ve estos datos cuando paga por transferencia.</p>' + pie(r) +
    '<div class="grid-2">' + campo('fin-r-banco', 'Banco', r.banco, ' maxlength="80"') +
    '<div class="campo"><label for="fin-r-tipo">Tipo de cuenta</label><select id="fin-r-tipo"' + dis + '><option value="ahorros"' + (r.tipoCuenta === 'ahorros' ? ' selected' : '') + '>Ahorros</option>' +
    '<option value="corriente"' + (r.tipoCuenta === 'corriente' ? ' selected' : '') + '>Corriente</option></select></div></div>' +
    '<div class="grid-2">' + campo('fin-r-numero', 'Número de cuenta', r.numeroCuenta, ' maxlength="20" inputmode="numeric"') + campo('fin-r-nequi', 'Nequi (opcional)', r.telefonoNequi, ' maxlength="10" inputmode="numeric" placeholder="3001234567"') + '</div>' +
    '<div class="grid-2">' + campo('fin-r-titular', 'Titular', r.titular, ' maxlength="150"') + campo('fin-r-documento', 'NIT o documento del titular', r.documentoTitular, ' maxlength="15"') + '</div>' +
    '<div class="campo"><label for="fin-r-instrucciones">Instrucciones para el estudiante</label><textarea id="fin-r-instrucciones" rows="2" maxlength="500"' + dis + '>' + finEsc(r.instrucciones || '') + '</textarea></div>' +
    (ed ? '<div class="flex gap-10"><button class="btn btn-primary" id="fin-r-guardar" onclick="finGuardarRecaudo()">Guardar cuenta</button>' : '<div class="flex gap-10">') +
    '<button class="btn btn-ghost" onclick="finHistorial(\'recaudo\')">Ver versiones anteriores</button></div></div></div>';
  document.getElementById('fin-datos').innerHTML = html;
}

function finV(id) { const e = document.getElementById(id); return e ? e.value.trim() : ''; }

async function finGuardarFacturacion() {
  const cuerpo = { razonSocial: finV('fin-f-razon'), nit: finV('fin-f-nit'), direccion: finV('fin-f-direccion'), ciudad: finV('fin-f-ciudad'),
    correo: finV('fin-f-correo'), telefono: finV('fin-f-telefono') };
  confirmarAccion('¿Guardar los nuevos datos de facturación? Se usarán en las facturas y notas crédito que se emitan desde ahora; las anteriores conservan los suyos.', async function () {
    try { FIN.config.facturacion = await FIN.pedir('PUT', '/api/finanzas/datos-facturacion', cuerpo); finPintarDatos(); finAviso('Datos de facturación guardados'); }
    catch (err) { finAviso(err.message, true); }
  }, 'Guardar');
}

async function finGuardarRecaudo() {
  const cuerpo = { banco: finV('fin-r-banco'), tipoCuenta: finV('fin-r-tipo'), numeroCuenta: finV('fin-r-numero').replace(/[\s-]/g, ''),
    titular: finV('fin-r-titular'), documentoTitular: finV('fin-r-documento'), telefonoNequi: finV('fin-r-nequi').replace(/\s/g, ''),
    instrucciones: finV('fin-r-instrucciones') };
  confirmarAccion('¿Guardar la nueva cuenta para transferencias? Los estudiantes la verán desde ahora.', async function () {
    try { FIN.config.recaudo = await FIN.pedir('PUT', '/api/finanzas/datos-recaudo', cuerpo); finPintarDatos(); finAviso('Cuenta para transferencias guardada'); }
    catch (err) { finAviso(err.message, true); }
  }, 'Guardar');
}

async function finHistorial(cual) {
  try {
    const lista = await FIN.pedir('GET', '/api/finanzas/' + (cual === 'facturacion' ? 'datos-facturacion' : 'datos-recaudo') + '/historial');
    if (!document.getElementById('modal-fin-historial')) {
      const d = document.createElement('div');
      d.innerHTML = '<div class="modal-overlay" id="modal-fin-historial"><div class="modal" style="max-width:760px;width:94vw">' +
        '<div class="modal-header"><h3 id="fin-h-titulo">Versiones</h3><button class="modal-close" onclick="cerrarModal(\'modal-fin-historial\')">✕</button></div>' +
        '<div class="modal-body" style="max-height:65vh;overflow:auto"><div id="fin-h-cuerpo"></div></div>' +
        '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-fin-historial\')">Cerrar</button></div></div></div>';
      document.body.appendChild(d.firstElementChild);
    }
    document.getElementById('fin-h-titulo').textContent = cual === 'facturacion' ? 'Versiones de los datos de facturación' : 'Versiones de la cuenta para transferencias';
    const cab = cual === 'facturacion' ? ['Vigente desde', 'Razón social', 'NIT', 'Dirección', 'Guardó'] : ['Vigente desde', 'Banco', 'Cuenta', 'Titular', 'Nequi', 'Guardó'];
    const filas = lista.map(v => '<tr' + (v.vigente ? ' style="background:var(--sage-soft,#E4EEE9)"' : '') + '><td>' + finFecha(v.actualizadoEn) + (v.vigente ? ' <span class="pill pill-verde">Vigente</span>' : '') + '</td>' +
      (cual === 'facturacion'
        ? '<td>' + finEsc(v.razonSocial) + '</td><td>' + finEsc(v.nit) + '</td><td>' + finEsc(finDireccion(v)) + '</td>'
        : '<td>' + finEsc(v.banco) + '</td><td>' + finEsc(v.tipoCuenta + ' ' + v.numeroCuenta) + '</td><td>' + finEsc(v.titular) + '</td><td>' + finEsc(v.telefonoNequi || '—') + '</td>') +
      '<td>' + finEsc(v.actualizadoPor || 'Registro inicial') + '</td></tr>').join('');
    document.getElementById('fin-h-cuerpo').innerHTML = '<div class="tabla-wrap"><table><thead><tr>' + cab.map(c => '<th>' + c + '</th>').join('') + '</tr></thead><tbody>' + filas + '</tbody></table></div>';
    abrirModal('modal-fin-historial');
  } catch (err) { finAviso(err.message, true); }
}

/* ================================================================ ADMINISTRADOR: pestañas */
function finRenderAdmin(cont) {
  cont.innerHTML = '<div class="section-header"><div><h2>Finanzas</h2><p class="subtitle">Reportes financieros y datos de facturación y recaudo</p></div></div>' +
    '<div class="flex gap-6 mb-18"><button class="fin-pestana' + (FIN.pestana === 'reportes' ? ' activo' : '') + '" id="fin-p-reportes" onclick="FIN.pestana=\'reportes\';finRenderAdmin(document.getElementById(\'app-content\'))">📊 Reportes financieros</button>' +
    '<button class="fin-pestana' + (FIN.pestana === 'datos' ? ' activo' : '') + '" id="fin-p-datos" onclick="FIN.pestana=\'datos\';finRenderAdmin(document.getElementById(\'app-content\'))">🏦 Facturación y recaudo</button></div>' +
    '<div id="fin-admin-cont"></div>';
  const c = document.getElementById('fin-admin-cont');
  if (FIN.pestana === 'datos') finRenderDatos(c, false); else finRenderReportes(c, false);
}

/* ================================================================ estilos del módulo */
(function () {
  const css = '.fin-grid-4{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}@media(max-width:1100px){.fin-grid-4{grid-template-columns:repeat(2,1fr)}}' +
    '.fin-barras{display:flex;align-items:flex-end;gap:12px;height:170px}.fin-barra-mes{flex:1;display:flex;flex-direction:column;align-items:center;gap:6px;height:100%}' +
    '.fin-barra-par{flex:1;width:100%;display:flex;align-items:flex-end;justify-content:center;gap:4px}.fin-barra{width:42%;border-radius:5px 5px 0 0;min-height:2px}' +
    '.fin-barra-ing{background:var(--blue-deep)}.fin-barra-dev{background:var(--clay)}.fin-punto{display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:6px;vertical-align:-1px}' +
    '.fin-tipos{display:grid;gap:8px}.fin-tipo{display:flex;gap:10px;align-items:flex-start;padding:10px 12px;border:1px solid var(--line);border-radius:10px;cursor:pointer;font-weight:400!important;text-transform:none!important;letter-spacing:0!important;color:var(--ink)!important;font-size:13.5px!important}' +
    '.fin-tipo input{width:auto;margin-top:3px}.fin-tipo:has(input:checked){border-color:var(--blue);background:var(--blue-soft)}' +
    '.fin-pestana{padding:8px 16px;border-radius:20px;border:1px solid var(--line);background:var(--panel);font-weight:700;font-size:13.5px;cursor:pointer;font-family:inherit;color:var(--ink-soft)}' +
    '.fin-pestana.activo{border-color:var(--blue);background:var(--blue-soft);color:var(--blue-deep)}';
  const s = document.createElement('style'); s.textContent = css; document.head.appendChild(s);
})();
