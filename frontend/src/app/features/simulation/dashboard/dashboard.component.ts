import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { Simulation } from '../../../core/models/simulation.model';
import { FUEL_TYPES } from '../../../core/models/vehicle.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (simulation()) {
      <div class="space-y-4 animate-in fade-in duration-300">
        <!-- Main metrics -->
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div class="card text-center">
            <div class="text-2xl font-bold text-primary-600 dark:text-primary-400">{{ simulation()!.route.distanceKm | number:'1.0-0' }} km</div>
            <div class="text-sm text-gray-500 dark:text-gray-400 mt-1">Distance</div>
          </div>
          <div class="card text-center">
            <div class="text-2xl font-bold text-gray-800 dark:text-gray-100">{{ formatDuration(simulation()!.costs.durationAdjustedMinutes) }}</div>
            <div class="text-sm text-gray-500 dark:text-gray-400 mt-1">Durée estimée</div>
          </div>
          <div class="card text-center">
            <div class="text-2xl font-bold text-emerald-600 dark:text-emerald-400">{{ simulation()!.costs.fuelConsumedTotal | number:'1.1-1' }} {{ getUnit() }}</div>
            <div class="text-sm text-gray-500 dark:text-gray-400 mt-1">Conso.</div>
          </div>
          <div class="card text-center bg-primary-600 dark:bg-primary-700 text-white border-0">
            <div class="text-2xl font-bold">{{ simulation()!.costs.costTotal | number:'1.2-2' }} €</div>
            <div class="text-sm text-primary-200 mt-1">Coût total</div>
          </div>
        </div>

        <!-- Comparison table -->
        <div class="card">
          <h3 class="mb-4">Comparatif carburants</h3>
          <div class="space-y-2">
            @for (entry of getSortedComparison(); track entry.fuelType) {
              <div [class]="getRowClass(entry.fuelType)">
                <span class="text-lg w-8">{{ getFuelIcon(entry.fuelType) }}</span>
                <div class="flex-1">
                  <div class="flex justify-between items-center">
                    <span class="font-medium text-sm text-gray-800 dark:text-gray-200">{{ getFuelLabel(entry.fuelType) }}</span>
                    <span class="font-bold text-gray-900 dark:text-white">{{ entry.totalCost | number:'1.2-2' }} €</span>
                  </div>
                  <div class="flex justify-between text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                    <span>{{ entry.consumptionPer100km | number:'1.1-1' }} {{ entry.unit }}/100km · {{ entry.pricePerUnit }} €/{{ entry.unit }}</span>
                    <span>{{ entry.fuelConsumed | number:'1.1-1' }} {{ entry.unit }}</span>
                  </div>
                  <div class="mt-1.5 bg-gray-200 dark:bg-gray-600 rounded-full h-1.5">
                    <div class="h-1.5 rounded-full bg-primary-500"
                         [style.width.%]="getCostPercent(entry.totalCost)"></div>
                  </div>
                </div>
                @if (entry.fuelType === simulation()!.costs.fuelType) {
                  <span class="text-primary-600 dark:text-primary-400 text-lg">✓</span>
                }
              </div>
            }
          </div>
        </div>
      </div>
    }
  `
})
export class DashboardComponent {
  readonly simulation = input<Simulation | null>(null);

  private get comparison() {
    return Object.values(this.simulation()?.costs?.comparison ?? {});
  }

  getSortedComparison() {
    return [...this.comparison].sort((a, b) => a.totalCost - b.totalCost);
  }

  getMaxCost() { return Math.max(...this.comparison.map(e => e.totalCost)); }
  getCostPercent(cost: number) { const max = this.getMaxCost(); return max ? (cost / max) * 100 : 0; }

  getUnit() {
    const ft = FUEL_TYPES.find(f => f.key === this.simulation()?.costs?.fuelType);
    return ft?.unit ?? 'L';
  }

  formatDuration(minutes: number) {
    const h = Math.floor(minutes / 60);
    const m = Math.round(minutes % 60);
    return h > 0 ? `${h}h${m.toString().padStart(2, '0')}` : `${m}min`;
  }

  getFuelIcon(key: string) {
    const icons: Record<string, string> = { SP95: '⛽', SP98: '⛽', DIESEL: '🛢️', E85: '🌿', GPL: '💨', ELECTRIC: '⚡' };
    return icons[key] || '⛽';
  }

  getFuelLabel(key: string) {
    return FUEL_TYPES.find(f => f.key === key)?.label ?? key;
  }

  getRowClass(fuelType: string): string {
    const base = 'flex items-center gap-3 p-3 rounded-lg transition-colors';
    const selected = fuelType === this.simulation()?.costs?.fuelType;
    return selected
      ? `${base} bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-700`
      : `${base} hover:bg-gray-50 dark:hover:bg-gray-700/50`;
  }
}
