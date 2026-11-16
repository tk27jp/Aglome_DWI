// Carga de una vez un catálogo de ejemplo (categorías, marcas, productos y variantes) usando la API del back.
// Es DATOS DE DEMOSTRACIÓN: los precios y el stock son referenciales, no los reales de Aglome.
//
// Uso (con el back corriendo):   npm run cargar-demo
// Se puede repetir sin miedo: lo que ya existe se omite (no duplica).
// Variables opcionales: API (por defecto http://localhost:8080), ADMIN_EMAIL y ADMIN_PASSWORD.

const API = (process.env.API || 'http://localhost:8080').replace(/\/$/, '');
const EMAIL = process.env.ADMIN_EMAIL || 'admin@aglome.com';
const PASSWORD = process.env.ADMIN_PASSWORD || 'Admin123';

// ── Datos de ejemplo ────────────────────────────────────────────────
const CATEGORIAS = [
  { nombre: 'Melamina' },
  { nombre: 'Madera' },
  { nombre: 'Ferretería' },
  { nombre: 'Bisagras', padre: 'Ferretería' },
  { nombre: 'Correderas', padre: 'Ferretería' },
  { nombre: 'Tornillería', padre: 'Ferretería' },
  { nombre: 'Acabados' },
];

const MARCAS = ['Aglome', 'Maderera Norte', 'Ferrex', 'Acabados Pro'];

const M = '1.22 x 2.44 m';
const G = '1.83 x 2.44 m';
const PRODUCTOS = [
  { nombre: 'Melamina Blanca 18 mm', cat: 'Melamina', marca: 'Aglome', desc: 'Tablero de melamina blanca de 18 mm, ideal para muebles de cocina, closets y oficina.',
    vars: [['MEL-BLA-18-M', 135, 40, 'Blanco', M], ['MEL-BLA-18-G', 190, 25, 'Blanco', G]] },
  { nombre: 'Melamina Roble Natural 18 mm', cat: 'Melamina', marca: 'Aglome', desc: 'Melamina con veta de roble natural. Acabado resistente a rayones y humedad.',
    vars: [['MEL-ROB-18-M', 150, 30, 'Roble', M], ['MEL-ROB-18-G', 210, 18, 'Roble', G]] },
  { nombre: 'Melamina Wengué 18 mm', cat: 'Melamina', marca: 'Aglome', desc: 'Melamina color wengué para un acabado oscuro y elegante en muebles a medida.',
    vars: [['MEL-WEN-18-M', 155, 22, 'Wengué', M], ['MEL-WEN-18-G', 215, 4, 'Wengué', G]] },
  { nombre: 'Melamina Gris Humo 15 mm', cat: 'Melamina', marca: 'Maderera Norte', desc: 'Melamina gris humo de 15 mm para frentes, repisas y escritorios.',
    vars: [['MEL-GRI-15-M', 120, 35, 'Gris', M]] },
  { nombre: 'Canto de PVC', cat: 'Melamina', marca: 'Aglome', desc: 'Canto de PVC para cubrir los bordes de la melamina. Rollo de 50 metros.',
    vars: [['CAN-BLA-22', 42, 60, 'Blanco', '22 mm x 50 m'], ['CAN-ROB-22', 48, 45, 'Roble', '22 mm x 50 m'], ['CAN-WEN-22', 48, 40, 'Wengué', '22 mm x 50 m']] },
  { nombre: 'Tablero MDF 15 mm', cat: 'Madera', marca: 'Maderera Norte', desc: 'Tablero MDF crudo de 15 mm, superficie lisa para pintar o enchapar.',
    vars: [['MDF-15-M', 98, 50, 'Natural', M], ['MDF-15-G', 140, 28, 'Natural', G]] },
  { nombre: 'Triplay', cat: 'Madera', marca: 'Maderera Norte', desc: 'Triplay en distintos espesores para construcción, encofrados y carpintería.',
    vars: [['TRI-04', 45, 70, 'Natural', '4 mm'], ['TRI-09', 78, 45, 'Natural', '9 mm'], ['TRI-18', 140, 20, 'Natural', '18 mm']] },
  { nombre: 'Listón de Pino Cepillado', cat: 'Madera', marca: 'Maderera Norte', desc: 'Listón de pino cepillado y seco, ideal para marcos, estructuras y acabados.',
    vars: [['LIS-2X2', 18, 120, 'Natural', '2 x 2 pulg x 3 m'], ['LIS-2X3', 26, 90, 'Natural', '2 x 3 pulg x 3 m']] },
  { nombre: 'Bisagra de Acero', cat: 'Bisagras', marca: 'Ferrex', desc: 'Bisagra de acero para puertas y muebles. Se vende por unidad.',
    vars: [['BIS-ACE-3', 4.5, 100, 'Acero', '3 pulgadas'], ['BIS-ACE-4', 6, 80, 'Acero', '4 pulgadas'], ['BIS-NEG-3', 5, 60, 'Negro', '3 pulgadas']] },
  { nombre: 'Bisagra Cangrejo 35 mm', cat: 'Bisagras', marca: 'Ferrex', desc: 'Bisagra cangrejo con cierre suave para puertas de muebles de melamina.',
    vars: [['BIS-CAN-35', 3.5, 200, 'Niquelado', '35 mm']] },
  { nombre: 'Corredera Telescópica', cat: 'Correderas', marca: 'Ferrex', desc: 'Par de correderas telescópicas para cajones, con tornillos incluidos.',
    vars: [['COR-35', 14, 50, 'Zincado', '35 cm'], ['COR-45', 17, 40, 'Zincado', '45 cm'], ['COR-50', 20, 3, 'Zincado', '50 cm']] },
  { nombre: 'Tornillo Autorroscante', cat: 'Tornillería', marca: 'Ferrex', desc: 'Caja de 100 tornillos autorroscantes cabeza avellanada para madera y melamina.',
    vars: [['TOR-1', 9, 150, 'Zincado', '1 pulg x 100 und'], ['TOR-15', 11, 130, 'Zincado', '1 1/2 pulg x 100 und'], ['TOR-2', 14, 100, 'Zincado', '2 pulg x 100 und']] },
  { nombre: 'Jaladera de Aluminio', cat: 'Ferretería', marca: 'Ferrex', desc: 'Jaladera de aluminio para puertas y cajones, de línea moderna.',
    vars: [['JAL-PLA-128', 7.5, 90, 'Plata', '128 mm'], ['JAL-NEG-128', 8, 70, 'Negro', '128 mm'], ['JAL-PLA-160', 9, 60, 'Plata', '160 mm']] },
  { nombre: 'Pegamento de Contacto', cat: 'Acabados', marca: 'Acabados Pro', desc: 'Pegamento de contacto para laminados, cantos y trabajos de carpintería.',
    vars: [['PEG-250', 12, 80, 'Amarillo', '250 ml'], ['PEG-1L', 38, 40, 'Amarillo', '1 litro']] },
  { nombre: 'Barniz Transparente Mate', cat: 'Acabados', marca: 'Acabados Pro', desc: 'Barniz transparente de acabado mate que protege y realza la madera.',
    vars: [['BAR-1-4', 32, 35, 'Transparente', '1/4 galón'], ['BAR-1', 98, 15, 'Transparente', '1 galón']] },
  { nombre: 'Lija de Agua', cat: 'Acabados', marca: 'Acabados Pro', desc: 'Lija de agua para acabados finos en madera, metal y pintura.',
    vars: [['LIJ-120', 1.5, 300, 'Gris', 'Grano 120'], ['LIJ-220', 1.5, 300, 'Gris', 'Grano 220']] },
];

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
let creados = { categorias: 0, marcas: 0, productos: 0, variantes: 0 };
let omitidos = 0;

// Categorías (las principales primero, luego las hijas)
const categoriaId = {};
for (const c of lista((await api('/api/categorias')).datos)) categoriaId[c.nombre] = c.id;
for (const c of CATEGORIAS) {
  if (categoriaId[c.nombre]) { omitidos++; continue; }
  const r = await api('/api/admin/categorias', { metodo: 'POST', token, cuerpo: { nombre: c.nombre, padreId: c.padre ? categoriaId[c.padre] : null } });
  if (!r.ok) falla(`No se pudo crear la categoría ${c.nombre}`, r);
  categoriaId[c.nombre] = r.datos.id; creados.categorias++;
}

// Marcas
const marcaId = {};
for (const m of lista((await api('/api/marcas')).datos)) marcaId[m.nombre] = m.id;
for (const nombre of MARCAS) {
  if (marcaId[nombre]) { omitidos++; continue; }
  const r = await api('/api/admin/marcas', { metodo: 'POST', token, cuerpo: { nombre } });
  if (!r.ok) falla(`No se pudo crear la marca ${nombre}`, r);
  marcaId[nombre] = r.datos.id; creados.marcas++;
}

// Productos y variantes
for (const p of PRODUCTOS) {
  const busqueda = await api(`/api/productos/filtrar?texto=${encodeURIComponent(p.nombre)}&tamano=50`);
  let producto = lista(busqueda.datos).find((x) => x.nombre === p.nombre);
  if (producto) omitidos++;
  else {
    const r = await api('/api/admin/productos', {
      metodo: 'POST', token,
      cuerpo: { nombre: p.nombre, descripcion: p.desc, marcaId: marcaId[p.marca], categoriaId: categoriaId[p.cat], publicado: true },
    });
    if (!r.ok) falla(`No se pudo crear el producto ${p.nombre}`, r);
    producto = r.datos; creados.productos++;
  }
  for (const [sku, precioVenta, stock, color, medida] of p.vars) {
    const r = await api('/api/admin/variantes', {
      metodo: 'POST', token,
      cuerpo: { productoBaseId: producto.id, sku, precioVenta, stock, atributos: { Color: color, Medida: medida } },
    });
    if (r.ok) creados.variantes++;
    else if (r.estado === 409) omitidos++; // el SKU ya existía
    else falla(`No se pudo crear la variante ${sku}`, r);
  }
}

console.log(`✓ Listo. Creados: ${creados.categorias} categorías, ${creados.marcas} marcas, ${creados.productos} productos, ${creados.variantes} variantes. Omitidos (ya existían): ${omitidos}.`);
