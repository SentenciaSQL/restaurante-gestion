import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { ROLE, label } from '../../core/labels';
import { Branch, RoleView, UserView } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule],
  templateUrl: './users.html',
})
export class UsersPage {
  readonly session = inject(Session);
  private readonly api = inject(Api);
  readonly rows = signal<UserView[]>([]);
  readonly roles = signal<RoleView[]>([]);
  readonly branches = signal<Branch[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly query = signal('');
  readonly selectedRoles = signal<string[]>([]);
  readonly form = inject(FormBuilder).nonNullable.group({
    id: [null as number | null],
    fullName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    password: [''],
    branchId: [null as number | null],
    active: [true],
  });
  label = label;
  roleNames = ROLE;

  constructor() {
    this.api.roles().subscribe({ next: (rows) => this.roles.set(rows) });
    this.api.branches().subscribe({ next: (rows) => this.branches.set(rows) });
    this.reload();
  }

  reload(): void {
    this.api.users(this.query(), this.page()).subscribe({
      next: (page) => { this.rows.set(page.items); this.total.set(page.total); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  search(): void { this.page.set(0); this.reload(); }

  edit(row: UserView): void {
    this.form.patchValue({ ...row, phone: row.phone ?? '', password: '' });
    this.selectedRoles.set([...row.roles]);
  }

  toggleRole(code: string): void {
    if (code === 'ADMIN' && !this.session.profile()?.roles.includes('ADMIN')) return;
    this.selectedRoles.update((codes) => (codes.includes(code) ? codes.filter((item) => item !== code) : [...codes, code]));
  }

  save(): void {
    if (this.form.invalid || !this.selectedRoles().length) {
      this.session.toast('error', 'Complete los datos y elija al menos un rol.');
      return;
    }
    const value = this.form.getRawValue();
    if (!value.id && !value.password) {
      this.session.toast('error', 'La contraseña inicial es obligatoria.');
      return;
    }
    this.api.saveUser(value.id, {
      email: value.email,
      fullName: value.fullName,
      phone: value.phone,
      password: value.password,
      branchId: value.branchId,
      roleCodes: this.selectedRoles(),
    }).subscribe({
      next: () => { this.session.toast('success', 'Usuario guardado.'); this.form.reset({ id: null, fullName: '', email: '', phone: '', password: '', branchId: null, active: true }); this.selectedRoles.set([]); this.reload(); },
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  async setActive(row: UserView, active: boolean): Promise<void> {
    if (!active) {
      const ok = await this.session.confirm('Desactivar usuario', `${row.fullName} no podrá iniciar sesión.`, 'Desactivar');
      if (!ok) return;
    }
    this.api.setUserActive(row.id, active).subscribe({
      next: () => this.reload(),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }
}
