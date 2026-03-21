import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { ReviewList } from './review-list';
import { ReviewService } from '../../../core/services/review';
import { RatingStatsResponse, ReviewResponse } from '../../../core/models/review.models';
import { PaginatedResponse } from '../../../core/models/booking.models';

describe('ReviewList', () => {
  let component: ReviewList;
  let fixture: ComponentFixture<ReviewList>;
  let reviewServiceMock: {
    getReviewsByListing: ReturnType<typeof vi.fn>;
    getRatingStats: ReturnType<typeof vi.fn>;
  };

  const mockStats: RatingStatsResponse = {
    averageRating: 4.2,
    totalReviews: 10,
    distribution: { 5: 5, 4: 3, 3: 1, 2: 1, 1: 0 },
  };

  const mockReview: ReviewResponse = {
    id: 1,
    studentName: 'Jean Paul',
    studentAvatar: null,
    rating: 4,
    comment: 'Very nice place',
    photoUrls: null,
    status: 'APPROVED',
    createdAt: '2026-03-19',
  };

  const mockReviewPage: PaginatedResponse<ReviewResponse> = {
    content: [mockReview],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    hasNext: false,
  };

  beforeEach(async () => {
    reviewServiceMock = {
      getReviewsByListing: vi.fn().mockReturnValue(of(mockReviewPage)),
      getRatingStats: vi.fn().mockReturnValue(of(mockStats)),
    };

    await TestBed.configureTestingModule({
      imports: [ReviewList],
      providers: [
        { provide: ReviewService, useValue: reviewServiceMock },
      ],
    })
      .overrideComponent(ReviewList, {
        set: {
          imports: [DatePipe, FormsModule],
          schemas: [NO_ERRORS_SCHEMA],
          template: '',
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ReviewList);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('listingId', 1);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load reviews and stats on init', () => {
    expect(reviewServiceMock.getRatingStats).toHaveBeenCalledWith(1);
    expect(reviewServiceMock.getReviewsByListing).toHaveBeenCalledWith(1, 0, 10, 'createdAt,desc');
  });

  it('should compute distribution bars correctly', () => {
    const bars = (component as any).distributionBars();
    expect(bars.length).toBe(5);
    expect(bars[0]).toEqual({ star: 5, count: 5, percentage: 50 });
    expect(bars[1]).toEqual({ star: 4, count: 3, percentage: 30 });
    expect(bars[2]).toEqual({ star: 3, count: 1, percentage: 10 });
    expect(bars[3]).toEqual({ star: 2, count: 1, percentage: 10 });
    expect(bars[4]).toEqual({ star: 1, count: 0, percentage: 0 });
  });

  it('should return empty distribution bars when no stats', () => {
    (component as any).ratingStats.set(null);
    expect((component as any).distributionBars()).toEqual([]);
  });

  it('should reset page on sort change', () => {
    (component as any).page.set(3);
    reviewServiceMock.getReviewsByListing.mockClear();

    (component as any).onSortChange();

    expect((component as any).page()).toBe(0);
    expect(reviewServiceMock.getReviewsByListing).toHaveBeenCalled();
  });

  it('should toggle expanded state', () => {
    expect((component as any).isExpanded(1)).toBe(false);
    (component as any).toggleExpanded(1);
    expect((component as any).isExpanded(1)).toBe(true);
    (component as any).toggleExpanded(1);
    expect((component as any).isExpanded(1)).toBe(false);
  });

  it('should get correct initials for two-word name', () => {
    expect((component as any).getInitials('Jean Paul')).toBe('JP');
  });

  it('should get correct initials for single name', () => {
    expect((component as any).getInitials('Marie')).toBe('M');
  });

  it('should get correct initials for three-word name (max 2 chars)', () => {
    expect((component as any).getInitials('Ana Clara Souza')).toBe('AC');
  });
});
