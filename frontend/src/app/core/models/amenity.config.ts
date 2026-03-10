export interface AmenityConfig {
  label: string;
  icon: string;
}

export const AMENITY_CONFIG: Record<string, AmenityConfig> = {
  WiFi: { label: 'WiFi', icon: 'pi pi-wifi' },
  'Running Water': { label: 'Eau courante', icon: 'pi pi-filter' },
  Electricity: { label: 'Électricité', icon: 'pi pi-bolt' },
  'Private Shower': { label: 'Douche privée', icon: 'pi pi-refresh' },
  Kitchen: { label: 'Cuisine', icon: 'pi pi-home' },
  'Security Guard': { label: 'Gardien', icon: 'pi pi-shield' },
  Parking: { label: 'Parking', icon: 'pi pi-car' },
  Fan: { label: 'Ventilateur', icon: 'pi pi-sun' },
  Furnished: { label: 'Meublé', icon: 'pi pi-box' },
  'Private Toilet': { label: 'Toilettes privées', icon: 'pi pi-building' },
};

export function amenityLabel(name: string): string {
  return AMENITY_CONFIG[name]?.label ?? name;
}

export function amenityIcon(name: string): string {
  return AMENITY_CONFIG[name]?.icon ?? 'pi pi-check';
}
