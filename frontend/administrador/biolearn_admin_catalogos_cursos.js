/* ================================================================
   BIOLearn — biolearn_admin_catalogos_cursos.js
================================================================ */
let CAT_BUSQUEDA = '';
function renderCatalogos(cont) {
  const q = CAT_BUSQUEDA.toLowerCase();
  const areasFiltradas = DATA.areas.filter(a => a.nombre.toLowerCase().includes(q) || (a.codigo||'').toLowerCase().includes(q));
  let html = '<div class="section-header"><div><h2>Catálogos académicos</h2><p class="subtitle">Áreas académicas y grados disponibles para clasificar los cursos. Puedes editarlas si necesitan ajustes.</p></div>' +
    '<button class="btn btn-primary" onclick="abrirModalArea()">+ Crear área académica</button></div>';
  html += '<input id="cat-buscar" placeholder="Buscar por nombre o código (ej: BIOMOL)..." value="'+esc(CAT_BUSQUEDA)+'" oninput="CAT_BUSQUEDA=this.value;renderCatalogos(document.getElementById(\'app-content\'))" style="width:320px;margin-bottom:16px">';
  html += '<div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:16px">';
  areasFiltradas.forEach(area => {
    html += '<div class="card"><div class="flex justify-between items-start mb-6"><h3 style="font-size:16.5px;margin:0">' + esc(area.nombre) + '</h3>' +
      '<div class="flex gap-4"><button class="btn btn-ghost btn-xs" onclick="abrirModalArea(\''+area.id+'\')">✏️</button><button class="btn btn-ghost btn-xs" onclick="confirmarEliminarArea(\''+area.id+'\')" style="color:var(--clay)">🗑</button></div></div>' +
      '<div class="text-sm mb-10" style="font-family:monospace;font-weight:700;color:var(--blue)">' + esc(area.codigo||'—') + '</div>' +
      '<div class="flex gap-6 flex-wrap">' + (area.grados.length ? area.grados.map(g=>'<span class="pill pill-verde">'+esc(g)+'</span>').join('') : '<span class="text-sm muted">Sin grados definidos</span>') + '</div></div>';
  });
  if (areasFiltradas.length === 0) html += '<div class="empty-state">Sin resultados para "' + esc(CAT_BUSQUEDA) + '".</div>';
  html += '</div>';
  cont.innerHTML = html;
  renderModalArea();
}
function confirmarEliminarArea(id) {
  const area = DATA.areas.find(x=>x.id===id);
  confirmarAccion('¿Eliminar <strong>' + esc(area.nombre) + '</strong>? Los cursos que la usan quedarán sin área asignada.', function () {
    DATA.areas = DATA.areas.filter(x=>x.id!==id);
    saveData(); render(); toast('Área "' + area.nombre + '" eliminada');
  }, 'Eliminar');
}
function sugerirCodigoArea(nombre) {
  return nombre.normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-zA-Z ]/g,'').split(' ').filter(Boolean).slice(0,2).map(w=>w.slice(0,4)).join('').toUpperCase();
}
function regenerarCodigoArea() {
  const nombre = document.getElementById('area-nombre').value.trim();
  if (!nombre) { toast('Escribe primero el nombre del área', true); return; }
  document.getElementById('area-codigo').value = sugerirCodigoArea(nombre);
}
function abrirModalArea(id) {
  const area = id ? DATA.areas.find(x=>x.id===id) : null;
  document.getElementById('area-modal-titulo').textContent = area ? 'Editar área académica' : 'Crear área académica';
  document.getElementById('area-modal-btn').textContent = area ? 'Guardar cambios' : 'Crear';
  document.getElementById('area-id').value = area ? area.id : '';
  document.getElementById('area-nombre').value = area ? area.nombre : '';
  document.getElementById('area-codigo').value = area ? (area.codigo||'') : '';
  document.getElementById('area-grados').value = area ? area.grados.join(', ') : '';
  abrirModal('modal-area');
}
function renderModalArea() {
  if (document.getElementById('modal-area')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-area"><div class="modal"><div class="modal-header"><h3 id="area-modal-titulo">Crear área académica</h3><button class="modal-close" onclick="cerrarModal(\'modal-area\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="area-id"><div class="campo"><label>Nombre del área</label><input id="area-nombre"></div>' +
    '<div class="campo"><label>Código (para búsqueda rápida)</label><div class="flex gap-8"><input id="area-codigo" style="flex:1;font-family:monospace" placeholder="Ej: BIOMOL"><button type="button" class="btn btn-ghost btn-sm" onclick="regenerarCodigoArea()">🔄 Generar</button></div></div>' +
    '<div class="campo"><label>Grados (separados por coma)</label><input id="area-grados" placeholder="Introductorio, Intermedio, Avanzado"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-area\')">Cancelar</button><button class="btn btn-primary" id="area-modal-btn" onclick="guardarArea()">Crear</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarArea() {
  const id = document.getElementById('area-id').value, nombre = document.getElementById('area-nombre').value.trim();
  if (!nombre) { toast('Escribe el nombre del área', true); return; }
  let codigo = document.getElementById('area-codigo').value.trim().toUpperCase();
  if (!codigo) codigo = sugerirCodigoArea(nombre);
  const enUso = DATA.areas.find(a => a.codigo === codigo && a.id !== id);
  if (enUso) { toast('Ese código ya lo usa "' + enUso.nombre + '" — elige otro', true); return; }
  const grados = document.getElementById('area-grados').value.split(',').map(g=>g.trim()).filter(Boolean);
  if (id) { Object.assign(DATA.areas.find(x=>x.id===id), { nombre, codigo, grados }); toast('Área "' + nombre + '" actualizada'); }
  else { DATA.areas.unshift({ id:uid('area'), nombre, codigo, grados }); toast('Área académica "' + nombre + '" creada'); }
  saveData(); cerrarModal('modal-area'); render();
}

/* ---------------------------- Cursos ---------------------------- */
let CUR_FILTRO_ESTADO = 'todos';
let CUR_BUSQUEDA = '';
function renderCursos(cont) {
  const q = CUR_BUSQUEDA.toLowerCase();
  const filtrados = DATA.cursos.filter(c => (CUR_FILTRO_ESTADO==='todos' || c.estado===CUR_FILTRO_ESTADO) &&
    (c.nombre.toLowerCase().includes(q) || (c.codigo||'').toLowerCase().includes(q)));
  let html = '<div class="section-header"><div><h2>Gestión de cursos</h2><p class="subtitle">Supervisa los cursos creados por los docentes: aprueba, publica, archiva o retira contenido</p></div></div>';
  html += '<input id="cur-buscar" placeholder="Buscar por nombre o código (ej: BIOMOL-101)..." value="'+esc(CUR_BUSQUEDA)+'" oninput="CUR_BUSQUEDA=this.value;renderCursos(document.getElementById(\'app-content\'))" style="width:320px;margin-bottom:14px">';
  html += '<div class="flex gap-8 mb-14">';
  ['todos','Publicado','En revisión','Borrador','Archivado'].forEach(e => {
    const activo = CUR_FILTRO_ESTADO === e;
    html += '<button onclick="CUR_FILTRO_ESTADO=\''+e+'\';render()" style="padding:7px 14px;border-radius:20px;font-size:13px;font-weight:600;cursor:pointer;border:1px solid '+(activo?'var(--blue)':'var(--line)')+';background:'+(activo?'var(--blue-soft)':'transparent')+';color:'+(activo?'var(--blue)':'var(--ink-soft)')+'">' + (e==='todos'?'Todos':e) + '</button>';
  });
  html += '</div>';
  html += '<div class="tabla-wrap"><table><thead><tr style="background:var(--blue-soft)">' +
    ['Código','Curso','Área','Docente','Inscritos','Lecciones','Precio','Estado',''].map(h=>'<th style="color:var(--blue)">'+h+'</th>').join('') + '</tr></thead><tbody>';
  filtrados.forEach(c => {
    html += '<tr><td style="font-family:monospace;font-weight:700;font-size:12.5px;color:var(--blue)">' + esc(c.codigo||'—') + '</td>' +
      '<td style="font-weight:600;cursor:pointer" onclick="abrirModalCursoDetalle(\''+c.id+'\')">' + esc(c.nombre) + '</td>' +
      '<td class="text-sm muted">' + esc(c.area) + '</td><td class="text-sm muted">' + esc(c.docente) + '</td>' +
      '<td class="text-sm muted">' + c.inscritos + '</td><td class="text-sm muted">' + c.lecciones + '</td><td class="text-sm muted">' + money(c.precio) + '</td>' +
      '<td>' + pill(c.estado) + '</td><td><div class="flex gap-8" style="justify-content:flex-end"><button class="btn btn-ghost btn-xs" onclick="abrirModalCursoDetalle(\''+c.id+'\')">✏️</button><button class="btn btn-ghost btn-xs" onclick="confirmarEliminarCurso(\''+c.id+'\')" style="color:var(--clay)">🗑</button></div></td></tr>';
  });
  if (filtrados.length === 0) html += '<tr><td colspan="9" class="empty-state">No hay cursos que coincidan.</td></tr>';
  html += '</tbody></table></div>';
  cont.innerHTML = html;
  renderModalCursoDetalle();
}
function confirmarEliminarCurso(id) {
  const c = DATA.cursos.find(x=>x.id===id);
  confirmarAccion('¿Eliminar <strong>' + esc(c.nombre) + '</strong>? Se perderán sus lecciones, foros y materiales asociados.', function () {
    DATA.cursos = DATA.cursos.filter(x=>x.id!==id);
    saveData(); render(); toast('Curso "' + c.nombre + '" eliminado');
  }, 'Eliminar');
}
function abrirModalCursoDetalle(id) {
  const c = DATA.cursos.find(x=>x.id===id);
  document.getElementById('cur-modal-titulo').textContent = 'Editar curso · ' + c.nombre;
  document.getElementById('cur-id').value = c.id;
  document.getElementById('cur-nombre').value = c.nombre;
  document.getElementById('cur-codigo').value = c.codigo||'';
  const selArea = document.getElementById('cur-area');
  selArea.innerHTML = DATA.areas.map(a=>'<option value="'+esc(a.nombre)+'"'+(a.nombre===c.area?' selected':'')+'>'+esc(a.codigo||'')+' — '+esc(a.nombre)+'</option>').join('') + (!DATA.areas.some(a=>a.nombre===c.area) ? '<option value="'+esc(c.area)+'" selected>'+esc(c.area)+'</option>' : '');
  const docentes = DATA.usuarios.filter(u=>u.rol==='docente');
  const selDoc = document.getElementById('cur-docente');
  selDoc.innerHTML = docentes.map(d=>'<option value="'+esc(d.nombre)+'"'+(d.nombre===c.docente?' selected':'')+'>'+esc(d.nombre)+'</option>').join('') + (!docentes.some(d=>d.nombre===c.docente) ? '<option value="'+esc(c.docente)+'" selected>'+esc(c.docente)+'</option>' : '');
  document.getElementById('cur-precio').value = c.precio;
  document.getElementById('cur-lecciones').value = c.lecciones;
  document.getElementById('cur-info').textContent = c.inscritos + ' estudiante(s) inscritos actualmente · las lecciones pueden incluir lecturas, laboratorios y juegos externos, artículos científicos, foros, sesiones en línea y eventos virtuales.';
  renderCursoEstadoAcciones(c);
  abrirModal('modal-curso-detalle');
}
function renderCursoEstadoAcciones(c) {
  let html = '';
  if (c.estado !== 'Publicado') html += '<button class="btn btn-amber btn-sm" onclick="cambiarEstadoCursoAdmin(\''+c.id+'\',\'Publicado\')">Publicar</button> ';
  if (c.estado === 'En revisión') html += '<button class="btn btn-danger btn-sm" onclick="cambiarEstadoCursoAdmin(\''+c.id+'\',\'Borrador\')">Devolver a borrador</button> ';
  if (c.estado === 'Publicado') html += '<button class="btn btn-ghost btn-sm" onclick="cambiarEstadoCursoAdmin(\''+c.id+'\',\'Archivado\')">Archivar</button> ';
  html += pill(c.estado);
  document.getElementById('cur-estado-acciones').innerHTML = html;
}
function cambiarEstadoCursoAdmin(id, estado) {
  const c = DATA.cursos.find(x=>x.id===id);
  c.estado = estado; saveData(); renderCursoEstadoAcciones(c);
  toast('Curso "' + c.nombre + '" ahora está ' + estado.toLowerCase());
}
function renderModalCursoDetalle() {
  if (document.getElementById('modal-curso-detalle')) return;
  const div = document.createElement('div');
  div.innerHTML = '<div class="modal-overlay" id="modal-curso-detalle"><div class="modal" style="max-width:560px"><div class="modal-header"><h3 id="cur-modal-titulo">Editar curso</h3><button class="modal-close" onclick="cerrarModal(\'modal-curso-detalle\')">✕</button></div>' +
    '<div class="modal-body"><input type="hidden" id="cur-id">' +
    '<div class="grid-2"><div class="campo"><label>Nombre del curso</label><input id="cur-nombre"></div><div class="campo"><label>Código</label><input id="cur-codigo" style="font-family:monospace"></div></div>' +
    '<div class="grid-2"><div class="campo"><label>Área académica</label><select id="cur-area"></select></div><div class="campo"><label>Docente</label><select id="cur-docente"></select></div></div>' +
    '<div class="grid-2"><div class="campo"><label>Precio (COP)</label><input type="number" id="cur-precio"></div><div class="campo"><label>Número de lecciones</label><input type="number" id="cur-lecciones"></div></div>' +
    '<p class="hint" id="cur-info" style="margin:-4px 0 16px"></p>' +
    '<div class="flex gap-8 flex-wrap" id="cur-estado-acciones" style="border-top:1px solid var(--line);padding-top:16px;margin-bottom:14px"></div></div>' +
    '<div class="modal-btns"><button class="btn btn-ghost" onclick="cerrarModal(\'modal-curso-detalle\')">Cancelar</button><button class="btn btn-primary" onclick="guardarCursoAdmin()">Guardar cambios</button></div></div></div>';
  document.body.appendChild(div.firstElementChild);
}
function guardarCursoAdmin() {
  const id = document.getElementById('cur-id').value, nombre = document.getElementById('cur-nombre').value.trim();
  if (!nombre) { toast('Escribe el nombre del curso', true); return; }
  const codigo = document.getElementById('cur-codigo').value.trim().toUpperCase();
  const enUso = codigo && DATA.cursos.find(c => c.codigo === codigo && c.id !== id);
  if (enUso) { toast('Ese código ya lo usa "' + enUso.nombre + '" — elige otro', true); return; }
  Object.assign(DATA.cursos.find(x=>x.id===id), { nombre, codigo, area:document.getElementById('cur-area').value, docente:document.getElementById('cur-docente').value,
    precio:Number(document.getElementById('cur-precio').value)||0, lecciones:Number(document.getElementById('cur-lecciones').value)||0 });
  saveData(); cerrarModal('modal-curso-detalle'); render(); toast('Curso "' + nombre + '" modificado');
}

