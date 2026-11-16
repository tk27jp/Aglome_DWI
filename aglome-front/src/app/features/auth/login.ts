import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { mensajeDeError } from '../../shared/formularios';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './auth.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly avisos = inject(NotificacionService);

  /** Página a la que volver después de ingresar (viene en la URL: ?retorno=/carrito). */
  readonly retorno = input<string>();

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verClave = signal(false);
  protected readonly mensaje = mensajeDeError;

  protected enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: (r) => {
        this.avisos.ok(`Bienvenido, ${r.nombre.split(' ')[0]}.`);
        this.router.navigateByUrl(this.destino());
      },
      error: (e: ApiError) => {
        this.enviando.set(false);
        this.error.set(e.mensaje);
        this.form.controls.password.reset('');
      },
    });
  }

  /** Solo se acepta una ruta interna: evita que un enlace manipulado envíe a otro sitio. */
  private destino(): string {
    const r = this.retorno();
    if (r && r.startsWith('/') && !r.startsWith('//')) return r;
    // El personal de la tienda entra directo a su panel.
    return this.auth.esPersonal() ? '/gestion' : '/';
  }
}
