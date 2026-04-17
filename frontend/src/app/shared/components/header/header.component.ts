import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-40 shadow-xs transition-colors duration-200">
      <div class="container mx-auto px-4 max-w-5xl">
        <div class="flex items-center justify-between h-16">
          <!-- Logo -->
          <a routerLink="/simulation" class="font-bold text-xl text-primary-600 dark:text-primary-400">
            Carnet Route
          </a>

          <!-- Desktop nav -->
          <nav class="hidden sm:flex items-center gap-1">
            @if (isAuthenticated()) {
              <a routerLink="/vehicles" routerLinkActive="text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/30"
                 class="px-3 py-2 rounded-lg text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-900/30 transition-colors">
                🚗 Véhicules
              </a>
              <a routerLink="/history" routerLinkActive="text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-900/30"
                 class="px-3 py-2 rounded-lg text-sm font-medium text-gray-600 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-400 hover:bg-primary-50 dark:hover:bg-primary-900/30 transition-colors">
                📋 Trajets
              </a>
            }
          </nav>

          <!-- Right side: theme toggle + auth -->
          <div class="flex items-center gap-3">
            <!-- Dark mode toggle -->
            <button
              type="button"
              (click)="themeService.toggle()"
              [title]="themeService.isDark() ? 'Passer en mode clair' : 'Passer en mode sombre'"
              class="relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200 focus:outline-hidden focus:ring-2 focus:ring-primary-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900"
              [class.bg-primary-600]="themeService.isDark()"
              [class.bg-gray-200]="!themeService.isDark()"
            >
              <span
                class="inline-block h-4 w-4 transform rounded-full bg-white shadow-sm transition-transform duration-200"
                [class.translate-x-6]="themeService.isDark()"
                [class.translate-x-1]="!themeService.isDark()"
              ></span>
              <span class="sr-only">{{ themeService.isDark() ? '☀️' : '🌙' }}</span>
            </button>
            <span class="text-sm" aria-hidden="true">{{ themeService.isDark() ? '🌙' : '☀️' }}</span>

            @if (isAuthenticated()) {
              <span class="hidden sm:block text-sm text-gray-600 dark:text-gray-300">{{ currentUser()?.name }}</span>
              <button (click)="logout()" class="btn-secondary text-sm py-1 px-3">Déconnexion</button>
            } @else {
              <a routerLink="/auth/login" class="btn-secondary text-sm py-1 px-3">Connexion</a>
              <a routerLink="/auth/register" class="btn-primary text-sm py-1 px-3">Inscription</a>
            }
          </div>
        </div>
      </div>
    </header>
  `
})
export class HeaderComponent {
  private readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);
  readonly isAuthenticated = this.authService.isAuthenticated;
  readonly currentUser = this.authService.currentUser;

  logout() { this.authService.logout(); }
}
