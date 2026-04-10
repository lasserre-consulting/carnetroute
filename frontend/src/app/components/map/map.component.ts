import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Coordinates } from '../../models/simulation.model';

interface CityPoint {
  name: string;
  lat: number;
  lng: number;
}

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card">
      <div class="map-header">
        <span class="card-title" style="margin-bottom:0">🗺️ Carte de France</span>
        <span *ngIf="distanceKm > 0" class="distance-badge mono">{{ distanceKm | number:'1.0-0' }} km</span>
      </div>
      <div class="map-container">
        <svg [attr.viewBox]="'0 0 ' + w + ' ' + h" class="map-svg">
          <defs>
            <radialGradient id="mbg" cx="50%" cy="50%" r="60%">
              <stop offset="0%" stop-color="#1a1f35"/>
              <stop offset="100%" stop-color="#0d1117"/>
            </radialGradient>
            <filter id="gl">
              <feGaussianBlur stdDeviation="3" result="b"/>
              <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
            </filter>
            <filter id="gls">
              <feGaussianBlur stdDeviation="6" result="b"/>
              <feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge>
            </filter>
            <linearGradient id="rg" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#06B6D4"/>
              <stop offset="100%" stop-color="#8B5CF6"/>
            </linearGradient>
          </defs>

          <rect [attr.width]="w" [attr.height]="h" rx="16" fill="url(#mbg)"/>

          <!-- Grid lines -->
          <line *ngFor="let i of gridV"
            [attr.x1]="pad + i * ((w - 2*pad) / 7)" [attr.y1]="pad"
            [attr.x2]="pad + i * ((w - 2*pad) / 7)" [attr.y2]="h - pad"
            stroke="#1e293b" stroke-width="0.5"/>
          <line *ngFor="let i of gridH"
            [attr.x1]="pad" [attr.y1]="pad + i * ((h - 2*pad) / 5)"
            [attr.x2]="w - pad" [attr.y2]="pad + i * ((h - 2*pad) / 5)"
            stroke="#1e293b" stroke-width="0.5"/>

          <!-- Reference cities -->
          <g *ngFor="let city of refCities">
            <circle
              [attr.cx]="project(city.lat, city.lng)[0]"
              [attr.cy]="project(city.lat, city.lng)[1]"
              [attr.r]="isCityActive(city) ? 5 : 2"
              [attr.fill]="getCityColor(city)"
              [attr.filter]="isCityActive(city) ? 'url(#gl)' : null"
              [attr.opacity]="isCityActive(city) ? 1 : 0.5"
            />
            <!-- Pulse ring for active cities -->
            <ng-container *ngIf="isCityActive(city)">
              <circle
                [attr.cx]="project(city.lat, city.lng)[0]"
                [attr.cy]="project(city.lat, city.lng)[1]"
                r="12" fill="none"
                [attr.stroke]="getCityColor(city)"
                stroke-width="1" opacity="0.4">
                <animate attributeName="r" from="8" to="18" dur="2s" repeatCount="indefinite"/>
                <animate attributeName="opacity" from="0.6" to="0" dur="2s" repeatCount="indefinite"/>
              </circle>
            </ng-container>
            <text
              [attr.x]="project(city.lat, city.lng)[0]"
              [attr.y]="project(city.lat, city.lng)[1] - (isCityActive(city) ? 13 : 7)"
              text-anchor="middle"
              [attr.fill]="isCityActive(city) ? '#E2E8F0' : '#64748B'"
              [attr.font-size]="isCityActive(city) ? 10 : 7"
              font-family="'JetBrains Mono', monospace"
              [attr.font-weight]="isCityActive(city) ? 'bold' : 'normal'">
              {{ city.name }}
            </text>
          </g>

          <!-- Custom from/to markers if not in refCities -->
          <ng-container *ngIf="fromCoords && !isInRefCities(fromCoords)">
            <circle [attr.cx]="project(fromCoords.lat, fromCoords.lng)[0]" [attr.cy]="project(fromCoords.lat, fromCoords.lng)[1]" r="5" fill="#06B6D4" filter="url(#gl)"/>
            <circle [attr.cx]="project(fromCoords.lat, fromCoords.lng)[0]" [attr.cy]="project(fromCoords.lat, fromCoords.lng)[1]" r="12" fill="none" stroke="#06B6D4" stroke-width="1" opacity="0.4">
              <animate attributeName="r" from="8" to="18" dur="2s" repeatCount="indefinite"/>
              <animate attributeName="opacity" from="0.6" to="0" dur="2s" repeatCount="indefinite"/>
            </circle>
            <text [attr.x]="project(fromCoords.lat, fromCoords.lng)[0]" [attr.y]="project(fromCoords.lat, fromCoords.lng)[1] - 13" text-anchor="middle" fill="#E2E8F0" font-size="10" font-family="'JetBrains Mono', monospace" font-weight="bold">{{ fromCoords.label }}</text>
          </ng-container>
          <ng-container *ngIf="toCoords && !isInRefCities(toCoords)">
            <circle [attr.cx]="project(toCoords.lat, toCoords.lng)[0]" [attr.cy]="project(toCoords.lat, toCoords.lng)[1]" r="5" fill="#8B5CF6" filter="url(#gl)"/>
            <circle [attr.cx]="project(toCoords.lat, toCoords.lng)[0]" [attr.cy]="project(toCoords.lat, toCoords.lng)[1]" r="12" fill="none" stroke="#8B5CF6" stroke-width="1" opacity="0.4">
              <animate attributeName="r" from="8" to="18" dur="2s" repeatCount="indefinite"/>
              <animate attributeName="opacity" from="0.6" to="0" dur="2s" repeatCount="indefinite"/>
            </circle>
            <text [attr.x]="project(toCoords.lat, toCoords.lng)[0]" [attr.y]="project(toCoords.lat, toCoords.lng)[1] - 13" text-anchor="middle" fill="#E2E8F0" font-size="10" font-family="'JetBrains Mono', monospace" font-weight="bold">{{ toCoords.label }}</text>
          </ng-container>

          <!-- Route loading sans géométrie existante : ligne pointillée -->
          <ng-container *ngIf="fromCoords && toCoords && loading && !routePath">
            <line
              [attr.x1]="project(fromCoords.lat, fromCoords.lng)[0]"
              [attr.y1]="project(fromCoords.lat, fromCoords.lng)[1]"
              [attr.x2]="project(toCoords.lat, toCoords.lng)[0]"
              [attr.y2]="project(toCoords.lat, toCoords.lng)[1]"
              stroke="#334155" stroke-width="2" stroke-dasharray="4 6"/>
          </ng-container>

          <!-- Route dessinée (atténuée si recalcul en cours) -->
          <ng-container *ngIf="fromCoords && toCoords && routePath">
            <path [attr.d]="routePath" fill="none" stroke="url(#rg)" stroke-width="3" stroke-dasharray="8 4" filter="url(#gl)" [attr.opacity]="loading ? 0.3 : 0.7"/>
            <path [attr.d]="routePath" fill="none" stroke="url(#rg)" [attr.stroke-width]="loading ? 1 : 1.5" [attr.opacity]="loading ? 0.4 : 1"/>

          </ng-container>

          <text [attr.x]="w - pad + 5" [attr.y]="h - 12" fill="#475569" font-size="7" font-family="'JetBrains Mono', monospace" text-anchor="end">CARNET ROUTE v2.0</text>
        </svg>
      </div>
    </div>
  `,
  styles: [`
    .map-header {
      display: flex; align-items: center; justify-content: space-between;
      margin-bottom: 8px;
    }
    .distance-badge {
      font-size: 11px; padding: 2px 10px; border-radius: 20px;
      background: rgba(6,182,212,0.12); color: #06B6D4;
      border: 1px solid rgba(6,182,212,0.2);
    }
    .map-container { padding: 0 4px 4px; }
    .map-svg { width: 100%; height: auto; }
  `]
})
export class MapComponent implements OnChanges {
  constructor(private cdr: ChangeDetectorRef) {}
  @Input() fromCoords: Coordinates | null = null;
  @Input() toCoords: Coordinates | null = null;
  @Input() distanceKm = 0;
  @Input() routeGeometry: number[][] | null | undefined = null;
  @Input() loading = false;

  w = 500; h = 370; pad = 55;
  gridV = [0,1,2,3,4,5,6,7];
  gridH = [0,1,2,3,4,5];

  routePath = '';

  refCities: CityPoint[] = [
    { name: 'Paris', lat: 48.8566, lng: 2.3522 },
    { name: 'Lyon', lat: 45.764, lng: 4.8357 },
    { name: 'Marseille', lat: 43.2965, lng: 5.3698 },
    { name: 'Toulouse', lat: 43.6047, lng: 1.4442 },
    { name: 'Nice', lat: 43.7102, lng: 7.262 },
    { name: 'Nantes', lat: 47.2184, lng: -1.5536 },
    { name: 'Strasbourg', lat: 48.5734, lng: 7.7521 },
    { name: 'Montpellier', lat: 43.6108, lng: 3.8767 },
    { name: 'Bordeaux', lat: 44.8378, lng: -0.5792 },
    { name: 'Lille', lat: 50.6292, lng: 3.0573 },
    { name: 'Rennes', lat: 48.1173, lng: -1.6778 },
    { name: 'Grenoble', lat: 45.1885, lng: 5.7245 },
    { name: 'Dijon', lat: 47.322, lng: 5.0415 },
    { name: 'Clermont-Ferrand', lat: 45.7772, lng: 3.087 },
    { name: 'Brest', lat: 48.3904, lng: -4.4861 },
    { name: 'Toulon', lat: 43.1242, lng: 5.928 },
    { name: 'Le Havre', lat: 49.4944, lng: 0.1079 },
    { name: 'Perpignan', lat: 42.6887, lng: 2.8948 },
    { name: 'Limoges', lat: 45.8336, lng: 1.2611 },
    { name: 'Amiens', lat: 49.894, lng: 2.2958 },
  ];

  private minLat = 41.5; private maxLat = 51.5;
  private minLng = -5.5; private maxLng = 10.0;

  project(lat: number, lng: number): [number, number] {
    const x = this.pad + ((lng - this.minLng) / (this.maxLng - this.minLng)) * (this.w - 2 * this.pad);
    const y = this.pad + ((this.maxLat - lat) / (this.maxLat - this.minLat)) * (this.h - 2 * this.pad);
    return [x, y];
  }

  isCityActive(city: CityPoint): boolean {
    if (!this.fromCoords && !this.toCoords) return false;
    const threshold = 0.15;
    if (this.fromCoords && Math.abs(city.lat - this.fromCoords.lat) < threshold && Math.abs(city.lng - this.fromCoords.lng) < threshold) return true;
    if (this.toCoords && Math.abs(city.lat - this.toCoords.lat) < threshold && Math.abs(city.lng - this.toCoords.lng) < threshold) return true;
    return false;
  }

  getCityColor(city: CityPoint): string {
    if (!this.fromCoords && !this.toCoords) return '#475569';
    const threshold = 0.15;
    if (this.fromCoords && Math.abs(city.lat - this.fromCoords.lat) < threshold && Math.abs(city.lng - this.fromCoords.lng) < threshold) return '#06B6D4';
    if (this.toCoords && Math.abs(city.lat - this.toCoords.lat) < threshold && Math.abs(city.lng - this.toCoords.lng) < threshold) return '#8B5CF6';
    return '#475569';
  }

  isInRefCities(coords: Coordinates): boolean {
    return this.refCities.some(c => Math.abs(c.lat - coords.lat) < 0.15 && Math.abs(c.lng - coords.lng) < 0.15);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (!this.fromCoords || !this.toCoords) return;

    const geometryChanged = 'routeGeometry' in changes;
    const coordsChanged = 'fromCoords' in changes || 'toCoords' in changes;
    const loadingFinished = 'loading' in changes && !this.loading;

    const hasRealGeometry = this.routeGeometry && this.routeGeometry.length > 1;

    if (geometryChanged && hasRealGeometry) {
      // Tracé ORS reçu
      this.updateRoute();
      this.startAnimation();
    } else if ((coordsChanged || loadingFinished) && !hasRealGeometry) {
      // Pas de géométrie ORS (fallback haversine) : bezier dès la fin du chargement
      this.updateRoute();
      this.startAnimation();
    }
  }

  private updateRoute() {
    if (!this.fromCoords || !this.toCoords) return;

    if (this.routeGeometry && this.routeGeometry.length > 1) {
      const pts = this.routeGeometry.map(([lng, lat]) => this.project(lat, lng));
      this.routePath = pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p[0].toFixed(1)} ${p[1].toFixed(1)}`).join(' ');
    } else {
      // Fallback: bezier curve
      const [fx, fy] = this.project(this.fromCoords.lat, this.fromCoords.lng);
      const [tx, ty] = this.project(this.toCoords.lat, this.toCoords.lng);
      const mx = (fx + tx) / 2, my = (fy + ty) / 2;
      const cx = mx - (ty - fy) * 0.2, cy = my + (tx - fx) * 0.2;
      this.routePath = `M ${fx} ${fy} Q ${cx} ${cy} ${tx} ${ty}`;
    }
  }

  private startAnimation() {
    this.cdr.detectChanges();
  }
}
