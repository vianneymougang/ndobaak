import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { PaymentReceipt } from './payment-receipt';
import { PaymentService } from '../../../core/services/payment';
import { ReceiptResponse } from '../../../core/models/payment.models';

describe('PaymentReceipt', () => {
  let component: PaymentReceipt;
  let fixture: ComponentFixture<PaymentReceipt>;
  let paymentServiceMock: { getReceipt: ReturnType<typeof vi.fn> };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };

  const mockReceipt: ReceiptResponse = {
    transactionId: 'TX-12345',
    paymentDate: '2026-03-19T10:30:00',
    payerName: 'Paul Biya',
    payerPhone: '237698123456',
    listingTitle: 'Studio Centre',
    listingAddress: 'Eyang, IUSJC',
    bookingStartDate: '2026-04-01',
    bookingEndDate: '2026-07-01',
    rentAmount: 150000,
    commission: 15000,
    totalCharged: 165000,
    paymentMethod: 'MOBILE_MONEY',
    operator: 'MTN_MOMO',
    status: 'COMPLETED',
    paymentType: 'INTEGRAL',
  };

  beforeEach(async () => {
    paymentServiceMock = { getReceipt: vi.fn().mockReturnValue(of(mockReceipt)) };
    messageServiceMock = { add: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [PaymentReceipt],
      providers: [
        { provide: PaymentService, useValue: paymentServiceMock },
        { provide: MessageService, useValue: messageServiceMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '42' } } },
        },
      ],
    })
      .overrideComponent(PaymentReceipt, {
        set: {
          imports: [],
          schemas: [NO_ERRORS_SCHEMA],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(PaymentReceipt);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load receipt on init', () => {
    expect(paymentServiceMock.getReceipt).toHaveBeenCalledWith(42);
    expect((component as any).receipt()).toEqual(mockReceipt);
    expect((component as any).loading()).toBe(false);
  });

  it('should show error on load failure', () => {
    paymentServiceMock.getReceipt.mockReturnValue(
      throwError(() => new Error('Not found')),
    );

    const newFixture = TestBed.createComponent(PaymentReceipt);
    newFixture.detectChanges();

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
  });

  it('should format operator label correctly for MTN_MOMO', () => {
    expect((component as any).operatorLabel()).toBe('MTN Mobile Money');
  });

  it('should format operator label correctly for ORANGE_MONEY', () => {
    const orangeReceipt: ReceiptResponse = { ...mockReceipt, operator: 'ORANGE_MONEY' };
    paymentServiceMock.getReceipt.mockReturnValue(of(orangeReceipt));

    const newFixture = TestBed.createComponent(PaymentReceipt);
    newFixture.detectChanges();
    const newComponent = newFixture.componentInstance;

    expect((newComponent as any).operatorLabel()).toBe('Orange Money');
  });

  it('should format payment type label correctly for INTEGRAL', () => {
    expect((component as any).paymentTypeLabel()).toBe('Paiement intégral');
  });

  it('should format payment type label correctly for INSTALLMENT', () => {
    const installmentReceipt: ReceiptResponse = { ...mockReceipt, paymentType: 'INSTALLMENT' };
    paymentServiceMock.getReceipt.mockReturnValue(of(installmentReceipt));

    const newFixture = TestBed.createComponent(PaymentReceipt);
    newFixture.detectChanges();
    const newComponent = newFixture.componentInstance;

    expect((newComponent as any).paymentTypeLabel()).toBe('Paiement échelonné');
  });
});
