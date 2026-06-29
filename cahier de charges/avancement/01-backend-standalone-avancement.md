# Avancement — Partie 1 : Backend Standalone

**Statut : TERMINÉ et TESTÉ end-to-end (succès + échec).**
Date : 2026-06-18 · Stack : Spring Boot 4.1 (Java 17) + RabbitMQ + jaudiotagger.

---

## 1. Ce qui est fait

Les **3 programmes** du fichier [01-backend-standalone.md](../01-backend-standalone.md)
sont implémentés dans le projet `back/`, sous forme d'**une seule application
Spring Boot** activable par **profil** (1 process = 1 profil) :

| Programme | Profil Spring | Rôle | Classe |
|-----------|---------------|------|--------|
| 1. Scanner | `scanner` | Scan périodique de `incoming/`, filtre `.mp3`, publie | `s6.mp3.scanner.DirectoryScanner` |
| 2. Extracteur | `extractor` | Lit `queue.scan`, extrait les métadonnées, publie | `s6.mp3.extractor.MetadataExtractor` |
| 3. Uploader | `uploader` | Lit `queue.metadata`, POST à l'API, supprime/déplace | `s6.mp3.uploader.ApiUploader` |

Détails couverts :
- **Scanner** : `@Scheduled` (intervalle configurable, défaut 5 min), ne traite que `.mp3`,
  ignore le reste, détecte les nouveaux fichiers (mémoire des chemins déjà vus + purge
  des fichiers disparus), publie `nom + chemin absolu` dans `queue.scan`.
- **Extracteur** : `@RabbitListener` sur `queue.scan`, extraction via **jaudiotagger**
  (titre, artiste, album, genre, durée, année), titre = nom de fichier si absent,
  publie le JSON dans `queue.metadata`.
- **Uploader** : `@RabbitListener` sur `queue.metadata`, envoi **multipart** (fichier + JSON)
  via `RestClient`, **N tentatives** (défaut 3) avec délai ; succès → suppression du
  fichier source ; échec total → déplacement dans `failed/`.
- **Logs** : chaque programme écrit dans `back/logs/{scanner,extractor,uploader}.log`
  (actions, erreurs, exceptions) — format conforme aux exemples du cahier des charges.
- **RabbitMQ** : 2 files durables `queue.scan` et `queue.metadata`, converter JSON
  (`JacksonJsonMessageConverter`, Jackson 3).

### Fichiers créés (`back/src/main/java/s6/mp3/`)
```
common/RabbitConfig.java        # files + converter JSON
common/ScanMessage.java         # DTO Prog1 -> Prog2
common/MetadataMessage.java     # DTO Prog2 -> Prog3
scanner/DirectoryScanner.java   # Programme 1
scanner/SchedulingConfig.java   # @EnableScheduling (profil scanner)
extractor/MetadataExtractor.java# Programme 2
uploader/ApiUploader.java       # Programme 3
```
Config : `application.properties` + `application-{scanner,extractor,uploader}.properties`.
Outils de test : `back/tools/` (génération MP3, mock API, scripts).

### Dépendances ajoutées (`back/pom.xml`)
`spring-boot-starter-web`, `spring-boot-starter-amqp`, `net.jthink:jaudiotagger:3.0.1`,
`org.postgresql:postgresql` (pilote, pour la Partie 2).

---

## 2. Pré-requis / infrastructure

- **RabbitMQ** (Docker) : `bash back/tools/start-rabbitmq.sh`
  → AMQP `localhost:5672`, console `http://localhost:15672` (guest/guest).
  *(le script contourne le bug `.erlang.cookie: eacces` de Docker Desktop sous Windows.)*
- **PostgreSQL** : déjà installé/démarré (port 5432). Base **`mp3`** créée
  (`psql -U postgres`, mot de passe `lapatia1706`). Utilisée en Partie 2, pas en Partie 1.

---

## 3. Utilisation

Compiler / packager (depuis `back/`) :
```bash
./mvnw clean package -DskipTests
```

Lancer les 3 programmes (3 terminaux, **toujours depuis `back/`** — chemins relatifs) :
```bash
java -jar target/mp3-0.0.1-SNAPSHOT.jar --spring.profiles.active=scanner
java -jar target/mp3-0.0.1-SNAPSHOT.jar --spring.profiles.active=extractor
java -jar target/mp3-0.0.1-SNAPSHOT.jar --spring.profiles.active=uploader
```
Puis déposer des `.mp3` dans `back/incoming/`. Paramètres utiles (override CLI) :
`--app.scan.interval-ms=`, `--app.upload.max-retries=`, `--app.api.upload-url=`.

Réglages clés (`application.properties`) : `app.incoming-dir=incoming`,
`app.failed-dir=failed`, `app.api.upload-url=http://localhost:8080/api/tracks/upload`.

---

## 4. Tests réalisés (réels)

Script `back/tools/run_success_test.sh` (RabbitMQ + mock API Python) :

- **Chemin succès** : dépôt `song1.mp3` (+ `notes.txt` ignoré) →
  `Nouveau fichier detecte: song1.mp3` → `Metadonnees extraites: Imagine / John Lennon /
  Rock / duration=5 / 1971` → `Envoi termine avec succes` → `Suppression du fichier source`.
  Résultat : fichier **supprimé** de `incoming/`, API a reçu 80260 octets (fichier + métadonnées).
- **Chemin échec** (API indisponible) : `tentative 1/3, 2/3, 3/3` échouées →
  `Deplacement vers le dossier d'echec` → fichier **déplacé dans `failed/`**.
- **Filtrage** : `.txt` jamais traité.

---

## 5. Contrat d'interface pour la Partie 2 (API)

L'Uploader (Programme 3) appelle **un seul endpoint**, à implémenter en Partie 2 :

- **`POST /api/tracks/upload`**, `Content-Type: multipart/form-data`, 2 parts :
  - `file` → le fichier MP3 (binaire),
  - `metadata` → JSON `application/json` :
    ```json
    {
      "path": "...", "fileName": "song1.mp3",
      "title": "Imagine", "artist": "John Lennon", "album": "Imagine",
      "genre": "Rock", "duration": 183, "year": "1971", "date": "1971"
    }
    ```
- **Réponse attendue** : statut **2xx** = succès (sinon l'Uploader réessaie puis déplace en échec).
- Côté API : enregistrer les métadonnées en base (`mp3`) + copier le MP3 dans le
  **stockage permanent**, puis répondre 2xx.

---

## 6. Reste à faire (Partie 2)

- Implémenter l'endpoint d'upload ci-dessus (profil par défaut = application web, port 8080).
- Brancher JPA/PostgreSQL (`spring-boot-starter-data-jpa` + datasource `mp3`).
- Entités + endpoints CRUD/auth/playlists → voir [02-api-backend.md](../02-api-backend.md).
