import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { LandlordBookings } from './landlord-bookings';
import { BookingService } from '../../../core/services/booking';
import { BookingResponse, PaginatedResponse } from '../../../core/models/booking.models';

describe('LandlordBookings', () => {
  let component: LandlordBookings;
  let fixture: ComponentFixture<LandlordBookings>;
  let bookingServiceMock: {
    getLandlordBookings: ReturnType<typeof vi.fn>;
    confirmBooking: ReturnType<typeof vi.fn>;
    rejectBooking: ReturnType<typeof vi.fn>;
    cancelBooking: ReturnType<typeof vi.fn>;
  };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };

  const mockBooking: BookingResponse = {
    id: 1,
    listing: { id: 1, title: 'Studio Eyang', address: 'Eyang', thumbnailUrl: null, monthlyPrice: 50000 },
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
      getLandlordBookings: vi.fn().mockReturnValue(of(mockPage)),
      confirmBooking: vi.fn(),
      rejectBooking: vi.fn(),
      cancelBooking: vi.fn(),
    };
    messageServiceMock = { add: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LandlordBookings],
      providers: [
        { provide: BookingService, useValue: bookingServiceMock },
        { provide: MessageService, useValue: messageServiceMock },
      ],
    })
      .overrideComponent(LandlordBookings, {
        set: {
          imports: [],
          schemas: [NO_ERRORS_SCHEMA],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(LandlordBookings);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load landlord bookings on init', () => {
    expect(bookingServiceMock.getLandlordBookings).toHaveBeenCalled();
  });

  it('should show error toast on load failure', () => {
    bookingServiceMock.getLandlordBookings.mockReturnValue(
      throwError(() => new Error('Network error')),
    );
    (component as any).performLoad(1, 0);
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

  it('should call confirmBooking on confirm', () => {
    bookingServiceMock.confirmBooking.mockReturnValue(of(mockBooking));
    (component as any).confirmBooking(1);
    expect(bookingServiceMock.confirmBooking).toHaveBeenCalledWith(1);
  });

  it('should reload after successful confirm', () => {
    bookingServiceMock.confirmBooking.mockReturnValue(of(mockBooking));
    bookingServiceMock.getLandlordBookings.mockClear();
    (component as any).confirmBooking(1);
    expect(bookingServiceMock.getLandlordBookings).toHaveBeenCalled();
  });

  it('should show error toast on confirm failure', () => {
    bookingServiceMock.confirmBooking.mockReturnValue(
      throwError(() => ({ error: { message: 'Cannot confirm' } })),
    );
    (component as any).confirmBooking(1);
    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
  });

  it('should open reject dialog with correct id', () => {
    (component as any).openRejectDialog(42);
    expect((component as any).rejectingId()).toBe(42);
    expect((component as any).rejectDialogVisible()).toBe(true);
  });

  it('should call rejectBooking on doReject', () => {
    bookingServiceMock.rejectBooking.mockReturnValue(of(mockBooking));
    (component as any).rejectingId.set(5);
    (component as any).rejectReason.set('Logement indisponible');
    (component as any).doReject();
    expect(bookingServiceMock.rejectBooking).toHaveBeenCalledWith(5, 'Logement indisponible');
  });

  it('should open cancel dialog with correct id', () => {
    (component as any).openCancelDialog(7);
    expect((component as any).cancellingId()).toBe(7);
    expect((component as any).cancelDialogVisible()).toBe(true);
  });

  it('should call cancelBooking on doCancel', () => {
    bookingServiceMock.cancelBooking.mockReturnValue(of(mockBooking));
    (component as any).cancellingId.set(3);
    (component as any).doCancel();
    expect(bookingServiceMock.cancelBooking).toHaveBeenCalledWith(3);
  });

  it('should compute stagger delay', () => {
    expect((component as any).staggerDelay(0)).toBe(0);
    expect((component as any).staggerDelay(5)).toBeCloseTo(0.3);
    expect((component as any).staggerDelay(15)).toBeCloseTo(0.6);
  });
});
