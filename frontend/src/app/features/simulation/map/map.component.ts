import {
  Component, ElementRef, ViewChild, AfterViewInit, OnDestroy,
  input, effect, ChangeDetectionStrategy, inject, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as maplibregl from 'maplibre-gl';
import { Simulation } from '../../../core/models/simulation.model';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="relative">
      <div #mapContainer class="w-full rounded-xl overflow-hidden" style="height: 360px;"></div>
      @if (!mapLoaded()) {
        <div class="absolute inset-0 flex items-center justify-center bg-gray-100 rounded-xl">
          <div class="text-center">
            <div class="animate-spin text-3xl mb-2">⏳</div>
            <p class="text-sm text-gray-500">Chargement de la carte...</p>
          </div>
        </div>
      }
    </div>
  `
})
export class MapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;

  readonly simulation = input<Simulation | null>(null);
  readonly mapLoaded = signal(false);

  private map: maplibregl.Map | null = null;
  private markers: maplibregl.Marker[] = [];

  constructor() {
    // effect() doit être dans le constructeur (contexte d'injection requis)
    effect(() => {
      const sim = this.simulation();
      if (this.map && this.mapLoaded()) {
        this.renderRoute(sim);
      }
    });
  }

  ngAfterViewInit() {
    this.map = new maplibregl.Map({
      container: this.mapContainer.nativeElement,
      style: 'https://tiles.openfreemap.org/styles/liberty',
      center: [2.35, 46.8],
      zoom: 5,
      attributionControl: { compact: true }
    });

    this.map.addControl(new maplibregl.NavigationControl(), 'top-right');

    this.map.on('load', () => {
      this.mapLoaded.set(true);
      this.renderRoute(this.simulation());
    });
  }

  private renderRoute(simulation: Simulation | null) {
    if (!this.map) return;

    // Clear previous markers
    this.markers.forEach(m => m.remove());
    this.markers = [];

    // Remove previous route layer
    if (this.map.getLayer('route')) this.map.removeLayer('route');
    if (this.map.getSource('route')) this.map.removeSource('route');

    if (!simulation) {
      this.map.flyTo({ center: [2.35, 46.8], zoom: 5 });
      return;
    }

    const { from, to, geometry } = simulation.route;

    // Add origin marker (green)
    const fromEl = document.createElement('div');
    fromEl.className = 'w-4 h-4 bg-green-500 rounded-full border-2 border-white shadow-lg';
    this.markers.push(
      new maplibregl.Marker({ element: fromEl })
        .setLngLat([from.lng, from.lat])
        .setPopup(new maplibregl.Popup().setHTML(`<strong>Départ</strong><br>${from.label}`))
        .addTo(this.map!)
    );

    // Add destination marker (red)
    const toEl = document.createElement('div');
    toEl.className = 'w-4 h-4 bg-red-500 rounded-full border-2 border-white shadow-lg';
    this.markers.push(
      new maplibregl.Marker({ element: toEl })
        .setLngLat([to.lng, to.lat])
        .setPopup(new maplibregl.Popup().setHTML(`<strong>Arrivée</strong><br>${to.label}`))
        .addTo(this.map!)
    );

    // Draw route if geometry is available
    if (geometry && geometry.length > 0) {
      this.map.addSource('route', {
        type: 'geojson',
        data: {
          type: 'Feature',
          properties: {},
          geometry: { type: 'LineString', coordinates: geometry }
        }
      });
      this.map.addLayer({
        id: 'route',
        type: 'line',
        source: 'route',
        layout: { 'line-join': 'round', 'line-cap': 'round' },
        paint: { 'line-color': '#2563eb', 'line-width': 4, 'line-opacity': 0.8 }
      });
    }

    // Fit bounds to show from + to
    const bounds = new maplibregl.LngLatBounds()
      .extend([from.lng, from.lat])
      .extend([to.lng, to.lat]);
    this.map.fitBounds(bounds, { padding: 60, maxZoom: 14 });
  }

  ngOnDestroy() {
    this.markers.forEach(m => m.remove());
    this.map?.remove();
  }
}
