# Avancement — Partie 2 : API Backend

**Statut : TERMINÉ et TESTÉ (API seule + intégration complète Partie 1 → Partie 2).**
Date : 2026-06-18 · Stack : Spring Boot 4.1 (Java 17) + Spring Data JPA + Spring Security 7 (JWT) + PostgreSQL.

---

## 1. Ce qui est fait

Tout le fichier [02-api-backend.md](../02-api-backend.md) est implémenté dans `back/`,
package `s6.mp3.api`. L'**API est le profil par défaut** (application web, port 8080) ;
les 3 programmes standalone restent activés par profil (`scanner`/`extractor`/`uploader`)
et **ne dépendent pas de la base** (auto-config JPA/DataSource exclue pour ces profils).

| Domaine | Classes | Endpoints |
|---|---|---|
| Auth | `user/` (User, UserRepository, AuthService, AuthController), `security/` (JwtService, JwtAuthFilter, CustomUserDetailsService, SecurityConfig) | `POST /api/auth/register`, `POST /api/auth/login` |
| Morceaux | `track/` (Track, TrackRepository, TrackService, StorageService, TrackController) | `POST /api/tracks/upload`, `GET /api/tracks`, `GET /api/tracks/{id}`, `GET /api/tracks/{id}/stream`, `POST /api/tracks`, `PUT /api/tracks/{id}`, `DELETE /api/tracks/{id}` |
| Playlists | `playlist/` (Playlist, PlaylistTrack, PlaylistRepository, PlaylistService, PlaylistGenerator, PlaylistController) | `POST /api/playlists/generate`, `POST /api/playlists`, `GET /api/playlists`, `GET /api/playlists/{id}`, `PUT /api/playlists/{id}`, `DELETE /api/playlists/{id}`, `GET /api/playlists/{id}/download` |
| Transverse | `common/` (NotFoundException, GlobalExceptionHandler) | gestion d'erreurs JSON, CORS |

Points clés :
- **Modèle** : tables `users`, `tracks`, `playlists`, `playlist_tracks` (créées auto via `ddl-auto=update`).
- **Stockage permanent** : `back/storage/`, nom de fichier unique (UUID), lecture (stream) + suppression physique.
- **Idempotence** : empreinte **SHA-256** du contenu (`tracks.content_hash` unique) → un même fichier n'est pas dupliqué.
- **Sécurité** : Spring Security 7, mots de passe **BCrypt**, **JWT** (HS384) via filtre, routes protégées par défaut.
- **Génération de playlist** : algorithme glouton + ajustement final pour approcher la durée cible.
- **Téléchargement ZIP** : archive en flux contenant les MP3 de la playlist (noms dédupliqués).
- **CORS** : autorisé pour `http://localhost:5173` / `5174` (Vite).
- **Validation** (`@Valid`) + **GlobalExceptionHandler** (`@RestControllerAdvice`).

### Dépendances ajoutées (`back/pom.xml`)
`spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`,
`io.jsonwebtoken:jjwt-{api,impl,gson}:0.12.6`, `org.projectlombok:lombok`.

---

## 2. Configuration (profil `api`, fichier `application-api.properties`)
- BD : `jdbc:postgresql://localhost:5432/mp3`, user `postgres`, mdp `lapatia1706`, `ddl-auto=update`.
- Stockage : `app.storage-dir=storage`. Upload : 50 Mo max.
- JWT : `app.jwt.secret`, `app.jwt.expiration-ms=86400000` (24 h).
- CORS : `app.cors.allowed-origins`.

---

## 3. Utilisation

Pré-requis : PostgreSQL démarré + base `mp3` existante (déjà créée).

```bash
cd back
./mvnw clean package -DskipTests
java -jar target/mp3-0.0.1-SNAPSHOT.jar        # API sur http://localhost:8080
```

Exemples :
```bash
# Inscription -> renvoie un token
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"secret123"}'

# Lister les morceaux (route protégée)
curl localhost:8080/api/tracks -H "Authorization: Bearer <TOKEN>"
```
Scripts de test : `back/tools/run_api_test.sh` (API seule) et
`back/tools/run_integration_test.sh` (chaîne complète Programme 1+2+3 → API → BD).

---

## 4. Tests réalisés (réels)
- **API seule** (`run_api_test.sh`) : register/login, protection 403 sans token, upload + **idempotence**
  (re-upload → même id), liste, **stream** (200, `audio/mpeg`), génération, sauvegarde, liste,
  **ZIP** (`['song1.mp3','song2.mp3']`), modification, suppression (204). Tout OK.
- **Intégration** (`run_integration_test.sh`) : dépôt d'un MP3 → scanner → extracteur → uploader →
  **API réelle** → fichier supprimé de `incoming/` → morceau persisté en base (`GET /api/tracks`).
  Vérifié aussi : le scanner démarre **sans** DataSource/Hibernate (Partie 1 indépendante de la BD).

---

## 5. Contrat pour la Partie 3 (Frontend Vue)
- **Base URL** : `http://localhost:8080`. **CORS** déjà ouvert pour Vite (5173/5174).
- **Auth** : `POST /api/auth/login` → `{ token, username, role }`. Envoyer ensuite
  `Authorization: Bearer <token>` sur toutes les routes (sauf `/api/auth/**`, `POST /api/tracks/upload`,
  `GET /api/tracks/{id}/stream` qui sont publiques).
- **Lecture audio** : `<audio :src="http://localhost:8080/api/tracks/{id}/stream">` (public, pas de header).
- **Téléchargement ZIP** : `GET /api/playlists/{id}/download` (avec token → récupérer en blob).
- **Génération** : `POST /api/playlists/generate` body `{ durationMinutes, genre, artist, album }`
  → playlist proposée (non sauvegardée). **Sauvegarde** : `POST /api/playlists` body `{ name, trackIds:[...] }`.

---

## 6. Reste à faire (Partie 3)
- Application Vue : login/inscription, CRUD MP3, génération/édition/sauvegarde de playlists,
  lecture audio, téléchargement ZIP, gestion des playlists. Voir [03-web-frontend.md](../03-web-frontend.md).
- (Optionnel API) : Swagger/springdoc, gestion fine des rôles, tests automatisés JUnit.
