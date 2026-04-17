import { Component, inject, signal, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HistoryService, JourneyHistory } from '../../core/services/history.service';
import { FUEL_TYPES } from '../../core/models/vehicle.model';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, DecimalPipe, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1>📋 Mes trajets</h1>
        <a routerLink="/stats" class="btn-secondary text-sm">📊 Statistiques</a>
      </div>

      @if (isLoading()) {
        <div class="flex justify-center py-12"><div class="animate-spin text-4xl">⏳</div></div>
      } @else if (journeys().length === 0) {
        <div class="card text-center py-12">
          <div class="text-5xl mb-4">📋</div>
          <h2 class="text-gray-600">Aucun trajet enregistré</h2>
          <p class="text-gray-400 mt-2">Vos simulations sauvegardées apparaîtront ici</p>
          <a routerLink="/simulation" class="btn-primary inline-block mt-4">Faire une simulation</a>
        </div>
      } @else {
        <div class="space-y-3">
          @for (journey of journeys(); track journey.id) {
            <div class="card">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-3">
                  <span class="text-2xl">{{ getFuelIcon(journey.fuelType) }}</span>
                  <div>
                    <div class="font-semibold text-gray-900">{{ journey.distanceKm | number:'1.0-0' }} km</div>
                    <div class="text-sm text-gray-500">{{ journey.createdAt | date:'dd/MM/yyyy HH:mm' }}</div>
                  </div>
                </div>
                <div class="text-right">
                  <div class="font-bold text-primary-600 text-lg">{{ journey.costTotal | number:'1.2-2' }} €</div>
                  <div class="text-sm text-gray-500">{{ formatDuration(journey.durationMinutes) }}</div>
                </div>
              </div>
              <div class="flex gap-4 mt-3 pt-3 border-t border-gray-100 text-xs text-gray-500">
                <span>⛽ {{ getFuelLabel(journey.fuelType) }}</span>
                <span>🌱 {{ journey.carbonEmissionKg | number:'1.1-1' }} kg CO₂</span>
                @if (journey.tags.length > 0) {
                  @for (tag of journey.tags; track tag) {
                    <span class="badge bg-gray-100 text-gray-600">{{ tag }}</span>
                  }
                }
              </div>
            </div>
          }
        </div>
        <!-- Pagination -->
        @if (totalElements() > pageSize) {
          <div class="flex justify-center gap-2">
            <button class="btn-secondary text-sm" [disabled]="page() === 0" (click)="loadPage(page() - 1)">← Précédent</button>
            <span class="flex items-center text-sm text-gray-600">Page {{ page() + 1 }}</span>
            <button class="btn-secondary text-sm" [disabled]="(page() + 1) * pageSize >= totalElements()" (click)="loadPage(page() + 1)">Suivant →</button>
          </div>
        }
      }
    </div>
  `
})
export class HistoryComponent {
  private readonly historyService = inject(HistoryService);
  private readonly destroyRef = inject(DestroyRef);

  readonly journeys = signal<JourneyHistory[]>([]);
  readonly isLoading = signal(true);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;

  constructor() { this.loadPage(0); }

  loadPage(p: number) {
    this.page.set(p);
    this.isLoading.set(true);
    this.historyService.getHistory(p, this.pageSize).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: r => { this.journeys.set(r.content); this.totalElements.set(r.totalElements); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  getFuelIcon(k: string) { return ({ SP95: '⛽', SP98: '⛽', DIESEL: '🛢️', E85: '🌿', GPL: '💨', ELECTRIC: '⚡' } as Record<string, string>)[k] ?? '⛽'; }
  getFuelLabel(k: string) { return FUEL_TYPES.find(f => f.key === k)?.label ?? k; }
  formatDuration(min: number) { const h = Math.floor(min / 60), m = Math.round(min % 60); return h > 0 ? `${h}h${m.toString().padStart(2, '0')}` : `${m}min`; }
}
