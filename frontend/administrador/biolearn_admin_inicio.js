/* ================================================================
   BIOLearn — biolearn_admin_inicio.js
   INICIO del Administrador: conectado al backend real (DEF-037).
   Antes mostraba cifras de ejemplo fijas (8 usuarios, $14.8M, abril–septiembre).
   Ahora consulta:
     GET /api/usuarios          → total, registrados por mes y distribución por rol
     GET /api/cursos            → cursos activos y áreas
     GET /api/instituciones     → instituciones y estudiantes vinculados
     GET /api/finanzas/resumen  → ingresos del mes, últimos 6 meses y últimos movimientos
================================================================ */
const MESES_CORTOS = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];

function inicioMoneda(v) {
  const n = Number(v || 0);
  return '$' + n.toLocaleString('es-CO', { maximumFractionDigits: 0 });
}

function inicioUltimosMeses(n) {
  const hoy = new Date(); const lista = [];
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
    lista.push({ clave: d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0'), mes: MESES_CORTOS[d.getMonth()] });
  }
  return lista;
}

async function inicioPedir(ruta) {
  try {
    const r = await fetch(API_BASE + ruta, { headers: authHeaders() });
    if (!r.ok) return null;
    return await r.json();
  } catch (e) { return null; }
}

function renderInicio(cont) {
  const horaActual = new Date().toLocaleTimeString('es-CO', { hour:'numeric', minute:'2-digit' });
  cont.innerHTML =
    '<div id="ini-banner"></div>' +
    '<div id="ini-cuerpo"><div class="card muted" style="padding:30px;text-align:center">Cargando el resumen del sistema…</div></div>';
  pintarBannerInicio(horaActual, null);
  cargarInicio(horaActual);
}

function pintarBannerInicio(horaActual, estado) {
  const el = document.getElementById('ini-banner'); if (!el) return;
  const items = estado
    ? [['Servidor', estado.servidor], ['Base de datos', estado.base]]
    : [['Conectando…', null]];
  let html = '<div style="background:linear-gradient(120deg,#1C332C,#16241F);border-radius:16px;padding:24px 30px;margin-bottom:24px;color:#fff" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><div style="font-family:\'Fraunces\',serif;font-size:21px">Panel de administración — BioLearn</div>' +
    '<div style="font-size:13px;opacity:.8">Último acceso: hoy ' + horaActual + ' · v1.0.0</div></div>' +
    '<div class="flex gap-18" style="font-size:12.5px">';
  items.forEach(([label, ok]) => {
    const color = ok === null ? '#C9C3B5' : (ok ? '#5FC97A' : 'var(--amber)');
    html += '<div class="flex items-center gap-6" style="opacity:.9"><span style="width:8px;height:8px;border-radius:50%;background:' + color + ';display:inline-block"></span>' + label + '</div>';
  });
  el.innerHTML = html + '</div></div>';
}

async function cargarInicio(horaActual) {
  const [usuarios, cursos, instituciones, finanzas] = await Promise.all([
    inicioPedir('/api/usuarios'), inicioPedir('/api/cursos'),
    inicioPedir('/api/instituciones'), inicioPedir('/api/finanzas/resumen')
  ]);
  const cuerpo = document.getElementById('ini-cuerpo'); if (!cuerpo) return;
  const algunDato = usuarios || cursos || instituciones || finanzas;
  pintarBannerInicio(horaActual, { servidor: !!algunDato, base: !!algunDato });
  if (!algunDato) {
    cuerpo.innerHTML = '<div class="card" style="padding:30px;text-align:center">No se pudo conectar con el servidor (' + esc(API_BASE) + '). Intenta de nuevo en unos segundos.</div>';
    return;
  }

  const listaU = usuarios || [], listaC = cursos || [], listaI = instituciones || [];
  const meses = inicioUltimosMeses(6);
  const mesActual = meses[meses.length - 1].clave;
  const registradosMes = listaU.filter(u => String(u.fechaRegistro || '').startsWith(mesActual)).length;
  const cursosActivos = listaC.filter(c => c.estado === 'activo').length;
  const areas = new Set(listaC.filter(c => c.estado === 'activo').map(c => c.area)).size;
  const vinculados = listaI.reduce((s, i) => s + (i.cuposUsados || 0), 0);

  const tarjeta = (icono, titulo, valor, detalle) =>
    '<div class="card"><div style="font-size:24px;margin-bottom:10px">' + icono + '</div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:6px">' + titulo + '</div>' +
    '<div style="font-family:\'Fraunces\',serif;font-size:30px">' + valor + '</div>' +
    '<div class="text-sm muted" style="margin-top:2px">' + detalle + '</div></div>';

  let html = '<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:26px">' +
    tarjeta('👥', 'Usuarios totales', usuarios ? listaU.length : '—', usuarios ? registradosMes + ' registrados este mes' : 'Sin datos') +
    tarjeta('📚', 'Cursos activos', cursos ? cursosActivos : '—', cursos ? areas + (areas === 1 ? ' área' : ' áreas') + ' · ' + listaC.length + ' en total' : 'Sin datos') +
    tarjeta('💰', 'Ingresos del mes', finanzas ? inicioMoneda(finanzas.ingresosMes) : '—', finanzas ? finanzas.pagosMes + ' pagos · neto ' + inicioMoneda(finanzas.netoMes) : 'Sin datos') +
    tarjeta('🏫', 'Instituciones', instituciones ? listaI.length : '—', instituciones ? vinculados + ' estudiantes vinculados' : 'Sin datos') +
  '</div>';

  /* Usuarios registrados por mes (últimos 6 meses) */
  const porMes = meses.map(m => ({ mes: m.mes, total: listaU.filter(u => String(u.fechaRegistro || '').startsWith(m.clave)).length }));
  const maxMes = Math.max(...porMes.map(m => m.total), 1);
  html += '<div class="flex gap-18 flex-wrap mb-20">';
  html += '<div class="card" style="flex:1.6;min-width:360px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 16px">📈 Usuarios registrados por mes</h3>' +
    '<div class="flex items-end gap-12" style="height:170px">';
  porMes.forEach(t => {
    html += '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;height:100%">' +
      '<span class="text-sm" style="font-weight:700;margin-bottom:6px">' + t.total + '</span>' +
      '<div style="width:100%;border-radius:6px 6px 0 0;background:linear-gradient(180deg,var(--blue),#1C332C);height:' + ((t.total / maxMes) * 100) + '%;min-height:6px"></div></div>';
  });
  html += '</div><div class="flex justify-between text-sm muted" style="margin-top:8px">' + porMes.map(t => '<span style="flex:1;text-align:center">' + t.mes + '</span>').join('') + '</div></div>';

  /* Últimos movimientos financieros reales */
  html += '<div class="card" style="flex:1;min-width:300px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 14px">🕐 Últimos movimientos</h3>';
  const movs = (finanzas && finanzas.movimientos ? finanzas.movimientos : []).slice(0, 5);
  if (!movs.length) html += '<p class="text-sm muted">Todavía no hay pagos ni reembolsos registrados.</p>';
  movs.forEach((m, i) => {
    const fecha = m.fecha ? new Date(m.fecha).toLocaleString('es-CO', { day:'numeric', month:'short', hour:'numeric', minute:'2-digit' }) : '';
    const signo = m.tipo === 'reembolso' ? '−' : '+';
    html += '<div style="padding:10px 0;' + (i > 0 ? 'border-top:1px solid var(--line)' : '') + ';font-size:13.5px" class="flex justify-between gap-12">' +
      '<div style="min-width:0"><span>' + esc(m.descripcion || (m.tipo + ' #' + m.id)) + '</span><div class="text-sm muted" style="margin-top:2px">' + esc(m.estudiante || '') + (fecha ? ' · ' + esc(fecha) : '') + '</div></div>' +
      '<strong style="white-space:nowrap;color:' + (m.tipo === 'reembolso' ? 'var(--clay)' : 'var(--sage)') + '">' + signo + inicioMoneda(m.monto) + '</strong></div>';
  });
  html += '</div></div>';

  /* Ingresos y reembolsos de los últimos 6 meses (mismos datos del panel del contador) */
  html += '<div class="flex gap-18 flex-wrap">';
  html += '<div class="card" style="flex:2;min-width:340px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 4px">Ingresos y reembolsos</h3><p class="text-sm muted" style="margin:0 0 14px">Pesos colombianos, últimos 6 meses</p>';
  const serie = meses.map(m => {
    const f = (finanzas && finanzas.ultimosMeses || []).find(x => x.mes === m.clave) || {};
    return { mes: m.mes, ingresos: Number(f.ingresos || 0), reembolsos: Number(f.reembolsos || 0) };
  });
  const maxMonto = Math.max(...serie.map(m => Math.max(m.ingresos, m.reembolsos)), 1);
  html += '<div class="flex items-end gap-14" style="height:180px">';
  serie.forEach(m => {
    html += '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;height:100%">' +
      '<div class="flex items-end gap-4" style="height:100%;width:100%;justify-content:center">' +
      '<div style="width:38%;border-radius:5px 5px 0 0;background:var(--blue);height:' + ((m.ingresos / maxMonto) * 100) + '%;min-height:2px" title="Ingresos: ' + inicioMoneda(m.ingresos) + '"></div>' +
      '<div style="width:38%;border-radius:5px 5px 0 0;background:var(--amber);height:' + ((m.reembolsos / maxMonto) * 100) + '%;min-height:2px" title="Reembolsos: ' + inicioMoneda(m.reembolsos) + '"></div></div>' +
      '<span class="text-sm muted" style="margin-top:6px">' + m.mes + '</span></div>';
  });
  html += '</div><div class="flex gap-14 text-sm muted" style="margin-top:10px"><span><span style="display:inline-block;width:9px;height:9px;background:var(--blue);border-radius:2px;margin-right:5px"></span>Ingresos</span><span><span style="display:inline-block;width:9px;height:9px;background:var(--amber);border-radius:2px;margin-right:5px"></span>Reembolsos</span></div></div>';

  /* Usuarios por rol */
  const conteo = {};
  listaU.forEach(u => conteo[u.rol] = (conteo[u.rol] || 0) + 1);
  html += '<div class="card" style="flex:1;min-width:260px"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0 0 4px">Usuarios por rol</h3><p class="text-sm muted" style="margin:0 0 14px">Distribución actual</p>';
  Object.entries(conteo).sort((a, b) => b[1] - a[1]).forEach(([rol, n]) => {
    const pct = Math.round((n / Math.max(listaU.length, 1)) * 100);
    html += '<div style="margin-bottom:12px"><div class="flex justify-between mb-4">' + rolePill(rol) + '<span style="font-weight:700">' + n + '</span></div>' +
      '<div class="progress-bar"><div class="progress-fill" style="width:' + pct + '%;background:' + (ROLE_COLOR[rol] || 'var(--ink-soft)') + '"></div></div></div>';
  });
  html += '</div></div>';

  cuerpo.innerHTML = html;
}
