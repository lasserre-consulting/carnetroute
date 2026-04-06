import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FuelProfile } from '../../models/simulation.model';

@Component({
  selector: 'app-fuel-selector',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card">
      <div class="card-title">🚘 Motorisation</div>

      <div class="fuel-grid">
        <button
          *ngFor="let fuel of fuels"
          class="fuel-btn"
          [class.active]="selectedFuel === fuel.key"
          [style.borderColor]="selectedFuel === fuel.key ? fuel.color : ''"
          [style.background]="selectedFuel === fuel.key ? fuel.color + '18' : ''"
          [style.boxShadow]="selectedFuel === fuel.key ? '0 0 15px ' + fuel.color + '20' : ''"
          (click)="selectFuel(fuel)"
        >
          <div class="fuel-icon">{{ fuel.icon }}</div>
          <div class="fuel-name" [style.color]="selectedFuel === fuel.key ? fuel.color : ''">
            {{ fuel.shortName }}
          </div>
        </button>
      </div>

      <div class="price-inputs" *ngIf="activeFuel">
        <div class="price-field">
          <label>Prix ({{ activeFuel.priceUnit }})</label>
          <input
            type="number"
            class="input"
            [step]="0.01"
            [ngModel]="price"
            (ngModelChange)="onPriceChange($event)"
            [style.color]="activeFuel.color"
          />
        </div>
        <div class="price-field">
          <label>Conso ({{ activeFuel.unit }})</label>
          <input
            type="number"
            class="input"
            [step]="0.1"
            [ngModel]="consumption"
            (ngModelChange)="onConsumptionChange($event)"
            [style.color]="activeFuel.color"
          />
        </div>
      </div>
    </div>
  `,
  styles: [`
    .fuel-grid {
      display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px;
    }

    .fuel-btn {
      background: #0F172A; border: 1.5px solid #1E293B;
      border-radius: 12px; padding: 10px 4px; text-align: center;
      cursor: pointer; transition: all 0.3s;
      font-family: 'DM Sans', sans-serif;
    }
    .fuel-btn:hover { border-color: #334155; }

    .fuel-icon { font-size: 20px; margin-bottom: 4px; }
    .fuel-name { font-size: 11px; font-weight: 500; color: #94A3B8; }

    .price-inputs {
      display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
      margin-top: 12px;
    }

    .price-field label {
      display: block; font-size: 11px; color: #64748B; margin-bottom: 4px;
    }

    .price-field .input { font-size: 13px; padding: 8px 10px; }
  `]
})
export class FuelSelectorComponent {
  @Input() fuels: FuelProfile[] = [];
  @Input() selectedFuel = 'sp95';
  @Output() fuelChanged = new EventEmitter<string>();
  @Output() priceChanged = new EventEmitter<number | null>();
  @Output() consumptionChanged = new EventEmitter<number | null>();

  price: number = 0;
  consumption: number = 0;

  get activeFuel(): FuelProfile | undefined {
    return this.fuels.find(f => f.key === this.selectedFuel);
  }

  ngOnChanges() {
    const fuel = this.activeFuel;
    if (fuel) {
      this.price = fuel.defaultPrice;
      this.consumption = fuel.consumption;
    }
  }

  selectFuel(fuel: FuelProfile) {
    this.selectedFuel = fuel.key;
    this.price = fuel.defaultPrice;
    this.consumption = fuel.consumption;
    this.fuelChanged.emit(fuel.key);
    this.priceChanged.emit(null);
    this.consumptionChanged.emit(null);
  }

  onPriceChange(value: number) {
    this.price = value;
    const fuel = this.activeFuel;
    this.priceChanged.emit(fuel && value !== fuel.defaultPrice ? value : null);
  }

  onConsumptionChange(value: number) {
    this.consumption = value;
    const fuel = this.activeFuel;
    this.consumptionChanged.emit(fuel && value !== fuel.consumption ? value : null);
  }
}
