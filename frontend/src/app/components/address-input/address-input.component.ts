import { Component, Input, Output, EventEmitter, OnDestroy, OnInit, ElementRef, ViewChild, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, debounceTime, switchMap } from 'rxjs';
import { SimulationService } from '../../services/simulation.service';
import { Coordinates, GeoFeature } from '../../models/simulation.model';

@Component({
  selector: 'app-address-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="address-input-wrapper" #wrapper>
      <div class="input-container">
        <input
          type="text"
          class="input"
          [placeholder]="placeholder"
          [(ngModel)]="query"
          (ngModelChange)="onQueryChange($event)"
          (focus)="onFocus()"
          (keydown)="onKeyDown($event)"
          [style.borderColor]="isOpen ? accentColor : ''"
        />
        <div *ngIf="loading" class="spinner-wrapper">
          <div class="spinner" [style.borderTopColor]="accentColor"></div>
        </div>
      </div>

      <div *ngIf="isOpen && suggestions.length > 0" class="dropdown">
        <button
          *ngFor="let item of suggestions; let i = index"
          class="dropdown-item"
          [class.highlighted]="i === activeIndex"
          (click)="selectItem(item)"
          (mouseenter)="activeIndex = i"
        >
          <span class="item-icon" [style.color]="accentColor">{{ getTypeIcon(item.properties.type) }}</span>
          <div class="item-content">
            <div class="item-label">{{ item.properties.label }}</div>
            <div *ngIf="item.properties.context" class="item-context">{{ item.properties.context }}</div>
          </div>
          <span class="item-coords mono">
            {{ item.geometry.coordinates[1].toFixed(3) }}, {{ item.geometry.coordinates[0].toFixed(3) }}
          </span>
        </button>
        <div class="dropdown-footer">
          <span class="mono">adresse.data.gouv.fr</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .address-input-wrapper { position: relative; }
    .input-container { position: relative; }
    .spinner-wrapper {
      position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
    }
    .dropdown {
      position: absolute; z-index: 50; width: 100%; margin-top: 4px;
      background: #1A2332; border: 1px solid #2D3748;
      border-radius: 12px; overflow: hidden;
      box-shadow: 0 20px 40px rgba(0,0,0,0.4);
      max-height: 260px; overflow-y: auto;
    }
    .dropdown-item {
      width: 100%; display: flex; align-items: flex-start; gap: 8px;
      padding: 10px 12px; background: none; border: none;
      border-bottom: 1px solid rgba(30,41,59,0.5);
      color: #CBD5E1; font-size: 14px; text-align: left; cursor: pointer;
      font-family: 'DM Sans', sans-serif;
      transition: background 0.15s;
    }
    .dropdown-item:last-of-type { border-bottom: none; }
    .dropdown-item:hover, .dropdown-item.highlighted { background: rgba(51,65,85,0.4); }
    .item-icon { flex-shrink: 0; margin-top: 2px; }
    .item-content { flex: 1; min-width: 0; }
    .item-label { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .item-context { font-size: 11px; color: #64748B; margin-top: 2px; }
    .item-coords { flex-shrink: 0; font-size: 10px; color: #475569; margin-top: 2px; }
    .dropdown-footer {
      padding: 6px 12px; text-align: center; background: #0F172A;
      font-size: 10px; color: #475569;
    }
  `]
})
export class AddressInputComponent implements OnInit, OnDestroy {
  @Input() placeholder = 'Tapez une adresse...';
  @Input() accentColor = '#06B6D4';
  @Input() initialValue = '';
  @Output() addressSelected = new EventEmitter<{ coords: Coordinates; label: string }>();

  @ViewChild('wrapper') wrapperRef!: ElementRef;

  query = '';
  suggestions: GeoFeature[] = [];
  isOpen = false;
  loading = false;
  activeIndex = -1;

  private searchSubject = new Subject<string>();
  private subscription: Subscription;

  constructor(private simulationService: SimulationService) {
    this.subscription = this.searchSubject.pipe(
      debounceTime(200),
      switchMap(q => {
        this.loading = true;
        return this.simulationService.geocode(q);
      })
    ).subscribe(response => {
      this.loading = false;
      this.suggestions = response.features || [];
      this.activeIndex = this.suggestions.length > 0 ? 0 : -1;
      this.isOpen = this.suggestions.length > 0;
    });
  }

  ngOnInit() {
    if (this.initialValue) this.query = this.initialValue;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  @HostListener('document:mousedown', ['$event'])
  onClickOutside(event: MouseEvent) {
    if (this.wrapperRef && !this.wrapperRef.nativeElement.contains(event.target)) {
      this.isOpen = false;
    }
  }

  onQueryChange(value: string) {
    if (value.length >= 2) {
      this.searchSubject.next(value);
    } else {
      this.suggestions = [];
      this.isOpen = false;
      this.activeIndex = -1;
    }
  }

  onFocus() {
    if (this.suggestions.length > 0) this.isOpen = true;
  }

  onKeyDown(event: KeyboardEvent) {
    if (!this.isOpen || this.suggestions.length === 0) return;
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.activeIndex = Math.min(this.activeIndex + 1, this.suggestions.length - 1);
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.activeIndex = Math.max(this.activeIndex - 1, 0);
        break;
      case 'Enter':
        event.preventDefault();
        if (this.activeIndex >= 0) this.selectItem(this.suggestions[this.activeIndex]);
        break;
      case 'Escape':
        this.isOpen = false;
        break;
    }
  }

  selectItem(feature: GeoFeature) {
    const [lng, lat] = feature.geometry.coordinates;
    const label = feature.properties.label;
    const city = feature.properties.city || label;
    this.query = label;
    this.isOpen = false;
    this.activeIndex = -1;
    this.addressSelected.emit({
      coords: { lat, lng, label: city },
      label
    });
  }

  getTypeIcon(type: string): string {
    switch (type) {
      case 'housenumber': return '🏠';
      case 'street': return '🛣️';
      case 'municipality': return '🏙️';
      case 'locality': return '📍';
      default: return '📍';
    }
  }
}
