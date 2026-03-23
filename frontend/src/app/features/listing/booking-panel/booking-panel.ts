import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  input,
  output,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { ListingResponse } from '../../../core/models/listing.models';
import { BookingResponse } from '../../../core/models/booking.models';
import { BookingConfirmDialog } from '../booking-confirm-dialog/booking-confirm-dialog';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-booking-panel',
  imports: [
    FormsModule,
    DatePickerModule,
    SelectModule,
    ButtonModule,
    SkeletonModule,
    BookingConfirmDialog,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './booking-panel.html',
  styleUrl: './booking-panel.scss',
})
export class BookingPanel {
  private authService = inject(AuthService);
  private router = inject(Router);

  listing = input.required<ListingResponse>();
  loading = input<boolean>(false);
  bookingCreated = output<BookingResponse>();

  protected startDate = signal<Date | null>(null);
  protected duration = signal<number>(10);
  protected confirmDialogVisible = signal(false);
  readonly minDate = new Date();

  protected endDate = computed(() => {
    const start = this.startDate();
    const dur = this.duration();
    if (!start) return null;
    const end = new Date(start);
    end.setMonth(end.getMonth() + dur);
    return end;
  });

  protected totalAmount = computed(() => this.listing().monthlyPrice * this.duration());

  protected formattedPrice = computed(() =>
    new Intl.NumberFormat('fr-FR').format(this.listing().monthlyPrice),
  );

  protected formattedTotal = computed(() =>
    new Intl.NumberFormat('fr-FR').format(this.totalAmount()),
  );

  protected isOwner = computed(() => {
    const userId = this.authService.user()?.id;
    return userId != null && userId === this.listing().landlord?.id;
  });
  protected isAuthenticated = this.authService.isAuthenticated;

  readonly durationOptions = [
    { label: '1 mois', value: 1 },
    { label: '3 mois', value: 3 },
    { label: '6 mois', value: 6 },
    { label: '9 mois', value: 9 },
    { label: '10 mois (année universitaire)', value: 10 },
    { label: '12 mois', value: 12 },
  ];

  protected onReserveClick(): void {
    if (!this.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }
    this.confirmDialogVisible.set(true);
  }

  protected onBookingConfirmed(booking: BookingResponse): void {
    this.bookingCreated.emit(booking);
  }
}
