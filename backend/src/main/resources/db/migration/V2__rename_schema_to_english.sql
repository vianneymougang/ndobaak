ALTER TYPE statut_utilisateur RENAME TO user_status;
ALTER TYPE user_status RENAME VALUE 'ACTIF' TO 'ACTIVE';
ALTER TYPE user_status RENAME VALUE 'INACTIF' TO 'INACTIVE';
ALTER TYPE user_status RENAME VALUE 'SUSPENDU' TO 'SUSPENDED';

ALTER TYPE type_chambre RENAME TO room_type;
ALTER TYPE room_type RENAME VALUE 'CHAMBRE_SIMPLE' TO 'SINGLE_ROOM';
ALTER TYPE room_type RENAME VALUE 'CHAMBRE_DOUBLE' TO 'DOUBLE_ROOM';
ALTER TYPE room_type RENAME VALUE 'APPARTEMENT' TO 'APARTMENT';

ALTER TYPE statut_annonce RENAME TO listing_status;
ALTER TYPE listing_status RENAME VALUE 'BROUILLON' TO 'DRAFT';
ALTER TYPE listing_status RENAME VALUE 'EN_ATTENTE' TO 'PENDING_REVIEW';
ALTER TYPE listing_status RENAME VALUE 'PUBLIEE' TO 'PUBLISHED';
ALTER TYPE listing_status RENAME VALUE 'REJETEE' TO 'REJECTED';
ALTER TYPE listing_status RENAME VALUE 'ARCHIVEE' TO 'ARCHIVED';

ALTER TYPE statut_reservation RENAME TO booking_status;
ALTER TYPE booking_status RENAME VALUE 'EN_ATTENTE' TO 'PENDING';
ALTER TYPE booking_status RENAME VALUE 'CONFIRMEE' TO 'CONFIRMED';
ALTER TYPE booking_status RENAME VALUE 'REFUSEE' TO 'REFUSED';
ALTER TYPE booking_status RENAME VALUE 'ANNULEE' TO 'CANCELLED';
ALTER TYPE booking_status RENAME VALUE 'TERMINEE' TO 'COMPLETED';

ALTER TYPE statut_paiement RENAME TO payment_status;
ALTER TYPE payment_status RENAME VALUE 'EN_ATTENTE' TO 'PENDING';
ALTER TYPE payment_status RENAME VALUE 'REUSSI' TO 'SUCCESS';
ALTER TYPE payment_status RENAME VALUE 'ECHOUE' TO 'FAILED';
ALTER TYPE payment_status RENAME VALUE 'REMBOURSE' TO 'REFUNDED';

ALTER TYPE operateur_mobile_money RENAME TO mobile_money_operator;

ALTER TYPE frequence_echeancier RENAME TO schedule_frequency;
ALTER TYPE schedule_frequency RENAME VALUE 'MENSUEL' TO 'MONTHLY';
ALTER TYPE schedule_frequency RENAME VALUE 'TRIMESTRIEL' TO 'QUARTERLY';

ALTER TYPE statut_echeance RENAME TO installment_status;
ALTER TYPE installment_status RENAME VALUE 'A_VENIR' TO 'UPCOMING';
ALTER TYPE installment_status RENAME VALUE 'PAYEE' TO 'PAID';
ALTER TYPE installment_status RENAME VALUE 'EN_RETARD' TO 'OVERDUE';

ALTER TYPE statut_avis RENAME TO review_status;
ALTER TYPE review_status RENAME VALUE 'PUBLIE' TO 'PUBLISHED';
ALTER TYPE review_status RENAME VALUE 'MASQUE' TO 'HIDDEN';
ALTER TYPE review_status RENAME VALUE 'SIGNALE' TO 'REPORTED';

ALTER TYPE statut_abonnement RENAME TO subscription_status;
ALTER TYPE subscription_status RENAME VALUE 'ACTIF' TO 'ACTIVE';
ALTER TYPE subscription_status RENAME VALUE 'EXPIRE' TO 'EXPIRED';
ALTER TYPE subscription_status RENAME VALUE 'ANNULE' TO 'CANCELLED';

ALTER TYPE type_notification RENAME TO notification_type;
ALTER TYPE notification_type RENAME VALUE 'RESERVATION' TO 'BOOKING';
ALTER TYPE notification_type RENAME VALUE 'PAIEMENT' TO 'PAYMENT';
ALTER TYPE notification_type RENAME VALUE 'ANNONCE' TO 'LISTING';
ALTER TYPE notification_type RENAME VALUE 'AVIS' TO 'REVIEW';
ALTER TYPE notification_type RENAME VALUE 'SYSTEME' TO 'SYSTEM';

ALTER TYPE canal_notification RENAME TO notification_channel;

ALTER TABLE utilisateur RENAME TO users;
ALTER TABLE users RENAME COLUMN type_utilisateur TO user_type;
ALTER TABLE users RENAME COLUMN nom TO last_name;
ALTER TABLE users RENAME COLUMN prenom TO first_name;
ALTER TABLE users RENAME COLUMN telephone TO phone;
ALTER TABLE users RENAME COLUMN mot_de_passe TO password;
ALTER TABLE users RENAME COLUMN date_inscription TO registration_date;
ALTER TABLE users RENAME COLUMN statut TO status;
ALTER TABLE users RENAME COLUMN photo_profil_url TO profile_photo_url;
ALTER TABLE users RENAME COLUMN filiere TO field_of_study;
ALTER TABLE users RENAME COLUMN annee_etude TO year_of_study;
ALTER TABLE users RENAME COLUMN nom_cite TO tax_id;
ALTER TABLE users RENAME COLUMN numero_cni TO id_card_number;

ALTER INDEX idx_utilisateur_email RENAME TO idx_users_email;
ALTER INDEX idx_utilisateur_type RENAME TO idx_users_type;

ALTER TABLE localisation RENAME TO location;
ALTER TABLE location RENAME COLUMN adresse TO address;
ALTER TABLE location RENAME COLUMN distance_campus TO distance_to_campus;
ALTER TABLE location ADD COLUMN neighborhood VARCHAR(200);
ALTER TABLE location ADD COLUMN city VARCHAR(100) DEFAULT 'Yaoundé';

ALTER TABLE equipement RENAME TO amenity;
ALTER TABLE amenity RENAME COLUMN nom TO name;
ALTER TABLE amenity RENAME COLUMN icone TO icon;

ALTER TABLE annonce RENAME TO listing;
ALTER TABLE listing RENAME COLUMN titre TO title;
ALTER TABLE listing RENAME COLUMN type_chambre TO room_type;
ALTER TABLE listing RENAME COLUMN prix_mensuel TO monthly_price;
ALTER TABLE listing RENAME COLUMN nombres_chambres_disponibles TO available_rooms;
ALTER TABLE listing RENAME COLUMN date_disponibilite TO available_date;
ALTER TABLE listing RENAME COLUMN statut TO status;
ALTER TABLE listing RENAME COLUMN motif_rejet TO rejection_reason;
ALTER TABLE listing RENAME COLUMN date_creation TO created_at;
ALTER TABLE listing RENAME COLUMN date_modification TO updated_at;
ALTER TABLE listing RENAME COLUMN proprietaire_id TO landlord_id;
ALTER TABLE listing RENAME COLUMN localisation_id TO location_id;

ALTER INDEX idx_annonce_statut RENAME TO idx_listing_status;
ALTER INDEX idx_annonce_proprietaire RENAME TO idx_listing_landlord;
ALTER INDEX idx_annonce_prix RENAME TO idx_listing_price;

ALTER TABLE annonce_equipement RENAME TO listing_amenity;
ALTER TABLE listing_amenity RENAME COLUMN annonce_id TO listing_id;
ALTER TABLE listing_amenity RENAME COLUMN equipement_id TO amenity_id;

ALTER TABLE photo RENAME COLUMN url_miniature TO thumbnail_url;
ALTER TABLE photo ADD COLUMN medium_url VARCHAR(500);
ALTER TABLE photo RENAME COLUMN taille_fichier TO file_size;
ALTER TABLE photo RENAME COLUMN date_ajout TO created_at;
ALTER TABLE photo RENAME COLUMN annonce_id TO listing_id;
ALTER TABLE photo ADD COLUMN display_order INTEGER DEFAULT 0;

ALTER TABLE reservation RENAME TO booking;
ALTER TABLE booking RENAME COLUMN date_reservation TO created_at;
ALTER TABLE booking RENAME COLUMN date_debut TO start_date;
ALTER TABLE booking RENAME COLUMN date_fin TO end_date;
ALTER TABLE booking RENAME COLUMN duree_contrat TO contract_duration;
ALTER TABLE booking RENAME COLUMN montant_total TO total_amount;
ALTER TABLE booking RENAME COLUMN statut TO status;
ALTER TABLE booking RENAME COLUMN motif_refus TO refusal_reason;
ALTER TABLE booking RENAME COLUMN date_reponse_proprietaire TO landlord_response_date;
ALTER TABLE booking RENAME COLUMN etudiant_id TO student_id;
ALTER TABLE booking RENAME COLUMN annonce_id TO listing_id;

ALTER INDEX idx_reservation_etudiant RENAME TO idx_booking_student;
ALTER INDEX idx_reservation_statut RENAME TO idx_booking_status;

ALTER TABLE paiement RENAME TO payment;
ALTER TABLE payment RENAME COLUMN montant TO amount;
ALTER TABLE payment RENAME COLUMN date_paiement TO payment_date;
ALTER TABLE payment RENAME COLUMN operateur TO operator;
ALTER TABLE payment RENAME COLUMN numero_mobile_money TO mobile_money_number;
ALTER TABLE payment RENAME COLUMN reference_transaction TO transaction_reference;
ALTER TABLE payment RENAME COLUMN reference_externe TO external_reference;
ALTER TABLE payment RENAME COLUMN statut TO status;
ALTER TABLE payment RENAME COLUMN reservation_id TO booking_id;

ALTER TABLE echeancier RENAME TO payment_schedule;
ALTER TABLE payment_schedule RENAME COLUMN frequence TO frequency;
ALTER TABLE payment_schedule RENAME COLUMN nombre_echeances TO installment_count;
ALTER TABLE payment_schedule RENAME COLUMN montant_par_echeance TO amount_per_installment;
ALTER TABLE payment_schedule RENAME COLUMN paiement_id TO payment_id;

ALTER TABLE echeance RENAME TO installment;
ALTER TABLE installment RENAME COLUMN montant TO amount;
ALTER TABLE installment RENAME COLUMN date_echeance TO due_date;
ALTER TABLE installment RENAME COLUMN date_paiement_effectif TO actual_payment_date;
ALTER TABLE installment RENAME COLUMN statut TO status;
ALTER TABLE installment RENAME COLUMN echeancier_id TO payment_schedule_id;

ALTER TABLE avis RENAME TO review;
ALTER TABLE review RENAME COLUMN note TO rating;
ALTER TABLE review RENAME COLUMN commentaire TO comment;
ALTER TABLE review RENAME COLUMN date_avis TO created_at;
ALTER TABLE review RENAME COLUMN statut TO status;
ALTER TABLE review RENAME COLUMN etudiant_id TO student_id;
ALTER TABLE review RENAME COLUMN annonce_id TO listing_id;

ALTER TABLE signalement RENAME TO report;
ALTER TABLE report RENAME COLUMN motif TO reason;
ALTER TABLE report RENAME COLUMN date_signalement TO created_at;
ALTER TABLE report RENAME COLUMN traite TO processed;
ALTER TABLE report RENAME COLUMN avis_id TO review_id;
ALTER TABLE report RENAME COLUMN signaleur_id TO reporter_id;

ALTER TABLE abonnement RENAME TO subscription;
ALTER TABLE subscription RENAME COLUMN date_debut TO start_date;
ALTER TABLE subscription RENAME COLUMN date_fin TO end_date;
ALTER TABLE subscription RENAME COLUMN montant TO amount;
ALTER TABLE subscription RENAME COLUMN statut TO status;
ALTER TABLE subscription RENAME COLUMN proprietaire_id TO landlord_id;

ALTER TABLE notification RENAME COLUMN titre TO title;
ALTER TABLE notification RENAME COLUMN lue TO read;
ALTER TABLE notification RENAME COLUMN date_envoi TO created_at;
ALTER TABLE notification RENAME COLUMN destinataire_id TO recipient_id;

ALTER INDEX idx_notification_destinataire RENAME TO idx_notification_recipient;
ALTER INDEX idx_notification_lue RENAME TO idx_notification_read;

ALTER TABLE preference_notification RENAME TO notification_preference;
ALTER TABLE notification_preference RENAME COLUMN canal TO channel;
ALTER TABLE notification_preference RENAME COLUMN utilisateur_id TO user_id;

UPDATE amenity SET name = 'WiFi', icon = 'pi-wifi' WHERE name = 'WiFi';
UPDATE amenity SET name = 'Running Water', icon = 'pi-droplet' WHERE name = 'Eau courante';
UPDATE amenity SET name = 'Electricity', icon = 'pi-bolt' WHERE name = 'Électricité';
UPDATE amenity SET name = 'Private Shower', icon = 'pi-shower' WHERE name = 'Douche interne';
UPDATE amenity SET name = 'Private Toilet', icon = 'pi-toilet' WHERE name = 'Toilettes internes';
UPDATE amenity SET name = 'Shared Kitchen', icon = 'pi-utensils' WHERE name = 'Cuisine partagée';
UPDATE amenity SET name = 'Parking', icon = 'pi-car' WHERE name = 'Parking';
UPDATE amenity SET name = 'Security Guard', icon = 'pi-shield' WHERE name = 'Gardien';
UPDATE amenity SET name = 'Furnished', icon = 'pi-home' WHERE name = 'Meublé';
UPDATE amenity SET name = 'Fan', icon = 'pi-sun' WHERE name = 'Ventilateur';
UPDATE amenity SET name = 'Air Conditioning', icon = 'pi-snowflake' WHERE name = 'Climatisation';
