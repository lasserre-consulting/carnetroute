export interface Coordinates {
  lat: number;
  lng: number;
  label?: string;
}

export interface SimulationRequest {
  from: Coordinates;
  to: Coordinates;
  fuelType: string;
  customPrice?: number;
  customConsumption?: number;
  trafficMode: 'manual' | 'auto';
  manualTrafficLevel: number;
  departureDay: number;
  departureHour: number;
  avoidTolls?: boolean;
}

export interface SimulationResult {
  distanceKm: number;
  baseTimeMin: number;
  adjustedTimeMin: number;
  trafficFactor: number;
  trafficLabel: string;
  trafficIcon: string;
  fuelConsumed: number;
  fuelUnit: string;
  totalCost: number;
  costPer100km: number;
  avgSpeedKmh: number;
  costPerHour: number;
  comparison: FuelComparison[];
  routingSource?: string;
  routeGeometry?: number[][]; // [[lng, lat], ...]
}

export interface FuelComparison {
  key: string;
  label: string;
  icon: string;
  color: string;
  cost: number;
  consumption: number;
  unit: string;
}

export interface WeeklyHeatmapResult {
  baseTimeMin: number;
  distanceKm: number;
  grid: HeatmapCell[][];
}

export interface HeatmapCell {
  day: number;
  hour: number;
  durationMin: number;
  trafficFactor: number;
  cost: number;
}

export interface FuelProfile {
  key: string;
  label: string;
  icon: string;
  consumption: number;
  unit: string;
  defaultPrice: number;
  priceUnit: string;
  color: string;
  shortName: string;
}

export interface GeoFeature {
  properties: {
    label: string;
    context: string;
    type: string;
    city?: string;
  };
  geometry: {
    coordinates: [number, number]; // [lng, lat]
  };
}

export interface GeoResponse {
  features: GeoFeature[];
}

export const DAY_NAMES = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
export const DAY_NAMES_FULL = ['Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi', 'Dimanche'];
