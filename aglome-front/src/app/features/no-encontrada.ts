import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-no-encontrada',
  imports: [RouterLink],
  template: `
    <section class="contenedor vacio">
      <h1>Página no encontrada</h1>
      <p>La dirección que buscas no existe o fue movida.</p>
      <a routerLink="/productos" class="btn">Ir al catálogo</a>
    </section>
  `,
  styles: [':host .vacio { padding: 4rem 1rem; text-align: center; } p { color: #5b6070; margin-bottom: 1.5rem; }'],
})
export class NoEncontrada {}
