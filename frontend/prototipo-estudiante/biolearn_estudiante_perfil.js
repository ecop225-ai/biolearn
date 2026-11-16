/* ================================================================
   BIOLearn — biolearn_estudiante_perfil.js
   «Mi institución» en Mi perfil, conectado al backend real (GET /api/estudiante/perfil).
   - Independiente: se une a su colegio con el código de afiliación.
   - Vinculado: ve su colegio, puede cambiar a otro con el código del nuevo colegio o salir del colegio
     (DELETE /api/estudiante/perfil/institucion) y quedar como independiente.
   Antes de confirmar se valida el código (GET /api/auth/validar-codigo-institucion) para mostrar el nombre
   del colegio; luego PUT /api/estudiante/perfil/institucion. Siempre conserva cursos, pagos y certificados.
   El resto de Mi perfil sigue siendo el del prototipo. Se carga después de biolearn_estudiante_pagos.js.
================================================================ */
let PERFIL_EST_API = null;
const renderPerfilPrototipo = window.renderPerfil;

window.renderPerfil = function (cont) {
  renderPerfilPrototipo(cont);
  const fila = cont.querySelector('.flex.gap-14.flex-wrap');
  const tarjeta = document.createElement('div');
  tarjeta.className = 'card'; tarjeta.id = 'perfil-institucion';
  tarjeta.style.cssText = 'flex:1;min-width:300px';
  tarjeta.innerHTML = '<h3 style="font-size:17px;margin:0 0 16px">🏫 Mi institución</h3><div id="perfil-inst-cuerpo" class="text-sm muted">Cargando...</div>';
  (fila || cont).insertBefore(tarjeta, fila ? fila.children[1] : null);
  cargarInstitucionEst();
};

async function cargarInstitucionEst() {
  try { PERFIL_EST_API = await pedirApiEst('GET', '/api/estudiante/perfil'); pintarInstitucionEst(); }
  catch (err) { const el = document.getElementById('perfil-inst-cuerpo'); if (el) el.textContent = err.message; }
}

function pintarInstitucionEst() {
  const el = document.getElementById('perfil-inst-cuerpo'); if (!el || !PERFIL_EST_API) return;
  const p = PERFIL_EST_API;
  el.className = '';
  const datos = '<div class="text-sm" style="margin-top:12px;color:var(--ink-soft)">Grado: <strong>' + esc(p.grado || '—') + '</strong> · Área: <strong>' + esc(p.area || '—') + '</strong></div>';
  const campoCodigo = (texto) => '<div class="campo"><label>Código de afiliación</label><input id="perfil-codigo-colegio" maxlength="8" placeholder="Ej.: K7M2Q9XA" style="text-transform:uppercase;letter-spacing:2px;font-family:monospace" onkeydown="if(event.key===\'Enter\')validarColegioEst()"></div>' +
    '<button class="btn btn-primary" id="perfil-btn-unirme" onclick="validarColegioEst()">' + texto + '</button>';
  if (p.institucion) {
    el.innerHTML = '<div class="flex gap-10 items-center" id="perfil-inst-vinculado"><span style="font-size:28px">🏫</span><div><div style="font-weight:800;font-size:16px" id="perfil-inst-nombre">' + esc(p.institucion) + '</div>' +
      '<span class="pill pill-verde">Vinculado</span></div></div>' + datos +
      '<p class="text-sm muted" style="margin-top:12px">Ves los cursos de tu colegio en el catálogo y puedes usar sus códigos de inscripción.</p>' +
      '<details id="perfil-cambiar" style="margin-top:6px"><summary style="cursor:pointer;font-weight:700;font-size:14px;margin-bottom:10px">¿Cambiaste de colegio?</summary>' +
      '<p class="text-sm" style="margin:0 0 10px">Escribe el código de afiliación de tu nuevo colegio. Conservas tus cursos, tu progreso, tus pagos y tus certificados.</p>' +
      campoCodigo('Cambiar de colegio') + '</details>' +
      '<div style="border-top:1px solid var(--line);margin-top:16px;padding-top:12px"><button class="btn btn-ghost btn-sm" id="perfil-btn-salir" onclick="salirColegioEst()">Salir de mi colegio</button></div>';
    return;
  }
  el.innerHTML = '<div id="perfil-inst-independiente"><span class="pill pill-gris">Estudiante independiente</span>' + datos +
    '<p class="text-sm" style="margin:14px 0 10px">¿Tu colegio usa BioLearn? Pídele el <strong>código de afiliación</strong> y únete: verás los cursos de tu colegio y conservarás todos tus cursos y pagos.</p>' +
    campoCodigo('Unirme a mi colegio') + '</div>';
}

async function validarColegioEst() {
  const input = document.getElementById('perfil-codigo-colegio');
  const codigo = (input.value || '').trim().toUpperCase();
  if (codigo.length !== 8) { toast('El código de afiliación tiene 8 caracteres', true); input.focus(); return; }
  const btn = document.getElementById('perfil-btn-unirme'); btn.disabled = true;
  try {
    const r = await fetch(API_BASE + '/api/auth/validar-codigo-institucion?codigo=' + encodeURIComponent(codigo));
    const data = await r.json().catch(() => ({}));
    if (!r.ok) throw new Error(data.mensaje || 'No se pudo validar el código');
    const cambio = PERFIL_EST_API && PERFIL_EST_API.institucion;
    confirmarAccion((cambio ? '¿Cambiar de <strong>' + esc(PERFIL_EST_API.institucion) + '</strong> a <strong>' + esc(data.nombre) + '</strong>?'
        : '¿Unirte a <strong>' + esc(data.nombre) + '</strong>?') +
      '<br><span class="text-sm muted">Conservas tus cursos, tu progreso, tus pagos y tus certificados.</span>',
      () => unirmeColegioEst(codigo), cambio ? 'Cambiar de colegio' : 'Unirme');
  } catch (err) { toast(err.message, true); }
  btn.disabled = false;
}

async function unirmeColegioEst(codigo) {
  try {
    PERFIL_EST_API = await pedirApiEst('PUT', '/api/estudiante/perfil/institucion', { codigoAfiliacion: codigo });
    pintarInstitucionEst();
    toast('¡Listo! Ahora perteneces a ' + PERFIL_EST_API.institucion);
  } catch (err) { toast(err.message, true); }
}

function salirColegioEst() {
  confirmarAccion('¿Salir de <strong>' + esc(PERFIL_EST_API.institucion) + '</strong>?<br><span class="text-sm muted">Quedarás como estudiante independiente. Conservas tus cursos, tu progreso, tus pagos y tus certificados, y puedes unirte de nuevo con un código de afiliación.</span>',
    async function () {
      try {
        const antes = PERFIL_EST_API.institucion;
        PERFIL_EST_API = await pedirApiEst('DELETE', '/api/estudiante/perfil/institucion');
        pintarInstitucionEst();
        toast('Saliste de ' + antes + '; ahora eres estudiante independiente');
      } catch (err) { toast(err.message, true); }
    }, 'Salir de mi colegio');
}
