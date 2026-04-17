import { Component, inject, signal, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { VehicleService } from '../../core/services/vehicle.service';
import { Vehicle, FUEL_TYPES } from '../../core/models/vehicle.model';
import { VehicleFormComponent } from './vehicle-form/vehicle-form.component';

@Component({
  selector: 'app-vehicles',
  standalone: true,
  imports: [CommonModule, DecimalPipe, RouterLink, VehicleFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1>🚗 Mes véhicules</h1>
        <button class="btn-primary" (click)="showForm.set(true)">+ Ajouter</button>
      </div>

      @if (showForm()) {
        <div class="card border-primary-200 border-2">
          <h2 class="mb-4">Nouveau véhicule</h2>
          <app-vehicle-form
            (saved)="onVehicleSaved($event)"
            (cancelled)="showForm.set(false)"
          />
        </div>
      }

      @if (isLoading()) {
        <div class="flex justify-center py-12">
          <div class="animate-spin text-4xl">⏳</div>
        </div>
      } @else if (vehicles().length === 0) {
        <div class="card text-center py-12">
          <div class="text-5xl mb-4">🚗</div>
          <h2 class="text-gray-600">Aucun véhicule enregistré</h2>
          <p class="text-gray-400 mt-2">Ajoutez votre premier véhicule pour personnaliser vos simulations</p>
          <button class="btn-primary mt-4" (click)="showForm.set(true)">Ajouter un véhicule</button>
        </div>
      } @else {
        <div class="grid gap-4 sm:grid-cols-2">
          @for (vehicle of vehicles(); track vehicle.id) {
            <div class="card-hover relative" [class.border-primary-300]="vehicle.isDefault" [class.border-2]="vehicle.isDefault">
              @if (vehicle.isDefault) {
                <span class="absolute top-3 right-3 badge bg-primary-100 text-primary-700">Par défaut</span>
              }
              <div class="flex items-start gap-3">
                <span class="text-3xl">{{ getFuelIcon(vehicle.fuelType) }}</span>
                <div class="flex-1 min-w-0">
                  <h3 class="font-semibold text-gray-900 truncate">{{ vehicle.name }}</h3>
                  <p class="text-sm text-gray-500">{{ getFuelLabel(vehicle.fuelType) }}</p>
                  <div class="flex gap-4 mt-2 text-xs text-gray-600">
                    <span>⛽ {{ vehicle.consumptionPer100km }} {{ getFuelUnit(vehicle.fuelType) }}/100km</span>
                    <span>💰 {{ vehicle.costPerUnit }} €/{{ getFuelUnit(vehicle.fuelType) }}</span>
                  </div>
                  <div class="flex gap-4 mt-1 text-xs text-gray-500">
                    <span>🛢️ {{ vehicle.tankCapacity }}L</span>
                    <span>📅 {{ vehicle.yearMake }}</span>
                  </div>
                </div>
              </div>
              <div class="flex gap-2 mt-4 pt-3 border-t border-gray-100">
                @if (!vehicle.isDefault) {
                  <button class="btn-secondary text-xs py-1 px-3 flex-1" (click)="setDefault(vehicle)">
                    Définir par défaut
                  </button>
                }
                <button class="btn-danger text-xs py-1 px-3" (click)="deleteVehicle(vehicle)">
                  🗑️
                </button>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class VehiclesComponent {
  private readonly vehicleService = inject(VehicleService);
  private readonly destroyRef = inject(DestroyRef);

  readonly vehicles = signal<Vehicle[]>([]);
  readonly isLoading = signal(true);
  readonly showForm = signal(false);

  constructor() {
    this.loadVehicles();
  }

  loadVehicles() {
    this.isLoading.set(true);
    this.vehicleService.getVehicles().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: v => { this.vehicles.set(v); this.isLoading.set(false); },
      error: () => this.isLoading.set(false)
    });
  }

  onVehicleSaved(vehicle: Vehicle) {
    this.vehicles.update(list => [...list, vehicle]);
    this.showForm.set(false);
  }

  deleteVehicle(v: Vehicle) {
    if (!confirm(`Supprimer "${v.name}" ?`)) return;
    this.vehicleService.deleteVehicle(v.id).subscribe(() => {
      this.vehicles.update(list => list.filter(x => x.id !== v.id));
    });
  }

  setDefault(v: Vehicle) {
    this.vehicleService.updateVehicle(v.id, { isDefault: true }).subscribe(() => {
      this.vehicles.update(list => list.map(x => ({ ...x, isDefault: x.id === v.id })));
    });
  }

  getFuelIcon(key: string) {
    const m: Record<string, string> = { SP95: '⛽', SP98: '⛽', DIESEL: '🛢️', E85: '🌿', GPL: '💨', ELECTRIC: '⚡' };
    return m[key] ?? '⛽';
  }
  getFuelLabel(key: string) { return FUEL_TYPES.find(f => f.key === key)?.label ?? key; }
  getFuelUnit(key: string) { return FUEL_TYPES.find(f => f.key === key)?.unit ?? 'L'; }
}
