# Partie 1 — Backend Standalone (3 programmes + RabbitMQ)

> Trois applications **Spring Boot standalone** (mode non-web, `CommandLineRunner` /
> `@Scheduled` / listeners AMQP) communiquant **uniquement via RabbitMQ**.

## Architecture du flux

```
[incoming/] --(scan)--> Prog 1 ──► Queue Scan ──► Prog 2 ──► Queue Metadata ──► Prog 3 ──► API
                                       (extraction)                              (upload + delete)
```

---

## 0. Mise en place commune

- [ ] Installer / lancer **RabbitMQ** (local ou Docker : `rabbitmq:management`).
- [ ] Créer un module/projet parent Maven (ou 3 projets séparés).
- [ ] Ajouter la dépendance `spring-boot-starter-amqp` à chaque programme.
- [ ] Définir la configuration RabbitMQ partagée (host, port, user, pass).
- [ ] Déclarer les **2 queues** : `queue.scan` et `queue.metadata` (durables).
- [ ] Définir un **DTO commun** sérialisable JSON (`Mp3Message` : path, name, metadata).
- [ ] Configurer le `Jackson2JsonMessageConverter` pour la (dé)sérialisation.
- [ ] Mettre en place le logging **SLF4J + Logback** (1 fichier de log par programme).

---

## 1. Programme 1 — Scanner de répertoire

- [ ] Créer l'application Spring Boot standalone `scanner`.
- [ ] Configurer le chemin du répertoire `incoming/` (application.properties).
- [ ] Implémenter un scan périodique avec `@Scheduled(fixedDelay = 5min)` + `@EnableScheduling`.
- [ ] Lister les fichiers du répertoire.
- [ ] Filtrer **uniquement** les fichiers `.mp3` (ignorer le reste).
- [ ] Détecter les **nouveaux** fichiers depuis le dernier scan
      (état persistant : fichier d'état local, BD, ou check via l'API).
- [ ] Construire le message : nom + **chemin absolu**.
- [ ] Publier chaque MP3 détecté dans la **Queue Scan**.
- [ ] Logger : début du scan, chaque nouveau fichier détecté, erreurs/exceptions.

**Exemple de log attendu**
```
10:00 Début du scan
10:01 Nouveau fichier détecté : song1.mp3
```

---

## 2. Programme 2 — Extracteur de métadonnées

- [ ] Créer l'application Spring Boot standalone `extractor`.
- [ ] Ajouter la dépendance **jaudiotagger** (ou **mp3agic**).
- [ ] Implémenter un `@RabbitListener` sur la **Queue Scan**.
- [ ] Ouvrir le fichier MP3 et extraire :
  - [ ] Titre
  - [ ] Artiste
  - [ ] Album
  - [ ] Genre
  - [ ] Durée (en secondes)
  - [ ] Année
  - [ ] Date
  - [ ] Toute autre métadonnée disponible
- [ ] Gérer les métadonnées **manquantes** (valeurs par défaut / null).
- [ ] Construire le JSON de sortie (path + métadonnées).
- [ ] Publier le résultat dans la **Queue Metadata**.
- [ ] Logger : début extraction, succès, erreurs/exceptions.

**Sortie attendue**
```json
{
  "path": "C:/incoming/song1.mp3",
  "title": "Imagine",
  "artist": "John Lennon",
  "album": "Imagine",
  "genre": "Rock",
  "duration": 183
}
```

**Exemple de log attendu**
```
10:02 Début extraction : song1.mp3
10:02 Métadonnées extraites avec succès
```

---

## 3. Programme 3 — Uploader API

- [ ] Créer l'application Spring Boot standalone `uploader`.
- [ ] Implémenter un `@RabbitListener` sur la **Queue Metadata**.
- [ ] Construire l'appel HTTP `multipart/form-data` (fichier MP3 + métadonnées JSON)
      via `RestClient` / `RestTemplate` / `WebClient`.
- [ ] Appeler l'endpoint d'upload de l'API.
- [ ] **Succès** : à la confirmation de l'API, supprimer le fichier de `incoming/`.
- [ ] **Échec** : ne pas supprimer ; relancer N tentatives (retry + backoff).
- [ ] Si toutes les tentatives échouent : déplacer le fichier dans `failed/`.
- [ ] Configurer le nombre de tentatives et le délai entre tentatives.
- [ ] Logger : début envoi, envoi en cours, succès, suppression, échec/déplacement.

**Exemple de log attendu**
```
10:03 Début envoi API : song1.mp3
10:03 Envoi en cours
10:04 Envoi terminé avec succès
10:04 Suppression du fichier source
```

---

## 4. Tests & validation

- [ ] Déposer des MP3 réels dans `incoming/` et vérifier le flux complet.
- [ ] Déposer un fichier non-mp3 → doit être ignoré.
- [ ] Couper l'API → vérifier les retries puis le déplacement vers `failed/`.
- [ ] Vérifier qu'un fichier déjà importé n'est pas retraité.
- [ ] Vérifier le contenu des 3 fichiers de logs.
