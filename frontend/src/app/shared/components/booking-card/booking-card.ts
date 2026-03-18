import {
  Component,
  ChangeDetectionStrategy,
  computed,
  input,
  output,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { BookingResponse, BookingStatus } from '../../../core/models/booking.models';

@Component({
  selector: 'app-booking-card',
  imports: [RouterLink, TagModule, ButtonModule, AvatarModule, NgmMotionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './booking-card.html',
  styleUrl: './booking-card.scss',
})
export class BookingCard {
  booking = input.required<BookingResponse>();
  viewMode = input.required<'student' | 'landlord'>();

  cancel = output<number>();
  confirm = output<number>();
  reject = output<number>();

  protected studentInitials = computed(() => {
    const s = this.booking().student;
    return (s.firstName?.charAt(0) ?? '') + (s.lastName?.charAt(0) ?? '');
  });

  protected formattedStartDate = computed(() =>
    new Intl.DateTimeFormat('fr-FR').format(new Date(this.booking().startDate)),
  );

  protected formattedEndDate = computed(() =>
    new Intl.DateTimeFormat('fr-FR').format(new Date(this.booking().endDate)),
  );

  protected formattedMonthlyRent = computed(() =>
    new Intl.NumberFormat('fr-FR').format(this.booking().monthlyRent),
  );

  protected formattedTotal = computed(() =>
    new Intl.NumberFormat('fr-FR').format(this.booking().totalAmount),
  );

  protected statusSeverity = computed(() => {
    const map: Record<BookingStatus, 'warn' | 'success' | 'danger' | 'info' | 'secondary'> = {
      PENDING: 'warn',
      CONFIRMED: 'success',
      CANCELLED: 'danger',
      COMPLETED: 'info',
      EXPIRED: 'secondary',
    };
    return map[this.booking().status];
  });

  protected statusLabel = computed(() => {
    const map: Record<BookingStatus, string> = {
      PENDING: 'En attente',
      CONFIRMED: 'Confirmée',
      CANCELLED: 'Annulée',
      COMPLETED: 'Terminée',
      EXPIRED: 'Expirée',
    };
    return map[this.booking().status];
  });

  protected canCancel = computed(() => {
    const s = this.booking().status;
    return s === 'PENDING' || s === 'CONFIRMED';
  });

  protected canConfirmOrReject = computed(
    () => this.viewMode() === 'landlord' && this.booking().status === 'PENDING',
  );

  protected canPay = computed(
    () => this.viewMode() === 'student' && this.booking().status === 'CONFIRMED',
  );
}
