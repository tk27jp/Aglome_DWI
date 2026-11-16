import { cpSync, existsSync, mkdirSync, readdirSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const aqui = dirname(fileURLToPath(import.meta.url));
const origen = resolve(aqui, '../dist/aglome-front/browser');
const destino = resolve(aqui, '../../aglome-tienda/src/main/resources/static');

if (!existsSync(origen)) {
  console.error('No existe ' + origen + '. Ejecuta primero: ng build');
  process.exit(1);
}
if (!existsSync(resolve(aqui, '../../aglome-tienda/pom.xml'))) {
  console.error('No encuentro la carpeta aglome-tienda al lado de aglome-front. Revisa las rutas de este script.');
  process.exit(1);
}

mkdirSync(destino, { recursive: true });
for (const nombre of readdirSync(destino)) rmSync(join(destino, nombre), { recursive: true, force: true });
cpSync(origen, destino, { recursive: true });
console.log('Front copiado a ' + destino);
