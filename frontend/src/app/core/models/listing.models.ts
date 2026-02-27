export type RoomType = 'STUDIO' | 'SINGLE_ROOM' | 'DOUBLE_ROOM' | 'APARTMENT';
export type ListingStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'ARCHIVED';

export interface LocationResponse {
  id: number;
  address: string;
  latitude: number;
  longitude: number;
  distanceToCampus?: number;
  neighborhood?: string;
  city?: string;
}

export interface PhotoResponse {
  id: number;
  url: string;
  thumbnailUrl?: string;
  mediumUrl?: string;
  displayOrder: number;
}

export interface AmenityResponse {
  id: number;
  name: string;
  icon: string;
}

export interface LandlordSummary {
  id: number;
  firstName: string;
  lastName: string;
  profilePhotoUrl?: string;
  phone?: string;
  email?: string;
}

export interface ListingResponse {
  id: number;
  title: string;
  description: string;
  roomType: RoomType;
  monthlyPrice: number;
  availableRooms: number;
  availableDate?: string;
  status: ListingStatus;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
  location?: LocationResponse;
  photos: PhotoResponse[];
  amenities: AmenityResponse[];
  landlord: LandlordSummary;
}

export interface CreateListingRequest {
  title?: string;
  description?: string;
  roomType?: RoomType;
  monthlyPrice?: number;
  availableRooms?: number;
  availableDate?: string;
  location?: {
    address: string;
    latitude: number;
    longitude: number;
    neighborhood?: string;
    city?: string;
  };
  amenityNames?: string[];
}

export interface ListingSearchResponse {
  id: number;
  title: string;
  thumbnailUrl?: string;
  monthlyPrice: number;
  roomType: RoomType;
  neighborhood?: string;
  distanceToCampus?: number;
  avgRating?: number;
  reviewCount: number;
  amenities: string[];
  verified: boolean;
  latitude?: number;
  longitude?: number;
  availableRooms: number;
}

export interface SearchPageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
