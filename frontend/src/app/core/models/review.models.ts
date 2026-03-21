export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ReviewResponse {
  id: number;
  studentName: string;
  studentAvatar: string | null;
  rating: number;
  comment: string | null;
  photoUrls: string[] | null;
  status: ReviewStatus;
  createdAt: string;
}

export interface RatingStatsResponse {
  averageRating: number;
  totalReviews: number;
  distribution: Record<number, number>;
}

export interface CreateReviewRequest {
  listingId: number;
  rating: number;
  comment?: string;
}
