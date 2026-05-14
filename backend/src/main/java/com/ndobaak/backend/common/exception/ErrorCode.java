package com.ndobaak.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(400, "Les informations envoyées sont invalides.", false),
    RESOURCE_NOT_FOUND(404, "La ressource demandée est introuvable.", false),
    DUPLICATE_RESOURCE(409, "Cette ressource existe déjà.", false),
    BUSINESS_RULE_VIOLATION(400, "Cette action n'est pas possible.", false),
    UNAUTHORIZED(401, "Authentification requise.", false),
    INVALID_CREDENTIALS(401, "Identifiants invalides.", false),
    FORBIDDEN(403, "Vous n'êtes pas autorisé à effectuer cette action.", false),
    UPLOAD_TOO_LARGE(400, "Le fichier dépasse la taille maximale autorisée (10 Mo).", false),
    BAD_REQUEST(400, "Paramètre manquant ou invalide.", false),
    RATE_LIMIT_EXCEEDED(429, "Trop de tentatives. Veuillez réessayer plus tard.", true),
    INTERNAL_ERROR(500, "Une erreur inattendue est survenue. Veuillez réessayer plus tard.", true),

    SUBSCRIPTION_REQUIRED(403, "Vous devez souscrire à un abonnement pour effectuer cette action.", false),
    LISTING_LIMIT_REACHED(409, "Vous avez atteint la limite d'annonces autorisées par votre abonnement.", false),
    PHOTO_LIMIT_REACHED(409, "Cette annonce a déjà atteint le nombre maximum de photos autorisées.", false),
    INVALID_FILE_FORMAT(400, "Format de fichier non accepté.", false),
    LISTING_INCOMPLETE(422, "L'annonce est incomplète et ne peut pas être soumise.", false),
    LISTING_TOO_FAR_FROM_CAMPUS(422, "L'annonce doit se trouver à moins de 10 km du campus.", false),
    LISTING_INVALID_STATUS(422, "L'annonce n'est pas dans l'état requis pour cette action.", false),
    BOOKING_UNAVAILABLE(422, "Cette annonce n'est pas disponible à la réservation.", false),
    BOOKING_DATE_INVALID(400, "Les dates de réservation sont invalides.", false),
    BOOKING_CONFLICT(409, "Vous avez déjà une réservation active pour cette annonce.", false),
    BOOKING_INVALID_STATUS(422, "Cette réservation ne peut pas être modifiée dans son état actuel.", false),
    PAYMENT_IN_PROGRESS(409, "Un paiement est déjà en cours. Veuillez attendre sa finalisation.", false),
    PAYMENT_ALREADY_COMPLETED(409, "Cette réservation a déjà été intégralement payée.", false),
    SUBSCRIPTION_ALREADY_ACTIVE(409, "Vous avez déjà un abonnement actif.", false),
    SUBSCRIPTION_PAYMENT_IN_PROGRESS(409, "Un paiement d'abonnement est déjà en cours.", false),
    RESET_TOKEN_EXPIRED(400, "Ce lien de réinitialisation a expiré. Veuillez en demander un nouveau.", false),
    RESET_TOKEN_USED(400, "Ce lien de réinitialisation a déjà été utilisé.", false);

    private final int httpStatus;
    private final String message;
    private final boolean retryable;

    ErrorCode(int httpStatus, String message, boolean retryable) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.retryable = retryable;
    }

    public HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(httpStatus);
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
