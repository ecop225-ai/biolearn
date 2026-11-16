/* ================================================================
   BIOLearn — biolearn_contador_finanzas_api.js
   Pagos, Fase 3, conectado al backend real. Reemplaza en el portal del contador:
   - Inicio: cifras reales del mes, transferencias por confirmar, reembolsos por revisar y por devolver,
     ingresos y devoluciones de los últimos 6 meses y los últimos movimientos.
   - Reportes: ingresos, facturas, reembolsos y general por período, en PDF (vista imprimible) y Excel.
   - Facturación y recaudo (nuevo): datos del emisor y cuenta para transferencias. Edita el administrador
     financiero; el auxiliar y el auditor solo consultan.
   Usa el módulo compartido ../compartido/biolearn_finanzas.js. Se carga al final del HTML.
================================================================ */
FIN.pedir = pedirApiCont;
FIN.base = API_BASE_CONT;
function renderInicio(cont) { finRenderInicio(cont, CONTADOR.nombre, CONTADOR.cargo); }
function renderReportes(cont) { finRenderReportes(cont); }
function renderDatosFinancieros(cont) { finRenderDatos(cont); }
