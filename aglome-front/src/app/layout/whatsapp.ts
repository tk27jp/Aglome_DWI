import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { negocio } from '../../environments/negocio';

/** Botón flotante de WhatsApp. Solo aparece si en environments/negocio.ts se configuró el número. */
@Component({
  selector: 'app-whatsapp',
  template: `
    @if (negocio.whatsapp && !enPanel()) {
      <a class="wa" [href]="enlace" target="_blank" rel="noopener" aria-label="Escríbenos por WhatsApp">
        <svg viewBox="0 0 32 32" width="30" height="30" fill="currentColor" aria-hidden="true"><path d="M16 3C9 3 3.3 8.6 3.3 15.6c0 2.4.7 4.7 1.9 6.6L3 29l7-1.8a12.8 12.8 0 0 0 6 1.5c7 0 12.7-5.600 12.7-12.600S23 3 16 3zm0 23.200a10.600 10.600 0 0 1-5.400-1.500l-.4-.2-4.100 1.100 1.100-4-.3-.4a10.400 10.400 0 0 1-1.600-5.500C5.300 9.800 10.100 5.200 16 5.200s10.700 4.600 10.700 10.400S21.900 26.200 16 26.200zm5.900-7.800c-.3-.2-1.900-.9-2.200-1-.3-.1-.5-.2-.7.200-.2.300-.8 1-1 1.200-.2.200-.4.200-.7.100a8.700 8.700 0 0 1-4.300-3.700c-.3-.6.300-.5.900-1.700.1-.2 0-.4 0-.5l-1-2.300c-.2-.6-.5-.5-.7-.5h-.6c-.2 0-.6.100-.9.400-.3.300-1.200 1.200-1.200 2.800s1.200 3.300 1.400 3.500c.2.200 2.400 3.700 5.800 5.100 2.200.9 3 1 4.100.8.700-.1 1.900-.8 2.200-1.600.3-.8.300-1.400.2-1.600-.1-.1-.3-.2-.6-.4z"/></svg>
        <span class="etiqueta">¿Dudas? Escríbenos</span>
      </a>
    }
  `,
  styleUrl: './whatsapp.scss',
})
export class Whatsapp {
  protected readonly negocio = negocio;
  private readonly router = inject(Router);
  /** Dentro del panel de gestión el botón estorba: es para los clientes. */
  protected readonly enPanel = toSignal(
    this.router.events.pipe(filter((e) => e instanceof NavigationEnd), startWith(null), map(() => this.router.url.startsWith('/gestion'))),
    { initialValue: false },
  );
  protected readonly enlace = `https://wa.me/${negocio.whatsapp}?text=${encodeURIComponent('Hola, quiero consultar por un producto de Aglome.')}`;
}
