-- =============================================
-- V1 — Schéma initial Ndobaâk
-- Basé sur le diagramme de classes de conception
-- =============================================

-- Énumérations PostgreSQL
CREATE TYPE statut_utilisateur AS ENUM ('ACTIF', 'INACTIF', 'SUSPENDU');
CREATE TYPE type_chambre AS ENUM ('STUDIO', 'CHAMBRE_SIMPLE', 'CHAMBRE_DOUBLE', 'APPARTEMENT');
CREATE TYPE statut_annonce AS ENUM ('BROUILLON', 'EN_ATTENTE', 'PUBLIEE', 'REJETEE', 'ARCHIVEE');
CREATE TYPE statut_reservation AS ENUM ('EN_ATTENTE', 'CONFIRMEE', 'REFUSEE', 'ANNULEE', 'TERMINEE');
CREATE TYPE statut_paiement AS ENUM ('EN_ATTENTE', 'REUSSI', 'ECHOUE', 'REMBOURSE');
CREATE TYPE operateur_mobile_money AS ENUM ('ORANGE_MONEY', 'MTN_MOMO');
CREATE TYPE frequence_echeancier AS ENUM ('MENSUEL', 'TRIMESTRIEL');
CREATE TYPE statut_echeance AS ENUM ('A_VENIR', 'PAYEE', 'EN_RETARD');
CREATE TYPE statut_avis AS ENUM ('PUBLIE', 'MASQUE', 'SIGNALE');
CREATE TYPE statut_abonnement AS ENUM ('ACTIF', 'EXPIRE', 'ANNULE');
CREATE TYPE type_notification AS ENUM ('RESERVATION', 'PAIEMENT', 'ANNONCE', 'AVIS', 'SYSTEME');
CREATE TYPE canal_notification AS ENUM ('EMAIL', 'SMS', 'PUSH', 'IN_APP');

-- Table Utilisateur (stratégie SINGLE_TABLE)
CREATE TABLE utilisateur (
    id BIGSERIAL PRIMARY KEY,
    type_utilisateur VARCHAR(20) NOT NULL,  -- Discriminateur JPA
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(20) UNIQUE,
    mot_de_passe VARCHAR(255),  -- Nullable car OAuth peut ne pas avoir de mdp
    date_inscription TIMESTAMP NOT NULL DEFAULT NOW(),
    statut statut_utilisateur NOT NULL DEFAULT 'ACTIF',
    -- Photo de profil (optionnel)
    photo_profil_url VARCHAR(500),

    -- Champs Etudiant
    matricule VARCHAR(50),
    filiere VARCHAR(100),
    annee_etude INTEGER,

    -- Champs Proprietaire
    nom_cite VARCHAR(200),
    numero_cni VARCHAR(50),

    -- Champs Administrateur
    role VARCHAR(50),

    -- OAuth
    oauth_provider VARCHAR(20),   -- 'google', null si inscription classique
    oauth_provider_id VARCHAR(255) -- ID Google unique
);

CREATE INDEX idx_utilisateur_email ON utilisateur(email);
CREATE INDEX idx_utilisateur_type ON utilisateur(type_utilisateur);

-- Table Localisation
CREATE TABLE localisation (
    id BIGSERIAL PRIMARY KEY,
    adresse VARCHAR(500) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    distance_campus DOUBLE PRECISION  -- Calculée via Haversine
);

-- Table Equipement
CREATE TABLE equipement (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    icone VARCHAR(50)
);

-- Table Annonce
CREATE TABLE annonce (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    type_chambre type_chambre NOT NULL,
    prix_mensuel DECIMAL(10, 0) NOT NULL,  -- FCFA, pas de centimes
    nombres_chambres_disponibles INTEGER NOT NULL DEFAULT 1,
    date_disponibilite DATE,
    statut statut_annonce NOT NULL DEFAULT 'BROUILLON',
    motif_rejet TEXT,
    date_creation TIMESTAMP NOT NULL DEFAULT NOW(),
    date_modification TIMESTAMP,

    -- Clés étrangères
    proprietaire_id BIGINT NOT NULL REFERENCES utilisateur(id),
    localisation_id BIGINT REFERENCES localisation(id)
);

CREATE INDEX idx_annonce_statut ON annonce(statut);
CREATE INDEX idx_annonce_proprietaire ON annonce(proprietaire_id);
CREATE INDEX idx_annonce_prix ON annonce(prix_mensuel);

-- Table de jointure Annonce-Equipement
CREATE TABLE annonce_equipement (
    annonce_id BIGINT NOT NULL REFERENCES annonce(id) ON DELETE CASCADE,
    equipement_id BIGINT NOT NULL REFERENCES equipement(id),
    PRIMARY KEY (annonce_id, equipement_id)
);

-- Table Photo
CREATE TABLE photo (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(500) NOT NULL,
    url_miniature VARCHAR(500),
    description VARCHAR(300),
    taille_fichier BIGINT,  -- En octets
    date_ajout TIMESTAMP NOT NULL DEFAULT NOW(),
    annonce_id BIGINT NOT NULL REFERENCES annonce(id) ON DELETE CASCADE
);

-- Table Reservation
CREATE TABLE reservation (
    id BIGSERIAL PRIMARY KEY,
    date_reservation TIMESTAMP NOT NULL DEFAULT NOW(),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    duree_contrat INTEGER NOT NULL,  -- En mois
    montant_total DECIMAL(12, 0) NOT NULL,
    statut statut_reservation NOT NULL DEFAULT 'EN_ATTENTE',
    motif_refus TEXT,
    date_reponse_proprietaire TIMESTAMP,

    -- Clés étrangères
    etudiant_id BIGINT NOT NULL REFERENCES utilisateur(id),
    annonce_id BIGINT NOT NULL REFERENCES annonce(id)
);

CREATE INDEX idx_reservation_etudiant ON reservation(etudiant_id);
CREATE INDEX idx_reservation_statut ON reservation(statut);

-- Table Paiement
CREATE TABLE paiement (
    id BIGSERIAL PRIMARY KEY,
    montant DECIMAL(12, 0) NOT NULL,
    date_paiement TIMESTAMP,
    operateur operateur_mobile_money NOT NULL,
    numero_mobile_money VARCHAR(20) NOT NULL,
    reference_transaction VARCHAR(100) UNIQUE,  -- Ref interne Ndobaâk
    reference_externe VARCHAR(100),              -- Ref NotchPay
    statut statut_paiement NOT NULL DEFAULT 'EN_ATTENTE',

    -- Clé étrangère
    reservation_id BIGINT NOT NULL REFERENCES reservation(id)
);

-- Table Echeancier
CREATE TABLE echeancier (
    id BIGSERIAL PRIMARY KEY,
    frequence frequence_echeancier NOT NULL DEFAULT 'MENSUEL',
    nombre_echeances INTEGER NOT NULL,
    montant_par_echeance DECIMAL(12, 0) NOT NULL,

    -- Clé étrangère
    paiement_id BIGINT NOT NULL REFERENCES paiement(id) UNIQUE
);

-- Table Echeance
CREATE TABLE echeance (
    id BIGSERIAL PRIMARY KEY,
    montant DECIMAL(12, 0) NOT NULL,
    date_echeance DATE NOT NULL,
    date_paiement_effectif TIMESTAMP,
    statut statut_echeance NOT NULL DEFAULT 'A_VENIR',

    -- Clé étrangère
    echeancier_id BIGINT NOT NULL REFERENCES echeancier(id) ON DELETE CASCADE
);

-- Table Avis
CREATE TABLE avis (
    id BIGSERIAL PRIMARY KEY,
    note INTEGER NOT NULL CHECK (note >= 1 AND note <= 5),
    commentaire TEXT,
    date_avis TIMESTAMP NOT NULL DEFAULT NOW(),
    statut statut_avis NOT NULL DEFAULT 'PUBLIE',

    -- Clés étrangères
    etudiant_id BIGINT NOT NULL REFERENCES utilisateur(id),
    annonce_id BIGINT NOT NULL REFERENCES annonce(id),

    -- Un étudiant ne peut laisser qu'un seul avis par annonce
    UNIQUE (etudiant_id, annonce_id)
);

-- Table Signalement
CREATE TABLE signalement (
    id BIGSERIAL PRIMARY KEY,
    motif TEXT NOT NULL,
    date_signalement TIMESTAMP NOT NULL DEFAULT NOW(),
    traite BOOLEAN NOT NULL DEFAULT FALSE,

    -- Clés étrangères
    avis_id BIGINT NOT NULL REFERENCES avis(id),
    signaleur_id BIGINT NOT NULL REFERENCES utilisateur(id)
);

-- Table Abonnement
CREATE TABLE abonnement (
    id BIGSERIAL PRIMARY KEY,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    montant DECIMAL(10, 0) NOT NULL,  -- 40 000 FCFA
    statut statut_abonnement NOT NULL DEFAULT 'ACTIF',

    -- Clé étrangère
    proprietaire_id BIGINT NOT NULL REFERENCES utilisateur(id)
);

-- Table Notification
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    type_notification type_notification NOT NULL,
    titre VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    lue BOOLEAN NOT NULL DEFAULT FALSE,
    date_envoi TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Clé étrangère
    destinataire_id BIGINT NOT NULL REFERENCES utilisateur(id)
);

CREATE INDEX idx_notification_destinataire ON notification(destinataire_id);
CREATE INDEX idx_notification_lue ON notification(destinataire_id, lue);

-- Table Préférence Notification
CREATE TABLE preference_notification (
    id BIGSERIAL PRIMARY KEY,
    canal canal_notification NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Clé étrangère
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateur(id),

    UNIQUE (utilisateur_id, canal)
);

-- Données initiales : équipements courants
INSERT INTO equipement (nom, icone) VALUES
    ('WiFi', 'wifi'),
    ('Eau courante', 'droplet'),
    ('Électricité', 'zap'),
    ('Douche interne', 'shower-head'),
    ('Toilettes internes', 'toilet'),
    ('Cuisine partagée', 'cooking-pot'),
    ('Parking', 'car'),
    ('Gardien', 'shield'),
    ('Meublé', 'sofa'),
    ('Ventilateur', 'fan'),
    ('Climatisation', 'thermometer-snowflake');
