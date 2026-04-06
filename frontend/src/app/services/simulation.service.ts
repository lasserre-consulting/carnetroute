import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, debounceTime, switchMap, of, catchError } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  SimulationRequest, SimulationResult,
  WeeklyHeatmapResult, FuelProfile,
  GeoResponse
} from '../models/simulation.model';

@Injectable({ providedIn: 'root' })
export class SimulationService {

  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getFuels(): Observable<FuelProfile[]> {
    return this.http.get<FuelProfile[]>(`${this.baseUrl}/fuels`);
  }

  simulate(request: SimulationRequest): Observable<SimulationResult> {
    return this.http.post<SimulationResult>(`${this.baseUrl}/simulate`, request);
  }

  getHeatmap(request: SimulationRequest): Observable<WeeklyHeatmapResult> {
    return this.http.post<WeeklyHeatmapResult>(`${this.baseUrl}/heatmap`, request);
  }

  geocode(query: string): Observable<GeoResponse> {
    if (!query || query.length < 3) {
      return of({ features: [] });
    }
    return this.http.get<GeoResponse>(`${this.baseUrl}/geocode`, {
      params: { q: query, limit: '7' }
    }).pipe(
      catchError(() => of({ features: [] }))
    );
  }
}
