import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, Subscription, switchMap, catchError, EMPTY } from 'rxjs';
import { SimulationService } from './services/simulation.service';
import { AddressInputComponent } from './components/address-input/address-input.component';
import { FuelSelectorComponent } from './components/fuel-selector/fuel-selector.component';
import { TrafficComponent } from './components/traffic/traffic.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { MapComponent } from './components/map/map.component';
import { AlertPanelComponent } from './components/alert-panel/alert-panel.component';
import {
  Coordinates, SimulationRequest, SimulationResult,
  FuelProfile
} from './models/simulation.model';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    AddressInputComponent,
    FuelSelectorComponent,
    TrafficComponent,
    DashboardComponent,
    MapComponent,
    AlertPanelComponent
  ],
  template: `
    <div class="container">
      <!-- Header -->
      <div class="app-header">
        <div class="header-row">
          <div class="header-icon">🚗</div>
          <h1 class="header-title heading">Carnet d'Route</h1>
        </div>
        <p class="header-sub mono">Simulation de trajet · Coût · Performance · Affluence</p>
      </div>

      <div class="grid-main">
        <!-- LEFT PANEL -->
        <div class="left-panel">
          <!-- Route -->
          <div class="card">
            <div class="card-title">📍 Itinéraire</div>

            <div class="route-section">
              <label class="field-label">Départ</label>
              <app-address-input
                placeholder="12 rue de la Paix, Paris..."
                accentColor="#06B6D4"
                [initialValue]="fromLabel"
                (addressSelected)="onFromSelected($event)">
              </app-address-input>
            </div>

            <div class="swap-row">
              <button class="swap-btn" (click)="swapRoute()">⇅</button>
            </div>

            <div class="route-section">
              <label class="field-label">Arrivée</label>
              <app-address-input
                placeholder="45 avenue Jean Jaurès, Toulouse..."
                accentColor="#8B5CF6"
                [initialValue]="toLabel"
                (addressSelected)="onToSelected($event)">
              </app-address-input>
            </div>


            <div *ngIf="hasRoute" class="route-summary">
              <span class="badge" style="background: rgba(6,182,212,0.08); color: #06B6D4; border-color: rgba(6,182,212,0.15)">
                {{ result?.distanceKm | number:'1.0-0' }} km · ~{{ formatDuration(result?.baseTimeMin || 0) }} (fluide)
              </span>
            </div>
          </div>

          <!-- Fuel -->
          <app-fuel-selector
            [fuels]="fuels"
            [selectedFuel]="fuelType"
            (fuelChanged)="onFuelChanged($event)"
            (priceChanged)="customPrice = $event; runSimulation()"
            (consumptionChanged)="customConsumption = $event; runSimulation()">
          </app-fuel-selector>

          <!-- Traffic -->
          <app-traffic
            [manualLevel]="manualLevel"
            (manualLevelChanged)="manualLevel = $event; runSimulation()">
          </app-traffic>
        </div>

        <!-- RIGHT PANEL -->
        <div class="right-panel">
          <!-- Map -->
          <app-map
            [fromCoords]="fromCoords"
            [toCoords]="toCoords"
            [distanceKm]="result?.distanceKm || 0"
            [routeGeometry]="result?.routeGeometry"
            [loading]="simulating">
          </app-map>

          <!-- Results -->
          <app-dashboard
            *ngIf="result && hasRoute"
            [result]="result"
            [activeFuel]="activeFuel">
          </app-dashboard>

          <!-- Live Kafka alerts -->
          <app-alert-panel></app-alert-panel>

          <div *ngIf="!hasRoute" class="empty-state card">
            <div class="empty-icon">🗺️</div>
            <div class="empty-text">Renseignez un départ et une arrivée pour lancer la simulation</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .app-header { margin-bottom: 20px; }
    .header-row { display: flex; align-items: center; gap: 12px; margin-bottom: 4px; }
    .header-icon {
      width: 36px; height: 36px; border-radius: 10px; display: flex;
      align-items: center; justify-content: center; font-size: 18px;
      background: linear-gradient(135deg, #06B6D4, #8B5CF6);
    }
    .header-title {
      font-size: 22px; font-weight: 700;
      background: linear-gradient(90deg, #06B6D4, #8B5CF6);
      -webkit-background-clip: text; -webkit-text-fill-color: transparent;
    }
    .header-sub { font-size: 11px; color: #64748B; margin-left: 48px; }

    .left-panel { display: flex; flex-direction: column; gap: 16px; }
    .right-panel { display: flex; flex-direction: column; gap: 16px; }

    .route-section { margin-bottom: 8px; }
    .field-label { display: block; font-size: 11px; color: #64748B; margin-bottom: 4px; }

    .swap-row { text-align: center; margin: 4px 0; }
    .swap-btn {
      width: 32px; height: 32px; border-radius: 50%;
      background: rgba(51,65,85,0.4); border: 1px solid #475569;
      color: #94A3B8; font-size: 14px; cursor: pointer;
      transition: all 0.2s;
    }
    .swap-btn:hover { color: #06B6D4; border-color: #06B6D4; }


    .route-summary { text-align: center; margin-top: 12px; }

    .empty-state { text-align: center; padding: 60px 20px; }
    .empty-icon { font-size: 48px; margin-bottom: 16px; }
    .empty-text { color: #64748B; font-size: 14px; }

  `]
})
export class AppComponent implements OnInit, OnDestroy {
  fuels: FuelProfile[] = [];
  fuelType = 'sp95';
  customPrice: number | null = null;
  customConsumption: number | null = null;
  manualLevel = 0;


  fromCoords: Coordinates | null = null;
  toCoords: Coordinates | null = null;
  fromLabel = '';
  toLabel = '';

  result: SimulationResult | null = null;
  simulating = false;

  private simTrigger = new Subject<SimulationRequest>();
  private fuelsSub: Subscription | null = null;
  private simSub: Subscription | null = null;

  constructor(private simService: SimulationService) {}

  ngOnInit() {
    this.fuelsSub = this.simService.getFuels().subscribe(fuels => {
      this.fuels = fuels;
    });

    this.simSub = this.simTrigger.pipe(
      switchMap(request =>
        this.simService.simulate(request).pipe(
          catchError(() => { this.simulating = false; return EMPTY; })
        )
      )
    ).subscribe(result => {
      this.result = result;
      this.simulating = false;
    });
  }

  ngOnDestroy() {
    this.fuelsSub?.unsubscribe();
    this.simSub?.unsubscribe();
  }

  get activeFuel(): FuelProfile | null {
    return this.fuels.find(f => f.key === this.fuelType) || null;
  }

  get hasRoute(): boolean {
    return !!(this.fromCoords && this.toCoords && (this.result || this.simulating));
  }

  onFromSelected(event: { coords: Coordinates; label: string }) {
    this.fromCoords = event.coords;
    this.fromLabel = event.label;
    if (this.toCoords) this.runSimulation();
  }

  onToSelected(event: { coords: Coordinates; label: string }) {
    this.toCoords = event.coords;
    this.toLabel = event.label;
    if (this.fromCoords) this.runSimulation();
  }

  swapRoute() {
    const tmpCoords = this.fromCoords;
    const tmpLabel = this.fromLabel;
    this.fromCoords = this.toCoords;
    this.fromLabel = this.toLabel;
    this.toCoords = tmpCoords;
    this.toLabel = tmpLabel;
    this.runSimulation();
  }

  onFuelChanged(fuelType: string) {
    this.fuelType = fuelType;
    this.customPrice = null;
    this.customConsumption = null;
    this.runSimulation();
  }

  runSimulation() {
    if (!this.fromCoords || !this.toCoords) return;

    this.simulating = true;
    this.simTrigger.next({
      from: this.fromCoords,
      to: this.toCoords,
      fuelType: this.fuelType,
      customPrice: this.customPrice ?? undefined,
      customConsumption: this.customConsumption ?? undefined,
      trafficMode: 'manual',
      manualTrafficLevel: this.manualLevel,
      departureDay: 0,
      departureHour: 8,
      avoidTolls: false
    });
  }

  formatDuration(minutes: number): string {
    if (!minutes || !isFinite(minutes)) return '—';
    const h = Math.floor(minutes / 60);
    const m = Math.round(minutes % 60);
    return h === 0 ? `${m} min` : `${h}h${m.toString().padStart(2, '0')}`;
  }
}
