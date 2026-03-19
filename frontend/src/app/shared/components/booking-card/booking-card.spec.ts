import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BookingCard } from './booking-card';
import { BookingResponse } from '../../../core/models/booking.models';

describe('BookingCard', () => {
  let component: BookingCard;
  let fixture: ComponentFixture<BookingCard>;

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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BookingCard],
    })
      .overrideComponent(BookingCard, {
        set: {
          imports: [],
          schemas: [NO_ERRORS_SCHEMA],
          template: '',
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(BookingCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('booking', mockBooking);
    fixture.componentRef.setInput('viewMode', 'student');
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should compute student initials', () => {
    expect((component as any).studentInitials()).toBe('PB');
  });

  it('should compute correct status severity for PENDING', () => {
    expect((component as any).statusSeverity()).toBe('warn');
  });

  it('should compute correct status label for PENDING', () => {
    expect((component as any).statusLabel()).toBe('En attente');
  });

  it('should allow cancel for PENDING status', () => {
    expect((component as any).canCancel()).toBe(true);
  });

  it('should not show confirm/reject for student viewMode', () => {
    expect((component as any).canConfirmOrReject()).toBe(false);
  });

  it('should show confirm/reject for landlord viewMode with PENDING', () => {
    fixture.componentRef.setInput('viewMode', 'landlord');
    fixture.detectChanges();
    expect((component as any).canConfirmOrReject()).toBe(true);
  });

  it('should show pay button for student with CONFIRMED status', () => {
    fixture.componentRef.setInput('booking', { ...mockBooking, status: 'CONFIRMED' });
    fixture.detectChanges();
    expect((component as any).canPay()).toBe(true);
  });

  it('should not show pay button for landlord', () => {
    fixture.componentRef.setInput('viewMode', 'landlord');
    fixture.componentRef.setInput('booking', { ...mockBooking, status: 'CONFIRMED' });
    fixture.detectChanges();
    expect((component as any).canPay()).toBe(false);
  });

  it('should compute severity for all statuses', () => {
    const expectations: Record<string, string> = {
      PENDING: 'warn',
      CONFIRMED: 'success',
      CANCELLED: 'danger',
      COMPLETED: 'info',
      EXPIRED: 'secondary',
    };
    for (const [status, severity] of Object.entries(expectations)) {
      fixture.componentRef.setInput('booking', { ...mockBooking, status });
      fixture.detectChanges();
      expect((component as any).statusSeverity()).toBe(severity);
    }
  });
});
