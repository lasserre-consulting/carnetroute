import {
  Component, inject, signal, output, ChangeDetectionStrategy, DestroyRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, switchMap, catchError, of } from 'rxjs';
import { Subject } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SimulationService } from '../../../core/services/simulation.service';
import { AddressSuggestion } from '../../../core/models/simulation.model';

@Component({
  selector: 'app-address-input',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  inputs: ['label', 'placeholder', 'icon'],
  template: `
    <div class="relative">
      <label class="label">{{ label }}</label>
      <div class="relative">
        <span class="absolute left-3 top-1/2 -translate-y-1/2 text-lg">{{ icon }}</span>
        <input
          type="text"
          class="input pl-9"
          [placeholder]="placeholder"
          [(ngModel)]="query"
          (ngModelChange)="onQueryChange($event)"
          (focus)="showSuggestions.set(true)"
          (blur)="onBlur()"
          autocomplete="off"
        />
        @if (isLoading()) {
          <span class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">...</span>
        }
      </div>

      @if (showSuggestions() && suggestions().length > 0) {
        <ul class="absolute z-50 w-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-600 rounded-lg shadow-lg mt-1 max-h-48 overflow-y-auto">
          @for (suggestion of suggestions(); track suggestion.label) {
            <li
              class="px-4 py-3 hover:bg-primary-50 dark:hover:bg-primary-900/30 cursor-pointer text-sm border-b border-gray-100 dark:border-gray-700 last:border-0"
              (mousedown)="selectSuggestion(suggestion)"
            >
              <span class="font-medium text-gray-900 dark:text-gray-100">{{ suggestion.label }}</span>
              @if (suggestion.city) {
                <span class="text-gray-500 dark:text-gray-400 ml-1">— {{ suggestion.city }}</span>
              }
            </li>
          }
        </ul>
      }
    </div>
  `
})
export class AddressInputComponent {
  private readonly simService = inject(SimulationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly querySubject = new Subject<string>();

  readonly selected = output<AddressSuggestion>();

  label = 'Adresse';
  placeholder = 'Saisissez une adresse...';
  icon = '📍';

  query = '';
  readonly suggestions = signal<AddressSuggestion[]>([]);
  readonly isLoading = signal(false);
  readonly showSuggestions = signal(false);

  constructor() {
    this.querySubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => {
        if (q.length < 3) { this.suggestions.set([]); return of([]); }
        this.isLoading.set(true);
        return this.simService.autocomplete(q).pipe(catchError(() => of([])));
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(results => {
      this.suggestions.set(results);
      this.isLoading.set(false);
    });
  }

  onQueryChange(value: string) { this.querySubject.next(value); }

  selectSuggestion(suggestion: AddressSuggestion) {
    this.query = suggestion.label;
    this.suggestions.set([]);
    this.showSuggestions.set(false);
    this.selected.emit(suggestion);
  }

  onBlur() { setTimeout(() => this.showSuggestions.set(false), 200); }
}
