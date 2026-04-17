import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col items-center justify-center gap-3 py-8">
      <div class="animate-spin rounded-full border-4 border-gray-200 border-t-primary-600"
           [style.width.px]="size()" [style.height.px]="size()"></div>
      @if (message()) {
        <p class="text-sm text-gray-500">{{ message() }}</p>
      }
    </div>
  `
})
export class LoadingSpinnerComponent {
  readonly size = input(40);
  readonly message = input('');
}
