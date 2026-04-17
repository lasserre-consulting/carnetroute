import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface JourneyHistory {
  id: string;
  simulationId: string;
  fuelType: string;
  distanceKm: number;
  costTotal: number;
  durationMinutes: number;
  carbonEmissionKg: number;
  createdAt: string;
  tags: string[];
}

export interface UserStats {
  totalJourneys: number;
  totalDistanceKm: number;
  totalCostEur: number;
  totalDurationMinutes: number;
  carbonEmissionKg: number;
  mostUsedFuelType?: string;
  monthlyStats: { [month: string]: { month: string; journeys: number; distanceKm: number; costEur: number } };
}

@Injectable({ providedIn: 'root' })
export class HistoryService {
  private readonly http = inject(HttpClient);

  getHistory(page = 0, size = 20): Observable<{ content: JourneyHistory[]; totalElements: number }> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>('/api/history', { params });
  }

  getStats(): Observable<UserStats> {
    return this.http.get<UserStats>('/api/stats');
  }
}
