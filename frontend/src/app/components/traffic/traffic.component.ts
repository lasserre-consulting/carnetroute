import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-traffic',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-title">🚦 Affluence</div>

      <div class="manual-section">
        <input type="range" min="0" max="3" step="1"
          [ngModel]="manualLevel"
          (ngModelChange)="onManualChange($event)"
          class="traffic-slider" />
        <div class="traffic-labels">
          <span *ngFor="let t of trafficLevels; let i = index"
            [style.color]="i === manualLevel ? t.color : '#475569'"
            [style.fontWeight]="i === manualLevel ? 'bold' : 'normal'">
            {{ t.icon }} {{ t.label }}
          </span>
        </div>
        <div class="traffic-badge-wrapper">
          <span class="badge"
            [style.background]="trafficLevels[manualLevel].color + '15'"
            [style.color]="trafficLevels[manualLevel].color"
            [style.borderColor]="trafficLevels[manualLevel].color + '30'">
            {{ trafficLevels[manualLevel].desc }}
          </span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .manual-section { padding-top: 4px; }
    .traffic-slider {
      width: 100%; height: 8px; border-radius: 4px; appearance: none; cursor: pointer;
      background: linear-gradient(90deg, #22C55E, #EAB308, #F97316, #EF4444);
    }
    .traffic-slider::-webkit-slider-thumb {
      appearance: none; width: 18px; height: 18px; border-radius: 50%;
      background: white; cursor: pointer; box-shadow: 0 2px 6px rgba(0,0,0,0.3);
    }
    .traffic-labels {
      display: flex; justify-content: space-between; margin-top: 8px;
      font-size: 11px;
    }
    .traffic-badge-wrapper { text-align: center; margin-top: 8px; }
  `]
})
export class TrafficComponent {
  @Input() manualLevel = 0;
  @Output() manualLevelChanged = new EventEmitter<number>();

  trafficLevels = [
    { label: 'Fluide', icon: '🟢', color: '#22C55E', desc: 'Aucun ralentissement' },
    { label: 'Modéré', icon: '🟡', color: '#EAB308', desc: '+25% de temps' },
    { label: 'Dense', icon: '🟠', color: '#F97316', desc: '+55% de temps' },
    { label: 'Embouteillage', icon: '🔴', color: '#EF4444', desc: '+100% de temps' },
  ];

  onManualChange(level: number) {
    this.manualLevel = level;
    this.manualLevelChanged.emit(level);
  }
}
