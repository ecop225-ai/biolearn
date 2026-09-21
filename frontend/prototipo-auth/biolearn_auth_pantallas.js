/* ================================================================
   BIOLearn — biolearn_auth_pantallas.js
================================================================ */

/* ============================== 1. Iniciar sesión ============================== */
function pantallaLogin() {
  const form =
    brandHTML() +
    '<h1 class="auth-title">Bienvenido a<br>BioLearn</h1>' +
    '<div class="campo"><label>Usuario</label>' + campoIconoHTML('login-user', '👤', 'Correo o nombre de usuario') + '</div>' +
    '<div class="campo"><label>Contraseña</label>' + campoPasswordHTML('login-pass') + '</div>' +
    '<button class="btn btn-primary w-full" style="margin-top:6px" onclick="intentarLogin()">Ingresar</button>' +
    '<div style="display:flex;justify-content:center;gap:10px;margin-top:16px;font-size:13px">' +
    '<button class="btn-text" onclick="goTo(\'recover-request\')">Olvidé contraseña</button><span style="color:var(--line)">|</span>' +
    '<button class="btn-text" onclick="goTo(\'signup\')">Crear usuario</button></div>' +
    '<p class="text-sm muted" style="text-align:center;margin-top:26px">Conectado al backend real de BioLearn (Auth)</p>';
  return shellHTML(form, sidePanelHTML('Aprende Biología de forma interactiva y divertida', 'Lecciones, laboratorios y foros en un solo lugar 🌿'));
}
function intentarLogin() {
  const user = document.getElementById('login-user').value.trim();
  const pass = document.getElementById('login-pass').value;
  if (!user) { toast('Escribe tu usuario o correo', true); return; }
  if (!pass) { toast('Escribe tu contraseña', true); return; }
  ejecutarLogin(user, pass);
}
async function ejecutarLogin(user, pass) {
  const btn = document.querySelector('.btn-primary.w-full');
  const textoOriginal = btn ? btn.textContent : '';
  if (btn) { btn.textContent = 'Ingresando...'; btn.disabled = true; }

  try {
    const response = await fetch(API_BASE + '/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usuario: user, contrasena: pass })
    });
    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.mensaje || 'Usuario o contraseña incorrectos.');
    }

    sessionStorage.setItem('biolearn_token', data.token);
    sessionStorage.setItem('biolearn_usuario', JSON.stringify(data.usuario));

    const dashboards = {
      estudiante: '../prototipo-estudiante/biolearn_estudiante_prototipo.html',
      docente: 'biolearn_dashboard_docente.html',
      admin: 'biolearn_dashboard_admin.html',
      supervisor: 'biolearn_dashboard_supervisor.html',
      contador: 'biolearn_dashboard_admin.html'
    };
    const destino = dashboards[data.usuario.rol];
    if (destino) window.location.href = destino;
    else toast('Bienvenido ' + data.usuario.nombre);
  } catch (err) {
    toast(err.message || 'No se pudo iniciar sesión.', true);
  } finally {
    if (btn) { btn.textContent = textoOriginal; btn.disabled = false; }
  }
}

/* ============================== 2. Recuperar / cambiar contraseña ============================== */
function pantallaRecuperar() {
  if (RECOVER_STEP === 'request') return pantallaRecuperarRequest();
  if (RECOVER_STEP === 'sent') return pantallaRecuperarSent();
  if (RECOVER_STEP === 'reset') return pantallaRecuperarReset();
  return pantallaRecuperarDone();
}
function pantallaRecuperarRequest() {
  const form =
    brandHTML() +
    '<button class="btn-text" style="margin-bottom:18px;display:flex;align-items:center;gap:6px" onclick="goTo(\'login\')">← Volver a iniciar sesión</button>' +
    '<h1 class="auth-title-sm">Recuperar contraseña</h1>' +
    '<p class="text-sm muted" style="margin-bottom:22px">Escribe el correo con el que te registraste en BioLearn y te enviaremos un enlace de restablecimiento.</p>' +
    '<div class="campo"><label>Correo electrónico</label>' + campoIconoHTML('rec-email', '✉️', 'tucorreo@ejemplo.com') + '</div>' +
    '<button class="btn btn-primary w-full" id="rec-btn-enviar" disabled onclick="enviarEnlaceRecuperacion()">Enviar enlace</button>';
  return shellHTML(form, sidePanelHTML('Tu cuenta, siempre bajo tu control', 'Restablece tu acceso en un par de pasos, sin complicaciones.'));
}
function actualizarValidacion() {
  if (SCREEN === 'recover-request' && RECOVER_STEP === 'request') {
    const email = document.getElementById('rec-email').value.trim();
    document.getElementById('rec-btn-enviar').disabled = !email.includes('@');
  }
  if (SCREEN === 'recover-request' && RECOVER_STEP === 'reset') actualizarChecklistPassword('rec-pass', 'rec-confirm', 'rec-req-', 'rec-btn-actualizar');
  if (SCREEN === 'signup') actualizarValidacionSignup();
}
window._recEmail = '';
window._recToken = '';
async function enviarEnlaceRecuperacion() {
  window._recEmail = document.getElementById('rec-email').value.trim();
  const btn = document.getElementById('rec-btn-enviar');
  const textoOriginal = btn.textContent;
  btn.textContent = 'Enviando...';
  btn.disabled = true;

  try {
    const response = await fetch(API_BASE + '/api/auth/recuperar-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ correo: window._recEmail })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo procesar la solicitud.');
    RECOVER_STEP = 'sent'; render();
  } catch (err) {
    toast(err.message || 'No se pudo procesar la solicitud.', true);
    btn.textContent = textoOriginal;
    btn.disabled = false;
  }
}
function pantallaRecuperarSent() {
  const form =
    brandHTML() +
    '<div class="icon-circle">✉️</div>' +
    '<h1 class="auth-title-sm">Enlace enviado</h1>' +
    '<p class="text-sm muted" style="margin-bottom:24px;line-height:1.6">Si el correo <strong style="color:var(--ink)">' + esc(window._recEmail || 'ingresado') + '</strong> está registrado, enviamos un enlace con instrucciones para restablecer tu contraseña. El enlace abre esta misma página con el token incluido.</p>' +
    '<div class="campo"><label>Modo de pruebas — pega aquí el token de recuperación</label>' + campoIconoHTML('rec-token-manual', '🔑', 'Token de recuperación') + '</div>' +
    '<p class="hint" style="margin:-8px 0 16px">Mientras el envío real de correo no esté conectado, el token se obtiene manualmente desde la base de datos.</p>' +
    '<button class="btn btn-primary w-full" onclick="continuarConTokenManual()">Continuar</button>' +
    '<div style="text-align:center;margin-top:16px"><button class="btn-text" onclick="RECOVER_STEP=\'request\';render()">¿No llegó? Reenviar correo</button></div>';
  return shellHTML(form, sidePanelHTML('Revisa tu bandeja de entrada', 'El enlace expira en 30 minutos por tu seguridad.'));
}
function continuarConTokenManual() {
  const token = document.getElementById('rec-token-manual').value.trim();
  if (!token) { toast('Pega el token de recuperación para continuar', true); return; }
  window._recToken = token;
  RECOVER_STEP = 'reset'; render();
}
function pantallaRecuperarReset() {
  const form =
    brandHTML() +
    '<h1 class="auth-title-sm">Crear nueva contraseña</h1>' +
    '<p class="text-sm muted" style="margin-bottom:20px">Tu identidad quedó verificada por el enlace de correo.</p>' +
    '<div class="campo"><label>Nueva contraseña</label>' + campoPasswordHTML('rec-pass') + '</div>' +
    '<div class="campo"><label>Confirmar contraseña</label>' + campoPasswordHTML('rec-confirm') + '</div>' +
    '<div class="req-box">' +
    '<div class="req-rule" id="rec-req-len">○ Mínimo 8 caracteres</div>' +
    '<div class="req-rule" id="rec-req-upper">○ Al menos una letra mayúscula</div>' +
    '<div class="req-rule" id="rec-req-num">○ Al menos un número</div>' +
    '<div class="req-rule" id="rec-req-match">○ Las contraseñas coinciden</div></div>' +
    '<button class="btn btn-primary w-full" id="rec-btn-actualizar" disabled onclick="actualizarPasswordReal()">Actualizar contraseña</button>';
  return shellHTML(form, sidePanelHTML('Elige una contraseña segura', 'Usa una combinación única que no repitas en otros sitios.'));
}
async function actualizarPasswordReal() {
  const nuevaContrasena = document.getElementById('rec-pass').value;
  const btn = document.getElementById('rec-btn-actualizar');
  const textoOriginal = btn.textContent;
  btn.textContent = 'Actualizando...';
  btn.disabled = true;

  try {
    const response = await fetch(API_BASE + '/api/auth/restablecer-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: window._recToken, nuevaContrasena: nuevaContrasena })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'El token es inválido o expiró.');
    RECOVER_STEP = 'done'; render();
  } catch (err) {
    toast(err.message || 'No se pudo actualizar la contraseña.', true);
    btn.textContent = textoOriginal;
    btn.disabled = false;
  }
}
function pantallaRecuperarDone() {
  const form =
    brandHTML() +
    '<div class="icon-circle">🛡️</div>' +
    '<h1 class="auth-title-sm">Contraseña actualizada</h1>' +
    '<p class="text-sm muted" style="margin-bottom:24px">Tu contraseña se actualizó correctamente. Ya puedes iniciar sesión con tus nuevos datos.</p>' +
    '<button class="btn btn-primary w-full" onclick="RECOVER_STEP=\'request\';goTo(\'login\')">Ir a iniciar sesión</button>';
  return shellHTML(form, sidePanelHTML('Todo listo', 'Ya puedes ingresar con tu nueva contraseña.'));
}
function actualizarChecklistPassword(passId, confirmId, prefix, btnId) {
  const passEl = document.getElementById(passId), confirmEl = document.getElementById(confirmId);
  if (!passEl) return;
  const pass = passEl.value, confirm = confirmEl.value;
  const rules = { len: pass.length >= 8, upper: /[A-Z]/.test(pass), num: /[0-9]/.test(pass), match: pass.length > 0 && pass === confirm };
  const textos = { len:'Mínimo 8 caracteres', upper:'Al menos una letra mayúscula', num:'Al menos un número', match:'Las contraseñas coinciden' };
  Object.keys(rules).forEach(k => {
    const el = document.getElementById(prefix + k);
    if (!el) return;
    el.classList.toggle('ok', rules[k]);
    el.textContent = (rules[k] ? '✓ ' : '○ ') + textos[k];
  });
  const allOk = rules.len && rules.upper && rules.num && rules.match;
  const btn = document.getElementById(btnId);
  if (btn) btn.disabled = !allOk;
}

/* ============================== 3. Crear cuenta (registro de estudiante) ============================== */
let SIGNUP = { tipo:'independiente', codigo:'', nombre:'', apellido:'', email:'', idGrado:'', idArea:'', acepta:false, leidoTerminos:false, leidoPrivacidad:false };

function pantallaRegistro() {
  if (SIGNUP_STEP === 'done') return pantallaRegistroDone();
  const inst = INSTITUCIONES_DEMO.find(i => i.codigo.toLowerCase() === SIGNUP.codigo.trim().toLowerCase());
  const form =
    brandHTML() +
    '<button class="btn-text" style="margin-bottom:14px;display:flex;align-items:center;gap:6px" onclick="goTo(\'login\')">← Volver a iniciar sesión</button>' +
    '<h1 class="auth-title-sm" style="margin-bottom:6px">Crear cuenta de estudiante</h1>' +
    '<p class="text-sm muted" style="margin-bottom:18px;line-height:1.5">Docentes, contadores y representantes institucionales no se registran aquí: su cuenta la crea el administrador de BioLearn o de su institución, y llega por invitación de correo.</p>' +
    '<div class="tipo-cuenta-row">' +
    '<button type="button" class="tipo-cuenta-btn' + (SIGNUP.tipo==='independiente'?' activo':'') + '" onclick="cambiarTipoSignup(\'independiente\')">🎓<div class="titulo">Estudiante independiente</div><div class="sub">Aprende a tu ritmo, sin institución</div></button>' +
    '<button type="button" class="tipo-cuenta-btn' + (SIGNUP.tipo==='institucion'?' activo':'') + '" onclick="cambiarTipoSignup(\'institucion\')">🏫<div class="titulo">Con mi institución</div><div class="sub">Usa el código que te dio tu colegio</div></button></div>' +
    (SIGNUP.tipo==='institucion' ?
      '<div class="campo"><label>Código de institución</label><input id="signup-codigo" value="'+esc(SIGNUP.codigo)+'" placeholder="Código proporcionado por tu institución" oninput="onCampoInput()">' +
      '<p class="hint' + (SIGNUP.codigo && !inst ? ' error':'') + '">' + (SIGNUP.codigo && !inst ? 'Código no reconocido. Verifica con tu institución.' : 'Ej: SANRAFAEL2026') + '</p></div>' : '') +
    '<div style="display:flex;gap:12px"><div style="flex:1"><div class="campo"><label>Nombre</label>' + campoIconoHTML('signup-nombre', '👤', '', SIGNUP.nombre) + '</div></div>' +
    '<div style="flex:1"><div class="campo"><label>Apellido</label>' + campoIconoHTML('signup-apellido', '👤', '', SIGNUP.apellido) + '</div></div></div>' +
    '<div class="campo"><label>Correo electrónico</label>' + campoIconoHTML('signup-email', '✉️', 'tucorreo@ejemplo.com', SIGNUP.email) + '</div>' +
    '<div style="display:flex;gap:12px"><div style="flex:1"><div class="campo"><label>Grado</label><select id="signup-grado" onchange="onCampoInput()"><option value="">-- Selecciona --</option>' +
    GRADOS_DEMO.map(g=>'<option value="'+g.id+'"'+(String(SIGNUP.idGrado)===String(g.id)?' selected':'')+'>'+esc(g.nombre)+'</option>').join('') + '</select></div></div>' +
    '<div style="flex:1"><div class="campo"><label>Área académica de interés</label><select id="signup-area" onchange="onCampoInput()"><option value="">-- Selecciona --</option>' +
    AREAS_DEMO.map(a=>'<option value="'+a.id+'"'+(String(SIGNUP.idArea)===String(a.id)?' selected':'')+'>'+esc(a.nombre)+'</option>').join('') + '</select></div></div></div>' +
    '<div style="display:flex;gap:12px"><div style="flex:1"><div class="campo"><label>Contraseña</label>' + campoPasswordHTML('signup-pass') + '</div></div>' +
    '<div style="flex:1"><div class="campo"><label>Confirmar</label>' + campoPasswordHTML('signup-confirm') + '</div></div></div>' +
    '<p class="hint error" id="signup-pass-hint" style="display:none;margin:-8px 0 12px">Mínimo 8 caracteres y ambas contraseñas deben coincidir.</p>' +
    '<label class="legal-check"><input type="checkbox" id="signup-acepta" ' + (SIGNUP.acepta?'checked':'') + ' onchange="SIGNUP.acepta=this.checked;actualizarValidacionSignup()">' +
    '<span>Acepto los <button type="button" class="legal-link" onclick="abrirModalLegal(\'terminos\')">términos de uso</button> y la ' +
    '<button type="button" class="legal-link" onclick="abrirModalLegal(\'privacidad\')">política de tratamiento de datos</button> de BioLearn.</span></label>' +
    '<div class="legal-status-row">' +
    '<span class="legal-status' + (SIGNUP.leidoTerminos?' leido':'') + '" id="signup-status-terminos">' + (SIGNUP.leidoTerminos?'✓':'○') + ' Términos ' + (SIGNUP.leidoTerminos?'leídos':'sin leer') + '</span>' +
    '<span class="legal-status' + (SIGNUP.leidoPrivacidad?' leido':'') + '" id="signup-status-privacidad">' + (SIGNUP.leidoPrivacidad?'✓':'○') + ' Política de datos ' + (SIGNUP.leidoPrivacidad?'leída':'sin leer') + '</span></div>' +
    '<button class="btn btn-primary w-full" id="signup-btn-crear" onclick="crearCuenta()">Crear cuenta</button>' +
    '<p class="text-sm muted" id="signup-faltantes" style="text-align:center;margin-top:10px;line-height:1.5"></p>';
  return shellHTML(form, sidePanelHTML('Aprende a tu ritmo o con tu institución', 'Elige cómo quieres comenzar tu camino en BioLearn.'));
}
function cambiarTipoSignup(tipo) { SIGNUP.tipo = tipo; SIGNUP.codigo = ''; render(); }
function marcarTerminosLeidos() { SIGNUP.leidoTerminos = true; actualizarEstadoLegalDom(); }
function marcarPrivacidadLeida() { SIGNUP.leidoPrivacidad = true; actualizarEstadoLegalDom(); }
function actualizarEstadoLegalDom() {
  const t = document.getElementById('signup-status-terminos'), p = document.getElementById('signup-status-privacidad');
  if (t) { t.classList.toggle('leido', SIGNUP.leidoTerminos); t.textContent = (SIGNUP.leidoTerminos?'✓':'○') + ' Términos ' + (SIGNUP.leidoTerminos?'leídos':'sin leer'); }
  if (p) { p.classList.toggle('leido', SIGNUP.leidoPrivacidad); p.textContent = (SIGNUP.leidoPrivacidad?'✓':'○') + ' Política de datos ' + (SIGNUP.leidoPrivacidad?'leída':'sin leer'); }
}
function actualizarValidacionSignup() {
  const nombre = (document.getElementById('signup-nombre')||{}).value || '';
  const apellido = (document.getElementById('signup-apellido')||{}).value || '';
  const email = (document.getElementById('signup-email')||{}).value || '';
  const idGrado = (document.getElementById('signup-grado')||{}).value || '';
  const idArea = (document.getElementById('signup-area')||{}).value || '';
  const pass = (document.getElementById('signup-pass')||{}).value || '';
  const confirm = (document.getElementById('signup-confirm')||{}).value || '';
  const codigoEl = document.getElementById('signup-codigo');
  if (codigoEl) SIGNUP.codigo = codigoEl.value;
  SIGNUP.nombre = nombre; SIGNUP.apellido = apellido; SIGNUP.email = email; SIGNUP.idGrado = idGrado; SIGNUP.idArea = idArea;
  const inst = INSTITUCIONES_DEMO.find(i => i.codigo.toLowerCase() === SIGNUP.codigo.trim().toLowerCase());
  const codigoValido = SIGNUP.tipo === 'independiente' || Boolean(inst);
  const passwordOk = pass.length >= 8 && pass === confirm;

  const passHint = document.getElementById('signup-pass-hint');
  if (passHint) passHint.style.display = (pass && confirm && !passwordOk) ? 'block' : 'none';
  if (codigoEl) {
    const hint = codigoEl.parentElement.querySelector('.hint');
    if (hint) { hint.classList.toggle('error', !!(SIGNUP.codigo && !inst)); hint.textContent = SIGNUP.codigo && !inst ? 'Código no reconocido. Verifica con tu institución.' : 'Ej: SANRAFAEL2026'; }
  }

  const faltantes = [];
  if (!nombre.trim()) faltantes.push('tu nombre');
  if (!apellido.trim()) faltantes.push('tu apellido');
  if (!email.includes('@') || !email.includes('.')) faltantes.push('un correo válido');
  if (!idGrado) faltantes.push('tu grado');
  if (!idArea) faltantes.push('un área académica de interés');
  if (!passwordOk) faltantes.push(pass.length < 8 ? 'una contraseña de mínimo 8 caracteres' : 'que ambas contraseñas coincidan');
  if (!codigoValido) faltantes.push('un código de institución válido');
  if (!SIGNUP.acepta) faltantes.push('aceptar los términos');
  const puedeCrear = faltantes.length === 0;

  const btn = document.getElementById('signup-btn-crear'); if (btn) btn.disabled = !puedeCrear;
  const msg = document.getElementById('signup-faltantes'); if (msg) msg.textContent = puedeCrear ? '' : ('Falta: ' + faltantes.join(', ') + '.');
}
// El backend exige un nombreUsuario (4-60 caracteres) que este diseño no pide
// explícitamente en el formulario. Se deriva del correo de forma automática;
// el estudiante igual puede iniciar sesión con su correo (el login acepta ambos).
function generarNombreUsuario(email) {
  let base = (email.split('@')[0] || '').toLowerCase().replace(/[^a-z0-9._-]/g, '');
  if (base.length < 4) base = base + Math.floor(1000 + Math.random() * 9000);
  return base.slice(0, 60);
}

async function crearCuenta() {
  actualizarValidacionSignup();
  if (document.getElementById('signup-btn-crear').disabled) return;

  const inst = INSTITUCIONES_DEMO.find(i => i.codigo.toLowerCase() === SIGNUP.codigo.trim().toLowerCase());
  const payload = {
    nombre: SIGNUP.nombre.trim(),
    apellido: SIGNUP.apellido.trim(),
    nombreUsuario: generarNombreUsuario(SIGNUP.email.trim()),
    correo: SIGNUP.email.trim(),
    contrasena: document.getElementById('signup-pass').value,
    idGrado: parseInt(SIGNUP.idGrado, 10),
    idArea: parseInt(SIGNUP.idArea, 10),
    codigoInstitucion: SIGNUP.tipo === 'institucion' ? SIGNUP.codigo.trim() : ''
  };

  const btn = document.getElementById('signup-btn-crear');
  const textoOriginal = btn.textContent;
  btn.textContent = 'Creando cuenta...';
  btn.disabled = true;

  try {
    const response = await fetch(API_BASE + '/api/auth/registro', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo crear la cuenta.');

    window._signupInst = inst;
    window._signupEmail = SIGNUP.email;
    SIGNUP_STEP = 'done'; render();
  } catch (err) {
    toast(err.message || 'No se pudo crear la cuenta.', true);
    btn.textContent = textoOriginal;
    btn.disabled = false;
  }
}
function pantallaRegistroDone() {
  const inst = window._signupInst;
  const form =
    brandHTML() +
    '<div class="icon-circle">✓</div>' +
    '<h1 class="auth-title-sm">Cuenta creada</h1>' +
    '<p class="text-sm muted" style="margin-bottom:10px;line-height:1.6">Enviamos un correo de confirmación a <strong style="color:var(--ink)">' + esc(window._signupEmail) + '</strong>.' +
    (inst ? ' Tu cuenta quedó afiliada a <strong style="color:var(--ink)">' + esc(inst.nombre) + '</strong>.' : '') + '</p>' +
    '<button class="btn btn-primary w-full" style="margin-top:8px" onclick="SIGNUP_STEP=\'form\';SIGNUP={tipo:\'independiente\',codigo:\'\',nombre:\'\',apellido:\'\',email:\'\',idGrado:\'\',idArea:\'\',acepta:false,leidoTerminos:false,leidoPrivacidad:false};goTo(\'login\')">Ir a iniciar sesión</button>';
  return shellHTML(form, sidePanelHTML('¡Bienvenido a BioLearn!', 'Ya puedes explorar tus primeros cursos de biología.'));
}
