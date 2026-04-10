export type NotificationType =
  | 'BOOKING_CREATED'
  | 'BOOKING_CONFIRMED'
  | 'BOOKING_CANCELLED'
  | 'PAYMENT_RECEIVED'
  | 'PAYMENT_FAILED'
  | 'PAYMENT_REMINDER'
  | 'REVIEW_POSTED'
  | 'REVIEW_FLAGGED'
  | 'LISTING_APPROVED'
  | 'LISTING_REJECTED'
  | 'SUBSCRIPTION_ACTIVATED'
  | 'SUBSCRIPTION_EXPIRING_SOON'
  | 'SUBSCRIPTION_EXPIRED'
  | 'ACCOUNT_DEACTIVATED'
  | 'USER_VERIFIED'
  | 'USER_REJECTED'
  | 'LANDLORD_DOCUMENT_APPROVED'
  | 'LANDLORD_DOCUMENT_REJECTED'
  | 'APPLICATION_APPROVED'
  | 'APPLICATION_REJECTED'
  | 'LEASE_READY'
  | 'LEASE_SIGNED'
  | 'KEYS_HANDED_OVER'
  | 'TRANSFER_FAILED';

export interface NotificationResponse {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  referenceId?: number;
  referenceType?: string;
  createdAt: string;
}

export interface NotificationTypePreferenceResponse {
  notificationType: NotificationType;
  emailEnabled: boolean;
  smsEnabled: boolean;
  inAppEnabled: boolean;
  critical: boolean;
}

export interface UpdatePreferenceRequest {
  emailEnabled?: boolean;
  smsEnabled?: boolean;
}

export interface PreferenceGroup {
  label: string;
  icon: string;
  types: NotificationType[];
  preferences: NotificationTypePreferenceResponse[];
}

export const NOTIFICATION_LABELS: Record<NotificationType, string> = {
  BOOKING_CREATED: 'Nouvelle demande de réservation',
  BOOKING_CONFIRMED: 'Réservation confirmée',
  BOOKING_CANCELLED: 'Réservation annulée',
  PAYMENT_RECEIVED: 'Paiement reçu',
  PAYMENT_FAILED: 'Échec de paiement',
  PAYMENT_REMINDER: 'Rappel de paiement',
  REVIEW_POSTED: 'Nouvel avis',
  REVIEW_FLAGGED: 'Avis signalé',
  LISTING_APPROVED: 'Annonce approuvée',
  LISTING_REJECTED: 'Annonce refusée',
  SUBSCRIPTION_ACTIVATED: 'Abonnement activé',
  SUBSCRIPTION_EXPIRING_SOON: 'Abonnement bientôt expiré',
  SUBSCRIPTION_EXPIRED: 'Abonnement expiré',
  ACCOUNT_DEACTIVATED: 'Compte désactivé',
  USER_VERIFIED: 'Compte vérifié',
  USER_REJECTED: 'Compte refusé',
  LANDLORD_DOCUMENT_APPROVED: 'Document propriétaire approuvé',
  LANDLORD_DOCUMENT_REJECTED: 'Document propriétaire refusé',
  APPLICATION_APPROVED: 'Dossier locataire approuvé',
  APPLICATION_REJECTED: 'Dossier locataire refusé',
  LEASE_READY: 'Contrat de bail disponible',
  LEASE_SIGNED: 'Contrat de bail signé',
  KEYS_HANDED_OVER: 'Remise des clés confirmée',
  TRANSFER_FAILED: 'Échec du virement propriétaire',
};

export const PREFERENCE_GROUPS: PreferenceGroup[] = [
  {
    label: 'Réservations',
    icon: 'pi-calendar',
    types: ['BOOKING_CREATED', 'BOOKING_CONFIRMED', 'BOOKING_CANCELLED'],
    preferences: [],
  },
  {
    label: 'Paiements',
    icon: 'pi-wallet',
    types: ['PAYMENT_RECEIVED', 'PAYMENT_FAILED', 'PAYMENT_REMINDER'],
    preferences: [],
  },
  {
    label: 'Annonces',
    icon: 'pi-home',
    types: ['LISTING_APPROVED', 'LISTING_REJECTED', 'REVIEW_POSTED', 'REVIEW_FLAGGED'],
    preferences: [],
  },
  {
    label: 'Cycle locatif',
    icon: 'pi-file-edit',
    types: [
      'LANDLORD_DOCUMENT_APPROVED',
      'LANDLORD_DOCUMENT_REJECTED',
      'APPLICATION_APPROVED',
      'APPLICATION_REJECTED',
      'LEASE_READY',
      'LEASE_SIGNED',
      'KEYS_HANDED_OVER',
      'TRANSFER_FAILED',
    ],
    preferences: [],
  },
  {
    label: 'Compte',
    icon: 'pi-user',
    types: ['USER_VERIFIED', 'USER_REJECTED', 'ACCOUNT_DEACTIVATED'],
    preferences: [],
  },
  {
    label: 'Abonnement',
    icon: 'pi-star',
    types: ['SUBSCRIPTION_ACTIVATED', 'SUBSCRIPTION_EXPIRING_SOON', 'SUBSCRIPTION_EXPIRED'],
    preferences: [],
  },
];
