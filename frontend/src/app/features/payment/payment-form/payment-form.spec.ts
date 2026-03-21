import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { PaymentForm } from './payment-form';
import { BookingService } from '../../../core/services/booking';
import { PaymentService } from '../../../core/services/payment';
import { BookingResponse } from '../../../core/models/booking.models';
import { PaymentResponse } from '../../../core/models/payment.models';

describe('PaymentForm', () => {
  let component: PaymentForm;
  let fixture: ComponentFixture<PaymentForm>;
  let bookingServiceMock: { getBookingById: ReturnType<typeof vi.fn> };
  let paymentServiceMock: { initiatePayment: ReturnType<typeof vi.fn> };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  const mockBooking: BookingResponse = {
    id: 1,
    listing: { id: 1, title: 'Studio', address: 'Eyang', thumbnailUrl: null, monthlyPrice: 50000 },
    student: { id: 2, firstName: 'Paul', lastName: 'Biya', profilePhotoUrl: null },
    startDate: '2026-04-01',
    endDate: '2026-07-01',
    monthlyRent: 50000,
    totalAmount: 150000,
    status: 'CONFIRMED',
    cancellationReason: null,
    landlordResponseDate: null,
    createdAt: '2026-03-19',
  };

  const mockPayment: PaymentResponse = {
    id: 100,
    bookingId: 1,
    amount: 150000,
    commission: 15000,
    totalCharged: 165000,
    status: 'PROCESSING',
    paymentType: 'INTEGRAL',
    paymentMethod: 'MOBILE_MONEY',
    operator: 'MTN_MOMO',
    transactionId: 'TX123',
    providerName: 'notchpay',
    createdAt: '2026-03-19',
  };

  beforeEach(async () => {
    bookingServiceMock = { getBookingById: vi.fn().mockReturnValue(of(mockBooking)) };
    paymentServiceMock = { initiatePayment: vi.fn() };
    messageServiceMock = { add: vi.fn() };
    routerMock = { navigate: vi.fn().mockReturnValue(Promise.resolve(true)) };

    await TestBed.configureTestingModule({
      imports: [PaymentForm],
      providers: [
        { provide: BookingService, useValue: bookingServiceMock },
        { provide: PaymentService, useValue: paymentServiceMock },
        { provide: MessageService, useValue: messageServiceMock },
        { provide: Router, useValue: routerMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } },
        },
      ],
    })
      .overrideComponent(PaymentForm, {
        set: {
          imports: [ReactiveFormsModule],
          schemas: [NO_ERRORS_SCHEMA],
          template: '',
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PaymentForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load booking on init', () => {
    expect(bookingServiceMock.getBookingById).toHaveBeenCalledWith(1);
    expect((component as any).booking()).toEqual(mockBooking);
    expect((component as any).loading()).toBe(false);
  });

  it('should show error on booking load failure', () => {
    bookingServiceMock.getBookingById.mockReturnValue(
      throwError(() => new Error('Not found')),
    );

    const newFixture = TestBed.createComponent(PaymentForm);
    newFixture.detectChanges();
    const newComponent = newFixture.componentInstance;

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
    expect((newComponent as any).loading()).toBe(false);
  });

  it('should have INTEGRAL as default payment type', () => {
    expect((component as any).form.controls.paymentType.value).toBe('INTEGRAL');
  });

  it('should add required validator to installmentFrequency when INSTALLMENT selected', () => {
    (component as any).form.controls.paymentType.setValue('INSTALLMENT');
    (component as any).onPaymentTypeChange();

    const ctrl = (component as any).form.controls.installmentFrequency;
    ctrl.setValue(null);
    ctrl.updateValueAndValidity();

    expect(ctrl.valid).toBe(false);
    expect(ctrl.errors?.['required']).toBeTruthy();
  });

  it('should clear installmentFrequency validators when INTEGRAL selected', () => {
    (component as any).form.controls.paymentType.setValue('INSTALLMENT');
    (component as any).onPaymentTypeChange();

    (component as any).form.controls.paymentType.setValue('INTEGRAL');
    (component as any).onPaymentTypeChange();

    const ctrl = (component as any).form.controls.installmentFrequency;
    ctrl.setValue(null);
    ctrl.updateValueAndValidity();

    expect(ctrl.valid).toBe(true);
  });

  it('should not submit invalid form', () => {
    (component as any).form.controls.operator.setValue(null);
    (component as any).form.controls.phoneNumber.setValue('');

    (component as any).submit();

    expect(paymentServiceMock.initiatePayment).not.toHaveBeenCalled();
  });

  it('should call initiatePayment on valid submit', () => {
    paymentServiceMock.initiatePayment.mockReturnValue(of(mockPayment));

    (component as any).form.controls.operator.setValue('MTN_MOMO');
    (component as any).form.controls.phoneNumber.setValue('698123456');
    (component as any).form.updateValueAndValidity();

    (component as any).submit();

    expect(paymentServiceMock.initiatePayment).toHaveBeenCalledWith(
      expect.objectContaining({
        bookingId: 1,
        paymentType: 'INTEGRAL',
        operator: 'MTN_MOMO',
        phoneNumber: '237698123456',
      }),
    );
  });

  it('should navigate to processing page on success', () => {
    paymentServiceMock.initiatePayment.mockReturnValue(of(mockPayment));

    (component as any).form.controls.operator.setValue('MTN_MOMO');
    (component as any).form.controls.phoneNumber.setValue('698123456');

    (component as any).submit();

    expect(routerMock.navigate).toHaveBeenCalledWith(
      ['/bookings', 1, 'pay', 'processing'],
      expect.objectContaining({ queryParams: { paymentId: 100 } }),
    );
  });

  it('should show error on payment failure', () => {
    paymentServiceMock.initiatePayment.mockReturnValue(
      throwError(() => ({ error: { message: 'Payment refused' } })),
    );

    (component as any).form.controls.operator.setValue('MTN_MOMO');
    (component as any).form.controls.phoneNumber.setValue('698123456');

    (component as any).submit();

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
    expect((component as any).submitting()).toBe(false);
  });
});
