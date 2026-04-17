import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Simulation, SimulateRequest, HeatmapResponse, AddressSuggestion } from '../models/simulation.model';

@Injectable({ providedIn: 'root' })
export class SimulationService {
  private readonly http = inject(HttpClient);

  simulate(request: SimulateRequest): Observable<Simulation> {
    return this.http.post<Simulation>('/api/simulate', request);
  }

  getHeatmap(): Observable<HeatmapResponse> {
    return this.http.post<HeatmapResponse>('/api/heatmap', {});
  }

  autocomplete(query: string, limit = 5): Observable<AddressSuggestion[]> {
    const params = new HttpParams().set('q', query).set('limit', limit);
    return this.http.get<AddressSuggestion[]>('/api/geocode', { params });
  }

  getHistory(page = 0, size = 20): Observable<{ content: Simulation[]; totalElements: number }> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>('/api/simulations', { params });
  }
}
