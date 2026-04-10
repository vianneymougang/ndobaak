import {
  Component,
  ChangeDetectionStrategy,
  inject,
  computed,
  OnInit,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { NgmMotionDirective } from '@scripttype/ng-motion';
import { PaymentStatus } from '../../../core/models/payment.models';

@Component({
  selector: 'app-payment-failure',
  imports: [RouterLink, ButtonModule, NgmMotionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payment-failure.html',
  styleUrl: './payment-failure.scss',
})
export class PaymentFailure implements OnInit {
  private route = inject(ActivatedRoute);

  protected reason = 'FAILED' as PaymentStatus;
  protected bookingId = '';

  protected readonly failureContent: Record<string, { title: string; desc: string; tips: string[] }> = {
    FAILED: {
      title: 'Paiement échoué',
      desc: 'Le paiement n\'a pas pu être traité. Cela peut être dû à un solde insuffisant, un code PIN incorrect ou un problème temporaire de l\'opérateur.',
      tips: [
        'Vérifiez votre solde Mobile Money',
        'Assurez-vous d\'avoir entré le bon code PIN',
        'Attendez quelques minutes si le réseau est instable',
      ],
    },
    CANCELED: {
      title: 'Paiement annulé',
      desc: 'Le paiement a été annulé. Vous pouvez relancer un paiement à tout moment depuis vos réservations.',
      tips: [
        'Aucune somme n\'a été débitée',
        'Votre réservation reste active',
        'Vous pouvez réessayer quand vous le souhaitez',
      ],
    },
    EXPIRED: {
      title: 'Délai de paiement expiré',
      desc: 'La session de paiement a expiré avant confirmation. Aucun montant n\'a été prélevé.',
      tips: [
        'Confirmez plus rapidement sur votre téléphone lors de la prochaine tentative',
        'Vérifiez que vos notifications Mobile Money sont activées',
      ],
    },
  };

  protected content = computed(() => {
    return this.failureContent[this.reason] ?? this.failureContent['FAILED'];
  });

  protected retryLink = computed(() =>
    this.bookingId ? `/bookings/${this.bookingId}/pay` : '/my-bookings'
  );

  ngOnInit(): void {
    this.reason = (this.route.snapshot.queryParamMap.get('reason') ?? 'FAILED') as PaymentStatus;
    this.bookingId = this.route.snapshot.queryParamMap.get('bookingId') ?? '';
  }
}
