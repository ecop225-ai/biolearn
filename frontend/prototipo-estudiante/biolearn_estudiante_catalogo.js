/* ================================================================
   BIOLearn — biolearn_estudiante_catalogo.js
   Catálogo de cursos: explorar, agregar al carrito, pagar e inscribirse
================================================================ */
let CATALOGO_DETALLE = null;
let CHECKOUT_ABIERTO = false;
let METODO_PAGO_TMP = 'pse';

function renderCatalogo(cont) {
  if (CHECKOUT_ABIERTO) { renderCheckout(cont); return; }
  if (CATALOGO_DETALLE) { renderCatalogoDetalle(cont); return; }

  let html = '<button class="btn btn-ghost" onclick="goTo(\'cursos\')" style="margin-bottom:16px">← Volver a mis cursos</button>';
  html += '<div class="section-header"><div><h2>Catálogo de cursos</h2><p class="subtitle">Explora e inscríbete a nuevos cursos</p></div>' +
    (DATA.carrito.length>0 ? '<button class="btn btn-primary" onclick="CHECKOUT_ABIERTO=true;render()">🛒 Ver carrito (' + DATA.carrito.length + ')</button>' : '') + '</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px">';
  DATA.catalogo.forEach(c => {
    const enCarrito = DATA.carrito.includes(c.id);
    html += '<div class="card"><div class="flex justify-between items-start mb-8"><span style="font-size:28px">🧬</span>' +
      '<span style="font-size:19px;font-weight:800;color:var(--blue-deep)">$' + c.precio.toLocaleString('es-CO') + '</span></div>' +
      '<h3 style="font-size:17px;margin:0 0 4px">' + esc(c.nombre) + '</h3>' +
      (c.codigo ? '<div class="text-sm mb-4" style="font-family:monospace;font-weight:700;color:var(--blue)">' + esc(c.codigo) + '</div>' : '') +
      '<p class="text-sm muted mb-14">Doc. ' + esc(c.docente) + ' · ' + esc(c.area) + ' · Grado ' + esc(c.grado) + '</p>' +
      '<div class="flex gap-14 text-sm muted mb-14"><span>📖 ' + c.lecciones + ' lecciones</span><span>⏱ ' + c.horas + ' h</span><span>👥 ' + c.inscritos + ' inscritos</span></div>' +
      '<div class="flex gap-8"><button class="btn btn-ghost" style="flex:1" onclick="verDetalleCatalogo(\''+c.id+'\')">Ver detalle</button>' +
      '<button class="btn '+(enCarrito?'btn-ghost':'btn-primary')+'" style="flex:1" onclick="'+(enCarrito?'quitarDelCarrito':'agregarAlCarrito')+'(\''+c.id+'\')">' + (enCarrito?'✓ En el carrito':'+ Agregar') + '</button></div></div>';
  });
  if (DATA.catalogo.length === 0) html += '<div class="empty-state">No hay cursos disponibles en el catálogo por ahora.</div>';
  html += '</div>';
  cont.innerHTML = html;
}
function agregarAlCarrito(id) { if (!DATA.carrito.includes(id)) DATA.carrito.push(id); saveData(); render(); toast('Curso agregado al carrito'); }
function quitarDelCarrito(id) { DATA.carrito = DATA.carrito.filter(x=>x!==id); saveData(); render(); }
function verDetalleCatalogo(id) { CATALOGO_DETALLE = id; render(); }
function volverAlCatalogo() { CATALOGO_DETALLE = null; CHECKOUT_ABIERTO = false; render(); }

function renderCatalogoDetalle(cont) {
  const c = DATA.catalogo.find(x=>x.id===CATALOGO_DETALLE);
  const enCarrito = DATA.carrito.includes(c.id);
  let html = '<button class="btn btn-ghost" onclick="volverAlCatalogo()" style="margin-bottom:16px">← Volver al catálogo</button>';
  html += '<div class="flex gap-14 flex-wrap" style="align-items:start">';
  html += '<div style="flex:2;min-width:320px"><h2 style="font-size:24px;margin:0 0 6px">' + esc(c.nombre) + '</h2>' +
    '<p class="text-sm muted mb-18">Doc. ' + esc(c.docente) + ' · ' + esc(c.area) + ' · Grado ' + esc(c.grado) + '</p>' +
    '<div class="card mb-14"><h3 style="font-size:15px;margin:0 0 8px">Descripción</h3><p class="text-sm">' + esc(c.descripcion) + '</p></div>' +
    '<div class="card mb-14"><h3 style="font-size:15px;margin:0 0 8px">Objetivos de aprendizaje</h3><p class="text-sm">' + esc(c.objetivos) + '</p></div>' +
    '<div class="card"><h3 style="font-size:15px;margin:0 0 8px">Temario</h3><ul style="margin:0;padding-left:20px;font-size:13.5px;line-height:1.9">' + c.temario.map(t=>'<li>'+esc(t)+'</li>').join('') + '</ul></div></div>';
  html += '<div style="flex:1;min-width:260px"><div class="card">' +
    '<div style="font-size:26px;font-weight:800;color:var(--blue-deep);margin-bottom:14px">$' + c.precio.toLocaleString('es-CO') + '</div>' +
    '<div class="flex gap-14 text-sm muted mb-18" style="flex-wrap:wrap"><span>📖 ' + c.lecciones + ' lecciones</span><span>⏱ ' + c.horas + ' h</span><span>🎓 ' + c.creditos + ' créditos</span><span>👥 ' + c.inscritos + ' inscritos</span></div>' +
    '<button class="btn '+(enCarrito?'btn-ghost':'btn-primary')+' w-full" style="justify-content:center;margin-bottom:8px" onclick="'+(enCarrito?'quitarDelCarrito':'agregarAlCarrito')+'(\''+c.id+'\')">' + (enCarrito?'✓ En el carrito':'+ Agregar al carrito') + '</button>' +
    '<button class="btn btn-ghost w-full" style="justify-content:center" onclick="agregarAlCarrito(\''+c.id+'\');CHECKOUT_ABIERTO=true;render()">Comprar ahora</button></div></div>';
  html += '</div>';
  cont.innerHTML = html;
}

function renderCheckout(cont) {
  const items = DATA.carrito.map(id => DATA.catalogo.find(c=>c.id===id)).filter(Boolean);
  const total = items.reduce((s,c)=>s+c.precio, 0);
  let html = '<button class="btn btn-ghost" onclick="CHECKOUT_ABIERTO=false;render()" style="margin-bottom:16px">← Volver al catálogo</button>';
  html += '<div class="section-header"><div><h2>Finalizar inscripción</h2><p class="subtitle">Revisa tu pedido y elige tu método de pago</p></div></div>';
  if (items.length === 0) { cont.innerHTML = html + '<div class="empty-state">Tu carrito está vacío.</div>'; return; }
  html += '<div class="flex gap-14 flex-wrap" style="align-items:start">';
  html += '<div style="flex:1.4;min-width:300px"><div class="card">';
  items.forEach((c,i) => {
    html += '<div class="flex justify-between items-center" style="padding:'+(i>0?'14px 0':'0 0 14px')+';'+(i>0?'border-top:1px solid var(--line)':'')+'">' +
      '<div><div style="font-size:14px;font-weight:700">' + esc(c.nombre) + '</div><div class="text-sm muted">Doc. ' + esc(c.docente) + '</div></div>' +
      '<div class="flex items-center gap-10"><span style="font-weight:700">$' + c.precio.toLocaleString('es-CO') + '</span><button onclick="quitarDelCarrito(\''+c.id+'\')" style="border:none;background:none;color:var(--clay);cursor:pointer;font-size:16px">✕</button></div></div>';
  });
  html += '</div></div>';
  html += '<div style="flex:1;min-width:280px"><div class="card">' +
    '<div class="flex justify-between mb-14" style="font-size:15px;font-weight:800"><span>Total</span><span>$' + total.toLocaleString('es-CO') + '</span></div>' +
    '<div class="text-sm" style="font-weight:700;text-transform:uppercase;color:var(--ink-soft);margin-bottom:10px">Método de pago</div>';
  [['pse','🏦 PSE'],['tarjeta','💳 Tarjeta de crédito/débito'],['nequi','📱 Nequi']].forEach(([val,label]) => {
    html += '<label class="flex items-center gap-8 text-sm" style="padding:10px;border:1.5px solid '+(METODO_PAGO_TMP===val?'var(--blue)':'var(--line)')+';background:'+(METODO_PAGO_TMP===val?'var(--blue-soft)':'transparent')+';border-radius:9px;margin-bottom:8px;cursor:pointer">' +
      '<input type="radio" name="metodo-pago" value="'+val+'" '+(METODO_PAGO_TMP===val?'checked':'')+' onchange="METODO_PAGO_TMP=\''+val+'\';render()" style="width:auto"> ' + label + '</label>';
  });
  html += '<button class="btn btn-primary w-full" style="justify-content:center;margin-top:10px" onclick="confirmarPagoEst()">Pagar $' + total.toLocaleString('es-CO') + '</button></div></div>';
  html += '</div>';
  cont.innerHTML = html;
}
function confirmarPagoEst() {
  const items = DATA.carrito.map(id => DATA.catalogo.find(c=>c.id===id)).filter(Boolean);
  if (items.length === 0) return;
  items.forEach(c => {
    const lecciones = [];
    DATA.cursos.push({ id: uid('curso'), codigo:c.codigo, nombre:c.nombre, docente:c.docente, docenteEmail: c.docente.toLowerCase().replace(/\s+/g,'.')+'@biolearn.co', area:c.area, grado:c.grado, imagenPortada:'', precio:c.precio, creditos:c.creditos, horas:c.horas, lecciones, companeros:[] });
    DATA.pagos.unshift({ id:uid('pago'), curso:c.nombre, fecha:new Date().toISOString().slice(0,10), monto:c.precio, metodo:METODO_PAGO_TMP.toUpperCase(), estado:'aprobado', factura:'FAC-'+new Date().getFullYear()+'-'+Math.floor(1000+Math.random()*8999) });
    DATA.catalogo = DATA.catalogo.filter(x=>x.id!==c.id);
  });
  DATA.carrito = [];
  CHECKOUT_ABIERTO = false; CATALOGO_DETALLE = null;
  saveData();
  toast('¡Inscripción exitosa! Ya puedes ver tu curso en "Mis cursos"');
  goTo('cursos');
}
