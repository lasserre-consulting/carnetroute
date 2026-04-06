import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SimulationResult, FuelProfile } from '../../models/simulation.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid-results" *ngIf="result">
      <!-- Left: Key metrics -->
      <div class="card">
        <div class="card-title">📊 Résultats</div>

        <div class="metric-grid">
          <div class="metric-box">
            <div class="metric-value heading" style="color: #06B6D4">{{ result.distanceKm | number:'1.0-0' }}</div>
            <div class="metric-label">km</div>
          </div>
          <div class="metric-box">
            <div class="metric-value heading" [style.color]="getTrafficColor(result.trafficFactor)">
              {{ formatDuration(result.adjustedTimeMin) }}
            </div>
            <div class="metric-label">durée estimée</div>
          </div>
        </div>

        <div class="detail-rows">
          <div class="detail-row">
            <span>⏱ Fluide</span>
            <span class="detail-value" style="color: #22C55E">{{ formatDuration(result.baseTimeMin) }}</span>
          </div>
          <div class="detail-row">
            <span>{{ result.trafficIcon }} {{ result.trafficLabel }}</span>
            <span class="detail-value" [style.color]="getTrafficColor(result.trafficFactor)">
              {{ formatDuration(result.adjustedTimeMin) }}
            </span>
          </div>
          <div class="detail-row">
            <span>{{ activeFuel?.icon }} Conso</span>
            <span class="detail-value" [style.color]="activeFuel?.color">
              {{ result.fuelConsumed | number:'1.1-1' }} {{ result.fuelUnit }}
            </span>
          </div>
        </div>

        <div class="total-cost" [style.background]="(activeFuel?.color || '#06B6D4') + '12'"
          [style.borderColor]="(activeFuel?.color || '#06B6D4') + '30'">
          <div class="cost-label">Coût total</div>
          <div class="cost-value heading" [style.color]="activeFuel?.color">
            {{ result.totalCost | number:'1.2-2' }} €
          </div>
          <div class="cost-sub">{{ result.costPer100km | number:'1.2-2' }} € / 100 km</div>
        </div>
      </div>

      <!-- Right: Comparison -->
      <div class="card">
        <div class="card-title">⚖️ Comparatif</div>

        <div class="comparison-bars">
          <div *ngFor="let item of result.comparison" class="comp-item">
            <div class="comp-header">
              <span class="comp-label">{{ item.icon }} {{ item.label }}</span>
              <span class="comp-cost" [style.color]="item.color">{{ item.cost | number:'1.2-2' }} €</span>
            </div>
            <div class="comp-bar-bg">
              <div class="comp-bar-fill"
                [style.width.%]="getBarWidth(item.cost)"
                [style.background]="'linear-gradient(90deg, ' + item.color + '90, ' + item.color + ')'"
                [style.boxShadow]="'0 0 8px ' + item.color + '40'">
              </div>
            </div>
          </div>
        </div>

        <div class="savings-box">
          <div class="savings-label">Économie max</div>
          <div class="savings-row" *ngIf="cheapest && mostExpensive">
            <span class="savings-icon">{{ cheapest.icon }}</span>
            <div>
              <div class="savings-value" style="color: #22C55E">
                -{{ (mostExpensive.cost - cheapest.cost) | number:'1.2-2' }} €
              </div>
              <div class="savings-sub">avec {{ cheapest.label }}</div>
            </div>
          </div>
        </div>

        <div class="gauges">
          <div class="gauge-item">
            <svg width="100" height="60" viewBox="0 0 100 60">
              <path d="M 10 55 A 40 40 0 0 1 90 55" fill="none" stroke="#1e293b" stroke-width="8" stroke-linecap="round"/>
              <path d="M 10 55 A 40 40 0 0 1 90 55" fill="none" stroke="#06B6D4" stroke-width="8" stroke-linecap="round"
                [attr.stroke-dasharray]="getGaugeDash(result.avgSpeedKmh, 130)" style="filter: drop-shadow(0 0 6px rgba(6,182,212,0.25))"/>
            </svg>
            <div class="gauge-value heading" style="color: #06B6D4">{{ result.avgSpeedKmh | number:'1.1-1' }} <span class="gauge-unit">km/h</span></div>
            <div class="gauge-label">Vit. moy.</div>
          </div>
          <div class="gauge-item">
            <svg width="100" height="60" viewBox="0 0 100 60">
              <path d="M 10 55 A 40 40 0 0 1 90 55" fill="none" stroke="#1e293b" stroke-width="8" stroke-linecap="round"/>
              <path d="M 10 55 A 40 40 0 0 1 90 55" fill="none" [attr.stroke]="activeFuel?.color || '#06B6D4'" stroke-width="8" stroke-linecap="round"
                [attr.stroke-dasharray]="getGaugeDash(result.costPerHour, 50)"/>
            </svg>
            <div class="gauge-value heading" [style.color]="activeFuel?.color">{{ result.costPerHour | number:'1.1-1' }} <span class="gauge-unit">€/h</span></div>
            <div class="gauge-label">Coût/h</div>
          </div>
        </div>

        <div class="info-box mono">
          ℹ️ Prix avril 2026 · Dist. ×1.35 · 90 km/h moy.
        </div>
      </div>
    </div>
  `,
  styles: [`
    .metric-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
    .metric-box { text-align: center; padding: 12px; border-radius: 12px; background: #0F172A; }
    .metric-value { font-size: 24px; font-weight: 700; }
    .metric-label { font-size: 11px; color: #64748B; }

    .detail-rows { display: flex; flex-direction: column; gap: 10px; }
    .detail-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: 8px 10px; border-radius: 8px; background: #0F172A;
      font-size: 12px; color: #94A3B8;
    }
    .detail-value { font-size: 14px; font-weight: 600; }

    .total-cost {
      margin-top: 16px; padding: 16px; border-radius: 12px;
      text-align: center; border: 1px solid;
    }
    .cost-label { font-size: 11px; color: #94A3B8; margin-bottom: 4px; }
    .cost-value { font-size: 28px; font-weight: 700; }
    .cost-sub { font-size: 11px; color: #64748B; margin-top: 4px; }

    .comparison-bars { display: flex; flex-direction: column; gap: 12px; }
    .comp-item {}
    .comp-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
    .comp-label { font-size: 11px; color: #94A3B8; }
    .comp-cost { font-size: 13px; font-weight: 700; }
    .comp-bar-bg { height: 8px; background: #1E293B; border-radius: 4px; overflow: hidden; }
    .comp-bar-fill { height: 100%; border-radius: 4px; transition: width 0.7s ease; }

    .savings-box { margin-top: 16px; padding: 12px; border-radius: 12px; background: #0F172A; }
    .savings-label { font-size: 11px; color: #94A3B8; margin-bottom: 8px; }
    .savings-row { display: flex; align-items: center; gap: 8px; }
    .savings-icon { font-size: 20px; }
    .savings-value { font-size: 14px; font-weight: 700; }
    .savings-sub { font-size: 11px; color: #64748B; }

    .gauges { display: flex; justify-content: space-around; margin-top: 16px; }
    .gauge-item { text-align: center; }
    .gauge-value { font-size: 16px; font-weight: 700; margin-top: -8px; }
    .gauge-unit { font-size: 11px; color: #64748B; }
    .gauge-label { font-size: 11px; color: #64748B; margin-top: 2px; }

    .info-box {
      margin-top: 16px; padding: 10px; border-radius: 12px;
      background: #0F172A; border: 1px solid #1E293B;
      font-size: 9px; color: #64748B;
    }
  `]
})
export class DashboardComponent {
  @Input() result: SimulationResult | null = null;
  @Input() activeFuel: FuelProfile | null = null;

  get cheapest() {
    if (!this.result) return null;
    return [...this.result.comparison].sort((a, b) => a.cost - b.cost)[0];
  }

  get mostExpensive() {
    if (!this.result) return null;
    const sorted = [...this.result.comparison].sort((a, b) => a.cost - b.cost);
    return sorted[sorted.length - 1];
  }

  getBarWidth(cost: number): number {
    if (!this.result) return 0;
    const max = Math.max(...this.result.comparison.map(c => c.cost), 1);
    return (cost / max) * 100;
  }

  getGaugeDash(value: number, max: number): string {
    const pct = Math.min(value / max, 1);
    const circ = Math.PI * 40;
    return `${pct * circ} ${circ}`;
  }

  getTrafficColor(factor: number): string {
    if (factor < 1.15) return '#22C55E';
    if (factor < 1.4) return '#EAB308';
    if (factor < 1.7) return '#F97316';
    return '#EF4444';
  }

  formatDuration(minutes: number): string {
    if (!minutes || !isFinite(minutes)) return '—';
    const h = Math.floor(minutes / 60);
    const m = Math.round(minutes % 60);
    return h === 0 ? `${m} min` : `${h}h${m.toString().padStart(2, '0')}`;
  }
}
