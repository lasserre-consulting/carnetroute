import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

function passwordMatchValidator(control: AbstractControl) {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen flex items-center justify-center py-12 px-4">
      <div class="max-w-md w-full space-y-8">
        <div class="text-center">
          <div class="text-5xl mb-4">🚗</div>
          <h1 class="text-3xl font-bold text-gray-900">Créer un compte</h1>
          <p class="mt-2 text-gray-600">Rejoignez Carnet Route</p>
        </div>

        <div class="card">
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-5">
            @if (errorMessage()) {
              <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {{ errorMessage() }}
              </div>
            }

            <div>
              <label class="label">Prénom et nom</label>
              <input type="text" formControlName="name" class="input" placeholder="Jean Dupont" autocomplete="name" />
              @if (form.get('name')?.invalid && form.get('name')?.touched) {
                <p class="mt-1 text-sm text-red-600">Le nom doit faire au moins 2 caractères</p>
              }
            </div>

            <div>
              <label class="label">Adresse email</label>
              <input type="email" formControlName="email" class="input" placeholder="vous@exemple.fr" autocomplete="email" />
              @if (form.get('email')?.invalid && form.get('email')?.touched) {
                <p class="mt-1 text-sm text-red-600">Email invalide</p>
              }
            </div>

            <div>
              <label class="label">Mot de passe</label>
              <input type="password" formControlName="password" class="input" placeholder="8 caractères minimum" autocomplete="new-password" />
              @if (form.get('password')?.invalid && form.get('password')?.touched) {
                <p class="mt-1 text-sm text-red-600">Minimum 8 caractères</p>
              }
            </div>

            <div>
              <label class="label">Confirmer le mot de passe</label>
              <input type="password" formControlName="confirmPassword" class="input" placeholder="••••••••" autocomplete="new-password" />
              @if (form.errors?.['passwordMismatch'] && form.get('confirmPassword')?.touched) {
                <p class="mt-1 text-sm text-red-600">Les mots de passe ne correspondent pas</p>
              }
            </div>

            <button type="submit" class="btn-primary w-full flex justify-center items-center gap-2" [disabled]="form.invalid || isLoading()">
              @if (isLoading()) {
                <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
              }
              Créer mon compte
            </button>
          </form>

          <p class="mt-4 text-center text-sm text-gray-600">
            Déjà un compte ?
            <a routerLink="/auth/login" class="text-primary-600 hover:text-primary-700 font-medium ml-1">Se connecter</a>
          </p>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: passwordMatchValidator });

  onSubmit() {
    if (this.form.invalid) return;
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { name, email, password } = this.form.value;
    this.authService.register({ name: name!, email: email!, password: password! }).subscribe({
      next: () => this.router.navigate(['/simulation']),
      error: (err) => {
        this.errorMessage.set(err.error?.error || 'Erreur lors de la création du compte');
        this.isLoading.set(false);
      }
    });
  }
}
