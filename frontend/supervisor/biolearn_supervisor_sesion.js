/* ================================================================
   BIOLearn — biolearn_supervisor_sesion.js
   Sesión real del representante institucional (DEF-040).
   El portal del representante todavía no está conectado al backend: sus secciones usan datos de demostración.
   Mientras se desarrolla, este archivo:
     - toma el nombre y el correo del usuario que inició sesión (antes saludaba siempre a «Mónica Salazar»);
     - exige una sesión válida con rol supervisor (como los demás portales);
     - cierra la sesión de verdad y vuelve al inicio de sesión;
     - muestra un Inicio «en construcción» y un aviso de datos de demostración en las demás secciones.
   Se carga después de los demás archivos del portal.
================================================================ */
const URL_LOGIN_SUP = '../prototipo-auth/biolearn_auth_prototipo.html';

(function () {
  try {
    const u = JSON.parse(sessionStorage.getItem('biolearn_usuario') || 'null');
    if (u) {
      SUPERVISOR.nombre = ((u.nombre || '') + ' ' + (u.apellido || '')).trim() || 'Representante';
      SUPERVISOR.cargo = 'Representante institucional';
      SUPERVISOR.email = u.correo || '';
    }
  } catch (e) { /* sin sesión: la verificación de abajo redirige al inicio de sesión */ }
})();

document.addEventListener('DOMContentLoaded', function () {
  let u = null;
  try { u = JSON.parse(sessionStorage.getItem('biolearn_usuario') || 'null'); } catch (e) { u = null; }
  if (!sessionStorage.getItem('biolearn_token') || !u) { window.location.href = URL_LOGIN_SUP; return; }
  if (u.rol !== 'supervisor') {
    window._supSinAcceso = true;
    document.body.innerHTML = '<div style="max-width:480px;margin:12vh auto;padding:32px;background:#fff;border-radius:16px;box-shadow:0 8px 24px rgba(27,50,44,.15);text-align:center">' +
      '<div style="font-size:40px">🔒</div><h2 style="margin:12px 0 8px">Acceso solo para representantes institucionales</h2>' +
      '<p style="color:#555">Tu cuenta (' + esc(u.nombre || '') + ', rol ' + esc(u.rol || '') + ') no tiene acceso a este portal.</p></div>';
    setTimeout(function () { window.location.href = URL_LOGIN_SUP; }, 2500);
  }
}, true);   // se ejecuta antes del arranque del prototipo

function confirmarCerrarSesion() {
  confirmarAccion('¿Cerrar sesión? Volverás a la pantalla de inicio de sesión.', function () {
    sessionStorage.removeItem('biolearn_token');
    sessionStorage.removeItem('biolearn_usuario');
    toast('Sesión cerrada');
    setTimeout(function () { window.location.href = URL_LOGIN_SUP; }, 500);
  }, 'Cerrar sesión');
}

const AVISO_DEMO_SUP = '<div style="background:var(--amber-soft,#FBF1DE);border:1px solid #E8C88A;color:#6B4A12;border-radius:12px;padding:12px 16px;margin-bottom:18px;font-size:13.5px">' +
  '🚧 <strong>Sección en construcción.</strong> Lo que ves aquí son datos de demostración; todavía no corresponden a tu institución.</div>';

function renderInicio(cont) {
  const h = new Date().getHours();
  const saludo = h < 12 ? 'Buenos días' : h < 19 ? 'Buenas tardes' : 'Buenas noches';
  let html = '<div style="background:linear-gradient(120deg,var(--blue-deep),var(--blue));border-radius:16px;padding:22px 28px;margin-bottom:24px;color:#fff">' +
    '<div style="font-family:\'Fraunces\',serif;font-size:21px">¡' + saludo + ', ' + esc(SUPERVISOR.nombre.split(' ')[0]) + '! 🏫</div>' +
    '<div style="font-size:13.5px;opacity:.9">Portal del representante institucional</div></div>';
  html += '<div class="card" style="padding:28px;text-align:center;margin-bottom:24px">' +
    '<div style="font-size:42px;margin-bottom:10px">🚧</div>' +
    '<h3 style="font-family:\'Fraunces\',serif;font-size:20px;margin:0 0 10px">Este portal está en construcción</h3>' +
    '<p class="muted" style="max-width:560px;margin:0 auto 6px;line-height:1.6">Muy pronto podrás consultar aquí los estudiantes de tu institución, su avance en los cursos, ' +
    'sus calificaciones y los reportes institucionales.</p>' +
    '<p class="muted" style="max-width:560px;margin:0 auto;line-height:1.6">Mientras tanto, las demás secciones muestran <strong>datos de demostración</strong> para que conozcas cómo funcionará.</p></div>';
  html += '<div class="text-sm" style="font-weight:800;letter-spacing:.5px;text-transform:uppercase;color:var(--ink-soft);margin-bottom:12px">Vista previa de las secciones</div>';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:14px">';
  ACCESO_RAPIDO_SUP.forEach(function (a) {
    html += '<div onclick="goTo(\'' + a[0] + '\')" class="card" style="text-align:center;cursor:pointer;padding:22px 12px"><div style="font-size:26px;margin-bottom:8px">' + a[2] + '</div><div style="font-weight:700;font-size:13px">' + a[1] + '</div></div>';
  });
  html += '</div>';
  cont.innerHTML = html;
}

/* Aviso de demostración encima de cada sección distinta del Inicio */
const _renderSupOriginal = render;
render = function () {
  if (window._supSinAcceso) return;
  _renderSupOriginal();
  const cont = document.getElementById('app-content');
  if (cont && CURRENT !== 'inicio' && CURRENT !== 'perfil') cont.insertAdjacentHTML('afterbegin', AVISO_DEMO_SUP);
};
