/* ================================================================
   BIOLearn — biolearn_estudiante_inscripciones.js
   Fase 3 del módulo de Cursos, conectada al backend real. Conserva las pantallas del prototipo
   (catálogo con detalle y carrito, Mis cursos, lista de lecciones y lección con sus materiales)
   y reemplaza sus datos simulados por los de la API:
   - Inscripción: un curso gratuito queda activo; uno con precio queda «pendiente de pago»
     y se paga en «Mis pagos» (biolearn_estudiante_pagos.js). El código del colegio vincula la inscripción.
   - Progreso: el estudiante marca cada material como visto; el porcentaje y la completación de la
     lección y del curso los calcula el servidor. Las lecciones con prerrequisito quedan bloqueadas.
   Las demás secciones del portal (actividades, evaluaciones, foros…) siguen con datos de demostración.
================================================================ */
const API_BASE = 'http://localhost:8080';
let EST_CATALOGO = [];
let EST_DETALLE = null;
let EST_CODIGO = null;          // curso encontrado por código { curso, codigo }
let EST_INSCRIPCIONES = [];
let EST_LECCIONES = [];
let EST_LECCION = null;
let EST_FILTRO = '';

/* ---------------------------------------------------------------- sesión y comunicación */
function authHeadersEst() {
  const token = sessionStorage.getItem('biolearn_token');
  if (!token) { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; return null; }
  return { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token };
}

async function pedirApiEst(metodo, ruta, cuerpo) {
  const headers = authHeadersEst();
  if (!headers) throw new Error('Sesión no válida');
  const op = { method: metodo, headers };
  if (cuerpo !== undefined) op.body = JSON.stringify(cuerpo);
  const r = await fetch(API_BASE + ruta, op);
  if (r.status === 401) {
    sessionStorage.removeItem('biolearn_token'); sessionStorage.removeItem('biolearn_usuario');
    toast('Tu sesión expiró. Inicia sesión nuevamente.', true);
    setTimeout(() => { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; }, 1200);
    throw new Error('Sesión expirada');
  }
  const data = await r.json().catch(() => ({}));
  if (!r.ok) {
    const det = data.detalles && data.detalles.length ? ' (' + data.detalles.join('; ') + ')' : '';
    throw new Error((data.mensaje || 'Error ' + r.status) + det);
  }
  return data;
}

function urlEst(u) { return u && u.startsWith('/api/') ? API_BASE + u : u; }
function precioEst(c) { return c.gratuito || !Number(c.precio) ? 'Gratuito' : '$' + Number(c.precio).toLocaleString('es-CO'); }
const ESTADO_INSC_LABEL = { activa: 'Inscrito', completada: 'Completado', pendiente_pago: 'Pendiente de pago', retirada: 'Retirado', suspendida: 'Suspendida' };
function pillInscripcion(e) {
  const clase = { activa: 'pill-verde', completada: 'pill-azul', pendiente_pago: 'pill-ambar', retirada: 'pill-gris', suspendida: 'pill-clay' }[e] || 'pill-gris';
  return '<span class="pill ' + clase + '">' + esc(ESTADO_INSC_LABEL[e] || e) + '</span>';
}
function portadaEst(c, alto) {
  return c.imagenPortada
    ? '<div style="height:' + alto + 'px;border-radius:12px 12px 0 0;margin:-20px -20px 14px;overflow:hidden"><img src="' + esc(urlEst(c.imagenPortada)) + '" style="width:100%;height:100%;object-fit:cover" alt=""></div>'
    : '<div style="height:8px;background:' + esc(c.color || 'var(--blue)') + ';border-radius:12px 12px 0 0;margin:-20px -20px 14px"></div>';
}

/* Igual que en los demás portales (DEF-003): este portal es solo para estudiantes. */
document.addEventListener('DOMContentLoaded', function () {
  let u = null;
  try { u = JSON.parse(sessionStorage.getItem('biolearn_usuario') || 'null'); } catch (e) { u = null; }
  if (!sessionStorage.getItem('biolearn_token') || !u) { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; return; }
  if (u.rol !== 'estudiante') {
    document.body.innerHTML = '<div id="acceso-denegado" style="max-width:480px;margin:12vh auto;padding:32px;background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(27,50,44,.15);text-align:center">' +
      '<div style="font-size:40px">🔒</div><h2 style="margin:12px 0 8px">Acceso solo para estudiantes</h2><p style="color:#555">Tu cuenta (' + esc(u.nombre || '') + ', rol ' + esc(u.rol || '') + ') no tiene acceso a este portal.</p></div>';
    setTimeout(() => { window.location.href = '../prototipo-auth/biolearn_auth_prototipo.html'; }, 2500);
  }
});

/* ================================================================
   CATÁLOGO (con código de inscripción) · DETALLE · CARRITO
================================================================ */
function renderCatalogo(cont) {
  if (CHECKOUT_ABIERTO) { renderCheckout(cont); return; }
  if (CATALOGO_DETALLE) { renderCatalogoDetalle(cont); return; }
  cont.innerHTML = '<button class="btn btn-ghost" onclick="goTo(\'cursos\')" style="margin-bottom:16px">← Volver a mis cursos</button>' +
    '<div class="section-header"><div><h2>Catálogo de cursos</h2><p class="subtitle">Explora e inscríbete a nuevos cursos</p></div>' +
    '<button class="btn btn-primary" id="est-btn-carrito" onclick="CHECKOUT_ABIERTO=true;render()" style="display:none"></button></div>' +
    '<div class="card mb-14" style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">' +
      '<span style="font-size:22px">🏫</span><div style="flex:1;min-width:220px"><div style="font-weight:700">¿Tu colegio te dio un código de inscripción?</div>' +
      '<div class="text-sm muted">Escríbelo para encontrar el curso que te asignaron.</div></div>' +
      '<input id="est-codigo" maxlength="8" placeholder="Ej: K7P2M9QX" style="width:170px;font-family:monospace;text-transform:uppercase">' +
      '<button class="btn btn-primary" id="est-btn-codigo" onclick="buscarCodigoEst()">Buscar</button></div>' +
    '<div class="flex gap-10 mb-10"><input id="est-buscar" placeholder="Buscar por nombre, área o código..." style="width:320px" oninput="EST_FILTRO=this.value;pintarCatalogoEst()"></div>' +
    '<div id="est-catalogo" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px"><div class="empty-state">Cargando el catálogo...</div></div>';
  document.getElementById('est-buscar').value = EST_FILTRO;
  pedirApiEst('GET', '/api/estudiante/catalogo').then(lista => { EST_CATALOGO = lista; pintarCatalogoEst(); })
    .catch(err => { document.getElementById('est-catalogo').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; });
}

function normEst(t) { return String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, ''); }

function pintarCatalogoEst() {
  const cont = document.getElementById('est-catalogo'); if (!cont) return;
  const btn = document.getElementById('est-btn-carrito');
  btn.style.display = DATA.carrito.length ? '' : 'none'; btn.textContent = '🛒 Ver carrito (' + DATA.carrito.length + ')';
  const q = normEst(EST_FILTRO.trim());
  const lista = EST_CATALOGO.filter(c => !q || normEst([c.nombre, c.area, c.codigo].join(' ')).includes(q));
  if (!lista.length) { cont.innerHTML = '<div class="empty-state">No hay cursos que coincidan.</div>'; return; }
  cont.innerHTML = lista.map(c => {
    const inscrito = !!c.estadoInscripcion && c.estadoInscripcion !== 'retirada';
    const enCarrito = DATA.carrito.includes(c.id);
    const accion = inscrito ? '<button class="btn btn-ghost" style="flex:1" disabled>' + esc(ESTADO_INSC_LABEL[c.estadoInscripcion]) + '</button>'
      : '<button class="btn ' + (enCarrito ? 'btn-ghost' : 'btn-primary') + '" style="flex:1" onclick="' + (enCarrito ? 'quitarDelCarrito' : 'agregarAlCarrito') + '(' + c.id + ')">' + (enCarrito ? '✓ En el carrito' : '+ Agregar') + '</button>';
    return '<div class="card est-curso-catalogo" data-nombre="' + esc(c.nombre) + '">' + portadaEst(c, 90) +
      '<div class="flex justify-between items-start mb-8">' + (c.deMiColegio ? '<span class="pill pill-verde">🏫 Curso de tu colegio</span>' : '<span></span>') +
      '<span style="font-size:19px;font-weight:800;color:var(--blue-deep)">' + precioEst(c) + '</span></div>' +
      '<h3 style="font-size:17px;margin:0 0 4px">' + esc(c.nombre) + '</h3>' +
      (c.codigo ? '<div class="text-sm mb-4" style="font-family:monospace;font-weight:700;color:var(--blue)">' + esc(c.codigo) + '</div>' : '') +
      '<p class="text-sm muted mb-14">' + (c.docenteTitular ? 'Doc. ' + esc(c.docenteTitular) + ' · ' : '') + esc(c.area) + ' · Grado ' + esc(c.grado) + '</p>' +
      '<div class="flex gap-14 text-sm muted mb-14"><span>📖 ' + c.totalLecciones + ' lecciones</span>' + (c.horasEstimadas ? '<span>⏱ ' + c.horasEstimadas + ' h</span>' : '') + '</div>' +
      '<div class="flex gap-8"><button class="btn btn-ghost" style="flex:1" onclick="verDetalleCatalogo(' + c.id + ')">Ver detalle</button>' + accion + '</div></div>';
  }).join('');
}

function agregarAlCarrito(id) { if (!DATA.carrito.includes(id)) DATA.carrito.push(id); saveData(); toast('Curso agregado al carrito'); if (CATALOGO_DETALLE) render(); else pintarCatalogoEst(); }
function quitarDelCarrito(id) { DATA.carrito = DATA.carrito.filter(x => x !== id); saveData(); if (CATALOGO_DETALLE || CHECKOUT_ABIERTO) render(); else pintarCatalogoEst(); }
function verDetalleCatalogo(id) { CATALOGO_DETALLE = id; EST_CODIGO = null; render(); }
function volverAlCatalogo() { CATALOGO_DETALLE = null; CHECKOUT_ABIERTO = false; EST_CODIGO = null; render(); }

async function buscarCodigoEst() {
  const codigo = document.getElementById('est-codigo').value.trim().toUpperCase();
  if (codigo.length !== 8) { toast('El código de inscripción tiene 8 caracteres', true); return; }
  try {
    const c = await pedirApiEst('GET', '/api/estudiante/codigos/' + encodeURIComponent(codigo));
    EST_CODIGO = { curso: c, codigo };
    CATALOGO_DETALLE = c.id;
    render();
  } catch (err) { toast(err.message, true); }
}

function renderCatalogoDetalle(cont) {
  cont.innerHTML = '<button class="btn btn-ghost" onclick="volverAlCatalogo()" style="margin-bottom:16px">← Volver al catálogo</button><div id="est-detalle">Cargando...</div>';
  const cargar = EST_CODIGO && EST_CODIGO.curso.id === CATALOGO_DETALLE ? Promise.resolve(EST_CODIGO.curso) : pedirApiEst('GET', '/api/estudiante/catalogo/' + CATALOGO_DETALLE);
  cargar.then(c => { EST_DETALLE = c; pintarDetalleEst(c); }).catch(err => { document.getElementById('est-detalle').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; });
}

function pintarDetalleEst(c) {
  const inscrito = !!c.estadoInscripcion && c.estadoInscripcion !== 'retirada';
  const conCodigo = EST_CODIGO && EST_CODIGO.curso.id === c.id;
  const enCarrito = DATA.carrito.includes(c.id);
  let html = '<div class="flex gap-14 flex-wrap" style="align-items:start">';
  html += '<div style="flex:2;min-width:320px">' +
    (conCodigo ? '<div class="card mb-14" style="background:var(--blue-soft);border-left:4px solid var(--blue)">🏫 <strong>Curso asignado por ' + esc(c.institucionCodigo) + '</strong> · al inscribirte con el código ' + esc(EST_CODIGO.codigo) + ' quedarás vinculado a tu colegio.</div>' : '') +
    '<h2 style="font-size:24px;margin:0 0 6px;color:' + esc(c.color || 'inherit') + '">' + esc(c.nombre) + '</h2>' +
    '<p class="text-sm muted mb-18">' + (c.docenteTitular ? 'Doc. ' + esc(c.docenteTitular) + ' · ' : '') + esc(c.area) + ' · Grado ' + esc(c.grado) + '</p>' +
    (c.descripcion ? '<div class="card mb-14"><h3 style="font-size:15px;margin:0 0 8px">Descripción</h3><p class="text-sm">' + esc(c.descripcion) + '</p></div>' : '') +
    (c.objetivos ? '<div class="card mb-14"><h3 style="font-size:15px;margin:0 0 8px">Objetivos de aprendizaje</h3><p class="text-sm">' + esc(c.objetivos) + '</p></div>' : '') +
    (c.infoEstudiante ? '<div class="card mb-14"><h3 style="font-size:15px;margin:0 0 8px">Antes de inscribirte</h3><div class="biolearn-richtext-body" style="font-size:13.5px">' + c.infoEstudiante + '</div></div>' : '') +
    '<div class="card"><h3 style="font-size:15px;margin:0 0 8px">Temario</h3>' + (c.lecciones && c.lecciones.length ? '<ul style="margin:0;padding-left:20px;font-size:13.5px;line-height:1.9">' + c.lecciones.map(t => '<li>' + esc(t) + '</li>').join('') + '</ul>' : '<p class="text-sm muted">El temario se publicará pronto.</p>') + '</div></div>';
  html += '<div style="flex:1;min-width:260px"><div class="card">' +
    '<div style="font-size:26px;font-weight:800;color:var(--blue-deep);margin-bottom:14px">' + precioEst(c) + '</div>' +
    '<div class="flex gap-14 text-sm muted mb-18" style="flex-wrap:wrap"><span>📖 ' + c.totalLecciones + ' lecciones</span>' + (c.horasEstimadas ? '<span>⏱ ' + c.horasEstimadas + ' h</span>' : '') + (c.creditos ? '<span>🎓 ' + c.creditos + ' créditos</span>' : '') + '</div>';
  if (inscrito) {
    html += '<div class="mb-8">' + pillInscripcion(c.estadoInscripcion) + '</div>' +
      (c.estadoInscripcion === 'activa' || c.estadoInscripcion === 'completada' ? '<button class="btn btn-primary w-full" style="justify-content:center" onclick="goTo(\'cursos\');abrirCurso(' + c.id + ')">Ir al curso</button>' : '');
  } else {
    html += '<button class="btn btn-primary w-full" id="est-btn-inscribir" style="justify-content:center;margin-bottom:8px" onclick="inscribirseEst(' + c.id + ',' + (conCodigo ? '\'' + EST_CODIGO.codigo + '\'' : 'null') + ')">' + (c.gratuito ? 'Inscribirme gratis' : 'Inscribirme') + '</button>' +
      (conCodigo ? '' : '<button class="btn btn-ghost w-full" style="justify-content:center" onclick="' + (enCarrito ? 'quitarDelCarrito' : 'agregarAlCarrito') + '(' + c.id + ')">' + (enCarrito ? '✓ En el carrito' : '+ Agregar al carrito') + '</button>') +
      (!c.gratuito ? '<p class="text-sm muted" style="margin-top:10px">Tu cupo quedará reservado como «pendiente de pago»; podrás pagarlo cuando se habilite el pago en línea.</p>' : '');
  }
  html += '</div></div></div>';
  document.getElementById('est-detalle').innerHTML = html;
}

async function inscribirseEst(idCurso, codigo) {
  try {
    const r = await pedirApiEst('POST', '/api/estudiante/inscripciones', { idCurso, codigo: codigo || null });
    DATA.carrito = DATA.carrito.filter(x => x !== idCurso); saveData();
    toast(r.estado === 'activa' ? '¡Inscripción exitosa! Ya puedes ver el curso en «Mis cursos»' : 'Cupo reservado en "' + r.curso + '": pendiente de pago');
    CATALOGO_DETALLE = null; EST_CODIGO = null;
    goTo('cursos');
  } catch (err) { toast(err.message, true); }
}

function renderCheckout(cont) {
  cont.innerHTML = '<button class="btn btn-ghost" onclick="CHECKOUT_ABIERTO=false;render()" style="margin-bottom:16px">← Volver al catálogo</button>' +
    '<div class="section-header"><div><h2>Finalizar inscripción</h2><p class="subtitle">Revisa los cursos de tu carrito</p></div></div><div id="est-checkout">Cargando...</div>';
  const cargar = EST_CATALOGO.length ? Promise.resolve(EST_CATALOGO) : pedirApiEst('GET', '/api/estudiante/catalogo');
  cargar.then(lista => {
    EST_CATALOGO = lista;
    const items = DATA.carrito.map(id => lista.find(c => c.id === id)).filter(Boolean);
    if (!items.length) { document.getElementById('est-checkout').innerHTML = '<div class="empty-state">Tu carrito está vacío.</div>'; return; }
    const total = items.reduce((s, c) => s + Number(c.precio || 0), 0);
    let html = '<div class="flex gap-14 flex-wrap" style="align-items:start"><div style="flex:1.4;min-width:300px"><div class="card">';
    items.forEach((c, i) => {
      html += '<div class="flex justify-between items-center" style="padding:' + (i > 0 ? '14px 0' : '0 0 14px') + ';' + (i > 0 ? 'border-top:1px solid var(--line)' : '') + '">' +
        '<div><div style="font-size:14px;font-weight:700">' + esc(c.nombre) + '</div><div class="text-sm muted">' + (c.docenteTitular ? 'Doc. ' + esc(c.docenteTitular) : esc(c.area)) + '</div></div>' +
        '<div class="flex items-center gap-10"><span style="font-weight:700">' + precioEst(c) + '</span><button onclick="quitarDelCarrito(' + c.id + ')" style="border:none;background:none;color:var(--clay);cursor:pointer;font-size:16px">✕</button></div></div>';
    });
    html += '</div></div><div style="flex:1;min-width:280px"><div class="card">' +
      '<div class="flex justify-between mb-14" style="font-size:15px;font-weight:800"><span>Total</span><span>$' + total.toLocaleString('es-CO') + '</span></div>' +
      '<p class="text-sm muted">Los cursos gratuitos quedan activos de inmediato. Los cursos con precio quedan reservados y pasas a pagarlos en «Mis pagos» (tarjeta, PSE, Nequi o transferencia).</p>' +
      '<button class="btn btn-primary w-full" id="est-btn-confirmar" style="justify-content:center;margin-top:10px" onclick="confirmarPagoEst()">Confirmar inscripción</button></div></div></div>';
    document.getElementById('est-checkout').innerHTML = html;
  }).catch(err => { document.getElementById('est-checkout').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; });
}

async function confirmarPagoEst() {
  const ids = DATA.carrito.slice();
  let activas = 0, pendientes = 0; const errores = [];
  for (const id of ids) {
    try { const r = await pedirApiEst('POST', '/api/estudiante/inscripciones', { idCurso: id }); r.estado === 'activa' ? activas++ : pendientes++; DATA.carrito = DATA.carrito.filter(x => x !== id); }
    catch (err) { errores.push(err.message); }
  }
  saveData();
  if (errores.length) toast(errores[0], true);
  if (activas || pendientes) toast((activas ? activas + ' inscripción(es) activa(s)' : '') + (activas && pendientes ? ' · ' : '') + (pendientes ? pendientes + ' pendiente(s) de pago' : ''));
  CHECKOUT_ABIERTO = false; CATALOGO_DETALLE = null;
  goTo('cursos');
}

/* ================================================================
   MIS CURSOS · LECCIONES DEL CURSO · LECCIÓN
================================================================ */
function renderCursos(cont) {
  if (LECCION_ACTIVA) { renderLeccionDetalle(cont); return; }
  if (CURSO_ACTIVO) { renderCursoDetalle(cont); return; }
  cont.innerHTML = '<div class="section-header"><div><h2>Mis cursos</h2><p class="subtitle">Cursos en los que estás inscrito</p></div></div>' +
    '<div id="est-mis-cursos" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px"><div class="empty-state">Cargando tus cursos...</div></div>';
  pedirApiEst('GET', '/api/estudiante/inscripciones').then(lista => {
    EST_INSCRIPCIONES = lista;
    const visibles = lista.filter(i => i.estado !== 'retirada');
    let html = visibles.map(i => {
      const pct = Math.round(Number(i.porcentaje || 0));
      const conAcceso = i.estado === 'activa' || i.estado === 'completada';
      const color = i.estado === 'completada' ? 'var(--amber)' : pct > 0 ? 'var(--sage)' : 'var(--blue)';
      const etapa = i.estado === 'completada' ? 'Completado' : i.estado === 'pendiente_pago' ? 'Pendiente de pago' : i.estado === 'suspendida' ? 'Suspendida' : pct > 0 ? 'En progreso' : 'Por comenzar';
      return '<div class="card est-mi-curso" data-nombre="' + esc(i.curso) + '" ' + (conAcceso ? 'onclick="abrirCurso(' + i.idCurso + ')" style="cursor:pointer;' : 'style="opacity:.8;') + 'border-top:4px solid ' + color + '">' +
        '<div class="flex justify-between items-start mb-14"><span style="font-size:30px">🧬</span>' + pillInscripcion(i.estado) + '</div>' +
        '<div class="text-sm" style="font-weight:700;color:var(--ink-soft);text-transform:uppercase;margin-bottom:4px">' + etapa + '</div>' +
        '<h3 style="font-size:17px;margin:0 0 4px">' + esc(i.curso) + '</h3>' +
        (i.codigoCurso ? '<div class="text-sm mb-4" style="font-family:monospace;font-weight:700;color:' + color + '">' + esc(i.codigoCurso) + '</div>' : '') +
        '<p class="text-sm muted mb-14">' + i.leccionesCompletadas + ' de ' + i.totalLecciones + ' lecciones' + (i.institucion ? ' · 🏫 ' + esc(i.institucion) : '') + '</p>' +
        (conAcceso ? '<div class="progress-bar"><div class="progress-fill" style="width:' + pct + '%;background:' + color + '"></div></div><p class="text-sm" style="color:' + color + ';font-weight:700;margin:6px 0 0">' + pct + '% completado</p>'
          : (i.estado === 'pendiente_pago' ? '<p class="text-sm muted">Podrás ver las lecciones cuando se confirme el pago.</p><button class="btn btn-primary btn-xs est-btn-pagar" onclick="event.stopPropagation();goTo(\'pagos\')">💳 Pagar ahora</button>' : '<p class="text-sm muted">Comunícate con el administrador.</p>')) +
        (i.estado === 'activa' || i.estado === 'pendiente_pago' ? '<button class="btn btn-ghost btn-xs" style="margin-top:10px" onclick="event.stopPropagation();retirarseEst(' + i.id + ',\'' + esc(i.curso).replace(/'/g, '') + '\')">Retirarme</button>' : '') +
        '</div>';
    }).join('');
    html += '<div onclick="goTo(\'catalogo\')" class="card" style="border:2px dashed var(--line);min-height:220px;display:flex;flex-direction:column;align-items:center;justify-content:center;cursor:pointer;gap:10px">' +
      '<div style="width:44px;height:44px;border-radius:50%;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;font-size:20px;color:var(--blue)">+</div>' +
      '<span style="font-weight:700;color:var(--blue)">Inscribirse a un curso</span></div>';
    document.getElementById('est-mis-cursos').innerHTML = html;
  }).catch(err => { document.getElementById('est-mis-cursos').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; });
}

function retirarseEst(id, nombre) {
  confirmarAccion('¿Retirarte de <strong>' + esc(nombre) + '</strong>? Si vuelves a inscribirte, conservarás tu progreso.', async function () {
    try { await pedirApiEst('PATCH', '/api/estudiante/inscripciones/' + id + '/retiro'); toast('Te retiraste de "' + nombre + '"'); render(); }
    catch (err) { toast(err.message, true); }
  }, 'Retirarme');
}

function abrirCurso(id) { CURSO_ACTIVO = Number(id); LECCION_ACTIVA = null; render(); }
function abrirLeccion(id) { LECCION_ACTIVA = Number(id); render(); }

function renderCursoDetalle(cont) {
  cont.innerHTML = '<button class="btn btn-ghost" onclick="volverAMisCursos()" style="margin-bottom:16px">← Volver a mis cursos</button><div id="est-curso">Cargando...</div>';
  pedirApiEst('GET', '/api/estudiante/cursos/' + CURSO_ACTIVO + '/lecciones').then(lecciones => {
    EST_LECCIONES = lecciones;
    const insc = EST_INSCRIPCIONES.find(i => i.idCurso === CURSO_ACTIVO) || {};
    const completadas = lecciones.filter(l => l.estadoProgreso === 'completada').length;
    const pct = lecciones.length ? Math.round(Number(lecciones[0].porcentajeCurso || 0)) : 0;
    const nombre = lecciones.length ? lecciones[0].curso : (insc.curso || 'Curso');
    let html = '<div class="section-header"><div><h2>' + esc(nombre) + '</h2><p class="subtitle">' + completadas + '/' + lecciones.length + ' lecciones completadas · ' + pct + '% del curso</p></div>' +
      (lecciones.length && lecciones[0].estadoInscripcion === 'completada' ? pillInscripcion('completada') : '') + '</div>' +
      '<div class="progress-bar" style="margin-bottom:20px"><div class="progress-fill" id="est-curso-avance" style="width:' + pct + '%"></div></div>' +
      '<div class="text-sm" style="font-weight:700;color:var(--ink-soft);text-transform:uppercase;letter-spacing:.3px;margin-bottom:10px">📖 Lecciones — ' + esc(nombre) + '</div>';
    if (!lecciones.length) html += '<div class="empty-state">Este curso todavía no tiene lecciones publicadas.</div>';
    lecciones.forEach((l, i) => {
      const estado = l.bloqueada ? 'bloqueada' : l.estadoProgreso;
      const borde = estado === 'completada' ? 'var(--sage)' : estado === 'en_progreso' ? 'var(--amber)' : 'var(--line)';
      const fondo = estado === 'completada' ? 'var(--blue-soft)' : estado === 'en_progreso' ? 'var(--amber-soft)' : 'var(--bone)';
      const pctL = Math.round(Number(l.porcentaje || 0));
      const detalle = estado === 'completada' ? 'Completada · ' + l.totalMateriales + ' materiales'
        : estado === 'en_progreso' ? 'En progreso · ' + pctL + '% · ' + l.materialesVistos + ' de ' + l.totalMateriales + ' materiales vistos'
        : estado === 'bloqueada' ? 'Bloqueada · requiere completar «' + esc(l.prerequisito || 'la lección anterior') + '»' : 'Disponible para comenzar · ' + l.totalMateriales + ' materiales';
      html += '<div class="flex items-center gap-14 est-leccion" data-nombre="' + esc(l.nombre) + '" onclick="' + (l.bloqueada ? 'toast(\'Completa primero la lección «' + esc(l.prerequisito || '').replace(/'/g, '') + '»\', true)' : 'abrirLeccion(' + l.id + ')') + '" style="background:var(--panel);border-radius:12px;border:1px solid var(--line);border-left:4px solid ' + borde + ';padding:14px 16px;cursor:pointer;margin-bottom:10px;opacity:' + (l.bloqueada ? '.65' : '1') + '">' +
        '<div style="width:34px;height:34px;border-radius:50%;background:' + fondo + ';display:flex;align-items:center;justify-content:center;font-weight:700;font-size:13px;flex-shrink:0">' + (estado === 'completada' ? '✓' : estado === 'bloqueada' ? '🔒' : (i + 1)) + '</div>' +
        '<div style="flex:1;min-width:0"><div style="font-size:14px;font-weight:600">' + (i + 1) + '. ' + esc(l.nombre) + '</div><div class="text-sm muted est-leccion-detalle">' + detalle + '</div>' +
        (estado === 'en_progreso' ? '<div style="margin-top:6px;width:160px" class="progress-bar"><div class="progress-fill" style="width:' + pctL + '%;background:var(--amber)"></div></div>' : '') + '</div>' + pill(estado) + '</div>';
    });
    document.getElementById('est-curso').innerHTML = html;
  }).catch(err => { document.getElementById('est-curso').innerHTML = '<div class="empty-state">' + esc(err.message) + '</div>'; });
}

function renderLeccionDetalle(cont) {
  cont.innerHTML = '<button class="btn btn-ghost" onclick="volverALecciones()" style="margin-bottom:16px">← Volver a lecciones</button><div id="est-leccion">Cargando...</div>';
  pedirApiEst('GET', '/api/estudiante/lecciones/' + LECCION_ACTIVA).then(l => { EST_LECCION = l; pintarLeccionEst(); })
    .catch(err => {
      document.getElementById('est-leccion').innerHTML = '<div class="card" style="padding:40px;text-align:center"><div style="font-size:32px">🔒</div><h3 style="font-size:19px;margin:12px 0 8px">No puedes abrir esta lección</h3><p class="muted">' + esc(err.message) + '</p></div>';
    });
}

function pintarLeccionEst() {
  const l = EST_LECCION;
  let html = '<div class="section-header"><div><h2>' + esc(l.nombre) + '</h2><p class="subtitle">' + esc(l.curso) + (l.descripcion ? ' · ' + esc(l.descripcion) : '') + '</p></div></div>' +
    '<div class="card mb-14" id="est-leccion-progreso">' + progresoLeccionHTML(l) + '</div>';
  if (!l.materiales.length) html += '<div class="empty-state">Esta lección todavía no tiene contenido publicado.</div>';
  l.materiales.forEach(x => { html += materialCardHTML(x); });
  document.getElementById('est-leccion').innerHTML = html;
  conectarSeguimientoEst();
}

function progresoLeccionHTML(l) {
  const pct = Math.round(Number(l.porcentaje || 0));
  return '<div class="flex justify-between text-sm mb-8"><strong>' + (l.estadoProgreso === 'completada' ? '✓ Lección completada' : 'Tu avance en la lección') + '</strong>' +
    '<span>' + l.materialesVistos + ' de ' + l.totalMateriales + ' materiales vistos · ' + pct + '%</span></div>' +
    '<div class="progress-bar"><div class="progress-fill" style="width:' + pct + '%;background:' + (l.estadoProgreso === 'completada' ? 'var(--sage)' : 'var(--amber)') + '"></div></div>' +
    '<p class="text-sm muted" style="margin:8px 0 0">Cada material se marca como visto automáticamente cuando lo revisas: al llegar al final de una lectura, al terminar un video o después de explorar un laboratorio, un juego o un documento.</p>';
}

/* ================================================================
   MARCADO AUTOMÁTICO DE MATERIALES VISTOS (migración 006)
   Al abrir un material, el portal registra la apertura en el servidor y sigue el criterio de su tipo:
   - lectura: llegar al final del texto · video: verlo hasta el 90 % (API de YouTube) o terminarlo
   - laboratorio y juego: mantenerlo iniciado · documento y artículo: mantenerlo abierto
   En todos los casos debe cumplirse el tiempo mínimo que exige el servidor, contando solo el tiempo
   con la pestaña visible. El estudiante ya no marca ni desmarca a mano.
================================================================ */
const SEGUIMIENTO = {};       // idMaterial → { minimos, segundos, criterio, enviando, visto }
let INTERVALO_SEGUIMIENTO = null;

function materialEstudiante(id) { return (EST_LECCION && EST_LECCION.materiales || []).find(x => x.material.id === id); }

function estadoVistoHTML(x) {
  const s = SEGUIMIENTO[x.material.id];
  if (x.visto) return '<span class="est-visto est-visto-ok" style="font-size:13px;font-weight:700;color:var(--sage);white-space:nowrap">✓ Visto</span>';
  if (s && MATERIAL_EXPANDIDO[x.material.id]) {
    const falta = Math.max(0, s.minimos - s.segundos);
    const pista = falta > 0 ? 'En revisión · ' + falta + ' s' : (x.material.tipo === 'lectura' ? 'Llega al final del texto' : x.material.tipo === 'video' ? 'Termina el video' : 'Registrando…');
    return '<span class="est-visto est-visto-revision" style="font-size:13px;font-weight:700;color:var(--amber);white-space:nowrap">⏳ ' + pista + '</span>';
  }
  return '<span class="est-visto est-visto-pendiente" style="font-size:13px;color:var(--ink-soft);white-space:nowrap">Pendiente</span>';
}

function actualizarIndicadorEst(id) {
  const x = materialEstudiante(id), el = document.getElementById('est-estado-' + id);
  if (x && el) el.innerHTML = estadoVistoHTML(x);
}

async function toggleMaterial(id) {
  MATERIAL_EXPANDIDO[id] = !MATERIAL_EXPANDIDO[id];
  pintarLeccionEst();
  const x = materialEstudiante(id);
  if (MATERIAL_EXPANDIDO[id] && x && !x.visto && !SEGUIMIENTO[id]) {
    try {
      const a = await pedirApiEst('POST', '/api/estudiante/materiales/' + id + '/apertura');
      SEGUIMIENTO[id] = { minimos: a.segundosMinimos, segundos: a.segundosTranscurridos, criterio: false, enviando: false };
      if (a.visto) x.visto = true;
      actualizarIndicadorEst(id);
      conectarSeguimientoEst();
    } catch (err) { toast(err.message, true); }
  }
}

function iniciarMaterialInteractivo(id) { MATERIAL_INICIADO[id] = true; pintarLeccionEst(); }

/* Observadores del criterio de cada material abierto (se reconectan después de cada pintado) */
function conectarSeguimientoEst() {
  if (!INTERVALO_SEGUIMIENTO) INTERVALO_SEGUIMIENTO = setInterval(tickSeguimientoEst, 1000);
  Object.keys(SEGUIMIENTO).forEach(k => {
    const id = Number(k), s = SEGUIMIENTO[id], x = materialEstudiante(id);
    if (!x || x.visto || !MATERIAL_EXPANDIDO[id]) return;
    if (x.material.tipo === 'lectura') {
      const fin = document.getElementById('est-fin-' + id);
      if (fin && 'IntersectionObserver' in window) {
        new IntersectionObserver((entradas, obs) => {
          if (entradas.some(e => e.isIntersecting)) { s.criterio = true; obs.disconnect(); }
        }, { threshold: 0.5 }).observe(fin);
      } else s.criterio = true;
    }
    if (x.material.tipo === 'video' && MATERIAL_INICIADO[id]) conectarYoutubeEst(id);
  });
}

function tickSeguimientoEst() {
  if (!EST_LECCION || document.visibilityState !== 'visible') return;
  Object.keys(SEGUIMIENTO).forEach(k => {
    const id = Number(k), s = SEGUIMIENTO[id], x = materialEstudiante(id);
    if (!x || x.visto || !MATERIAL_EXPANDIDO[id]) return;
    const tipo = x.material.tipo;
    // laboratorios, juegos y videos cuentan desde que el estudiante los inicia
    if ((tipo === 'laboratorio' || tipo === 'juego' || tipo === 'video') && !MATERIAL_INICIADO[id]) { actualizarIndicadorEst(id); return; }
    s.segundos++;
    if (tipo === 'documento' || tipo === 'articulo' || tipo === 'laboratorio' || tipo === 'juego') s.criterio = true;
    if (tipo === 'video' && s.yt) {
      try {
        const d = s.yt.getDuration(), t = s.yt.getCurrentTime();
        if (d > 0 && t / d >= 0.9) s.criterio = true;
      } catch (e) { /* el reproductor aún no está listo */ }
    }
    if (tipo === 'video' && s.sinApi && s.segundos >= s.minimos) s.criterio = true;   // sin la API de YouTube: basta el tiempo
    actualizarIndicadorEst(id);
    if (s.criterio && s.segundos >= s.minimos && !s.enviando) enviarVistoEst(id);
  });
}

async function enviarVistoEst(id) {
  const s = SEGUIMIENTO[id]; s.enviando = true;
  try {
    const antes = EST_LECCION.estadoProgreso;
    const r = await pedirApiEst('PUT', '/api/estudiante/materiales/' + id + '/visto');
    // se actualizan los datos y el progreso sin volver a pintar los materiales (no se reinician videos ni simulaciones)
    const vistos = new Set(r.materiales.filter(m => m.visto).map(m => m.material.id));
    EST_LECCION.materiales.forEach(m => { if (vistos.has(m.material.id)) m.visto = true; });
    ['porcentaje', 'estadoProgreso', 'materialesVistos', 'totalMateriales', 'porcentajeCurso', 'estadoInscripcion'].forEach(c => { EST_LECCION[c] = r[c]; });
    document.getElementById('est-leccion-progreso').innerHTML = progresoLeccionHTML(EST_LECCION);
    actualizarIndicadorEst(id);
    toast('✓ "' + materialEstudiante(id).material.nombre + '" quedó como visto');
    if (EST_LECCION.estadoProgreso === 'completada' && antes !== 'completada') toast('¡Completaste la lección "' + EST_LECCION.nombre + '"!');
    if (EST_LECCION.estadoInscripcion === 'completada') toast('🎓 ¡Completaste el curso "' + EST_LECCION.curso + '"!');
  } catch (err) {
    // el servidor indica cuántos segundos faltan (por ejemplo, si la pestaña estuvo oculta): se reintenta después
    const m = /faltan (\d+)/.exec(err.message);
    if (m) { s.minimos = s.segundos + Number(m[1]) + 1; s.enviando = false; }
    else { toast(err.message, true); setTimeout(() => { s.enviando = false; }, 10000); }
  }
}

/* Videos de YouTube: se usa su API para saber cuánto se ha visto */
let YT_CARGANDO = null;
function cargarApiYoutubeEst() {
  if (window.YT && window.YT.Player) return Promise.resolve(true);
  if (!YT_CARGANDO) YT_CARGANDO = new Promise(res => {
    const previo = window.onYouTubeIframeAPIReady;
    window.onYouTubeIframeAPIReady = () => { if (previo) previo(); res(true); };
    const sc = document.createElement('script'); sc.src = 'https://www.youtube.com/iframe_api'; sc.onerror = () => res(false);
    document.head.appendChild(sc);
    setTimeout(() => res(!!(window.YT && window.YT.Player)), 8000);
  });
  return YT_CARGANDO;
}

function conectarYoutubeEst(id) {
  const s = SEGUIMIENTO[id], iframe = document.getElementById('est-video-' + id);
  if (!s || !iframe || s.ytIframe === iframe) return;
  s.ytIframe = iframe; s.yt = null;
  cargarApiYoutubeEst().then(ok => {
    if (!ok) { s.sinApi = true; return; }
    try {
      s.yt = new YT.Player(iframe, { events: { onStateChange: e => { if (e.data === 0) s.criterio = true; } } });   // 0 = terminó
    } catch (e) { s.sinApi = true; }
  });
}

function urlVideoEst(url) {
  if (!/youtube(-nocookie)?\.com\/embed\//.test(url)) return url;
  return url + (url.includes('?') ? '&' : '?') + 'enablejsapi=1&origin=' + encodeURIComponent(location.origin);
}

/* ---------------------------------------------------------------- tarjeta de material (diseño del prototipo) */
function materialCardHTML(x) {
  const m = x.material, t = MATERIAL_TYPES[m.tipo] || { label: m.tipo, icon: '📎', color: 'var(--ink-soft)' };
  const esInteractivo = m.tipo === 'laboratorio' || m.tipo === 'juego' || m.tipo === 'video';
  const expandido = !!MATERIAL_EXPANDIDO[m.id];
  const apoyo = (m.recursos || []).filter(r => r.tipoContenido !== 'primario');
  let html = '<div class="card mb-14 est-material" data-nombre="' + esc(m.nombre) + '" style="padding:0;overflow:hidden">' +
    '<div class="flex items-center gap-12" style="padding:14px 16px">' +
    '<div onclick="toggleMaterial(' + m.id + ')" class="flex items-center gap-12 est-material-cabecera" style="flex:1;cursor:pointer">' +
    '<div style="width:32px;height:32px;border-radius:8px;background:var(--blue-soft);display:flex;align-items:center;justify-content:center;flex-shrink:0;color:' + t.color + '">' + t.icon + '</div>' +
    '<div style="flex:1"><div style="font-size:14px;font-weight:600">' + esc(m.nombre) + '</div>' +
    '<div class="text-sm muted">' + t.label + (m.proveedor ? ' · ' + esc(m.proveedor) : '') + (m.duracionMin ? ' · ' + m.duracionMin + ' min' : '') + (apoyo.length ? ' · ' + apoyo.length + ' recurso(s) de apoyo' : '') + '</div></div>' +
    '<span style="transform:rotate(' + (expandido ? '90deg' : '0') + ');transition:transform .15s;color:var(--ink-soft)">›</span></div>' +
    '<span id="est-estado-' + m.id + '">' + estadoVistoHTML(x) + '</span></div>';
  if (expandido) {
    html += '<div style="padding:0 16px 18px">';
    if (m.tipo === 'lectura') html += '<div class="biolearn-richtext-body" style="background:' + esc(m.colorFondo || 'var(--bone)') + ';border-radius:10px;padding:16px">' + (m.contenido || '<p>Sin contenido.</p>') + '</div>' +
      '<div id="est-fin-' + m.id + '" class="est-fin-lectura" style="height:2px"></div>';
    if (m.tipo === 'articulo') html += '<div style="background:var(--bone);border-radius:10px;padding:16px;font-size:13.5px"><p style="margin:0 0 6px"><strong>' + esc(m.autores || '') + '</strong></p><p style="margin:0 0 6px;color:var(--ink-soft)">' + esc(m.revista || '') + (m.doi ? ' · DOI: ' + esc(m.doi) : '') + '</p>' + (m.descripcion ? '<p style="margin:0 0 6px">' + esc(m.descripcion) + '</p>' : '') + (m.url ? '<a href="' + esc(m.url) + '" target="_blank" rel="noopener" style="color:var(--blue);font-weight:700">Ver artículo completo →</a>' : '') + '</div>';
    if (m.tipo === 'documento') html += m.url ? '<div style="border:1px solid var(--line);border-radius:10px;overflow:hidden"><iframe class="est-pdf" src="' + esc(urlEst(m.url)) + '" style="width:100%;height:420px;border:none"></iframe></div><a href="' + esc(urlEst(m.url)) + '" target="_blank" rel="noopener" style="display:inline-block;margin-top:8px;color:var(--blue);font-weight:700;font-size:12.5px">↗ Abrir en una pestaña nueva</a>' : '<p class="text-sm muted">Documento sin archivo.</p>';
    if (esInteractivo) {
      if (!m.url) html += '<p class="text-sm muted">Este material todavía no tiene enlace.</p>';
      else if (!MATERIAL_INICIADO[m.id]) {
        html += '<div style="background:linear-gradient(135deg,#1E3345,#2E5588);border-radius:12px;padding:30px 20px;text-align:center">' +
          '<p style="color:#fff;font-size:13.5px;margin-bottom:14px">' + (m.proveedor ? 'Contenido alojado en ' + esc(m.proveedor) : m.tipo === 'video' ? 'Video de YouTube' : 'Contenido interactivo externo') + '</p>' +
          '<button onclick="event.stopPropagation();iniciarMaterialInteractivo(' + m.id + ')" style="background:#fff;color:var(--blue-deep);border:none;border-radius:30px;padding:10px 22px;font-weight:700;font-size:13.5px;cursor:pointer">▶ ' + (m.tipo === 'video' ? 'Reproducir video' : 'Iniciar simulación') + '</button></div>';
      } else {
        const src = m.tipo === 'video' ? urlVideoEst(m.url) : m.url;
        html += '<div style="border-radius:12px;overflow:hidden;border:1px solid var(--line)"><iframe id="' + (m.tipo === 'video' ? 'est-video-' : 'est-interactivo-') + m.id + '" class="est-interactivo" title="' + esc(m.nombre) + '" src="' + esc(src) + '" style="width:100%;height:380px;border:none" allow="accelerometer; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe></div>' +
          '<a href="' + esc(m.url) + '" target="_blank" rel="noopener" style="display:inline-block;margin-top:8px;color:var(--blue);font-weight:700;font-size:12.5px">↗ Abrir en una pestaña nueva</a>';
      }
    }
    if (m.guia) html += '<div style="margin-top:14px;background:var(--blue-soft);border-radius:10px;padding:14px"><div class="text-sm" style="font-weight:700;color:var(--blue-deep);text-transform:uppercase;margin-bottom:6px">Guía de observación</div><div class="biolearn-richtext-body">' + m.guia + '</div></div>';
    if (apoyo.length) {
      html += '<div style="margin-top:14px"><div class="text-sm" style="font-weight:700;color:var(--sage);text-transform:uppercase;margin-bottom:6px">Recursos de apoyo</div>' +
        apoyo.map(r => '<div class="flex items-center gap-10" style="padding:8px 0;border-top:1px solid var(--line)"><span>' + ({ video: '🎬', documento: '📄', imagen: '🖼', audio: '🎧', enlace: '🔗', interactivo: '🧩' }[r.tipoRecurso] || '📎') + '</span>' +
          '<span style="flex:1;font-size:13.5px">' + esc(r.nombre) + (r.duracionMin ? ' <span class="muted">· ' + r.duracionMin + ' min</span>' : '') + '</span>' +
          '<a href="' + esc(urlEst(r.url)) + '" target="_blank" rel="noopener" style="color:var(--blue);font-weight:700;font-size:12.5px">' + (r.tipoRecurso === 'video' ? '▶ Ver' : '↗ Abrir') + '</a></div>').join('') + '</div>';
    }
    html += '</div>';
  }
  return html + '</div>';
}
