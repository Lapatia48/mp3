# Partie 2 — API Backend (Spring Boot)

> API REST **Spring Boot** : reçoit les MP3 + métadonnées, les stocke en base et
> sur disque (stockage permanent), et fournit les données à l'application web.

## Stack
Java 17+, Spring Boot 3.x, Spring Web, Spring Data JPA, Spring Security (JWT),
PostgreSQL/MySQL, stockage fichiers sur disque (ou MinIO/S3 en option).

---

## 0. Mise en place

- [ ] Créer le projet Spring Boot `api` (Spring Initializr).
- [ ] Dépendances : Web, Data JPA, Validation, Security, driver BD (PostgreSQL/MySQL), Lombok.
- [ ] Configurer la connexion BD (`application.properties`).
- [ ] Configurer le répertoire de **stockage permanent** des MP3.
- [ ] Configurer les logs (SLF4J + Logback).
- [ ] (Option) `docker-compose` : BD + RabbitMQ + API.

---

## 1. Modèle de données

- [ ] Entité **User** (id, username, email, password hashé, rôle).
- [ ] Entité **Track / Mp3** (id, title, artist, album, genre, duration, year,
      date, fileName, storagePath, createdAt).
- [ ] Entité **Playlist** (id, name, user_id, createdAt).
- [ ] Entité **PlaylistTrack** (playlist_id, track_id, position) — relation N-N ordonnée.
- [ ] Repositories Spring Data JPA pour chaque entité.
- [ ] (Option) Migrations Flyway/Liquibase.

---

## 2. Stockage des MP3

- [ ] Service de stockage : enregistrer un MP3 dans le stockage permanent.
- [ ] Générer un nom de fichier unique (éviter les collisions).
- [ ] Service de lecture : récupérer un MP3 par son id (stream).
- [ ] Service de suppression du fichier physique.

---

## 3. Endpoints — Import (utilisés par le Programme 3)

- [ ] `POST /api/tracks/upload` — **multipart** : fichier MP3 + métadonnées JSON.
  - [ ] Valider le fichier et les métadonnées.
  - [ ] Sauvegarder le fichier dans le stockage permanent.
  - [ ] Enregistrer les métadonnées en base.
  - [ ] Gérer les **doublons** (idempotence).
  - [ ] Retourner une réponse de confirmation (200/201) — déclenche la suppression
        côté Programme 3.

---

## 4. Endpoints — CRUD MP3 (utilisés par le web)

- [ ] `GET    /api/tracks` — liste (avec filtres : genre, artiste, album, recherche).
- [ ] `GET    /api/tracks/{id}` — détail d'un MP3.
- [ ] `GET    /api/tracks/{id}/stream` — lecture / streaming du fichier MP3.
- [ ] `POST   /api/tracks` — ajout manuel (option).
- [ ] `PUT    /api/tracks/{id}` — modification des informations.
- [ ] `DELETE /api/tracks/{id}` — suppression (BD + fichier physique).

---

## 5. Endpoints — Authentification

- [ ] `POST /api/auth/register` — création de compte.
- [ ] `POST /api/auth/login` — login, retourne un **JWT**.
- [ ] Spring Security : encodage des mots de passe (BCrypt).
- [ ] Filtre JWT + protection des routes.
- [ ] (Option) Gestion des rôles (USER / ADMIN).

---

## 6. Endpoints — Playlists

- [ ] `POST /api/playlists/generate` — **génération** selon critères :
  - [ ] Entrée : durée totale souhaitée, genre, artiste, album, autres critères.
  - [ ] Algorithme : sélectionner des morceaux dont la **somme des durées**
        est proche/égale à la durée demandée (ex. glouton / sac à dos approché).
  - [ ] Retourner la liste proposée (non encore sauvegardée).
- [ ] `POST   /api/playlists` — sauvegarder une playlist (nom + liste de tracks).
- [ ] `GET    /api/playlists` — playlists de l'utilisateur connecté.
- [ ] `GET    /api/playlists/{id}` — détail (morceaux ordonnés).
- [ ] `PUT    /api/playlists/{id}` — modifier (ajout/retrait/réordre).
- [ ] `DELETE /api/playlists/{id}` — supprimer.
- [ ] `GET    /api/playlists/{id}/download` — **archive ZIP** des MP3 de la playlist.

---

## 7. Qualité & transverse

- [ ] DTOs + mapping (MapStruct ou manuel) — ne pas exposer les entités directement.
- [ ] Validation des entrées (`@Valid`, Bean Validation).
- [ ] Gestion globale des erreurs (`@ControllerAdvice`).
- [ ] Configuration **CORS** pour le frontend Vue.js.
- [ ] Documentation API (Swagger / springdoc-openapi).
- [ ] Tests unitaires + tests d'intégration (endpoints, génération de playlist).

---

## 8. Tests & validation

- [ ] Tester l'upload depuis le Programme 3 (flux complet).
- [ ] Vérifier la persistance des métadonnées et du fichier physique.
- [ ] Tester la génération de playlist (durée cible respectée).
- [ ] Tester le téléchargement ZIP.
- [ ] Tester l'authentification et la protection des routes.
