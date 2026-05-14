package com.ndobaak.backend.common.exception;

public enum ErrorCode {
    VALIDATION_ERROR("Les informations envoyées sont invalides.", false),
    RESOURCE_NOT_FOUND("La ressource demandée est introuvable.", false),
    DUPLICATE_RESOURCE("Cette ressource existe déjà.", false),
    BUSINESS_RULE_VIOLATION("Cette action n'est pas possible.", false),
    UNAUTHORIZED("Authentification requise.", false),
    INVALID_CREDENTIALS("Identifiants invalides.", false),
    FORBIDDEN("Vous n'êtes pas autorisé à effectuer cette action.", false),
    UPLOAD_TOO_LARGE("Le fichier dépasse la taille maximale autorisée (10 Mo).", false),
    BAD_REQUEST("Paramètre manquant ou invalide.", false),
    RATE_LIMIT_EXCEEDED("Trop de tentatives. Veuillez réessayer plus tard.", true),
    INTERNAL_ERROR("Une erreur inattendue est survenue. Veuillez réessayer plus tard.", true);

    private final String message;
    private final boolean retryable;

    ErrorCode(String message, boolean retryable) {
        this.message = message;
        this.retryable = retryable;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
