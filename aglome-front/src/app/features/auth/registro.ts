import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { aplicarErroresDelServidor, igualA, letrasYNumeros, mensajeDeError } from '../../shared/formularios';

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registro.html',
  styleUrl: './auth.scss',
})
export class Registro {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly avisos = inject(NotificacionService);

  readonly retorno = input<string>();

  protected readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72), letrasYNumeros]],
    repetir: ['', [Validators.required, igualA('password')]],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verClave = signal(false);
  protected readonly mensaje = mensajeDeError;

  constructor() {
    // si cambia la contraseña, hay que volver a comparar la repetición
    this.form.controls.password.valueChanges.subscribe(() => this.form.controls.repetir.updateValueAndValidity());
  }

  protected enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { nombre, email, password } = this.form.getRawValue();
    this.auth.registro(nombre.trim(), email.trim(), password).subscribe({
      next: (r) => {
        this.avisos.ok(`Cuenta creada. Bienvenido, ${r.nombre.split(' ')[0]}.`);
        const r2 = this.retorno();
        this.router.navigateByUrl(r2 && r2.startsWith('/') && !r2.startsWith('//') ? r2 : '/');
      },
      error: (e: ApiError) => {
        this.enviando.set(false);
        if (e.status === 409) {
          this.form.controls.email.setErrors({ servidor: e.mensaje });
          this.form.controls.email.markAsTouched();
        } else if (!aplicarErroresDelServidor(e, { nombre: this.form.controls.nombre, email: this.form.controls.email, password: this.form.controls.password })) {
          this.error.set(e.mensaje);
        }
      },
    });
  }
}
