import type { MobileMoneyOperator } from './payment.models';

export type SubscriptionStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'PENDING_PAYMENT';

export interface Subscription {
  id: number;
  status: SubscriptionStatus;
  startDate: string;
  endDate: string;
  maxListings: number;
  amount: number;
  autoRenew: boolean;
  daysRemaining: number;
  paymentReference?: string;
}

export interface SubscribeRequest {
  operator: MobileMoneyOperator;
  phoneNumber: string;
}

export type RenewRequest = SubscribeRequest;
