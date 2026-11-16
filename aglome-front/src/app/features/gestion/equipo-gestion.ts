import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiError, Perfil, Rol } from '../../core/models/api.models';
import { AuthService } from '../../core/services/auth.service';
import { GestionService } from '../../core/services/gestion.service';
import { NotificacionService } from '../../core/services/notificacion.service';
import { aplicarErroresDelServidor, mensajeDeError } from '../../shared/formularios';
import { PanelLateral } from './panel-lateral';

const ROL_TEXTO: Record<Rol, string> = { ADMIN: 'Administrador', VENDEDOR: 'Vendedor', CLIENTE: 'Cliente' };

/** Personas con acceso al panel y clientes registrados. Solo ADMIN. */
@Component({
  selector: 'app-equipo-gestion',
  imports: [ReactiveFormsModule, PanelLateral],
  templateUrl: './equipo-gestion.html',
  styleUrl: './equipo-gestion.scss',
})
export class EquipoGestion {
  private readonly servicio = inject(GestionService);
  private readonly avisos = inject(NotificacionService);
  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly auth = inject(AuthService);

  protected readonly mensaje = mensajeDeError;
  protected readonly rolTexto = ROL_TEXTO;
  protected readonly usuarios = signal<Perfil[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal<'personal' | 'clientes'>('personal');
  protected readonly abierto = signal(false);
  protected readonly guardando = signal(false);
  protected readonly verClave = signal(false);

  protected readonly personal = computed(() => (this.usuarios() ?? []).filter((u) => u.rol !== 'CLIENTE'));
  protected readonly clientes = computed(() => (this.usuarios() ?? []).filter((u) => u.rol === 'CLIENTE'));
  protected readonly lista = computed(() => (this.filtro() === 'personal' ? this.personal() : this.clientes()));

  protected readonly form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    rol: ['VENDEDOR' as Rol, [Validators.required]],
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.error.set(null);
    this.servicio.usuarios().subscribe({ next: (u) => this.usuarios.set(u), error: (e: ApiError) => this.error.set(e.mensaje) });
  }

  protected abrir(): void {
    this.form.reset({ nombre: '', email: '', password: '', rol: 'VENDEDOR' });
    this.verClave.set(false);
    this.abierto.set(true);
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.guardando.set(true);
    this.servicio.crearUsuario({ nombre: v.nombre.trim(), email: v.email.trim(), password: v.password, rol: v.rol }).subscribe({
      next: (u) => {
        this.guardando.set(false);
        this.abierto.set(false);
        this.filtro.set('personal');
        this.avisos.ok(`Cuenta creada para ${u.nombre}. Pásale su correo y su contraseña inicial.`);
        this.cargar();
      },
      error: (e: ApiError) => {
        this.guardando.set(false);
        if (!aplicarErroresDelServidor(e, { nombre: this.form.controls.nombre, email: this.form.controls.email, password: this.form.controls.password })) this.avisos.mostrar(e.mensaje);
      },
    });
  }
}
