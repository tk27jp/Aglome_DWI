import { Component, input } from '@angular/core';

/** Logo oficial de Aglome (public/logo-aglome.png). El tamaño es el alto en píxeles. */
@Component({
  selector: 'app-logo',
  template: `<img src="logo-aglome.png" alt="Logo de Aglome" [style.height.px]="tamano()" />`,
  styles: [':host { display: inline-flex; } img { display: block; width: auto; }'],
})
export class Logo {
  readonly tamano = input(36);
}
