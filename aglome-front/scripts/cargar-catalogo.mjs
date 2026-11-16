// Carga el catálogo REAL de Aglome (tomado de la "LISTA DE PRECIOS" del Excel) usando la API del back.
// Es una selección de productos fáciles de reconocer y de encontrar en internet, no todo el inventario.
//
// Uso (con el back corriendo):   npm run cargar-catalogo
// Se puede repetir sin miedo: lo que ya existe se omite (no duplica).
//   · scripts/catalogo-real.json  → categorías, marcas, productos, variantes, precios y stock (del Excel).
//   · scripts/imagenes.json       → enlace de la foto de cada producto. Si agregas o cambias un enlace y vuelves
//                                   a correr el script, la foto se actualiza en los productos que ya existen.
// Variables opcionales: API (por defecto http://localhost:8081), ADMIN_EMAIL y ADMIN_PASSWORD.

import { readFileSync } from 'node:fs';

const API = (process.env.API || 'http://localhost:8081').replace(/\/$/, '');
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
const urlDeImagen = (nombre) => {
  const u = IMAGENES[nombre]?.url;
  return typeof u === 'string' && /^https?:\/\//i.test(u.trim()) ? u.trim() : null;
};

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
const n = { categorias: 0, marcas: 0, productos: 0, variantes: 0, fotos: 0, omitidos: 0 };

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
  for (const v of p.variantes) {
    const r = await api('/api/admin/variantes', {
      metodo: 'POST', token,
      cuerpo: { productoBaseId: producto.id, sku: v.sku, precioVenta: v.precioVenta, stock: v.stock, atributos: v.atributos },
    });
    if (r.ok) n.variantes++;
    else if (r.estado === 409) n.omitidos++; // el SKU ya existía
    else falla(`No se pudo crear la variante ${v.sku}`, r);
  }
}

const sinFoto = CATALOGO.productos.filter((p) => !urlDeImagen(p.nombre)).length;
console.log(`✓ Listo. Creados: ${n.categorias} categorías, ${n.marcas} marcas, ${n.productos} productos, ${n.variantes} variantes. Fotos puestas: ${n.fotos}. Omitidos (ya existían): ${n.omitidos}.`);
if (sinFoto) console.log(`  ${sinFoto} de ${CATALOGO.productos.length} productos aún sin foto: pega los enlaces en scripts/imagenes.json y vuelve a correr este script.`);
