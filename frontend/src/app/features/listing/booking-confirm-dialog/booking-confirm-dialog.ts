import {
  Component,
  ChangeDetectionStrategy,
  DestroyRef,
  inject,
  signal,
  computed,
  input,
  output,
  model,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { MessageService } from 'primeng/api';
import { ListingResponse } from '../../../core/models/listing.models';
import { BookingResponse } from '../../../core/models/booking.models';
import { BookingService } from '../../../core/services/booking';

@Component({
  selector: 'app-booking-confirm-dialog',
  imports: [FormsModule, DialogModule, ButtonModule, CheckboxModule, NgmMotionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './booking-confirm-dialog.html',
  styleUrl: './booking-confirm-dialog.scss',
})
export class BookingConfirmDialog {
  private bookingService = inject(BookingService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  visible = model<boolean>(false);
  listing = input.required<ListingResponse>();
  startDate = input.required<Date>();
  duration = input.required<number>();
  totalAmount = input.required<number>();

  confirmed = output<BookingResponse>();

  protected termsAccepted = signal(false);
  protected submitting = signal(false);

  protected endDate = computed(() => {
    const start = this.startDate();
    const end = new Date(start);
    end.setMonth(end.getMonth() + this.duration());
    return end;
  });

  protected formattedStartDate = computed(() =>
    new Intl.DateTimeFormat('fr-FR').format(this.startDate()),
  );

  protected formattedEndDate = computed(() =>
    new Intl.DateTimeFormat('fr-FR').format(this.endDate()),
  );

  protected formattedMonthlyRent = computed(() =>
    new Intl.NumberFormat('fr-FR').format(this.listing().monthlyPrice),
  );

  protected formattedTotal = computed(() =>
    new Intl.NumberFormat('fr-FR').format(this.totalAmount()),
  );

  protected thumbnailUrl = computed(() => this.listing().photos[0]?.thumbnailUrl ?? null);

  private toIsoDate(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  protected submitBooking(): void {
    if (!this.termsAccepted() || this.submitting()) return;
    this.submitting.set(true);

    this.bookingService
      .createBooking({
        listingId: this.listing().id,
        startDate: this.toIsoDate(this.startDate()),
        endDate: this.toIsoDate(this.endDate()),
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (booking) => {
          this.submitting.set(false);
          this.visible.set(false);
          this.termsAccepted.set(false);
          this.messageService.add({
            severity: 'success',
            summary: 'Réservation envoyée',
            detail: 'Le propriétaire a été notifié de votre demande.',
          });
          this.confirmed.emit(booking);
        },
        error: (err) => {
          this.submitting.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Erreur',
            detail: err?.error?.message ?? 'Impossible d\'envoyer la réservation. Réessayez.',
          });
        },
      });
  }
}
