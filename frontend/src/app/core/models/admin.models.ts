export type AdminUserRole = 'STUDENT' | 'LANDLORD' | 'ADMIN';
export type AdminUserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
export type AdminReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type AdminPaymentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type AdminPaymentType = 'BOOKING' | 'SUBSCRIPTION';
export type RejectionMotif =
  | 'INACCURATE_PHOTOS'
  | 'MISLEADING_DESCRIPTION'
  | 'INVALID_PRICE'
  | 'INCOMPLETE_INFORMATION'
  | 'DUPLICATE_LISTING'
  | 'LOCATION_TOO_FAR'
  | 'UNVERIFIED_LANDLORD'
  | 'OTHER';

export interface AdminUserListItem {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: AdminUserRole;
  status: AdminUserStatus;
  deactivated: boolean;
  registrationDate: string;
  listingCount?: number;
  bookingCount?: number;
}

export interface AdminUserDetail extends AdminUserListItem {
  deactivationReason?: string;
  profilePhotoUrl?: string;
  activeListingCount?: number;
  totalRevenue?: number;
  completedBookingCount?: number;
  reviewCount?: number;
}

export interface AdminReview {
  id: number;
  rating: number;
  comment?: string;
  status: AdminReviewStatus;
  createdAt: string;
  studentName: string;
  studentId: number;
  listingTitle: string;
  listingId: number;
  reportCount: number;
  photoUrls?: string[];
}

export interface ReviewReport {
  id: number;
  reviewId: number;
  reporterName: string;
  reporterId: number;
  reason: string;
  processed: boolean;
  resolvedByName?: string;
  resolvedAt?: string;
  createdAt: string;
}

export interface MonthlyStatPoint {
  month: string;
  value: number;
}

export interface GlobalStats {
  totalUsers: number;
  totalLandlords: number;
  totalStudents: number;
  totalListings: number;
  publishedListings: number;
  pendingListings: number;
  totalBookings: number;
  confirmedBookings: number;
  totalRevenue: number;
  activeSubscriptions: number;
  newUsersThisMonth: number;
  newBookingsThisMonth: number;
  monthlyUsers: MonthlyStatPoint[];
  monthlyBookings: MonthlyStatPoint[];
  monthlyRevenue: MonthlyStatPoint[];
}

export interface AdminTransaction {
  id: number;
  amount: number;
  paymentType: AdminPaymentType;
  status: AdminPaymentStatus;
  payerName?: string;
  phoneNumber?: string;
  operator?: string;
  providerName?: string;
  transactionId?: string;
  createdAt: string;
  paidAt?: string;
  bookingId?: number;
  anomaly: boolean;
  anomalyReason?: string;
}

export interface AdminPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface RejectionMotifRequest {
  motif: RejectionMotif;
  additionalReason?: string;
}

export interface DeactivateUserRequest {
  reason: string;
}

export interface ResolveReportRequest {
  approveReview: boolean;
}

export const REJECTION_MOTIF_LABELS: Record<RejectionMotif, string> = {
  INACCURATE_PHOTOS: 'Photos inexactes',
  MISLEADING_DESCRIPTION: 'Description trompeuse',
  INVALID_PRICE: 'Prix invalide',
  INCOMPLETE_INFORMATION: 'Informations incomplètes',
  DUPLICATE_LISTING: 'Annonce en double',
  LOCATION_TOO_FAR: 'Localisation hors zone',
  UNVERIFIED_LANDLORD: 'Propriétaire non vérifié',
  OTHER: 'Autre motif',
};
