/* ================================================================
   BIOLearn — biolearn_supervisor_core.js
   Prototipo interactivo, módulo Supervisor (Supervisor)
   Paleta "institucional": índigo + dorado
================================================================ */

const STORAGE_KEY = "biolearn_supervisor_state_v1";
const SUPERVISOR = { nombre: "Mónica Salazar", cargo: "Supervisor", email: "monica.salazar@sanrafael.edu.co" };

function uid(p) { return p + '-' + Math.random().toString(36).slice(2, 8); }
function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
function fmtDate(iso) { return iso ? new Date(iso).toLocaleDateString('es-CO', { day:'2-digit', month:'short', year:'numeric' }) : '—'; }

/* ---------------------------- datos semilla ---------------------------- */
function seedData() {
  const institucion = {
    nombre:'Colegio San Rafael', nit:'900.223.114-2', rector:'Dr. Fernando Ríos',
    telefono:'601 745 8820', correo:'rectoria@sanrafael.edu.co', direccion:'Cra 8 # 14-32, Popayán, Cauca',
    codigoAfiliacion:'SANRAFAEL2026', estudiantesAfiliados:5, fechaRegistro:'2026-01-15',
    supervisores:['Mónica Salazar', 'Ricardo Peña'],
  };

  const estudiantes = [
    { id:uid('est'), nombre:'Ana Gómez', grado:'Décimo', curso:'Fundamentos de Genética', docente:'Laura Restrepo', estado:'Bien', avance:75, promedio:4.5, actividadesEntregadas:2, actividadesTotal:2, certificados:1,
      detalle:{ leccionesCompletadas:3, leccionesTotal:4, notasEvaluaciones:[{nombre:'Evaluación: leyes de Mendel', nota:4.5, max:5}], participacionForos:2,
        asistenciaEventos:[{evento:'Conferencia: Avances en biología celular 2024', asistio:false},{evento:'Salida virtual: monitoreo de fauna andina', asistio:true}] } },
    { id:uid('est'), nombre:'Juan Pérez', grado:'Décimo', curso:'Fundamentos de Genética', docente:'Laura Restrepo', estado:'Regular', avance:40, promedio:3.8, actividadesEntregadas:1, actividadesTotal:2, certificados:0,
      detalle:{ leccionesCompletadas:2, leccionesTotal:4, notasEvaluaciones:[{nombre:'Evaluación: leyes de Mendel', nota:3.5, max:5}], participacionForos:1,
        asistenciaEventos:[{evento:'Conferencia: Avances en biología celular 2024', asistio:true}] } },
    { id:uid('est'), nombre:'María Torres', grado:'Décimo', curso:'Fundamentos de Genética', docente:'Laura Restrepo', estado:'Bien', avance:90, promedio:4.8, actividadesEntregadas:2, actividadesTotal:2, certificados:1,
      detalle:{ leccionesCompletadas:4, leccionesTotal:4, notasEvaluaciones:[{nombre:'Evaluación: leyes de Mendel', nota:4.8, max:5}], participacionForos:3,
        asistenciaEventos:[{evento:'Conferencia: Avances en biología celular 2024', asistio:true}] } },
    { id:uid('est'), nombre:'Sara Higuera', grado:'Noveno', curso:'Conservación de Especies Andinas', docente:'Laura Restrepo', estado:'Riesgo', avance:20, promedio:2.3, actividadesEntregadas:0, actividadesTotal:2, certificados:0,
      detalle:{ leccionesCompletadas:1, leccionesTotal:3, notasEvaluaciones:[], participacionForos:0,
        asistenciaEventos:[{evento:'Salida virtual: monitoreo de fauna andina', asistio:true}] } },
    { id:uid('est'), nombre:'Pedro Silva', grado:'Noveno', curso:'Conservación de Especies Andinas', docente:'Laura Restrepo', estado:'Riesgo', avance:10, promedio:2.1, actividadesEntregadas:0, actividadesTotal:2, certificados:0,
      detalle:{ leccionesCompletadas:0, leccionesTotal:3, notasEvaluaciones:[], participacionForos:0, asistenciaEventos:[] } },
  ];

  const anaId = uid('com'), mariaId = uid('com');
  const foros = [
    { id:uid('foro'), nombre:'¿Qué pasaría si el ciclo celular fallara?', curso:'Fundamentos de Genética', docente:'Laura Restrepo',
      tema:'¿Qué pasaría si las células no se reprodujeran? ¿Qué enfermedades podrían originarse si el ciclo celular fallara?',
      estado:'activo', fechaCierre:'2026-09-30',
      comentarios: [
        { id:anaId, autor:'Ana Gómez', rol:'estudiante', titulo:'El cáncer: cuando el ciclo celular pierde el control',
          contenido:'Si las células no se reprodujeran correctamente, los organismos no podrían crecer ni reparar tejidos dañados. Cuando el ciclo sí ocurre pero sin control se genera el cáncer.',
          padreId:null, fecha:'2026-09-08', horaTexto:'hace 4 días', likes:3 },
        { id:uid('com'), autor:'Laura Restrepo', rol:'docente', titulo:null, contenido:'¡Buen análisis, Ana! Te invito a profundizar en cómo la telomerasa se relaciona con el envejecimiento celular.', padreId:anaId, fecha:'2026-09-09', horaTexto:'hace 3 días', likes:0 },
        { id:mariaId, autor:'María Torres', rol:'estudiante', titulo:'Apoptosis: la muerte celular programada como mecanismo de control',
          contenido:'Investigando encontré que existe un mecanismo llamado apoptosis, la "muerte celular programada", que evita que errores del ADN pasen a las células hijas.',
          padreId:null, fecha:'2026-09-09', horaTexto:'hace 3 días', likes:2 },
      ] },
    { id:uid('foro'), nombre:'Impacto humano en los páramos andinos', curso:'Conservación de Especies Andinas', docente:'Laura Restrepo',
      tema:'¿Qué actividades humanas amenazan más a los ecosistemas de páramo?', estado:'activo', fechaCierre:'2026-09-28',
      comentarios: [
        { id:uid('com'), autor:'Sara Higuera', rol:'estudiante', titulo:null, contenido:'Creo que la ganadería y la expansión agrícola son las mayores amenazas para el páramo.', padreId:null, fecha:'2026-09-05', horaTexto:'hace 1 semana', likes:1 },
      ] },
  ];

  const certificadosRegistro = [
    { codigo:'BIO-CERT-77621', receptor:'Ana Gómez', origen:'Participación — Salida virtual: monitoreo de fauna andina', emitidoEn:'2026-06-15', estado:'válido' },
    { codigo:'BIO-CERT-00098', receptor:'María Torres', origen:'Certificado de finalización — Fundamentos de Genética', emitidoEn:'2026-08-20', estado:'válido' },
  ];

  const reportesGenerados = [
    { id:uid('rep'), tipo:'Progreso de estudiantes', curso:'Fundamentos de Genética', periodo:'Ago - Sept 2026', formato:'PDF', fecha:'2026-09-05' },
  ];

  const mensajes = [
    { id:uid('msg'), de:'Laura Restrepo', rol:'docente', asunto:'Actualización de Sara Higuera', cuerpo:'Quería comentarle que Sara Higuera no ha entregado las últimas dos actividades. Le sugiero contactar a la familia para hacer seguimiento.', fecha:'2026-09-10', leido:false, respuestas:[] },
    { id:uid('msg'), de:'Ana Bernal', rol:'admin', asunto:'Renovación del código de afiliación', cuerpo:'El código de afiliación de su institución vence en 60 días. Le recomendamos gestionar la renovación con anticipación.', fecha:'2026-09-08', leido:true, respuestas:[] },
  ];

  const notificaciones = [
    { id:uid('not'), texto:'Sara Higuera está en riesgo académico (20% de avance)', fecha:'hace 1 día', leido:false, target:'estudiantes' },
    { id:uid('not'), texto:'Laura Restrepo respondió en el foro de Genética', fecha:'hace 3 días', leido:false, target:'foros' },
    { id:uid('not'), texto:'Tu reporte de Progreso está listo para descargar', fecha:'hace 5 días', leido:true, target:'reportes' },
  ];

  const historial = [
    { id:uid('hist'), evento:'Inicio de sesión', fecha:'2026-09-12 08:05' },
    { id:uid('hist'), evento:'Generación de reporte', fecha:'2026-09-05 15:20' },
  ];

  const cursosInscritos = [
    { id:uid('curso'), nombre:'Fundamentos de Genética', docente:'Laura Restrepo', lecciones:4, misInscritos:3, progresoProm:68, notaProm:4.4, estado:'activo',
      avancePorLeccion: [
        { nombre:'1. Introducción a la herencia mendeliana', completaron:3, total:3, promedio:4.6, estado:'completada' },
        { nombre:'2. Taller práctico de cruces genéticos', completaron:2, total:3, promedio:4.0, estado:'en_progreso' },
        { nombre:'3. Evaluación módulo 1', completaron:1, total:3, promedio:4.5, estado:'en_progreso' },
        { nombre:'4. CRISPR y edición génica', completaron:0, total:3, promedio:null, estado:'pendiente' },
      ] },
    { id:uid('curso'), nombre:'Conservación de Especies Andinas', docente:'Laura Restrepo', lecciones:2, misInscritos:2, progresoProm:15, notaProm:2.3, estado:'activo',
      avancePorLeccion: [
        { nombre:'1. Ecosistemas de páramo y su fragilidad', completaron:1, total:2, promedio:2.3, estado:'en_progreso' },
        { nombre:'2. Fauna endémica de los Andes', completaron:0, total:2, promedio:null, estado:'pendiente' },
      ] },
  ];

  return { institucion, estudiantes, cursosInscritos, foros, certificadosRegistro, reportesGenerados, mensajes, notificaciones, historial };
}

/* ---------------------------- estado y persistencia ---------------------------- */
let DATA = null;
let CURRENT = 'inicio';
let SIDEBAR_COLLAPSED = false;

function loadData() {
  try { const raw = localStorage.getItem(STORAGE_KEY); DATA = raw ? JSON.parse(raw) : seedData(); } catch (e) { DATA = seedData(); }
  if (!DATA) DATA = seedData();
}
function saveData() { try { localStorage.setItem(STORAGE_KEY, JSON.stringify(DATA)); } catch (e) {} }
function resetData() { DATA = seedData(); saveData(); render(); toast('Datos de prueba restablecidos'); }

function toast(text, esError) {
  const el = document.createElement('div');
  el.className = 'toast-item';
  el.innerHTML = (esError ? '⚠️ ' : '✅ ') + esc(text);
  document.getElementById('toast-cnt-box').appendChild(el);
  setTimeout(() => el.remove(), 3200);
}

/* ---------------------------- selector de archivos compartido ---------------------------- */
const MAX_ARCHIVO_MB = 10;
function seleccionarArchivos(accept, onListo) {
  const input = document.createElement('input');
  input.type = 'file'; input.multiple = true;
  if (accept) input.accept = accept;
  input.onchange = function () {
    const archivos = Array.from(input.files || []);
    if (archivos.length === 0) return;
    const validos = [], rechazados = [];
    archivos.forEach(f => { (f.size / (1024*1024) > MAX_ARCHIVO_MB ? rechazados : validos).push(f); });
    if (rechazados.length > 0) toast(rechazados.map(f=>f.name).join(', ') + ' supera el límite de ' + MAX_ARCHIVO_MB + ' MB', true);
    if (validos.length === 0) return;
    let restantes = validos.length; const resultado = [];
    validos.forEach(f => {
      const reader = new FileReader();
      reader.onload = e => { resultado.push({ nombre:f.name, tamañoMB:(f.size/(1024*1024)).toFixed(1), dataUrl:e.target.result }); restantes--; if (restantes===0) onListo(resultado); };
      reader.readAsDataURL(f);
    });
  };
  input.click();
}
function verImagenGrande(src) {
  const w = window.open('');
  if (!w) { toast('Habilita las ventanas emergentes para ver la imagen', true); return; }
  w.document.write('<body style="margin:0;background:#111;display:flex;align-items:center;justify-content:center;min-height:100vh"><img src="'+src+'" style="max-width:100%;max-height:100vh"></body>');
}

/* ---------------------------- pills ---------------------------- */
const ESTADO_ESTUDIANTE = {
  Bien: { bg:'var(--sage-soft)', fg:'var(--sage)', icon:'↑' },
  Regular: { bg:'var(--amber-soft)', fg:'#8A611E', icon:'–' },
  Riesgo: { bg:'var(--clay-soft)', fg:'var(--clay)', icon:'↓' },
};
function pillClass(estado) {
  const verde = ['activo','válido','Bien','Completada','completada'];
  const ambar = ['Regular','en_progreso','En progreso','en revisión'];
  const rojo = ['Riesgo'];
  const gris = ['cerrado','Pendiente','pendiente'];
  if (verde.includes(estado)) return 'pill-verde';
  if (ambar.includes(estado)) return 'pill-ambar';
  if (rojo.includes(estado)) return 'pill-clay';
  if (gris.includes(estado)) return 'pill-gris';
  return 'pill-gris';
}
function pill(estado) { return '<span class="pill ' + pillClass(estado) + '">' + esc(estado) + '</span>'; }

/* ---------------------------- Sidebar / Topbar / navegación ---------------------------- */
const NAV = [
  { key:'inicio', label:'Inicio', icon:'grid' },
  { group:'Seguimiento' },
  { key:'cursos', label:'Cursos inscritos', icon:'book' },
  { key:'estudiantes', label:'Estudiantes', icon:'users' },
  { key:'foros', label:'Foros', icon:'chat' },
  { key:'reportes', label:'Reportes', icon:'chart' },
  { group:'Institución' },
  { key:'institucion', label:'Mi institución', icon:'building' },
  { group:'Cuenta' },
  { key:'mensajeria', label:'Mensajería', icon:'send' },
  { key:'perfil', label:'Mi perfil', icon:'user' },
];
const ICONS = {
  grid:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>',
  book:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>',
  users:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
  chat:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/></svg>',
  chart:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>',
  building:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 22V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v18Z"/><path d="M2 22h20"/><path d="M9 8h1M14 8h1M9 12h1M14 12h1M9 16h1M14 16h1"/></svg>',
  send:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>',
  user:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
  logout:'<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>',
  bell:'<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>',
  eye:'<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z"/><circle cx="12" cy="12" r="3"/></svg>',
};
function iconSvg(name) { return ICONS[name] || ''; }

function goTo(key) {
  document.querySelectorAll('.modal-overlay.open').forEach(m => m.classList.remove('open'));
  CURRENT = key; render(); window.scrollTo(0,0);
}

function renderSidebar() {
  let html = '<div class="sb-logo"><div class="sb-logo-icon">' + iconSvg('building') + '</div><span class="sb-logo-text">BioLearn</span></div><div class="sb-nav">';
  NAV.forEach(item => {
    if (item.group) { html += '<div class="sb-group-title">' + esc(item.group) + '</div>'; return; }
    html += '<button class="sb-item' + (CURRENT===item.key?' activo':'') + '" onclick="goTo(\''+item.key+'\')">' + iconSvg(item.icon) + '<span class="sb-item-label">'+esc(item.label)+'</span></button>';
  });
  html += '</div><div class="sb-user"><div class="sb-avatar" style="background:var(--amber-soft);color:var(--amber)">' + esc(SUPERVISOR.nombre.split(' ').map(p=>p[0]).slice(0,2).join('')) + '</div>' +
    '<div class="sb-user-info"><div class="nombre">' + esc(SUPERVISOR.nombre) + '</div><div class="rol">' + esc(SUPERVISOR.cargo) + '</div></div></div>' +
    '<button class="sb-item" style="margin:0 12px 12px" onclick="confirmarCerrarSesion()">' + iconSvg('logout') + '<span class="sb-item-label">Cerrar sesión</span></button>';
  document.getElementById('sidebar').innerHTML = html;
  document.getElementById('sidebar').classList.toggle('collapsed', SIDEBAR_COLLAPSED);
  const badge = document.getElementById('notif-badge');
  if (badge) { const unread = DATA.notificaciones.filter(n=>!n.leido).length; badge.textContent = unread; badge.style.display = unread>0?'flex':'none'; }
}
function toggleSidebar() { SIDEBAR_COLLAPSED = !SIDEBAR_COLLAPSED; document.getElementById('sidebar').classList.toggle('collapsed', SIDEBAR_COLLAPSED); }
function toggleNotif() { document.getElementById('notif-panel').classList.toggle('abierto'); }
function marcarLeida(id) { const n = DATA.notificaciones.find(x=>x.id===id); if (n) n.leido = true; saveData(); renderTopbar(); }
function irDesdeNotificacion(id, target) { marcarLeida(id); if (target) goTo(target); }

function renderTopbar() {
  const unread = DATA.notificaciones.filter(n=>!n.leido).length;
  let html = '<div class="tb-left"><button class="tb-menu-btn" onclick="toggleSidebar()">☰</button>' +
    '<div class="tb-search"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg><input placeholder="Buscar estudiante..."></div></div>' +
    '<div class="tb-notif-wrap"><button class="tb-notif-btn" onclick="toggleNotif()">' + iconSvg('bell') + '<span class="tb-notif-badge" id="notif-badge" style="display:'+(unread>0?'flex':'none')+'">'+unread+'</span></button>' +
    '<div class="tb-notif-panel" id="notif-panel"><div class="tb-notif-header">Notificaciones</div>';
  if (DATA.notificaciones.length === 0) html += '<div class="empty-state">Sin notificaciones</div>';
  DATA.notificaciones.forEach(n => {
    html += '<div class="tb-notif-item' + (n.leido?'':' sin-leer') + '" onclick="irDesdeNotificacion(\''+n.id+'\',\''+(n.target||'')+'\')">' +
      '<div>' + esc(n.texto) + '</div><div class="fila"><span class="fecha">' + esc(n.fecha) + '</span>' + (n.target?'<span class="ver">Ver →</span>':'') + '</div></div>';
  });
  html += '</div></div>';
  document.getElementById('topbar').innerHTML = html;
}

function confirmarCerrarSesion() {
  confirmarAccion('¿Cerrar sesión? Volverás a la pantalla de inicio de sesión.', function () {
    toast('Sesión cerrada');
    setTimeout(function () {
      document.getElementById('app-content').innerHTML = '<div class="empty-state" style="padding:80px 20px"><h2 style="font-family:\'Fraunces\',serif">Sesión cerrada</h2><p class="muted">En la plataforma real, aquí volverías a la pantalla de inicio de sesión.</p></div>';
    }, 300);
  }, 'Cerrar sesión');
}

/* ---------------------------- router ---------------------------- */
function render() {
  renderSidebar();
  renderTopbar();
  const cont = document.getElementById('app-content');
  const renderers = {
    inicio: renderInicio, cursos: renderCursosInscritos, estudiantes: renderEstudiantes, foros: renderForos,
    reportes: renderReportes, institucion: renderInstitucion, mensajeria: renderMensajeria, perfil: renderPerfil,
  };
  (renderers[CURRENT] || renderInicio)(cont);
}

/* ---------------------------- modal genérico + confirmación ---------------------------- */
function abrirModal(id) { const m = document.getElementById(id); if (m) m.classList.add('open'); }
function cerrarModal(id) { const m = document.getElementById(id); if (m) m.classList.remove('open'); }

let _confirmCallback = null;
function confirmarAccion(mensajeHtml, callback, tituloBoton) {
  renderModalConfirmar();
  document.getElementById('confirm-mensaje').innerHTML = mensajeHtml;
  document.getElementById('confirm-btn').textContent = tituloBoton || 'Confirmar';
  _confirmCallback = callback;
  abrirModal('modal-confirmar');
}
function ejecutarConfirmacion() { const cb = _confirmCallback; cerrarModal('modal-confirmar'); _confirmCallback = null; if (cb) cb(); }
function renderModalConfirmar() {
  if (document.getElementById('modal-confirmar')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-confirmar"><div class="modal" style="max-width:400px">' +
    '<div class="modal-header"><h3>Confirmar</h3><button class="modal-close" onclick="cerrarModal(\'modal-confirmar\')">✕</button></div>' +
    '<div class="modal-body"><p id="confirm-mensaje" style="font-size:14.5px;line-height:1.6;margin:0"></p></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-confirmar\')">Cancelar</button>' +
    '<button class="btn btn-danger" id="confirm-btn" onclick="ejecutarConfirmacion()">Confirmar</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}

document.addEventListener('DOMContentLoaded', function () {
  loadData();
  render();
  document.addEventListener('click', function (e) {
    if (!e.target.closest('.tb-notif-wrap')) { const p = document.getElementById('notif-panel'); if (p) p.classList.remove('abierto'); }
  });
  document.addEventListener('click', function (e) {
    if (e.target.classList && e.target.classList.contains('modal-overlay')) e.target.classList.remove('open');
  });
});
