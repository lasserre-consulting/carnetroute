import { Component, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen flex items-center justify-center py-12 px-4">
      <div class="max-w-md w-full space-y-8">
        <!-- Logo & title -->
        <div class="text-center">
          <div class="text-5xl mb-4">🚗</div>
          <h1 class="text-3xl font-bold text-gray-900">Carnet Route</h1>
          <p class="mt-2 text-gray-600">Connectez-vous à votre compte</p>
        </div>

        <!-- Card -->
        <div class="card">
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-5">
            <!-- Error message -->
            @if (errorMessage()) {
              <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {{ errorMessage() }}
              </div>
            }

            <!-- Email -->
            <div>
              <label for="email" class="label">Adresse email</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                class="input"
                placeholder="vous@exemple.fr"
                autocomplete="email"
              />
              @if (form.get('email')?.invalid && form.get('email')?.touched) {
                <p class="mt-1 text-sm text-red-600">Email invalide</p>
              }
            </div>

            <!-- Password -->
            <div>
              <label for="password" class="label">Mot de passe</label>
              <input
                id="password"
                type="password"
                formControlName="password"
                class="input"
                placeholder="••••••••"
                autocomplete="current-password"
              />
              @if (form.get('password')?.invalid && form.get('password')?.touched) {
                <p class="mt-1 text-sm text-red-600">Mot de passe requis</p>
              }
            </div>

            <!-- Submit -->
            <button
              type="submit"
              class="btn-primary w-full flex justify-center items-center gap-2"
              [disabled]="form.invalid || isLoading()"
            >
              @if (isLoading()) {
                <svg class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
              }
              Se connecter
            </button>
          </form>

          <p class="mt-4 text-center text-sm text-gray-600">
            Pas encore de compte ?
            <a routerLink="/auth/register" class="text-primary-600 hover:text-primary-700 font-medium ml-1">
              Créer un compte
            </a>
          </p>
        </div>

        <!-- Guest mode -->
        <div class="text-center">
          <button (click)="continueAsGuest()" class="text-sm text-gray-500 hover:text-gray-700 underline">
            Continuer sans compte
          </button>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  onSubmit() {
    if (this.form.invalid) return;
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.value;
    this.authService.login({ email: email!, password: password! }).subscribe({
      next: () => this.router.navigate(['/simulation']),
      error: (err) => {
        this.errorMessage.set(err.error?.error || 'Email ou mot de passe incorrect');
        this.isLoading.set(false);
      }
    });
  }

  continueAsGuest() {
    this.router.navigate(['/simulation']);
  }
}
