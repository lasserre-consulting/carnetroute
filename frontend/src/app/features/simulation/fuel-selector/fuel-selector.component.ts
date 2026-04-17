import { Component, signal, output, ChangeDetectionStrategy, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FUEL_TYPES } from '../../../core/models/vehicle.model';

@Component({
  selector: 'app-fuel-selector',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div>
      <label class="label">Type de carburant</label>
      <div class="grid grid-cols-2 sm:grid-cols-3 gap-2">
        @for (fuel of fuels; track fuel.key) {
          <button
            type="button"
            (click)="select(fuel.key)"
            [class]="getFuelClass(fuel.key)"
          >
            <span class="text-lg">{{ getFuelIcon(fuel.key) }}</span>
            <span class="font-medium text-sm">{{ fuel.label }}</span>
            <span class="text-xs text-gray-500 dark:text-gray-400">{{ fuel.defaultPrice }} €/{{ fuel.unit }}</span>
          </button>
        }
      </div>
    </div>
  `
})
export class FuelSelectorComponent {
  readonly selectedFuel = input<string>('SP95');
  readonly fuelSelected = output<string>();

  readonly fuels = FUEL_TYPES;

  select(fuelKey: string) { this.fuelSelected.emit(fuelKey); }

  getFuelIcon(key: string): string {
    const icons: Record<string, string> = {
      SP95: '⛽', SP98: '⛽', DIESEL: '🛢️', E85: '🌿', GPL: '💨', ELECTRIC: '⚡'
    };
    return icons[key] || '⛽';
  }

  getFuelClass(key: string): string {
    const base = 'flex flex-col items-center gap-1 p-3 rounded-lg border-2 transition-all cursor-pointer';
    const selected = this.selectedFuel() === key;
    return `${base} ${selected
      ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300'
      : 'border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 hover:border-gray-300 dark:hover:border-gray-500 text-gray-700 dark:text-gray-300'}`;
  }
}
