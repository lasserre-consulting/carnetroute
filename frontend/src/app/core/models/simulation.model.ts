export interface Coordinates {
  lat: number;
  lng: number;
  label: string;
}

export interface Route {
  from: Coordinates;
  to: Coordinates;
  distanceKm: number;
  durationMinutes: number;
  geometry?: number[][];
}

export interface TrafficConditions {
  mode: 'manual' | 'auto';
  factor: number;
  departureTime?: string;
}

export interface ComparisonEntry {
  fuelType: string;
  pricePerUnit: number;
  consumptionPer100km: number;
  fuelConsumed: number;
  totalCost: number;
  unit: string;
}

export interface CostBreakdown {
  fuelType: string;
  pricePerUnit: number;
  consumptionPer100km: number;
  fuelConsumedTotal: number;
  costTotal: number;
  durationAdjustedMinutes: number;
  comparison: { [key: string]: ComparisonEntry };
}

export interface Simulation {
  id: string;
  userId?: string;
  vehicleId?: string;
  route: Route;
  traffic: TrafficConditions;
  costs: CostBreakdown;
  createdAt: string;
}

export interface SimulateRequest {
  fromLat: number;
  fromLng: number;
  fromLabel?: string;
  toLat: number;
  toLng: number;
  toLabel?: string;
  fuelType: string;
  trafficMode?: 'manual' | 'auto';
  trafficFactor?: number;
  departureHour?: number;
  departureDay?: number;
  customPrices?: { [key: string]: number };
  customConsumptions?: { [key: string]: number };
  vehicleId?: string;
  saveToHistory?: boolean;
}

export interface HeatmapResponse {
  matrix: number[][];
}

export interface AddressSuggestion {
  label: string;
  lat: number;
  lng: number;
  city?: string;
  postcode?: string;
}
