// Carga el catálogo REAL de Aglome (tomado de la "LISTA DE PRECIOS" del Excel) usando la API del back.
// Es una selección de productos fáciles de reconocer y de encontrar en internet, no todo el inventario.
//
// Uso (con el back corriendo):   npm run cargar-catalogo
// Se puede repetir sin miedo: lo que ya existe se omite (no duplica).
//   · scripts/catalogo-real.json  → categorías, marcas, productos, variantes, precios y stock (del Excel).
//   · scripts/imagenes.json       → enlaces de las fotos: la portada de cada producto y una foto por cada color
//                                   ("colores"). Opcionalmente una foto por opción exacta, con "variantes": { "SKU": { "url": "…" } }.
//                                   Si agregas o cambias un enlace y vuelves a correr el script, las fotos se actualizan
//                                   también en los productos que ya existen. Las ofertas ("Precio anterior") también se actualizan.
//   · Con la opción --stock también se vuelve a poner el stock del JSON en las opciones que ya existían.
// Variables opcionales: API (por defecto http://localhost:8080), ADMIN_EMAIL y ADMIN_PASSWORD.

import { readFileSync } from 'node:fs';

const API = (process.env.API || 'http://localhost:8080').replace(/\/$/, '');
const EMAIL = process.env.ADMIN_EMAIL || 'admin@aglome.com';
const PASSWORD = process.env.ADMIN_PASSWORD || 'Admin123';

const leerJson = (nombre) => JSON.parse(readFileSync(new URL(nombre, import.meta.url), 'utf8'));
const CATALOGO = leerJson('./catalogo-real.json');
let IMAGENES = {};
try { IMAGENES = leerJson('./imagenes.json'); } catch { /* sin imágenes todavía: no pasa nada */ }

// ── Utilidades ──────────────────────────────────────────────────────
async function api(ruta, { metodo = 'GET', token, cuerpo } = {}) {
  const r = await fetch(API + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
  });
  const texto = await r.text();
  let datos = null;
  try { datos = texto ? JSON.parse(texto) : null; } catch { datos = texto; }
  return { ok: r.ok, estado: r.status, datos };
}

const lista = (d) => (Array.isArray(d) ? d : d?.contenido ?? []);
const limpia = (u) => (typeof u === 'string' && /^https?:\/\//i.test(u.trim()) ? u.trim() : null);
const urlDeImagen = (nombre) => limpia(IMAGENES[nombre]?.url);
/** Foto de una opción: la suya (por SKU), si no la de su color, si no la portada del producto. */
const urlDeVariante = (producto, v) =>
  limpia(IMAGENES[producto]?.variantes?.[v.sku]?.url) || limpia(IMAGENES[producto]?.colores?.[v.atributos?.Color]?.url) || urlDeImagen(producto);
const FORZAR_STOCK = process.argv.includes('--stock');
/** Compara características sin importar el orden en que las devuelva el back. */
const mismasCaracteristicas = (a, b) => JSON.stringify(Object.entries(a ?? {}).sort()) === JSON.stringify(Object.entries(b ?? {}).sort());

function falla(mensaje, r) {
  console.error(`✗ ${mensaje}` + (r ? ` (HTTP ${r.estado}: ${r.datos?.mensaje ?? JSON.stringify(r.datos)})` : ''));
  process.exit(1);
}

// ── Carga ───────────────────────────────────────────────────────────
console.log(`API: ${API}`);
const login = await api('/api/auth/login', { metodo: 'POST', cuerpo: { email: EMAIL, password: PASSWORD } }).catch(() => null);
if (!login) falla('No se pudo conectar. ¿Está el back encendido en ' + API + '?');
if (!login.ok) falla('No se pudo iniciar sesión como administrador. Revisa ADMIN_EMAIL y ADMIN_PASSWORD', login);
const token = login.datos.token;
const n = { categorias: 0, marcas: 0, productos: 0, variantes: 0, fotos: 0, fotosVariantes: 0, actualizadas: 0, omitidos: 0 };

// Categorías (las principales primero, luego las hijas)
const categoriaId = {};
for (const c of lista((await api('/api/categorias')).datos)) categoriaId[c.nombre] = c.id;
const ordenadas = [...CATALOGO.categorias.filter((c) => !c.padre), ...CATALOGO.categorias.filter((c) => c.padre)];
for (const c of ordenadas) {
  if (categoriaId[c.nombre]) { n.omitidos++; continue; }
  const r = await api('/api/admin/categorias', { metodo: 'POST', token, cuerpo: { nombre: c.nombre, padreId: c.padre ? categoriaId[c.padre] : null } });
  if (!r.ok) falla(`No se pudo crear la categoría ${c.nombre}`, r);
  categoriaId[c.nombre] = r.datos.id; n.categorias++;
}

// Marcas
const marcaId = {};
for (const m of lista((await api('/api/marcas')).datos)) marcaId[m.nombre] = m.id;
for (const nombre of CATALOGO.marcas) {
  if (marcaId[nombre]) { n.omitidos++; continue; }
  const r = await api('/api/admin/marcas', { metodo: 'POST', token, cuerpo: { nombre } });
  if (!r.ok) falla(`No se pudo crear la marca ${nombre}`, r);
  marcaId[nombre] = r.datos.id; n.marcas++;
}

// Productos y variantes
for (const p of CATALOGO.productos) {
  const foto = urlDeImagen(p.nombre);
  const busqueda = await api(`/api/productos/filtrar?texto=${encodeURIComponent(p.nombre)}&tamano=50`);
  let producto = lista(busqueda.datos).find((x) => x.nombre === p.nombre);
  if (producto) {
    n.omitidos++;
    if (foto && producto.imagenUrl !== foto) { // el producto ya existía: solo se actualiza su foto
      const r = await api(`/api/admin/productos/${producto.id}`, {
        metodo: 'PUT', token,
        cuerpo: { nombre: producto.nombre, descripcion: producto.descripcion, marcaId: producto.marcaId ?? null, categoriaId: producto.categoriaId, imagenUrl: foto, publicado: true },
      });
      if (r.ok) n.fotos++; else console.warn(`! No se pudo actualizar la foto de ${p.nombre} (HTTP ${r.estado}: ${r.datos?.mensaje ?? ''})`);
    }
  } else {
    const r = await api('/api/admin/productos', {
      metodo: 'POST', token,
      cuerpo: { nombre: p.nombre, descripcion: p.descripcion, marcaId: marcaId[p.marca], categoriaId: categoriaId[p.categoria], imagenUrl: foto, publicado: true },
    });
    if (!r.ok) falla(`No se pudo crear el producto ${p.nombre}`, r);
    producto = r.datos; n.productos++; if (foto) n.fotos++;
  }
  const existentes = new Map((producto.variantes ?? []).map((x) => [x.sku, x]));
  for (const v of p.variantes) {
    const imagenUrl = urlDeVariante(p.nombre, v);
    const previa = existentes.get(v.sku);
    if (previa) { // la opción ya existía: se actualizan su foto y sus características (ofertas); el stock solo con --stock
      n.omitidos++;
      const cambio = (previa.imagenUrl ?? null) !== imagenUrl || !mismasCaracteristicas(previa.atributos, v.atributos) || (FORZAR_STOCK && previa.stock !== v.stock);
      if (cambio) {
        const r = await api(`/api/admin/variantes/${previa.id}`, {
          metodo: 'PUT', token,
          cuerpo: { productoBaseId: producto.id, sku: v.sku, precioVenta: previa.precioVenta, stock: FORZAR_STOCK ? v.stock : previa.stock, imagenUrl, atributos: v.atributos },
        });
        if (r.ok) { n.actualizadas++; if (imagenUrl) n.fotosVariantes++; } else console.warn(`! No se pudo actualizar ${v.sku} (HTTP ${r.estado}: ${r.datos?.mensaje ?? ''})`);
      }
      continue;
    }
    const r = await api('/api/admin/variantes', {
      metodo: 'POST', token,
      cuerpo: { productoBaseId: producto.id, sku: v.sku, precioVenta: v.precioVenta, stock: v.stock, imagenUrl, atributos: v.atributos },
    });
    if (r.ok) { n.variantes++; if (imagenUrl) n.fotosVariantes++; }
    else if (r.estado === 409) n.omitidos++; // el SKU ya existía (pero no se pudo leer en la búsqueda)
    else falla(`No se pudo crear la variante ${v.sku}`, r);
  }
}

const sinFoto = CATALOGO.productos.filter((p) => !urlDeImagen(p.nombre)).length;
const opcionesSinFoto = CATALOGO.productos.reduce((t, p) => t + p.variantes.filter((v) => !limpia(IMAGENES[p.nombre]?.variantes?.[v.sku]?.url) && !limpia(IMAGENES[p.nombre]?.colores?.[v.atributos?.Color]?.url)).length, 0);
console.log(`✓ Listo. Creados: ${n.categorias} categorías, ${n.marcas} marcas, ${n.productos} productos, ${n.variantes} variantes. Actualizadas: ${n.actualizadas} variantes. Portadas puestas: ${n.fotos}. Omitidos (ya existían): ${n.omitidos}.`);
if (sinFoto) console.log(`  ${sinFoto} de ${CATALOGO.productos.length} productos aún sin portada.`);
if (opcionesSinFoto) console.log(`  ${opcionesSinFoto} opciones aún sin foto propia (usan la portada): pega los enlaces en scripts/imagenes.json (o en el Excel) y vuelve a correr este script.`);
