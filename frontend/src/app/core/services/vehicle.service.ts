import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Vehicle, CreateVehicleRequest } from '../models/vehicle.model';

@Injectable({ providedIn: 'root' })
export class VehicleService {
  private readonly http = inject(HttpClient);

  getVehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>('/api/vehicles');
  }

  createVehicle(request: CreateVehicleRequest): Observable<Vehicle> {
    return this.http.post<Vehicle>('/api/vehicles', request);
  }

  updateVehicle(id: string, request: Partial<CreateVehicleRequest>): Observable<Vehicle> {
    return this.http.put<Vehicle>(`/api/vehicles/${id}`, request);
  }

  deleteVehicle(id: string): Observable<void> {
    return this.http.delete<void>(`/api/vehicles/${id}`);
  }
}
