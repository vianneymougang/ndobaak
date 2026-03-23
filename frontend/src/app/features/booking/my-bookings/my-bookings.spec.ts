import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TabsModule } from 'primeng/tabs';
import { PaginatorModule } from 'primeng/paginator';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { DialogModule } from 'primeng/dialog';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { MyBookings } from './my-bookings';
import { BookingCard } from '../../../shared/components/booking-card/booking-card';
import { BookingService } from '../../../core/services/booking';
import { BookingResponse, PaginatedResponse } from '../../../core/models/booking.models';

describe('MyBookings', () => {
  let component: MyBookings;
  let fixture: ComponentFixture<MyBookings>;
  let bookingServiceMock: {
    getMyBookings: ReturnType<typeof vi.fn>;
    cancelBooking: ReturnType<typeof vi.fn>;
  };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };

  const mockBooking: BookingResponse = {
    id: 1,
    listing: { id: 1, title: 'Studio', address: 'Eyang', thumbnailUrl: null, monthlyPrice: 50000 },
    student: { id: 2, firstName: 'Paul', lastName: 'Biya', profilePhotoUrl: null },
    startDate: '2026-04-01',
    endDate: '2026-07-01',
    monthlyRent: 50000,
    totalAmount: 150000,
    status: 'PENDING',
    cancellationReason: null,
    landlordResponseDate: null,
    createdAt: '2026-03-19',
  };

  const mockPage: PaginatedResponse<BookingResponse> = {
    content: [mockBooking],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    hasNext: false,
  };

  beforeEach(async () => {
    bookingServiceMock = {
      getMyBookings: vi.fn().mockReturnValue(of(mockPage)),
      cancelBooking: vi.fn(),
    };
    messageServiceMock = { add: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [MyBookings],
      providers: [
        { provide: BookingService, useValue: bookingServiceMock },
        { provide: MessageService, useValue: messageServiceMock },
      ],
    })
      .overrideComponent(MyBookings, {
        set: {
          imports: [],
          schemas: [NO_ERRORS_SCHEMA],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(MyBookings);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load bookings on init', () => {
    expect(bookingServiceMock.getMyBookings).toHaveBeenCalled();
  });

  it('should show error toast on load failure', () => {
    bookingServiceMock.getMyBookings.mockReturnValue(
      throwError(() => new Error('Network error')),
    );

    (component as any).performLoad(0, 0);

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
  });

  it('should reset page on tab change', () => {
    (component as any).page.set(3);
    (component as any).onTabChange(2);
    expect((component as any).page()).toBe(0);
    expect((component as any).activeTab()).toBe(2);
  });

  it('should open cancel dialog with correct id', () => {
    (component as any).openCancelDialog(42);
    expect((component as any).cancellingId()).toBe(42);
    expect((component as any).cancelDialogVisible()).toBe(true);
  });

  it('should call cancelBooking on confirmCancel', () => {
    bookingServiceMock.cancelBooking.mockReturnValue(of(mockBooking));
    (component as any).cancellingId.set(5);

    (component as any).confirmCancel();

    expect(bookingServiceMock.cancelBooking).toHaveBeenCalledWith(5);
  });

  it('should reload bookings after successful cancel', () => {
    bookingServiceMock.cancelBooking.mockReturnValue(of(mockBooking));
    bookingServiceMock.getMyBookings.mockClear();
    (component as any).cancellingId.set(5);

    (component as any).confirmCancel();

    expect(bookingServiceMock.getMyBookings).toHaveBeenCalled();
  });

  it('should show error toast on cancel failure', () => {
    bookingServiceMock.cancelBooking.mockReturnValue(
      throwError(() => ({ error: { message: 'Cannot cancel' } })),
    );
    (component as any).cancellingId.set(5);

    (component as any).confirmCancel();

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
  });

  it('should compute stagger delay', () => {
    expect((component as any).staggerDelay(0)).toBe(0);
    expect((component as any).staggerDelay(5)).toBeCloseTo(0.3);
    expect((component as any).staggerDelay(15)).toBeCloseTo(0.6);
  });
});
