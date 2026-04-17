import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FuelPrice } from '../models/fuel-price.model';

@Injectable({ providedIn: 'root' })
export class FuelPriceService {
  private readonly http = inject(HttpClient);

  getFuelPrices(): Observable<FuelPrice[]> {
    return this.http.get<FuelPrice[]>('/api/fuels');
  }
}
