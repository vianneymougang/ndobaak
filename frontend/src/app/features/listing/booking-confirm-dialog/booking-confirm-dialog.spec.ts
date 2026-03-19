import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { BookingConfirmDialog } from './booking-confirm-dialog';
import { BookingService } from '../../../core/services/booking';
import { BookingResponse } from '../../../core/models/booking.models';
import { ListingResponse } from '../../../core/models/listing.models';

describe('BookingConfirmDialog', () => {
  let component: BookingConfirmDialog;
  let fixture: ComponentFixture<BookingConfirmDialog>;
  let bookingServiceMock: { createBooking: ReturnType<typeof vi.fn> };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };

  const mockListing: ListingResponse = {
    id: 1,
    title: 'Studio Centre',
    description: 'Nice studio',
    roomType: 'STUDIO',
    monthlyPrice: 50000,
    availableRooms: 2,
    status: 'PUBLISHED',
    createdAt: '2026-01-01',
    updatedAt: '2026-01-01',
    photos: [{ id: 1, url: '/photo.jpg', thumbnailUrl: '/thumb.jpg', displayOrder: 0 }],
    amenities: [],
    landlord: { id: 1, firstName: 'Jean', lastName: 'Dupont' },
  };

  const mockBookingResponse: BookingResponse = {
    id: 10,
    listing: { id: 1, title: 'Studio Centre', address: 'Eyang', thumbnailUrl: null, monthlyPrice: 50000 },
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

  beforeEach(async () => {
    bookingServiceMock = { createBooking: vi.fn() };
    messageServiceMock = { add: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [BookingConfirmDialog],
      providers: [
        { provide: BookingService, useValue: bookingServiceMock },
        { provide: MessageService, useValue: messageServiceMock },
      ],
    })
      .overrideComponent(BookingConfirmDialog, {
        set: {
          imports: [FormsModule],
          schemas: [NO_ERRORS_SCHEMA],
          template: '',
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(BookingConfirmDialog);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('listing', mockListing);
    fixture.componentRef.setInput('startDate', new Date(2026, 3, 1));
    fixture.componentRef.setInput('duration', 3);
    fixture.componentRef.setInput('totalAmount', 150000);
    fixture.componentRef.setInput('visible', false);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should compute endDate correctly', () => {
    const endDate = (component as any).endDate();
    const startDate = new Date(2026, 3, 1);
    const expectedEnd = new Date(startDate);
    expectedEnd.setMonth(expectedEnd.getMonth() + 3);
    expect(endDate.getFullYear()).toBe(expectedEnd.getFullYear());
    expect(endDate.getMonth()).toBe(expectedEnd.getMonth());
    expect(endDate.getDate()).toBe(expectedEnd.getDate());
  });

  it('should not submit when terms not accepted', () => {
    (component as any).termsAccepted.set(false);
    (component as any).submitBooking();
    expect(bookingServiceMock.createBooking).not.toHaveBeenCalled();
  });

  it('should not submit when already submitting', () => {
    (component as any).termsAccepted.set(true);
    (component as any).submitting.set(true);
    (component as any).submitBooking();
    expect(bookingServiceMock.createBooking).not.toHaveBeenCalled();
  });

  it('should call bookingService.createBooking on submit', () => {
    bookingServiceMock.createBooking.mockReturnValue(of(mockBookingResponse));
    (component as any).termsAccepted.set(true);

    (component as any).submitBooking();

    expect(bookingServiceMock.createBooking).toHaveBeenCalledWith(
      expect.objectContaining({ listingId: 1 }),
    );
  });

  it('should emit confirmed and close dialog on success', () => {
    bookingServiceMock.createBooking.mockReturnValue(of(mockBookingResponse));
    (component as any).termsAccepted.set(true);

    const confirmedSpy = vi.spyOn(component.confirmed, 'emit');

    (component as any).submitBooking();

    expect(confirmedSpy).toHaveBeenCalledWith(mockBookingResponse);
    expect(component.visible()).toBe(false);
    expect((component as any).termsAccepted()).toBe(false);
  });

  it('should show success toast on successful submit', () => {
    bookingServiceMock.createBooking.mockReturnValue(of(mockBookingResponse));
    (component as any).termsAccepted.set(true);

    (component as any).submitBooking();

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success' }),
    );
  });

  it('should show error toast on failure', () => {
    bookingServiceMock.createBooking.mockReturnValue(
      throwError(() => ({ error: { message: 'Booking failed' } })),
    );
    (component as any).termsAccepted.set(true);

    (component as any).submitBooking();

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
  });

  it('should set submitting state during submission', () => {
    let submittingDuringCall = false;
    bookingServiceMock.createBooking.mockImplementation(() => {
      submittingDuringCall = (component as any).submitting();
      return of(mockBookingResponse);
    });
    (component as any).termsAccepted.set(true);

    (component as any).submitBooking();

    expect(submittingDuringCall).toBe(true);
    expect((component as any).submitting()).toBe(false);
  });
});
