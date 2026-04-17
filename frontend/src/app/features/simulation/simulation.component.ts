import { Component, signal, computed, inject, ChangeDetectionStrategy, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { filter, take, switchMap } from 'rxjs';
import { SimulationService } from '../../core/services/simulation.service';
import { AuthService } from '../../core/auth/auth.service';
import { VehicleService } from '../../core/services/vehicle.service';
import { Vehicle } from '../../core/models/vehicle.model';
import { Simulation, AddressSuggestion } from '../../core/models/simulation.model';
import { MapComponent } from './map/map.component';
import { AddressInputComponent } from './address-input/address-input.component';
import { FuelSelectorComponent } from './fuel-selector/fuel-selector.component';
import { TrafficComponent, TrafficSelection } from './traffic/traffic.component';
import { DashboardComponent } from './dashboard/dashboard.component';

@Component({
  selector: 'app-simulation',
  standalone: true,
  imports: [CommonModule, MapComponent, AddressInputComponent, FuelSelectorComponent, TrafficComponent, DashboardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Left panel: form -->
        <div class="space-y-4">
          <div class="card">
            <h2 class="mb-4">Trajet</h2>
            <div class="space-y-3">
              <app-address-input
                label="Départ"
                placeholder="Ville, adresse de départ..."
                icon="🟢"
                (selected)="onFromSelected($event)"
              />
              <app-address-input
                label="Arrivée"
                placeholder="Ville, adresse d'arrivée..."
                icon="🔴"
                (selected)="onToSelected($event)"
              />
            </div>
          </div>

          <div class="card">
            <h2 class="mb-4">Véhicule & carburant</h2>

            @if (isAuthenticated() && vehicles().length > 0) {
              <div class="mb-4">
                <label class="label">Mon véhicule</label>
                <div class="grid gap-2">
                  @for (v of vehicles(); track v.id) {
                    <button
                      type="button"
                      (click)="selectVehicle(v)"
                      [class]="getVehicleClass(v.id)"
                    >
                      <span class="text-lg">🚗</span>
                      <div class="flex-1 text-left">
                        <div class="font-medium text-sm">{{ v.name }}</div>
                        <div class="text-xs text-gray-500 dark:text-gray-400">{{ getFuelLabel(v.fuelType) }} · {{ v.consumptionPer100km }} L/100km</div>
                      </div>
                      @if (selectedVehicleId() === v.id) {
                        <span class="text-primary-600 dark:text-primary-400">✓</span>
                      }
                    </button>
                  }
                  @if (selectedVehicleId() !== null) {
                    <button type="button" (click)="clearVehicle()" class="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 text-left pl-1">
                      Choisir manuellement le carburant
                    </button>
                  }
                </div>
              </div>
            }

            @if (selectedVehicleId() === null) {
              <app-fuel-selector
                [selectedFuel]="selectedFuel()"
                (fuelSelected)="selectedFuel.set($event)"
              />
            }
          </div>

          <div class="card">
            <h2 class="mb-4">Trafic</h2>
            <app-traffic (trafficChanged)="onTrafficChange($event)" />
          </div>

          <button
            type="button"
            class="btn-primary w-full text-lg py-3"
            [disabled]="!canSimulate() || isLoading()"
            (click)="simulate()"
          >
            @if (isLoading()) {
              <span class="animate-spin mr-2">⏳</span> Calcul en cours...
            } @else {
              🔍 Calculer le trajet
            }
          </button>

          @if (errorMessage()) {
            <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              ⚠️ {{ errorMessage() }}
            </div>
          }
        </div>

        <!-- Right panel: map + results -->
        <div class="space-y-4">
          <app-map [simulation]="currentSimulation()" />
          <app-dashboard [simulation]="currentSimulation()" />
        </div>
      </div>
    </div>
  `
})
export class SimulationComponent {
  private readonly simService = inject(SimulationService);
  private readonly authService = inject(AuthService);
  private readonly vehicleService = inject(VehicleService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isAuthenticated = this.authService.isAuthenticated;

  readonly fromAddress = signal<AddressSuggestion | null>(null);
  readonly toAddress = signal<AddressSuggestion | null>(null);
  readonly selectedFuel = signal('SP95');
  readonly selectedVehicleId = signal<string | null>(null);
  readonly vehicles = signal<Vehicle[]>([]);
  readonly trafficSelection = signal<TrafficSelection>({ mode: 'manual', factor: 1.0 });
  readonly currentSimulation = signal<Simulation | null>(null);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly canSimulate = computed(() => this.fromAddress() !== null && this.toAddress() !== null);

  constructor() {
    // Charger les véhicules uniquement quand currentUser est effectivement défini
    // (évite la race condition avec isRestoringSession)
    toObservable(this.authService.currentUser).pipe(
      filter(user => user !== null),
      take(1),
      switchMap(() => this.vehicleService.getVehicles()),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (list) => {
        this.vehicles.set(list);
        const def = list.find(v => v.isDefault) ?? list[0];
        if (def) this.selectVehicle(def);
      },
      error: () => {}
    });
  }

  selectVehicle(v: Vehicle) {
    this.selectedVehicleId.set(v.id);
    this.selectedFuel.set(v.fuelType);
  }

  clearVehicle() {
    this.selectedVehicleId.set(null);
  }

  getVehicleClass(id: string): string {
    const base = 'flex items-center gap-3 p-3 rounded-lg border-2 transition-all cursor-pointer w-full';
    return id === this.selectedVehicleId()
      ? `${base} border-primary-500 bg-primary-50 dark:bg-primary-900/30`
      : `${base} border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-700 hover:border-gray-300`;
  }

  getFuelLabel(key: string): string {
    const map: Record<string, string> = { SP95: 'SP95', SP98: 'SP98', DIESEL: 'Diesel', E85: 'E85', GPL: 'GPL', ELECTRIC: 'Électrique' };
    return map[key] ?? key;
  }

  onFromSelected(s: AddressSuggestion) { this.fromAddress.set(s); }
  onToSelected(s: AddressSuggestion) { this.toAddress.set(s); }
  onTrafficChange(t: TrafficSelection) { this.trafficSelection.set(t); }

  simulate() {
    const from = this.fromAddress()!;
    const to = this.toAddress()!;
    const traffic = this.trafficSelection();
    const vehicle = this.vehicles().find(v => v.id === this.selectedVehicleId());

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const customConsumptions: { [key: string]: number } = {};
    if (vehicle) {
      customConsumptions[vehicle.fuelType] = vehicle.consumptionPer100km;
    }

    this.simService.simulate({
      fromLat: from.lat, fromLng: from.lng, fromLabel: from.label,
      toLat: to.lat, toLng: to.lng, toLabel: to.label,
      fuelType: this.selectedFuel(),
      trafficMode: traffic.mode,
      trafficFactor: traffic.factor,
      departureDay: traffic.departureDay,
      departureHour: traffic.departureHour,
      vehicleId: this.selectedVehicleId() ?? undefined,
      customConsumptions: Object.keys(customConsumptions).length > 0 ? customConsumptions : undefined,
      saveToHistory: this.isAuthenticated()
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (result) => {
        this.currentSimulation.set(result);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.error ?? 'Erreur lors du calcul du trajet');
        this.isLoading.set(false);
      }
    });
  }
}
