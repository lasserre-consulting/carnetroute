import { Component, inject, signal, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HistoryService, UserStats } from '../../../core/services/history.service';
import { FUEL_TYPES } from '../../../core/models/vehicle.model';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule, DecimalPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <div class="flex items-center gap-4">
        <a routerLink="/history" class="text-gray-500 hover:text-gray-700">← Trajets</a>
        <h1>📊 Mes statistiques</h1>
      </div>

      @if (isLoading()) {
        <div class="flex justify-center py-12"><div class="animate-spin text-4xl">⏳</div></div>
      } @else if (stats()) {
        <!-- Overview cards -->
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <div class="card text-center">
            <div class="text-3xl font-bold text-primary-600">{{ stats()!.totalJourneys }}</div>
            <div class="text-sm text-gray-500 mt-1">Trajets effectués</div>
          </div>
          <div class="card text-center">
            <div class="text-3xl font-bold text-gray-800">{{ stats()!.totalDistanceKm | number:'1.0-0' }} km</div>
            <div class="text-sm text-gray-500 mt-1">Distance totale</div>
          </div>
          <div class="card text-center">
            <div class="text-3xl font-bold text-emerald-600">{{ stats()!.totalCostEur | number:'1.2-2' }} €</div>
            <div class="text-sm text-gray-500 mt-1">Coût total</div>
          </div>
          <div class="card text-center">
            <div class="text-3xl font-bold text-green-600">{{ stats()!.carbonEmissionKg | number:'1.0-0' }} kg</div>
            <div class="text-sm text-gray-500 mt-1">CO₂ émis</div>
          </div>
          <div class="card text-center">
            <div class="text-3xl font-bold text-gray-800">{{ formatDuration(stats()!.totalDurationMinutes) }}</div>
            <div class="text-sm text-gray-500 mt-1">Durée totale</div>
          </div>
          @if (stats()!.mostUsedFuelType) {
            <div class="card text-center">
              <div class="text-3xl font-bold">{{ getFuelIcon(stats()!.mostUsedFuelType!) }}</div>
              <div class="text-sm text-gray-500 mt-1">{{ getFuelLabel(stats()!.mostUsedFuelType!) }}</div>
              <div class="text-xs text-gray-400">Carburant favori</div>
            </div>
          }
        </div>

        <!-- Monthly stats -->
        @if (monthlyEntries().length > 0) {
          <div class="card">
            <h2 class="mb-4">Par mois</h2>
            <div class="space-y-3">
              @for (entry of monthlyEntries(); track entry.month) {
                <div class="flex items-center gap-4 p-3 bg-gray-50 rounded-lg">
                  <div class="w-20 text-sm font-medium text-gray-600">{{ entry.month }}</div>
                  <div class="flex-1">
                    <div class="flex justify-between text-sm mb-1">
                      <span class="text-gray-600">{{ entry.journeys }} trajets · {{ entry.distanceKm | number:'1.0-0' }} km</span>
                      <span class="font-semibold text-primary-600">{{ entry.costEur | number:'1.2-2' }} €</span>
                    </div>
                  </div>
                </div>
              }
            </div>
          </div>
        }
      }
    </div>
  `
})
export class StatsComponent {
  private readonly historyService = inject(HistoryService);
  private readonly destroyRef = inject(DestroyRef);

  readonly stats = signal<UserStats | null>(null);
  readonly isLoading = signal(true);

  readonly monthlyEntries = signal<any[]>([]);

  constructor() {
    this.historyService.getStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: s => {
        this.stats.set(s);
        this.monthlyEntries.set(Object.values(s.monthlyStats).sort((a, b) => b.month.localeCompare(a.month)));
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  getFuelIcon(k: string) { return ({ SP95: '⛽', SP98: '⛽', DIESEL: '🛢️', E85: '🌿', GPL: '💨', ELECTRIC: '⚡' } as Record<string, string>)[k] ?? '⛽'; }
  getFuelLabel(k: string) { return FUEL_TYPES.find(f => f.key === k)?.label ?? k; }
  formatDuration(min: number) {
    const h = Math.floor(min / 60), m = Math.round(min % 60);
    if (h > 24) return `${Math.floor(h / 24)}j ${h % 24}h`;
    return h > 0 ? `${h}h${m.toString().padStart(2, '0')}` : `${m}min`;
  }
}
