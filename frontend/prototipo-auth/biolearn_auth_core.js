/* ================================================================
   BIOLearn — biolearn_auth_core.js
   Utilidades compartidas: marca, panel lateral, modal legal, router simple
================================================================ */
/* URL del backend local — requiere que BiolearnBackendApplication esté corriendo en IntelliJ */
const API_BASE = 'http://localhost:8080';

function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
function uid(p) { return p + '-' + Math.random().toString(36).slice(2, 8); }

function toast(text, esError) {
  const el = document.createElement('div');
  el.className = 'toast-item';
  el.innerHTML = (esError ? '⚠️ ' : '✅ ') + esc(text);
  document.getElementById('toast-wrap').appendChild(el);
  setTimeout(() => el.remove(), 3200);
}

/* El código de institución ya no se valida contra una lista local: se consulta al backend
   (GET /api/auth/validar-codigo-institucion). Los códigos tienen 8 caracteres, ej. SR7K2MQX
   (ver backend/sql/datos_iniciales.sql). */
const LONGITUD_CODIGO_INSTITUCION = 8;

/* Catálogos de Grado y Área académica: se consultan al backend (endpoints públicos, solo activos).
   GET /api/catalogos/grados y GET /api/catalogos/areas — así el registro siempre muestra lo que el
   Administrador tiene habilitado en Catálogos, sin listas fijas en el código. */
let CATALOGOS = { grados: [], areas: [], estado: 'cargando' }; // estado: cargando | ok | error

async function cargarCatalogosRegistro() {
  try {
    const [rg, ra] = await Promise.all([
      fetch(API_BASE + '/api/catalogos/grados'),
      fetch(API_BASE + '/api/catalogos/areas')
    ]);
    if (!rg.ok || !ra.ok) throw new Error('error ' + (rg.ok ? ra.status : rg.status));
    CATALOGOS = { grados: await rg.json(), areas: await ra.json(), estado: 'ok' };
  } catch (err) {
    CATALOGOS = { grados: [], areas: [], estado: 'error' };
  }
  if (typeof SCREEN !== 'undefined' && SCREEN === 'signup' && typeof render === 'function') render();
}

const TERMINOS_TEXTO = [
  { titulo:'1. Aceptación de los términos', texto:'Al crear una cuenta en BioLearn aceptas estos términos de uso. Si estás creando la cuenta de un menor de edad o eres menor de edad, esta cuenta debe contar con la autorización de tu acudiente.' },
  { titulo:'2. Qué es BioLearn', texto:'BioLearn es una plataforma educativa de biología que ofrece lecturas, laboratorios virtuales, artículos científicos, juegos, foros y eventos en línea, dictados por docentes propios o vinculados a instituciones aliadas.' },
  { titulo:'3. Tu cuenta', texto:'Eres responsable de mantener la confidencialidad de tu contraseña y de la información que registras. Debes notificarnos si detectas un uso no autorizado de tu cuenta.' },
  { titulo:'4. Uso aceptable', texto:'No está permitido compartir tus credenciales, copiar o redistribuir el material de los cursos, ni usar los foros o el chat para acoso, discriminación o contenido inapropiado.' },
  { titulo:'5. Cursos, pagos y reembolsos', texto:'Algunos cursos tienen costo. Los pagos, facturas y solicitudes de reembolso se gestionan desde la plataforma y están sujetos a la política de reembolsos vigente en el momento de la compra.' },
  { titulo:'6. Contenido de terceros', texto:'Los laboratorios virtuales y juegos educativos pueden alojarse en plataformas externas. BioLearn no es responsable por los términos propios de esos proveedores.' },
  { titulo:'7. Suspensión de cuentas', texto:'BioLearn puede suspender o cancelar cuentas que incumplan estos términos, sin perjuicio del derecho a solicitar la eliminación de tus datos.' },
  { titulo:'8. Cambios en los términos', texto:'Podemos actualizar estos términos; te avisaremos por correo o dentro de la plataforma antes de que entren en vigor los cambios importantes.' },
  { titulo:'9. Legislación aplicable', texto:'Estos términos se rigen por la legislación colombiana. Cualquier controversia se resolverá ante los jueces competentes de Colombia.' },
];
const PRIVACIDAD_TEXTO = [
  { titulo:'1. Responsable del tratamiento', texto:'BioLearn S.A.S. es responsable del tratamiento de los datos personales que recopila a través de la plataforma, conforme a la Ley 1581 de 2012 y el Decreto 1377 de 2013 de Colombia.' },
  { titulo:'2. Datos que recopilamos', texto:'Nombre, correo electrónico, institución (si aplica), progreso académico, calificaciones, asistencia y, cuando corresponde, información de pago para procesar inscripciones a cursos.' },
  { titulo:'3. Datos de menores de edad', texto:'Cuando el titular sea menor de edad, el tratamiento de sus datos requiere la autorización de sus padres o acudientes, quienes podrán ejercer en su nombre los derechos de acceso, corrección y eliminación.' },
  { titulo:'4. Finalidad', texto:'Usamos tus datos para darte acceso a los cursos, generar certificados, calcular reportes académicos y financieros, enviarte notificaciones y mensajes relacionados con tu aprendizaje.' },
  { titulo:'5. Con quién compartimos datos', texto:'Tu institución (si estás afiliado a una) puede ver tu progreso, asistencia y certificados. No vendemos tus datos a terceros con fines publicitarios.' },
  { titulo:'6. Tus derechos (Habeas Data)', texto:'Puedes conocer, actualizar, rectificar o solicitar la eliminación de tus datos personales escribiendo a soporte@biolearn.co en cualquier momento.' },
  { titulo:'7. Seguridad', texto:'Aplicamos medidas técnicas y organizativas razonables para proteger tu información contra accesos no autorizados.' },
  { titulo:'8. Contacto', texto:'Para preguntas sobre el tratamiento de tus datos personales puedes escribirnos a soporte@biolearn.co.' },
];

/* ---------------------------- bloques visuales compartidos ---------------------------- */
function brandHTML() {
  return '<div class="auth-brand"><div class="auth-brand-icon">🌿</div><span class="auth-brand-name">BioLearn</span></div>';
}
function sidePanelHTML(heading, sub) {
  return '<div class="auth-side"><div class="auth-side-leaves">' +
    '<div class="auth-side-leaf">🌿</div><div class="auth-side-leaf">🌿</div><div class="auth-side-leaf">🌿</div></div>' +
    '<h2>' + esc(heading) + '</h2><p>' + esc(sub) + '</p></div>';
}
function shellHTML(formHtml, sideHtml) {
  return '<div class="auth-shell"><div class="auth-form">' + formHtml + '</div>' + sideHtml + '</div>';
}
function campoIconoHTML(id, icon, placeholder, value, type) {
  return '<div class="campo-icon-wrap"><input type="'+(type||'text')+'" id="'+id+'" value="'+esc(value||'')+'" placeholder="'+esc(placeholder||'')+'" oninput="onCampoInput()"><span class="campo-icon">'+icon+'</span></div>';
}
function campoPasswordHTML(id) {
  return '<div class="campo-icon-wrap"><input type="password" id="'+id+'" placeholder="••••••••" oninput="onCampoInput()"><span class="campo-icon">🔒</span>' +
    '<button type="button" class="campo-pass-toggle" onclick="togglePassword(\''+id+'\', this)">👁</button></div>';
}
function togglePassword(id, btn) {
  const input = document.getElementById(id);
  const showing = input.type === 'text';
  input.type = showing ? 'password' : 'text';
  btn.textContent = showing ? '👁' : '🙈';
}

/* ---------------------------- Modal legal ---------------------------- */
let LEGAL_ON_ACCEPT = null;
function abrirModalLegal(tipo) {
  const esTerminos = tipo === 'terminos';
  const secciones = esTerminos ? TERMINOS_TEXTO : PRIVACIDAD_TEXTO;
  document.getElementById('legal-titulo').innerHTML = (esTerminos ? '📄 ' : '📜 ') + (esTerminos ? 'Términos de uso de BioLearn' : 'Política de tratamiento de datos');
  document.getElementById('legal-cuerpo').innerHTML = '<p class="disclaimer">Contenido de referencia para este prototipo — no reemplaza el documento legal definitivo de BioLearn.</p>' +
    secciones.map(s => '<div class="modal-section"><h4>' + esc(s.titulo) + '</h4><p>' + esc(s.texto) + '</p></div>').join('');
  LEGAL_ON_ACCEPT = esTerminos ? 'terminos' : 'privacidad';
  document.getElementById('modal-legal').classList.add('open');
}
function cerrarModalLegal() { document.getElementById('modal-legal').classList.remove('open'); }
function aceptarModalLegal() {
  if (LEGAL_ON_ACCEPT === 'terminos' && typeof marcarTerminosLeidos === 'function') marcarTerminosLeidos();
  if (LEGAL_ON_ACCEPT === 'privacidad' && typeof marcarPrivacidadLeida === 'function') marcarPrivacidadLeida();
  cerrarModalLegal();
}
function renderModalLegal() {
  if (document.getElementById('modal-legal')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-legal"><div class="modal"><div class="modal-header"><h3 id="legal-titulo"></h3><button class="modal-close" onclick="cerrarModalLegal()">✕</button></div>' +
    '<div class="modal-body" id="legal-cuerpo"></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModalLegal()">Cerrar</button><button class="btn btn-primary" onclick="aceptarModalLegal()">He leído y acepto</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
document.addEventListener('click', function (e) {
  if (e.target.classList && e.target.classList.contains('modal-overlay')) e.target.classList.remove('open');
});

/* ---------------------------- router simple ---------------------------- */
let SCREEN = 'login';
let RECOVER_STEP = 'request';
let SIGNUP_STEP = 'form';

function goTo(screen) {
  SCREEN = screen;
  if (screen === 'recover-request') RECOVER_STEP = 'request';
  render();
}
function render() {
  renderModalLegal();
  const root = document.getElementById('app-root');
  if (SCREEN === 'login') root.innerHTML = pantallaLogin();
  else if (SCREEN === 'recover-request') root.innerHTML = pantallaRecuperar();
  else if (SCREEN === 'signup') { root.innerHTML = pantallaRegistro(); if (SIGNUP_STEP === 'form') actualizarValidacionSignup(); }
  else if (SCREEN === 'activar') { root.innerHTML = pantallaActivar(); actualizarValidacion(); }
  window.scrollTo(0,0);
}
function onCampoInput() { if (typeof actualizarValidacion === 'function') actualizarValidacion(); }

/* Enlaces de correo (cuando se integre el envío real):
   ...auth_prototipo.html?verificar=TOKEN   → verifica la cuenta de estudiante automáticamente
   ...auth_prototipo.html?invitacion=TOKEN  → abre la activación de personal con el token cargado */
document.addEventListener('DOMContentLoaded', function () {
  cargarCatalogosRegistro();
  const params = new URLSearchParams(window.location.search);
  if (params.get('verificar')) {
    SCREEN = 'activar'; ACTIVAR.modo = 'estudiante'; ACTIVAR.token = params.get('verificar');
    render(); verificarCuentaEstudiante();
    return;
  }
  if (params.get('invitacion')) {
    SCREEN = 'activar'; ACTIVAR.modo = 'invitacion'; ACTIVAR.token = params.get('invitacion');
  }
  render();
});
