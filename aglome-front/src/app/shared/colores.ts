import { Producto } from '../core/models/api.models';

/** Colores de productos de carpintería y ferretería, para pintar la vista previa cuando no hay foto. */
const COLORES: [string, string][] = [
  ['blanco', '#f1f0ec'], ['humo', '#7c8087'], ['grafito', '#4b4f56'], ['gris', '#8b8f95'],
  ['negro', '#25272b'], ['wengue', '#4a3226'], ['chocolate', '#4e3524'], ['nogal', '#6b4a33'],
  ['roble', '#c4996a'], ['cedro', '#a8693f'], ['caoba', '#7a3b26'], ['cerezo', '#9a5237'],
  ['pino', '#dcb985'], ['natural', '#d8b98a'], ['beige', '#d9c8a9'], ['marron', '#6f4a32'], ['cafe', '#6f4a32'],
  ['azul marino', '#1f3a68'], ['marino', '#1f3a68'], ['azul', '#2f62b5'], ['celeste', '#79a8d8'],
  ['verde', '#3f7d4e'], ['rojo', '#b3342f'], ['naranja', '#d8742b'], ['amarillo', '#e2b43a'],
  ['acero', '#b3b8bf'], ['plata', '#c6cacf'], ['niquel', '#bcc0c6'], ['zinc', '#a9aeb4'], ['dorado', '#c9a24a'], ['bronce', '#9c7a47'],
  ['transparente', '#e3ecf2'], ['cristal', '#e3ecf2'],
  // colores de melamina y cantos
  ['arena', '#d6c3a3'], ['sahara', '#c9a977'], ['santorini', '#b9a48a'], ['carbono', '#3a3d42'], ['caramelo', '#b07a45'],
  ['espresso', '#3b2a20'], ['haya', '#d2a56e'], ['ceniza', '#a7a49d'], ['almendra', '#d9c09a'], ['cromado', '#c0c4ca'],
  ['maderado', '#b98a5a'], ['unicolor', '#9aa3b0'], ['calido', '#f2d9a0'], ['zincado', '#a9aeb4'],
];

function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

export function colorDe(nombre: string | null | undefined): string {
  if (!nombre) return '#c9ced6';
  const n = normalizar(nombre);
  const hallado = COLORES.find(([clave]) => n.includes(clave));
  if (hallado) return hallado[1];
  let h = 0;
  for (const c of n) h = (h * 31 + c.charCodeAt(0)) % 360;
  return `hsl(${h} 28% 62%)`;
}

export function colorDelProducto(p: Producto): string {
  const variante = (p.variantes ?? []).find((v) => v.activa && v.atributos?.['Color']);
  return colorDe(variante?.atributos['Color'] ?? p.nombre);
}

export type TipoVista = 'tablero' | 'tornillo' | 'bisagra' | 'envase' | 'general';

export function tipoDeVista(p: Producto): TipoVista {
  const t = normalizar(`${p.nombre} ${p.categoriaNombre ?? ''}`);
  if (/(melamin|mdf|mdp|trupan|plywood|osb|triplay|tablero|madera|liston|canto)/.test(t)) return 'tablero';
  if (/(tornill|perno|clavo|tarugo)/.test(t)) return 'tornillo';
  if (/(bisagra|corredera|jaladera|jalador|tirador|riel)/.test(t)) return 'bisagra';
  if (/(pegamento|silicona|barniz|pintura|lija|acabado|laca|sellador)/.test(t)) return 'envase';
  return 'general';
}
