import { Component, signal, output, inject, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SimulationService } from '../../../core/services/simulation.service';

export interface TrafficSelection {
  mode: 'manual' | 'auto';
  factor: number;
  departureHour?: number;
  departureDay?: number;
}

@Component({
  selector: 'app-traffic',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-4">
      <div class="flex gap-2">
        <button type="button" (click)="setMode('manual')" [class]="tabClass('manual')">Manuel</button>
        <button type="button" (click)="setMode('auto')" [class]="tabClass('auto')">Par heure</button>
      </div>

      @if (mode() === 'manual') {
        <div>
          <div class="flex justify-between items-center mb-2">
            <label class="text-sm font-medium text-gray-700 dark:text-gray-300">Condition de trafic</label>
            <span class="badge" [class]="getFactorBadgeClass()">{{ getFactorLabel() }}</span>
          </div>
          <input
            type="range" min="1" max="2" step="0.1"
            [(ngModel)]="manualFactor"
            (ngModelChange)="onManualFactorChange($event)"
            class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-600"
          />
          <div class="flex justify-between text-xs text-gray-500 dark:text-gray-400 mt-1">
            <span>Fluide</span><span>Embouteillages</span>
          </div>
        </div>
      }

      @if (mode() === 'auto') {
        <div>
          <p class="text-sm text-gray-600 dark:text-gray-400 mb-3">Cliquez sur l'heure de départ pour estimer le trafic :</p>
          @if (heatmapMatrix().length > 0) {
            <div class="overflow-x-auto">
              <div class="grid gap-0.5 min-w-max" style="grid-template-columns: 48px repeat(24, 20px)">
                <!-- Header hours -->
                <div class="text-xs text-gray-400"></div>
                @for (h of hours; track h) {
                  <div class="text-xs text-gray-400 dark:text-gray-500 text-center" style="font-size: 9px">{{ h }}</div>
                }
                <!-- Days rows -->
                @for (day of days; track day.idx) {
                  <div class="text-xs text-gray-600 dark:text-gray-400 flex items-center font-medium pr-1" style="font-size: 10px">{{ day.short }}</div>
                  @for (h of hours; track h) {
                    <div
                      class="w-5 h-5 rounded-xs cursor-pointer transition-transform hover:scale-125"
                      [style.background]="getCellColor(heatmapMatrix()[day.idx][h])"
                      [class.ring-2]="selectedDay() === day.idx && selectedHour() === h"
                      [class.ring-primary-500]="selectedDay() === day.idx && selectedHour() === h"
                      [title]="'Facteur ' + heatmapMatrix()[day.idx][h].toFixed(1)"
                      (click)="selectCell(day.idx, h)"
                    ></div>
                  }
                }
              </div>
            </div>
            @if (selectedDay() !== null) {
              <p class="mt-2 text-sm text-primary-600 font-medium">
                ✅ {{ days[selectedDay()!].label }} {{ selectedHour()! }}h → facteur {{ selectedFactor().toFixed(1) }}
                ({{ getFactorLabelForValue(selectedFactor()) }})
              </p>
            }
          } @else {
            <div class="flex justify-center py-4">
              <div class="animate-spin text-2xl">⏳</div>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class TrafficComponent {
  private readonly simService = inject(SimulationService);
  private readonly destroyRef = inject(DestroyRef);

  readonly trafficChanged = output<TrafficSelection>();

  readonly mode = signal<'manual' | 'auto'>('manual');
  readonly heatmapMatrix = signal<number[][]>([]);
  readonly selectedDay = signal<number | null>(null);
  readonly selectedHour = signal<number | null>(null);
  readonly selectedFactor = signal(1.0);

  manualFactor = 1.0;

  readonly hours = Array.from({ length: 24 }, (_, i) => i);
  readonly days = [
    { idx: 0, short: 'Lun', label: 'Lundi' }, { idx: 1, short: 'Mar', label: 'Mardi' },
    { idx: 2, short: 'Mer', label: 'Mercredi' }, { idx: 3, short: 'Jeu', label: 'Jeudi' },
    { idx: 4, short: 'Ven', label: 'Vendredi' }, { idx: 5, short: 'Sam', label: 'Samedi' },
    { idx: 6, short: 'Dim', label: 'Dimanche' }
  ];

  constructor() {
    this.simService.getHeatmap().pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(r => this.heatmapMatrix.set(r.matrix));
  }

  setMode(m: 'manual' | 'auto') {
    this.mode.set(m);
    if (m === 'manual') this.emit();
  }

  onManualFactorChange(v: number) {
    this.manualFactor = v;
    this.emit();
  }

  selectCell(day: number, hour: number) {
    this.selectedDay.set(day);
    this.selectedHour.set(hour);
    const factor = this.heatmapMatrix()[day]?.[hour] ?? 1.0;
    this.selectedFactor.set(factor);
    this.trafficChanged.emit({ mode: 'auto', factor, departureDay: day + 1, departureHour: hour });
  }

  private emit() {
    this.trafficChanged.emit({ mode: 'manual', factor: this.manualFactor });
  }

  tabClass(m: string) {
    const base = 'px-4 py-2 rounded-lg text-sm font-medium transition-colors';
    return `${base} ${this.mode() === m ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`;
  }

  getFactorLabel() { return this.getFactorLabelForValue(this.manualFactor); }
  getFactorLabelForValue(f: number) {
    if (f <= 1.1) return 'Fluide';
    if (f <= 1.3) return 'Normal';
    if (f <= 1.6) return 'Chargé';
    if (f <= 1.8) return 'Dense';
    return 'Embouteillages';
  }

  getFactorBadgeClass() {
    const f = this.manualFactor;
    if (f <= 1.1) return 'badge bg-green-100 text-green-800';
    if (f <= 1.3) return 'badge bg-yellow-100 text-yellow-800';
    if (f <= 1.6) return 'badge bg-orange-100 text-orange-800';
    return 'badge bg-red-100 text-red-800';
  }

  getCellColor(factor: number): string {
    const t = (factor - 1.0) / 1.0;
    const r = Math.round(34 + t * (220 - 34));
    const g = Math.round(197 - t * (197 - 38));
    const b = Math.round(94 - t * 94);
    return `rgb(${r},${g},${b})`;
  }
}
