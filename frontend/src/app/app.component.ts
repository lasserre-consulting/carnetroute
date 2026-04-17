import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent],
  template: `
    <div class="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col transition-colors duration-200">
      <app-header />
      <main class="flex-1 container mx-auto px-4 py-6 max-w-5xl">
        <router-outlet />
      </main>
      <footer class="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 py-4 text-center text-sm text-gray-500 dark:text-gray-400 transition-colors duration-200">
        Carnet Route — Simulateur de trajet
      </footer>
    </div>
  `
})
export class AppComponent {}
