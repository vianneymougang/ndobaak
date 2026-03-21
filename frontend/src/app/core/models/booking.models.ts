export interface CreateBookingRequest {
  listingId: number;
  startDate: string;
  endDate: string;
}

export interface BookingListingSummary {
  id: number;
  title: string;
  address: string;
  thumbnailUrl: string | null;
  monthlyPrice: number;
}

export interface BookingStudentSummary {
  id: number;
  firstName: string;
  lastName: string;
  profilePhotoUrl: string | null;
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'EXPIRED' | 'COMPLETED';

export interface BookingResponse {
  id: number;
  listing: BookingListingSummary;
  student: BookingStudentSummary;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  totalAmount: number;
  status: BookingStatus;
  cancellationReason: string | null;
  landlordResponseDate: string | null;
  createdAt: string;
}

export interface BookingStatusUpdateRequest {
  reason?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}
