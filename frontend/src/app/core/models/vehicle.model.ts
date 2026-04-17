export interface Vehicle {
  id: string;
  userId: string;
  name: string;
  fuelType: string;
  consumptionPer100km: number;
  costPerUnit: number;
  tankCapacity: number;
  yearMake: number;
  isDefault: boolean;
  createdAt: string;
}

export interface CreateVehicleRequest {
  name: string;
  fuelType: string;
  consumptionPer100km?: number;
  costPerUnit?: number;
  tankCapacity?: number;
  yearMake?: number;
  isDefault?: boolean;
}

export const FUEL_TYPES = [
  { key: 'SP95', label: 'SP95 (E10)', unit: 'L', color: 'green', defaultPrice: 1.85, defaultConsumption: 7.0 },
  { key: 'SP98', label: 'SP98 (E5)', unit: 'L', color: 'green', defaultPrice: 1.96, defaultConsumption: 7.2 },
  { key: 'DIESEL', label: 'Diesel (B7)', unit: 'L', color: 'amber', defaultPrice: 2.19, defaultConsumption: 5.8 },
  { key: 'E85', label: 'Éthanol E85', unit: 'L', color: 'lime', defaultPrice: 0.73, defaultConsumption: 9.5 },
  { key: 'GPL', label: 'GPL', unit: 'L', color: 'cyan', defaultPrice: 1.05, defaultConsumption: 9.8 },
  { key: 'ELECTRIC', label: 'Électrique', unit: 'kWh', color: 'purple', defaultPrice: 0.44, defaultConsumption: 17.0 },
] as const;
