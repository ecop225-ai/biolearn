/* ================================================================
   BIOLearn — biolearn_contador_inicio.js
================================================================ */
function renderInicio(cont) {
  const meses = DATA.ingresosMensuales.length ? DATA.ingresosMensuales : [{mes:'—',valor:0}];
  const mesActual = meses[meses.length-1], mesAnterior = meses[meses.length-2];
  const variacionPct = mesAnterior ? Math.round(((mesActual.valor-mesAnterior.valor)/mesAnterior.valor)*100) : 0;
  const maxIngreso = Math.max(...meses.map(m=>m.valor), 1);
  const mejorMes = meses.reduce((a,b)=>b.valor>a.valor?b:a, meses[0]);
  const promedioMes = Math.round(meses.reduce((s,m)=>s+m.valor,0)/meses.length);
  const totalAnio = meses.reduce((s,m)=>s+m.valor,0);

  const pagosAprobados = DATA.pagos.filter(p=>p.estado==='aprobado'||p.estado==='reembolsado');
  const pendientes = DATA.pagos.filter(p=>p.estado==='pendiente');
  const montoPendiente = pendientes.reduce((s,p)=>s+p.monto,0);
  const facturasEnviadas = DATA.facturas.filter(f=>f.correoEnviado).length;
  const facturasPendientes = DATA.facturas.length - facturasEnviadas;
  const reembolsosSolicitados = DATA.reembolsos.filter(r=>r.estado==='solicitado');
  const montoReembolsosRevision = reembolsosSolicitados.reduce((s,r)=>s+r.monto,0);
  const mesNombre = new Date().toLocaleDateString('es-CO', { month:'long', year:'numeric' });

  let html = '<div style="background:linear-gradient(120deg,var(--blue-deep),#4A3A2C);border-radius:16px;padding:26px 30px;margin-bottom:24px;color:#fff" class="flex justify-between items-center flex-wrap gap-14">' +
    '<div><div style="font-family:\'Fraunces\',serif;font-size:21px">Panel financiero — BioLearn</div>' +
    '<div style="font-size:13px;opacity:.85;text-transform:capitalize">' + esc(CONTADOR.nombre) + ' · ' + esc(CONTADOR.cargo) + ' · ' + mesNombre + '</div></div>' +
    '<div style="text-align:right"><div style="font-size:12px;opacity:.8">Ingresos del mes</div><div style="font-family:\'Fraunces\',serif;font-size:30px;color:#8CE0A8">' + fmtMoney(mesActual.valor) + '</div></div></div>';

  html += '<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:26px">' +
    '<div class="card"><div style="width:34px;height:34px;border-radius:9px;background:var(--sage-soft);display:flex;align-items:center;justify-content:center;margin-bottom:10px;color:var(--sage)">' + iconSvg('check') + '</div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">Pagos procesados</div><div style="font-family:\'Fraunces\',serif;font-size:26px">' + pagosAprobados.length + '</div>' +
    '<div class="text-sm" style="color:' + (variacionPct>=0?'var(--sage)':'var(--clay)') + ';font-weight:700;margin-top:2px">' + (variacionPct>=0?'↑':'↓') + ' ' + Math.abs(variacionPct) + '% vs ' + (mesAnterior?mesAnterior.mes:'') + '</div></div>' +
    '<div class="card"><div style="font-size:22px;margin-bottom:10px">⏳</div><div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">Pagos pendientes</div>' +
    '<div style="font-family:\'Fraunces\',serif;font-size:26px;color:var(--amber)">' + pendientes.length + '</div><div class="text-sm muted" style="margin-top:2px">' + fmtMoney(montoPendiente) + ' por confirmar</div></div>' +
    '<div class="card"><div style="width:34px;height:34px;border-radius:9px;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;margin-bottom:10px;color:var(--blue)">' + iconSvg('receipt') + '</div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">Facturas emitidas</div><div style="font-family:\'Fraunces\',serif;font-size:26px">' + DATA.facturas.length + '</div>' +
    '<div class="text-sm muted" style="margin-top:2px">' + facturasEnviadas + ' enviadas · ' + facturasPendientes + ' pendientes</div></div>' +
    '<div class="card"><div style="width:34px;height:34px;border-radius:9px;background:var(--clay-soft);display:flex;align-items:center;justify-content:center;margin-bottom:10px;color:var(--clay)">' + iconSvg('undo') + '</div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:4px">Reembolsos pendientes</div><div style="font-family:\'Fraunces\',serif;font-size:26px;color:var(--clay)">' + reembolsosSolicitados.length + '</div>' +
    '<div class="text-sm muted" style="margin-top:2px">' + fmtMoney(montoReembolsosRevision) + ' en revisión</div></div></div>';

  html += '<div class="flex gap-18 flex-wrap mb-26">';
  html += '<div class="card" style="flex:1.6;min-width:360px"><div class="flex justify-between items-center mb-18"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0">💰 Ingresos mensuales ' + new Date().getFullYear() + '</h3>' +
    '<button class="btn btn-ghost btn-sm" onclick="toast(\'Exportando gráfico de ingresos...\')">⬇ Exportar</button></div>';
  html += '<div class="flex items-end gap-10" style="height:160px;margin-bottom:10px">';
  meses.forEach(m => {
    html += '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:flex-end;height:100%"><span class="text-sm" style="font-weight:700;color:var(--ink-soft);margin-bottom:4px">' + (m.valor/1000000).toFixed(1) + 'M</span>' +
      '<div style="width:100%;border-radius:6px 6px 0 0;background:linear-gradient(180deg,#4A3A2C,var(--blue-deep));height:'+((m.valor/maxIngreso)*100)+'%;min-height:6px"></div></div>';
  });
  html += '</div><div class="flex justify-between text-sm muted mb-16">' + meses.map(m=>'<span style="flex:1;text-align:center">'+m.mes+'</span>').join('') + '</div>';
  html += '<div class="flex gap-24 text-sm" style="border-top:1px solid var(--line);padding-top:14px">' +
    '<div><div class="muted">Mejor mes</div><div style="font-weight:700">' + mejorMes.mes + ' · ' + (mejorMes.valor/1000000).toFixed(1) + 'M</div></div>' +
    '<div><div class="muted">Promedio</div><div style="font-weight:700">' + (promedioMes/1000000).toFixed(1) + 'M / mes</div></div>' +
    '<div><div class="muted">Total ' + new Date().getFullYear() + '</div><div style="font-weight:700;color:var(--sage)">' + (totalAnio/1000000).toFixed(1) + 'M</div></div></div></div>';

  html += '<div class="card" style="flex:1;min-width:260px"><h3 style="font-family:\'Fraunces\',serif;font-size:16px;margin:0 0 16px">💳 Por método de pago</h3>';
  DATA.ingresosPorMetodo.forEach(m => {
    html += '<div style="margin-bottom:14px"><div class="flex justify-between text-sm mb-6"><span>' + esc(m.metodo) + '</span><span style="font-weight:700">' + m.pct + '%</span></div>' +
      '<div class="progress-bar"><div class="progress-fill" style="width:'+m.pct+'%;background:'+m.color+'"></div></div></div>';
  });
  html += '</div></div>';

  html += '<div class="card"><div class="flex justify-between items-center mb-16"><h3 style="font-family:\'Fraunces\',serif;font-size:17px;margin:0">⏳ Pagos pendientes de confirmación</h3>' +
    '<button class="btn btn-ghost btn-sm" onclick="goTo(\'pagos\')">Ver todos</button></div>';
  if (pendientes.length === 0) html += '<div class="empty-state">No hay pagos pendientes 🎉</div>';
  pendientes.forEach(p => {
    const icon = p.metodo.includes('Efectivo') ? '💵' : p.metodo.includes('Transferencia') ? '🏦' : '💳';
    html += '<div class="flex items-center gap-14" style="background:var(--bone);border-radius:10px;padding:14px 16px;border-left:4px solid var(--amber);margin-bottom:10px">' +
      '<div style="width:34px;height:34px;border-radius:9px;background:var(--amber-soft);display:flex;align-items:center;justify-content:center;flex-shrink:0;font-size:15px">' + icon + '</div>' +
      '<div style="flex:1"><div style="font-size:14px;font-weight:700">' + esc(p.estudiante) + ' — ' + esc(p.metodo) + '</div>' +
      '<div class="text-sm muted">' + esc(p.curso) + (p.detalle?' · '+esc(p.detalle):p.referencia?' · Ref: '+esc(p.referencia):'') + ' · ' + esc(p.horaTexto) + '</div></div>' +
      '<div style="font-weight:800;font-size:15px">' + fmtMoney(p.monto) + '</div>' +
      '<button class="btn btn-sm" style="background:var(--sage);color:#fff" onclick="goTo(\'pagos\')">✓ Confirmar</button></div>';
  });
  html += '</div>';
  cont.innerHTML = html;
}
