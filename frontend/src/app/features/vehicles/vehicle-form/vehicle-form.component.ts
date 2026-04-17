import { Component, inject, signal, output, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { VehicleService } from '../../../core/services/vehicle.service';
import { Vehicle, FUEL_TYPES } from '../../../core/models/vehicle.model';

@Component({
  selector: 'app-vehicle-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="onSubmit()" class="space-y-4">
      <div>
        <label class="label">Nom du véhicule</label>
        <input type="text" formControlName="name" class="input" placeholder="Ex: Ma Peugeot 308" />
      </div>
      <div>
        <label class="label">Type de carburant</label>
        <select formControlName="fuelType" class="input" (change)="onFuelTypeChange()">
          @for (fuel of fuels; track fuel.key) {
            <option [value]="fuel.key">{{ fuel.label }}</option>
          }
        </select>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="label">Consommation ({{ selectedUnit() }}/100km)</label>
          <input type="number" formControlName="consumptionPer100km" class="input" step="0.1" />
        </div>
        <div>
          <label class="label">Prix (€/{{ selectedUnit() }})</label>
          <input type="number" formControlName="costPerUnit" class="input" step="0.01" />
        </div>
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="label">Réservoir (L)</label>
          <input type="number" formControlName="tankCapacity" class="input" />
        </div>
        <div>
          <label class="label">Année</label>
          <input type="number" formControlName="yearMake" class="input" />
        </div>
      </div>
      <div class="flex items-center gap-2">
        <input type="checkbox" formControlName="isDefault" id="isDefault" class="rounded" />
        <label for="isDefault" class="text-sm text-gray-700">Définir comme véhicule par défaut</label>
      </div>
      @if (errorMessage()) {
        <p class="text-sm text-red-600">{{ errorMessage() }}</p>
      }
      <div class="flex gap-2">
        <button type="submit" class="btn-primary flex-1" [disabled]="form.invalid || isSaving()">
          {{ isSaving() ? 'Enregistrement...' : 'Enregistrer' }}
        </button>
        <button type="button" class="btn-secondary" (click)="cancelled.emit()">Annuler</button>
      </div>
    </form>
  `
})
export class VehicleFormComponent {
  private readonly vehicleService = inject(VehicleService);
  private readonly fb = inject(FormBuilder);

  readonly saved = output<Vehicle>();
  readonly cancelled = output<void>();

  readonly fuels = FUEL_TYPES;
  readonly isSaving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedUnit = signal('L');

  readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    fuelType: ['SP95', Validators.required],
    consumptionPer100km: [7.0, [Validators.required, Validators.min(0.1)]],
    costPerUnit: [1.85, [Validators.required, Validators.min(0.01)]],
    tankCapacity: [50, [Validators.required, Validators.min(5)]],
    yearMake: [new Date().getFullYear(), Validators.required],
    isDefault: [false]
  });

  onFuelTypeChange() {
    const ft = FUEL_TYPES.find(f => f.key === this.form.value.fuelType);
    if (ft) {
      this.selectedUnit.set(ft.unit);
      this.form.patchValue({ consumptionPer100km: ft.defaultConsumption, costPerUnit: ft.defaultPrice });
    }
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.isSaving.set(true);
    this.vehicleService.createVehicle(this.form.value as any).subscribe({
      next: v => { this.isSaving.set(false); this.saved.emit(v); },
      error: err => { this.errorMessage.set(err.error?.error ?? 'Erreur'); this.isSaving.set(false); }
    });
  }
}
