# Guide d'utilisation — Vinylia

Guide pratique pour **lancer** et **exploiter** l'application de gestion de musique et de
génération de playlists. Pour le détail technique de chaque partie, voir les fichiers
`01/02/03-*-avancement.md` du même dossier.

---

## 1. Que fait l'application ?

Vinylia se compose de **deux mondes complémentaires** :

1. **L'import automatique** (en arrière-plan) — vous déposez des fichiers `.mp3` dans un dossier,
   et la chaîne les détecte, lit leurs métadonnées (titre, artiste, album, genre, durée…),
   puis les envoie à l'API qui les stocke durablement. Le fichier d'origine est ensuite supprimé.
2. **L'application web** (interface utilisateur) — vous vous connectez, gérez votre bibliothèque,
   écoutez la musique, **générez des playlists à la durée souhaitée**, les sauvegardez et les
   téléchargez en archive ZIP.

> Concrètement : la musique entre par le dossier `incoming/` **ou** par le bouton « Importer »
> du site ; elle ressort sous forme de playlists écoutables et téléchargeables.

---

## 2. Les briques et leurs rôles

| Brique | Rôle | Où |
|---|---|---|
| **Programme 1 — Scanner** | Surveille `incoming/`, repère les nouveaux `.mp3` | profil `scanner` |
| **Programme 2 — Extracteur** | Lit les métadonnées des MP3 | profil `extractor` |
| **Programme 3 — Uploader** | Envoie fichier + métadonnées à l'API, puis supprime la source | profil `uploader` |
| **API Backend** | Stocke en base + sur disque, expose les données au web | profil par défaut (port 8080) |
| **Application Web** | Interface : login, MP3, playlists, lecture, ZIP | `front/` (port 5173) |
| **RabbitMQ** | Fait communiquer les 3 programmes entre eux | Docker (ports 5672 / 15672) |
| **PostgreSQL** | Base de données (`mp3`) | port 5432 |

---

## 3. Pré-requis

- **Java 17+** et **Maven** (via `./mvnw` fourni).
- **Node 22+** et **npm** (pour le web).
- **PostgreSQL** démarré, base **`mp3`** existante (user `postgres` / mdp `lapatia1706`).
- **Docker** (pour RabbitMQ), uniquement si vous utilisez l'import automatique.

---

## 4. Démarrage rapide

### Étape A — Infrastructure
```bash
# RabbitMQ (uniquement pour l'import automatique)
bash back/tools/start-rabbitmq.sh        # console : http://localhost:15672 (guest/guest)
# PostgreSQL : déjà lancé, base mp3 déjà créée
```

### Étape B — L'API (indispensable pour le web)
```bash
cd back
./mvnw clean package -DskipTests
java -jar target/mp3-0.0.1-SNAPSHOT.jar          # API sur http://localhost:8080
```

### Étape C — L'application web
```bash
cd front
npm install        # la première fois seulement
npm run dev        # http://localhost:5173
```
Ouvrez **http://localhost:5173** dans votre navigateur.

### Étape D — L'import automatique (les 3 programmes)

> ⚠️ **Indispensable** pour que les fichiers déposés dans `incoming/` soient traités.
> Tant que ces 3 programmes ne tournent pas, **rien ne se passe** quand vous déposez un MP3
> (aucun log, aucun morceau ajouté). Pré-requis : RabbitMQ (étape A) **et** l'API (étape B) lancés.

**Option 1 — tout lancer d'un coup** (depuis `back/`) :
```bash
bash tools/start-pipeline.sh          # scan toutes les 5 min (défaut du sujet)
bash tools/start-pipeline.sh 5000     # scan toutes les 5 s (pratique pour tester)
# pour arrêter : bash tools/stop-pipeline.sh
```

**Option 2 — 3 terminaux séparés** (montre bien les « 3 programmes indépendants »), **depuis `back/`** :
```bash
java -jar target/mp3-0.0.1-SNAPSHOT.jar --spring.profiles.active=extractor
java -jar target/mp3-0.0.1-SNAPSHOT.jar --spring.profiles.active=uploader
java -jar target/mp3-0.0.1-SNAPSHOT.jar --spring.profiles.active=scanner
```

Ensuite, déposez vos `.mp3` dans le dossier **`back/incoming/`** (et non à la racine du projet).

**À savoir :**
- Le scanner lit le dossier **toutes les 5 minutes** par défaut (conforme au sujet) : après
  un dépôt, patientez jusqu'au prochain scan, ou baissez l'intervalle pour tester
  (`--app.scan.interval-ms=5000` ou `bash tools/start-pipeline.sh 5000`).
- Suivez le traitement en direct dans `back/logs/scanner.log`, `extractor.log`, `uploader.log`.
- Dans le site, **rafraîchissez la page Bibliothèque** pour voir les nouveaux morceaux
  (la liste se charge à l'ouverture de la page).

---

## 5. Utiliser l'application web

### 5.1 Se connecter
À la première ouverture, onglet **Inscription** → choisissez un identifiant + mot de passe
(email facultatif) → vous êtes connecté automatiquement. Ensuite, onglet **Connexion**.
La session reste active (jeton mémorisé) ; bouton de **déconnexion** en bas de la barre latérale.

### 5.2 Bibliothèque (gérer les MP3)
- **Rechercher** par titre/artiste/album ; **filtrer** par genre.
- **Importer** un MP3 manuellement (bouton « Importer » : fichier + infos).
- **Écouter** : survolez une ligne et cliquez sur ▶ (ou double-clic). Le morceau se lance dans
  la barre du bas ; le **vinyle tourne** pendant la lecture.
- **Tout lire** / **Aléatoire** : lance toute la liste affichée.
- **Modifier** (crayon) ou **Supprimer** (corbeille) un morceau.

### 5.3 Lecteur (barre du bas)
Lecture/pause, précédent/suivant, **barre de progression** (cliquable pour se déplacer),
**volume**, et infos du morceau en cours. Le lecteur reste actif quand vous changez de page.

### 5.4 Générer une playlist
1. Réglez la **durée souhaitée** (curseur ou présets 30/60/90/120 min).
2. Affinez avec **genre / artiste / album** (facultatif).
3. Cliquez **Générer** : l'app propose une sélection dont la durée s'approche de l'objectif
   (une **jauge** montre obtenu vs objectif).
4. Ajustez librement : **Ajouter** d'autres morceaux, **retirer** (✕) — même si la durée
   ne correspond plus.
5. **Enregistrer** (donnez un nom) → la playlist est sauvegardée.

### 5.5 Mes playlists
- Grille de vos playlists ; cliquez pour ouvrir le détail.
- Dans le détail : **Lire** tout, **télécharger en ZIP** (tous les MP3),
  **Ajouter**/retirer des morceaux, **Renommer**, **Supprimer**.

---

## 6. Comment circule un morceau (vue d'ensemble)

```
Dépôt dans incoming/ ──► [Scanner] ──Queue Scan──► [Extracteur] ──Queue Metadata──►
[Uploader] ──HTTP──► [API] ──► PostgreSQL + stockage disque ──► [Web] écoute / playlists / ZIP
```
En cas d'échec d'envoi, l'Uploader réessaie puis déplace le fichier dans `back/failed/`
(le fichier n'est jamais perdu).

---

## 7. Dépannage

| Symptôme | Cause probable / solution |
|---|---|
| Le site ne charge rien / erreurs réseau | L'API n'est pas lancée → lancez l'étape B. |
| « 403 » ou retour à la page de connexion | Jeton expiré → reconnectez-vous. |
| Les MP3 déposés n'apparaissent pas | Vérifiez RabbitMQ (`docker ps`) et que les 3 programmes tournent. |
| RabbitMQ ne démarre pas (`.erlang.cookie`) | Utilisez `back/tools/start-rabbitmq.sh` (contournement intégré). |
| Erreur de connexion à la base | PostgreSQL non démarré ou base `mp3` absente. |
| Le port 8080 est occupé | Une API tourne déjà ; arrêtez-la avant d'en relancer une. |

Journaux : `back/logs/` (`scanner.log`, `extractor.log`, `uploader.log`, `api.log`).

---

## 8. Scripts utiles (`back/tools/`)
- `start-rabbitmq.sh` — démarre RabbitMQ (Docker).
- `run_api_test.sh` — teste l'API seule (auth, upload, playlists, ZIP).
- `run_integration_test.sh` — teste toute la chaîne (programmes → API → base).
- `make_mp3.py` — génère un MP3 de test ; `mock_api.py` — fausse API de test.
