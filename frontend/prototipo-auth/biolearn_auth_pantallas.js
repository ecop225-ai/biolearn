/* ================================================================
   BIOLearn — biolearn_auth_pantallas.js
================================================================ */

/* ============================== 1. Iniciar sesión ============================== */
function pantallaLogin() {
  const form =
    brandHTML() +
    '<h1 class="auth-title">Bienvenido a<br>BioLearn</h1>' +
    '<div class="campo"><label for="login-user">Usuario</label>' + campoIconoHTML('login-user', '👤', 'Correo o nombre de usuario') + '</div>' +
    '<div class="campo"><label for="login-pass">Contraseña</label>' + campoPasswordHTML('login-pass') + '</div>' +
    '<button class="btn btn-primary w-full" style="margin-top:6px" onclick="intentarLogin()">Ingresar</button>' +
    '<div style="display:flex;justify-content:center;gap:10px;margin-top:16px;font-size:13px">' +
    '<button class="btn-text" onclick="goTo(\'recover-request\')">Olvidé contraseña</button><span style="color:var(--line)">|</span>' +
    '<button class="btn-text" onclick="goTo(\'signup\')">Crear usuario</button><span style="color:var(--line)">|</span>' +
    '<button class="btn-text" onclick="goTo(\'activar\')">Activar cuenta</button></div>' +
    '<div id="login-aviso"></div>' +
    '<p class="text-sm muted" style="text-align:center;margin-top:26px">' + (API_BASE.includes('localhost:8080') ? 'Conectado a tu backend local (localhost:8080) · usa un usuario real ya registrado' : 'Conectado al servidor de BioLearn · usa un usuario real ya registrado') + '</p>';
  return shellHTML(form, sidePanelHTML('Aprende Biología de forma interactiva y divertida', 'Lecciones, laboratorios y foros en un solo lugar 🌿'));
}
async function intentarLogin() {
  const user = document.getElementById('login-user').value.trim();
  const pass = document.getElementById('login-pass').value;
  if (!user) { toast('Escribe tu usuario o correo', true); return; }
  if (!pass) { toast('Escribe tu contraseña', true); return; }

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
    if (!response.ok && data.codigo === 'CUENTA_NO_VERIFICADA') {
      // Cuenta pendiente: en vez de solo un error, se ofrece ir directo a activarla.
      document.getElementById('login-aviso').innerHTML =
        '<div class="req-box" style="margin-top:16px;text-align:center"><p class="text-sm" style="margin:0 0 10px">' + esc(data.mensaje) + '</p>' +
        '<button class="btn btn-primary w-full" onclick="ACTIVAR.correo=\'' + esc(user.includes('@') ? user : '') + '\';goTo(\'activar\')">Activar mi cuenta</button></div>';
      return;
    }
    if (!response.ok) throw new Error(data.mensaje || 'Usuario o contraseña incorrectos.');

    sessionStorage.setItem('biolearn_token', data.token);
    sessionStorage.setItem('biolearn_usuario', JSON.stringify(data.usuario));
    toast('Sesión iniciada como ' + data.usuario.nombre + ' (' + data.usuario.rol + ')');

    // Redirige al prototipo correspondiente según el rol. Ajusta estas rutas si el nombre
    // de alguna carpeta/archivo en tu proyecto real es distinto.
    const DASHBOARDS = {
      estudiante: '../prototipo-estudiante/biolearn_estudiante_prototipo.html',
      docente: '../docente/biolearn_docente_prototipo.html',
      admin: '../administrador/biolearn_admin_prototipo.html',
      supervisor: '../supervisor/biolearn_supervisor_prototipo.html',
      contador: '../contador/biolearn_contador_prototipo.html',
    };
    const destino = DASHBOARDS[data.usuario.rol];
    if (destino) {
      setTimeout(() => { window.location.href = destino; }, 900); // deja ver el toast antes de navegar
    } else {
      toast('No hay un prototipo configurado para el rol "' + data.usuario.rol + '"', true);
    }
  } catch (err) {
    toast(err.message || 'No se pudo conectar con el servidor (' + API_BASE + '). ¿Está encendido?', true);
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
    '<div class="campo"><label for="rec-email">Correo electrónico</label>' + campoIconoHTML('rec-email', '✉️', 'tucorreo@ejemplo.com') + '</div>' +
    '<button class="btn btn-primary w-full" id="rec-btn-enviar" disabled onclick="enviarEnlaceRecuperacion()">Enviar enlace</button>';
  return shellHTML(form, sidePanelHTML('Tu cuenta, siempre bajo tu control', 'Restablece tu acceso en un par de pasos, sin complicaciones.'));
}
function actualizarValidacion() {
  if (SCREEN === 'recover-request' && RECOVER_STEP === 'request') {
    const email = document.getElementById('rec-email').value.trim();
    document.getElementById('rec-btn-enviar').disabled = !email.includes('@');
  }
  if (SCREEN === 'recover-request' && RECOVER_STEP === 'reset') {
    actualizarChecklistPassword('rec-pass', 'rec-confirm', 'rec-req-', 'rec-btn-actualizar');
    const tokenEl = document.getElementById('rec-token');
    const btn = document.getElementById('rec-btn-actualizar');
    if (tokenEl && btn && !tokenEl.value.trim()) btn.disabled = true;
  }
  if (SCREEN === 'signup') actualizarValidacionSignup();
  if (SCREEN === 'activar' && !ACTIVAR.hecho) validarActivar();
}
window._recEmail = '';
async function enviarEnlaceRecuperacion() {
  window._recEmail = document.getElementById('rec-email').value.trim();
  const btn = document.getElementById('rec-btn-enviar');
  if (btn) { btn.disabled = true; btn.textContent = 'Enviando...'; }
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
    toast(err.message || 'No se pudo conectar con el servidor.', true);
    if (btn) { btn.disabled = false; btn.textContent = 'Enviar enlace'; }
  }
}
function pantallaRecuperarSent() {
  const form =
    brandHTML() +
    '<div class="icon-circle">✉️</div>' +
    '<h1 class="auth-title-sm">Solicitud enviada</h1>' +
    '<p class="text-sm muted" style="margin-bottom:24px;line-height:1.6">Si <strong style="color:var(--ink)">' + esc(window._recEmail || 'ese correo') + '</strong> está registrado, te enviamos un correo con el enlace para crear una contraseña nueva. Abre el enlace; si no te llega, revisa la carpeta de spam o usa el token de la consola del backend ("Token de recuperación...") en el siguiente paso.</p>' +
    '<button class="btn btn-primary w-full" onclick="RECOVER_STEP=\'reset\';render()">Ya tengo el token →</button>' +
    '<div style="text-align:center;margin-top:16px"><button class="btn-text" onclick="RECOVER_STEP=\'request\';render()">Solicitar de nuevo</button></div>';
  return shellHTML(form, sidePanelHTML('Revisa tu correo', 'El enlace expira en 30 minutos por tu seguridad.'));
}
function pantallaRecuperarReset() {
  const form =
    brandHTML() +
    '<h1 class="auth-title-sm">Crear nueva contraseña</h1>' +
    '<p class="text-sm muted" style="margin-bottom:20px">Si abriste el enlace del correo, el token ya está cargado. Si no, pégalo desde el correo o desde la consola del backend.</p>' +
    '<div class="campo"><label for="rec-token">Token de recuperación</label>' + campoIconoHTML('rec-token', '🔑', 'Pega aquí el token') + '</div>' +
    '<div class="campo"><label for="rec-pass">Nueva contraseña</label>' + campoPasswordHTML('rec-pass') + '</div>' +
    '<div class="campo"><label for="rec-confirm">Confirmar contraseña</label>' + campoPasswordHTML('rec-confirm') + '</div>' +
    '<div class="req-box">' +
    '<div class="req-rule" id="rec-req-len">○ Mínimo 8 caracteres</div>' +
    '<div class="req-rule" id="rec-req-upper">○ Al menos una letra mayúscula</div>' +
    '<div class="req-rule" id="rec-req-num">○ Al menos un número</div>' +
    '<div class="req-rule" id="rec-req-match">○ Las contraseñas coinciden</div></div>' +
    '<button class="btn btn-primary w-full" id="rec-btn-actualizar" disabled onclick="confirmarRestablecerPassword()">Actualizar contraseña</button>';
  return shellHTML(form, sidePanelHTML('Elige una contraseña segura', 'Usa una combinación única que no repitas en otros sitios.'));
}
async function confirmarRestablecerPassword() {
  const token = document.getElementById('rec-token').value.trim();
  const pass = document.getElementById('rec-pass').value;
  if (!token) { toast('Pega el token de recuperación', true); return; }

  const btn = document.getElementById('rec-btn-actualizar');
  if (btn) { btn.disabled = true; btn.textContent = 'Actualizando...'; }
  try {
    const response = await fetch(API_BASE + '/api/auth/restablecer-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, nuevaContrasena: pass })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo actualizar la contraseña.');
    RECOVER_STEP = 'done'; render();
  } catch (err) {
    toast(err.message || 'No se pudo conectar con el servidor.', true);
    if (btn) { btn.disabled = false; btn.textContent = 'Actualizar contraseña'; }
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
/* Resultado de validar el código contra el backend: estado = '' | 'validando' | 'ok' | 'error' */
let SIGNUP_COD = { codigo:'', estado:'', nombre:'', mensaje:'' };
let _timerCodigo = null;

function hintCodigoTexto() {
  if (SIGNUP_COD.estado === 'validando') return 'Validando código...';
  if (SIGNUP_COD.estado === 'ok') return '✓ ' + SIGNUP_COD.nombre;
  if (SIGNUP_COD.estado === 'error') return SIGNUP_COD.mensaje;
  return 'Código de ' + LONGITUD_CODIGO_INSTITUCION + ' caracteres, ej: SR7K2MQX';
}
function pantallaRegistro() {
  if (SIGNUP_STEP === 'done') return pantallaRegistroDone();
  const form =
    brandHTML() +
    '<button class="btn-text" style="margin-bottom:14px;display:flex;align-items:center;gap:6px" onclick="goTo(\'login\')">← Volver a iniciar sesión</button>' +
    '<h1 class="auth-title-sm" style="margin-bottom:6px">Crear cuenta de estudiante</h1>' +
    '<p class="text-sm muted" style="margin-bottom:18px;line-height:1.5">Docentes, contadores y representantes institucionales no se registran aquí: su cuenta la crea el administrador de BioLearn y llega por invitación de correo.</p>' +
    '<div class="tipo-cuenta-row">' +
    '<button type="button" class="tipo-cuenta-btn' + (SIGNUP.tipo==='independiente'?' activo':'') + '" onclick="cambiarTipoSignup(\'independiente\')">🎓<div class="titulo">Estudiante independiente</div><div class="sub">Aprende a tu ritmo, sin institución</div></button>' +
    '<button type="button" class="tipo-cuenta-btn' + (SIGNUP.tipo==='institucion'?' activo':'') + '" onclick="cambiarTipoSignup(\'institucion\')">🏫<div class="titulo">Con mi institución</div><div class="sub">Usa el código que te dio tu colegio</div></button></div>' +
    (SIGNUP.tipo==='institucion' ?
      '<div class="campo"><label for="signup-codigo">Código de institución</label><input id="signup-codigo" maxlength="' + LONGITUD_CODIGO_INSTITUCION + '" style="text-transform:uppercase;font-family:monospace;letter-spacing:2px" value="'+esc(SIGNUP.codigo)+'" placeholder="Código proporcionado por tu institución" oninput="onCampoInput()">' +
      '<p class="hint' + (SIGNUP_COD.estado==='error' ? ' error':'') + '" id="signup-codigo-hint">' + esc(hintCodigoTexto()) + '</p></div>' : '') +
    '<div style="display:flex;gap:12px"><div style="flex:1"><div class="campo"><label for="signup-nombre">Nombre</label>' + campoIconoHTML('signup-nombre', '👤', '', SIGNUP.nombre) + '</div></div>' +
    '<div style="flex:1"><div class="campo"><label for="signup-apellido">Apellido</label>' + campoIconoHTML('signup-apellido', '👤', '', SIGNUP.apellido) + '</div></div></div>' +
    '<div class="campo"><label for="signup-email">Correo electrónico</label>' + campoIconoHTML('signup-email', '✉️', 'tucorreo@ejemplo.com', SIGNUP.email) + '</div>' +
    '<div style="display:flex;gap:12px"><div style="flex:1"><div class="campo"><label for="signup-grado">Grado</label><select id="signup-grado" onchange="onCampoInput()"><option value="">-- Selecciona --</option>' +
    opcionesCatalogo(CATALOGOS.grados, 'grados') + CATALOGOS.grados.map(g=>'<option value="'+g.id+'"'+(String(SIGNUP.idGrado)===String(g.id)?' selected':'')+'>'+esc(g.nombre)+'</option>').join('') + '</select></div></div>' +
    '<div style="flex:1"><div class="campo"><label for="signup-area">Área académica de interés</label><select id="signup-area" onchange="onCampoInput()"><option value="">-- Selecciona --</option>' +
    opcionesCatalogo(CATALOGOS.areas, 'áreas') + CATALOGOS.areas.map(a=>'<option value="'+a.id+'"'+(String(SIGNUP.idArea)===String(a.id)?' selected':'')+'>'+esc(a.nombre)+'</option>').join('') + '</select></div></div></div>' +
    '<div style="display:flex;gap:12px"><div style="flex:1"><div class="campo"><label for="signup-pass">Contraseña</label>' + campoPasswordHTML('signup-pass') + '</div></div>' +
    '<div style="flex:1"><div class="campo"><label for="signup-confirm">Confirmar</label>' + campoPasswordHTML('signup-confirm') + '</div></div></div>' +
    checklistPasswordHTML('signup-req-') +
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
/* Checklist de contraseña reutilizable: misma política que el backend (PoliticaContrasena.java) */
function checklistPasswordHTML(prefix) {
  return '<div class="req-box">' +
    '<div class="req-rule" id="' + prefix + 'len">○ Mínimo 8 caracteres</div>' +
    '<div class="req-rule" id="' + prefix + 'upper">○ Al menos una letra mayúscula</div>' +
    '<div class="req-rule" id="' + prefix + 'num">○ Al menos un número</div>' +
    '<div class="req-rule" id="' + prefix + 'match">○ Las contraseñas coinciden</div></div>';
}
function passwordCumplePolitica(pass, confirm) {
  return pass.length >= 8 && /[A-Z]/.test(pass) && /[0-9]/.test(pass) && pass === confirm;
}
/* Mensaje dentro del selector mientras cargan los catálogos o si el backend no responde */
function opcionesCatalogo(lista, nombre) {
  if (CATALOGOS.estado === 'cargando') return '<option value="" disabled>Cargando ' + nombre + '...</option>';
  if (CATALOGOS.estado === 'error') return '<option value="" disabled>No se pudieron cargar las ' + nombre + ' (¿backend apagado?)</option>';
  if (!lista.length) return '<option value="" disabled>No hay ' + nombre + ' disponibles</option>';
  return '';
}
function cambiarTipoSignup(tipo) { SIGNUP.tipo = tipo; SIGNUP.codigo = ''; SIGNUP_COD = { codigo:'', estado:'', nombre:'', mensaje:'' }; render(); }
function marcarTerminosLeidos() { SIGNUP.leidoTerminos = true; actualizarEstadoLegalDom(); }
function marcarPrivacidadLeida() { SIGNUP.leidoPrivacidad = true; actualizarEstadoLegalDom(); }
function actualizarEstadoLegalDom() {
  const t = document.getElementById('signup-status-terminos'), p = document.getElementById('signup-status-privacidad');
  if (t) { t.classList.toggle('leido', SIGNUP.leidoTerminos); t.textContent = (SIGNUP.leidoTerminos?'✓':'○') + ' Términos ' + (SIGNUP.leidoTerminos?'leídos':'sin leer'); }
  if (p) { p.classList.toggle('leido', SIGNUP.leidoPrivacidad); p.textContent = (SIGNUP.leidoPrivacidad?'✓':'○') + ' Política de datos ' + (SIGNUP.leidoPrivacidad?'leída':'sin leer'); }
}
function pintarHintCodigo() {
  const hint = document.getElementById('signup-codigo-hint');
  if (!hint) return;
  hint.classList.toggle('error', SIGNUP_COD.estado === 'error');
  hint.textContent = hintCodigoTexto();
}
/* Valida el código contra el backend cuando tiene los 8 caracteres (con una pequeña espera para no disparar en cada tecla) */
function programarValidacionCodigo(codigo) {
  clearTimeout(_timerCodigo);
  if (codigo.length !== LONGITUD_CODIGO_INSTITUCION) {
    SIGNUP_COD = { codigo, estado: codigo ? 'error' : '', nombre:'', mensaje: codigo ? 'El código debe tener ' + LONGITUD_CODIGO_INSTITUCION + ' caracteres' : '' };
    pintarHintCodigo();
    return;
  }
  if (SIGNUP_COD.codigo === codigo && SIGNUP_COD.estado !== '') return; // DEF-001: ya validado (ok o error) o validándose: no repetir
  SIGNUP_COD = { codigo, estado:'validando', nombre:'', mensaje:'' };
  pintarHintCodigo();
  _timerCodigo = setTimeout(async () => {
    try {
      const response = await fetch(API_BASE + '/api/auth/validar-codigo-institucion?codigo=' + encodeURIComponent(codigo));
      const data = await response.json();
      if (SIGNUP.codigo.trim().toUpperCase() !== codigo) return; // el usuario siguió escribiendo
      SIGNUP_COD = response.ok ? { codigo, estado:'ok', nombre:data.nombre, mensaje:'' }
                               : { codigo, estado:'error', nombre:'', mensaje:data.mensaje || 'Código no válido' };
    } catch (err) {
      SIGNUP_COD = { codigo, estado:'error', nombre:'', mensaje:'No se pudo validar el código. ¿Está corriendo el backend?' };
    }
    pintarHintCodigo();
    actualizarValidacionSignup();
  }, 350);
}
function actualizarValidacionSignup() {
  const val = id => (document.getElementById(id)||{}).value || '';
  const nombre = val('signup-nombre'), apellido = val('signup-apellido'), email = val('signup-email');
  const idGrado = val('signup-grado'), idArea = val('signup-area');
  const pass = val('signup-pass'), confirm = val('signup-confirm');
  const codigoEl = document.getElementById('signup-codigo');
  if (codigoEl) {
    SIGNUP.codigo = codigoEl.value;
    programarValidacionCodigo(SIGNUP.codigo.trim().toUpperCase());
  }
  SIGNUP.nombre = nombre; SIGNUP.apellido = apellido; SIGNUP.email = email; SIGNUP.idGrado = idGrado; SIGNUP.idArea = idArea;
  const codigoValido = SIGNUP.tipo === 'independiente' || SIGNUP_COD.estado === 'ok';
  actualizarChecklistPassword('signup-pass', 'signup-confirm', 'signup-req-', null);
  const passwordOk = passwordCumplePolitica(pass, confirm);

  const faltantes = [];
  if (!nombre.trim()) faltantes.push('tu nombre');
  if (!apellido.trim()) faltantes.push('tu apellido');
  if (!email.includes('@') || !email.includes('.')) faltantes.push('un correo válido');
  if (!idGrado) faltantes.push('tu grado');
  if (!idArea) faltantes.push('un área académica de interés');
  if (!passwordOk) faltantes.push('una contraseña que cumpla los requisitos');
  if (!codigoValido) faltantes.push('un código de institución válido');
  if (!SIGNUP.acepta) faltantes.push('aceptar los términos');
  const puedeCrear = faltantes.length === 0;

  const btn = document.getElementById('signup-btn-crear'); if (btn) btn.disabled = !puedeCrear;
  const msg = document.getElementById('signup-faltantes'); if (msg) msg.textContent = puedeCrear ? '' : ('Falta: ' + faltantes.join(', ') + '.');
}
async function crearCuenta() {
  actualizarValidacionSignup();
  if (document.getElementById('signup-btn-crear').disabled) return;

  const pass = document.getElementById('signup-pass').value;
  window._signupInst = SIGNUP.tipo === 'institucion' ? SIGNUP_COD.nombre : '';
  window._signupEmail = SIGNUP.email;

  const btn = document.getElementById('signup-btn-crear');
  const textoOriginal = btn.textContent;
  btn.disabled = true; btn.textContent = 'Creando cuenta...';

  try {
    // nombreUsuario no se envía: el backend genera uno único (nombre.apellido, con dígitos si ya existe).
    const response = await fetch(API_BASE + '/api/auth/registro', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        nombre: SIGNUP.nombre.trim(),
        apellido: SIGNUP.apellido.trim(),
        correo: SIGNUP.email.trim(),
        contrasena: pass,
        idGrado: Number(SIGNUP.idGrado),
        idArea: Number(SIGNUP.idArea),
        codigoInstitucion: SIGNUP.tipo === 'institucion' ? SIGNUP.codigo.trim().toUpperCase() : ''
      })
    });
    const data = await response.json();
    if (!response.ok) throw new Error((data.detalles && data.detalles.length ? data.detalles.join(' · ') : data.mensaje) || 'No se pudo crear la cuenta.');

    window._signupUsuario = data.nombreUsuario;
    SIGNUP_STEP = 'done'; render();
  } catch (err) {
    toast(err.message || 'No se pudo conectar con el servidor (' + API_BASE + '). ¿Está encendido?', true);
    btn.disabled = false; btn.textContent = textoOriginal;
  }
}
function reiniciarSignup() {
  SIGNUP_STEP = 'form';
  SIGNUP = { tipo:'independiente', codigo:'', nombre:'', apellido:'', email:'', idGrado:'', idArea:'', acepta:false, leidoTerminos:false, leidoPrivacidad:false };
  SIGNUP_COD = { codigo:'', estado:'', nombre:'', mensaje:'' };
}
function pantallaRegistroDone() {
  const form =
    brandHTML() +
    '<div class="icon-circle">✓</div>' +
    '<h1 class="auth-title-sm">Cuenta creada</h1>' +
    '<p class="text-sm muted" style="margin-bottom:10px;line-height:1.6">Tu cuenta quedó creada para <strong style="color:var(--ink)">' + esc(window._signupEmail) + '</strong>' +
    (window._signupUsuario ? ' con el nombre de usuario <strong style="color:var(--ink)">' + esc(window._signupUsuario) + '</strong>' : '') + '.' +
    (window._signupInst ? ' Quedaste afiliado a <strong style="color:var(--ink)">' + esc(window._signupInst) + '</strong>.' : '') + '</p>' +
    '<p class="text-sm muted" style="margin-bottom:10px;line-height:1.6">Antes de iniciar sesión debes <strong style="color:var(--ink)">activarla</strong> con el enlace que te enviamos al correo. Abre el enlace y la cuenta se activa sola; si no te llega, revisa la carpeta de spam o usa el token de la consola del backend ("Token de activación de cuenta...").</p>' +
    '<button class="btn btn-primary w-full" style="margin-top:8px" onclick="ACTIVAR.modo=\'estudiante\';ACTIVAR.correo=window._signupEmail;reiniciarSignup();goTo(\'activar\')">Activar mi cuenta →</button>' +
    '<div style="text-align:center;margin-top:14px"><button class="btn-text" onclick="reiniciarSignup();goTo(\'login\')">Ir a iniciar sesión</button></div>';
  return shellHTML(form, sidePanelHTML('¡Bienvenido a BioLearn!', 'Solo falta activar tu cuenta para empezar.'));
}

/* ============================== 4. Activar cuenta (estudiante) o invitación (personal) ============================== */
/* modo 'estudiante': pega el token de verificación de correo → GET /api/auth/verificar-cuenta
   modo 'invitacion': Docente/Contador/Supervisor pega el token y define su contraseña → POST /api/auth/completar-invitacion */
let ACTIVAR = { modo:'estudiante', token:'', correo:'', hecho:false, mensaje:'' };

function pantallaActivar() {
  if (ACTIVAR.hecho) return pantallaActivarDone();
  const esEst = ACTIVAR.modo === 'estudiante';
  let form =
    brandHTML() +
    '<button class="btn-text" style="margin-bottom:14px;display:flex;align-items:center;gap:6px" onclick="ACTIVAR.token=\'\';goTo(\'login\')">← Volver a iniciar sesión</button>' +
    '<h1 class="auth-title-sm" style="margin-bottom:6px">Activar cuenta</h1>' +
    '<div class="tipo-cuenta-row">' +
    '<button type="button" class="tipo-cuenta-btn' + (esEst?' activo':'') + '" onclick="cambiarModoActivar(\'estudiante\')">🎓<div class="titulo">Me registré como estudiante</div><div class="sub">Confirmo mi correo</div></button>' +
    '<button type="button" class="tipo-cuenta-btn' + (!esEst?' activo':'') + '" onclick="cambiarModoActivar(\'invitacion\')">✉️<div class="titulo">Recibí una invitación</div><div class="sub">Docente, contador o representante</div></button></div>' +
    '<p class="text-sm muted" style="margin-bottom:16px;line-height:1.5">' + (esEst
      ? 'Si abriste el enlace del correo, el token ya está cargado; si no, pégalo desde el correo o desde la consola del backend. Expira en 24 horas.'
      : 'Si abriste el enlace del correo de invitación, el token ya está cargado; si no, pégalo desde el correo o desde la consola del backend. Define tu contraseña. Expira en 7 días.') + '</p>' +
    '<div class="campo"><label for="act-token">Token</label>' + campoIconoHTML('act-token', '🔑', 'Pega aquí el token', ACTIVAR.token) + '</div>';
  if (esEst) {
    form += '<button class="btn btn-primary w-full" id="act-btn" onclick="verificarCuentaEstudiante()">Activar cuenta</button>' +
      '<div class="req-box" style="margin-top:20px"><p class="text-sm" style="margin:0 0 8px"><strong>¿Tu enlace expiró o no lo encuentras?</strong> Genera uno nuevo:</p>' +
      '<div style="display:flex;gap:8px"><input id="act-correo" value="' + esc(ACTIVAR.correo) + '" placeholder="tucorreo@ejemplo.com" style="flex:1">' +
      '<button class="btn btn-ghost" onclick="reenviarVerificacion()">Reenviar</button></div></div>';
  } else {
    form += '<div class="campo"><label for="act-pass">Contraseña</label>' + campoPasswordHTML('act-pass') + '</div>' +
      '<div class="campo"><label for="act-confirm">Confirmar contraseña</label>' + campoPasswordHTML('act-confirm') + '</div>' +
      checklistPasswordHTML('act-req-') +
      '<button class="btn btn-primary w-full" id="act-btn" onclick="completarInvitacion()">Activar y definir contraseña</button>' +
      '<p class="text-sm muted" style="text-align:center;margin-top:12px">¿Tu invitación expiró? Pídele al administrador que te la reenvíe.</p>';
  }
  return shellHTML(form, sidePanelHTML('Un paso más', 'Activa tu cuenta y empieza a usar BioLearn 🌿'));
}
function cambiarModoActivar(modo) { ACTIVAR.modo = modo; guardarTokenActivar(); render(); }
function guardarTokenActivar() { const el = document.getElementById('act-token'); if (el) ACTIVAR.token = el.value.trim(); }
function validarActivar() {
  guardarTokenActivar();
  const btn = document.getElementById('act-btn');
  if (!btn) return;
  if (ACTIVAR.modo === 'estudiante') { btn.disabled = !ACTIVAR.token; return; }
  actualizarChecklistPassword('act-pass', 'act-confirm', 'act-req-', null);
  btn.disabled = !(ACTIVAR.token && passwordCumplePolitica(document.getElementById('act-pass').value, document.getElementById('act-confirm').value));
}
async function verificarCuentaEstudiante() {
  guardarTokenActivar();
  if (!ACTIVAR.token) { toast('Pega el token de activación', true); return; }
  try {
    const response = await fetch(API_BASE + '/api/auth/verificar-cuenta?token=' + encodeURIComponent(ACTIVAR.token));
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo activar la cuenta.');
    ACTIVAR.hecho = true; ACTIVAR.mensaje = data.mensaje; render();
  } catch (err) {
    toast(err.message || 'No se pudo conectar con el servidor.', true);
  }
}
async function reenviarVerificacion() {
  const correo = (document.getElementById('act-correo').value || '').trim();
  if (!correo.includes('@')) { toast('Escribe el correo con el que te registraste', true); return; }
  ACTIVAR.correo = correo;
  try {
    const response = await fetch(API_BASE + '/api/auth/reenviar-verificacion', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ correo })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.mensaje || 'No se pudo procesar la solicitud.');
    toast(data.mensaje);
  } catch (err) {
    toast(err.message || 'No se pudo conectar con el servidor.', true);
  }
}
async function completarInvitacion() {
  guardarTokenActivar();
  const pass = document.getElementById('act-pass').value;
  try {
    const response = await fetch(API_BASE + '/api/auth/completar-invitacion', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: ACTIVAR.token, contrasena: pass })
    });
    const data = await response.json();
    if (!response.ok) throw new Error((data.detalles && data.detalles.length ? data.detalles.join(' · ') : data.mensaje) || 'No se pudo activar la invitación.');
    ACTIVAR.hecho = true; ACTIVAR.mensaje = data.mensaje; render();
  } catch (err) {
    toast(err.message || 'No se pudo conectar con el servidor.', true);
  }
}
function pantallaActivarDone() {
  const form =
    brandHTML() +
    '<div class="icon-circle">🛡️</div>' +
    '<h1 class="auth-title-sm">Cuenta activada</h1>' +
    '<p class="text-sm muted" style="margin-bottom:24px">' + esc(ACTIVAR.mensaje) + '</p>' +
    '<button class="btn btn-primary w-full" onclick="ACTIVAR={modo:\'estudiante\',token:\'\',correo:\'\',hecho:false,mensaje:\'\'};history.replaceState(null,\'\',window.location.pathname);goTo(\'login\')">Ir a iniciar sesión</button>';
  return shellHTML(form, sidePanelHTML('Todo listo', 'Ya puedes ingresar a BioLearn.'));
}
