export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELED' | 'EXPIRED' | 'REFUNDED';

export type PaymentMethod = 'MOBILE_MONEY' | 'BANK_TRANSFER';

export type PaymentType = 'INTEGRAL' | 'INSTALLMENT';

export type MobileMoneyOperator = 'MTN_MOMO' | 'ORANGE_MONEY';

export type ScheduleFrequency = 'MONTHLY' | 'QUARTERLY' | 'BLOCK';

export type InstallmentStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface InitiatePaymentRequest {
  bookingId: number;
  operator: MobileMoneyOperator;
  phoneNumber: string;
}

export interface PayInstallmentRequest {
  operator: MobileMoneyOperator;
  phoneNumber: string;
}

export interface PaymentResponse {
  id: number;
  bookingId: number;
  amount: number;
  commission: number;
  totalCharged: number;
  status: PaymentStatus;
  paymentType: PaymentType;
  paymentMethod: PaymentMethod;
  operator: MobileMoneyOperator;
  transactionId: string;
  authorizationUrl?: string;
  providerName: string;
  paidAt?: string;
  createdAt: string;
}

export interface ReceiptResponse {
  transactionId: string;
  paymentDate: string;
  payerName: string;
  payerPhone: string;
  listingTitle: string;
  listingAddress: string;
  bookingStartDate: string;
  bookingEndDate: string;
  rentAmount: number;
  commission: number;
  totalCharged: number;
  paymentMethod: PaymentMethod;
  operator: MobileMoneyOperator;
  status: PaymentStatus;
  paymentType: PaymentType;
}

export interface InstallmentResponse {
  id: number;
  installmentNumber: number;
  amount: number;
  commission: number;
  dueDate: string;
  status: InstallmentStatus;
  paidAt?: string;
}

export interface PaymentScheduleResponse {
  id: number;
  bookingId: number;
  totalAmount: number;
  frequency: ScheduleFrequency;
  numberOfInstallments: number;
  paidInstallments: number;
  paidAmount: number;
  remainingAmount: number;
  installments: InstallmentResponse[];
  createdAt: string;
}
