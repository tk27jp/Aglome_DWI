import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiError, Perfil } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { igualA, letrasYNumeros, mensajeDeError } from '../../shared/formularios';

const ROLES: Record<string, string> = { ADMIN: 'Administrador', VENDEDOR: 'Vendedor', CLIENTE: 'Cliente' };

@Component({
  selector: 'app-cuenta',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './cuenta.html',
  styleUrl: './cuenta.scss',
})
export class Cuenta {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(NotificacionService);

  protected readonly perfil = signal<Perfil | null>(null);
  protected readonly errorPerfil = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly mensaje = mensajeDeError;
  protected readonly etiquetaRol = (r: string) => ROLES[r] ?? r;

  protected readonly form = this.fb.nonNullable.group({
    actual: ['', [Validators.required]],
    nueva: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72), letrasYNumeros]],
    repetir: ['', [Validators.required, igualA('nueva')]],
  });

  constructor() {
    this.auth.perfil().subscribe({
      next: (p) => this.perfil.set(p),
      error: (e: ApiError) => this.errorPerfil.set(e.mensaje),
    });
    this.form.controls.nueva.valueChanges.subscribe(() => this.form.controls.repetir.updateValueAndValidity());
  }

  protected enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { actual, nueva } = this.form.getRawValue();
    if (actual === nueva) {
      this.form.controls.nueva.setErrors({ distintaDeLaActual: true });
      this.form.controls.nueva.markAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    this.auth.cambiarContrasena(actual, nueva).subscribe({
      next: () => {
        this.enviando.set(false);
        this.form.reset();
        this.avisos.ok('Contraseña actualizada.');
      },
      error: (e: ApiError) => {
        this.enviando.set(false);
        this.error.set(e.mensaje);
      },
    });
  }
}
