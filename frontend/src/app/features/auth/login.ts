import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Session } from '../../core/session';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  template: `
    <main class="grid min-h-dvh place-items-center bg-base-200 px-4">
      <form class="card w-full max-w-md bg-base-100 shadow-sm" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="card-body gap-4">
          <p class="text-sm uppercase tracking-[0.18em] text-primary">Restaurante</p>
          <h1 class="font-display text-4xl">Fogón</h1>
          <p class="text-base-content/70">Inicie sesión para operar mesas, cocina, caja e inventario.</p>
          <label class="form-control">
            <span class="label-text">Correo</span>
            <input class="input input-bordered" type="email" formControlName="email" autocomplete="username" [attr.aria-invalid]="invalid('email')" />
            @if (invalid('email')) { <span class="text-sm text-error" role="alert">Indique un correo válido.</span> }
          </label>
          <label class="form-control">
            <span class="label-text">Contraseña</span>
            <input class="input input-bordered" type="password" formControlName="password" autocomplete="current-password" [attr.aria-invalid]="invalid('password')" />
            @if (invalid('password')) { <span class="text-sm text-error" role="alert">La contraseña es obligatoria.</span> }
          </label>
          @if (message()) { <div class="alert alert-error" role="alert">{{ message() }}</div> }
          <button class="btn btn-primary" type="submit" [disabled]="busy()">
            @if (busy()) { <span class="loading loading-spinner loading-sm"></span> }
            Entrar
          </button>
        </div>
      </form>
    </main>
  `,
})
export class LoginPage {
  private readonly api = inject(Api);
  private readonly session = inject(Session);
  private readonly router = inject(Router);
  readonly busy = signal(false);
  readonly message = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  invalid(name: 'email' | 'password'): boolean {
    const control = this.form.controls[name];
    return control.invalid && (control.touched || this.form.touched);
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.busy.set(true);
    this.message.set('');
    const value = this.form.getRawValue();
    this.api.login(value.email, value.password).subscribe({
      next: (response) => {
        this.session.remember(response.accessToken, response.user);
        void this.router.navigateByUrl(this.session.home());
      },
      error: (error: unknown) => {
        this.busy.set(false);
        this.message.set(errorMessage(error, 'No se pudo iniciar sesión.'));
      },
    });
  }
}
